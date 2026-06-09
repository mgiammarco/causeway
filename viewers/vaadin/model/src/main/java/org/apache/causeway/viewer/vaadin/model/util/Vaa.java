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
}
