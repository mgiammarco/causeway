# Causeway Vaadin Viewer Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a first-class Vaadin Flow viewer to Apache Causeway as a new `viewers/vaadin` module family (model / ui / viewer), feature-scoped to: login via Causeway's own AuthenticationManager, metamodel-driven menubar, domain-object page rendered from the layout grid, collection tables, and action invocation with parameter dialogs.

**Architecture:** The viewer reuses the UI-framework-agnostic abstractions already in `viewers/commons` — `UiComponentFactory<B,C>` (Factory + Chain of Responsibility), `UiGridLayout.Visitor` (Visitor), the decorator models (`DisablingDecorationModel`), and the menu/header records (`HeaderUiModel`, `NavbarUiModel`, `MenuVisitor`). All Vaadin-specific code is a thin adaptation layer from those models to Vaadin Flow components. Three Maven modules mirror the Wicket viewer's decomposition: `model` (UI-context contracts, no heavy Vaadin deps), `ui` (components, views, factories), `viewer` (Spring bootstrapping + servlet integration). The retired incubator Vaadin viewer (removed in commit `ec8002b2989`, CAUSEWAY-3400) is the design reference; its code is recoverable via `git show 'ec8002b2989^:incubator/viewers/vaadin/<path>'` but uses outdated APIs (javax.*, Vaadin 23, `DataTableModel`) — every class in this plan is already modernized to current APIs, so copy from THIS PLAN, not from git history.

**Tech Stack:** Java 17, Spring Boot 3 (jakarta), Vaadin platform 24.8.x (via `vaadin-bom`), Causeway `4.0.0-SNAPSHOT`, JUnit 5 + Mockito (via `spring-boot-starter-test`), Lombok (already configured repo-wide).

**Verified current-API facts used throughout (do not "correct" them back to older forms):**
- `UiComponentFactory.ButtonRequest(managedAction, disablingUiModelIfAny, actionEventHandler)` and `ComponentRequest(managedValue, managedFeature, disablingUiModelIfAny)` are **records** (`viewers/commons/model/.../components/UiComponentFactory.java`).
- `UiGridLayout.forObject(ManagedObject)` returns `Optional<UiGridLayout>`; visitor methods: `newActionPanel, newRow, newCol, newTabGroup, newTab, newFieldSet, onObjectTitle, onClearfix, onAction, onProperty, onCollection` (`viewers/commons/model/.../layout/UiGridLayout.java`).
- Tabular API is `org.apache.causeway.core.metamodel.tabular.DataTableInteractive` (NOT `DataTableModel`): `dataColumnsObservable()`, `dataRowsFilteredAndSortedObservable()`, `titleObservable()`, static `forAction(...)`; `DataRow.rowElement()`, `DataRow.getCellElementsForColumn(DataColumn)`; `DataColumn.columnFriendlyNameObservable()`.
- `ChainOfResponsibility<X,R>` is a record; construct `new ChainOfResponsibility<>(name, List<Handler>)`, call `handleElseFail(request)`.
- `ManagedAction.invoke(Can<ManagedObject>)` returns `Railway<InteractionVeto, ManagedObject>`; `startParameterNegotiation()` returns `ParameterNegotiationModel` with `getParamModels()`.
- `AuthenticationManager.authenticate(AuthenticationRequest)` returns `InteractionContext`; request impl `AuthenticationRequestPassword`.
- `ManagedValue`: `getValue()` is `Bindable<ManagedObject>`, `getValueAsParsableText()` is `Bindable<String>`, `getValueAsTitle()`/`getValidationMessage()` are `Observable<String>`; commons `Observable.addListener(ChangeListener)` with `changed(observable, oldV, newV)`.
- `MmTitleUtils.titleOf(ManagedObject)` (plural "Utils").
- `MetaModelContext.getHomePageAdapter()`, `getAuthenticationManager()`.
- `LogoutHandler` (core/security) declares a single method `void logout()`.
- Aggregation: `core/pom.xml` lists `<module>../viewers/wicket</module>` etc. (lines ~73-76). Viewer poms use parent `org.apache.causeway:causeway-parent` (relativePath `../../parent/pom.xml`) and groupId `org.apache.causeway.viewer`. The bom lists each artifact in `bom/pom.xml` dependencyManagement (wicket entries around line 562).

**Build note:** the first `mvn install -pl viewers/vaadin -am` compiles core upstream modules; expect 10–20 min. Later tasks rebuild only `viewers/vaadin/*` (seconds). All commands run from the repo root. Append `-DskipTests -Dmaven.javadoc.skip=true -Denforcer.skip=true` if the full reactor build trips unrelated checks; record it in the commit message if used.

**Out of scope (phase 2+, do not implement now):** blob/clob upload fields, markup/Prism rendering, bookmarks/breadcrumbs, hint store, theme picker, i18n parity, property inline editing beyond text/temporal, multi-select tables, Vaadin production-mode frontend bundling.

---

## File Structure

```
viewers/vaadin/pom.xml                                  (parent, packaging pom, imports vaadin-bom)
viewers/vaadin/model/pom.xml                            causeway-viewer-vaadin-model
viewers/vaadin/model/src/main/java/org/apache/causeway/viewer/vaadin/model/
    CausewayModuleViewerVaadinModel.java                Spring @Configuration for the module
    context/UiContextVaa.java                           contract: routing + page replacement (interface)
    context/MemberInvocationHandler.java                contract: ManagedObject/action-result -> UI component
    util/Vaa.java                                       small static helpers (container add, button)
viewers/vaadin/ui/pom.xml                               causeway-viewer-vaadin-ui
viewers/vaadin/ui/src/main/java/org/apache/causeway/viewer/vaadin/ui/
    CausewayModuleViewerVaadinUi.java                   Spring @Configuration, imports Model + CommonsServices
    binding/BindingsVaa.java                            ManagedValue <-> Vaadin field binding helpers
    components/UiComponentHandlerVaa.java               marker: UiComponentFactory.Handler<Component>
    components/UiComponentFactoryVaa.java               chain-of-responsibility component factory
    components/text/TextFieldFactory.java               String attribute -> TextField
    components/temporal/TemporalFieldFactory.java       LocalDate/LocalDateTime -> DatePicker/DateTimePicker
    components/other/FallbackFieldFactory.java          everything else -> readonly TextArea (chain tail)
    components/collection/TableViewVaa.java             DataTableInteractive -> Grid
    components/object/ObjectViewVaa.java                UiGridLayout.Visitor -> object page
    components/action/ActionForm.java                   parameter negotiation form
    components/action/ActionDialog.java                 modal Dialog hosting ActionForm
    auth/AuthSessionStoreUtil.java                      InteractionContext <-> HTTP session store
    auth/VaadinAuthenticationHandler.java               AppShellConfigurator + route guard + login API
    auth/LogoutHandlerVaa.java                          core LogoutHandler implementation
    pages/login/VaadinLoginView.java                    @Route("login") LoginForm view
    pages/main/MainViewVaa.java                         @Route("") AppLayout shell
    pages/main/MenuBuilderVaa.java                      MenuVisitor -> Vaadin MenuBar
    pages/main/UiActionHandlerVaa.java                  action click -> dialog/invoke -> route result
    pages/main/UiContextVaaDefault.java                 default UiContextVaa implementation
viewers/vaadin/ui/src/test/java/org/apache/causeway/viewer/vaadin/ui/   (unit tests, see tasks)
viewers/vaadin/viewer/pom.xml                           causeway-viewer-vaadin-viewer
viewers/vaadin/viewer/src/main/java/org/apache/causeway/viewer/vaadin/viewer/
    CausewayModuleViewerVaadinViewer.java               root @Configuration, servlet registration
    CausewayServletForVaadin.java                       SpringServlet wrapping requests in an Interaction
viewers/vaadin/viewer/src/main/resources/vaadin.properties
viewers/vaadin/viewer/src/test/java/org/apache/causeway/viewer/vaadin/viewer/
    VaadinModuleContextLoadsTest.java                   Spring smoke test
Modify: core/pom.xml                                    add <module>../viewers/vaadin</module>
Modify: bom/pom.xml                                     add 4 dependencyManagement entries
```

Each class has one responsibility; the dependency direction is strictly `viewer -> ui -> model -> (viewers/commons, core/metamodel)`. No class in `model` may import from `ui` or `viewer`.

---

### Task 1: Maven module skeleton

**Files:**
- Create: `viewers/vaadin/pom.xml`
- Create: `viewers/vaadin/model/pom.xml`
- Create: `viewers/vaadin/ui/pom.xml`
- Create: `viewers/vaadin/viewer/pom.xml`
- Modify: `core/pom.xml` (module list, after `<module>../viewers/graphql</module>`, around line 76)
- Modify: `bom/pom.xml` (dependencyManagement, after the `causeway-viewer-wicket-viewer` entry, around line 590)

