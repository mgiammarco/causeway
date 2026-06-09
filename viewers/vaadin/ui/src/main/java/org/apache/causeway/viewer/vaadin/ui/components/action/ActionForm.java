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
package org.apache.causeway.viewer.vaadin.ui.components.action;

import com.vaadin.flow.component.formlayout.FormLayout;

import org.apache.causeway.core.metamodel.interactions.managed.ManagedAction;
import org.apache.causeway.core.metamodel.interactions.managed.ParameterNegotiationModel;
import org.apache.causeway.viewer.commons.model.components.UiComponentFactory;
import org.apache.causeway.viewer.vaadin.ui.components.UiComponentFactoryVaa;

/**
 * Form with one input field per action parameter, built through the
 * component-factory chain and bound to the parameter negotiation model.
 */
public class ActionForm extends FormLayout {

    private static final long serialVersionUID = 1L;

    private final transient ParameterNegotiationModel parameterNegotiation;

    public static ActionForm forManagedAction(
            final UiComponentFactoryVaa uiComponentFactory,
            final ManagedAction managedAction) {
        return new ActionForm(uiComponentFactory, managedAction);
    }

    private ActionForm(
            final UiComponentFactoryVaa uiComponentFactory,
            final ManagedAction managedAction) {

        this.parameterNegotiation = managedAction.startParameterNegotiation();

        parameterNegotiation.getParamModels().forEach(managedParameter ->
                add(uiComponentFactory.parameterFor(
                        UiComponentFactory.ComponentRequest.of(managedParameter))));

        setResponsiveSteps(new ResponsiveStep("0", 1));
    }

    public ParameterNegotiationModel getParameterNegotiation() {
        return parameterNegotiation;
    }
}
