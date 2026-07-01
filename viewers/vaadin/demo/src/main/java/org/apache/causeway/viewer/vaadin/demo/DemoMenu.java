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
import java.util.List;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.apache.causeway.applib.annotation.Action;
import org.apache.causeway.applib.annotation.ActionLayout;
import org.apache.causeway.applib.annotation.DomainService;
import org.apache.causeway.applib.annotation.DomainServiceLayout;
import org.apache.causeway.applib.services.factory.FactoryService;
import org.apache.causeway.applib.services.repository.RepositoryService;

/**
 * Repository-backed menu: its actions list and create the JPA entities, so the
 * viewer is exercised against a real persistence store (including an object
 * reference — {@code createBook}'s {@code author} parameter and the book's
 * {@code author} property).
 */
@Named("demo.DemoMenu")
@DomainService
@DomainServiceLayout(named = "Library")
public class DemoMenu {

    @Inject private RepositoryService repositoryService;
    @Inject private FactoryService factoryService;

    @Action
    @ActionLayout(sequence = "1")
    public List<DemoBook> listBooks() {
        return repositoryService.allInstances(DemoBook.class);
    }

    @Action
    @ActionLayout(sequence = "2")
    public List<DemoAuthor> listAuthors() {
        return repositoryService.allInstances(DemoAuthor.class);
    }

    @Action
    @ActionLayout(sequence = "3")
    public DemoAuthor createAuthor(final String name) {
        var author = factoryService.detachedEntity(DemoAuthor.class);
        author.setName(name);
        return repositoryService.persist(author);
    }

    @Action
    @ActionLayout(sequence = "4")
    public DemoBook createBook(final String title, final DemoAuthor author) {
        return book(title, author, LocalDate.now(), 0, "0.00", false, DemoBook.Genre.PROGRAMMING);
    }

    /** choices for the {@code author} parameter (index 1) of {@link #createBook}. */
    public List<DemoAuthor> choices1CreateBook() {
        return listAuthors();
    }

    @Action
    @ActionLayout(sequence = "5", describedAs = "demonstrates editable Blob/Clob upload fields")
    public DemoUpload openUploadDemo() {
        return factoryService.viewModel(DemoUpload.class);
    }

    @Action
    @ActionLayout(sequence = "9", describedAs = "(re)create three sample authors and books")
    public List<DemoBook> populate() {
        var fowler = createAuthor("Martin Fowler");
        var evans = createAuthor("Eric Evans");
        var martin = createAuthor("Robert C. Martin");
        book("Refactoring", fowler, LocalDate.of(1999, 7, 8), 448, "54.99", true, DemoBook.Genre.PROGRAMMING);
        book("Domain-Driven Design", evans, LocalDate.of(2003, 8, 30), 560, "64.99", true, DemoBook.Genre.ARCHITECTURE);
        book("Clean Code", martin, LocalDate.of(2008, 8, 1), 464, "44.99", false, DemoBook.Genre.PROGRAMMING);
        return listBooks();
    }

    private DemoBook book(final String title, final DemoAuthor author, final LocalDate published,
            final int pageCount, final String price, final boolean inStock, final DemoBook.Genre genre) {
        var book = factoryService.detachedEntity(DemoBook.class);
        book.setTitle(title);
        book.setAuthor(author);
        book.setPublished(published);
        book.setPageCount(pageCount);
        book.setPrice(new BigDecimal(price));
        book.setInStock(inStock);
        book.setGenre(genre);
        return repositoryService.persist(book);
    }
}
