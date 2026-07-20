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
package org.apache.causeway.viewer.vaadin.demo;

import jakarta.inject.Inject;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;

import org.apache.causeway.applib.services.iactn.InteractionService;
import org.apache.causeway.applib.services.xactn.TransactionService;

/**
 * Seeds the in-memory database with sample authors and books on startup, so the
 * demo is populated out of the box. Runs inside an anonymous interaction and a
 * transaction (the persistence layer requires both).
 */
@Component
public class DemoSeeder implements ApplicationRunner {

    @Inject private InteractionService interactionService;
    @Inject private TransactionService transactionService;
    @Inject private DemoMenu demoMenu;

    @Override
    public void run(final ApplicationArguments args) {
        interactionService.runAnonymous(() ->
                transactionService.runTransactional(Propagation.REQUIRES_NEW, () -> {
                    if (demoMenu.listBooks().isEmpty()) {
                        demoMenu.populate();
                    }
                }));
    }
}
