# SideNav Rail — Design Spec (MVP, Iteration 1)

**Date:** 2026-04-21 (design), 2026-04-23 (last sync with implementation)
**Status:** Implemented through §9.1 (user-facing polish). §9.2 (accessibility) in design — branch `phase9.2/accessibility`.
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
- A hover popover for items with children, gated by a configurable `PopoverOn`.
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
│       │   └── PopoverOn.java
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

    /** Controls when the hover popover appears for items with children. Default: ALL_COLLAPSED_ITEMS. */
    public void setPopoverOn(PopoverOn mode);
    public PopoverOn getPopoverOn();

    /** Whether (and how) each popover renders a header for its parent. Default: NONE (opt-in). */
    public void setPopoverParentLabelMode(PopoverParentLabelMode mode);
    public PopoverParentLabelMode getPopoverParentLabelMode();

    /** Which root items surface their label as a tooltip in rail mode. Default: ALL. */
    public void setRailTooltipMode(RailTooltipMode mode);
    public RailTooltipMode getRailTooltipMode();

    /** Use the browser-native `title` tooltip instead of the addon's CSS pseudo-element. Default: false. */
    public void setRailTooltipNative(boolean useNative);
    public boolean isRailTooltipNative();

    /** Hover delay (ms) before the popover opens. Default: 200. */
    public void setPopoverHoverDelay(int hoverDelayMs);
    public int getPopoverHoverDelay();

    /** Hide delay (ms) after mouseout before the popover closes. Default: 300. */
    public void setPopoverHideDelay(int hideDelayMs);
    public int getPopoverHideDelay();

    /** Popover position relative to its item. Default: END_TOP. */
    public void setPopoverPosition(PopoverPosition position);
    public PopoverPosition getPopoverPosition();

    public Registration addRailModeChangedListener(
            ComponentEventListener<RailModeChangedEvent> listener);

    /**
     * Only {@link SideNavRailItem} children are accepted. Passing a plain
     * {@link SideNavItem} throws {@link IllegalArgumentException} — the label wrap
     * and popover gating depend on {@code SideNavRailItem}'s overrides.
     */
    @Override public void addItem(SideNavItem... items);
    @Override public void addItemAsFirst(SideNavItem item);
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

    // Same type-guard as SideNavRail: only SideNavRailItem children accepted —
    // plain SideNavItem throws IllegalArgumentException.
    @Override public void addItem(SideNavItem... items);
    @Override public void addItemAsFirst(SideNavItem item);
}
```

**Note:** The label wrap only applies to String labels (constructor + `setLabel(String)`). If a consumer manipulates label rendering externally or fills the default slot with their own components, the styling is their responsibility — the override only manages the text node that `super` produces.

**Letter-avatar fallback:** if an item has a non-blank label but no prefix component, `onAttach` (and every subsequent `setLabel` / `setPrefixComponent(null)`) auto-generates a `vaadin-avatar` (`LUMO_SMALL`, 24×24) with the first letter of the label (uppercase) as its abbreviation. The avatar carries the marker class `side-nav-rail-letter-avatar`; CSS hides it in normal mode and shows it in rail mode so the item doesn't collapse to a blank tile. A user-provided prefix always wins — the addon only fills an empty slot and never overwrites a real icon.

### 3.3 `PopoverOn`

Three values, each describing which set of items is eligible for the hover popover:

```java
public enum PopoverOn {
    /** Every non-expanded item with children, any depth, any nav state. Default. */
    ALL_COLLAPSED_ITEMS,
    /** Only direct children of the SideNavRail (top level). Nested items never get a popover. */
    ONLY_ROOT_COLLAPSED_ITEMS,
    /** Any item with children, but only while the whole nav is in rail mode. */
    ONLY_RAIL_MODE
}
```

`ONLY_ROOT_COLLAPSED_ITEMS` in rail mode is effectively equivalent to `ALL_COLLAPSED_ITEMS` because rail mode already hides nested items, so the root-only restriction is a no-op there.

### 3.4 `PopoverParentLabelMode`

Opt-in header on the popover overlay that identifies the parent item. Default is
{@code NONE} — existing MVP behaviour is unchanged unless the consumer opts in.

```java
public enum PopoverParentLabelMode {
    /** No header. Default. */
    NONE,
    /** Header shows the parent's text label only. */
    LABEL_ONLY,
    /** Header shows a copy of the parent's prefix component (typically an icon) only. */
    ICON_ONLY,
    /** Header shows both the prefix component and the label, icon first. */
    FULL
}
```

**Rendering:**

- A single `Div` with CSS class `side-nav-rail-popover-header` is inserted as the first child of the popover, before the nested `SideNav` that renders the children.
- The label is wrapped in a `Span.side-nav-rail-popover-header-label`; the icon is cloned via the same `copyComponent(...)` helper used for the nested `SideNav` items.
- Graceful empty handling: if the configured mode asks for content the parent does not provide (`ICON_ONLY` with no prefix component, `LABEL_ONLY` with a blank label), the header is skipped entirely rather than rendered blank.
- Live switch: `setPopoverParentLabelMode(...)` rebuilds the content of all existing popovers so the header toggles without requiring a reattach.

**Known limitation:** a parent's `setLabel(...)` or `setPrefixComponent(...)` applied <em>after</em> the popover exists is not picked up automatically. Call `setPopoverParentLabelMode(...)` again (e.g. with the current value) to force a rebuild, or configure the item before the popover is first populated.

### 3.5 `RailTooltipMode`

Controls which root items surface their label as a native Vaadin tooltip while the
rail is in rail mode. Tooltips are never shown in normal mode — the label is already
visible there — and they apply to <em>direct children of the rail only</em>, never to
nested items.

```java
public enum RailTooltipMode {
    /** No tooltips on root items. */
    NONE,
    /** Tooltip only on root items that have no children (popover-less leaves). */
    ONLY_WITHOUT_CHILDREN,
    /** Tooltip on every root item. Default. */
    ALL
}
```

**Rendering:** the tooltip is a **pure-CSS pseudo-element** (`::after`) on the rail
item, driven by a `data-rail-tooltip` DOM attribute that `SideNavRail` sets on each
eligible item. Styling mirrors `vaadin-tooltip-overlay` (Lumo `contrast-90pct`
background, `base-color` text, `border-radius-s`, `font-size-s`); the position is
top-left of the item so it doesn't collide horizontally with the popover.
Configurable via CSS custom properties:

- `--side-nav-rail-tooltip-hover-delay` (default `500ms`)
- `--side-nav-rail-tooltip-fade-duration` (default `120ms`)

**Trigger:** the tooltip becomes visible on either pointer **hover** *or*
keyboard **focus** of the item. The focus path uses `:focus-within` so it crosses
shadow-DOM boundaries — focus on a `vaadin-side-nav-item` lands on the inner
`<a>` inside its shadow root, which a host-level `:focus-visible` would not match,
and `:has(:focus-visible)` does not pierce shadow boundaries. `:focus-within` does.
Both trigger paths share the same hover-delay and fade timings; the native
browser `title` tooltip (used when `setRailTooltipNative(true)`) only appears on
hover and is therefore unsuited as a sole accessibility affordance.

**Why pseudo-element instead of `vaadin-tooltip`:** Vaadin's native tooltip
auto-dismisses itself whenever a peer overlay opens — `vaadin-tooltip-mixin`
listens on `document.body` for `vaadin-overlay-open` events (see
[vaadin/web-components#9768](https://github.com/vaadin/web-components/issues/9768)).
That meant tooltips on items with a hover popover would flash and disappear as the
popover opened. A CSS pseudo-element isn't part of the overlay system, so it stays
visible alongside the popover.

**Native-tooltip fallback:** `SideNavRail.setRailTooltipNative(true)` switches from
the pseudo-element to the browser's native `title` tooltip. No overlay interaction,
no Vaadin styling — the browser decides delay, position, and look. Useful when
`title` semantics are specifically needed (assistive tech, automation tooling) or
when consumers want the OS-native rendering.

### 3.6 `RailModeChangedEvent`

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

`PopoverOn` picks which of these two cases triggers the hover popover, and whether the rule applies to every item with children or only to root items (direct children of the rail):

| `PopoverOn` | Popover appears when… |
|---|---|
| `ALL_COLLAPSED_ITEMS` *(default)* | …*any* parent item's children are hidden — whether because the nav is in rail mode or because the item is inline-closed in normal mode. Applies at every depth. |
| `ONLY_ROOT_COLLAPSED_ITEMS` | …same condition as `ALL_COLLAPSED_ITEMS`, but restricted to **root items** (direct children of the `SideNavRail`). Nested parents never open a popover, even when their children are hidden. |
| `ONLY_RAIL_MODE` | …*only* the nav is in rail mode. Inline-closed items in normal mode stay silent — they only open on click, like standard `SideNav`. |

**Full behaviour matrix (items with children):**

| Nav state | Item depth | Item inline state | `ALL_COLLAPSED_ITEMS` | `ONLY_ROOT_COLLAPSED_ITEMS` | `ONLY_RAIL_MODE` |
|---|---|---|---|---|---|
| rail | root | any (children hidden anyway) | Popover | Popover | Popover |
| rail | nested | — (not reachable: rail mode hides nested items) | — | — | — |
| normal | root | inline-closed | Popover | Popover | **No popover** |
| normal | root | inline-open | No popover | No popover | No popover |
| normal | nested | inline-closed | Popover | **No popover** | **No popover** |
| normal | nested | inline-open | No popover | No popover | No popover |

In rail mode, `ONLY_ROOT_COLLAPSED_ITEMS` is effectively equivalent to `ALL_COLLAPSED_ITEMS`: rail mode hides nested items entirely, so the root-only restriction never has anything to filter out.

Items **without** children never open a popover — they are directly clickable in every state, including rail mode.

**Live state transitions:** the popover reacts to inline-expand toggles so its visibility stays in sync with whether the item's children are already visible elsewhere:

- **Inline-expand while popover is open** (`false → true` transition): the children are now shown inline, so the popover is redundant — `popover.close()` is invoked.
- **Inline-collapse while still hovering** (`true → false` transition): the children are hidden again but the user's cursor is still on the item (they just clicked the toggle). The popover is opened explicitly; Vaadin's hover trigger would otherwise wait for the next `mouseenter`.

Both effects are driven by the `expanded-changed` DOM event on the underlying `<vaadin-side-nav-item>`. The listener tracks the last-known expanded state server-side and only reacts to real transitions — the initial attach-time fire (which carries the current value, not a change) is ignored, so popovers don't pop open on page load.

### 4.2 Popover details

- Implementation: the Vaadin `Popover` component (Flow), one instance per `SideNavRailItem` that has children, lazily created on first `onAttach`.
- Target: the root element of the associated `SideNavRailItem`.
- Trigger: `setOpenOnHover(true)` gated by `applyPopoverGating(mode, railMode)`; re-evaluated on rail-mode toggle, popover-mode change, and the item's `expanded-changed` DOM event. When the gate flips to ineligible while the popover is open, `popover.close()` is called so it disappears immediately.
- Focus trigger (rail mode only): with §9.2 the popover additionally opens on keyboard focus of its target root item (`setOpenOnFocus(true)`). Outside rail mode, focus-triggered opening is not used (popovers stay hover-only in normal mode, consistent with the inline-expand UX). See [§4.4.4](#444-focus-entry-and-exit).
- Timing: default `setHoverDelay(200)`, `setHideDelay(300)` (Lumo-typical). Configurable at the nav level via `SideNavRail.setPopoverHoverDelay(int)` and `setPopoverHideDelay(int)` — new values propagate to every existing popover immediately and seed new ones created after the change.
- Position: default `PopoverPosition.END_TOP` (top-aligned, to the inline-end of the item). Configurable via `SideNavRail.setPopoverPosition(PopoverPosition)` — new values propagate live to existing popovers, and any non-null `PopoverPosition` value is accepted (rails pinned to the inline-end of a layout typically use `START_TOP`, etc.).
- Overlay role: `setOverlayRole("menu")`.
- Content: a secondary `SideNav` instance (not a `SideNavRail`) rendering the children of the item. Nested expand/collapse inside the popover then works via the standard `SideNav` mechanism (`initial.md`: *"Inside that popover, side nav items … can be expanded and collapsed like within the normal side nav"*).
- Rendering strategy (rebuilding a copy from the item hierarchy vs. DOM-reparenting the existing light DOM) is intentionally left open to implementation; what matters is that navigation and active highlighting work inside the popover the same way as in a standard `SideNav`.
- **Only one popover per item**, even for multiple hierarchy levels — nested popovers are excluded (`initial.md`).
- Optional header identifying the parent — see [§3.4 `PopoverParentLabelMode`](#34-popoverparentlabelmode). Default is no header.

### 4.3 Rail mode behaviour

- `setRailMode(true)` sets `theme="rail"` on the `<vaadin-side-nav>` root.
- Inline expansion state of individual items is left untouched; in rail mode it is only suppressed visually via CSS, and is restored to its previous state when `setRailMode(false)` is called.
- Re-attach case: on component reuse or navigation, `isRailMode()` retains its last server-side value — no automatic reset.

### 4.4 Keyboard navigation

The rail is fully keyboard-navigable in both modes. Arrow keys drive focus and expand/collapse; Tab stays functional as a conservative fallback (walks root items; nested children follow DOM order — Vaadin default). The implementation lives in a small client-side JS module (`frontend/side-nav-rail.js`) that installs one delegated `keydown` listener on the `<vaadin-side-nav>` root. No custom web component, no new Java API.

**Rationale for client-side handling.** Keyboard navigation must feel immediate; a Flow server-roundtrip per arrow-key press introduces unacceptable latency. The JS adapter is event-handler glue only — it does not define new custom elements and does not replace any Vaadin component. See [§9.6](#96-explicitly-not-planned) for the boundary.

**Focus-stop at list boundaries** (no wrap). Consistent behaviour between root-level and nested-level arrow navigation matters more than WAI-ARIA menubar's wrap convention — users mentally model "arrow keys walk within the current level".

**Arrow-key handling short-circuits the browser's default scroll** via `preventDefault()` only when focus is inside the nav. Normal page scrolling via wheel, Space, Page-Up/Down, etc. stays unaffected.

#### 4.4.1 Normal mode (rail off)

Focus on any `vaadin-side-nav-item`:

| Key | Action |
|---|---|
| Arrow-Down | Move focus to next **visible** item (skips collapsed subtrees). Stop at last. |
| Arrow-Up | Move focus to previous visible item. Stop at first. |
| Arrow-Right | If item has children and is collapsed: expand. If already expanded: move focus to first child. Else (leaf): no-op. |
| Arrow-Left | If item is expanded: collapse. Else (collapsed or leaf): move focus to parent item. At top level: no-op. |
| Enter / Space | Activate link (native browser behaviour on `<a href="…">`). |

"Visible items" = items whose ancestors are all expanded. Collapsed subtrees are skipped from the arrow-key walk.

#### 4.4.2 Rail mode — focus on a root item

| Key | Action |
|---|---|
| Arrow-Down | Next root item. Stop at last. |
| Arrow-Up | Previous root item. Stop at first. |
| Arrow-Right | Universal "into the popover": open popover if closed, then move focus to first popover menu item. |
| Arrow-Left | No-op (no parent above a root item). |
| Enter / Space | Activate link (native). |
| Esc | Close popover if open. Focus stays on root item. |

Popover auto-opens on focus (`Popover.setOpenOnFocus(true)` — see §4.2). Arrow-Right on a first-focus therefore typically moves focus into an already-open popover. If the user closed the popover with Esc, Arrow-Right reopens it.

#### 4.4.3 Rail mode — focus inside a popover

| Key | Action |
|---|---|
| Arrow-Down | Next popover menu item at the current level. Stop at last. |
| Arrow-Up | Previous popover menu item at the current level. Stop at first. |
| Arrow-Right | If item has sub-items and is collapsed: expand. If expanded: move focus to first sub-item. Else (leaf): no-op. |
| Arrow-Left | If item's sub-items are expanded: collapse. Else if the item is nested: move focus to its popover-parent. Else (top level of the popover): close popover and return focus to the owning rail-root item (flow-preserving alternative to Esc). |
| Enter / Space | Activate link (native). |
| Esc | Close popover and return focus to rail-root item. Always works at any depth — "panic key" guarantee. |

#### 4.4.4 Focus entry and exit

- **Mouse click / hover** on a rail-root: unchanged. Popover opens on hover; focus is not moved.
- **Tab onto a rail-root**: focus lands on the item, popover auto-opens via `setOpenOnFocus`. Focus stays on the item (Arrow-Right moves it in).
- **Tab-away from a rail-root** with an open popover: popover closes (Vaadin's `closeOnFocusOut` default).
- **Tab from inside a popover**: focus moves to the next element in document order (typically the next rail-root item); popover closes.

#### 4.4.5 ARIA attributes

- `<vaadin-side-nav>` root role: unchanged — Vaadin's default `role="navigation"` (Landmark). We deliberately do **not** switch to `role="menubar"`: that would alter screen-reader announcements for every SideNav user of the component and lock us into stricter WAI-ARIA menubar semantics (which we don't fully match — e.g., `menubar` prescribes wrap on arrow navigation).
- Root items with children, while rail mode is active: `aria-haspopup="menu"` + `aria-expanded` synchronized with the popover's open/close state. Both attributes are cleared on leaving rail mode.
- Popover overlay: `role="menu"` is already set on the overlay (see §4.2). Popover menu items receive `role="menuitem"` via the client adapter.
- Active route: `aria-current="page"` — Vaadin sets this natively on route matches; no additional work.

## 5. Styling

### 5.1 Marker attributes

- `theme="rail"` on the `<vaadin-side-nav>` root marks rail mode. We deliberately do **not** use `[collapsed]` — the latter is already owned by Vaadin's native label-collapse feature (active when `SideNav.setCollapsible(true)` and the user clicks the header).
- `[root-item]` on a `<vaadin-side-nav-item>` marks a direct child of the rail (top-level navigation). The addon sets the attribute automatically in `SideNavRail.addItem` / `addItemAsFirst` and does not style it itself — it is a hook for consumer CSS that wants to distinguish root items from nested ones. Typical use (e.g. for the active-descendant indicator in [§9.1](#91-phase-2--user-facing-polish)):
- `aria-haspopup="menu"` and `aria-expanded` on root items with children while rail mode is active (see [§4.4.5](#445-aria-attributes)). Both are addon-managed: set on rail-mode enter + popover open/close, cleared on rail-mode exit. They are not public styling hooks but are documented here as observable DOM state for testing and assistive-tech debugging.

  ```css
  /* highlight the root item's icon when any descendant route is active */
  vaadin-side-nav-item[root-item]:has([current]) > vaadin-icon {
      color: var(--lumo-primary-color);
  }
  ```

  Combine with `SideNavItem.setMatchNested(true)` on the root if you want `[current]` to propagate from descendant routes.

### 5.2 CSS module

`addon/src/main/resources/META-INF/resources/frontend/side-nav-rail.css`, included via `@CssImport` on `SideNavRail`. The essentials:

```css
/* Rail width + coordinated transition for the mode toggle */
vaadin-side-nav {
  --side-nav-rail-transition-duration: 200ms;
  --side-nav-rail-transition-easing: ease-out;
  transition: width
    var(--side-nav-rail-transition-duration)
    var(--side-nav-rail-transition-easing);
}
vaadin-side-nav[theme~="rail"] { width: var(--lumo-size-l); }

