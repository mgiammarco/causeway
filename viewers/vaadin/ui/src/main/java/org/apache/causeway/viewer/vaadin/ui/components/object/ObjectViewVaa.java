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
package org.apache.causeway.viewer.vaadin.ui.components.object;

import java.util.function.Consumer;

import com.vaadin.flow.component.HasComponents;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.formlayout.FormLayout.ResponsiveStep;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.FlexLayout.FlexWrap;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.TabSheet;

import org.apache.causeway.applib.annotation.Where;
import org.apache.causeway.applib.layout.component.ActionLayoutData;
import org.apache.causeway.applib.layout.component.CollectionLayoutData;
import org.apache.causeway.applib.layout.component.DomainObjectLayoutData;
import org.apache.causeway.applib.layout.component.FieldSet;
import org.apache.causeway.applib.layout.component.PropertyLayoutData;
import org.apache.causeway.applib.layout.grid.bootstrap.BSClearFix;
import org.apache.causeway.applib.layout.grid.bootstrap.BSCol;
import org.apache.causeway.applib.layout.grid.bootstrap.BSRow;
import org.apache.causeway.applib.layout.grid.bootstrap.BSTab;
import org.apache.causeway.applib.layout.grid.bootstrap.BSTabGroup;
import org.apache.causeway.core.metamodel.interactions.managed.ActionInteraction;
import org.apache.causeway.core.metamodel.interactions.managed.CollectionInteraction;
import org.apache.causeway.core.metamodel.interactions.managed.ManagedAction;
import org.apache.causeway.core.metamodel.interactions.managed.PropertyInteraction;
import org.apache.causeway.core.metamodel.object.ManagedObject;
import org.apache.causeway.core.metamodel.object.MmTitleUtils;
import org.apache.causeway.viewer.commons.model.components.UiComponentFactory;
import org.apache.causeway.viewer.commons.model.decorators.DisablingDecorator.DisablingDecorationModel;
import org.apache.causeway.viewer.commons.model.layout.UiGridLayout;
import org.apache.causeway.viewer.vaadin.model.util.Vaa;
import org.apache.causeway.viewer.vaadin.ui.components.UiComponentFactoryVaa;
import org.apache.causeway.viewer.vaadin.ui.components.collection.TableViewVaa;

/**
 * Renders a domain object's page: title, actions, fieldsets with properties,
 * tab groups and collections — driven by the object's bootstrap grid layout
 * via the {@link UiGridLayout.Visitor} (Visitor pattern: layout traversal is
 * in commons, only the Vaadin rendering lives here).
 */
public class ObjectViewVaa extends VerticalLayout {

    private static final long serialVersionUID = 1L;

    public static ObjectViewVaa fromObject(
            final UiComponentFactoryVaa uiComponentFactory,
            final Consumer<ManagedAction> actionEventHandler,
            final Consumer<ManagedObject> objectNavHandler,
            final ManagedObject managedObject) {
        return new ObjectViewVaa(uiComponentFactory, actionEventHandler, objectNavHandler, managedObject);
    }

