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
package org.apache.causeway.viewer.vaadin.demo.petclinic.dom.petowner;

import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.apache.causeway.applib.annotation.Action;
import org.apache.causeway.applib.annotation.ActionLayout;
import org.apache.causeway.applib.annotation.BookmarkPolicy;
import org.apache.causeway.applib.annotation.Collection;
import org.apache.causeway.applib.annotation.DomainObject;
import org.apache.causeway.applib.annotation.DomainObjectLayout;
import org.apache.causeway.applib.annotation.Editing;
import org.apache.causeway.applib.annotation.MemberSupport;
import org.apache.causeway.applib.annotation.ObjectSupport;
import org.apache.causeway.applib.annotation.Optionality;
import org.apache.causeway.applib.annotation.Property;
import org.apache.causeway.applib.annotation.Publishing;
import org.apache.causeway.applib.jaxb.PersistentEntityAdapter;
import org.apache.causeway.applib.services.clock.ClockService;
import org.apache.causeway.applib.services.message.MessageService;
import org.apache.causeway.applib.services.repository.RepositoryService;
import org.apache.causeway.applib.services.title.TitleService;
import org.apache.causeway.persistence.jpa.applib.integration.CausewayEntityListener;

import org.apache.causeway.viewer.vaadin.demo.petclinic.dom.pet.Pet;
import org.apache.causeway.viewer.vaadin.demo.petclinic.dom.pet.PetSpecies;
import org.apache.causeway.viewer.vaadin.demo.petclinic.types.Name;
import org.apache.causeway.viewer.vaadin.demo.petclinic.types.Notes;
import org.apache.causeway.viewer.vaadin.demo.petclinic.types.PetName;
import org.apache.causeway.viewer.vaadin.demo.petclinic.types.PhoneNumber;
import org.apache.causeway.viewer.vaadin.demo.petclinic.value.EmailAddress;

import static org.apache.causeway.applib.annotation.SemanticsOf.IDEMPOTENT;
import static org.apache.causeway.applib.annotation.SemanticsOf.NON_IDEMPOTENT_ARE_YOU_SURE;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Ported from {@code apache/causeway-app-petclinic} (module-petowner), trimmed
 * of extensions not on this demo's classpath (fullcalendar, pdfjs, blob
 * attachment) — kept: a one-to-many collection with add/remove mixin-style
 * actions, a custom embedded value type ({@link EmailAddress}), regex/length
 * validated text properties, and update/delete actions.
 */
@Entity
@Table(
        uniqueConstraints = {
                @UniqueConstraint(name = "PetOwner__name__UNQ", columnNames = { "name" })
        }
)
@NamedQueries({
        @NamedQuery(
                name = PetOwner.NAMED_QUERY__FIND_BY_NAME_LIKE,
                query = "SELECT po FROM PetOwner po WHERE po.name LIKE :name"
        )
})
@EntityListeners(CausewayEntityListener.class)
@Named("demo.petclinic.PetOwner")
@DomainObject(entityChangePublishing = Publishing.ENABLED)
@DomainObjectLayout(bookmarking = BookmarkPolicy.AS_ROOT)
@NoArgsConstructor(access = AccessLevel.PUBLIC)
@XmlJavaTypeAdapter(PersistentEntityAdapter.class)
@ToString(onlyExplicitlyIncluded = true)
public class PetOwner implements Comparable<PetOwner> {

    static final String NAMED_QUERY__FIND_BY_NAME_LIKE = "PetOwner.findByNameLike";

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false)
    private Long id;

    @Version
    @Column(name = "version", nullable = false)
    @Getter @Setter
    private long version;

    public static PetOwner withName(final String name) {
        var petOwner = new PetOwner();
        petOwner.setName(name);
        return petOwner;
    }

    @Inject @Transient RepositoryService repositoryService;
    @Inject @Transient TitleService titleService;
    @Inject @Transient MessageService messageService;
    @Inject @Transient ClockService clockService;

    @ObjectSupport
    public String title() {
        return getName() + (getKnownAs() != null ? " (" + getKnownAs() + ")" : "");
    }

    @Name
    @Column(length = Name.MAX_LEN, nullable = false, name = "name")
    @Getter @Setter @ToString.Include
    private String name;

    @Column(length = 40, nullable = true, name = "knownAs")
    @Getter @Setter
    @Property(editing = Editing.ENABLED)
    private String knownAs;

    @PhoneNumber
    @Column(length = PhoneNumber.MAX_LEN, nullable = true, name = "telephoneNumber")
    @Getter @Setter
    private String telephoneNumber;

    @Embedded
    @Getter @Setter
    @Property(editing = Editing.ENABLED, optionality = Optionality.OPTIONAL)
    private EmailAddress emailAddress;

    @Notes
    @Column(length = Notes.MAX_LEN, nullable = true)
    @Getter @Setter
    @Property(commandPublishing = Publishing.ENABLED, executionPublishing = Publishing.ENABLED)
    private String notes;

    @Collection
    @Getter
    @OneToMany(mappedBy = "petOwner", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Pet> pets = new TreeSet<>();

    @Action
    @ActionLayout(associateWith = "pets", sequence = "1")
    public PetOwner addPet(@PetName final String name, final PetSpecies species) {
        var pet = new Pet(this, name);
        pet.setSpecies(species);
        pets.add(pet);
        return this;
    }

    @MemberSupport
    public String validate0AddPet(final String name) {
        if (getPets().stream().anyMatch(x -> Objects.equals(x.getName(), name))) {
            return "This owner already has a pet called '" + name + "'";
        }
        return null;
    }

    @Action(choicesFrom = "pets")
    @ActionLayout(associateWith = "pets", sequence = "2")
    public PetOwner removePet(@PetName final Pet pet) {
        pets.remove(pet);
        return this;
    }

    @Property(optionality = Optionality.OPTIONAL, editing = Editing.ENABLED)
    @Column(nullable = true)
    @Getter @Setter
    private java.time.LocalDate lastVisit;

    @Property
    public Long getDaysSinceLastVisit() {
        return getLastVisit() != null
                ? ChronoUnit.DAYS.between(getLastVisit(), clockService.getClock().nowAsLocalDate())
                : null;
    }

    @Action(semantics = IDEMPOTENT, commandPublishing = Publishing.ENABLED, executionPublishing = Publishing.ENABLED)
    @ActionLayout(
            describedAs = "Updates the name of this object, certain characters ("
                    + Name.PROHIBITED_CHARACTERS + ") are not allowed.")
    public PetOwner updateName(@Name final String name) {
        setName(name);
        return this;
    }
    @MemberSupport public String default0UpdateName() {
        return getName();
    }

    public static class DeleteActionDomainEvent
            extends org.apache.causeway.applib.events.domain.ActionDomainEvent<PetOwner> { }

    @Action(semantics = NON_IDEMPOTENT_ARE_YOU_SURE, domainEvent = DeleteActionDomainEvent.class)
    @ActionLayout(describedAs = "Deletes this object from the persistent datastore")
    public void delete() {
        var title = titleService.titleOf(this);
        messageService.informUser(String.format("'%s' deleted", title));
        repositoryService.removeAndFlush(this);
    }

    private static final Comparator<PetOwner> comparator = Comparator.comparing(PetOwner::getName);

    @Override
    public int compareTo(final PetOwner other) {
        return comparator.compare(this, other);
    }
}
