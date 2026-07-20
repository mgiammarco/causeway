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
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.server.StreamResource;

import org.springframework.core.annotation.Order;

import org.apache.causeway.applib.annotation.PriorityPrecedence;
import org.apache.causeway.applib.value.Blob;
import org.apache.causeway.applib.value.Clob;
import org.apache.causeway.core.metamodel.object.ManagedObject;
import org.apache.causeway.viewer.commons.model.components.UiComponentFactory.ComponentRequest;
import org.apache.causeway.viewer.vaadin.ui.components.UiComponentHandlerVaa;

/**
 * Renders a {@link Blob} or {@link Clob} value property: a download link for the
 * current content, plus (when editable) a file {@link Upload} whose result is
 * written back into the value.
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

        if (!request.disablingUiModelIfAny().isPresent()) {
            layout.add(uploadField(request));
        }
        return layout;
    }

    private static Upload uploadField(final ComponentRequest request) {
        var isClob = request.isFeatureTypeEqualTo(Clob.class);
        var buffer = new MemoryBuffer();
        var upload = new Upload(buffer);
        upload.setMaxFiles(1);
        upload.addSucceededListener(event -> {
            try {
                var bytes = buffer.getInputStream().readAllBytes();
                var name = event.getFileName();
                var mime = event.getMIMEType();
                Object value = isClob
                        ? new Clob(name, mime, new String(bytes, StandardCharsets.UTF_8).toCharArray())
                        : new Blob(name, mime, bytes);
                request.managedValue().getValue().setValue(
                        ManagedObject.adaptSingular(request.getFeatureTypeSpec(), value));
            } catch (IOException ex) {
                throw new RuntimeException("failed to read uploaded file", ex);
            }
        });
        return upload;
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
