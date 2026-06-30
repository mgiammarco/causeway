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
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;

import org.apache.causeway.core.config.presets.CausewayPresets;
import org.apache.causeway.core.runtimeservices.CausewayModuleCoreRuntimeServices;
import org.apache.causeway.security.bypass.CausewayModuleSecurityBypass;
import org.apache.causeway.viewer.vaadin.viewer.CausewayModuleViewerVaadinViewer;

/**
 * Standalone runnable demo: boots the Vaadin viewer against the small
 * view-model domain in this package (no persistence). Run with
 * {@code mvn spring-boot:run} from this module, then open http://localhost:8080.
 */
@SpringBootApplication
@ComponentScan
// the @Route views (MainViewVaa, VaadinLoginView) live under the viewer's
// own package, not under this app's package; tell Vaadin to scan it.
@EnableVaadin("org.apache.causeway.viewer.vaadin")
@Import({
    CausewayModuleCoreRuntimeServices.class,
    CausewayModuleSecurityBypass.class,
    CausewayModuleViewerVaadinViewer.class,
})
public class DemoAppVaa {

    public static void main(final String[] args) {
        CausewayPresets.prototyping();
        SpringApplication.run(DemoAppVaa.class, args);
    }
}