/* Labels + suffix fade out AND collapse horizontally during the rail toggle */
vaadin-side-nav vaadin-side-nav-item .label,
vaadin-side-nav vaadin-side-nav-item [slot="suffix"] {
  opacity: 1;
  max-width: 20em;
  overflow: hidden;
  white-space: nowrap;
  transition:
    opacity  var(--side-nav-rail-transition-duration) var(--side-nav-rail-transition-easing),
    max-width var(--side-nav-rail-transition-duration) var(--side-nav-rail-transition-easing);
}
vaadin-side-nav[theme~="rail"] vaadin-side-nav-item .label,
vaadin-side-nav[theme~="rail"] vaadin-side-nav-item [slot="suffix"] {
  opacity: 0;
  max-width: 0;
}

/* Toggle-button fades out, inline children snap away (no reliable height to animate) */
vaadin-side-nav vaadin-side-nav-item::part(toggle-button) {
  opacity: 1;
  transition: opacity
    var(--side-nav-rail-transition-duration)
    var(--side-nav-rail-transition-easing);
}
vaadin-side-nav[theme~="rail"] vaadin-side-nav-item::part(toggle-button) {
  opacity: 0;
  pointer-events: none;
}
vaadin-side-nav[theme~="rail"] vaadin-side-nav-item[slot="children"] { display: none; }

