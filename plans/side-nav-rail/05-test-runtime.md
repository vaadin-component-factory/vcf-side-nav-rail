# Phase 5 — Test runtime

**Prereqs:** Phase 4 complete. Read [`../2026-04-21-side-nav-rail-plan.md`](../2026-04-21-side-nav-rail-plan.md) for cross-phase conventions.

**Output of this phase:** a Spring Boot test application under `addon/src/test/` with four `@Route` test views, a scaffolded Playwright project (TypeScript config, `package.json`, `playwright.config.ts`), and Maven phase bindings so `./mvnw verify` runs the full pipeline end-to-end.

---

## Task 14: Test application skeleton

**Files:**
- Create: `addon/src/test/java/org/vaadin/addons/componentfactory/sidenavrail/app/TestApplication.java`
- Create: `addon/src/test/java/org/vaadin/addons/componentfactory/sidenavrail/app/TestMainLayout.java`
- Create: `addon/src/test/java/org/vaadin/addons/componentfactory/sidenavrail/app/views/BasicTestView.java`
- Create: `addon/src/test/resources/application.properties`

- [ ] **Step 1: Write the Spring Boot entry point**

Create `/workspace/addon/src/test/java/org/vaadin/addons/componentfactory/sidenavrail/app/TestApplication.java` with the full Apache 2.0 header, then:
```java
package org.vaadin.addons.componentfactory.sidenavrail.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class TestApplication {
    public static void main(String[] args) {
        SpringApplication.run(TestApplication.class, args);
    }
}
```

- [ ] **Step 2: Write `application.properties` to pin the port**

Create `/workspace/addon/src/test/resources/application.properties`:
```properties
server.port=8081
vaadin.productionMode=false
logging.level.org.atmosphere=warn
```

- [ ] **Step 3: Write a minimal main layout**

Create `/workspace/addon/src/test/java/org/vaadin/addons/componentfactory/sidenavrail/app/TestMainLayout.java` with the full Apache 2.0 header, then:
```java
package org.vaadin.addons.componentfactory.sidenavrail.app;

import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.Layout;
import org.vaadin.addons.componentfactory.sidenavrail.SideNavRail;
import org.vaadin.addons.componentfactory.sidenavrail.SideNavRailItem;

@Layout
public class TestMainLayout extends AppLayout {

    private final SideNavRail nav = new SideNavRail();

    public TestMainLayout() {
        nav.addItem(new SideNavRailItem("Basic", "basic"));
        nav.addItem(new SideNavRailItem("Collapsed Item", "collapsed-item"));
        nav.addItem(new SideNavRailItem("Rail Only", "rail-only"));
        nav.addItem(new SideNavRailItem("Nested", "nested"));

        Button toggle = new Button("Toggle rail",
                e -> nav.setRailMode(!nav.isRailMode()));
        toggle.setId("toggle-rail");

        addToNavbar(new HorizontalLayout(toggle));
        addToDrawer(nav);
    }
}
```

- [ ] **Step 4: Write the Basic test view**

Create `/workspace/addon/src/test/java/org/vaadin/addons/componentfactory/sidenavrail/app/views/BasicTestView.java` with the full Apache 2.0 header, then:
```java
package org.vaadin.addons.componentfactory.sidenavrail.app.views;

import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

@Route("basic")
public class BasicTestView extends VerticalLayout {
    public BasicTestView() {
        add(new Paragraph("Basic test view — validates rail rendering and toggle."));
    }
}
```

- [ ] **Step 5: Add `exec-maven-plugin` to the addon POM**

Open `/workspace/addon/pom.xml`. Add inside `<build><plugins>` (alongside existing plugins):
```xml
<plugin>
    <groupId>org.codehaus.mojo</groupId>
    <artifactId>exec-maven-plugin</artifactId>
    <version>3.5.0</version>
</plugin>
```

The full integration binding is added later in Task 17 — this step just puts the plugin on the classpath so we can invoke `exec:java` ad-hoc in Step 6.

