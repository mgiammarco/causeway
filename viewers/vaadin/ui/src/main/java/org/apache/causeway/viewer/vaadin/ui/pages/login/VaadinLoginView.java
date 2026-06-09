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
package org.apache.causeway.viewer.vaadin.ui.pages.login;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.login.LoginForm;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

import org.apache.causeway.core.security.authentication.AuthenticationRequestPassword;
import org.apache.causeway.viewer.vaadin.ui.auth.VaadinAuthenticationHandler;

@Route("login")
public class VaadinLoginView extends VerticalLayout {

    private static final long serialVersionUID = 1L;

    public VaadinLoginView(final VaadinAuthenticationHandler authenticationHandler) {
        setSizeFull();
        setJustifyContentMode(JustifyContentMode.CENTER);
        setAlignItems(FlexComponent.Alignment.CENTER);

        var loginForm = new LoginForm();
        loginForm.setForgotPasswordButtonVisible(false);
        loginForm.addLoginListener(event -> {
            var request = new AuthenticationRequestPassword(
                    event.getUsername(), event.getPassword());
            if (authenticationHandler.loginToSession(request)) {
                UI.getCurrent().getPage().setLocation("/");
            } else {
                loginForm.setError(true);
            }
        });

        add(new H1("Apache Causeway"), loginForm);
    }
}