- [ ] **Step 1: Create `viewers/vaadin/pom.xml`** (license header: copy the standard ASF XML header verbatim from `viewers/wicket/pom.xml` into every new pom)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!-- (ASF license header here, copied from viewers/wicket/pom.xml) -->
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.apache.causeway</groupId>
        <artifactId>causeway-parent</artifactId>
        <version>4.0.0-SNAPSHOT</version>
        <relativePath>../../parent/pom.xml</relativePath>
    </parent>

    <groupId>org.apache.causeway.viewer</groupId>
    <artifactId>causeway-viewer-vaadin</artifactId>
    <name>Apache Causeway Viewer - Vaadin</name>
    <description>Vaadin Flow based viewer for Apache Causeway.</description>

    <packaging>pom</packaging>

    <properties>
        <vaadin.version>24.8.4</vaadin.version>
    </properties>

    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>com.vaadin</groupId>
                <artifactId>vaadin-bom</artifactId>
                <version>${vaadin.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <modules>
        <module>model</module>
        <module>ui</module>
        <module>viewer</module>
    </modules>
</project>
```

- [ ] **Step 2: Create `viewers/vaadin/model/pom.xml`**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!-- (ASF license header) -->
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.apache.causeway.viewer</groupId>
        <artifactId>causeway-viewer-vaadin</artifactId>
        <version>4.0.0-SNAPSHOT</version>
    </parent>

    <artifactId>causeway-viewer-vaadin-model</artifactId>
    <name>Apache Causeway Viewer - Vaadin (Model)</name>

    <dependencies>
        <dependency>
            <groupId>org.apache.causeway.viewer</groupId>
            <artifactId>causeway-viewer-commons-model</artifactId>
        </dependency>
        <dependency>
            <groupId>com.vaadin</groupId>
            <artifactId>vaadin-core</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
</project>
```

- [ ] **Step 3: Create `viewers/vaadin/ui/pom.xml`**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!-- (ASF license header) -->
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.apache.causeway.viewer</groupId>
        <artifactId>causeway-viewer-vaadin</artifactId>
        <version>4.0.0-SNAPSHOT</version>
    </parent>

    <artifactId>causeway-viewer-vaadin-ui</artifactId>
    <name>Apache Causeway Viewer - Vaadin (UI Components)</name>

    <dependencies>
        <dependency>
            <groupId>org.apache.causeway.viewer</groupId>
            <artifactId>causeway-viewer-vaadin-model</artifactId>
        </dependency>
        <dependency>
            <groupId>org.apache.causeway.viewer</groupId>
            <artifactId>causeway-viewer-commons-services</artifactId>
        </dependency>
        <dependency>
            <groupId>org.apache.causeway.core</groupId>
            <artifactId>causeway-core-security</artifactId>
        </dependency>
        <dependency>
            <groupId>com.vaadin</groupId>
            <artifactId>vaadin-spring-boot-starter</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
</project>
```

- [ ] **Step 4: Create `viewers/vaadin/viewer/pom.xml`**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!-- (ASF license header) -->
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.apache.causeway.viewer</groupId>
        <artifactId>causeway-viewer-vaadin</artifactId>
        <version>4.0.0-SNAPSHOT</version>
    </parent>

    <artifactId>causeway-viewer-vaadin-viewer</artifactId>
    <name>Apache Causeway Viewer - Vaadin (Viewer)</name>

    <dependencies>
        <dependency>
            <groupId>org.apache.causeway.viewer</groupId>
            <artifactId>causeway-viewer-vaadin-ui</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.apache.causeway.core</groupId>
            <artifactId>causeway-core-runtimeservices</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.apache.causeway.security</groupId>
            <artifactId>causeway-security-bypass</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
</project>
```

(If `causeway-security-bypass` has a different groupId, check `security/bypass/pom.xml` and match it.)

- [ ] **Step 5: Register the aggregator module.** In `core/pom.xml`, after `<module>../viewers/graphql</module>` add:

```xml
        <module>../viewers/vaadin</module>
```

- [ ] **Step 6: Register bom entries.** In `bom/pom.xml`, immediately after the `causeway-viewer-wicket-viewer` dependencyManagement entry, add (mirror the exact formatting of the wicket entries):

```xml
            <dependency>
                <groupId>org.apache.causeway.viewer</groupId>
                <artifactId>causeway-viewer-vaadin</artifactId>
                <version>4.0.0-SNAPSHOT</version>
                <type>pom</type>
            </dependency>
            <dependency>
                <groupId>org.apache.causeway.viewer</groupId>
                <artifactId>causeway-viewer-vaadin-model</artifactId>
                <version>4.0.0-SNAPSHOT</version>
            </dependency>
            <dependency>
                <groupId>org.apache.causeway.viewer</groupId>
                <artifactId>causeway-viewer-vaadin-ui</artifactId>
                <version>4.0.0-SNAPSHOT</version>
            </dependency>
            <dependency>
                <groupId>org.apache.causeway.viewer</groupId>
                <artifactId>causeway-viewer-vaadin-viewer</artifactId>
                <version>4.0.0-SNAPSHOT</version>
            </dependency>
```

(If the wicket entries use `${project.version}` instead of a literal version, use that.)

- [ ] **Step 7: Validate the reactor**

Run: `mvn -q validate -pl viewers/vaadin -am -DskipTests`
Expected: `BUILD SUCCESS` (no sources yet, validation of pom graph only).

- [ ] **Step 8: First full dependency build (slow, once)**

Run: `mvn -q install -pl viewers/vaadin -am -DskipTests -Dmaven.javadoc.skip=true`
Expected: `BUILD SUCCESS`. This installs core + viewers/commons snapshots locally so all later tasks rebuild only `viewers/vaadin/*`.

- [ ] **Step 9: Commit**

```bash
git add viewers/vaadin core/pom.xml bom/pom.xml
git commit -m "CAUSEWAY-VAADIN: add viewers/vaadin maven module skeleton (model/ui/viewer)"
```

---

### Task 2: `model` module — contracts and helpers

**Files:**
- Create: `viewers/vaadin/model/src/main/java/org/apache/causeway/viewer/vaadin/model/CausewayModuleViewerVaadinModel.java`
- Create: `viewers/vaadin/model/src/main/java/org/apache/causeway/viewer/vaadin/model/context/UiContextVaa.java`
- Create: `viewers/vaadin/model/src/main/java/org/apache/causeway/viewer/vaadin/model/context/MemberInvocationHandler.java`
- Create: `viewers/vaadin/model/src/main/java/org/apache/causeway/viewer/vaadin/model/util/Vaa.java`
- Test: `viewers/vaadin/model/src/test/java/org/apache/causeway/viewer/vaadin/model/util/VaaTest.java`

Every new `.java` file in this plan starts with the standard ASF license header — copy it verbatim from any existing source file, e.g. `viewers/commons/model/src/main/java/org/apache/causeway/viewer/commons/model/layout/UiGridLayout.java`.

- [ ] **Step 1: Write the failing test**

```java
package org.apache.causeway.viewer.vaadin.model.util;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class VaaTest {

    @Test
    void add_attachesChildAndReturnsIt() {
        var parent = new Div();
        var child = new Span("hello");

        var returned = Vaa.add(parent, child);

        assertSame(child, returned);
        assertEquals(1, parent.getChildren().count());
    }

    @Test
    void newButton_carriesLabel() {
        var button = Vaa.newButton("Do it");
        assertEquals("Do it", button.getText());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q test -pl viewers/vaadin/model -Dtest=VaaTest`
Expected: COMPILATION ERROR — `Vaa` does not exist.

- [ ] **Step 3: Write `Vaa.java`**

```java
package org.apache.causeway.viewer.vaadin.model.util;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasComponents;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;

import lombok.experimental.UtilityClass;

/**
 * Small static helpers for fluent Vaadin component tree construction.
 */
@UtilityClass
public class Vaa {

    /** Adds {@code component} to {@code container} and returns it, to allow fluent chaining. */
    public <T extends Component> T add(final HasComponents container, final T component) {
        container.add(component);
        return component;
    }

    public Button newButton(final String label) {
        var button = new Button(label);
        button.addThemeVariants(ButtonVariant.LUMO_SMALL);
        return button;
    }
}
```

- [ ] **Step 4: Write `MemberInvocationHandler.java`**

```java
package org.apache.causeway.viewer.vaadin.model.context;

import org.apache.causeway.commons.collections.Can;
import org.apache.causeway.core.metamodel.interactions.managed.ManagedAction;
import org.apache.causeway.core.metamodel.object.ManagedObject;

/**
 * Renders domain artifacts to UI components of type {@code T}.
 * <p>
 * Implemented by the application shell; consumed by routing/action handling,
 * which must not depend on concrete views (Dependency Inversion).
 */
public interface MemberInvocationHandler<T> {

    /** Renders a domain object (typically as an object page). */
    T handle(ManagedObject object);

    /** Renders an action result (scalar, object or collection). */
    T handle(ManagedAction managedAction, Can<ManagedObject> params, ManagedObject actionResult);
}
```

- [ ] **Step 5: Write `UiContextVaa.java`**

```java
package org.apache.causeway.viewer.vaadin.model.context;

import java.util.function.Consumer;

import com.vaadin.flow.component.Component;

import org.apache.causeway.applib.services.iactnlayer.InteractionService;
import org.apache.causeway.commons.collections.Can;
import org.apache.causeway.core.metamodel.interactions.managed.ManagedAction;
import org.apache.causeway.core.metamodel.object.ManagedObject;

/**
 * Mediates between domain-level events (navigate to object, show action result)
 * and the application shell that swaps page content.
 */
public interface UiContextVaa {

    InteractionService getInteractionService();

    /** Registered by the application shell: receives the new page content. */
    void setNewPageHandler(Consumer<Component> newPageHandler);

    /** Registered by the application shell: knows how to render domain artifacts. */
    void setPageFactory(MemberInvocationHandler<Component> pageFactory);

    /** Renders {@code object} and hands it to the page handler. */
    void route(ManagedObject object);

    /** Renders an action result and hands it to the page handler. */
    void route(ManagedAction managedAction, Can<ManagedObject> params, ManagedObject actionResult);
}
```

- [ ] **Step 6: Write `CausewayModuleViewerVaadinModel.java`**

```java
package org.apache.causeway.viewer.vaadin.model;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import org.apache.causeway.viewer.commons.model.CausewayModuleViewerCommonsModel;

@Configuration
@Import({
        CausewayModuleViewerCommonsModel.class,
})
public class CausewayModuleViewerVaadinModel {
    public static final String NAMESPACE = "causeway.viewer.vaadin";
}
```

- [ ] **Step 7: Run tests to verify they pass**

Run: `mvn -q test -pl viewers/vaadin/model`
Expected: `Tests run: 2, Failures: 0` — PASS.

- [ ] **Step 8: Commit**

```bash
git add viewers/vaadin/model
git commit -m "CAUSEWAY-VAADIN: model module - UiContextVaa, MemberInvocationHandler, Vaa helpers"
```

---

### Task 3: `ui` module base — component factory chain + fallback

**Files:**
- Create: `viewers/vaadin/ui/src/main/java/org/apache/causeway/viewer/vaadin/ui/CausewayModuleViewerVaadinUi.java`
- Create: `viewers/vaadin/ui/src/main/java/org/apache/causeway/viewer/vaadin/ui/components/UiComponentHandlerVaa.java`
- Create: `viewers/vaadin/ui/src/main/java/org/apache/causeway/viewer/vaadin/ui/components/UiComponentFactoryVaa.java`
- Create: `viewers/vaadin/ui/src/main/java/org/apache/causeway/viewer/vaadin/ui/components/other/FallbackFieldFactory.java`
- Test: `viewers/vaadin/ui/src/test/java/org/apache/causeway/viewer/vaadin/ui/components/UiComponentFactoryVaaTest.java`

- [ ] **Step 1: Write the failing test**

```java
package org.apache.causeway.viewer.vaadin.ui.components;

import java.util.List;
import java.util.Optional;

import com.vaadin.flow.component.textfield.TextArea;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import org.apache.causeway.commons.binding.Observable;
import org.apache.causeway.core.metamodel.interactions.managed.ManagedFeature;
import org.apache.causeway.core.metamodel.interactions.managed.ManagedValue;
import org.apache.causeway.viewer.commons.model.components.UiComponentFactory;
import org.apache.causeway.viewer.vaadin.ui.components.other.FallbackFieldFactory;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class UiComponentFactoryVaaTest {

    @SuppressWarnings("unchecked")
    static UiComponentFactory.ComponentRequest requestWithTitle(final String title) {
        var managedValue = Mockito.mock(ManagedValue.class);
        var managedFeature = Mockito.mock(ManagedFeature.class);
        var titleObservable = (Observable<String>) Mockito.mock(Observable.class);
        Mockito.when(titleObservable.getValue()).thenReturn(title);
        Mockito.when(managedValue.getValueAsTitle()).thenReturn(titleObservable);
        Mockito.when(managedFeature.getFriendlyName()).thenReturn("Some Feature");
        return new UiComponentFactory.ComponentRequest(managedValue, managedFeature, Optional.empty());
    }

    @Test
    void chain_fallsBackToReadonlyTextArea_forUnhandledType() {
        var factory = new UiComponentFactoryVaa(List.of(new FallbackFieldFactory()));

        var component = factory.componentFor(requestWithTitle("some value"));

        assertInstanceOf(TextArea.class, component);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q test -pl viewers/vaadin/ui -Dtest=UiComponentFactoryVaaTest`
Expected: COMPILATION ERROR — `UiComponentFactoryVaa` does not exist.

- [ ] **Step 3: Write `UiComponentHandlerVaa.java`**

```java
package org.apache.causeway.viewer.vaadin.ui.components;

import com.vaadin.flow.component.Component;

import org.apache.causeway.viewer.commons.model.components.UiComponentFactory;

/**
 * Marker for all Vaadin field factories participating in the
 * {@link UiComponentFactoryVaa} chain of responsibility.
 */
public interface UiComponentHandlerVaa extends UiComponentFactory.Handler<Component> {
}
```

- [ ] **Step 4: Write `UiComponentFactoryVaa.java`**

```java
package org.apache.causeway.viewer.vaadin.ui.components;

import java.util.List;

import com.vaadin.flow.component.Component;

import jakarta.inject.Inject;

import org.springframework.stereotype.Service;

import org.apache.causeway.commons.handler.ChainOfResponsibility;
import org.apache.causeway.commons.internal.exceptions._Exceptions;
import org.apache.causeway.viewer.commons.model.components.UiComponentFactory;
import org.apache.causeway.viewer.vaadin.model.util.Vaa;

/**
 * Creates Vaadin components for buttons, properties and parameters.
 * Field creation is delegated to a chain of {@link UiComponentHandlerVaa}
 * handlers, ordered by their Spring {@code @Order}/{@code @Priority}.
 */
@Service
public class UiComponentFactoryVaa implements UiComponentFactory<Component, Component> {

    private final ChainOfResponsibility<ComponentRequest, Component> chainOfHandlers;

    @Inject
    public UiComponentFactoryVaa(final List<UiComponentHandlerVaa> handlers) {
        this.chainOfHandlers = new ChainOfResponsibility<>("UiComponentFactoryVaa", handlers);
    }

    @Override
    public Component buttonFor(final ButtonRequest request) {
        var managedAction = request.managedAction();
        var uiButton = Vaa.newButton(managedAction.getFriendlyName());

        request.disablingUiModelIfAny().ifPresentOrElse(
                disabling -> uiButton.setEnabled(false),
                () -> uiButton.addClickListener(event ->
                        request.actionEventHandler().accept(managedAction)));
        return uiButton;
    }

    @Override
    public Component componentFor(final ComponentRequest request) {
        return chainOfHandlers.handleElseFail(request);
    }

    @Override
    public Component parameterFor(final ComponentRequest request) {
        return chainOfHandlers.handleElseFail(request);
    }

    @Override
    public LabelAndPosition<Component> labelFor(final ComponentRequest request) {
        throw _Exceptions.unsupportedOperation(
                "not needed for Vaadin: field components carry their own label");
    }
}
```

- [ ] **Step 5: Write `FallbackFieldFactory.java`**

```java
package org.apache.causeway.viewer.vaadin.ui.components.other;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.textfield.TextArea;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component as SpringComponent; // see note below

import org.apache.causeway.applib.annotation.PriorityPrecedence;
import org.apache.causeway.viewer.commons.model.components.UiComponentFactory.ComponentRequest;
import org.apache.causeway.viewer.vaadin.ui.components.UiComponentHandlerVaa;

/**
 * Chain tail: renders any otherwise unhandled value as a readonly text area
 * showing the value's title. Guarantees the chain always produces a component.
 */
@org.springframework.stereotype.Component
@Order(PriorityPrecedence.LAST)
public class FallbackFieldFactory implements UiComponentHandlerVaa {

    @Override
    public boolean isHandling(final ComponentRequest request) {
        return true; // chain tail
    }

    @Override
    public Component handle(final ComponentRequest request) {
        var uiField = new TextArea(request.getFriendlyName());
        uiField.setValue(java.util.Objects.toString(
                request.managedValue().getValueAsTitle().getValue(), ""));
        uiField.setReadOnly(true);
        return uiField;
    }
}
```

Note: Java has no import alias — the line `import org.springframework.stereotype.Component as SpringComponent` above is illegal; use the fully-qualified annotation `@org.springframework.stereotype.Component` exactly as shown and do NOT import Spring's `Component` (it clashes with Vaadin's `Component`). Delete the bogus import line.

