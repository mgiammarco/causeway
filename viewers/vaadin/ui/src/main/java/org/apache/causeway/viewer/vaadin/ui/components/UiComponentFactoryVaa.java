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
package org.apache.causeway.viewer.vaadin.ui.components;

import java.util.List;

import com.vaadin.flow.component.Component;

import org.springframework.stereotype.Service;

import org.apache.causeway.commons.handler.ChainOfResponsibility;
import org.apache.causeway.commons.internal.exceptions._Exceptions;
import org.apache.causeway.viewer.commons.model.components.UiComponentFactory;
import org.apache.causeway.viewer.vaadin.model.util.Vaa;

/**
 * Creates Vaadin components for buttons, properties and parameters.
 * Field creation is delegated to a chain of {@link UiComponentHandlerVaa}
 * handlers, ordered by their Spring {@code @Order} precedence.
 */
@Service
public class UiComponentFactoryVaa implements UiComponentFactory<Component, Component> {

    private final ChainOfResponsibility<ComponentRequest, Component> chainOfHandlers;

    public UiComponentFactoryVaa(final List<UiComponentHandlerVaa> handlers) {
        this.chainOfHandlers = new ChainOfResponsibility<>("UiComponentFactoryVaa", handlers);
    }

    @Override
    public Component buttonFor(final ButtonRequest request) {
        var managedAction = request.managedAction();
        var uiButton = Vaa.newButton(managedAction.getFriendlyName());

        request.disablingUiModelIfAny().ifPresentOrElse(
                disabling -> uiButton.setEnabled(false),
                () -> uiButton.addClickListener(event ->
                        request.actionEventHandler().accept(managedAction)));
        return uiButton;
    }

    @Override
    public Component componentFor(final ComponentRequest request) {
        return chainOfHandlers.handleElseFail(request);
    }

    @Override
    public Component parameterFor(final ComponentRequest request) {
        return chainOfHandlers.handleElseFail(request);
    }

    @Override
    public LabelAndPosition<Component> labelFor(final ComponentRequest request) {
        throw _Exceptions.unsupportedOperation(
                "not needed for Vaadin: field components carry their own label");
    }
}
