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
        // model -> UI
        parsableText.addListener((observable, oldValue, newValue) ->
                uiField.setValue(Objects.toString(newValue, "")));

        if (uiField instanceof HasValidation hasValidation) {
            validationMessage.addListener((observable, oldValue, newValue) -> {
                hasValidation.setErrorMessage(newValue);
                hasValidation.setInvalid(newValue != null && !newValue.isBlank());
            });
        }
    }
}