- [ ] **Step 6: Boot the app manually to sanity-check**

Run (from `/workspace/addon`):
```bash
../mvnw test-compile
../mvnw exec:java \
    -Dexec.mainClass=org.vaadin.addons.componentfactory.sidenavrail.app.TestApplication \
    -Dexec.classpathScope=test
```
Expected: Spring Boot starts, Vaadin frontend bundles, app reachable at `http://localhost:8081/basic`. Ctrl+C to stop.

- [ ] **Step 7: Commit**

```bash
git add addon/src/test/java/org/vaadin/addons/componentfactory/sidenavrail/app/ \
        addon/src/test/resources/ \
        addon/pom.xml
git commit -m "test: add Spring Boot test application with BasicTestView"
```

---

## Task 15: Remaining test views

**Files:**
- Create: `addon/src/test/java/org/vaadin/addons/componentfactory/sidenavrail/app/views/PopoverCollapsedItemView.java`
- Create: `addon/src/test/java/org/vaadin/addons/componentfactory/sidenavrail/app/views/PopoverRailOnlyView.java`
- Create: `addon/src/test/java/org/vaadin/addons/componentfactory/sidenavrail/app/views/NestedPopoverView.java`

All three views construct their own `SideNavRail` inside the view body (instead of relying on the drawer nav) so Playwright tests can exercise specific modes in isolation.

- [ ] **Step 1: Write `PopoverCollapsedItemView`**

Full Apache 2.0 header, then:
```java
package org.vaadin.addons.componentfactory.sidenavrail.app.views;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import org.vaadin.addons.componentfactory.sidenavrail.PopoverMode;
import org.vaadin.addons.componentfactory.sidenavrail.SideNavRail;
import org.vaadin.addons.componentfactory.sidenavrail.SideNavRailItem;

@Route("collapsed-item")
public class PopoverCollapsedItemView extends VerticalLayout {

    public PopoverCollapsedItemView() {
        SideNavRail rail = new SideNavRail();
        rail.setPopoverMode(PopoverMode.COLLAPSED_ITEM);
        rail.setId("rail");

        SideNavRailItem code = new SideNavRailItem(
                "Code", "/code", VaadinIcon.CODE.create());
        code.addItem(new SideNavRailItem("Branches", "/code/branches"));
        code.addItem(new SideNavRailItem("Tags", "/code/tags"));
        rail.addItem(code);

        Button toggle = new Button("Toggle rail", e -> rail.setRailMode(!rail.isRailMode()));
        toggle.setId("toggle-rail");

        add(new HorizontalLayout(rail, toggle));
    }
}
```

- [ ] **Step 2: Write `PopoverRailOnlyView`**

Full Apache 2.0 header, then:
```java
package org.vaadin.addons.componentfactory.sidenavrail.app.views;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import org.vaadin.addons.componentfactory.sidenavrail.PopoverMode;
import org.vaadin.addons.componentfactory.sidenavrail.SideNavRail;
import org.vaadin.addons.componentfactory.sidenavrail.SideNavRailItem;

@Route("rail-only")
public class PopoverRailOnlyView extends VerticalLayout {

    public PopoverRailOnlyView() {
        SideNavRail rail = new SideNavRail();
        rail.setPopoverMode(PopoverMode.RAIL_ONLY);
        rail.setId("rail");

        SideNavRailItem code = new SideNavRailItem(
                "Code", "/code", VaadinIcon.CODE.create());
        code.addItem(new SideNavRailItem("Branches", "/code/branches"));
        code.addItem(new SideNavRailItem("Tags", "/code/tags"));
        rail.addItem(code);

        Button toggle = new Button("Toggle rail", e -> rail.setRailMode(!rail.isRailMode()));
        toggle.setId("toggle-rail");

        add(new HorizontalLayout(rail, toggle));
    }
}
```

- [ ] **Step 3: Write `NestedPopoverView`**

