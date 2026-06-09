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

import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinServiceInitListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import org.apache.causeway.core.metamodel.context.MetaModelContext;
import org.apache.causeway.core.security.authentication.AuthenticationRequest;
import org.apache.causeway.viewer.vaadin.ui.pages.login.VaadinLoginView;

/**
 * Route guard: every navigation requires an authenticated session, otherwise
 * reroutes to the login view. Also offers the login entry point used by
 * {@link VaadinLoginView}.
 * <p>
 * Sole responsibility of the guard: reroute unauthenticated navigation to the
 * login view. The per-request Interaction lifecycle is owned by the servlet
 * wrapper, not by this guard.
 */
@Component
public class VaadinAuthenticationHandler
        implements AppShellConfigurator, VaadinServiceInitListener {

    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(VaadinAuthenticationHandler.class);

    private final transient MetaModelContext metaModelContext;

    public VaadinAuthenticationHandler(
            final MetaModelContext metaModelContext) {
        this.metaModelContext = metaModelContext;
    }

    @Override
    public void serviceInit(final ServiceInitEvent event) {
        event.getSource().addUIInitListener(uiEvent ->
                uiEvent.getUI().addBeforeEnterListener(this::beforeEnter));
    }

    /** @return whether authentication succeeded */
    public boolean loginToSession(final AuthenticationRequest authenticationRequest) {
        try {
            var authentication = metaModelContext.getAuthenticationManager()
                    .authenticate(authenticationRequest);
            if (authentication == null) {
                return false;
            }
            log.debug("logging in {}", authentication.getUser().name());
            AuthSessionStoreUtil.put(authentication);
            return true;
        } catch (Exception e) {
            log.error("authentication failed with an unexpected error", e);
            return false;
        }
    }

    private void beforeEnter(final BeforeEnterEvent event) {
        var authentication = AuthSessionStoreUtil.get().orElse(null);
        if (authentication != null) {
            return; // access granted
        }
        if (!VaadinLoginView.class.equals(event.getNavigationTarget())) {
            event.rerouteTo(VaadinLoginView.class);
        }
    }
}
