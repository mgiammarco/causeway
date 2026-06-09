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

import java.util.Optional;

import com.vaadin.flow.server.VaadinSession;

import jakarta.servlet.http.HttpSession;

import org.apache.causeway.applib.services.iactn.InteractionContext;

/**
 * Stores the authenticated {@link InteractionContext} in the HTTP session,
 * readable both from Vaadin UI code ({@link VaadinSession}) and from the
 * servlet layer ({@link HttpSession}).
 */
public final class AuthSessionStoreUtil {

    private static final String ATTRIBUTE = InteractionContext.class.getName();

    private AuthSessionStoreUtil() {
    }

    public static void put(final HttpSession session, final InteractionContext authentication) {
        session.setAttribute(ATTRIBUTE, authentication);
    }

    public static Optional<InteractionContext> get(final HttpSession session) {
        return Optional.ofNullable(session)
                .map(s -> (InteractionContext) s.getAttribute(ATTRIBUTE));
    }

    /** Variant for Vaadin UI threads. */
    public static void put(final InteractionContext authentication) {
        VaadinSession.getCurrent().getSession().setAttribute(ATTRIBUTE, authentication);
    }

    /** Variant for Vaadin UI threads. */
    public static Optional<InteractionContext> get() {
        return Optional.ofNullable(VaadinSession.getCurrent())
                .map(VaadinSession::getSession)
                .map(s -> (InteractionContext) s.getAttribute(ATTRIBUTE));
    }

    public static void clear() {
        Optional.ofNullable(VaadinSession.getCurrent())
                .map(VaadinSession::getSession)
                .ifPresent(s -> s.setAttribute(ATTRIBUTE, null));
    }
}
