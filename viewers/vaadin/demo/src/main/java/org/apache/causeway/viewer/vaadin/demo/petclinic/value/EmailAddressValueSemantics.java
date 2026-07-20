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

import jakarta.inject.Named;

import org.springframework.stereotype.Component;

import org.apache.causeway.applib.services.bookmark.IdStringifier;
import org.apache.causeway.applib.value.semantics.DefaultsProvider;
import org.apache.causeway.applib.value.semantics.Parser;
import org.apache.causeway.applib.value.semantics.Renderer;
import org.apache.causeway.applib.value.semantics.ValueDecomposition;
import org.apache.causeway.applib.value.semantics.ValueSemanticsAbstract;
import org.apache.causeway.commons.internal.base._Strings;
import org.apache.causeway.schema.common.v2.ValueType;
import org.apache.causeway.schema.common.v2.ValueWithTypeDto;

import lombok.NonNull;

/** Ported from {@code apache/causeway-app-petclinic}. */
@Named("demo.petclinic.EmailAddressValueSemantics")
@Component
public class EmailAddressValueSemantics extends ValueSemanticsAbstract<EmailAddress> {

    @Override
    public Class<EmailAddress> getCorrespondingClass() {
        return EmailAddress.class;
    }

    @Override
    public ValueType getSchemaValueType() {
        return ValueType.STRING;
    }

    @Override
    public ValueDecomposition decompose(final EmailAddress value) {
        return decomposeAsNullable(value, EmailAddress::getValue, () -> null);
    }

    @Override
    public EmailAddress compose(final ValueDecomposition decomposition) {
        return composeFromNullable(
                decomposition, ValueWithTypeDto::getString, EmailAddress::of, () -> null);
    }

    @Override
    public DefaultsProvider<EmailAddress> getDefaultsProvider() {
        return () -> null;
    }

    @Override
    public Renderer<EmailAddress> getRenderer() {
        return (context, emailAddress) -> emailAddress == null ? null : emailAddress.getValue();
    }

    @Override
    public Parser<EmailAddress> getParser() {
        return new Parser<>() {

            @Override
            public String parseableTextRepresentation(final Context context, final EmailAddress emailAddress) {
                return renderTitle(emailAddress, EmailAddress::getValue);
            }

            @Override
            public EmailAddress parseTextRepresentation(final Context context, final String text) {
                return EmailAddress.of(text);
            }

            @Override
            public int typicalLength() {
                return EmailAddress.TYPICAL_LEN;
            }

            @Override
            public int maxLength() {
                return EmailAddress.MAX_LEN;
            }
        };
    }

    @Override
    public IdStringifier<EmailAddress> getIdStringifier() {
        return new IdStringifier.EntityAgnostic<>() {
            @Override
            public Class<EmailAddress> getCorrespondingClass() {
                return EmailAddressValueSemantics.this.getCorrespondingClass();
            }

            @Override
            public String enstring(@NonNull final EmailAddress value) {
                return _Strings.base64UrlEncode(value.getValue());
            }

            @Override
            public EmailAddress destring(@NonNull final String stringified) {
                return EmailAddress.of(_Strings.base64UrlDecode(stringified));
            }
        };
    }
}
