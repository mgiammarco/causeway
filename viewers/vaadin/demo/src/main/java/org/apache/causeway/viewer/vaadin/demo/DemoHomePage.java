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

import java.util.List;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;

import org.apache.causeway.applib.annotation.Action;
import org.apache.causeway.applib.annotation.ActionLayout;
import org.apache.causeway.applib.annotation.DomainObject;
import org.apache.causeway.applib.annotation.HomePage;
import org.apache.causeway.applib.annotation.Nature;
import org.apache.causeway.applib.annotation.ObjectSupport;
import org.apache.causeway.applib.services.user.UserService;

/**
 * Landing page rendered by the viewer on entry (via {@code @HomePage}).
 */
@XmlRootElement(name = "demoHomePage")
@XmlAccessorType(XmlAccessType.FIELD)
@Named("demo.HomePage")
@DomainObject(nature = Nature.VIEW_MODEL)
@HomePage
public class DemoHomePage {

    @Inject private DemoMenu demoMenu;
    @Inject private UserService userService;

    @ObjectSupport
    public String title() {
        return "Welcome, " + userService.currentUserNameElseNobody();
    }

    @Action
    @ActionLayout(sequence = "1", describedAs = "show the sample books")
    public List<DemoBook> books() {
        return demoMenu.listBooks();
    }
}
