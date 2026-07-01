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

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.causeway.commons.collections.Can;
import org.apache.causeway.core.metamodel.context.MetaModelContext;
import org.apache.causeway.core.metamodel.interactions.managed.ManagedAction;
import org.apache.causeway.core.metamodel.object.ManagedObject;
import org.apache.causeway.core.metamodel.object.ManagedObjects;
import org.apache.causeway.core.metamodel.tabular.DataTableInteractive;
import org.apache.causeway.viewer.commons.applib.services.header.HeaderUiService;
import org.apache.causeway.viewer.vaadin.model.context.MemberInvocationHandler;
import org.apache.causeway.viewer.vaadin.model.context.UiContextVaa;
import org.apache.causeway.viewer.vaadin.ui.components.UiComponentFactoryVaa;
import org.apache.causeway.viewer.vaadin.ui.components.collection.TableViewVaa;
import org.apache.causeway.viewer.vaadin.ui.components.object.ObjectViewVaa;
import org.apache.causeway.viewer.vaadin.ui.components.result.ValueResultViewVaa;

/**
 * Application shell: navbar with the metamodel-driven menu, swappable page
 * content.  Implements {@link MemberInvocationHandler} so routing can render
 * objects and action results without knowing any view class.
 *
 * <p>
 * Discovered by Vaadin's route scanner via {@code @Route("") } — not wired
 * into the module {@code @Import} list.
 * </p>
 */
@Route("")
public class MainViewVaa extends AppLayout
        implements BeforeEnterObserver, MemberInvocationHandler<Component> {

    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(MainViewVaa.class);

    private final transient MetaModelContext metaModelContext;
    private final transient UiContextVaa uiContext;
    private final transient UiActionHandlerVaa uiActionHandler;
    private final transient UiComponentFactoryVaa uiComponentFactory;
    private final transient HeaderUiService headerUiService;

    /** Swappable page area that holds whichever view is currently displayed. */
    private final Div pageContent = new Div();

    /**
     * Guards against rebuilding the navbar on every navigation event.
     * {@link BeforeEnterEvent} fires each time the user navigates to this route,
     * but the navbar must be populated only once per shell instance.
     */
    private boolean initialized = false;

    public MainViewVaa(
            final MetaModelContext metaModelContext,
            final UiContextVaa uiContext,
            final UiActionHandlerVaa uiActionHandler,
            final UiComponentFactoryVaa uiComponentFactory,
            final HeaderUiService headerUiService) {
        this.metaModelContext = metaModelContext;
        this.uiContext = uiContext;
        this.uiActionHandler = uiActionHandler;
        this.uiComponentFactory = uiComponentFactory;
        this.headerUiService = headerUiService;

        uiContext.setNewPageHandler(this::replaceContent);
        uiContext.setPageFactory(this);
    }

    // -- BeforeEnterObserver

    @Override
    public void beforeEnter(final BeforeEnterEvent event) {
        if (initialized) {
            return;
        }
        initialized = true;

        setPrimarySection(Section.NAVBAR);

        var header = headerUiService.getHeader();
        var appName = header.branding() != null
                ? header.branding().getName().orElse("Apache Causeway")
                : "Apache Causeway";
        var title = new H1(appName);
        title.addClassNames(LumoUtility.FontSize.LARGE, LumoUtility.Margin.NONE,
                LumoUtility.Whitespace.NOWRAP);

        var navbar = header.navbar();
        var headerBar = new HorizontalLayout(
                title,
                MenuBuilderVaa.buildMenuBar(navbar.primary(), uiActionHandler::handleActionLinkClicked),
                MenuBuilderVaa.buildMenuBar(navbar.secondary(), uiActionHandler::handleActionLinkClicked),
                MenuBuilderVaa.buildMenuBar(navbar.tertiary(), uiActionHandler::handleActionLinkClicked));
        headerBar.setAlignItems(FlexComponent.Alignment.CENTER);
        headerBar.setWidthFull();
        headerBar.setSpacing(true);
        headerBar.addClassNames(LumoUtility.Padding.Horizontal.MEDIUM);
        addToNavbar(headerBar);

        pageContent.setWidthFull();
        pageContent.addClassNames(LumoUtility.Padding.LARGE);
        setContent(pageContent);
        renderHomepage();
    }

    // -- MemberInvocationHandler

    @Override
    public Component handle(final ManagedObject object) {
        return ObjectViewVaa.fromObject(
                uiComponentFactory, uiActionHandler::handleActionLinkClicked,
                uiContext::route, object);
    }

    @Override
    public Component handle(
            final ManagedAction managedAction,
            final Can<ManagedObject> params,
            final ManagedObject actionResult) {
        // a collection-valued action result is a *packed* ManagedObject; its
        // objSpec() reports the element type (not plural), so detect packing
        // explicitly rather than via objSpec().isPlural().
        if (ManagedObjects.isPacked(actionResult)) {
            return TableViewVaa.forDataTableInteractive(
                    DataTableInteractive.forAction(managedAction, actionResult),
                    uiContext::route);
        }
        // a value result (Blob/Clob/Markup/other scalar) is not a domain object,
        // so render it as a value (download / html / text) rather than an object page.
        if (actionResult.objSpec().isValue()) {
            return ValueResultViewVaa.forValue(actionResult);
        }
        return handle(actionResult);
    }

    // -- helpers

    private void replaceContent(final Component component) {
        pageContent.removeAll();
        pageContent.add(component);
    }

    private void renderHomepage() {
        var homepage = metaModelContext.getHomePageAdapter();
        if (homepage != null && homepage.getPojo() != null) {
            uiContext.route(homepage);
        } else {
            log.debug("no home-page adapter configured — page area left empty");
        }
    }
}
