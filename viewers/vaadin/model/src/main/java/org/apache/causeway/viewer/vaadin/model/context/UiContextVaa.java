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
package org.apache.causeway.viewer.vaadin.model.context;

import java.util.function.Consumer;

import com.vaadin.flow.component.Component;

import org.apache.causeway.applib.services.iactn.InteractionService;
import org.apache.causeway.commons.collections.Can;
import org.apache.causeway.core.metamodel.interactions.managed.ManagedAction;
import org.apache.causeway.core.metamodel.object.ManagedObject;

/**
 * Mediates between domain-level events (navigate to object, show action result)
 * and the application shell that swaps page content.
 */
public interface UiContextVaa {

    InteractionService getInteractionService();

    /** Registered by the application shell: receives the new page content. */
    void setNewPageHandler(Consumer<Component> newPageHandler);

    /** Registered by the application shell: knows how to render domain artifacts. */
    void setPageFactory(MemberInvocationHandler<Component> pageFactory);

    /** Renders {@code object} and hands it to the page handler. */
    void route(ManagedObject object);

    /** Renders an action result and hands it to the page handler. */
    void route(ManagedAction managedAction, Can<ManagedObject> params, ManagedObject actionResult);
}
