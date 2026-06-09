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
package org.apache.causeway.viewer.vaadin.ui.components.text;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.textfield.TextField;

import org.springframework.core.annotation.Order;

import org.apache.causeway.applib.annotation.PriorityPrecedence;
import org.apache.causeway.viewer.commons.model.components.UiComponentFactory.ComponentRequest;
import org.apache.causeway.viewer.vaadin.ui.binding.BindingsVaa;
import org.apache.causeway.viewer.vaadin.ui.components.UiComponentHandlerVaa;

/**
 * Renders {@code String} properties and parameters as a {@link TextField},
 * two-way bound via the value's parsable-text representation.
 */
@org.springframework.stereotype.Component
@Order(PriorityPrecedence.MIDPOINT)
public class TextFieldFactory implements UiComponentHandlerVaa {

    static boolean handles(final Class<?> featureType) {
        return String.class.equals(featureType);
    }

    @Override
    public boolean isHandling(final ComponentRequest request) {
        return handles(request.getFeatureType());
    }

    @Override
    public Component handle(final ComponentRequest request) {
        var uiField = new TextField(request.getFriendlyName());
        var readOnly = request.disablingUiModelIfAny().isPresent();
        BindingsVaa.bindParsableText(uiField, request.managedValue(), readOnly);
        return uiField;
    }
}
