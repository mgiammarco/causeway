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
package org.apache.causeway.viewer.vaadin.ui.components.result;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.server.StreamResource;

import org.apache.causeway.applib.value.Blob;
import org.apache.causeway.applib.value.Clob;
import org.apache.causeway.applib.value.Markup;
import org.apache.causeway.core.metamodel.object.ManagedObject;
import org.apache.causeway.core.metamodel.object.MmTitleUtils;

/**
 * Renders a scalar <i>value</i> returned by an action (as opposed to a domain
 * object or a collection): a {@link Blob}/{@link Clob} as a download link, a
 * {@link Markup} as HTML, anything else as its title text.
 */
public final class ValueResultViewVaa {

    private ValueResultViewVaa() {
    }

    public static Component forValue(final ManagedObject valueResult) {
        var layout = new VerticalLayout();
        var pojo = valueResult != null ? valueResult.getPojo() : null;

        if (pojo instanceof Blob blob) {
            layout.add(downloadLink(blob.name(), blob.bytes()));
        } else if (pojo instanceof Clob clob) {
            layout.add(downloadLink(clob.name(),
                    clob.chars().toString().getBytes(StandardCharsets.UTF_8)));
        } else if (pojo instanceof Markup markup) {
            var div = new Div();
            div.getElement().setProperty("innerHTML", markup.html() == null ? "" : markup.html());
            layout.add(div);
        } else {
            layout.add(new Span(MmTitleUtils.titleOf(valueResult)));
        }
        return layout;
    }

    private static Anchor downloadLink(final String fileName, final byte[] content) {
        var safeName = fileName == null ? "download" : fileName;
        var resource = new StreamResource(safeName,
                () -> new ByteArrayInputStream(content == null ? new byte[0] : content));
        var anchor = new Anchor(resource, "Download " + safeName);
        anchor.getElement().setAttribute("download", true);
        return anchor;
    }
}
