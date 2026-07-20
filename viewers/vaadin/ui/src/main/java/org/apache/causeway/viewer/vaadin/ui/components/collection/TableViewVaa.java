/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.apache.causeway.viewer.vaadin.ui.components.collection;

import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.ListDataProvider;

import org.apache.causeway.commons.collections.Can;
import org.apache.causeway.core.metamodel.object.ManagedObject;
import org.apache.causeway.core.metamodel.object.MmTitleUtils;
import org.apache.causeway.core.metamodel.tabular.DataColumn;
import org.apache.causeway.core.metamodel.tabular.DataRow;
import org.apache.causeway.core.metamodel.tabular.DataTableInteractive;

/**
 * Renders a {@link DataTableInteractive} (parented collection or standalone
 * action result) as a Vaadin {@link Grid}: first column is the row element's
 * title, then one column per visible association.
 */
public class TableViewVaa extends VerticalLayout {

    private static final long serialVersionUID = 1L;

    /**
     * Above this row count, a fixed-height scrollable grid (Vaadin's default) is
     * cheaper than rendering every row into the DOM; at or below it, sizing the
     * grid to its content (no dead scroll space, no arbitrary fixed height next
     * to content-hugging fieldset cards) reads far better.
     */
    private static final int ALL_ROWS_VISIBLE_THRESHOLD = 15;

    public static Component forDataTableInteractive(final DataTableInteractive dataTable) {
        return new TableViewVaa(dataTable, null);
    }

    /**
     * @param onRowSelect invoked with the row's domain object when a row is clicked
     *                    (e.g. to navigate to that object's page); may be {@code null}.
     */
    public static Component forDataTableInteractive(
            final DataTableInteractive dataTable,
            final Consumer<ManagedObject> onRowSelect) {
        return new TableViewVaa(dataTable, onRowSelect);
    }

    private TableViewVaa(final DataTableInteractive dataTable, final Consumer<ManagedObject> onRowSelect) {
        var rows = dataTable.dataRowsFilteredAndSortedObservable().getValue();
        if (rows.isEmpty()) {
            add(new Span("No rows to display."));
            return;
        }

        var rowsList = rows.toList();
        var columns = dataTable.dataColumnsObservable().getValue();

        var grid = new Grid<DataRow>();
        // fit the parent's width (e.g. a 50/50 layout column) with its own internal
        // horizontal scrollbar if the auto-widened columns don't all fit, rather than
        // sizing to the sum of the columns' content width and overflowing the parent.
        grid.setWidthFull();
        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES, GridVariant.LUMO_COMPACT);
        if (rowsList.size() <= ALL_ROWS_VISIBLE_THRESHOLD) {
            grid.setAllRowsVisible(true);
        }

        grid.addColumn(row -> MmTitleUtils.titleOf(row.rowElement()))
                .setHeader("");

        columns.forEach(column -> {
            var gridColumn = grid.addColumn(row -> stringifyCell(row, column))
                    .setHeader(column.columnFriendlyNameObservable().getValue())
                    .setSortable(true);
            // the ValueProvider above returns the cell's *displayed* String, so a
            // plain setSortable(true) would sort numbers/dates lexicographically
            // ("10" before "2"). Compare the underlying value when there's exactly
            // one comparable cell element; fall back to the displayed text otherwise
            // (e.g. multi-valued cells, or a value type with no natural ordering).
            gridColumn.setComparator((rowA, rowB) -> compareCells(rowA, rowB, column));
        });

        grid.getColumns().forEach(column -> column.setAutoWidth(true));

        if (onRowSelect != null) {
            grid.addItemClickListener(event -> onRowSelect.accept(event.getItem().rowElement()));
        }

        var dataProvider = new ListDataProvider<>(rowsList);
        grid.setItems(dataProvider);
        grid.setColumnReorderingAllowed(true);

        if (rowsList.size() > 1) {
            var searchField = new TextField();
            searchField.setPlaceholder("Search");
            searchField.setPrefixComponent(VaadinIcon.SEARCH.create());
            searchField.setClearButtonVisible(true);
            searchField.setWidth("16em");
            // matched on the fly per keystroke rather than a precomputed per-row
            // index: at this data scale that's cheaper than eagerly re-stringifying
            // every cell (the grid's own lazy renderer already does that per cell).
            searchField.addValueChangeListener(event -> {
                var term = event.getValue() == null ? "" : event.getValue().strip().toLowerCase();
                dataProvider.setFilter(row -> term.isEmpty() || matchesSearch(row, columns, term));
            });
            add(searchField);
        }

        add(grid);
        setWidthFull();
    }

    private static String stringifyCell(final DataRow row, final DataColumn column) {
        return row.getCellElementsForColumn(column).stream()
                .map(TableViewVaa::stringifyCellElement)
                .collect(Collectors.joining(", "));
    }

    private static String stringifyCellElement(final ManagedObject cellElement) {
        if (cellElement == null || cellElement.getPojo() == null) {
            return "";
        }
        var pojo = cellElement.getPojo();
        // render "large" value types compactly in a table cell rather than via their
        // (verbose) default title / toString.
        if (pojo instanceof org.apache.causeway.applib.value.Markup markup) {
            return markup.html() == null ? "" : markup.html().replaceAll("<[^>]+>", "").strip();
        }
        if (pojo instanceof org.apache.causeway.applib.value.NamedWithMimeType named) {
            return named.name();
        }
        return MmTitleUtils.titleOf(cellElement);
    }

    /** The cell's single underlying pojo, or {@code null} if empty/multi-valued. */
    private static Object singleCellPojo(final DataRow row, final DataColumn column) {
        var elements = row.getCellElementsForColumn(column).stream().toList();
        if (elements.size() != 1 || elements.get(0) == null) {
            return null;
        }
        return elements.get(0).getPojo();
    }

    @SuppressWarnings("unchecked")
    private static int compareCells(final DataRow rowA, final DataRow rowB, final DataColumn column) {
        var pojoA = singleCellPojo(rowA, column);
        var pojoB = singleCellPojo(rowB, column);
        if (pojoA instanceof Comparable<?> && pojoB != null && pojoA.getClass().isInstance(pojoB)) {
            return ((Comparable<Object>) pojoA).compareTo(pojoB);
        }
        return stringifyCell(rowA, column).compareToIgnoreCase(stringifyCell(rowB, column));
    }

    private static boolean matchesSearch(final DataRow row, final Can<DataColumn> columns, final String term) {
        return MmTitleUtils.titleOf(row.rowElement()).toLowerCase().contains(term)
                || columns.stream().anyMatch(column -> stringifyCell(row, column).toLowerCase().contains(term));
    }
}
