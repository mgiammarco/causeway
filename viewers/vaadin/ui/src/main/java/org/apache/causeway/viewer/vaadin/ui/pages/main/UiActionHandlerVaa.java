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
package org.apache.causeway.viewer.vaadin.ui.pages.main;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.spring.annotation.UIScope;

import org.springframework.stereotype.Service;

import org.apache.causeway.commons.collections.Can;
import org.apache.causeway.core.metamodel.interactions.managed.ManagedAction;
import org.apache.causeway.core.metamodel.object.ManagedObject;
import org.apache.causeway.core.metamodel.object.ManagedObjects;
import org.apache.causeway.viewer.vaadin.model.context.UiContextVaa;
import org.apache.causeway.viewer.vaadin.ui.components.UiComponentFactoryVaa;
import org.apache.causeway.viewer.vaadin.ui.components.action.ActionDialog;

/**
 * Reacts to action-link clicks: zero-parameter actions are invoked directly,
 * otherwise an {@link ActionDialog} collects parameters first. The action
 * result is routed to the current page via {@link UiContextVaa}.
 */
@UIScope
@Service
public class UiActionHandlerVaa {

    private static final Logger log = LoggerFactory.getLogger(UiActionHandlerVaa.class);

    private final UiContextVaa uiContext;
    private final UiComponentFactoryVaa uiComponentFactory;

    public UiActionHandlerVaa(
            final UiContextVaa uiContext,
            final UiComponentFactoryVaa uiComponentFactory) {
        this.uiContext = uiContext;
        this.uiComponentFactory = uiComponentFactory;
    }

    public void handleActionLinkClicked(final ManagedAction managedAction) {
        if (managedAction.getAction().getParameterCount() == 0) {
            invoke(managedAction, Can.empty());
            return;
        }
        ActionDialog.forManagedAction(uiComponentFactory, managedAction, params -> {
            invoke(managedAction, params);
            return true;
        }).open();
    }

    private void invoke(final ManagedAction managedAction, final Can<ManagedObject> params) {
        managedAction.invoke(params)
                .ifSuccess(actionResult -> {
                    if (ManagedObjects.isNullOrUnspecifiedOrEmpty(actionResult)) {
                        // void or empty result: no page to show, just acknowledge.
                        Notification.show(managedAction.getFriendlyName() + " – done");
                    } else {
                        uiContext.route(managedAction, params, actionResult);
                    }
                })
                .ifFailure(veto ->
                        log.warn("action {} vetoed: {}", managedAction.getIdentifier(), veto));
    }
}
