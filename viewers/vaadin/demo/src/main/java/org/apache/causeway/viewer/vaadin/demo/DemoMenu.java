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

import java.time.LocalDate;
import java.util.List;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.apache.causeway.applib.annotation.Action;
import org.apache.causeway.applib.annotation.ActionLayout;
import org.apache.causeway.applib.annotation.DomainService;
import org.apache.causeway.applib.annotation.DomainServiceLayout;
import org.apache.causeway.applib.services.factory.FactoryService;

/**
 * Menu service: its actions populate the metamodel-driven menubar of the
 * Vaadin viewer. Exercises a standalone collection (Grid), an object page,
 * and an action with parameters (dialog).
 */
@Named("demo.DemoMenu")
@DomainService
@DomainServiceLayout(named = "Library")
public class DemoMenu {

    @Inject private FactoryService factoryService;

    @Action
    @ActionLayout(sequence = "1")
    public List<DemoBook> listBooks() {
        return List.of(
                book("Refactoring", "Martin Fowler", LocalDate.of(1999, 7, 8), 448, "54.99", true, DemoBook.Genre.PROGRAMMING),
                book("Domain-Driven Design", "Eric Evans", LocalDate.of(2003, 8, 30), 560, "64.99", true, DemoBook.Genre.ARCHITECTURE),
                book("Clean Code", "Robert C. Martin", LocalDate.of(2008, 8, 1), 464, "44.99", false, DemoBook.Genre.PROGRAMMING));
    }

    @Action
    @ActionLayout(sequence = "2")
    public DemoBook createBook(final String title, final String author) {
        return book(title, author, LocalDate.now(), 0, "0.00", false, DemoBook.Genre.PROGRAMMING);
    }

    private DemoBook book(final String title, final String author, final LocalDate published,
            final int pageCount, final String price, final boolean inStock, final DemoBook.Genre genre) {
        var demoBook = factoryService.viewModel(DemoBook.class);
        demoBook.setTitle(title);
        demoBook.setAuthor(author);
        demoBook.setPublished(published);
        demoBook.setPageCount(pageCount);
        demoBook.setPrice(new java.math.BigDecimal(price));
        demoBook.setInStock(inStock);
        demoBook.setGenre(genre);
        return demoBook;
    }
}
