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
package org.apache.causeway.viewer.vaadin.ui.components.reference;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.combobox.ComboBox;

import jakarta.inject.Inject;

import org.springframework.core.annotation.Order;

import org.apache.causeway.applib.annotation.PriorityPrecedence;
import org.apache.causeway.applib.services.repository.RepositoryService;
import org.apache.causeway.core.metamodel.object.ManagedObject;
import org.apache.causeway.core.metamodel.object.MmTitleUtils;
import org.apache.causeway.viewer.commons.model.components.UiComponentFactory.ComponentRequest;
import org.apache.causeway.viewer.vaadin.ui.components.UiComponentHandlerVaa;

/**
 * Renders an object-reference property or parameter (a reference to an entity or
 * view-model that has no explicit choices) as a {@link ComboBox} populated from
 * {@link RepositoryService#allInstances(Class) all instances} of the referenced
 * type. References that <em>do</em> declare choices are handled earlier by the
 * choices factory; this is the fallback selector for free references.
 */
@org.springframework.stereotype.Component
@Order(PriorityPrecedence.LATE)
public class ObjectReferenceFieldFactory implements UiComponentHandlerVaa {

    private final RepositoryService repositoryService;

    @Inject
    public ObjectReferenceFieldFactory(final RepositoryService repositoryService) {
        this.repositoryService = repositoryService;
    }

    @Override
    public boolean isHandling(final ComponentRequest request) {
        var spec = request.getFeatureTypeSpec();
        return spec.isEntityOrViewModelOrAbstract() && !spec.isPlural();
    }

    @Override
    public Component handle(final ComponentRequest request) {
        var spec = request.getFeatureTypeSpec();
        var readOnly = request.disablingUiModelIfAny().isPresent();

        var items = new ArrayList<ManagedObject>();
        for (var pojo : repositoryService.allInstances(spec.getCorrespondingClass())) {
            items.add(ManagedObject.adaptSingular(spec, pojo));
        }

        var comboBox = new ComboBox<ManagedObject>(request.getFriendlyName());
        comboBox.setItemLabelGenerator(MmTitleUtils::titleOf);
        // type-to-filter (autocomplete) matching the title, evaluated on the server.
        comboBox.setItems(
                (item, filter) -> MmTitleUtils.titleOf(item).toLowerCase()
                        .contains(filter.toLowerCase()),
                items);
        comboBox.setClearButtonVisible(true);
        comboBox.setValue(matchCurrent(items, request));
        comboBox.setReadOnly(readOnly);
        if (!readOnly) {
            comboBox.addValueChangeListener(event ->
                    request.managedValue().getValue().setValue(event.getValue()));
        }
        return comboBox;
    }

    /** Selects the item whose pojo equals the current value, so the combo pre-selects it. */
    private static ManagedObject matchCurrent(final List<ManagedObject> items, final ComponentRequest request) {
        var current = request.managedValue().getValue().getValue();
        var currentPojo = current != null ? current.getPojo() : null;
        if (currentPojo == null) {
            return null;
        }
        for (var item : items) {
            if (Objects.equals(item.getPojo(), currentPojo)) {
                return item;
            }
        }
        return null;
    }
}
