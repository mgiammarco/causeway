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
package org.apache.causeway.viewer.vaadin.model.util;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasComponents;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;

import org.apache.causeway.core.metamodel.object.ManagedObject;
import org.apache.causeway.core.metamodel.object.ManagedObjects;
import org.apache.causeway.core.metamodel.object.MmTitleUtils;

/**
 * Small static helpers for fluent Vaadin component tree construction.
 */
public class Vaa {

    private Vaa() {
    }

    /** Adds {@code component} to {@code container} and returns it, to allow fluent chaining. */
    public static <T extends Component> T add(final HasComponents container, final T component) {
        container.add(component);
        return component;
    }

    public static Button newButton(final String label) {
        var button = new Button(label);
        button.addThemeVariants(ButtonVariant.LUMO_SMALL);
        return button;
    }

    /** A small tertiary icon-only button (edit pencils, theme toggle, breadcrumb crumbs, ...). */
    public static Button newIconButton(final VaadinIcon icon, final String ariaLabel) {
        return newIconButton(icon.create(), ariaLabel);
    }

    public static Button newIconButton(final Icon icon, final String ariaLabel) {
        var button = new Button(icon);
        button.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE, ButtonVariant.LUMO_SMALL);
        button.getElement().setAttribute("aria-label", ariaLabel);
        return button;
    }

    /**
     * {@link MmTitleUtils#titleOf(ManagedObject)}, but blank instead of the
     * internal diagnostic string (e.g. "empty java.lang.String") for a
     * null/unspecified/empty value — every rendering path that might show an
     * unset value should go through this rather than titleOf() directly.
     */
    public static String titleOfOrBlank(final ManagedObject managedObject) {
        return ManagedObjects.isNullOrUnspecifiedOrEmpty(managedObject)
                ? ""
                : MmTitleUtils.titleOf(managedObject);
    }
}
