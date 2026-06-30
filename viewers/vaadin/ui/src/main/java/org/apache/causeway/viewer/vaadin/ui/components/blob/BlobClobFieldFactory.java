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
package org.apache.causeway.viewer.vaadin.ui.components.blob;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.server.StreamResource;

import org.springframework.core.annotation.Order;

import org.apache.causeway.applib.annotation.PriorityPrecedence;
import org.apache.causeway.applib.value.Blob;
import org.apache.causeway.applib.value.Clob;
import org.apache.causeway.viewer.commons.model.components.UiComponentFactory.ComponentRequest;
import org.apache.causeway.viewer.vaadin.ui.components.UiComponentHandlerVaa;

/**
 * Renders a {@link Blob} or {@link Clob} value property as a labelled download
 * link (the binary/character content is streamed on demand). Read-only; upload
 * editing of persisted blobs is a later increment.
 */
@org.springframework.stereotype.Component
@Order(PriorityPrecedence.MIDPOINT)
public class BlobClobFieldFactory implements UiComponentHandlerVaa {

    static boolean handles(final Class<?> featureType) {
        return Blob.class.equals(featureType) || Clob.class.equals(featureType);
    }

    @Override
    public boolean isHandling(final ComponentRequest request) {
        return handles(request.getFeatureType());
    }

    @Override
    public Component handle(final ComponentRequest request) {
        var layout = new VerticalLayout();
        layout.setPadding(false);
        layout.setSpacing(false);
        layout.add(new Span(request.getFriendlyName()));

        var managedObject = request.managedValue().getValue().getValue();
        var pojo = managedObject != null ? managedObject.getPojo() : null;

        if (pojo instanceof Blob blob) {
            layout.add(downloadLink(blob.name(), blob.bytes()));
        } else if (pojo instanceof Clob clob) {
            layout.add(downloadLink(clob.name(),
                    clob.chars().toString().getBytes(StandardCharsets.UTF_8)));
        } else {
            layout.add(new Span("(none)"));
        }
        return layout;
    }

    private static Anchor downloadLink(final String fileName, final byte[] content) {
        var safeName = fileName == null ? "download" : fileName;
        var resource = new StreamResource(safeName,
                () -> new ByteArrayInputStream(content == null ? new byte[0] : content));
        var anchor = new Anchor(resource, safeName);
        anchor.getElement().setAttribute("download", true);
        return anchor;
    }
}
