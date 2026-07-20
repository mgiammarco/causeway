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
package org.apache.causeway.viewer.vaadin.demo.petclinic.dom.visit;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.apache.causeway.applib.annotation.DomainObject;
import org.apache.causeway.applib.annotation.DomainObjectLayout;
import org.apache.causeway.applib.annotation.ObjectSupport;
import org.apache.causeway.applib.annotation.PropertyLayout;
import org.apache.causeway.applib.annotation.Publishing;
import org.apache.causeway.applib.jaxb.PersistentEntityAdapter;
import org.apache.causeway.applib.services.title.TitleService;
import org.apache.causeway.persistence.jpa.applib.integration.CausewayEntityListener;

import org.apache.causeway.viewer.vaadin.demo.petclinic.dom.pet.Pet;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Ported from {@code apache/causeway-app-petclinic} (module-visit) — exercises
 * a {@link LocalDateTime} property (date+time picker widget).
 */
@Entity
@Table(
        uniqueConstraints = {
                @UniqueConstraint(name = "Visit__pet_visitAt__UNQ", columnNames = { "pet_id", "visitAt" })
        }
)
@EntityListeners(CausewayEntityListener.class)
@Named("demo.petclinic.Visit")
@DomainObject(entityChangePublishing = Publishing.ENABLED)
@DomainObjectLayout()
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@XmlJavaTypeAdapter(PersistentEntityAdapter.class)
@ToString(onlyExplicitlyIncluded = true)
public class Visit implements Comparable<Visit> {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false)
    @Getter @Setter
    private Long id;

    @Version
    @Column(name = "version", nullable = false)
    @PropertyLayout(fieldSetId = "metadata", sequence = "999")
    @Getter @Setter
    private long version;

    public Visit(final Pet pet, final LocalDateTime visitAt) {
        this.pet = pet;
        this.visitAt = visitAt;
    }

    @ObjectSupport
    public String title() {
        return titleService.titleOf(getPet())
                + " @ "
                + getVisitAt().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm"));
    }

    @ManyToOne(optional = false)
    @JoinColumn(name = "pet_id")
    @PropertyLayout(fieldSetId = "identity", sequence = "1")
    @Getter @Setter
    private Pet pet;

    @Column(name = "visitAt", nullable = false)
    @Getter @Setter
    @PropertyLayout(fieldSetId = "identity", sequence = "2")
    private LocalDateTime visitAt;

    private static final Comparator<Visit> comparator =
            Comparator.comparing(Visit::getPet).thenComparing(Visit::getVisitAt);

    @Override
    public int compareTo(final Visit other) {
        return comparator.compare(this, other);
    }

    @Inject @Transient TitleService titleService;
}
