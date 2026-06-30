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
package org.apache.causeway.viewer.vaadin.ui.components.choices;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.combobox.ComboBox;

import org.springframework.core.annotation.Order;

import org.apache.causeway.applib.annotation.PriorityPrecedence;
import org.apache.causeway.core.metamodel.object.ManagedObject;
import org.apache.causeway.core.metamodel.object.MmTitleUtils;
import org.apache.causeway.viewer.commons.model.components.UiComponentFactory.ComponentRequest;
import org.apache.causeway.viewer.vaadin.ui.components.UiComponentHandlerVaa;

/**
 * Renders any property or parameter that offers a (non-empty) set of choices —
 * enums, or references with a choices/autoComplete supporting method — as a
 * Vaadin {@link ComboBox} of the available {@link ManagedObject}s.
 * <p>
 * Ordered ahead of the text/value factories so a constrained value is a
 * dropdown rather than a free-text field.
 */
@org.springframework.stereotype.Component
@Order(PriorityPrecedence.EARLY)
public class ChoiceFieldFactory implements UiComponentHandlerVaa {

    @Override
    public boolean isHandling(final ComponentRequest request) {
        var choices = request.managedValue().getChoices().getValue();
        return choices != null && !choices.isEmpty();
    }

    @Override
    public Component handle(final ComponentRequest request) {
        var managedValue = request.managedValue();
        var choices = managedValue.getChoices().getValue();
        var readOnly = request.disablingUiModelIfAny().isPresent();

        var comboBox = new ComboBox<ManagedObject>(request.getFriendlyName());
        comboBox.setItems(choices.toList());
        comboBox.setItemLabelGenerator(MmTitleUtils::titleOf);
        comboBox.setValue(managedValue.getValue().getValue());
        comboBox.setReadOnly(readOnly);
        if (!readOnly) {
            comboBox.addValueChangeListener(event ->
                    managedValue.getValue().setValue(event.getValue()));
        }
        return comboBox;
    }
}
