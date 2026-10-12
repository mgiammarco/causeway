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
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.Lumo;
import com.vaadin.flow.theme.lumo.LumoUtility;

import org.apache.causeway.core.metamodel.object.MmTitleUtils;

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
import org.apache.causeway.viewer.vaadin.model.util.Vaa;
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

    /** Breadcrumb trail of recently-visited objects (most recent last). */
    private final HorizontalLayout breadcrumbBar = new HorizontalLayout();
    private static final int MAX_BREADCRUMBS = 8;

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
        uiContext.setObjectVisitedHandler(this::addBreadcrumb);
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
        title.addClassNames("vaa-brand", LumoUtility.FontSize.LARGE, LumoUtility.Margin.NONE);

        var navbar = header.navbar();
        var primaryMenu = MenuBuilderVaa.buildMenuBar(
                navbar.primary(), uiActionHandler::handleActionLinkClicked);
        // The primary menu takes the free width and may shrink to zero: a MenuBar folds
        // the items that no longer fit into its own overflow ("...") button, so on a phone
        // nothing is pushed off-screen. (Without min-width:0 a flex item refuses to shrink
        // below its content and the utility menus on the right became unreachable.)
        primaryMenu.getStyle().set("min-width", "0");

        var userProfile = header.userProfile();
        var accountLabel = userProfile.getUserProfileName() != null
                ? userProfile.getUserProfileName()
                : userProfile.userMemento().map(user -> user.name()).orElse("Account");
        var accountMenu = MenuBuilderVaa.buildAccountMenuBar(
                accountLabel, uiActionHandler::handleActionLinkClicked,
                navbar.secondary(), navbar.tertiary());
        var themeToggle = newThemeToggle();

        var headerBar = new HorizontalLayout(title, primaryMenu, accountMenu, themeToggle);
        headerBar.setAlignItems(FlexComponent.Alignment.CENTER);
        headerBar.setWidthFull();
        headerBar.expand(primaryMenu);
        headerBar.addClassNames(LumoUtility.Padding.Horizontal.MEDIUM);
        // setWidthFull() + Padding.* without this adds the padding on top of 100%
        // width instead of carving it out — the same recipe already proven to
        // overflow once in ObjectViewVaa's fieldset card.
        headerBar.getStyle().set("box-sizing", "border-box");
        addToNavbar(headerBar);

        breadcrumbBar.setSpacing(false);
        breadcrumbBar.setAlignItems(FlexComponent.Alignment.CENTER);
        breadcrumbBar.addClassNames(LumoUtility.Padding.Horizontal.MEDIUM, LumoUtility.Padding.Top.SMALL,
                LumoUtility.FlexWrap.WRAP);
        breadcrumbBar.setVisible(false); // shown only once there is an actual trail

        pageContent.setWidthFull();
        // "vaa-page" is the container the object-page grid measures to decide when
        // side-by-side columns stack (see the "causeway" theme)
        pageContent.addClassNames("vaa-page", LumoUtility.Padding.LARGE);
        pageContent.getStyle().set("box-sizing", "border-box");

        var contentWrap = new VerticalLayout(breadcrumbBar, pageContent);
        contentWrap.setPadding(false);
        contentWrap.setSpacing(false);
        contentWrap.setSizeFull();
        setContent(contentWrap);
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

    /**
     * Closest Vaadin/Lumo equivalent to Wicket's live Bootswatch theme-switcher
     * footer: Lumo ships exactly two built-in variants (light/dark, no separate
     * theme files to swap), toggled via the page's theme attribute list.
     *
     * <p>
     * The first render follows the browser's {@code prefers-color-scheme}, a manual
     * toggle is remembered in {@code localStorage} and wins from then on.
     * </p>
     */
    private Button newThemeToggle() {
        var button = Vaa.newIconButton(VaadinIcon.MOON_O, "Toggle dark theme");
        button.addClickListener(event -> {
            var dark = !UI.getCurrent().getElement().getThemeList().contains(Lumo.DARK);
            applyTheme(button, dark);
            UI.getCurrent().getPage().executeJs("try{localStorage.setItem('causeway-vaa-theme',$0)}catch(e){}",
                    dark ? "dark" : "light");
        });
        UI.getCurrent().getPage()
                .executeJs("try{return localStorage.getItem('causeway-vaa-theme')"
                        + "||(matchMedia('(prefers-color-scheme: dark)').matches?'dark':'light')}"
                        + "catch(e){return 'light'}")
                .then(String.class, preferred -> applyTheme(button, "dark".equals(preferred)));
        return button;
    }

    private static void applyTheme(final Button toggle, final boolean dark) {
        var themeList = UI.getCurrent().getElement().getThemeList();
        if (dark) {
            themeList.add(Lumo.DARK);
        } else {
            themeList.remove(Lumo.DARK);
        }
        toggle.setIcon((dark ? VaadinIcon.SUN_O : VaadinIcon.MOON_O).create());
    }

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

    /** Appends a clickable breadcrumb for a visited object (deduping the most recent). */
    private void addBreadcrumb(final ManagedObject object) {
        if (object == null || object.getPojo() == null) {
            return;
        }
        var title = MmTitleUtils.titleOf(object);
        var count = breadcrumbBar.getComponentCount();
        if (count > 0 && breadcrumbBar.getComponentAt(count - 1) instanceof Button last
                && title.equals(last.getText())) {
            return; // already the current crumb
        }
        if (count > 0) {
            var sep = new Span("›");
            sep.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.Padding.Horizontal.XSMALL);
            breadcrumbBar.add(sep);
        }
        var crumb = new Button(title, event -> uiContext.route(object));
        crumb.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE, ButtonVariant.LUMO_SMALL);
        breadcrumbBar.add(crumb);

        // cap the trail: each crumb is a Button plus a preceding separator (except the first)
        while (breadcrumbBar.getComponentCount() > (MAX_BREADCRUMBS * 2 - 1)) {
            breadcrumbBar.remove(breadcrumbBar.getComponentAt(0));
            if (breadcrumbBar.getComponentCount() > 0
                    && breadcrumbBar.getComponentAt(0) instanceof Span) {
                breadcrumbBar.remove(breadcrumbBar.getComponentAt(0));
            }
        }

        // show the trail only once it holds more than one object (otherwise it just
        // duplicates the page title)
        var crumbCount = (int) breadcrumbBar.getChildren().filter(c -> c instanceof Button).count();
        breadcrumbBar.setVisible(crumbCount > 1);
    }
}
