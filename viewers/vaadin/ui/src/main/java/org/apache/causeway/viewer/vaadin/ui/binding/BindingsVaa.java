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
package org.apache.causeway.viewer.vaadin.ui.binding;

import java.util.Objects;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasValidation;
import com.vaadin.flow.component.HasValue;

import org.apache.causeway.commons.binding.Bindable;
import org.apache.causeway.commons.binding.ChangeListener;
import org.apache.causeway.commons.binding.Observable;
import org.apache.causeway.core.metamodel.interactions.managed.ManagedValue;

/**
 * Binds Causeway value models to Vaadin fields.
 * <p>
 * The core binding operates on the value's parsable-text representation, so
 * any value type with a parser can be edited through a text-ish field.
 */
public final class BindingsVaa {

    private BindingsVaa() {
    }

    /** Convenience overload binding a whole {@link ManagedValue}. */
    public static <F extends Component & HasValue<?, String>> void bindParsableText(
            final F uiField,
            final ManagedValue managedValue,
            final boolean readOnly) {
        bindParsableText(uiField,
                managedValue.getValueAsParsableText(),
                managedValue.getValidationMessage(),
                readOnly);
    }

    /**
     * Two-way binding between {@code uiField} and {@code parsableText},
     * plus validation feedback from {@code validationMessage}.
     */
    public static <F extends Component & HasValue<?, String>> void bindParsableText(
            final F uiField,
            final Bindable<String> parsableText,
            final Observable<String> validationMessage,
            final boolean readOnly) {

        uiField.setValue(Objects.toString(parsableText.getValue(), ""));

        if (readOnly) {
            uiField.setReadOnly(true);
            return;
        }

        // UI -> model
        uiField.addValueChangeListener(event -> parsableText.setValue(event.getValue()));

        // model -> UI (reentrancy guard: skip set when value is already current)
        ChangeListener<String> modelListener = (observable, oldValue, newValue) -> {
            var next = Objects.toString(newValue, "");
            if (!next.equals(uiField.getValue())) {
                uiField.setValue(next);
            }
        };
        parsableText.addListener(modelListener);

        // validation feedback + listener leak fix
        ChangeListener<String> validationListener;
        if (uiField instanceof HasValidation hasValidation) {
            validationListener = (observable, oldValue, newValue) -> {
                hasValidation.setErrorMessage(newValue);
                hasValidation.setInvalid(newValue != null && !newValue.isBlank());
            };
            validationMessage.addListener(validationListener);
        } else {
            validationListener = null;
        }

        // detach cleanup: remove model listeners when the field leaves the UI
        final ChangeListener<String> capturedValidationListener = validationListener;
        uiField.addDetachListener(event ->
                unbind(parsableText, validationMessage, modelListener, capturedValidationListener));
    }

    /**
     * Removes the given listeners from their respective observables.
     * Package-private for testing.
     *
     * @param parsableText        the model observable for the field value
     * @param validationMessage   the model observable for validation feedback
     * @param modelListener       listener registered on {@code parsableText}
     * @param validationListener  listener registered on {@code validationMessage}, or {@code null}
     */
    static void unbind(
            final Observable<String> parsableText,
            final Observable<String> validationMessage,
            final ChangeListener<String> modelListener,
            final ChangeListener<String> validationListener) {
        parsableText.removeListener(modelListener);
        if (validationListener != null) {
            validationMessage.removeListener(validationListener);
        }
    }
}
