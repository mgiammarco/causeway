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
package org.apache.causeway.viewer.vaadin.demo;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.inject.Named;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

import org.apache.causeway.applib.annotation.Action;
import org.apache.causeway.applib.annotation.DomainObject;
import org.apache.causeway.applib.annotation.Editing;
import org.apache.causeway.applib.annotation.Nature;
import org.apache.causeway.applib.annotation.ObjectSupport;
import org.apache.causeway.applib.annotation.Property;

/**
 * A JPA entity exercising every supported field kind on its object page:
 * text ({@code title}/genre), number ({@code pageCount}), {@code BigDecimal}
 * ({@code price}), {@code boolean} ({@code inStock}), date ({@code published}),
 * enum choices ({@code genre}) and an object reference ({@code author}).
 */
@Entity
@Named("demo.Book")
@DomainObject(nature = Nature.ENTITY)
public class DemoBook {

    /** value-typed enum -> rendered as a dropdown (ComboBox) of its constants. */
    public enum Genre { PROGRAMMING, DESIGN, ARCHITECTURE, TESTING }

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Property(editing = Editing.ENABLED)
    private String title;

    /** object reference -> rendered as a ComboBox over all persisted authors. */
    @ManyToOne
    @Property(editing = Editing.ENABLED)
    private DemoAuthor author;

    @Property(editing = Editing.ENABLED)
    private LocalDate published;

    @Property(editing = Editing.ENABLED)
    private int pageCount;

    @Property(editing = Editing.ENABLED)
    private BigDecimal price;

    @Property(editing = Editing.ENABLED)
    private boolean inStock;

    @Property(editing = Editing.ENABLED)
    private Genre genre;

    @ObjectSupport
    public String title() {
        return title == null ? "(untitled)" : title;
    }

    public Long getId() {
        return id;
    }
    public void setId(final Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }
    public void setTitle(final String title) {
        this.title = title;
    }

    public DemoAuthor getAuthor() {
        return author;
    }
    public void setAuthor(final DemoAuthor author) {
        this.author = author;
    }

    public LocalDate getPublished() {
        return published;
    }
    public void setPublished(final LocalDate published) {
        this.published = published;
    }

    public int getPageCount() {
        return pageCount;
    }
    public void setPageCount(final int pageCount) {
        this.pageCount = pageCount;
    }

    public BigDecimal getPrice() {
        return price;
    }
    public void setPrice(final BigDecimal price) {
        this.price = price;
    }

    public boolean isInStock() {
        return inStock;
    }
    public void setInStock(final boolean inStock) {
        this.inStock = inStock;
    }

    public Genre getGenre() {
        return genre;
    }
    public void setGenre(final Genre genre) {
        this.genre = genre;
    }

    @Action
    public DemoBook rename(final String newTitle) {
        setTitle(newTitle);
        return this;
    }
}
