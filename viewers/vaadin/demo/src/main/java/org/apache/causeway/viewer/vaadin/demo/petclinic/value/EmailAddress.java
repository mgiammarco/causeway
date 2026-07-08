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
package org.apache.causeway.viewer.vaadin.demo.petclinic.value;

import java.io.Serializable;
import java.util.regex.Pattern;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import org.apache.causeway.applib.annotation.Value;
import org.apache.causeway.commons.internal.base._Strings;

import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * Ported from {@code apache/causeway-app-petclinic} — a custom, immutable,
 * regex-validated composite value type. Exercises the viewer's generic
 * parsable-text value rendering ({@code ValueFieldFactory}) against a value
 * type with its own {@link EmailAddressValueSemantics}, rather than one of
 * the JDK-builtin scalar types.
 */
@Embeddable
@Value
@EqualsAndHashCode
public class EmailAddress implements Serializable {

    static final int MAX_LEN = 100;
    static final int TYPICAL_LEN = 30;
    static final Pattern REGEX = Pattern.compile("^[\\w-\\+]+(\\.[\\w]+)*@[\\w-]+(\\.[\\w]+)*(\\.[a-zA-Z]{2,})$");

    public static EmailAddress of(final String value) {
        if (_Strings.isNullOrEmpty(value)) {
            return null;
        }
        if (!REGEX.matcher(value).matches()) {
            throw new IllegalArgumentException("Invalid email format");
        }
        final var ea = new EmailAddress();
        ea.value = value;
        return ea;
    }

    protected EmailAddress() { } // required by JPA

    @Getter
    @Column(length = MAX_LEN, nullable = true, name = "emailAddress")
    String value;
}
