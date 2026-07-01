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

import com.vaadin.flow.spring.annotation.EnableVaadin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;

import org.apache.causeway.core.config.presets.CausewayPresets;
import org.apache.causeway.core.runtimeservices.CausewayModuleCoreRuntimeServices;
import org.apache.causeway.persistence.jpa.eclipselink.CausewayModulePersistenceJpaEclipselink;
import org.apache.causeway.security.bypass.CausewayModuleSecurityBypass;
import org.apache.causeway.viewer.vaadin.viewer.CausewayModuleViewerVaadinViewer;

/**
 * Standalone runnable demo: boots the Vaadin viewer against a tiny JPA domain
 * ({@link DemoAuthor}, {@link DemoBook}) on an in-memory H2 database. Run with
 * {@code mvn spring-boot:run} from this module, then open http://localhost:8080.
 */
@SpringBootApplication
@ComponentScan
@EntityScan(basePackageClasses = DemoBook.class)
// the @Route views (MainViewVaa, VaadinLoginView) live under the viewer's
// own package, not under this app's package; tell Vaadin to scan it.
@EnableVaadin("org.apache.causeway.viewer.vaadin")
@Import({
    CausewayModuleCoreRuntimeServices.class,
    CausewayModuleSecurityBypass.class,
    CausewayModulePersistenceJpaEclipselink.class,
    CausewayModuleViewerVaadinViewer.class,
})
@PropertySource(CausewayPresets.H2InMemory_withUniqueSchema)
public class DemoAppVaa {

    public static void main(final String[] args) {
        // note: prototyping mode is intentionally NOT enabled — it would add the
        // 'Prototyping' menu and a raft of developer actions (Download Layout,
        // Inspect Metamodel, ...) to every object, cluttering the demo UI.
        SpringApplication.run(DemoAppVaa.class, args);
    }
}
