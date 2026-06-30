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
package org.apache.causeway.viewer.vaadin.ui;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import org.apache.causeway.viewer.commons.services.CausewayModuleViewerCommonsServices;
import org.apache.causeway.viewer.vaadin.model.CausewayModuleViewerVaadinModel;
import org.apache.causeway.viewer.vaadin.ui.components.UiComponentFactoryVaa;
import org.apache.causeway.viewer.vaadin.ui.components.bool.BooleanFieldFactory;
import org.apache.causeway.viewer.vaadin.ui.components.choices.ChoiceFieldFactory;
import org.apache.causeway.viewer.vaadin.ui.components.other.FallbackFieldFactory;
import org.apache.causeway.viewer.vaadin.ui.components.reference.ObjectReferenceFieldFactory;
import org.apache.causeway.viewer.vaadin.ui.components.temporal.TemporalFieldFactory;
import org.apache.causeway.viewer.vaadin.ui.components.text.TextFieldFactory;
import org.apache.causeway.viewer.vaadin.ui.components.value.ValueFieldFactory;
import org.apache.causeway.viewer.vaadin.ui.auth.LogoutHandlerVaa;
import org.apache.causeway.viewer.vaadin.ui.auth.VaadinAuthenticationHandler;
import org.apache.causeway.viewer.vaadin.ui.pages.main.UiActionHandlerVaa;
import org.apache.causeway.viewer.vaadin.ui.pages.main.UiContextVaaDefault;

@Configuration
@Import({
        // modules
        CausewayModuleViewerVaadinModel.class,
        CausewayModuleViewerCommonsServices.class,

        // @Service & @Component beans of this module
        UiComponentFactoryVaa.class,
        FallbackFieldFactory.class,
        ChoiceFieldFactory.class,
        ObjectReferenceFieldFactory.class,
        TextFieldFactory.class,
        TemporalFieldFactory.class,
        BooleanFieldFactory.class,
        ValueFieldFactory.class,
        UiContextVaaDefault.class,
        UiActionHandlerVaa.class,
        VaadinAuthenticationHandler.class,
        LogoutHandlerVaa.class,
})
public class CausewayModuleViewerVaadinUi {
}
