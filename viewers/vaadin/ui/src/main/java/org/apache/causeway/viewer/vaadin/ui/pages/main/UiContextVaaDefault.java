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

import java.util.Objects;
import java.util.function.Consumer;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.spring.annotation.UIScope;

import org.springframework.stereotype.Service;

import org.apache.causeway.applib.services.iactn.InteractionService;
import org.apache.causeway.commons.collections.Can;
import org.apache.causeway.core.metamodel.interactions.managed.ManagedAction;
import org.apache.causeway.core.metamodel.object.ManagedObject;
import org.apache.causeway.viewer.vaadin.model.context.MemberInvocationHandler;
import org.apache.causeway.viewer.vaadin.model.context.UiContextVaa;

/**
 * Default {@link UiContextVaa}: renders domain artifacts through the
 * registered page factory and hands the result to the registered page handler.
 * <p>Scoped to the Vaadin UI instance so each browser tab/window gets its own
 * state; must be used only within a Vaadin UI context.</p>
 */
@UIScope
@Service
public class UiContextVaaDefault implements UiContextVaa {

    private final InteractionService interactionService;

    private Consumer<Component> newPageHandler;
    private MemberInvocationHandler<Component> pageFactory;

    public UiContextVaaDefault(final InteractionService interactionService) {
        this.interactionService = interactionService;
    }

    @Override
    public InteractionService getInteractionService() {
        return interactionService;
    }

    @Override
    public void setNewPageHandler(final Consumer<Component> newPageHandler) {
        this.newPageHandler = Objects.requireNonNull(newPageHandler, "newPageHandler");
    }

    @Override
    public void setPageFactory(final MemberInvocationHandler<Component> pageFactory) {
        this.pageFactory = Objects.requireNonNull(pageFactory, "pageFactory");
    }

    @Override
    public void route(final ManagedObject object) {
        ensureInitialized();
        newPageHandler.accept(pageFactory.handle(object));
    }

    @Override
    public void route(
            final ManagedAction managedAction,
            final Can<ManagedObject> params,
            final ManagedObject actionResult) {
        ensureInitialized();
        newPageHandler.accept(pageFactory.handle(managedAction, params, actionResult));
    }

    // -- HELPER

    private void ensureInitialized() {
        if (newPageHandler == null || pageFactory == null) {
            throw new IllegalStateException(
                    "UiContextVaa not initialized: the application shell must register "
                    + "newPageHandler and pageFactory before routing");
        }
    }
}
