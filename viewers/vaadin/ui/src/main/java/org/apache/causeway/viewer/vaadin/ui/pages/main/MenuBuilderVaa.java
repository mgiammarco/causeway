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

import java.util.function.Consumer;

import com.vaadin.flow.component.contextmenu.SubMenu;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.menubar.MenuBar;

import org.apache.causeway.core.metamodel.interactions.managed.ManagedAction;
import org.apache.causeway.viewer.commons.applib.services.menu.MenuVisitor;
import org.apache.causeway.viewer.commons.applib.services.menu.model.MenuAction;
import org.apache.causeway.viewer.commons.applib.services.menu.model.MenuDropdown;
import org.apache.causeway.viewer.commons.applib.services.menu.model.NavbarSection;

/**
 * Translates the viewer-agnostic menu model (visited depth-first via
 * {@link MenuVisitor}) into a Vaadin {@link MenuBar}.
 *
 * <p>
 * Not a Spring bean — instantiated only by {@link #buildMenuBar(NavbarSection, Consumer)}.
 * </p>
 */
public class MenuBuilderVaa implements MenuVisitor {

    /**
     * Builds a {@link MenuBar} from the given {@link NavbarSection} by
     * walking its items depth-first and rendering each node.
     *
     * @param navbarSection the section to render
     * @param actionEventHandler called when a menu action is clicked
     * @return a fully populated {@link MenuBar}
     */
    public static MenuBar buildMenuBar(
            final NavbarSection navbarSection,
            final Consumer<ManagedAction> actionEventHandler) {
        var menuBar = new MenuBar();
        navbarSection.visitMenuItems(new MenuBuilderVaa(menuBar, actionEventHandler));
        return menuBar;
    }

    // -- constructor / fields

    private final MenuBar menuBar;
    private final Consumer<ManagedAction> actionEventHandler;

    /**
     * Tracks the sub-menu opened by the most recent {@link #onTopLevel(MenuDropdown)} call.
     * NavbarSection guarantees that {@code onTopLevel} is always called before any
     * {@code onMenuAction}/{@code onSectionSpacer}/{@code onSectionLabel} call.
     */
    private SubMenu currentSubMenu;

    private MenuBuilderVaa(
            final MenuBar menuBar,
            final Consumer<ManagedAction> actionEventHandler) {
        this.menuBar = menuBar;
        this.actionEventHandler = actionEventHandler;
    }

    // -- MenuVisitor

    @Override
    public void onTopLevel(final MenuDropdown menuDropdown) {
        currentSubMenu = menuBar.addItem(menuDropdown.name()).getSubMenu();
    }

    @Override
    public void onMenuAction(final MenuAction menuAction) {
        currentSubMenu.addItem(menuAction.name(), event ->
                menuAction.managedAction().ifPresent(actionEventHandler));
    }

    @Override
    public void onSectionSpacer() {
        currentSubMenu.add(new Hr());
    }

    @Override
    public void onSectionLabel(final String named) {
        var label = new Span(named);
        label.getStyle().set("font-weight", "bold");
        currentSubMenu.add(label);
    }
}