- [ ] **Step 6: Write `CausewayModuleViewerVaadinUi.java`**

```java
package org.apache.causeway.viewer.vaadin.ui;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import org.apache.causeway.viewer.commons.services.CausewayModuleViewerCommonsServices;
import org.apache.causeway.viewer.vaadin.model.CausewayModuleViewerVaadinModel;
import org.apache.causeway.viewer.vaadin.ui.components.UiComponentFactoryVaa;
import org.apache.causeway.viewer.vaadin.ui.components.other.FallbackFieldFactory;

@Configuration
@Import({
        // modules
        CausewayModuleViewerVaadinModel.class,
        CausewayModuleViewerCommonsServices.class,

        // @Service & @Component beans of this module
        UiComponentFactoryVaa.class,
        FallbackFieldFactory.class,
})
public class CausewayModuleViewerVaadinUi {
}
```

(Each later task that adds a Spring bean to the `ui` module must also add it to this `@Import` list — the tasks repeat this explicitly.)

- [ ] **Step 7: Run test to verify it passes**

Run: `mvn -q test -pl viewers/vaadin/ui -Dtest=UiComponentFactoryVaaTest`
Expected: `Tests run: 1, Failures: 0` — PASS.

- [ ] **Step 8: Commit**

```bash
git add viewers/vaadin/ui
git commit -m "CAUSEWAY-VAADIN: ui module - UiComponentFactoryVaa chain with fallback handler"
```

---

### Task 4: Text binding + `TextFieldFactory`

**Files:**
- Create: `viewers/vaadin/ui/src/main/java/org/apache/causeway/viewer/vaadin/ui/binding/BindingsVaa.java`
- Create: `viewers/vaadin/ui/src/main/java/org/apache/causeway/viewer/vaadin/ui/components/text/TextFieldFactory.java`
- Modify: `viewers/vaadin/ui/src/main/java/org/apache/causeway/viewer/vaadin/ui/CausewayModuleViewerVaadinUi.java` (add `TextFieldFactory.class` to `@Import`)
- Test: `viewers/vaadin/ui/src/test/java/org/apache/causeway/viewer/vaadin/ui/components/text/TextFieldFactoryTest.java`

- [ ] **Step 1: Write the failing test**

```java
package org.apache.causeway.viewer.vaadin.ui.components.text;

import java.util.Optional;
import java.util.UUID;

import com.vaadin.flow.component.textfield.TextField;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import org.apache.causeway.commons.binding.Bindable;
import org.apache.causeway.commons.binding.Observable;
import org.apache.causeway.core.metamodel.interactions.managed.ManagedFeature;
import org.apache.causeway.core.metamodel.interactions.managed.ManagedValue;
import org.apache.causeway.viewer.commons.model.components.UiComponentFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TextFieldFactoryTest {

    final TextFieldFactory factory = new TextFieldFactory();

    @SuppressWarnings("unchecked")
    UiComponentFactory.ComponentRequest stringRequest(final String currentText) {
        var managedValue = Mockito.mock(ManagedValue.class);
        var managedFeature = Mockito.mock(ManagedFeature.class);
        var parsableText = (Bindable<String>) Mockito.mock(Bindable.class);
        var validationMessage = (Observable<String>) Mockito.mock(Observable.class);
        Mockito.when(parsableText.getValue()).thenReturn(currentText);
        Mockito.when(managedValue.getValueAsParsableText()).thenReturn(parsableText);
        Mockito.when(managedValue.getValidationMessage()).thenReturn(validationMessage);
        Mockito.when(managedFeature.getFriendlyName()).thenReturn("Name");
        Mockito.doReturn(String.class).when(managedFeature).getElementClass();
        return new UiComponentFactory.ComponentRequest(managedValue, managedFeature, Optional.empty());
    }

    @SuppressWarnings("unchecked")
    UiComponentFactory.ComponentRequest nonStringRequest() {
        var managedValue = Mockito.mock(ManagedValue.class);
        var managedFeature = Mockito.mock(ManagedFeature.class);
        Mockito.doReturn(UUID.class).when(managedFeature).getElementClass();
        return new UiComponentFactory.ComponentRequest(managedValue, managedFeature, Optional.empty());
    }

    @Test
    void handlesStringFeatures() {
        assertTrue(factory.isHandling(stringRequest("x")));
        assertFalse(factory.isHandling(nonStringRequest()));
    }

    @Test
    void createsTextFieldWithCurrentValue() {
        var component = factory.handle(stringRequest("current text"));

        var textField = assertInstanceOf(TextField.class, component);
        assertEquals("current text", textField.getValue());
        assertEquals("Name", textField.getLabel());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q test -pl viewers/vaadin/ui -Dtest=TextFieldFactoryTest`
Expected: COMPILATION ERROR — `TextFieldFactory` does not exist.

- [ ] **Step 3: Write `BindingsVaa.java`**

