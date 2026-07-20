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

import com.vaadin.flow.spring.RootMappedCondition;
import com.vaadin.flow.spring.SpringBootAutoConfiguration;
import com.vaadin.flow.spring.SpringServlet;
import com.vaadin.flow.spring.VaadinConfigurationProperties;
import com.vaadin.flow.spring.VaadinServletConfiguration;
import com.vaadin.flow.spring.VaadinServletContextInitializer;

import jakarta.inject.Inject;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.util.ClassUtils;
import org.springframework.web.context.WebApplicationContext;

import org.apache.causeway.applib.services.iactn.InteractionService;
import org.apache.causeway.viewer.vaadin.ui.CausewayModuleViewerVaadinUi;

/**
 * Root configuration of the Vaadin viewer: import this single class from an
 * application manifest to enable the viewer.
 */
@Configuration
@Import({
        CausewayModuleViewerVaadinUi.class,
        VaadinConfigurationProperties.class,
        // registers the root-mapping forwarding controller (/ -> /vaadinServlet/*),
        // active only when vaadin.urlMapping is the root mapping; normally pulled in by
        // Vaadin's SpringBootAutoConfiguration, which we exclude to install our own servlet.
        VaadinServletConfiguration.class,
})
@PropertySource("classpath:/vaadin.properties")
// standard Vaadin Spring Boot bootstrapping is replaced by the beans below
@EnableAutoConfiguration(exclude = {SpringBootAutoConfiguration.class})
public class CausewayModuleViewerVaadinViewer {

    @Inject private WebApplicationContext context;
    @Inject private VaadinConfigurationProperties configurationProperties;
    @Inject private InteractionService interactionService;

    @Bean
    public ServletContextInitializer vaadinServletContextInitializer() {
        return new VaadinServletContextInitializer(context);
    }

    @Bean
    public ServletRegistrationBean<SpringServlet> vaadinServletRegistrationBean() {
        var urlMapping = configurationProperties.getUrlMapping();
        var isRootMapping = RootMappedCondition.isRootMapping(urlMapping);
        if (isRootMapping) {
            urlMapping = "/vaadinServlet/*";
        }
        var registration = new ServletRegistrationBean<SpringServlet>(
                new CausewayServletForVaadin(interactionService, context, isRootMapping),
                urlMapping);
        registration.setAsyncSupported(configurationProperties.isAsyncSupported());
        registration.setName(ClassUtils.getShortNameAsProperty(SpringServlet.class));
        return registration;
    }
}
