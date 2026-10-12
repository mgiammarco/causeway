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
package org.apache.causeway.viewer.vaadin.ui.components.object;

import java.util.function.Consumer;
import java.util.function.Supplier;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.HasComponents;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.formlayout.FormLayout.ResponsiveStep;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.tabs.TabSheetVariant;
import com.vaadin.flow.theme.lumo.LumoUtility;

import org.apache.causeway.applib.annotation.Where;
import org.apache.causeway.applib.layout.component.ActionLayoutData;
import org.apache.causeway.applib.layout.component.CollectionLayoutData;
import org.apache.causeway.applib.layout.component.DomainObjectLayoutData;
import org.apache.causeway.applib.layout.component.FieldSet;
import org.apache.causeway.applib.layout.component.PropertyLayoutData;
import org.apache.causeway.applib.layout.grid.bootstrap.BSClearFix;
import org.apache.causeway.applib.layout.grid.bootstrap.BSCol;
import org.apache.causeway.applib.layout.grid.bootstrap.BSRow;
import org.apache.causeway.applib.layout.grid.bootstrap.BSTab;
import org.apache.causeway.applib.layout.grid.bootstrap.BSTabGroup;
import org.apache.causeway.applib.value.Blob;
import org.apache.causeway.applib.value.Clob;
import org.apache.causeway.commons.binding.ChangeListener;
import org.apache.causeway.core.metamodel.interactions.managed.ActionInteraction;
import org.apache.causeway.core.metamodel.interactions.managed.CollectionInteraction;
import org.apache.causeway.core.metamodel.interactions.managed.ManagedAction;
import org.apache.causeway.core.metamodel.interactions.managed.ManagedProperty;
import org.apache.causeway.core.metamodel.interactions.managed.PropertyInteraction;
import org.apache.causeway.core.metamodel.interactions.managed.PropertyNegotiationModel;
import org.apache.causeway.core.metamodel.object.ManagedObject;
import org.apache.causeway.core.metamodel.object.MmTitleUtils;
import org.apache.causeway.core.metamodel.spec.feature.ObjectAction;
import org.apache.causeway.viewer.commons.model.components.UiComponentFactory;
import org.apache.causeway.viewer.commons.model.decorators.DisablingDecorator.DisablingDecorationModel;
import org.apache.causeway.viewer.commons.model.layout.UiGridLayout;
import org.apache.causeway.viewer.vaadin.model.util.Vaa;
import org.apache.causeway.viewer.vaadin.ui.components.UiComponentFactoryVaa;
import org.apache.causeway.viewer.vaadin.ui.components.collection.TableViewVaa;

/**
 * Renders a domain object's page: title, actions, fieldsets with properties,
 * tab groups and collections — driven by the object's bootstrap grid layout
 * via the {@link UiGridLayout.Visitor} (Visitor pattern: layout traversal is
 * in commons, only the Vaadin rendering lives here).
 */
public class ObjectViewVaa extends VerticalLayout {

    private static final long serialVersionUID = 1L;

    /** {@link ComponentUtil} data key under which a tab's content remembers the tab's name. */
    private static final String TAB_NAME_KEY = "causeway.vaa.tab-name";

    /** Name of the tab {@code component} is rendered in, or {@code null} when it is not inside one. */
    private static String enclosingTabName(final HasComponents component) {
        Component current = (Component) component;
        while (current != null) {
            var name = ComponentUtil.getData(current, TAB_NAME_KEY);
            if (name instanceof String tabName) {
                return tabName;
            }
            current = current.getParent().orElse(null);
        }
        return null;
    }

    public static ObjectViewVaa fromObject(
            final UiComponentFactoryVaa uiComponentFactory,
            final Consumer<ManagedAction> actionEventHandler,
            final Consumer<ManagedObject> objectNavHandler,
            final ManagedObject managedObject) {
        return new ObjectViewVaa(uiComponentFactory, actionEventHandler, objectNavHandler, managedObject);
    }