```java
package org.apache.causeway.viewer.vaadin.ui.binding;

import java.util.Objects;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasValidation;
import com.vaadin.flow.component.HasValue;

import org.apache.causeway.core.metamodel.interactions.managed.ManagedValue;

import lombok.experimental.UtilityClass;

/**
 * Binds Causeway {@link ManagedValue} models to Vaadin fields.
 * <p>
 * Uses the value's parsable-text representation, so any value type with a
 * parser can be edited through a text-ish field.
 */
@UtilityClass
public class BindingsVaa {

    /**
     * Two-way binding via {@link ManagedValue#getValueAsParsableText()},
     * plus validation feedback via {@link ManagedValue#getValidationMessage()}.
     */
    public <F extends Component & HasValue<?, String>> void bindParsableText(
            final F uiField,
            final ManagedValue managedValue,
            final boolean readOnly) {

        var parsableText = managedValue.getValueAsParsableText();
        uiField.setValue(Objects.toString(parsableText.getValue(), ""));

        if (readOnly) {
            uiField.setReadOnly(true);
            return;
        }

        // UI -> model
        uiField.addValueChangeListener(event -> parsableText.setValue(event.getValue()));
        // model -> UI
        parsableText.addListener((observable, oldValue, newValue) ->
                uiField.setValue(Objects.toString(newValue, "")));

        if (uiField instanceof HasValidation hasValidation) {
            managedValue.getValidationMessage().addListener((observable, oldValue, newValue) -> {
                hasValidation.setErrorMessage(newValue);
                hasValidation.setInvalid(newValue != null && !newValue.isBlank());
            });
        }
    }
}
```

- [ ] **Step 4: Write `TextFieldFactory.java`**

```java
package org.apache.causeway.viewer.vaadin.ui.components.text;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.textfield.TextField;

import org.springframework.core.annotation.Order;

import org.apache.causeway.applib.annotation.PriorityPrecedence;
import org.apache.causeway.viewer.commons.model.components.UiComponentFactory.ComponentRequest;
import org.apache.causeway.viewer.vaadin.ui.binding.BindingsVaa;
import org.apache.causeway.viewer.vaadin.ui.components.UiComponentHandlerVaa;

@org.springframework.stereotype.Component
@Order(PriorityPrecedence.MIDPOINT)
public class TextFieldFactory implements UiComponentHandlerVaa {

    @Override
    public boolean isHandling(final ComponentRequest request) {
        return request.isFeatureTypeEqualTo(String.class);
    }

    @Override
    public Component handle(final ComponentRequest request) {
        var uiField = new TextField(request.getFriendlyName());
        var readOnly = request.disablingUiModelIfAny().isPresent();
        BindingsVaa.bindParsableText(uiField, request.managedValue(), readOnly);
        return uiField;
    }
}
```

Note: do NOT call `request.isReadOnly()` here — that shortcut casts the feature to `ManagedProperty` and throws for parameters; the disabling model already covers the readonly case for both.

- [ ] **Step 5: Add to module config.** In `CausewayModuleViewerVaadinUi`, extend the `@Import` list with `org.apache.causeway.viewer.vaadin.ui.components.text.TextFieldFactory.class`.

- [ ] **Step 6: Run tests to verify they pass**

Run: `mvn -q test -pl viewers/vaadin/ui`
Expected: all tests PASS (UiComponentFactoryVaaTest + TextFieldFactoryTest).

- [ ] **Step 7: Commit**

```bash
git add viewers/vaadin/ui
git commit -m "CAUSEWAY-VAADIN: BindingsVaa parsable-text binding and TextFieldFactory"
```

---

### Task 5: `TemporalFieldFactory`

**Files:**
- Create: `viewers/vaadin/ui/src/main/java/org/apache/causeway/viewer/vaadin/ui/components/temporal/TemporalFieldFactory.java`
- Modify: `viewers/vaadin/ui/src/main/java/org/apache/causeway/viewer/vaadin/ui/CausewayModuleViewerVaadinUi.java` (add to `@Import`)
- Test: `viewers/vaadin/ui/src/test/java/org/apache/causeway/viewer/vaadin/ui/components/temporal/TemporalFieldFactoryTest.java`

- [ ] **Step 1: Write the failing test**

```java
package org.apache.causeway.viewer.vaadin.ui.components.temporal;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.datetimepicker.DateTimePicker;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import org.apache.causeway.commons.binding.Bindable;
import org.apache.causeway.core.metamodel.interactions.managed.ManagedFeature;
import org.apache.causeway.core.metamodel.interactions.managed.ManagedValue;
import org.apache.causeway.core.metamodel.object.ManagedObject;
import org.apache.causeway.viewer.commons.model.components.UiComponentFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TemporalFieldFactoryTest {

    final TemporalFieldFactory factory = new TemporalFieldFactory();

    @SuppressWarnings("unchecked")
    UiComponentFactory.ComponentRequest requestFor(final Class<?> type, final Object currentPojo) {
        var managedValue = Mockito.mock(ManagedValue.class);
        var managedFeature = Mockito.mock(ManagedFeature.class);
        var valueBindable = (Bindable<ManagedObject>) Mockito.mock(Bindable.class);
        var currentValue = Mockito.mock(ManagedObject.class);
        Mockito.when(currentValue.getPojo()).thenReturn(currentPojo);
        Mockito.when(valueBindable.getValue()).thenReturn(currentValue);
        Mockito.when(managedValue.getValue()).thenReturn(valueBindable);
        Mockito.when(managedFeature.getFriendlyName()).thenReturn("When");
        Mockito.doReturn(type).when(managedFeature).getElementClass();
        return new UiComponentFactory.ComponentRequest(managedValue, managedFeature, Optional.empty());
    }

    @Test
    void handlesOnlyLocalDateAndLocalDateTime() {
        assertTrue(factory.isHandling(requestFor(LocalDate.class, null)));
        assertTrue(factory.isHandling(requestFor(LocalDateTime.class, null)));
        assertFalse(factory.isHandling(requestFor(String.class, null)));
    }

    @Test
    void createsDatePickerWithCurrentValue() {
        var today = LocalDate.of(2026, 6, 9);
        var component = factory.handle(requestFor(LocalDate.class, today));

        var picker = assertInstanceOf(DatePicker.class, component);
        assertEquals(today, picker.getValue());
    }

    @Test
    void createsDateTimePickerForLocalDateTime() {
        var now = LocalDateTime.of(2026, 6, 9, 12, 30);
        var component = factory.handle(requestFor(LocalDateTime.class, now));

        var picker = assertInstanceOf(DateTimePicker.class, component);
        assertEquals(now, picker.getValue());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q test -pl viewers/vaadin/ui -Dtest=TemporalFieldFactoryTest`
Expected: COMPILATION ERROR — `TemporalFieldFactory` does not exist.

- [ ] **Step 3: Write `TemporalFieldFactory.java`**

```java
package org.apache.causeway.viewer.vaadin.ui.components.temporal;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.datetimepicker.DateTimePicker;

import org.springframework.core.annotation.Order;

import org.apache.causeway.applib.annotation.PriorityPrecedence;
import org.apache.causeway.core.metamodel.object.ManagedObject;
import org.apache.causeway.viewer.commons.model.components.UiComponentFactory.ComponentRequest;
import org.apache.causeway.viewer.vaadin.ui.components.UiComponentHandlerVaa;

@org.springframework.stereotype.Component
@Order(PriorityPrecedence.MIDPOINT)
public class TemporalFieldFactory implements UiComponentHandlerVaa {

    @Override
    public boolean isHandling(final ComponentRequest request) {
        return request.isFeatureTypeEqualTo(LocalDate.class)
                || request.isFeatureTypeEqualTo(LocalDateTime.class);
    }

    @Override
    public Component handle(final ComponentRequest request) {
        var readOnly = request.disablingUiModelIfAny().isPresent();
        return request.isFeatureTypeEqualTo(LocalDate.class)
                ? datePicker(request, readOnly)
                : dateTimePicker(request, readOnly);
    }

    private Component datePicker(final ComponentRequest request, final boolean readOnly) {
        var picker = new DatePicker(request.getFriendlyName());
        picker.setValue((LocalDate) currentPojo(request));
        picker.setReadOnly(readOnly);
        if (!readOnly) {
            picker.addValueChangeListener(event -> updateValue(request, event.getValue()));
        }
        return picker;
    }

    private Component dateTimePicker(final ComponentRequest request, final boolean readOnly) {
        var picker = new DateTimePicker(request.getFriendlyName());
        picker.setValue((LocalDateTime) currentPojo(request));
        picker.setReadOnly(readOnly);
        if (!readOnly) {
            picker.addValueChangeListener(event -> updateValue(request, event.getValue()));
        }
        return picker;
    }

    private Object currentPojo(final ComponentRequest request) {
        var managedObject = request.managedValue().getValue().getValue();
        return managedObject != null ? managedObject.getPojo() : null;
    }

    private void updateValue(final ComponentRequest request, final Object newPojo) {
        request.managedValue().getValue().setValue(
                ManagedObject.adaptSingular(request.getFeatureTypeSpec(), newPojo));
    }
}
```

- [ ] **Step 4: Add `TemporalFieldFactory.class` to the `@Import` list of `CausewayModuleViewerVaadinUi`.**

- [ ] **Step 5: Run tests to verify they pass**

Run: `mvn -q test -pl viewers/vaadin/ui`
Expected: all PASS.

- [ ] **Step 6: Commit**

```bash
git add viewers/vaadin/ui
git commit -m "CAUSEWAY-VAADIN: TemporalFieldFactory for LocalDate/LocalDateTime"
```

---

### Task 6: `TableViewVaa` — collections as Grid

**Files:**
- Create: `viewers/vaadin/ui/src/main/java/org/apache/causeway/viewer/vaadin/ui/components/collection/TableViewVaa.java`
- Test: `viewers/vaadin/ui/src/test/java/org/apache/causeway/viewer/vaadin/ui/components/collection/TableViewVaaTest.java`

- [ ] **Step 1: Write the failing test**

```java
package org.apache.causeway.viewer.vaadin.ui.components.collection;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import org.apache.causeway.commons.binding.Observable;
import org.apache.causeway.commons.collections.Can;
import org.apache.causeway.core.metamodel.tabular.DataRow;
import org.apache.causeway.core.metamodel.tabular.DataTableInteractive;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class TableViewVaaTest {

    @SuppressWarnings("unchecked")
    @Test
    void rendersEmptyPlaceholder_whenNoRows() {
        var table = Mockito.mock(DataTableInteractive.class);
        var rowsObservable = (Observable<Can<DataRow>>) Mockito.mock(Observable.class);
        Mockito.when(rowsObservable.getValue()).thenReturn(Can.empty());
        Mockito.when(table.dataRowsFilteredAndSortedObservable()).thenReturn(rowsObservable);

        var component = TableViewVaa.forDataTableInteractive(table);

        assertInstanceOf(TableViewVaa.class, component);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q test -pl viewers/vaadin/ui -Dtest=TableViewVaaTest`
