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

import jakarta.inject.Named;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;

import org.apache.causeway.applib.annotation.DomainObject;
import org.apache.causeway.applib.annotation.Editing;
import org.apache.causeway.applib.annotation.Nature;
import org.apache.causeway.applib.annotation.ObjectSupport;
import org.apache.causeway.applib.annotation.Property;
import org.apache.causeway.applib.value.Blob;
import org.apache.causeway.applib.value.Clob;

/**
 * View-model demonstrating editable {@link Blob} / {@link Clob} properties: the
 * object page shows a file upload (and a download link once a value is set).
 */
@XmlRootElement(name = "demoUpload")
@XmlAccessorType(XmlAccessType.FIELD)
@Named("demo.Upload")
@DomainObject(nature = Nature.VIEW_MODEL)
public class DemoUpload {

    @Property(editing = Editing.ENABLED)
    private Blob attachment;

    @Property(editing = Editing.ENABLED)
    private Clob notes;

    @ObjectSupport
    public String title() {
        return "Upload demo";
    }

    public Blob getAttachment() {
        return attachment;
    }
    public void setAttachment(final Blob attachment) {
        this.attachment = attachment;
    }

    public Clob getNotes() {
        return notes;
    }
    public void setNotes(final Clob notes) {
        this.notes = notes;
    }
}