/* Letter-avatar fallback (see §3.2) — hidden normally, visible in rail */
vaadin-avatar.side-nav-rail-letter-avatar { display: none; }
vaadin-side-nav[theme~="rail"] vaadin-avatar.side-nav-rail-letter-avatar { display: inline-flex; }

/* Rail-mode tooltip (see §3.5) — pseudo-element fed by data-rail-tooltip */
vaadin-side-nav[theme~="rail"] { position: relative; z-index: 10000; overflow: visible; }
vaadin-side-nav[theme~="rail"] vaadin-side-nav-item[data-rail-tooltip] { position: relative; }
vaadin-side-nav[theme~="rail"] vaadin-side-nav-item[data-rail-tooltip]::after {
  content: attr(data-rail-tooltip);
  position: absolute;
  /* top-left above the icon so it doesn't clash horizontally with the popover */
  background: var(--lumo-contrast-90pct); color: var(--lumo-base-color);
  padding: 0.1875em var(--lumo-space-xs);
  border-radius: var(--lumo-border-radius-s);
  font-size: var(--lumo-font-size-s);
  opacity: 0;
  transition: opacity var(--side-nav-rail-tooltip-fade-duration, 120ms);
}
vaadin-side-nav[theme~="rail"] vaadin-side-nav-item[data-rail-tooltip]:hover::after,
vaadin-side-nav[theme~="rail"] vaadin-side-nav-item[data-rail-tooltip]:focus-within::after {
  opacity: 1;
  transition-delay: var(--side-nav-rail-tooltip-hover-delay, 500ms);
}
```

**Customizable transition:** the duration and easing are CSS custom properties set on `vaadin-side-nav`; consumers can override them at any scope (per-rail, theme-level, or `:root`). Setting `--side-nav-rail-transition-duration: 0s` disables the animation entirely.

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
  - Plugins: `vaadin-maven-plugin` (`prepare-frontend` + `build-frontend` in `compile` — builds the production JS bundle into the JAR), `spring-boot-maven-plugin` (start/stop), `frontend-maven-plugin` (installs Node + `npm ci` + `npx playwright install chromium`), `exec-maven-plugin` (`npx playwright test`), `maven-failsafe-plugin` (for the `verify` goal binding).
  - Runs in **production mode** (`vaadin.productionMode=true` in `application.properties`). E2E tests verify the production artifact, not the dev-mode hot-deploy pipeline.
  - TestApplication + test views live in `src/main/java` (compile scope) — not `src/test/java`. The plugin chain then runs with standard defaults, no `classesDirectory`/`useTestClasspath` workarounds.
- `demo/pom.xml`:
  - `packaging: jar`, inherits from reactor.
  - Compile deps: addon + `vaadin-spring-boot-starter`.
  - Plugin: `spring-boot-maven-plugin` (for `spring-boot:run`).
  - Runs in dev mode with `vaadin.frontend.hotdeploy=true` — Vite HMR is useful here because the demo is bedient by humans during development/showcasing, not by CI.
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
├── PopoverOnTest.java           PopoverOn accessors + gating tests
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

The `compile` phase of the e2e module runs `vaadin-maven-plugin:build-frontend`, which invokes Vite once and writes the production bundle into `target/classes/META-INF/VAADIN/`. The bundle ships inside the JAR that `spring-boot:start` then boots, so the test app starts in ~2.5 s with zero dev-mode compile on first page load.

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
| `PopoverOnTest` | Unit | `setPopoverOn`/`getPopoverOn`, default value, effect of the mode on popover lifecycle |
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

- ~~Parent name as popover header (mentioned in the customer email, *not* in `initial.md`).~~ **Shipped** — see [§3.4 `PopoverParentLabelMode`](#34-popoverparentlabelmode) and [§4.2](#42-popover-details). Opt-in with four modes: `NONE` (default), `LABEL_ONLY`, `ICON_ONLY`, `FULL`.
- ~~Active/selected indicator on the icon when a descendant of a parent hidden in rail mode is active.~~ **Exposed as a styling hook** — see [§5.1](#51-marker-attributes). `SideNavRail` marks each direct child with the `[root-item]` attribute so consumer CSS can target them via `vaadin-side-nav-item[root-item]:has([current]) > vaadin-icon`. The actual visual treatment is app-level CSS on purpose — different apps want different looks, and Vaadin's standard `setMatchNested(true)` + `[current]` mechanism already covers the detection side.
- ~~Icon fallback or warning when a `SideNavRailItem` ends up in rail mode without an icon.~~ **Shipped as a letter-avatar fallback** — see [§3.2](#32-sidenavrailitem) and [§5.2](#52-css-module). A `SideNavRailItem` without a prefix component auto-generates a `vaadin-avatar` (`LUMO_SMALL`, 24×24 to match the Lumo icon size) whose abbreviation is the first letter of the label, uppercase. Hidden in normal mode, visible in rail mode. The avatar is replaced by any user-provided prefix component and regenerated if the user later clears it.
- ~~Tooltip in rail mode for items *without* children (shows label on hover).~~ **Shipped as `RailTooltipMode`** — a three-valued enum on `SideNavRail` controlling which root items surface their label as a native Vaadin tooltip while rail mode is active. See [§3.5 `RailTooltipMode`](#35-railtooltipmode). Values: `NONE`, `ONLY_WITHOUT_CHILDREN`, `ALL` (default). Tooltips are never shown in normal mode; leaving rail mode clears them. Scope extended beyond the original "only items without children" wording: we set tooltips on *all* root items by default because `PopoverParentLabelMode.NONE` (the default) means a parent popover doesn't repeat the label, so the tooltip provides a consistent discovery mechanism across all root items.
- ~~Transition/animation on rail mode toggle (width, labels).~~ **Shipped as CSS transitions** — see [§5.2](#52-css-module). Rail width, label text, suffix, and the expand-toggle chevron fade/collapse together over a single configurable duration + easing (`--side-nav-rail-transition-duration` default `200ms`, `--side-nav-rail-transition-easing` default `ease-out`). Setting the duration to `0s` disables the animation. No Java API added.
- ~~Configurable hover delays and popover position at the nav level.~~ **Shipped** — see [§3.1](#31-sidenavrail) for the three new setters (`setPopoverHoverDelay`, `setPopoverHideDelay`, `setPopoverPosition`) and [§4.2](#42-popover-details) for the behaviour. Defaults unchanged (200/300/`END_TOP`). Live propagation to all existing popovers; new popovers seed from the current values.

### 9.2 Phase 2 — accessibility

Designed — see [§4.4](#44-keyboard-navigation) for the full keyboard behaviour and [§4.4.5](#445-aria-attributes) for ARIA.

- ~~Keyboard navigation in rail mode (Tab, arrow keys, Esc closes popover).~~ **Designed** as full keyboard navigation in **both** modes (normal + rail). Arrow-Up/Down walks the current level (stop at boundaries), Arrow-Right expands or moves into sub-items / popover, Arrow-Left collapses or returns. Tab is preserved as a conservative fallback. See [§4.4](#44-keyboard-navigation).
- ~~Correct ARIA roles and attributes (`aria-haspopup`, `aria-expanded`).~~ **Designed** — addon-managed on root items while rail mode is active, cleared on exit. `role="navigation"` (the Vaadin default) is kept over `role="menubar"` to avoid altering screen-reader announcements for existing users. See [§4.4.5](#445-aria-attributes) and [§5.1](#51-marker-attributes).
- ~~Screen reader labels for items in rail mode.~~ **Covered by the existing DOM:** labels stay in the item DOM (collapsed to `max-width: 0`, not `display: none` — see [§5.2](#52-css-module)), so screen readers still announce them. Verified as part of keyboard-nav E2E tests; no Java API added.
- ~~Focus management on popover open/close.~~ **Designed** — see [§4.4.4](#444-focus-entry-and-exit). Popover auto-opens on focus, closes on focus-out (Vaadin default `closeOnFocusOut`); Esc returns focus to the rail-root. Arrow-Right moves focus into the popover; Arrow-Left at popover top level exits it.

**Implementation approach:** a new client-side JS module `frontend/side-nav-rail.js` with one delegated `keydown` listener on the `<vaadin-side-nav>` root. No new Java API, no custom web component — see [§9.6](#96-explicitly-not-planned) for the boundary.

### 9.3 Phase 2 — touch/mobile

~~Touch behaviour (no hover trigger). Either tap-to-open or an explicit desktop-only declaration.~~ **Dropped** — not part of the original customer requirements (`initial.md`), and `AppLayout` remains the recommended approach for mobile navigation. Moved to [§9.6](#96-explicitly-not-planned).

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
- Touch/mobile adaptations (tap-to-open, no-hover variant). Not in `initial.md`; `AppLayout` remains the recommended mobile-navigation approach. Moved here from [§9.3](#93-phase-2--touchmobile).
- A custom TypeScript / web component — stays pure Java + CSS. The keyboard adapter introduced for [§9.2](#92-phase-2--accessibility) is a small event-handler JS module (`side-nav-rail.js`), not a custom element; it registers delegated listeners on the stock `<vaadin-side-nav>` and does not replace any Vaadin component.

## 11. Active view item lookup (post-9.5)

A pair of getters on `SideNavRail` that resolve the currently active view to the
matching `SideNavRailItem`(s). The intent is to give application code a typed
hook on the navigation tree (e.g. for breadcrumbs, page titles, contextual
secondary chrome) without duplicating the route-matching logic.

### 11.1 Public API

```java
public class SideNavRail extends SideNav {

