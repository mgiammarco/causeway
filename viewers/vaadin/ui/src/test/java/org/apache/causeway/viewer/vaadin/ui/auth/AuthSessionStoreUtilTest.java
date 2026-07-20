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

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import jakarta.servlet.http.HttpSession;

import org.apache.causeway.applib.services.iactn.InteractionContext;
import org.apache.causeway.applib.services.user.UserMemento;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthSessionStoreUtilTest {

    @Test
    void roundTripsInteractionContextOnHttpSession() {
        var session = Mockito.mock(HttpSession.class);
        var auth = InteractionContext.ofUserWithSystemDefaults(UserMemento.ofName("tester"));
        var store = new Object() { Object value; };

        Mockito.doAnswer(invocation -> { store.value = invocation.getArgument(1); return null; })
                .when(session).setAttribute(Mockito.anyString(), Mockito.any());
        Mockito.when(session.getAttribute(Mockito.anyString()))
                .thenAnswer(invocation -> store.value);

        AuthSessionStoreUtil.put(session, auth);
        var roundTripped = AuthSessionStoreUtil.get(session);

        assertTrue(roundTripped.isPresent());
        assertEquals(auth, roundTripped.get());
    }
}
