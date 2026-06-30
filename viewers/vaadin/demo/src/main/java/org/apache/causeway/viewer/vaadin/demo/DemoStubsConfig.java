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

import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;

import org.apache.causeway.applib.services.metrics.MetricsService;
import org.apache.causeway.applib.services.repository.RepositoryService;
import org.apache.causeway.core.metamodel.services.deadlock.DeadlockRecognizer;

/**
 * Supplies the two services normally contributed by a persistence module.
 * This demo is persistence-free (view-models only), so they are stubbed:
 * <ul>
 *   <li>{@link DeadlockRecognizer} — never recognises a deadlock;</li>
 *   <li>{@link RepositoryService} — a Mockito stub returning empty results; the
 *       view-model flows of this demo never query a repository, so it is only
 *       needed to satisfy injection at startup.</li>
 * </ul>
 */
@Configuration
public class DemoStubsConfig {

    @Bean
    public DeadlockRecognizer deadlockRecognizer() {
        return e -> false;
    }

    @Bean
    public RepositoryService repositoryService() {
        return Mockito.mock(RepositoryService.class);
    }

    @Bean
    public MetricsService metricsService() {
        return Mockito.mock(MetricsService.class);
    }

    /**
     * A no-op transaction manager (normally provided by a persistence module).
     * Causeway's {@code AuthenticationManager} opens an anonymous interaction
     * whose teardown queries the transaction state, which requires exactly one
     * {@link PlatformTransactionManager} to be present.
     */
    @Bean
    public PlatformTransactionManager transactionManager() {
        return new PlatformTransactionManager() {
            @Override
            public TransactionStatus getTransaction(final TransactionDefinition definition) {
                return new SimpleTransactionStatus();
            }
            @Override
            public void commit(final TransactionStatus status) {
                // no-op
            }
            @Override
            public void rollback(final TransactionStatus status) {
                // no-op
            }
        };
    }
}
