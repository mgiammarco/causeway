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

import java.time.LocalDate;

import jakarta.inject.Named;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.apache.causeway.applib.annotation.Action;
import org.apache.causeway.applib.annotation.DomainObject;
import org.apache.causeway.applib.annotation.Editing;
import org.apache.causeway.applib.annotation.Nature;
import org.apache.causeway.applib.annotation.ObjectSupport;
import org.apache.causeway.applib.annotation.Property;
import org.apache.causeway.applib.jaxb.JavaTimeJaxbAdapters.LocalDateToStringAdapter;

/**
 * A tiny JAXB view-model used to exercise the Vaadin viewer's object page:
 * a String property (text field), a date property (date picker) and an action
 * with a parameter (parameter dialog).
 */
@XmlRootElement(name = "demoBook")
@XmlAccessorType(XmlAccessType.FIELD)
@Named("demo.Book")
@DomainObject(nature = Nature.VIEW_MODEL)
public class DemoBook {

    @Property(editing = Editing.ENABLED)
    private String title;

    @Property(editing = Editing.ENABLED)
    private String author;

    @Property(editing = Editing.ENABLED)
    @XmlJavaTypeAdapter(LocalDateToStringAdapter.class)
    private LocalDate published;

    @ObjectSupport
    public String title() {
        return title == null ? "(untitled)" : title;
    }

    public String getTitle() {
        return title;
    }
    public void setTitle(final String title) {
        this.title = title;
    }

    public String getAuthor() {
        return author;
    }
    public void setAuthor(final String author) {
        this.author = author;
    }

    public LocalDate getPublished() {
        return published;
    }
    public void setPublished(final LocalDate published) {
        this.published = published;
    }

    @Action
    public DemoBook rename(final String newTitle) {
        setTitle(newTitle);
        return this;
    }
}