Full Apache 2.0 header, then:
```java
package org.vaadin.addons.componentfactory.sidenavrail.app.views;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import org.vaadin.addons.componentfactory.sidenavrail.SideNavRail;
import org.vaadin.addons.componentfactory.sidenavrail.SideNavRailItem;

@Route("nested")
public class NestedPopoverView extends VerticalLayout {

    public NestedPopoverView() {
        SideNavRail rail = new SideNavRail();
        rail.setId("rail");

        SideNavRailItem code = new SideNavRailItem(
                "Code", "/code", VaadinIcon.CODE.create());

        SideNavRailItem branches = new SideNavRailItem("Branches", "/code/branches");
        branches.addItem(new SideNavRailItem("Active", "/code/branches/active"));
        branches.addItem(new SideNavRailItem("Stale", "/code/branches/stale"));
        code.addItem(branches);

        code.addItem(new SideNavRailItem("Tags", "/code/tags"));
        rail.addItem(code);

        Button toggle = new Button("Toggle rail", e -> rail.setRailMode(!rail.isRailMode()));
        toggle.setId("toggle-rail");

        add(new HorizontalLayout(rail, toggle));
    }
}
```

- [ ] **Step 4: Compile**

Run: `./mvnw -pl addon test-compile`
Expected: `BUILD SUCCESS`.

- [ ] **Step 5: Commit**

```bash
git add addon/src/test/java/org/vaadin/addons/componentfactory/sidenavrail/app/views/
git commit -m "test: add popover-mode and nested-popover test views"
```

---

## Task 16: Playwright project setup

**Files:**
- Create: `addon/src/test/playwright/package.json`
- Create: `addon/src/test/playwright/playwright.config.ts`
- Create: `addon/src/test/playwright/tsconfig.json`

- [ ] **Step 1: Write `package.json`**

Create `/workspace/addon/src/test/playwright/package.json`:
```json
{
  "name": "side-nav-rail-playwright",
  "version": "0.1.0",
  "private": true,
  "type": "module",
  "scripts": {
    "test": "playwright test"
  },
  "devDependencies": {
    "@playwright/test": "^1.49.0",
    "@types/node": "^22.7.0",
    "typescript": "^5.6.0"
  }
}
```

- [ ] **Step 2: Write `playwright.config.ts`**

Create `/workspace/addon/src/test/playwright/playwright.config.ts`:
```ts
import { defineConfig, devices } from '@playwright/test';

export default defineConfig({
  testDir: './tests',
  timeout: 30_000,
  retries: 0,
  use: {
    baseURL: 'http://localhost:8081',
    trace: 'on-first-retry',
  },
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
  ],
});
```

- [ ] **Step 3: Write `tsconfig.json`**

Create `/workspace/addon/src/test/playwright/tsconfig.json`:
```json
{
  "compilerOptions": {
    "target": "ES2022",
    "module": "ESNext",
    "moduleResolution": "Bundler",
    "strict": true,
    "esModuleInterop": true,
    "skipLibCheck": true,
    "types": ["node", "@playwright/test"]
  },
  "include": ["playwright.config.ts", "tests/**/*.ts"]
}
```

- [ ] **Step 4: Install Playwright deps locally and verify**

Run:
```bash
cd /workspace/addon/src/test/playwright
npm install
npx playwright --version
```
Expected: prints a Playwright version.

- [ ] **Step 5: Commit**

```bash
git add addon/src/test/playwright/
git commit -m "test: add Playwright project scaffold"
```

> Note: `addon/src/test/playwright/package-lock.json` is generated by `npm install` and is included in the commit — `frontend-maven-plugin`'s `npm ci` in Task 17 relies on it.

---

## Task 17: Maven bindings for integration tests

**Files:**
- Modify: `addon/pom.xml`

Three plugins bind to integration phases: `spring-boot-maven-plugin` (start/stop the test app), `frontend-maven-plugin` (install Node + run `npm ci` + `npx playwright install chromium`), and `exec-maven-plugin` (invoke `npx playwright test`).