    protected ObjectViewVaa(
            final UiComponentFactoryVaa uiComponentFactory,
            final Consumer<ManagedAction> actionEventHandler,
            final Consumer<ManagedObject> objectNavHandler,
            final ManagedObject managedObject) {

        var objectTitle = MmTitleUtils.titleOf(managedObject);

        var gridVisitor = new UiGridLayout.Visitor<HasComponents, TabSheet>(this) {

            /**
             * Ids of the actions already shown in the current fieldset's action panel. The
             * metamodel can also report such an action as "associated" with one of the
             * fieldset's properties (so it would appear a second time next to that property);
             * showing a button twice in one card only confuses, so the property skips it.
             */
            private final java.util.Set<String> fieldSetActionIds = new java.util.HashSet<>();

            @Override
            protected void onObjectTitle(final HasComponents container, final DomainObjectLayoutData domainObjectData) {
                // a generic per-object icon (Wicket shows a type-specific one via
                // @DomainObjectLayout(cssClassFa=...), a Font Awesome class name with
                // no equivalent in Vaadin's own icon set; none of this demo's domain
                // objects declare one anyway, so a single consistent icon here gives
                // the same "not just bare text" visual weight without a brittle
                // fa-class-name-to-VaadinIcon mapping for a facet nothing uses yet).
                var icon = VaadinIcon.RECORDS.create();
                icon.setSize("1.25em");
                icon.addClassNames(LumoUtility.TextColor.PRIMARY);

                // one dominant heading style: neutral header color, the icon carries the accent
                var h1 = new H1(objectTitle);
                h1.addClassNames(LumoUtility.Margin.NONE, LumoUtility.FontSize.XXLARGE,
                        LumoUtility.TextColor.HEADER);

                var titleRow = Vaa.add(container, new HorizontalLayout(icon, h1));
                titleRow.setAlignItems(FlexComponent.Alignment.CENTER);
                titleRow.setPadding(false);
            }

            // The bootstrap 12-column grid maps onto CSS Grid (see the "causeway" theme):
            // a row is a 12-track grid with a gap, a column spans N tracks via a custom
            // property, and the theme stacks columns below a container width. This replaces
            // the former flex-basis/calc() gutter arithmetic, which drifted by the gutter
            // width on every nesting level (tabbed vs untabbed cards were visibly misaligned).
            @Override
            protected HasComponents newRow(final HasComponents container, final BSRow bsRow) {
                var uiRow = Vaa.add(container, new Div());
                uiRow.addClassName("vaa-row");
                return uiRow;
            }

            @Override
            protected HasComponents newCol(final HasComponents container, final BSCol bsCol) {
                var uiCol = Vaa.add(container, new Div());
                uiCol.addClassName("vaa-col");
                uiCol.getStyle().set("--vaa-span", String.valueOf(bsCol.getSpan()));
                return uiCol;
            }

            @Override
            protected HasComponents newActionPanel(final HasComponents container) {
                var uiActionPanel = Vaa.add(container, new Div());
                uiActionPanel.addClassName("vaa-actions");
                return uiActionPanel;
            }

            @Override
            protected TabSheet newTabGroup(final HasComponents container, final BSTabGroup tabGroupData) {
                var tabSheet = new TabSheet();
                container.add(tabSheet);
                tabSheet.setWidthFull();
                // TabSheet pads its content area by default (--vaadin-tabsheet-padding);
                // our own fieldset cards already carry their own padding, so left as-is a
                // tabbed fieldset (e.g. "Identity") renders narrower per side than an
                // untabbed sibling fieldset (e.g. "Contact") in the same row.
                tabSheet.addThemeVariants(TabSheetVariant.LUMO_NO_PADDING);
                // a layout may declare a tab group with no tabs (Causeway's grid validation
                // requires one as the "unreferenced collections" safety net): shown as is,
                // it would draw an empty tab strip, so it stays hidden until a tab arrives
                tabSheet.setVisible(false);
                return tabSheet;
            }

            @Override
            protected HasComponents newTab(final TabSheet tabSheet, final BSTab tabData) {
                var tabContent = new Div();
                // same column rules as any other grid column, so a card inside a tab lines up
                // exactly with an untabbed sibling card in the same column
                tabContent.addClassName("vaa-col");
                ComponentUtil.setData(tabContent, TAB_NAME_KEY, tabData.getName());
                tabSheet.add(tabData.getName(), tabContent);
                tabSheet.setVisible(true);
                return tabContent;
            }

            @Override
            protected HasComponents newFieldSet(final HasComponents container, final FieldSet fieldSetData) {
                // render each fieldset as a light "card": header, associated actions, then a
                // two-column responsive form for its properties. The card look (surface,
                // border, radius, elevation) lives in the theme, so it follows dark mode.
                var card = Vaa.add(container, new Div());
                card.addClassName("vaa-card");

                // a tab already titled like its only fieldset (e.g. tab "Identity" holding
                // fieldset "Identity") would show the same word twice, one under the other
                if (!fieldSetData.getName().equals(enclosingTabName(container))) {
                    Vaa.add(card, new H2(fieldSetData.getName()));
                }

                var actionBar = newActionPanel(card);
                fieldSetActionIds.clear();
                for (var actionData : fieldSetData.getActions()) {
                    fieldSetActionIds.add(actionData.getId());
                    onAction(actionBar, actionData);
                }

                var uiFieldSet = Vaa.add(card, new FormLayout());
                uiFieldSet.setResponsiveSteps(
                        new ResponsiveStep("0", 1),
                        new ResponsiveStep("40em", 2));
                return uiFieldSet;
            }

            @Override
            protected void onClearfix(final HasComponents container, final BSClearFix clearFixData) {
                // not needed: CSS Grid places columns on its own
            }

            @Override
            protected void onAction(final HasComponents container, final ActionLayoutData actionData) {
                onAction(container, actionData.getId());
            }

            /**
             * Renders an action found by live metamodel lookup (an {@link ObjectAction},
             * e.g. via {@code ManagedProperty}/{@code ManagedCollection#getAssociatedActions()})
             * rather than one listed in a static layout.xml.
             */
            private void onAction(final HasComponents container, final ObjectAction objectAction) {
                onAction(container, objectAction.getId());
            }

            private void onAction(final HasComponents container, final String actionId) {
                var interaction = ActionInteraction.start(managedObject, actionId, Where.OBJECT_FORMS);
                interaction.checkVisibility()
                        .getManagedAction()
                        .ifPresent(managedAction -> {
                            interaction.checkUsability();
                            Vaa.add(container, uiComponentFactory.buttonFor(
                                    new UiComponentFactory.ButtonRequest(
                                            managedAction,
                                            DisablingDecorationModel.of(interaction),
                                            actionEventHandler)));
                        });
            }

            @Override
            protected void onProperty(final HasComponents container, final PropertyLayoutData propertyData) {
                var interaction = PropertyInteraction.start(managedObject, propertyData.getId(), Where.OBJECT_FORMS);
                interaction.checkVisibility()
                        .getManagedProperty()
                        .ifPresent(managedProperty -> {
                            interaction.checkUsability();
                            var disablingModel = DisablingDecorationModel.of(interaction);
                            var propertyNegotiation = managedProperty.startNegotiation();
                            // persist edits: whenever a field writes a new value into the
                            // negotiation, submit it into the owning object (within the
                            // request's interaction/transaction).
                            ChangeListener<ManagedObject> submitListener = (observable, oldValue, newValue) ->
                                    propertyNegotiation.submit();
                            propertyNegotiation.getValue().addListener(submitListener);

                            Supplier<Component> editorSupplier = () -> uiComponentFactory.componentFor(
                                    new UiComponentFactory.ComponentRequest(
                                            propertyNegotiation, managedProperty, disablingModel));

                            // editable properties default to a compact read-only view (a
                            // value "chip" + a pencil icon), only building/swapping in the
                            // actual input widget once the user asks to edit — rather than
                            // every property looking like an always-open form field. A
                            // disabled property (no veto == editable) is left exactly as the
                            // factory rendered it (already shown in its own disabled/faded
                            // style). Blob/Clob's factory already manages its own view (a
                            // download link shown unconditionally, an Upload appended only
                            // when editable) — wrapping that in the toggle would hide the
                            // download link itself behind the "edit" pencil, so it's excluded.
                            // A property that is disabled (read-only) and holds a plain value gets
                            // the same chip as an editable one, minus the pencil, so every value
                            // on the page reads alike; the veto reason becomes its tooltip.
                            // Other disabled widgets (checkbox, markup, ...) stay as rendered.
                            Component added;
                            if (isBlobOrClob(managedProperty)) {
                                added = editorSupplier.get();
                            } else if (disablingModel.isEmpty()) {
                                added = newPropertyViewEditToggle(managedProperty, propertyNegotiation, editorSupplier);
                            } else if (isPlainValue(managedProperty)) {
                                added = newReadOnlyProperty(managedProperty, propertyNegotiation,
                                        disablingModel.get().reason());
                            } else {
                                added = editorSupplier.get();
                            }
                            Vaa.add(container, added);
                            added.addDetachListener(event ->
                                    propertyNegotiation.getValue().removeListener(submitListener));

                            // resolved live off the metamodel (not propertyData.getActions(),
                            // which is only populated from a hand-written layout.xml) so
                            // @ActionLayout(associateWith=...) actions show up even on an
                            // auto-generated fallback layout.
                            // the property's own actions belong to its cell: placed beside it they
                            // would become a separate form cell and drift away from their property
                            HasComponents actionHost = added instanceof Div cell
                                    && cell.hasClassName("vaa-property") ? cell : container;
                            var actionBar = newActionPanel(actionHost);
                            for (ObjectAction associatedAction : managedProperty.getAssociatedActions()) {
                                if (!fieldSetActionIds.contains(associatedAction.getId())) {
                                    onAction(actionBar, associatedAction);
                                }
                            }
                        });
            }

            private boolean isBlobOrClob(final ManagedProperty managedProperty) {
                var correspondingClass = managedProperty.getElementType().correspondingClass();
                return Blob.class.equals(correspondingClass) || Clob.class.equals(correspondingClass);
            }

            /**
             * Wraps an editable field: a compact read view (label + value "chip")
             * shown by default, with a pencil button that builds the real editor
             * widget on first click and toggles between the two on each further
             * click (rather than a one-way reveal that leaves the property stuck
             * open). The editor is built lazily — not at page-load time — so a
             * reference/choice property's ComboBox (which may eagerly fetch all
             * instances of the referenced type) isn't populated for a widget the
             * user never opens.
             */
            private Component newPropertyViewEditToggle(
                    final ManagedProperty managedProperty,
                    final PropertyNegotiationModel propertyNegotiation,
                    final Supplier<Component> editorSupplier) {

                var labelSpan = new Span(managedProperty.getFriendlyName());
                labelSpan.addClassName("vaa-property-label");

                var valueChip = newValueChip();
                Runnable refreshChip = () -> setChipText(valueChip, propertyNegotiation);
                refreshChip.run();
                ChangeListener<ManagedObject> chipListener = (observable, oldValue, newValue) -> refreshChip.run();
                propertyNegotiation.getValue().addListener(chipListener);

                // the pencil/done button lives in its own row alongside whichever of
                // (valueChip, editor) is current, and is never itself hidden — an
                // earlier version of this toggle hid the button along with the chip
                // when entering edit mode, leaving no way to switch back.
                var editIcon = Vaa.newIconButton(VaadinIcon.PENCIL, "Edit " + managedProperty.getFriendlyName());
                var contentRow = new Div(valueChip, editIcon);
                contentRow.addClassName("vaa-property-value");

                var wrapper = new Div(labelSpan, contentRow);
                wrapper.addClassName("vaa-property");

                // editor built on first click; toggled (not rebuilt) on every click after
                var editorHolder = new Component[1];
                var editing = new boolean[] { false };
                editIcon.addClickListener(event -> {
                    if (editorHolder[0] == null) {
                        editorHolder[0] = editorSupplier.get();
                    }
                    editing[0] = !editing[0];
                    contentRow.removeAll();
                    if (editing[0]) {
                        // the editor renders its own label, so ours would duplicate it
                        labelSpan.setVisible(false);
                        contentRow.add(editorHolder[0], editIcon);
                        editIcon.setIcon(VaadinIcon.CHECK.create());
                        editIcon.getElement().setAttribute("aria-label",
                                "Done editing " + managedProperty.getFriendlyName());
                    } else {
                        refreshChip.run();
                        labelSpan.setVisible(true);
                        contentRow.add(valueChip, editIcon);
                        editIcon.setIcon(VaadinIcon.PENCIL.create());
                        editIcon.getElement().setAttribute("aria-label",
                                "Edit " + managedProperty.getFriendlyName());
                    }
                });

                wrapper.addDetachListener(event -> propertyNegotiation.getValue().removeListener(chipListener));
                return wrapper;
            }

            /** Value types worth showing as a text chip (not checkbox, markup or blob/clob widgets). */
            private boolean isPlainValue(final ManagedProperty managedProperty) {
                var cls = managedProperty.getElementType().correspondingClass();
                return !isBlobOrClob(managedProperty)
                        && !Boolean.class.equals(cls) && !boolean.class.equals(cls)
                        && !org.apache.causeway.applib.value.Markup.class.equals(cls);
            }

            /**
             * A disabled property: label + the same value chip an editable one shows,
             * without the pencil. {@code reason} (why it is read-only) is the chip's tooltip.
             */
            private Component newReadOnlyProperty(
                    final ManagedProperty managedProperty,
                    final PropertyNegotiationModel propertyNegotiation,
                    final String reason) {
                var labelSpan = new Span(managedProperty.getFriendlyName());
                labelSpan.addClassName("vaa-property-label");

                var valueChip = newValueChip();
                setChipText(valueChip, propertyNegotiation);
                if (reason != null && !reason.isBlank()) {
                    valueChip.getElement().setAttribute("title", reason);
                }
                ChangeListener<ManagedObject> chipListener =
                        (observable, oldValue, newValue) -> setChipText(valueChip, propertyNegotiation);
                propertyNegotiation.getValue().addListener(chipListener);

                var contentRow = new Div(valueChip);
                contentRow.addClassName("vaa-property-value");
                var wrapper = new Div(labelSpan, contentRow);
                wrapper.addClassName("vaa-property");
                wrapper.addDetachListener(event -> propertyNegotiation.getValue().removeListener(chipListener));
                return wrapper;
            }

            private Span newValueChip() {
                var chip = new Span();
                chip.addClassName("vaa-chip");
                return chip;
            }

            private void setChipText(final Span chip, final PropertyNegotiationModel propertyNegotiation) {
                var text = Vaa.titleOfOrBlank(propertyNegotiation.getValue().getValue());
                chip.setText(text.isBlank() ? "(none)" : text);
            }

            @Override
            protected void onCollection(final HasComponents container, final CollectionLayoutData collectionData) {
                CollectionInteraction.start(managedObject, collectionData.getId(), Where.OBJECT_FORMS)
                        .checkVisibility()
                        .getManagedCollection()
                        .ifPresent(managedCollection -> {
                            // heading, actions and table form one section so the spacing between
                            // them is set once (theme: .vaa-section) rather than per component
                            var section = Vaa.add(container, new Div());
                            section.addClassName("vaa-section");
                            Vaa.add(section, new H3(managedCollection.getFriendlyName()));

                            // see onProperty: live lookup, not collectionData.getActions()
                            // (XML-only), so mixin/entity actions associated via
                            // @ActionLayout(associateWith=...) render without a layout.xml.
                            var actionBar = newActionPanel(section);
                            for (ObjectAction associatedAction : managedCollection.getAssociatedActions()) {
                                onAction(actionBar, associatedAction);
                            }

                            Vaa.add(section, TableViewVaa.forDataTableInteractive(
                                    managedCollection.createDataTableModel(),
                                    objectNavHandler));
                        });
            }
        };

        UiGridLayout.forObject(managedObject)
                .ifPresentOrElse(
                        uiGridLayout -> uiGridLayout.visit(gridVisitor),
                        () -> add(new H1(objectTitle)));
        // Capped and centred, not full-bleed: a single-column object (no custom
        // layout.xml, the common case) would otherwise hug the left edge with a
        // large unbalanced blank area on a wide viewport. 90em is comfortably wider
        // than this app's widest layout.xml (a 6/6 split), so multi-column pages
        // are unaffected — the cap only kicks in on viewports wider than that.
        setWidthFull();
        setMaxWidth("90em");
        getStyle().set("margin-inline", "auto");
        setPadding(false);
    }
}