Expected: COMPILATION ERROR.

- [ ] **Step 3: Write `TableViewVaa.java`**

```java
package org.apache.causeway.viewer.vaadin.ui.components.collection;

import java.util.stream.Collectors;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import org.apache.causeway.core.metamodel.object.ManagedObject;
import org.apache.causeway.core.metamodel.object.MmTitleUtils;
import org.apache.causeway.core.metamodel.tabular.DataColumn;
import org.apache.causeway.core.metamodel.tabular.DataRow;
import org.apache.causeway.core.metamodel.tabular.DataTableInteractive;

import org.jspecify.annotations.NonNull;

/**
 * Renders a {@link DataTableInteractive} (parented collection or standalone
 * action result) as a Vaadin {@link Grid}: first column is the row element's
 * title, then one column per visible association.
 */
public class TableViewVaa extends VerticalLayout {

    private static final long serialVersionUID = 1L;

    public static Component forDataTableInteractive(
            final @NonNull DataTableInteractive dataTable) {
        return new TableViewVaa(dataTable);
    }

    private TableViewVaa(final DataTableInteractive dataTable) {
        var rows = dataTable.dataRowsFilteredAndSortedObservable().getValue();
        if (rows.isEmpty()) {
            add(new Span("No rows to display."));
            return;
        }

        var grid = new Grid<DataRow>();
        add(grid);

        grid.addColumn(row -> MmTitleUtils.titleOf(row.rowElement()))
                .setHeader(dataTable.titleObservable().getValue());

        dataTable.dataColumnsObservable().getValue().forEach(column ->
                grid.addColumn(row -> stringifyCell(row, column))
                        .setHeader(column.columnFriendlyNameObservable().getValue()));

        grid.setItems(rows.toList());
        grid.setColumnReorderingAllowed(true);
        setWidthFull();
    }

    private static String stringifyCell(final DataRow row, final DataColumn column) {
        return row.getCellElementsForColumn(column).stream()
                .map(TableViewVaa::stringifyCellElement)
                .collect(Collectors.joining(", "));
    }

    private static String stringifyCellElement(final ManagedObject cellElement) {
        return cellElement != null && cellElement.getPojo() != null
                ? MmTitleUtils.titleOf(cellElement)
                : "";
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `mvn -q test -pl viewers/vaadin/ui`
Expected: all PASS.

- [ ] **Step 5: Commit**

```bash
git add viewers/vaadin/ui
git commit -m "CAUSEWAY-VAADIN: TableViewVaa renders DataTableInteractive as Grid"
```

---

### Task 7: `ObjectViewVaa` — object page via `UiGridLayout.Visitor`

**Files:**
- Create: `viewers/vaadin/ui/src/main/java/org/apache/causeway/viewer/vaadin/ui/components/object/ObjectViewVaa.java`

This class needs a live metamodel, so it has no isolated unit test; it is exercised by the Task 13 smoke test and by manual verification. It must still compile cleanly.

- [ ] **Step 1: Write `ObjectViewVaa.java`**

```java
package org.apache.causeway.viewer.vaadin.ui.components.object;

import java.util.function.Consumer;

import com.vaadin.flow.component.HasComponents;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.formlayout.FormLayout.ResponsiveStep;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.FlexLayout.FlexWrap;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;

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

import org.jspecify.annotations.NonNull;

/**
 * Renders a domain object's page: title, actions, fieldsets with properties,
 * tab groups and collections — driven by the object's bootstrap grid layout
 * via the {@link UiGridLayout.Visitor} (Visitor pattern: layout traversal is
 * in commons, only the Vaadin rendering lives here).
 */
public class ObjectViewVaa extends VerticalLayout {

    private static final long serialVersionUID = 1L;

    public static ObjectViewVaa fromObject(
            final @NonNull UiComponentFactoryVaa uiComponentFactory,
            final @NonNull Consumer<ManagedAction> actionEventHandler,
            final @NonNull ManagedObject managedObject) {
        return new ObjectViewVaa(uiComponentFactory, actionEventHandler, managedObject);
    }

    protected ObjectViewVaa(
            final UiComponentFactoryVaa uiComponentFactory,
            final Consumer<ManagedAction> actionEventHandler,
            final ManagedObject managedObject) {

        var objectTitle = MmTitleUtils.titleOf(managedObject);

        var gridVisitor = new UiGridLayout.Visitor<HasComponents, Tabs>(this) {

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
                uiActionPanel.setAlignItems(Alignment.BASELINE);
                return uiActionPanel;
            }

            @Override
            protected Tabs newTabGroup(final HasComponents container, final BSTabGroup tabGroupData) {
                var uiTabGroup = Vaa.add(container, new Tabs());
                uiTabGroup.setOrientation(Tabs.Orientation.HORIZONTAL);
                return uiTabGroup;
            }

            @Override
            protected HasComponents newTab(final Tabs tabGroup, final BSTab tabData) {
                return Vaa.add(tabGroup, new Tab(tabData.getName()));
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
                                    managedCollection.createDataTableModel()));
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
```

- [ ] **Step 2: Verify it compiles**

Run: `mvn -q test-compile -pl viewers/vaadin/ui`
Expected: `BUILD SUCCESS`. (If `Alignment` is unresolved inside the anonymous visitor, qualify it as `FlexLayout.Alignment.BASELINE` — the anonymous class does not extend a Vaadin layout, unlike the old incubator code which subclassed `VerticalLayout` directly.)

- [ ] **Step 3: Run all module tests**

Run: `mvn -q test -pl viewers/vaadin/ui`
Expected: PASS (no regression).

- [ ] **Step 4: Commit**

```bash
git add viewers/vaadin/ui
git commit -m "CAUSEWAY-VAADIN: ObjectViewVaa renders object page via UiGridLayout visitor"
```

---

### Task 8: Action parameter dialog — `ActionForm` + `ActionDialog`

**Files:**
- Create: `viewers/vaadin/ui/src/main/java/org/apache/causeway/viewer/vaadin/ui/components/action/ActionForm.java`
- Create: `viewers/vaadin/ui/src/main/java/org/apache/causeway/viewer/vaadin/ui/components/action/ActionDialog.java`

- [ ] **Step 1: Write `ActionForm.java`**

```java
package org.apache.causeway.viewer.vaadin.ui.components.action;

import com.vaadin.flow.component.formlayout.FormLayout;

import org.apache.causeway.core.metamodel.interactions.managed.ManagedAction;
import org.apache.causeway.core.metamodel.interactions.managed.ParameterNegotiationModel;
import org.apache.causeway.viewer.commons.model.components.UiComponentFactory;
import org.apache.causeway.viewer.vaadin.ui.components.UiComponentFactoryVaa;

import org.jspecify.annotations.NonNull;

import lombok.Getter;

/**
 * Form with one input field per action parameter, built through the
 * component-factory chain and bound to the parameter negotiation model.
 */
public class ActionForm extends FormLayout {

    private static final long serialVersionUID = 1L;

    @Getter
    private final ParameterNegotiationModel parameterNegotiation;

    public static ActionForm forManagedAction(
            final @NonNull UiComponentFactoryVaa uiComponentFactory,
            final @NonNull ManagedAction managedAction) {
        return new ActionForm(uiComponentFactory, managedAction);
    }

    private ActionForm(
            final UiComponentFactoryVaa uiComponentFactory,
            final ManagedAction managedAction) {

        this.parameterNegotiation = managedAction.startParameterNegotiation();

        parameterNegotiation.getParamModels().forEach(managedParameter ->
                add(uiComponentFactory.parameterFor(
                        UiComponentFactory.ComponentRequest.of(managedParameter))));

        setResponsiveSteps(new ResponsiveStep("0", 1));
    }
}
```

- [ ] **Step 2: Write `ActionDialog.java`**

```java
package org.apache.causeway.viewer.vaadin.ui.components.action;

import java.util.function.Predicate;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;

import org.apache.causeway.commons.collections.Can;
import org.apache.causeway.core.metamodel.interactions.managed.ManagedAction;
import org.apache.causeway.core.metamodel.object.ManagedObject;
import org.apache.causeway.viewer.vaadin.ui.components.UiComponentFactoryVaa;

import org.jspecify.annotations.NonNull;

/**
 * Modal dialog prompting for action parameters; OK hands the negotiated
 * parameter values to the supplied callback (returns true to close).
 */
public class ActionDialog extends Dialog {

    private static final long serialVersionUID = 1L;

    public static ActionDialog forManagedAction(
            final @NonNull UiComponentFactoryVaa uiComponentFactory,
            final @NonNull ManagedAction managedAction,
            final @NonNull Predicate<Can<ManagedObject>> submitHandler) {
        return new ActionDialog(uiComponentFactory, managedAction, submitHandler);
    }

