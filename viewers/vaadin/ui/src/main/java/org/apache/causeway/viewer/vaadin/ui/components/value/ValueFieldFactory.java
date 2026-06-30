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
package org.apache.causeway.viewer.vaadin.ui.components.value;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.textfield.TextField;

import org.springframework.core.annotation.Order;

import org.apache.causeway.applib.annotation.PriorityPrecedence;
import org.apache.causeway.viewer.commons.model.components.UiComponentFactory.ComponentRequest;
import org.apache.causeway.viewer.vaadin.ui.binding.BindingsVaa;
import org.apache.causeway.viewer.vaadin.ui.components.UiComponentHandlerVaa;

/**
 * Catch-all for scalar value types not handled by a more specific factory
 * (numbers, BigDecimal/BigInteger, char, UUID, enums, ...): renders an editable
 * {@link TextField} bound to the value's parsable-text representation.
 * <p>
 * Ordered late so the specialized factories (text, temporal, boolean) win first;
 * non-value features (object references) fall through to the fallback handler.
 */
@org.springframework.stereotype.Component
@Order(PriorityPrecedence.LATE)
public class ValueFieldFactory implements UiComponentHandlerVaa {

    @Override
    public boolean isHandling(final ComponentRequest request) {
        return request.getFeatureTypeSpec().isValue();
    }

    @Override
    public Component handle(final ComponentRequest request) {
        var uiField = new TextField(request.getFriendlyName());
        var readOnly = request.disablingUiModelIfAny().isPresent();
        BindingsVaa.bindParsableText(uiField, request.managedValue(), readOnly);
        return uiField;
    }
}
