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
package org.apache.causeway.viewer.vaadin.ui.binding;

import com.vaadin.flow.component.textfield.TextField;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import org.apache.causeway.commons.binding.Bindable;
import org.apache.causeway.commons.binding.ChangeListener;
import org.apache.causeway.commons.binding.Observable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BindingsVaaTest {

    @SuppressWarnings("unchecked")
    @Test
    void bindParsableText_initializesFieldFromModel_andPropagatesModelChanges() {
        var field = new TextField("Name");
        var parsableText = (Bindable<String>) Mockito.mock(Bindable.class);
        var validationMessage = (Observable<String>) Mockito.mock(Observable.class);
        Mockito.when(parsableText.getValue()).thenReturn("initial");

        BindingsVaa.bindParsableText(field, parsableText, validationMessage, false);

        assertEquals("initial", field.getValue());

        // model -> UI propagation: capture the listener registered on the model
        var listenerCaptor = ArgumentCaptor.forClass(ChangeListener.class);
        Mockito.verify(parsableText).addListener(listenerCaptor.capture());
        listenerCaptor.getValue().changed(parsableText, "initial", "changed");
        assertEquals("changed", field.getValue());

        // UI -> model propagation
        field.setValue("typed");
        Mockito.verify(parsableText, Mockito.atLeastOnce()).setValue("typed");
    }

    @SuppressWarnings("unchecked")
    @Test
    void bindParsableText_readOnly_doesNotRegisterListeners() {
        var field = new TextField("Name");
        var parsableText = (Bindable<String>) Mockito.mock(Bindable.class);
        var validationMessage = (Observable<String>) Mockito.mock(Observable.class);
        Mockito.when(parsableText.getValue()).thenReturn("ro");

        BindingsVaa.bindParsableText(field, parsableText, validationMessage, true);

        assertEquals("ro", field.getValue());
        assertTrue(field.isReadOnly());
        Mockito.verify(parsableText, Mockito.never()).addListener(Mockito.<ChangeListener<? super String>>any());
    }

    @SuppressWarnings("unchecked")
    @Test
    void bindParsableText_validationMessage_drivesErrorState() {
        var field = new TextField("Name");
        var parsableText = (Bindable<String>) Mockito.mock(Bindable.class);
        var validationMessage = (Observable<String>) Mockito.mock(Observable.class);
        Mockito.when(parsableText.getValue()).thenReturn("x");

        BindingsVaa.bindParsableText(field, parsableText, validationMessage, false);

        var listenerCaptor = ArgumentCaptor.forClass(ChangeListener.class);
        Mockito.verify(validationMessage).addListener(listenerCaptor.capture());

        listenerCaptor.getValue().changed(validationMessage, null, "must not be empty");
        assertTrue(field.isInvalid());
        assertEquals("must not be empty", field.getErrorMessage());

        listenerCaptor.getValue().changed(validationMessage, "must not be empty", "");
        assertTrue(!field.isInvalid());
    }
}
