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

import org.apache.causeway.applib.annotation.Parameter;
import org.apache.causeway.applib.annotation.ParameterLayout;
import org.apache.causeway.applib.annotation.Property;
import org.apache.causeway.applib.spec.AbstractSpecification;

/**
 * Ported from {@code apache/causeway-app-petclinic} (module-petowner) — composed
 * annotation carrying validation for a person/owner name.
 */
@Property(maxLength = Name.MAX_LEN, mustSatisfy = Name.Spec.class)
@Parameter(maxLength = Name.MAX_LEN, mustSatisfy = Name.Spec.class)
@ParameterLayout(named = "Name")
@Target({ ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface Name {

    int MAX_LEN = 40;
    String PROHIBITED_CHARACTERS = "&%$!";

    class Spec extends AbstractSpecification<String> {
        @Override public String satisfiesSafely(final String candidate) {
            for (char prohibitedCharacter : PROHIBITED_CHARACTERS.toCharArray()) {
                if (candidate.contains("" + prohibitedCharacter)) {
                    return "Character '" + prohibitedCharacter + "' is not allowed.";
                }
            }
            return null;
        }
    }
}
