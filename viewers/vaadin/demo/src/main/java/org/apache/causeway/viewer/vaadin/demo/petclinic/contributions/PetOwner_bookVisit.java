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

import java.time.LocalDateTime;
import java.util.Set;

import jakarta.inject.Inject;

import org.apache.causeway.applib.annotation.Action;
import org.apache.causeway.applib.annotation.ActionLayout;
import org.apache.causeway.applib.annotation.MemberSupport;
import org.apache.causeway.applib.services.clock.ClockService;
import org.apache.causeway.applib.services.repository.RepositoryService;

import org.apache.causeway.viewer.vaadin.demo.petclinic.dom.pet.Pet;
import org.apache.causeway.viewer.vaadin.demo.petclinic.dom.petowner.PetOwner;
import org.apache.causeway.viewer.vaadin.demo.petclinic.dom.visit.Visit;

import lombok.RequiredArgsConstructor;

/**
 * Ported from {@code apache/causeway-app-petclinic} (module-visit) — a
 * contributed (mixin) action: exercises the viewer's handling of mixin
 * actions with parameter defaults/choices/validation drawn from the mixee.
 */
@Action
@ActionLayout(associateWith = "visits")
@RequiredArgsConstructor
public class PetOwner_bookVisit {

    private final PetOwner petOwner;

    @MemberSupport
    public PetOwner act(final Pet pet, final LocalDateTime visitAt) {
        repositoryService.persistAndFlush(new Visit(pet, visitAt));
        return petOwner;
    }

    @MemberSupport
    public Set<Pet> choices0Act() {
        return petOwner.getPets();
    }

    @MemberSupport
    public Pet default0Act() {
        var pets = petOwner.getPets();
        return pets.size() == 1 ? pets.iterator().next() : null;
    }

    @MemberSupport
    public LocalDateTime default1Act() {
        return officeHoursTomorrow();
    }

    @MemberSupport
    public String validate1Act(final LocalDateTime visitAt) {
        return visitAt.isBefore(officeHoursTomorrow()) ? "Must book in the future" : null;
    }

    private LocalDateTime officeHoursTomorrow() {
        return clockService.getClock().nowAsLocalDate().atStartOfDay().plusDays(1).plusHours(9);
    }

    @Inject ClockService clockService;
    @Inject RepositoryService repositoryService;
}
