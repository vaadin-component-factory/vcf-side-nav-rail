# SideNav Rail — Design Spec (MVP, Iteration 1)

**Date:** 2026-04-21
**Status:** Design, awaiting final user review
**Source:** [`initial.md`](./initial.md) (authoritative), customer email PDF (background context, not authoritative)
**Target platform:** Vaadin 24, Lumo theme
**License:** Apache 2.0
**Publishing target:** Vaadin Component Factory addon (Directory)

> **Naming note:** The component is called `SideNavRail` in this design, not "Collapsible SideNav" as in `initial.md`. The working name was renamed during design review because "collapsible" collides with multiple existing Vaadin concepts — `SideNav.setCollapsible(true)` (label collapse) and `SideNavItem` expand/collapse for children. "Rail" is the established UI-pattern name (Material Design, Carbon) for exactly this kind of togglable, icon-driven navigation. `initial.md` is kept as-is as the original requirements record; this spec is the authoritative naming source from here on.

## 1. Scope of this iteration

This spec describes **Iteration 1 (MVP)**. Further iterations are intentionally planned; see [§9 Out of Scope](#9-out-of-scope--phase-2).

**In scope:**
- Two new public components, `SideNavRail` and `SideNavRailItem`, as subclasses of the Vaadin standard components.
- A togglable rail mode exposed as a Java API (no built-in toggle button — applications integrate their own, as `initial.md` mandates).
- A hover popover for items with children, gated by a configurable `PopoverMode`.
- Styling implemented purely through CSS on the existing `<vaadin-side-nav>` web component — no custom element of our own.
- A two-tier test pyramid (Karibu + Playwright) inside the addon module, triggered by `mvn verify`.

**Multi-iteration work sequence:**

| Phase | Content |
|---|---|
| **A** (this iteration) | Addon + demo as separate Maven modules, component functional, tests in the addon, demo showing example integration |
| B | Publishing prep: README, code examples, addon metadata |
| C | Directory submission: CI release pipeline, versioning, tags |

## 2. Project and module structure

Three Maven modules plus the reactor:

```
/workspace
├── pom.xml                                      (reactor, packaging=pom)
├── addon/                                       (addon module, packaging=jar)
│   ├── pom.xml                                  (standalone — no <parent>)
│   └── src/
│       ├── main/java/org/vaadin/addons/componentfactory/sidenavrail/
│       │   ├── SideNavRail.java
│       │   ├── SideNavRailItem.java
│       │   ├── RailModeChangedEvent.java
│       │   └── PopoverMode.java
│       ├── main/resources/META-INF/resources/frontend/
│       │   └── side-nav-rail.css                (included via @CssImport)
│       └── test/java/…/sidenavrail/unit/        (Karibu unit tests; see §7)
├── e2e/                                         (end-to-end tests, packaging=jar, never published)
│   ├── pom.xml                                  (inherits from reactor)
│   └── src/
│       ├── main/java/…/sidenavrail/e2e/         (TestApplication + views)
│       ├── main/resources/application.properties
│       └── test/playwright/                     (Playwright project)
└── demo/                                        (demo app, packaging=jar, never published)
    ├── pom.xml                                  (inherits from reactor)
    └── src/main/java/…/sidenavrail/demo/
        ├── Application.java
        ├── MainLayout.java
        └── views/
            ├── ShowcaseView.java                (@Route(""))
            └── LabelWrapSmokeView.java          (@Route("smoke/label-wrap"))
```

**Coordinates:**
- **groupId:** `org.vaadin.addons.componentfactory`
- **artifactId (addon):** `vcf-side-nav-rail` (`vcf-` prefix is standard for Vaadin Component Factory addons)
- **artifactId (e2e):** `vcf-side-nav-rail-e2e`
- **artifactId (demo):** `vcf-side-nav-rail-demo`
- **artifactId (reactor):** `vcf-side-nav-rail-parent`
- **Package:** `org.vaadin.addons.componentfactory.sidenavrail` (production) / `…sidenavrail.e2e` (test app) / `…sidenavrail.demo` (demo app)

**Module dependencies:**
- `addon/` depends on `vaadin-core` (transitively pulling `side-nav` and `popover`). No Spring Boot in compile scope. Test-scope: JUnit Jupiter + Karibu Testing v24.
- `e2e/` depends on the addon (compile) plus `vaadin-spring-boot-starter` (compile). Runs Spring Boot + Playwright via its own `pom.xml`.
- `demo/` depends on the addon (compile) plus `vaadin-spring-boot-starter` (compile). Showcase only — no tests.

**POM inheritance:**
- The `addon/pom.xml` **does not** reference a parent POM — it is fully standalone so the published artifact does not require consumers to also pull a parent. It declares its own `groupId`/`version`, imports the Vaadin BOM directly, and owns its Java/compiler properties.
- `e2e/pom.xml` and `demo/pom.xml` inherit from the reactor — they are never published, so inheritance saves version/property duplication.
- The root `pom.xml` exists as the reactor: `./mvnw verify` at the workspace root builds all three modules. The addon can also be built in isolation (`./mvnw -pl addon install`).

**Why e2e is a separate module** (and not part of `addon/`'s test scope, as the initial plan assumed): a Vaadin Flow library that wants to run a Spring Boot test app inside its own test scope runs into `spring-boot-maven-plugin` + `classesDirectory` + `useTestClasspath` friction that isn't worth fighting. Moving the test runtime into its own module makes Spring Boot compile-scope for the e2e module, keeps the addon POM minimal (no Spring Boot deps leak into the published artifact), and lets the plugin chain run with standard defaults.

## 3. Public API

### 3.1 `SideNavRail`

```java
public class SideNavRail extends SideNav {

    public SideNavRail();
    public SideNavRail(String label);

    /** Toggles the nav between rail mode and normal mode. */
    public void setRailMode(boolean railMode);
    public boolean isRailMode();

    /** Controls when the hover popover appears for items with children. Default: COLLAPSED_ITEM. */
    public void setPopoverMode(PopoverMode mode);
    public PopoverMode getPopoverMode();

    public Registration addRailModeChangedListener(
            ComponentEventListener<RailModeChangedEvent> listener);
}
```

### 3.2 `SideNavRailItem`

```java
public class SideNavRailItem extends SideNavItem {

    public SideNavRailItem(String label);
    public SideNavRailItem(String label, String path);
    public SideNavRailItem(String label, Class<? extends Component> view);
    public SideNavRailItem(String label, String path, Component prefixComponent);
    public SideNavRailItem(String label, Class<? extends Component> view, Component prefixComponent);

    // No new public methods in the MVP.
    // Internal override at two points:
    //   1. in the String-label constructors, after the super(...) call
    //   2. in setLabel(String), after super.setLabel(label) has applied the label
    // Both paths ensure the visible label is rendered inside a <span class="label">.
    // Idempotent: if the span does not exist yet, the bare text node produced by
    // super is wrapped in a new <span class="label">. If it already exists from a
    // previous call, only its text content is updated — no second span is created.
}
```

**Note:** The label wrap only applies to String labels (constructor + `setLabel(String)`). If a consumer manipulates label rendering externally or fills the default slot with their own components, the styling is their responsibility — the override only manages the text node that `super` produces.

### 3.3 `PopoverMode`

The enum name refers to the *item* state (Vaadin standard: `[expanded]` vs. not expanded), not the nav state. This makes the distinction from `RAIL_ONLY` (nav state) explicit.

```java
public enum PopoverMode {
    /** Popover on every non-expanded item-with-children — regardless of whether the nav is in rail mode. Default. */
    COLLAPSED_ITEM,
    /** Popover only when the nav as a whole is in rail mode (isRailMode() == true). */
    RAIL_ONLY
}
```

### 3.4 `RailModeChangedEvent`

```java
public class RailModeChangedEvent extends ComponentEvent<SideNavRail> {
    public RailModeChangedEvent(SideNavRail source, boolean fromClient, boolean railMode);
    public boolean isRailMode();
}
```

Fires on every call to `setRailMode(...)` that actually changes the state (no-ops don't fire), with `fromClient = false` (the rail mode is set server-side and propagated to the client via the theme attribute). The event carries the new rail-mode value alongside the standard `ComponentEvent` fields.

## 4. Interaction model

### 4.1 Behaviour

**Mental model.** The popover gives the user access to child items whenever those children are not currently visible in the nav. Children are hidden in two cases:

1. The whole nav is in **rail mode** — all labels and inline children are suppressed.
2. A specific parent item is **inline-closed** in normal mode — that item's children are hidden while the rest of the nav is fully visible.

`PopoverMode` picks which of these two cases triggers the hover popover:

| `PopoverMode` | Popover appears when… |
|---|---|
| `COLLAPSED_ITEM` *(default)* | …*any* parent item's children are hidden — whether that's because the nav is in rail mode or because the item is inline-closed in normal mode. |
| `RAIL_ONLY` | …*only* the nav is in rail mode. Inline-closed items in normal mode stay silent — they only open on click, like standard `SideNav`. |

**Full behaviour matrix (items with children):**

| Nav state | Item inline state | `COLLAPSED_ITEM` | `RAIL_ONLY` |
|---|---|---|---|
| rail | any (children hidden anyway) | Popover | Popover |
| normal | inline-closed | Popover | **No popover** |
| normal | inline-open | No popover | No popover |

Items **without** children never open a popover — they are directly clickable in every state, including rail mode.

### 4.2 Popover details

- Implementation: the Vaadin `Popover` component (Flow), one instance per `SideNavRailItem` that has children, lazily created on first `onAttach`.
- Target: the root element of the associated `SideNavRailItem`.
- Trigger: `setOpenOnHover(true)`, all other triggers disabled.
- Timing: `setHoverDelay(200)`, `setHideDelay(300)` (Lumo-typical values, made configurable in phase 2).
- Position: aligned to the right of the item, top-aligned with the item — concretely `PopoverPosition.END_TOP` if present (the Vaadin popover enum follows the `DIRECTION_ALIGNMENT` naming pattern; exact enum value to be verified during implementation, fallback `END`).
- Overlay role: `setOverlayRole("menu")`.
- Content: a secondary `SideNav` instance (not a `SideNavRail`) rendering the children of the item. Nested expand/collapse inside the popover then works via the standard `SideNav` mechanism (`initial.md`: *"Inside that popover, side nav items … can be expanded and collapsed like within the normal side nav"*).
- Rendering strategy (rebuilding a copy from the item hierarchy vs. DOM-reparenting the existing light DOM) is intentionally left open to implementation; what matters is that navigation and active highlighting work inside the popover the same way as in a standard `SideNav`.
- **Only one popover per item**, even for multiple hierarchy levels — nested popovers are excluded (`initial.md`).

### 4.3 Rail mode behaviour

- `setRailMode(true)` sets `theme="rail"` on the `<vaadin-side-nav>` root.
- Inline expansion state of individual items is left untouched; in rail mode it is only suppressed visually via CSS, and is restored to its previous state when `setRailMode(false)` is called.
- Re-attach case: on component reuse or navigation, `isRailMode()` retains its last server-side value — no automatic reset.

## 5. Styling

### 5.1 Marker attribute

`theme="rail"` is set on the `<vaadin-side-nav>`, *not* the `[collapsed]` attribute. The latter is already taken by the Vaadin-native label-collapse feature (active when `SideNav.setCollapsible(true)` and the user clicks the header).

### 5.2 CSS module

`addon/src/main/resources/META-INF/resources/frontend/side-nav-rail.css`, included via `@CssImport` on `SideNavRail`:

```css
vaadin-side-nav[theme~="rail"] {
  width: var(--lumo-size-l);
}

vaadin-side-nav[theme~="rail"] vaadin-side-nav-item .label,
vaadin-side-nav[theme~="rail"] vaadin-side-nav-item [slot="suffix"] {
  display: none;
}

vaadin-side-nav[theme~="rail"] vaadin-side-nav-item::part(toggle-button) {
  display: none;
}

vaadin-side-nav[theme~="rail"] vaadin-side-nav-item[slot="children"] {
  display: none;
}
```

### 5.3 Label wrap

`SideNavRailItem` wraps the string label in a `<span class="label">` element — both in the string constructors and in `setLabel(String)`. Goal: the text becomes selectable by CSS (text nodes are not).

**Rendering neutrality:** An inline `<span>` inherits font-family, font-size, color, line-height, and vertical-align from its parent and has no default margins or paddings. Text rendering remains visually identical to a standard `SideNavItem`. This will be verified with a side-by-side screenshot (with and without the span) in the demo view during implementation.

## 6. Packaging details

- `addon/pom.xml`:
  - `packaging: jar`, **no `<parent>`** (standalone publishable).
  - Declares its own `groupId`/`version`/properties and imports the Vaadin BOM directly.
  - Compile deps: `com.vaadin:vaadin-core`.
  - Test-scope deps: `junit-jupiter`, `kaributesting-v24` — nothing Spring Boot.
  - Plugins: `maven-compiler-plugin`, `maven-surefire-plugin`. No integration-test plugins (those moved to `e2e/`).
- `e2e/pom.xml`:
  - `packaging: jar`, inherits from reactor.
  - **Never published:** `<maven.install.skip>true</maven.install.skip>` + `<maven.deploy.skip>true</maven.deploy.skip>`.
  - Compile deps: the addon + `vaadin-spring-boot-starter`.
  - Plugins: `spring-boot-maven-plugin` (start/stop), `frontend-maven-plugin` (install Node + `npm ci` + `npx playwright install chromium`), `exec-maven-plugin` (`npx playwright test`), `maven-failsafe-plugin` (for the `verify` goal binding).
  - TestApplication + test views live in `src/main/java` (compile scope) — not `src/test/java`. The plugin chain then runs with standard defaults, no `classesDirectory`/`useTestClasspath` workarounds.
- `demo/pom.xml`:
  - `packaging: jar`, inherits from reactor.
  - Compile deps: addon + `vaadin-spring-boot-starter`.
  - Plugin: `spring-boot-maven-plugin` (for `spring-boot:run`).
- Reactor `pom.xml`:
  - `packaging: pom`.
  - Modules: `addon`, `e2e`, `demo`.
  - Vaadin BOM, Java 17 (via devcontainer Dockerfile) as `maven.compiler.release`.
  - Shared `pluginManagement`: `maven-compiler-plugin`, `maven-surefire-plugin`, `maven-failsafe-plugin`, `spotless-maven-plugin`.

## 7. Tests

The test pyramid is split across two modules: the addon holds browser-free unit tests; the e2e module holds the Spring Boot test runtime and the Playwright spec files.

### 7.1 Layout

```
addon/src/test/java/org/vaadin/addons/componentfactory/sidenavrail/unit/
├── RailModeStateTest.java        setRailMode/isRailMode + event firing
├── PopoverModeTest.java           PopoverMode accessors + gating tests
├── LabelWrapTest.java             <span class="label"> wrap invariants
└── PopoverLifecycleTest.java      popover attach/copy semantics
(JUnit + Karibu, browser-free, bound to `test` phase.)

e2e/src/main/java/org/vaadin/addons/componentfactory/sidenavrail/e2e/
├── TestApplication.java           @SpringBootApplication
└── views/
    ├── BasicTestView.java             @Route("basic")
    ├── PopoverCollapsedItemView.java  @Route("collapsed-item")
    ├── PopoverRailOnlyView.java       @Route("rail-only")
    └── NestedPopoverView.java         @Route("nested")

e2e/src/main/resources/application.properties   server.port=8081

e2e/src/test/playwright/
├── package.json / package-lock.json
├── playwright.config.ts                       baseURL = http://localhost:8081
├── tsconfig.json
└── tests/
    ├── basic.spec.ts
    ├── popover-collapsed-item.spec.ts
    ├── popover-rail-only.spec.ts
    └── nested-popover.spec.ts
```

### 7.2 Maven phase bindings (e2e module)

| Phase | Plugin | What it does |
|---|---|---|
| `test` | `maven-surefire-plugin` (addon) | Unit tests under `addon/src/test/java/…/unit/` |
| `pre-integration-test` | `spring-boot-maven-plugin:start` | Starts `TestApplication` on port 8081 |
| `pre-integration-test` | `frontend-maven-plugin` | Installs Node + runs `npm ci` + `npx playwright install chromium` inside `e2e/src/test/playwright/` |
| `integration-test` | `exec-maven-plugin` | `npx playwright test` in the playwright dir |
| `post-integration-test` | `spring-boot-maven-plugin:stop` | Stops the test app |
| `verify` | `maven-failsafe-plugin` | Fails the build if any integration-test step failed |

Because TestApplication is in `src/main/java` (compile scope) of the e2e module, `spring-boot-maven-plugin` runs with its plain defaults — no `classesDirectory`, no `useTestClasspath`, no `additionalClasspathElements`.

### 7.3 Dependencies

**Addon test-scope:** `junit-jupiter`, `kaributesting-v24`. No Spring Boot.

**E2E compile-scope:** the addon, `vaadin-spring-boot-starter` (transitively pulls everything needed to boot the test app). Playwright is installed via `npm ci`, not as a Maven dependency.

### 7.4 Commands

- `./mvnw test` from the workspace root — runs unit tests across all modules (effectively only the addon has unit tests).
- `./mvnw verify` from the workspace root — unit + E2E. Starts and stops the Spring Boot test app automatically.
- `./mvnw -pl addon install -DskipTests` — publishes the addon to the local `~/.m2` so the demo and e2e modules can resolve it from there (needed before running e2e/demo in isolation).
- Local Playwright debug (UI mode):
  1. `cd /workspace/e2e && ../mvnw spring-boot:run` — boots the test app on port 8081.
  2. In another shell: `cd /workspace/e2e/src/test/playwright && npx playwright test --ui`.

### 7.5 MVP test coverage

| Test | Level | Covers |
|---|---|---|
| `RailModeStateTest` | Unit | `setRailMode`/`isRailMode`, event firing, theme attribute application |
| `PopoverModeTest` | Unit | `setPopoverMode`/`getPopoverMode`, default value, effect of the mode on popover lifecycle |
| `LabelWrapTest` | Unit | Label gets wrapped in `<span class="label">`, structural neutrality |
| `basic.spec.ts` | E2E | Nav renders, rail mode toggle visible, icon stays, labels disappear |
| `popover-collapsed-item.spec.ts` | E2E | Default mode: popover appears for inline-closed items in normal mode AND in rail mode |
| `popover-rail-only.spec.ts` | E2E | `RAIL_ONLY` mode: popover only appears in rail mode, not for inline-closed items in normal mode |
| `nested-popover.spec.ts` | E2E | Items inside the popover with their own children can expand/collapse inline, no second popover |

## 8. Demo module (MVP content)

- `Application.java` (Spring Boot starter).
- `MainLayout.java` (`AppLayout`) with a `SideNavRail` in the drawer and a simple toggle button in the header that calls `setRailMode(!isRailMode())`.
- `ShowcaseView.java` (`@Route("")`) — shows the component in realistic use with 2–3 levels of navigation.
- No Spring Security, no persistence, no backend integration — pure component showcase.

## 9. Out of scope / Phase 2+

### 9.1 Phase 2 — user-facing polish

- Parent name as popover header (mentioned in the customer email, *not* in `initial.md`).
- Active/selected indicator on the icon when a descendant of a parent hidden in rail mode is active.
- Icon fallback or warning when a `SideNavRailItem` ends up in rail mode without an icon.
- Tooltip in rail mode for items *without* children (shows label on hover).
- Transition/animation on rail mode toggle (width, labels).
- Configurable hover delays and popover position at the nav level.

### 9.2 Phase 2 — accessibility

- Keyboard navigation in rail mode (Tab, arrow keys, Esc closes popover).
- Correct ARIA roles and attributes (`aria-haspopup`, `aria-expanded`).
- Screen reader labels for items in rail mode.
- Focus management on popover open/close.

### 9.3 Phase 2 — touch/mobile

- Touch behaviour (no hover trigger).
- Either tap-to-open or an explicit desktop-only declaration.

### 9.4 Phase 2 — tests

- A11y assertions in E2E tests (in parallel with the a11y work itself).

### 9.5 Phase B → C — publishing

- README with screenshots, code examples, feature matrix.
- Directory metadata (`addon.json`, tags, compatibility matrix).
- CI/release pipeline.
- Versioning strategy (semver).
- Release checklist.

### 9.6 Explicitly not planned

- Built-in collapse button (`initial.md`: *"The collapsible side nav will not automatically add a collapse button"*).
- Auto-collapse on viewport resize.
- Auto-hide like the `AppLayout` drawer.
- A custom TypeScript / web component — stays pure Java + CSS.

## 10. Verified during implementation

The two points flagged here at design time were both resolved cleanly:

- **CSS reach:** `::part(toggle-button)` and the `[slot="children"]` selector work without any JS helper. Pure CSS in `side-nav-rail.css` is sufficient. The "no custom element" architectural decision stands.
- **Label wrap rendering:** `SideNavRailItem` wraps the label in a `<span class="label">` both in the String constructors and in `setLabel(String)`. `LabelWrapTest` asserts idempotency (exactly one span after repeated `setLabel` calls) and that slotted children (prefix icons) survive the wrap. `demo/LabelWrapSmokeView` renders a standard `SideNav` and a `SideNavRail` side-by-side for visual confirmation.

Additional implementation-time findings worth noting:

- **Spring Boot + Java-version gotcha:** Spring Boot 3.4's bundled ASM cannot parse Java 25 class files. The compile target was pinned to Java 17 accordingly (see the devcontainer Dockerfile + `<maven.compiler.release>17</maven.compiler.release>` in the addon POM).
- **Vaadin `Element.getTag()` on text nodes:** throws `UnsupportedOperationException`. The `SideNavRailItem` implementation and `LabelWrapTest` filter with `e -> !e.isTextNode()` before calling `getTag()` in stream operations.
- **Popover attachment:** the popover is appended to the owning `SideNavRail` element rather than directly to the UI root. This has no visual effect (the overlay teleports to the body anyway) but keeps the popover in the component's logical scope.
