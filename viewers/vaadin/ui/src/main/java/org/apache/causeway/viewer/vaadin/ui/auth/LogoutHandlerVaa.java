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
package org.apache.causeway.viewer.vaadin.ui.auth;

import com.vaadin.flow.component.UI;

import org.springframework.stereotype.Service;

import org.apache.causeway.core.security.authentication.logout.LogoutHandler;

/**
 * Invalidates this viewer's session-stored authentication when the framework
 * (e.g. the {@code logout} mixin) triggers a logout, then redirects the current
 * UI to the app root so the route guard reroutes to the login view.
 */
@Service
public class LogoutHandlerVaa implements LogoutHandler {

    @Override
    public void logout() {
        AuthSessionStoreUtil.clear();
        // if invoked on a Vaadin UI thread (the usual case, via the logout menu),
        // force a full-page redirect so the now-anonymous next request is rerouted
        // to the login view.
        var ui = UI.getCurrent();
        if (ui != null) {
            ui.getPage().setLocation("/");
        }
    }
}
