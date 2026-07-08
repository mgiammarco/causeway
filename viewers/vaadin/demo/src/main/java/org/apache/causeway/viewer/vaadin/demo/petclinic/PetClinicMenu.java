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
package org.apache.causeway.viewer.vaadin.demo.petclinic;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.apache.causeway.applib.annotation.Action;
import org.apache.causeway.applib.annotation.ActionLayout;
import org.apache.causeway.applib.annotation.DomainService;
import org.apache.causeway.applib.annotation.DomainServiceLayout;
import org.apache.causeway.applib.services.factory.FactoryService;
import org.apache.causeway.applib.services.repository.RepositoryService;

import org.apache.causeway.viewer.vaadin.demo.petclinic.dom.pet.PetSpecies;
import org.apache.causeway.viewer.vaadin.demo.petclinic.dom.petowner.PetOwner;
import org.apache.causeway.viewer.vaadin.demo.petclinic.dom.visit.Visit;
import org.apache.causeway.viewer.vaadin.demo.petclinic.types.Name;
import org.apache.causeway.viewer.vaadin.demo.petclinic.value.EmailAddress;

/**
 * Repository-backed menu over the ported {@code apache/causeway-app-petclinic}
 * domain — a second, unrelated domain area (alongside {@link
 * org.apache.causeway.viewer.vaadin.demo.DemoMenu}'s Library) so the viewer's
 * widgets can be exercised against entity relationships, mixin
 * actions/collections, and a custom value type it wasn't originally built
 * against.
 */
@Named("demo.PetClinicMenu")
@DomainService
@DomainServiceLayout(named = "PetClinic")
public class PetClinicMenu {

    @Inject private RepositoryService repositoryService;
    @Inject private FactoryService factoryService;

    @Action
    @ActionLayout(sequence = "1")
    public List<PetOwner> listOwners() {
        return repositoryService.allInstances(PetOwner.class);
    }

    @Action
    @ActionLayout(sequence = "2")
    public PetOwner createOwner(@Name final String name) {
        var owner = factoryService.detachedEntity(PetOwner.class);
        owner.setName(name);
        return repositoryService.persist(owner);
    }

    @Action
    @ActionLayout(sequence = "9", describedAs = "(re)create sample pet owners, pets and visits")
    public List<PetOwner> populate() {
        var jones = owner("Fiona Jones", "Fi", "+353 1 555 1234",
                EmailAddress.of("fiona.jones@example.com"), LocalDate.now().minusDays(3));
        jones.addPet("Rex", PetSpecies.Dog);
        jones.addPet("Whiskers", PetSpecies.Cat);
        repositoryService.persistAndFlush(jones);
        jones.getPets().forEach(pet ->
                repositoryService.persistAndFlush(new Visit(pet, LocalDateTime.now().minusDays(3).withHour(9))));

        var oconnor = owner("Liam O'Connor", null, null, null, null);
        oconnor.addPet("Charlie", PetSpecies.Budgerigar);
        repositoryService.persistAndFlush(oconnor);

        var singh = owner("Priya Singh", "Priy", "07123 456789",
                EmailAddress.of("priya.singh@example.com"), null);
        singh.addPet("Nibbles", PetSpecies.Hamster);
        repositoryService.persistAndFlush(singh);

        return listOwners();
    }

    private PetOwner owner(final String name, final String knownAs, final String telephoneNumber,
            final EmailAddress emailAddress, final LocalDate lastVisit) {
        var owner = factoryService.detachedEntity(PetOwner.class);
        owner.setName(name);
        owner.setKnownAs(knownAs);
        owner.setTelephoneNumber(telephoneNumber);
        owner.setEmailAddress(emailAddress);
        owner.setLastVisit(lastVisit);
        return owner;
    }
}
