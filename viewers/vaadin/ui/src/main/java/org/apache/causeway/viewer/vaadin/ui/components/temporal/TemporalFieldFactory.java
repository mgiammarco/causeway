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
package org.apache.causeway.viewer.vaadin.ui.components.temporal;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.function.Consumer;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.datetimepicker.DateTimePicker;

import org.springframework.core.annotation.Order;

import org.apache.causeway.applib.annotation.PriorityPrecedence;
import org.apache.causeway.core.metamodel.object.ManagedObject;
import org.apache.causeway.viewer.commons.model.components.UiComponentFactory.ComponentRequest;
import org.apache.causeway.viewer.vaadin.ui.components.UiComponentHandlerVaa;

/**
 * Renders {@code LocalDate} / {@code LocalDateTime} properties and parameters
 * as {@link DatePicker} / {@link DateTimePicker}.
 * <p>
 * One-way write-back binding: the field initializes from the current value and
 * writes edits back to the model; no model-to-UI listener is registered (the
 * page is re-rendered after edits), so no detach cleanup is required.
 */
@org.springframework.stereotype.Component
@Order(PriorityPrecedence.MIDPOINT)
public class TemporalFieldFactory implements UiComponentHandlerVaa {

    static boolean handles(final Class<?> featureType) {
        return LocalDate.class.equals(featureType)
                || LocalDateTime.class.equals(featureType);
    }

    @Override
    public boolean isHandling(final ComponentRequest request) {
        return handles(request.getFeatureType());
    }

    @Override
    public Component handle(final ComponentRequest request) {
        var label = request.getFriendlyName();
        var currentPojo = currentPojo(request);
        var readOnly = request.disablingUiModelIfAny().isPresent();

        return LocalDate.class.equals(request.getFeatureType())
                ? createDateField(label, currentPojo, readOnly,
                        newValue -> writeBack(request, newValue))
                : createDateTimeField(label, currentPojo, readOnly,
                        newValue -> writeBack(request, newValue));
    }

    static DatePicker createDateField(
            final String label,
            final Object currentPojo,
            final boolean readOnly,
            final Consumer<LocalDate> writeBack) {

        var picker = new DatePicker(label);
        picker.setValue((LocalDate) currentPojo);
        picker.setReadOnly(readOnly);
        if (!readOnly) {
            picker.addValueChangeListener(event -> writeBack.accept(event.getValue()));
        }
        return picker;
    }

    static DateTimePicker createDateTimeField(
            final String label,
            final Object currentPojo,
            final boolean readOnly,
            final Consumer<LocalDateTime> writeBack) {

        var picker = new DateTimePicker(label);
        picker.setValue((LocalDateTime) currentPojo);
        picker.setReadOnly(readOnly);
        if (!readOnly) {
            picker.addValueChangeListener(event -> writeBack.accept(event.getValue()));
        }
        return picker;
    }

    private static Object currentPojo(final ComponentRequest request) {
        var managedObject = request.managedValue().getValue().getValue();
        return managedObject != null ? managedObject.getPojo() : null;
    }

    private static void writeBack(final ComponentRequest request, final Object newPojo) {
        request.managedValue().getValue().setValue(
                ManagedObject.adaptSingular(request.getFeatureTypeSpec(), newPojo));
    }
}
