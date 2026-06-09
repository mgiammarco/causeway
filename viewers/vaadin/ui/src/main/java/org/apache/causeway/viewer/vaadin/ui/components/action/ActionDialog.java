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

import java.util.function.Predicate;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;

import org.apache.causeway.commons.collections.Can;
import org.apache.causeway.core.metamodel.interactions.managed.ManagedAction;
import org.apache.causeway.core.metamodel.object.ManagedObject;
import org.apache.causeway.viewer.vaadin.ui.components.UiComponentFactoryVaa;

/**
 * Modal dialog prompting for action parameters; OK hands the negotiated
 * parameter values to the supplied callback (which returns true to close).
 */
public class ActionDialog extends Dialog {

    private static final long serialVersionUID = 1L;

    public static ActionDialog forManagedAction(
            final UiComponentFactoryVaa uiComponentFactory,
            final ManagedAction managedAction,
            final Predicate<Can<ManagedObject>> submitHandler) {
        return new ActionDialog(uiComponentFactory, managedAction, submitHandler);
    }

    private ActionDialog(
            final UiComponentFactoryVaa uiComponentFactory,
            final ManagedAction managedAction,
            final Predicate<Can<ManagedObject>> submitHandler) {

        setHeaderTitle(managedAction.getFriendlyName());
        setModal(true);

        var actionForm = ActionForm.forManagedAction(uiComponentFactory, managedAction);
        add(actionForm);

        var okButton = new Button("OK", event -> {
            var paramValues = actionForm.getParameterNegotiation().getParamValues();
            if (submitHandler.test(paramValues)) {
                close();
            }
        });
        var cancelButton = new Button("Cancel", event -> close());
        getFooter().add(new HorizontalLayout(okButton, cancelButton));
    }
}