    protected ObjectViewVaa(
            final UiComponentFactoryVaa uiComponentFactory,
            final Consumer<ManagedAction> actionEventHandler,
            final Consumer<ManagedObject> objectNavHandler,
            final ManagedObject managedObject) {

        var objectTitle = MmTitleUtils.titleOf(managedObject);

        var gridVisitor = new UiGridLayout.Visitor<HasComponents, TabSheet>(this) {

            @Override
            protected void onObjectTitle(final HasComponents container, final DomainObjectLayoutData domainObjectData) {
                Vaa.add(container, new H1(objectTitle));
            }

            @Override
            protected HasComponents newRow(final HasComponents container, final BSRow bsRow) {
                var uiRow = Vaa.add(container, new FlexLayout());
                uiRow.setWidthFull();
                uiRow.setFlexWrap(FlexWrap.WRAP);
                return uiRow;
            }

            @Override
            protected HasComponents newCol(final HasComponents container, final BSCol bsCol) {
                var uiCol = Vaa.add(container, new VerticalLayout());
                if (container instanceof FlexLayout flexLayout) {
                    flexLayout.setFlexGrow(bsCol.getSpan(), uiCol);
                }
                uiCol.setWidth(null);
                uiCol.setMinWidth(String.format("%dem", bsCol.getSpan() * 3));
                return uiCol;
            }

            @Override
            protected HasComponents newActionPanel(final HasComponents container) {
                var uiActionPanel = Vaa.add(container, new FlexLayout());
                uiActionPanel.setFlexWrap(FlexWrap.WRAP);
                uiActionPanel.setAlignItems(FlexComponent.Alignment.BASELINE);
                return uiActionPanel;
            }

            @Override
            protected TabSheet newTabGroup(final HasComponents container, final BSTabGroup tabGroupData) {
                var tabSheet = new TabSheet();
                container.add(tabSheet);
                tabSheet.setWidthFull();
                return tabSheet;
            }

            @Override
            protected HasComponents newTab(final TabSheet tabSheet, final BSTab tabData) {
                var tabContent = new VerticalLayout();
                tabContent.setWidthFull();
                tabSheet.add(tabData.getName(), tabContent);
                return tabContent;
            }

            @Override
            protected HasComponents newFieldSet(final HasComponents container, final FieldSet fieldSetData) {
                Vaa.add(container, new H2(fieldSetData.getName()));

                var actionBar = newActionPanel(container);
                for (var actionData : fieldSetData.getActions()) {
                    onAction(actionBar, actionData);
                }

                var uiFieldSet = Vaa.add(container, new FormLayout());
                uiFieldSet.setResponsiveSteps(new ResponsiveStep("0", 1));
                return uiFieldSet;
            }

            @Override
            protected void onClearfix(final HasComponents container, final BSClearFix clearFixData) {
                // not needed: FlexLayout wraps lines on its own
            }

            @Override
            protected void onAction(final HasComponents container, final ActionLayoutData actionData) {
                var interaction = ActionInteraction.start(managedObject, actionData.getId(), Where.OBJECT_FORMS);
                interaction.checkVisibility()
                        .getManagedAction()
                        .ifPresent(managedAction -> {
                            interaction.checkUsability();
                            Vaa.add(container, uiComponentFactory.buttonFor(
                                    new UiComponentFactory.ButtonRequest(
                                            managedAction,
                                            DisablingDecorationModel.of(interaction),
                                            actionEventHandler)));
                        });
            }

            @Override
            protected void onProperty(final HasComponents container, final PropertyLayoutData propertyData) {
                var interaction = PropertyInteraction.start(managedObject, propertyData.getId(), Where.OBJECT_FORMS);
                interaction.checkVisibility()
                        .getManagedProperty()
                        .ifPresent(managedProperty -> {
                            interaction.checkUsability();
                            var propertyNegotiation = managedProperty.startNegotiation();
                            Vaa.add(container, uiComponentFactory.componentFor(
                                    new UiComponentFactory.ComponentRequest(
                                            propertyNegotiation,
                                            managedProperty,
                                            DisablingDecorationModel.of(interaction))));

                            var actionBar = newActionPanel(container);
                            for (var actionData : propertyData.getActions()) {
                                onAction(actionBar, actionData);
                            }
                        });
            }

            @Override
            protected void onCollection(final HasComponents container, final CollectionLayoutData collectionData) {
                CollectionInteraction.start(managedObject, collectionData.getId(), Where.OBJECT_FORMS)
                        .checkVisibility()
                        .getManagedCollection()
                        .ifPresent(managedCollection -> {
                            Vaa.add(container, new H3(managedCollection.getFriendlyName()));

                            var actionBar = newActionPanel(container);
                            for (var actionData : collectionData.getActions()) {
                                onAction(actionBar, actionData);
                            }

                            Vaa.add(container, TableViewVaa.forDataTableInteractive(
                                    managedCollection.createDataTableModel(),
                                    objectNavHandler));
                        });
            }
        };

        UiGridLayout.forObject(managedObject)
                .ifPresentOrElse(
                        uiGridLayout -> uiGridLayout.visit(gridVisitor),
                        () -> add(new H1(objectTitle)));
        setWidthFull();
    }
}
