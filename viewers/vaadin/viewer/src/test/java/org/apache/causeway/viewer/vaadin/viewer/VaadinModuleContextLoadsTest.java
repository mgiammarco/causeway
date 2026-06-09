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

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import org.apache.causeway.applib.services.repository.RepositoryService;
import org.apache.causeway.core.metamodel.services.deadlock.DeadlockRecognizer;
import org.apache.causeway.core.runtimeservices.CausewayModuleCoreRuntimeServices;
import org.apache.causeway.security.bypass.CausewayModuleSecurityBypass;
import org.apache.causeway.viewer.vaadin.ui.components.UiComponentFactoryVaa;

import static org.junit.jupiter.api.Assertions.assertNotNull;

// MOCK provides a mock servlet environment so WebApplicationContext and
// ServletRegistrationBean can be satisfied without a real embedded server.
@SpringBootTest(
        classes = VaadinModuleContextLoadsTest.AppManifest.class,
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = {
                // suppress metamodel validation so the empty domain boots cleanly
                "causeway.core.meta-model.validator.allow-deprecated=true",
                // minimal introspection: no domain classes to scan
                "causeway.core.meta-model.introspector.mode=FULL",
                // do not fail on absent object-type (no domain objects registered)
                "causeway.core.meta-model.validator.explicit-object-type=false",
                // disable JPA/persistence auto-config that is not on test classpath
                "spring.autoconfigure.exclude=" +
                        "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration," +
                        "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration," +
                        // Hilla (transitive via vaadin-spring) auto-configs need Hilla runtime not present in test
                        "com.vaadin.hilla.EndpointController," +
                        "com.vaadin.hilla.push.PushConfigurer," +
                        "com.vaadin.hilla.ApplicationContextProvider," +
                        "com.vaadin.hilla.startup.EndpointRegistryInitializer," +
                        "com.vaadin.hilla.startup.RouteUnifyingServiceInitListener," +
                        "com.vaadin.hilla.route.RouteUtil," +
                        "com.vaadin.hilla.route.RouteUnifyingConfiguration," +
                        "com.vaadin.hilla.signals.config.SignalsConfiguration",
        })
class VaadinModuleContextLoadsTest {

    @Configuration
    @Import({
            CausewayModuleCoreRuntimeServices.class,
            CausewayModuleSecurityBypass.class,
            CausewayModuleViewerVaadinViewer.class,
    })
    static class AppManifest {
        // no-op stub: normally provided by persistence-commons (not on test classpath)
        @Bean DeadlockRecognizer deadlockRecognizer() { return e -> false; }
        // mock stub: normally provided by persistence-commons (not on test classpath)
        @Bean RepositoryService repositoryService() { return Mockito.mock(RepositoryService.class); }
    }

    @Autowired ApplicationContext context;

    @Test
    void contextLoads_withComponentFactoryChain() {
        var factory = context.getBean(UiComponentFactoryVaa.class);
        assertNotNull(factory);
        assertNotNull(context.getBean(CausewayModuleViewerVaadinViewer.class));
    }
}
