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

```
/workspace
├── pom.xml                                      (parent, packaging=pom)
├── addon/                                       (addon module, packaging=jar)
│   ├── pom.xml
│   └── src/
│       ├── main/java/org/vaadin/addons/componentfactory/sidenavrail/
│       │   ├── SideNavRail.java
│       │   ├── SideNavRailItem.java
│       │   ├── RailModeChangedEvent.java
│       │   └── PopoverMode.java
│       ├── main/resources/META-INF/resources/frontend/
│       │   └── side-nav-rail.css         (included via @CssImport)
│       └── test/                                (see §7)
└── demo/                                        (demo module, packaging=jar)
    ├── pom.xml
    └── src/main/java/.../demo/
        ├── Application.java
        ├── MainLayout.java
        └── views/ShowcaseView.java
```

**Coordinates:**
- **groupId:** `org.vaadin.addons.componentfactory`
- **artifactId (addon):** `vcf-side-nav-rail` (`vcf-` prefix is standard for Vaadin Component Factory addons)
- **artifactId (demo):** `vcf-side-nav-rail-demo`
- **Package:** `org.vaadin.addons.componentfactory.sidenavrail`

**Module dependencies:**
- `addon/` depends on `vaadin-core` (transitively pulling `side-nav` and `popover`). *No* Spring Boot in compile scope.
- `demo/` depends on `addon/` (compile) plus `vaadin-spring-boot-starter` (compile).

**POM inheritance:**
- The `addon/pom.xml` **does not** reference a parent POM — it is fully standalone so the published artifact does not require consumers to also pull a parent. It declares its own `groupId`/`version`, imports the Vaadin BOM directly, and owns its Java/compiler properties.
- The `demo/pom.xml` inherits from the root `pom.xml` (group/version/property reuse is fine — demo is never published).
- The root `pom.xml` only exists as a convenience reactor POM: `./mvnw verify` at the workspace root builds both modules, but neither module *requires* it to be built individually.

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
    public RailModeChangedEvent(SideNavRail source, boolean fromClient);
    public boolean isRailMode();
}
```

Fires on every call to `setRailMode(...)`, `fromClient = false` (the rail mode is set server-side and propagated to the client via the theme attribute).

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
  - `packaging: jar`
  - Compile deps: `com.vaadin:vaadin-core` (Vaadin 24, version managed via BOM in parent POM).
  - Test-scope deps: see §7.
  - `spring-boot-maven-plugin` only bound in test-scope plugin configuration, not in compile.
- `demo/pom.xml`:
  - `packaging: jar`
  - Compile deps: `addon/` + `com.vaadin:vaadin-spring-boot-starter`.
  - Contains `Application.java` with `@SpringBootApplication`.
- Parent POM (`/workspace/pom.xml`):
  - `packaging: pom`
  - Modules: `addon`, `demo`.
  - Vaadin BOM, Java 25 (devcontainer default) as `maven.compiler.release`.
  - Shared plugins: `spotless`, `maven-compiler-plugin`.

## 7. Tests

### 7.1 Layout

Everything lives in the `addon/` module. The demo module is test-free.

```
addon/src/test/java/org/vaadin/addons/componentfactory/sidenavrail/
├── unit/                              JUnit + Karibu (kaributesting-v24), browser-free
│   ├── RailModeStateTest.java
│   ├── PopoverModeTest.java
│   └── LabelWrapTest.java
└── app/                               Spring Boot test runtime
    ├── TestApplication.java
    ├── TestMainLayout.java
    └── views/
        ├── BasicTestView.java             @Route("basic")
        ├── PopoverCollapsedItemView.java  @Route("collapsed-item")
        ├── PopoverRailOnlyView.java       @Route("rail-only")
        └── NestedPopoverView.java         @Route("nested")

addon/src/test/playwright/
├── package.json
├── playwright.config.ts               baseURL = http://localhost:8081
└── tests/
    ├── basic.spec.ts
    ├── popover-collapsed-item.spec.ts
    ├── popover-rail-only.spec.ts
    └── nested-popover.spec.ts
```

### 7.2 Maven phase bindings

| Phase | Plugin | What it does |
|---|---|---|
| `test` | `maven-surefire-plugin` | Unit tests in `unit/` (naming: `*Test.java`) |
| `pre-integration-test` | `spring-boot-maven-plugin:start` | Starts `TestApplication` on port 8081 |
| `integration-test` | `frontend-maven-plugin` + `exec-maven-plugin` | `npm ci` + `npx playwright test` inside `src/test/playwright/` |
| `post-integration-test` | `spring-boot-maven-plugin:stop` | Stops the test app |

**Note on `spring-boot-maven-plugin` in a library project:** because the addon module itself is not a Spring Boot application (no Spring Boot on the compile classpath), the plugin must be configured explicitly with `<mainClass>…TestApplication</mainClass>` and `<classesDirectory>${project.build.testOutputDirectory}</classesDirectory>` so it picks up the test app from the test classpath. If the plugin misbehaves in this configuration, the fallback is `exec-maven-plugin:exec` with `classpathScope=test` and a manual shutdown — the implementation plan will decide.

### 7.3 Test-scope dependencies

- `org.springframework.boot:spring-boot-starter-web`
- `com.vaadin:vaadin-spring-boot-starter`
- `com.github.mvysny.kaributesting:karibu-testing-v24`
- `org.junit.jupiter:junit-jupiter`

Playwright itself is installed via `npm ci` inside `src/test/playwright/` and is not a Maven dependency.

### 7.4 Commands

- `./mvnw test` — unit tests only.
- `./mvnw verify` — unit + E2E (starts and stops the test app automatically).
- Local Playwright debug (UI mode): start `TestApplication` on port 8081 manually, then run `npx playwright test --ui` inside the test playwright folder. The concrete start command for `TestApplication` (which lives in the addon module's `test` scope, not in the demo) will be defined during implementation — options include a Surefire fork, `exec:java` with `classpathScope=test`, or a small helper script analogous to `./server-start.sh`.

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

## 10. Open points at implementation time

No spec-critical points are open. The following are to be verified during implementation:

- **CSS reach:** if `::part(toggle-button)` or hiding `[slot="children"]` does not behave as expected (e.g. due to unforeseen web-component internals), document it and decide in a small phase-1.5 mini-round whether a ~5-line JS helper is needed. The architectural decision "no custom element" remains.
- **Label wrap rendering:** a side-by-side screenshot (with and without `<span>`) in the demo view as a verification smoke test before the spec is declared implemented as designed.