    private ActionDialog(
            final UiComponentFactoryVaa uiComponentFactory,
            final ManagedAction managedAction,
            final Predicate<Can<ManagedObject>> submitHandler) {

        setHeaderTitle(managedAction.getFriendlyName());
        setModal(true);

        var actionForm = ActionForm.forManagedAction(uiComponentFactory, managedAction);
        add(actionForm);

        var okButton = new Button("OK", event -> {
            var paramValues = actionForm.getParameterNegotiation().getParamValues();
            if (submitHandler.test(paramValues)) {
                close();
            }
        });
        var cancelButton = new Button("Cancel", event -> close());
        getFooter().add(new HorizontalLayout(okButton, cancelButton));
    }
}
```

- [ ] **Step 3: Verify compilation**

Run: `mvn -q test-compile -pl viewers/vaadin/ui`
Expected: `BUILD SUCCESS`. (If `getParamValues()` does not exist on `ParameterNegotiationModel`, check the actual accessor with `grep -n "ParamValues\|paramValues" core/metamodel/src/main/java/org/apache/causeway/core/metamodel/interactions/managed/ParameterNegotiationModel.java` and use what it returns — the value is a `Can<ManagedObject>` of the negotiated parameter values.)

- [ ] **Step 4: Run module tests**

Run: `mvn -q test -pl viewers/vaadin/ui`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add viewers/vaadin/ui
git commit -m "CAUSEWAY-VAADIN: ActionForm and ActionDialog for parameter negotiation"
```

---

### Task 9: `UiContextVaaDefault` + `UiActionHandlerVaa`

**Files:**
- Create: `viewers/vaadin/ui/src/main/java/org/apache/causeway/viewer/vaadin/ui/pages/main/UiContextVaaDefault.java`
- Create: `viewers/vaadin/ui/src/main/java/org/apache/causeway/viewer/vaadin/ui/pages/main/UiActionHandlerVaa.java`
- Modify: `viewers/vaadin/ui/src/main/java/org/apache/causeway/viewer/vaadin/ui/CausewayModuleViewerVaadinUi.java` (add both to `@Import`)

- [ ] **Step 1: Write `UiContextVaaDefault.java`**

```java
package org.apache.causeway.viewer.vaadin.ui.pages.main;

import java.util.function.Consumer;

import com.vaadin.flow.component.Component;

import jakarta.inject.Inject;

import org.springframework.stereotype.Service;

import org.apache.causeway.applib.services.iactnlayer.InteractionService;
import org.apache.causeway.commons.collections.Can;
import org.apache.causeway.core.metamodel.interactions.managed.ManagedAction;
import org.apache.causeway.core.metamodel.object.ManagedObject;
import org.apache.causeway.viewer.vaadin.model.context.MemberInvocationHandler;
import org.apache.causeway.viewer.vaadin.model.context.UiContextVaa;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Service
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class UiContextVaaDefault implements UiContextVaa {

    @Getter private final InteractionService interactionService;

    @Setter private Consumer<Component> newPageHandler;
    @Setter private MemberInvocationHandler<Component> pageFactory;

    @Override
    public void route(final ManagedObject object) {
        newPageHandler.accept(pageFactory.handle(object));
    }

    @Override
    public void route(
            final ManagedAction managedAction,
            final Can<ManagedObject> params,
            final ManagedObject actionResult) {
        newPageHandler.accept(pageFactory.handle(managedAction, params, actionResult));
    }
}
```

- [ ] **Step 2: Write `UiActionHandlerVaa.java`**

```java
package org.apache.causeway.viewer.vaadin.ui.pages.main;

import jakarta.inject.Inject;

import org.springframework.stereotype.Service;

import org.apache.causeway.commons.collections.Can;
import org.apache.causeway.core.metamodel.interactions.managed.ManagedAction;
import org.apache.causeway.core.metamodel.object.ManagedObject;
import org.apache.causeway.viewer.vaadin.model.context.UiContextVaa;
import org.apache.causeway.viewer.vaadin.ui.components.UiComponentFactoryVaa;
import org.apache.causeway.viewer.vaadin.ui.components.action.ActionDialog;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Reacts to action-link clicks: zero-parameter actions are invoked directly,
 * otherwise an {@link ActionDialog} collects parameters first. The action
 * result is routed to the current page via {@link UiContextVaa}.
 */
@Service
@RequiredArgsConstructor(onConstructor_ = {@Inject})
@Slf4j
public class UiActionHandlerVaa {

    private final UiContextVaa uiContext;
    private final UiComponentFactoryVaa uiComponentFactory;

    public void handleActionLinkClicked(final ManagedAction managedAction) {
        if (managedAction.getAction().getParameterCount() == 0) {
            invoke(managedAction, Can.empty());
            return;
        }
        ActionDialog.forManagedAction(uiComponentFactory, managedAction, params -> {
            invoke(managedAction, params);
            return true;
        }).open();
    }

    private void invoke(final ManagedAction managedAction, final Can<ManagedObject> params) {
        managedAction.invoke(params)
                .ifSuccess(actionResult ->
                        uiContext.route(managedAction, params, actionResult))
                .ifFailure(veto ->
                        log.warn("action {} vetoed: {}", managedAction.getId(), veto));
    }
}
```

(If `Railway` exposes differently-named terminal operations, check `commons/src/main/java/org/apache/causeway/commons/functional/Railway.java` — use the success/failure consumers it actually provides, e.g. `fold`.)

- [ ] **Step 3: Add `UiContextVaaDefault.class` and `UiActionHandlerVaa.class` to `CausewayModuleViewerVaadinUi`'s `@Import`.**

- [ ] **Step 4: Compile + test**

Run: `mvn -q test -pl viewers/vaadin/ui`
Expected: `BUILD SUCCESS`, all tests PASS.

- [ ] **Step 5: Commit**

```bash
git add viewers/vaadin/ui
git commit -m "CAUSEWAY-VAADIN: UiContextVaaDefault routing and UiActionHandlerVaa action invocation"
```

---

### Task 10: Menu + application shell — `MenuBuilderVaa`, `MainViewVaa`

**Files:**
- Create: `viewers/vaadin/ui/src/main/java/org/apache/causeway/viewer/vaadin/ui/pages/main/MenuBuilderVaa.java`
- Create: `viewers/vaadin/ui/src/main/java/org/apache/causeway/viewer/vaadin/ui/pages/main/MainViewVaa.java`

- [ ] **Step 1: Write `MenuBuilderVaa.java`**

```java
package org.apache.causeway.viewer.vaadin.ui.pages.main;

import java.util.function.Consumer;

import com.vaadin.flow.component.contextmenu.SubMenu;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.menubar.MenuBar;

import org.apache.causeway.core.metamodel.interactions.managed.ManagedAction;
import org.apache.causeway.viewer.commons.applib.services.menu.MenuVisitor;
import org.apache.causeway.viewer.commons.applib.services.menu.model.MenuAction;
import org.apache.causeway.viewer.commons.applib.services.menu.model.MenuDropdown;
import org.apache.causeway.viewer.commons.applib.services.menu.model.NavbarSection;

import org.jspecify.annotations.NonNull;

/**
 * Translates the viewer-agnostic menu model (visited depth-first via
 * {@link MenuVisitor}) into a Vaadin {@link MenuBar}.
 */
public class MenuBuilderVaa implements MenuVisitor {

    public static MenuBar buildMenuBar(
            final @NonNull NavbarSection navbarSection,
            final @NonNull Consumer<ManagedAction> actionEventHandler) {
        var menuBar = new MenuBar();
        navbarSection.visitMenuItems(new MenuBuilderVaa(menuBar, actionEventHandler));
        return menuBar;
    }

    private final MenuBar menuBar;
    private final Consumer<ManagedAction> actionEventHandler;
    private SubMenu currentSubMenu;

    private MenuBuilderVaa(final MenuBar menuBar, final Consumer<ManagedAction> actionEventHandler) {
        this.menuBar = menuBar;
        this.actionEventHandler = actionEventHandler;
    }

    @Override
    public void onTopLevel(final MenuDropdown menuDropdown) {
        currentSubMenu = menuBar.addItem(menuDropdown.name()).getSubMenu();
    }

    @Override
    public void onMenuAction(final MenuAction menuAction) {
        currentSubMenu.addItem(menuAction.name(), event ->
                menuAction.managedAction().ifPresent(actionEventHandler));
    }

    @Override
    public void onSectionSpacer() {
        currentSubMenu.add(new Hr());
    }

    @Override
    public void onSectionLabel(final String named) {
        var label = new Span(named);
        label.getStyle().set("font-weight", "bold");
        currentSubMenu.add(label);
    }
}
```

(If `MenuVisitor` declares additional methods — check `viewers/commons/applib/src/main/java/org/apache/causeway/viewer/commons/applib/services/menu/MenuVisitor.java` — implement them as no-ops with a one-line comment.)

- [ ] **Step 2: Write `MainViewVaa.java`**

```java
package org.apache.causeway.viewer.vaadin.ui.pages.main;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;

import jakarta.inject.Inject;

import org.apache.causeway.commons.collections.Can;
import org.apache.causeway.core.metamodel.context.MetaModelContext;
import org.apache.causeway.core.metamodel.interactions.managed.ManagedAction;
import org.apache.causeway.core.metamodel.object.ManagedObject;
import org.apache.causeway.core.metamodel.tabular.DataTableInteractive;
import org.apache.causeway.viewer.commons.applib.services.header.HeaderUiService;
import org.apache.causeway.viewer.vaadin.model.context.MemberInvocationHandler;
import org.apache.causeway.viewer.vaadin.model.context.UiContextVaa;
import org.apache.causeway.viewer.vaadin.ui.components.UiComponentFactoryVaa;
import org.apache.causeway.viewer.vaadin.ui.components.collection.TableViewVaa;
import org.apache.causeway.viewer.vaadin.ui.components.object.ObjectViewVaa;

import lombok.extern.slf4j.Slf4j;

/**
 * Application shell: navbar with the metamodel-driven menu, swappable page
 * content. Implements {@link MemberInvocationHandler} so routing can render
 * objects and action results without knowing any view class.
 */
@Route("")
@Slf4j
public class MainViewVaa extends AppLayout
        implements BeforeEnterObserver, MemberInvocationHandler<Component> {

    private static final long serialVersionUID = 1L;

    private final transient MetaModelContext metaModelContext;
    private final transient UiContextVaa uiContext;
    private final transient UiActionHandlerVaa uiActionHandler;
    private final transient UiComponentFactoryVaa uiComponentFactory;
    private final transient HeaderUiService headerUiService;

    private final Div pageContent = new Div();

    @Inject
    public MainViewVaa(
            final MetaModelContext metaModelContext,
            final UiContextVaa uiContext,
            final UiActionHandlerVaa uiActionHandler,
            final UiComponentFactoryVaa uiComponentFactory,
            final HeaderUiService headerUiService) {
        this.metaModelContext = metaModelContext;
        this.uiContext = uiContext;
        this.uiActionHandler = uiActionHandler;
        this.uiComponentFactory = uiComponentFactory;
        this.headerUiService = headerUiService;

        uiContext.setNewPageHandler(this::replaceContent);
        uiContext.setPageFactory(this);
    }

    @Override
    public void beforeEnter(final BeforeEnterEvent event) {
        setPrimarySection(Section.NAVBAR);

        var navbar = headerUiService.getHeader().navbar();
        addToNavbar(MenuBuilderVaa.buildMenuBar(navbar.primary(), uiActionHandler::handleActionLinkClicked));
        addToNavbar(MenuBuilderVaa.buildMenuBar(navbar.secondary(), uiActionHandler::handleActionLinkClicked));
        addToNavbar(MenuBuilderVaa.buildMenuBar(navbar.tertiary(), uiActionHandler::handleActionLinkClicked));

        setContent(pageContent);
        renderHomepage();
    }

    private void replaceContent(final Component component) {
        pageContent.removeAll();
        pageContent.add(component);
    }

    private void renderHomepage() {
        var homepage = metaModelContext.getHomePageAdapter();
        if (homepage != null && homepage.getPojo() != null) {
            uiContext.route(homepage);
        }
    }

    @Override
    public Component handle(final ManagedObject object) {
        return ObjectViewVaa.fromObject(
                uiComponentFactory, uiActionHandler::handleActionLinkClicked, object);
    }

    @Override
    public Component handle(
            final ManagedAction managedAction,
            final Can<ManagedObject> params,
            final ManagedObject actionResult) {
        return actionResult.objSpec().isPlural()
                ? TableViewVaa.forDataTableInteractive(
                        DataTableInteractive.forAction(managedAction, actionResult))
                : handle(actionResult);
    }
}
```

(`DataTableInteractive.forAction(...)` — check its exact parameter list at `core/metamodel/src/main/java/org/apache/causeway/core/metamodel/tabular/DataTableInteractive.java:55` and pass what it requires; it may also take the parameter `Can`. Adjust the call, not the design. Same for `objSpec().isPlural()` — if absent, use `getSpecification().isPlural()`.)

- [ ] **Step 3: Compile + test**

Run: `mvn -q test -pl viewers/vaadin/ui`
Expected: `BUILD SUCCESS`, tests PASS. (`@Route` views are discovered by Vaadin's Spring scanner at runtime, not via `@Import`.)

- [ ] **Step 4: Commit**

```bash
git add viewers/vaadin/ui
git commit -m "CAUSEWAY-VAADIN: MainViewVaa app shell and MenuBuilderVaa metamodel menu"
```

---

### Task 11: Authentication — session store, route guard, login view, logout

**Files:**
- Create: `viewers/vaadin/ui/src/main/java/org/apache/causeway/viewer/vaadin/ui/auth/AuthSessionStoreUtil.java`
- Create: `viewers/vaadin/ui/src/main/java/org/apache/causeway/viewer/vaadin/ui/auth/VaadinAuthenticationHandler.java`
- Create: `viewers/vaadin/ui/src/main/java/org/apache/causeway/viewer/vaadin/ui/auth/LogoutHandlerVaa.java`
- Create: `viewers/vaadin/ui/src/main/java/org/apache/causeway/viewer/vaadin/ui/pages/login/VaadinLoginView.java`
- Modify: `viewers/vaadin/ui/src/main/java/org/apache/causeway/viewer/vaadin/ui/CausewayModuleViewerVaadinUi.java` (add `VaadinAuthenticationHandler.class`, `LogoutHandlerVaa.class` to `@Import`)
- Test: `viewers/vaadin/ui/src/test/java/org/apache/causeway/viewer/vaadin/ui/auth/AuthSessionStoreUtilTest.java`

- [ ] **Step 1: Write the failing test**

```java
package org.apache.causeway.viewer.vaadin.ui.auth;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import jakarta.servlet.http.HttpSession;

import org.apache.causeway.applib.services.iactnlayer.InteractionContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthSessionStoreUtilTest {

    @Test
    void roundTripsInteractionContextOnHttpSession() {
        var session = Mockito.mock(HttpSession.class);
        var auth = Mockito.mock(InteractionContext.class);
        var store = new Object() { Object value; };

        Mockito.doAnswer(invocation -> { store.value = invocation.getArgument(1); return null; })
                .when(session).setAttribute(Mockito.anyString(), Mockito.any());
        Mockito.when(session.getAttribute(Mockito.anyString()))
                .thenAnswer(invocation -> store.value);

        AuthSessionStoreUtil.put(session, auth);
        var roundTripped = AuthSessionStoreUtil.get(session);

        assertTrue(roundTripped.isPresent());
        assertEquals(auth, roundTripped.get());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q test -pl viewers/vaadin/ui -Dtest=AuthSessionStoreUtilTest`
Expected: COMPILATION ERROR.

- [ ] **Step 3: Write `AuthSessionStoreUtil.java`**

```java
package org.apache.causeway.viewer.vaadin.ui.auth;

import java.util.Optional;

import com.vaadin.flow.server.VaadinSession;

import jakarta.servlet.http.HttpSession;

import org.apache.causeway.applib.services.iactnlayer.InteractionContext;

import lombok.experimental.UtilityClass;

/**
 * Stores the authenticated {@link InteractionContext} in the HTTP session,
 * readable both from Vaadin UI code ({@link VaadinSession}) and from the
 * servlet layer ({@link HttpSession}).
 */
@UtilityClass
public class AuthSessionStoreUtil {

    private static final String ATTRIBUTE = InteractionContext.class.getName();

    public void put(final HttpSession session, final InteractionContext authentication) {
        session.setAttribute(ATTRIBUTE, authentication);
    }

    public Optional<InteractionContext> get(final HttpSession session) {
        return Optional.ofNullable(session)
                .map(s -> (InteractionContext) s.getAttribute(ATTRIBUTE));
    }

    /** Variant for Vaadin UI threads. */
    public void put(final InteractionContext authentication) {
        VaadinSession.getCurrent().getSession()
                .setAttribute(ATTRIBUTE, authentication);
    }

    /** Variant for Vaadin UI threads. */
    public Optional<InteractionContext> get() {
        return Optional.ofNullable(VaadinSession.getCurrent())
                .map(VaadinSession::getSession)
                .map(s -> (InteractionContext) s.getAttribute(ATTRIBUTE));
    }

    public void clear() {
        Optional.ofNullable(VaadinSession.getCurrent())
                .map(VaadinSession::getSession)
                .ifPresent(s -> s.setAttribute(ATTRIBUTE, null));
    }
}
```

(`VaadinSession.getSession()` returns `WrappedSession`, whose `setAttribute/getAttribute` mirror `HttpSession` — that is why both variants compile.)

- [ ] **Step 4: Write `VaadinAuthenticationHandler.java`**

```java
package org.apache.causeway.viewer.vaadin.ui.auth;

import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinServiceInitListener;

import jakarta.inject.Inject;

import org.apache.causeway.applib.services.iactnlayer.InteractionService;
import org.apache.causeway.core.metamodel.context.MetaModelContext;
import org.apache.causeway.core.security.authentication.AuthenticationRequest;
import org.apache.causeway.viewer.vaadin.ui.pages.login.VaadinLoginView;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Route guard: every navigation requires an authenticated session, otherwise
 * reroutes to the login view. Also offers the login entry point used by
 * {@link VaadinLoginView}.
 */
@org.springframework.stereotype.Component
@RequiredArgsConstructor(onConstructor_ = {@Inject})
@Slf4j
public class VaadinAuthenticationHandler
        implements AppShellConfigurator, VaadinServiceInitListener {

    private static final long serialVersionUID = 1L;

    private final transient InteractionService interactionService;
    private final transient MetaModelContext metaModelContext;

    @Override
    public void serviceInit(final ServiceInitEvent event) {
        event.getSource().addUIInitListener(uiEvent ->
                uiEvent.getUI().addBeforeEnterListener(this::beforeEnter));
    }

    /** @return whether authentication succeeded */
    public boolean loginToSession(final AuthenticationRequest authenticationRequest) {
        var authentication = metaModelContext.getAuthenticationManager()
                .authenticate(authenticationRequest);
        if (authentication == null) {
            return false;
        }
        log.debug("logging in {}", authentication.getUser().name());
        AuthSessionStoreUtil.put(authentication);
        return true;
    }

    private void beforeEnter(final BeforeEnterEvent event) {
        var authentication = AuthSessionStoreUtil.get().orElse(null);
        if (authentication != null) {
            if (!interactionService.isInInteraction()) {
                interactionService.openInteraction(authentication);
            }
            return; // access granted
        }
        if (!VaadinLoginView.class.equals(event.getNavigationTarget())) {
            event.rerouteTo(VaadinLoginView.class);
        }
    }
}
```

(If `UserMemento.name()` is not a record accessor, use `getName()` — check `api/applib/.../services/user/UserMemento.java`.)

- [ ] **Step 5: Write `VaadinLoginView.java`**

```java
package org.apache.causeway.viewer.vaadin.ui.pages.login;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.login.LoginForm;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

import jakarta.inject.Inject;

import org.apache.causeway.core.security.authentication.AuthenticationRequestPassword;
import org.apache.causeway.viewer.vaadin.ui.auth.VaadinAuthenticationHandler;

@Route("login")
public class VaadinLoginView extends VerticalLayout {

    private static final long serialVersionUID = 1L;

    @Inject
    public VaadinLoginView(final VaadinAuthenticationHandler authenticationHandler) {
        setSizeFull();
        setJustifyContentMode(JustifyContentMode.CENTER);
        setAlignItems(FlexComponent.Alignment.CENTER);

        var loginForm = new LoginForm();
        loginForm.setForgotPasswordButtonVisible(false);
        loginForm.addLoginListener(event -> {
            var request = new AuthenticationRequestPassword(
                    event.getUsername(), event.getPassword());
            if (authenticationHandler.loginToSession(request)) {
                UI.getCurrent().getPage().setLocation("/");
            } else {
                loginForm.setError(true);
            }
        });

        add(new H1("Apache Causeway"), loginForm);
    }
}
```

- [ ] **Step 6: Write `LogoutHandlerVaa.java`**

```java
package org.apache.causeway.viewer.vaadin.ui.auth;

import org.springframework.stereotype.Service;

import org.apache.causeway.core.security.authentication.logout.LogoutHandler;

/**
 * Invalidates this viewer's session-stored authentication when the framework
 * (e.g. the {@code logout} mixin) triggers a logout.
 */
@Service
public class LogoutHandlerVaa implements LogoutHandler {

    @Override
    public void logout() {
        AuthSessionStoreUtil.clear();
    }
}
```

- [ ] **Step 7: Add `VaadinAuthenticationHandler.class` and `LogoutHandlerVaa.class` to `CausewayModuleViewerVaadinUi`'s `@Import`.**

- [ ] **Step 8: Run tests to verify they pass**

Run: `mvn -q test -pl viewers/vaadin/ui`
Expected: all PASS.

- [ ] **Step 9: Commit**

```bash
git add viewers/vaadin/ui
git commit -m "CAUSEWAY-VAADIN: Causeway-native authentication (session store, route guard, login view, logout)"
```

---

### Task 12: `viewer` module — servlet integration and root configuration

**Files:**
- Create: `viewers/vaadin/viewer/src/main/java/org/apache/causeway/viewer/vaadin/viewer/CausewayServletForVaadin.java`
- Create: `viewers/vaadin/viewer/src/main/java/org/apache/causeway/viewer/vaadin/viewer/CausewayModuleViewerVaadinViewer.java`
- Create: `viewers/vaadin/viewer/src/main/resources/vaadin.properties`

- [ ] **Step 1: Write `CausewayServletForVaadin.java`**

```java
package org.apache.causeway.viewer.vaadin.viewer;

import java.io.IOException;

import com.vaadin.flow.spring.SpringServlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.context.ApplicationContext;

import org.apache.causeway.applib.services.iactnlayer.InteractionService;
import org.apache.causeway.viewer.vaadin.ui.auth.AuthSessionStoreUtil;

import org.jspecify.annotations.NonNull;

import lombok.extern.slf4j.Slf4j;

/**
 * Vaadin servlet that wraps each authenticated request in a Causeway
 * {@code Interaction}, so domain code on the request thread always sees the
 * session's {@code InteractionContext}.
 */
@Slf4j
public class CausewayServletForVaadin extends SpringServlet {

    private static final long serialVersionUID = 1L;

    private final transient InteractionService interactionService;

    public CausewayServletForVaadin(
            final @NonNull InteractionService interactionService,
            final @NonNull ApplicationContext context,
            final boolean rootMapping) {
        super(context, rootMapping);
        this.interactionService = interactionService;
    }

    @Override
    protected void service(
            final HttpServletRequest request,
            final HttpServletResponse response) throws ServletException, IOException {

        var authentication = AuthSessionStoreUtil.get(request.getSession(true)).orElse(null);
        log.debug("incoming request (authenticated={})", authentication != null);

        if (authentication != null) {
            interactionService.run(authentication, () -> super.service(request, response));
        } else {
            // unauthenticated: let the request through; VaadinAuthenticationHandler
            // reroutes UI navigation to the login view
            super.service(request, response);
        }
    }
}
```

(`InteractionService.run(context, runnable)` — the runnable parameter is a `ThrowingRunnable`, so the checked `ServletException/IOException` from `super.service` compile inside the lambda. If the compiler disagrees, check `api/applib/.../iactnlayer/InteractionService.java` for the `run` overload taking `ThrowingRunnable` and adjust.)

- [ ] **Step 2: Write `vaadin.properties`** at `viewers/vaadin/viewer/src/main/resources/vaadin.properties`:

```properties
vaadin.urlMapping=/*
vaadin.allowed-packages=org.apache.causeway.viewer.vaadin
```

- [ ] **Step 3: Write `CausewayModuleViewerVaadinViewer.java`**

```java
package org.apache.causeway.viewer.vaadin.viewer;

import com.vaadin.flow.spring.RootMappedCondition;
import com.vaadin.flow.spring.SpringBootAutoConfiguration;
import com.vaadin.flow.spring.SpringServlet;
import com.vaadin.flow.spring.VaadinConfigurationProperties;
import com.vaadin.flow.spring.VaadinServletContextInitializer;

import jakarta.inject.Inject;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.util.ClassUtils;
import org.springframework.web.context.WebApplicationContext;

import org.apache.causeway.applib.services.iactnlayer.InteractionService;
import org.apache.causeway.viewer.vaadin.ui.CausewayModuleViewerVaadinUi;

/**
 * Root configuration of the Vaadin viewer: import this single class from an
 * application manifest to enable the viewer.
 */
@Configuration
@Import({
        CausewayModuleViewerVaadinUi.class,
        VaadinConfigurationProperties.class,
})
@PropertySource("classpath:/vaadin.properties")
// standard Vaadin Spring Boot bootstrapping is replaced by the beans below
@EnableAutoConfiguration(exclude = {SpringBootAutoConfiguration.class})
public class CausewayModuleViewerVaadinViewer {

    @Inject private WebApplicationContext context;
    @Inject private VaadinConfigurationProperties configurationProperties;
    @Inject private InteractionService interactionService;

    @Bean
    public ServletContextInitializer vaadinServletContextInitializer() {
        return new VaadinServletContextInitializer(context);
    }

    @Bean
    public ServletRegistrationBean<SpringServlet> vaadinServletRegistrationBean() {
        var urlMapping = configurationProperties.getUrlMapping();
        var isRootMapping = RootMappedCondition.isRootMapping(urlMapping);
        if (isRootMapping) {
            urlMapping = "/vaadinServlet/*";
        }
        var registration = new ServletRegistrationBean<SpringServlet>(
                new CausewayServletForVaadin(interactionService, context, isRootMapping),
                urlMapping);
        registration.setAsyncSupported(configurationProperties.isAsyncSupported());
        registration.setName(ClassUtils.getShortNameAsProperty(SpringServlet.class));
        return registration;
    }
}
```

- [ ] **Step 4: Compile**

Run: `mvn -q install -pl viewers/vaadin/viewer -am -DskipTests`
Expected: `BUILD SUCCESS`. (Vaadin 24 may have changed `VaadinWebsocketEndpointExporter`/push wiring relative to the old incubator code — push/websocket support is intentionally NOT configured in phase 1.)

- [ ] **Step 5: Commit**

```bash
git add viewers/vaadin/viewer
git commit -m "CAUSEWAY-VAADIN: viewer module - servlet integration and root Spring configuration"
```

---

### Task 13: Spring smoke test

**Files:**
- Test: `viewers/vaadin/viewer/src/test/java/org/apache/causeway/viewer/vaadin/viewer/VaadinModuleContextLoadsTest.java`

- [ ] **Step 1: Write the test**

```java
package org.apache.causeway.viewer.vaadin.viewer;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import org.apache.causeway.core.runtimeservices.CausewayModuleCoreRuntimeServices;
import org.apache.causeway.security.bypass.CausewayModuleSecurityBypass;
import org.apache.causeway.viewer.vaadin.ui.components.UiComponentFactoryVaa;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(
        classes = VaadinModuleContextLoadsTest.AppManifest.class,
        webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class VaadinModuleContextLoadsTest {

    @Configuration
    @Import({
            CausewayModuleCoreRuntimeServices.class,
            CausewayModuleSecurityBypass.class,
            CausewayModuleViewerVaadinViewer.class,
    })
    static class AppManifest {
    }

    @Autowired ApplicationContext context;

    @Test
    void contextLoads_withComponentFactoryChain() {
        var factory = context.getBean(UiComponentFactoryVaa.class);
        assertNotNull(factory);
        assertNotNull(context.getBean(CausewayModuleViewerVaadinViewer.class));
    }
}
```

(Exact module class names: check `core/runtimeservices/src/main/java/.../CausewayModuleCoreRuntimeServices.java` and `security/bypass/src/main/java/.../CausewayModuleSecurityBypass.java`; fix the imports if packages differ. If the context requires extra config properties, add `properties = {"causeway.core.meta-model.introspector.lock-after-full-introspection=false"}`-style entries only as actually demanded by failure messages.)

- [ ] **Step 2: Run the test**

Run: `mvn -q test -pl viewers/vaadin/viewer`
Expected: PASS. Iterate on missing-bean errors by importing the module the error names — record each addition in the commit message.

- [ ] **Step 3: Commit**

```bash
git add viewers/vaadin/viewer
git commit -m "CAUSEWAY-VAADIN: smoke test - viewer Spring context boots with security bypass"
```

---

### Task 14: Final verification + docs stub

**Files:**
- Create: `viewers/vaadin/adoc/modules/vaadin/pages/about.adoc`

- [ ] **Step 1: Write `about.adoc`**

```adoc
= Vaadin Viewer

WARNING: This viewer is experimental (phase 1). It renders domain objects,
collections and action prompts using Vaadin Flow, reusing the
viewer-agnostic models from `causeway-viewer-commons`.

== Enabling

Import `CausewayModuleViewerVaadinViewer` from your application manifest:

[source,java]
----
@Configuration
@Import({
    CausewayModuleCoreRuntimeServices.class,
    CausewayModuleViewerVaadinViewer.class,
    // plus persistence + security modules of your choice
})
public class AppManifest { }
----

== Scope (phase 1)

* Login via Causeway's own `AuthenticationManager`
* Menubar built from the domain-service metamodel
* Domain-object page driven by the bootstrap grid layout
* Collections rendered as Vaadin `Grid`
* Action invocation incl. parameter dialogs (text, date, date-time inputs)

Not yet: blob/clob, markup rendering, bookmarks, hints, themes, i18n.
```

- [ ] **Step 2: Full build of the new modules**

Run: `mvn -q install -pl viewers/vaadin -am -DskipTests=false`
Expected: `BUILD SUCCESS`, all viewers/vaadin tests pass.

- [ ] **Step 3: Whole-reactor sanity (optional but recommended before any PR)**

Run: `mvn -q validate`
Expected: `BUILD SUCCESS` — confirms aggregator + bom edits parse everywhere.

- [ ] **Step 4: Commit**

```bash
git add viewers/vaadin/adoc docs/superpowers/plans/2026-06-09-vaadin-viewer.md
git commit -m "CAUSEWAY-VAADIN: docs stub for vaadin viewer"
```

---

## Self-Review Notes (already applied)

1. **Spec coverage:** login ✔ (Task 11), menu ✔ (Task 10), object page ✔ (Task 7), collections ✔ (Task 6), actions ✔ (Tasks 8–9), bootstrapping ✔ (Tasks 1, 12), OOP criteria ✔ (Factory/Chain: Tasks 3–5; Visitor: Task 7; DI/Interface segregation: Task 2's contracts; single responsibility per class throughout).
2. **Known API-drift hotspots** are called out inline where the executing engineer must verify a signature before relying on it (`DataTableInteractive.forAction`, `Railway` terminal ops, `ParameterNegotiationModel.getParamValues`, `UserMemento.name()`, module class packages in Task 13). Everything else was verified against the working tree at commit `a0318936607`.
3. **Type consistency:** `UiContextVaa`/`MemberInvocationHandler` signatures defined in Task 2 are used verbatim in Tasks 9–10; `TableViewVaa.forDataTableInteractive(DataTableInteractive)` defined in Task 6 is what Tasks 7 and 10 call; `UiComponentFactoryVaa(List<UiComponentHandlerVaa>)` defined in Task 3 matches the Task 3 test and Spring injection.