- [ ] **Step 1: Extend `addon/pom.xml` plugin section**

Open `/workspace/addon/pom.xml`. Inside `<build><plugins>`, add (in addition to `exec-maven-plugin` from Task 14 Step 5, which gets executions added):
```xml
<plugin>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-maven-plugin</artifactId>
    <version>${spring-boot.version}</version>
    <configuration>
        <mainClass>org.vaadin.addons.componentfactory.sidenavrail.app.TestApplication</mainClass>
        <classesDirectory>${project.build.testOutputDirectory}</classesDirectory>
    </configuration>
    <executions>
        <execution>
            <id>pre-integration-test</id>
            <goals><goal>start</goal></goals>
        </execution>
        <execution>
            <id>post-integration-test</id>
            <goals><goal>stop</goal></goals>
        </execution>
    </executions>
</plugin>

<plugin>
    <groupId>com.github.eirslett</groupId>
    <artifactId>frontend-maven-plugin</artifactId>
    <version>1.15.1</version>
    <configuration>
        <workingDirectory>src/test/playwright</workingDirectory>
        <installDirectory>target</installDirectory>
    </configuration>
    <executions>
        <execution>
            <id>install-node-and-npm</id>
            <phase>pre-integration-test</phase>
            <goals><goal>install-node-and-npm</goal></goals>
            <configuration>
                <nodeVersion>v20.18.0</nodeVersion>
            </configuration>
        </execution>
        <execution>
            <id>npm-ci</id>
            <phase>pre-integration-test</phase>
            <goals><goal>npm</goal></goals>
            <configuration>
                <arguments>ci</arguments>
            </configuration>
        </execution>
        <execution>
            <id>playwright-install</id>
            <phase>pre-integration-test</phase>
            <goals><goal>npx</goal></goals>
            <configuration>
                <arguments>playwright install chromium</arguments>
            </configuration>
        </execution>
    </executions>
</plugin>
```

Then update the existing `exec-maven-plugin` from Task 14 Step 5 to add an execution binding:
```xml
<plugin>
    <groupId>org.codehaus.mojo</groupId>
    <artifactId>exec-maven-plugin</artifactId>
    <version>3.5.0</version>
    <executions>
        <execution>
            <id>playwright-test</id>
            <phase>integration-test</phase>
            <goals><goal>exec</goal></goals>
            <configuration>
                <executable>npx</executable>
                <workingDirectory>${project.basedir}/src/test/playwright</workingDirectory>
                <arguments>
                    <argument>playwright</argument>
                    <argument>test</argument>
                </arguments>
            </configuration>
        </execution>
    </executions>
</plugin>
```

- [ ] **Step 2: Add `maven-failsafe-plugin` to expose `verify`**

Even though we do not use Failsafe for JUnit itself, its binding defines the `integration-test` and `verify` lifecycle goals:
```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-failsafe-plugin</artifactId>
    <executions>
        <execution>
            <goals>
                <goal>integration-test</goal>
                <goal>verify</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

- [ ] **Step 3: Sanity-check the build still compiles**

Run: `./mvnw -pl addon test`
Expected: `BUILD SUCCESS`. (Full `verify` will be exercised in Phase 6 Task 22 once E2E specs exist.)

- [ ] **Step 4: Commit**

```bash
git add addon/pom.xml
git commit -m "build: wire spring-boot + frontend + exec plugins for E2E phase"
```

---

## Phase 5 complete when

- Test runtime and all four test views compile via `./mvnw -pl addon test-compile`.
- Manual boot (`../mvnw exec:java …`) loads `http://localhost:8081/basic`.
- `addon/src/test/playwright/` contains a working npm project (Playwright CLI runs).
- Maven bindings for integration test phase are in place (no failures — even though tests don't exist yet, `./mvnw -pl addon test` still succeeds).
- Four green commits added in Phase 5.

Next: [Phase 6 — E2E tests](./06-e2e-tests.md).