    /**
     * Returns all rail items whose own path (or any path alias) equals the
     * current view's location, in DFS pre-order. Usually 0 or 1 elements;
     * more only when the user has configured colliding paths or aliases.
     */
    public List<SideNavRailItem> getActiveViewItems();

    /**
     * The first match from {@link #getActiveViewItems()}, or empty if no item
     * matches. Convenience accessor for the typical single-match case.
     */
    public Optional<SideNavRailItem> getActiveViewItem();
}
```

### 11.2 Match semantics

- **Path equality, no nesting.** An item is active iff its own `getPath()` or
  one of its `getPathAliases()` equals the current location's path. `matchNested`
  is **deliberately ignored** — a parent does not become active because one of
  its descendants matches.
- **Aliases count.** Mirrors Vaadin's client-side `[active]` semantics, which
  also matches aliases. Without this, server-side answer and visual highlight
  could diverge.
- **Path-equality only.** Query parameters and route-template placeholders are
  not part of V1. Concrete route parameters work transparently because Vaadin's
  `setPath(view, RouteParameters)` already stores the resolved path.
- **Tree scope.** Only `SideNavRailItem`s actually inserted by user code are
  considered. The DFS walks the rail's own item tree — clones generated for
  popover content are skipped (they live under a separate nested `SideNav`,
  not in `SideNavRail.getItems()`).

### 11.3 Location source

`UI.getCurrent().getInternals().getActiveViewLocation()` is used as the source
of truth for the current location. It is technically `@Internal` API on
Vaadin's side, but has been stable across V24 and V25 in practice and is the
same value that fires through `AfterNavigationEvent.getLocation()`. A future
refactor to a self-managed cache populated via `addAfterNavigationListener`
remains possible without breaking the public API.

### 11.4 Multi-match handling

Two real-world causes of multiple matches:

1. The user wires the same path to two items (typo or intentional duplicate).
2. Path-alias collision — Item A's `path` matches Item B's alias.

Both are surfaced in `getActiveViewItems()` so consumers can detect them.
`getActiveViewItem()` returns the first DFS-pre-order match, which is
deterministic but tied to insertion order; if disambiguation matters, callers
should iterate the list themselves.

### 11.5 Out of scope for V1

- `matchNested`-style folding (a parent matching when a descendant matches).
- Active-item change events / listener hook. If demand emerges, the
  implementation can switch to a self-managed cache fed by an
  `AfterNavigationListener` and add `addActiveItemChangeListener`
  non-breakingly.
- Query-parameter and route-template matching.
- A `findItemsForLocation(Location)` overload — added later if needed.

## 10. Verified during implementation

The two points flagged here at design time were both resolved cleanly:

- **CSS reach:** `::part(toggle-button)` and the `[slot="children"]` selector work without any JS helper. Pure CSS in `side-nav-rail.css` is sufficient. The "no custom element" architectural decision stands.
- **Label wrap rendering:** `SideNavRailItem` wraps the label in a `<span class="label">` both in the String constructors and in `setLabel(String)`. `LabelWrapTest` asserts idempotency (exactly one span after repeated `setLabel` calls) and that slotted children (prefix icons) survive the wrap. `demo/LabelWrapSmokeView` renders a standard `SideNav` and a `SideNavRail` side-by-side for visual confirmation.

Additional implementation-time findings worth noting:

- **Spring Boot + Java-version gotcha:** Spring Boot 3.4's bundled ASM cannot parse Java 25 class files. The compile target was pinned to Java 17 accordingly (see the devcontainer Dockerfile + `<maven.compiler.release>17</maven.compiler.release>` in the addon POM).
- **Vaadin `Element.getTag()` on text nodes:** throws `UnsupportedOperationException`. The `SideNavRailItem` implementation and `LabelWrapTest` filter with `e -> !e.isTextNode()` before calling `getTag()` in stream operations.
- **Popover lifecycle:** `Popover.setTarget(...)` installs attach/detach listeners on the target that auto-add the popover to the UI and auto-remove it on detach. No manual `appendChild` is needed — the popover becomes a direct UI child, and removing the rail detaches it cleanly. `PopoverLifecycleTest` pins both invariants.
