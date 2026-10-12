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
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.login.LoginForm;
import com.vaadin.flow.component.login.LoginI18n;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;

import org.apache.causeway.core.security.authentication.AuthenticationRequestPassword;
import org.apache.causeway.viewer.vaadin.ui.auth.VaadinAuthenticationHandler;

@Route("login")
public class VaadinLoginView extends VerticalLayout {

    private static final long serialVersionUID = 1L;

    public VaadinLoginView(final VaadinAuthenticationHandler authenticationHandler) {
        setSizeFull();
        setJustifyContentMode(JustifyContentMode.CENTER);
        setAlignItems(FlexComponent.Alignment.CENTER);

        var loginForm = new LoginForm(loginTexts());
        loginForm.setForgotPasswordButtonVisible(false);
        loginForm.addLoginListener(event -> {
            var request = new AuthenticationRequestPassword(
                    event.getUsername(), event.getPassword());
            if (authenticationHandler.loginToSession(request)) {
                // full-page redirect to the app root: this starts a NEW request that the
                // servlet wraps in an interaction bound to the just-stored session auth,
                // so the main view renders as the logged-in user (a client-side navigate()
                // would stay in the current, still-anonymous interaction).
                UI.getCurrent().getPage().setLocation("/");
            } else {
                loginForm.setError(true);
            }
        });

        // brand above, form on an elevated card: one clear focal point on the tinted canvas
        var brand = new H1("Apache Causeway");
        brand.addClassNames(LumoUtility.Margin.NONE, LumoUtility.FontSize.XXLARGE, LumoUtility.TextColor.PRIMARY);
        var tagline = new Span("Sign in to continue");
        tagline.addClassNames(LumoUtility.TextColor.SECONDARY);

        var card = new Div(loginForm);
        card.addClassNames(LumoUtility.Background.BASE, LumoUtility.BorderRadius.LARGE,
                LumoUtility.BoxShadow.MEDIUM, LumoUtility.Padding.SMALL);
        card.setWidth("22em");
        card.setMaxWidth("100%");

        setSpacing(true);
        add(brand, tagline, card);
    }

    private static LoginI18n loginTexts() {
        var i18n = LoginI18n.createDefault();
        i18n.getForm().setTitle("Log in");
        i18n.getErrorMessage().setTitle("Incorrect username or password");
        i18n.getErrorMessage().setMessage("Check that you have entered the correct username and password and try again.");
        return i18n;
    }
}
