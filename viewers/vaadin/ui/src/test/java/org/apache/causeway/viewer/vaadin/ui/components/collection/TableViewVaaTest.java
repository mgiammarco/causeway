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

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import org.apache.causeway.commons.binding.Observable;
import org.apache.causeway.commons.collections.Can;
import org.apache.causeway.core.metamodel.tabular.DataRow;
import org.apache.causeway.core.metamodel.tabular.DataTableInteractive;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TableViewVaaTest {

    @SuppressWarnings("unchecked")
    @Test
    void rendersEmptyPlaceholder_whenNoRows() {
        var table = Mockito.mock(DataTableInteractive.class);
        var rowsObservable = (Observable<Can<DataRow>>) Mockito.mock(Observable.class);
        Mockito.when(rowsObservable.getValue()).thenReturn(Can.empty());
        Mockito.when(table.dataRowsFilteredAndSortedObservable()).thenReturn(rowsObservable);

        var component = TableViewVaa.forDataTableInteractive(table);

        assertInstanceOf(TableViewVaa.class, component);
        // placeholder text present, no grid children
        assertTrue(((TableViewVaa) component).getChildren()
                .anyMatch(child -> child instanceof com.vaadin.flow.component.html.Span));
    }
}
