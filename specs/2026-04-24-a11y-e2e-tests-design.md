# A11y E2E Tests — Design (§9.4)

**Status:** design, ready for implementation
**Scope:** E2E-level assertions covering the accessibility contracts introduced in [§9.2](2026-04-21-side-nav-rail-design.md#92-phase-2--accessibility) of the SideNav Rail design spec.

## 1. Goal

Lock in the observable DOM contracts from [§4.4.5](2026-04-21-side-nav-rail-design.md#445-aria-attributes) of the main spec so they cannot regress silently. The §9.2 implementation produced a specific set of attributes and focus behaviour; this work adds deterministic Playwright assertions against them.

Out of scope:

- No automated WCAG audit (e.g. `@axe-core/playwright`). Such a tool would flag issues outside the addon (Lumo defaults, page chrome) and produce noise; the addon's contract is narrower and better expressed as explicit assertions.
- No screen-reader simulation.
- No colour-contrast or visual-design assertions.
- No new addon API — tests only.

## 2. Test view

A new dedicated view in the `e2e/` module:

- **File:** `e2e/src/main/java/org/vaadin/addons/componentfactory/sidenavrail/e2e/views/AccessibilityView.java`
- **Route:** `/accessibility`
- **Structure:** a clone of `KeyboardNavigationView` — `Dashboard` (leaf, no children), `Code` (two flat children `Branches`, `Commits`), `Admin` (mixed: nested `Users` → `Active`, `Archived`, plus flat `Roles`). Plus a `Button#toggle-rail` that flips `rail.setRailMode(!rail.isRailMode())`. Rail starts in normal mode (rail off).
- **Why a dedicated view:** keyboard-navigation tests and a11y contract tests must be decoupled — tweaks to the keyboard view (e.g. adding items to exercise a new key binding) should not cascade into a11y regressions. One view per test concern.

## 3. Spec file

- **File:** `e2e/src/test/playwright/tests/accessibility.spec.ts`
- **Structure:** grouped by scenario with `test.describe`. Reuses the existing Playwright setup (`baseURL`, production build, etc.) already configured for the other specs.
- **Test independence:** each `test(...)` starts with `page.goto('/accessibility')` and re-applies whatever rail-mode state it needs — no shared state across cases. A failure in an earlier test must never mask or cascade into a later one.

## 4. Assertions

All assertions are made against the rendered DOM via Playwright locators. Selectors use `vaadin-side-nav-item[path="…"]`, matching existing specs. The popover overlay is located via `vaadin-popover-overlay`.

### 4.1 Rail mode off (normal mode) — baseline

Initial state after navigating to `/accessibility`:

- **Leaf root** (`Dashboard`): has **no** `aria-haspopup` attribute and **no** `aria-expanded` attribute.
- **Parent roots** (`Code`, `Admin`): Vaadin's stock `<vaadin-side-nav-item>` web component natively sets `aria-haspopup="true"` and `aria-expanded="false"` on any item that has children, regardless of rail mode. We cannot (and should not) fight these values in normal mode. What we assert is the *negative*: `aria-haspopup` is **not** `"menu"` and `aria-expanded` is **not** `"true"` in normal mode. Both of those values are addon-owned rail-mode states (see §4.2–§4.4), and their presence outside rail mode would be a regression.
- Nested items (`Branches`, `Commits`, `Users`, `Active`, `Archived`, `Roles`) have **no** `tabindex` attribute. (The §9.2 implementation `removeAttribute("tabindex")` on rail-off, and never sets one in normal mode.)

### 4.2 Rail mode on — popover closed

After clicking `#toggle-rail`:

- `Code` and `Admin` (root items with children) have `aria-haspopup="menu"` and `aria-expanded="false"`.
- `Dashboard` (leaf) has **neither** `aria-haspopup` nor `aria-expanded`.
- Nested items (`Branches`, `Commits`, `Users`, `Active`, `Archived`, `Roles`) have `tabindex="-1"`.

### 4.3 Rail mode on — popover opened

Opening the popover: focus the root item programmatically using the shadow-DOM-piercing helper pattern from `keyboard-navigation.spec.ts` (reach into `shadowRoot` and focus the inner `<a id="link">`). In rail mode the popover has `openOnFocus=true` (see §4.2 of the main spec), so focus alone triggers it. Before asserting, wait for one `vaadin-popover-overlay[opened]` element to be present — this is the same ready-signal `keyboard-navigation.spec.ts` uses and is more deterministic than a generic visibility check (the overlay can linger in the DOM in a hidden state between open/close cycles).

**Locator scoping — mandatory:** every popover-item assertion must use a locator scoped to `vaadin-popover-overlay[opened]`. The rail's own DOM also contains `Branches`, `Commits`, `Users`, `Roles`, `Active`, `Archived` as `vaadin-side-nav-item` elements (they carry `tabindex="-1"` in rail mode but do **not** have `role="menuitem"`). An unscoped selector like `vaadin-side-nav-item[path="code/branches"]` would hit both the rail-DOM original and the popover copy and could give a false green.

Two separate tests — one per root — so a failure on `Code` doesn't mask a failure on `Admin`:

**Test A — `Code` (two flat children):**

- `Code` has `aria-expanded="true"` (and `aria-haspopup="menu"` unchanged).
- The overlay itself has `role="menu"` — set by `Popover.setOverlayRole("menu")` on the Java side (see [§4.2](2026-04-21-side-nav-rail-design.md#42-popover-behaviour)). This is a distinct code path from `role="menuitem"` and is asserted separately.
- Every `vaadin-side-nav-item` inside `vaadin-popover-overlay[opened]` has `role="menuitem"`. Concretely: `Branches` and `Commits`. The `Code` root item itself is the popover's *target* and stays in the rail DOM — it is **not** duplicated inside the overlay.

**Test B — `Admin` (one nested subtree + one flat child):**

- `Admin` has `aria-expanded="true"`.
- Overlay has `role="menu"`.
- Scoped to `vaadin-popover-overlay[opened]`: `Users`, `Roles`, and the deeply nested `Active`, `Archived` all have `role="menuitem"`. Expanding `Users` is not required for this assertion — `role="menuitem"` is applied recursively at populate time, independent of expanded state.

**Why server-side matters for diagnosing regressions:** `role="menuitem"` is set by `SideNavRailItem.tagAsMenuItem()` during `populatePopover()` (server-side Java), **not** by the client-side keyboard adapter. A developer debugging a failure here should look in `SideNavRailItem.java`, not in `side-nav-rail-keyboard.js`.

### 4.4 Rail mode on — popover closed again

After closing the popover by pressing `Escape` (deterministic; focus-out also closes the popover but carries a 300 ms hide-delay and is not used here):

- `Code` returns to `aria-expanded="false"`.

### 4.5 Rail mode off after being on — cleanup

After clicking `#toggle-rail` a second time:

- `Code` and `Admin` no longer have `aria-haspopup="menu"` — the addon's override is removed. Vaadin may re-apply its native `"true"` value (see §4.1); what matters is the *negative*: the value is not `"menu"` after rail-off. Same for `aria-expanded`: the addon does not carry it outside rail mode.
- Nested items no longer have `tabindex="-1"`.

### 4.6 Rail mode toggled back on — re-apply

After clicking `#toggle-rail` a third time, the §4.2 assertions hold again. This guards the cleanup/re-apply symmetry.

## 5. Organization & naming

One `describe` block per section above; each block contains 1–3 `test(...)` cases. Expected total: roughly 10–12 tests (the split of §4.3 into separate `Code` / `Admin` tests and the extra `role="menu"` overlay assertion brings the count above the original 6–8 estimate, but each test stays short). Target runtime comparable to `keyboard-navigation.spec.ts` (roughly 10–15s for the whole file).

Test names read as contracts, e.g.:

- `"rail off — roots have no aria-haspopup / aria-expanded"`
- `"rail off — nested items have no tabindex"`
- `"rail on, popover closed — roots with children get aria-haspopup=menu, leaf untouched"`
- `"rail on, popover closed — nested items have tabindex=-1"`
- `"rail on, popover open (Code) — aria-expanded=true on focused root"`
- `"rail on, popover open (Code) — overlay has role=menu"`
- `"rail on, popover open (Code) — flat children have role=menuitem"`
- `"rail on, popover open (Admin) — deeply nested children have role=menuitem"`
- `"popover closed again — aria-expanded returns to false"`
- `"rail toggled off — aria-haspopup / aria-expanded cleared"`
- `"rail toggled off — tabindex cleared"`
- `"rail off → on again — contracts re-apply"`

## 6. Non-requirements

- No new addon code. If an assertion fails and reveals a missing contract, that is a bug in the addon, not in the spec — fixes go through the normal addon workflow, not this spec.
- No changes to `KeyboardNavigationView`; it keeps covering the keyboard-nav specs.
- No changes to existing spec files.
