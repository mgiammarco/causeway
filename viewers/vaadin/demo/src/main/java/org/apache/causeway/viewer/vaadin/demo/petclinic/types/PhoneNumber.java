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
package org.apache.causeway.viewer.vaadin.demo.petclinic.types;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.apache.causeway.applib.annotation.Editing;
import org.apache.causeway.applib.annotation.Optionality;
import org.apache.causeway.applib.annotation.Parameter;
import org.apache.causeway.applib.annotation.ParameterLayout;
import org.apache.causeway.applib.annotation.Property;

/**
 * Ported from {@code apache/causeway-app-petclinic} — exercises regex-pattern
 * validated text input.
 */
@Property(
        editing = Editing.ENABLED,
        maxLength = PhoneNumber.MAX_LEN,
        optionality = Optionality.OPTIONAL,
        regexPattern = PhoneNumber.REGEX_PATTERN,
        regexPatternReplacement = PhoneNumber.REGEX_PATTERN_REPLACEMENT
)
@Parameter(
        maxLength = PhoneNumber.MAX_LEN,
        optionality = Optionality.OPTIONAL,
        regexPattern = PhoneNumber.REGEX_PATTERN,
        regexPatternReplacement = PhoneNumber.REGEX_PATTERN_REPLACEMENT
)
@Target({ ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface PhoneNumber {

    int MAX_LEN = 40;
    String REGEX_PATTERN = "[+]?[0-9 ]+";
    String REGEX_PATTERN_REPLACEMENT =
            "Specify only numbers and spaces, optionally prefixed with '+'.  "
            + "For example, '+353 1 555 1234', or '07123 456789'";
}
