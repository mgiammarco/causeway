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
package org.apache.causeway.viewer.vaadin.viewer;

import java.io.IOException;

import com.vaadin.flow.spring.SpringServlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import org.apache.causeway.applib.services.iactn.InteractionService;
import org.apache.causeway.viewer.vaadin.ui.auth.AuthSessionStoreUtil;

/**
 * Vaadin servlet that wraps each authenticated request in a Causeway
 * {@code Interaction}, so domain code on the request thread always sees the
 * session's {@code InteractionContext}. Unauthenticated requests pass through;
 * the UI-level route guard reroutes them to the login view.
 */
public class CausewayServletForVaadin extends SpringServlet {

    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(CausewayServletForVaadin.class);

    private final transient InteractionService interactionService;

    public CausewayServletForVaadin(
            final InteractionService interactionService,
            final ApplicationContext context,
            final boolean rootMapping) {
        super(context, rootMapping);
        this.interactionService = interactionService;
    }

    @Override
    protected void service(
            final HttpServletRequest request,
            final HttpServletResponse response) throws ServletException, IOException {

        var authentication = AuthSessionStoreUtil.get(request.getSession(false)).orElse(null);
        log.debug("incoming request (authenticated={})", authentication != null);

        if (authentication != null) {
            interactionService.run(authentication, () -> super.service(request, response));
        } else {
            // unauthenticated: let the request through; VaadinAuthenticationHandler
            // reroutes UI navigation to the login view
            super.service(request, response);
        }
    }
}
