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
package org.apache.causeway.viewer.vaadin.ui.components.bool;

import java.util.function.Consumer;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.checkbox.Checkbox;

import org.springframework.core.annotation.Order;

import org.apache.causeway.applib.annotation.PriorityPrecedence;
import org.apache.causeway.core.metamodel.object.ManagedObject;
import org.apache.causeway.viewer.commons.model.components.UiComponentFactory.ComponentRequest;
import org.apache.causeway.viewer.vaadin.ui.components.UiComponentHandlerVaa;

/**
 * Renders {@code Boolean} / {@code boolean} properties and parameters as a
 * Vaadin {@link Checkbox} (one-way write-back; the page is re-rendered on edit).
 */
@org.springframework.stereotype.Component
@Order(PriorityPrecedence.MIDPOINT)
public class BooleanFieldFactory implements UiComponentHandlerVaa {

    static boolean handles(final Class<?> featureType) {
        return Boolean.class.equals(featureType) || boolean.class.equals(featureType);
    }

    @Override
    public boolean isHandling(final ComponentRequest request) {
        return handles(request.getFeatureType());
    }

    @Override
    public Component handle(final ComponentRequest request) {
        var readOnly = request.disablingUiModelIfAny().isPresent();
        return createCheckbox(request.getFriendlyName(), currentPojo(request), readOnly,
                newValue -> writeBack(request, newValue));
    }

    static Checkbox createCheckbox(
            final String label,
            final Object currentPojo,
            final boolean readOnly,
            final Consumer<Boolean> writeBack) {
        var checkbox = new Checkbox(label);
        checkbox.setValue(Boolean.TRUE.equals(currentPojo));
        checkbox.setReadOnly(readOnly);
        if (!readOnly) {
            checkbox.addValueChangeListener(event -> writeBack.accept(event.getValue()));
        }
        return checkbox;
    }

    private static Object currentPojo(final ComponentRequest request) {
        var managedObject = request.managedValue().getValue().getValue();
        return managedObject != null ? managedObject.getPojo() : null;
    }

    private static void writeBack(final ComponentRequest request, final Boolean newPojo) {
        request.managedValue().getValue().setValue(
                ManagedObject.adaptSingular(request.getFeatureTypeSpec(), newPojo));
    }
}
