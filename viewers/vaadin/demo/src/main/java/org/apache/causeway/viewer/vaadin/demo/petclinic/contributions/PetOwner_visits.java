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
package org.apache.causeway.viewer.vaadin.demo.petclinic.contributions;

import java.util.List;

import jakarta.inject.Inject;

import org.apache.causeway.applib.annotation.Collection;
import org.apache.causeway.applib.annotation.MemberSupport;

import org.apache.causeway.viewer.vaadin.demo.petclinic.dom.petowner.PetOwner;
import org.apache.causeway.viewer.vaadin.demo.petclinic.dom.visit.Visit;
import org.apache.causeway.viewer.vaadin.demo.petclinic.dom.visit.VisitRepository;

import lombok.RequiredArgsConstructor;

/**
 * Ported from {@code apache/causeway-app-petclinic} — a contributed
 * (mixin) collection, rendered as an extra tab/panel on the owner's object
 * page alongside its own declared properties/collections.
 */
@Collection
@RequiredArgsConstructor
public class PetOwner_visits {

    private final PetOwner petOwner;

    @MemberSupport
    public List<Visit> coll() {
        return visitRepository.findByPetOwner(petOwner);
    }

    @Inject VisitRepository visitRepository;
}
