# SideNavRail §9.2 — Accessibility Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement §9.2 of the spec — full keyboard navigation in normal AND rail mode, ARIA attributes synchronized with popover state, focus management on popover open/close.

**Architecture:** Java side owns attribute management (aria-haspopup, aria-expanded, tabindex on nested items in rail mode, setOpenOnFocus gated on rail mode). Keyboard handling lives in one client-side ES module (`side-nav-rail-keyboard.js`) that installs a single delegated document-level keydown listener per SideNavRail instance. No new Java API, no custom web component.

**Tech Stack:** Vaadin 24.10.1 Flow (Java 17), Karibu Testing v24 (unit, browser-free), Playwright TypeScript (E2E, production bundle), Spring Boot 3.5.13 (test runner only).

**Spec reference:** [`specs/2026-04-21-side-nav-rail-design.md`](../specs/2026-04-21-side-nav-rail-design.md) §4.4.

---

## File structure

**New:**
- `addon/src/main/resources/META-INF/resources/frontend/side-nav-rail-keyboard.js` — keyboard adapter (evolves across Tasks 4–9)
- `addon/src/test/java/.../unit/AriaAttributesTest.java` — Task 1 tests
- `addon/src/test/java/.../unit/FocusTriggerTest.java` — Task 2 tests
- `addon/src/test/java/.../unit/NestedTabindexTest.java` — Task 3 tests
- `e2e/src/main/java/.../e2e/views/KeyboardNavigationView.java` — Task 4 view
- `e2e/src/test/playwright/tests/keyboard-navigation.spec.ts` — Tasks 4–9 E2E

**Modified:**
- `addon/src/main/java/.../SideNavRail.java` — ARIA sync, tabindex-nested, @JsModule, init-on-attach
- `addon/src/main/java/.../SideNavRailItem.java` — aria-expanded popover-event sync, setOpenOnFocus rail-gated

**Conventions:**
- All Java files: Apache 2.0 header, `package org.vaadin.addons.componentfactory.sidenavrail(...)`, Vaadin 24 imports.
- All test classes follow the pattern of existing `RailTooltipModeTest.java` (MockVaadin.setup/tearDown, JUnit 5).
- One task = one commit unless explicitly noted. Commit message format: `feat(addon): …`, `test(addon): …`, `docs: …` (follow recent history in `git log --oneline -10`).

**Build verification:** `./mvnw clean verify` from `/workspace` must stay green after every task. This runs addon unit tests (~67 today, growing) + E2E (25 today, growing).

---

## Task 1: ARIA attributes on rail-mode root items

Add `aria-haspopup="menu"` + `aria-expanded` to root items that have children while rail mode is active. Clear both on rail-mode exit. Sync `aria-expanded` with popover `opened-changed` events.

**Files:**
- Modify: `addon/src/main/java/org/vaadin/addons/componentfactory/sidenavrail/SideNavRailItem.java`
- Modify: `addon/src/main/java/org/vaadin/addons/componentfactory/sidenavrail/SideNavRail.java`
- Create: `addon/src/test/java/org/vaadin/addons/componentfactory/sidenavrail/unit/AriaAttributesTest.java`

- [ ] **Step 1: Write the failing tests**

Create `addon/src/test/java/org/vaadin/addons/componentfactory/sidenavrail/unit/AriaAttributesTest.java`:

```java
/*
 * Copyright 2026 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */
package org.vaadin.addons.componentfactory.sidenavrail.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.github.mvysny.kaributesting.v10.MockVaadin;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.sidenav.SideNavItem;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.vaadin.addons.componentfactory.sidenavrail.SideNavRail;
import org.vaadin.addons.componentfactory.sidenavrail.SideNavRailItem;

/**
 * §9.2 / §4.4.5: aria-haspopup + aria-expanded on root items with children while
 * rail mode is active; cleared on exit.
 */
class AriaAttributesTest {

    @BeforeEach void setUp() { MockVaadin.setup(); }
    @AfterEach void tearDown() { MockVaadin.tearDown(); }

    @Test
    void noAriaAttributesInNormalMode() {
        SideNavRail nav = parentAndLeafRail();
        UI.getCurrent().add(nav);

        for (SideNavItem item : nav.getItems()) {
            assertFalse(item.getElement().hasAttribute("aria-haspopup"));
            assertFalse(item.getElement().hasAttribute("aria-expanded"));
        }
    }

    @Test
    void railModeSetsHaspopupOnParentOnly() {
        SideNavRail nav = parentAndLeafRail();
        UI.getCurrent().add(nav);

        nav.setRailMode(true);

        // Parent (has children) -> aria-haspopup="menu"
        assertEquals("menu", nav.getItems().get(0).getElement().getAttribute("aria-haspopup"));
        // Leaf (no children) -> no aria-haspopup
        assertFalse(nav.getItems().get(1).getElement().hasAttribute("aria-haspopup"));
    }

    @Test
    void railModeSetsAriaExpandedFalseInitially() {
        SideNavRail nav = parentAndLeafRail();
        UI.getCurrent().add(nav);

        nav.setRailMode(true);

        assertEquals("false", nav.getItems().get(0).getElement().getAttribute("aria-expanded"));
    }

    @Test
    void leavingRailModeClearsAriaAttributes() {
        SideNavRail nav = parentAndLeafRail();
        UI.getCurrent().add(nav);

        nav.setRailMode(true);
        nav.setRailMode(false);

        SideNavItem parent = nav.getItems().get(0);
        assertFalse(parent.getElement().hasAttribute("aria-haspopup"));
        assertFalse(parent.getElement().hasAttribute("aria-expanded"));
    }

    @Test
    void ariaExpandedTracksPopoverOpenState() {
        SideNavRail nav = parentAndLeafRail();
        UI.getCurrent().add(nav);
        nav.setRailMode(true);

        SideNavRailItem parent = (SideNavRailItem) nav.getItems().get(0);

        // Simulate popover open via the adapter method (Task 1 will expose this).
        parent.syncAriaExpanded(true);
        assertEquals("true", parent.getElement().getAttribute("aria-expanded"));

        parent.syncAriaExpanded(false);
        assertEquals("false", parent.getElement().getAttribute("aria-expanded"));
    }

    private static SideNavRail parentAndLeafRail() {
        SideNavRail nav = new SideNavRail();
        SideNavRailItem parent = new SideNavRailItem("Code", "/code");
        parent.addItem(new SideNavRailItem("Branches", "/code/branches"));
        SideNavRailItem leaf = new SideNavRailItem("Dashboard", "/");
        nav.addItem(parent, leaf);
        return nav;
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd /workspace && ./mvnw -pl addon test -Dtest=AriaAttributesTest -q`
Expected: FAIL with "cannot find symbol: method syncAriaExpanded" + missing attributes.

- [ ] **Step 3: Add `applyAriaAttributes(boolean)` + `syncAriaExpanded(boolean)` on `SideNavRailItem`**

In `SideNavRailItem.java`, add these package-private methods (location: near `applyPopoverGating` around line 417):

```java
    /**
     * Applies §4.4.5 ARIA attributes: {@code aria-haspopup="menu"} on items with children
     * while rail mode is active; cleared otherwise. {@code aria-expanded} is seeded to
     * "false" and then tracked via {@link #syncAriaExpanded(boolean)} as the popover
     * opens/closes. Package-private — called by {@link SideNavRail#setRailMode(boolean)}.
     */
    void applyAriaAttributes(boolean railMode) {
        boolean hasChildren = !getItems().isEmpty();
        if (railMode && hasChildren) {
            getElement().setAttribute("aria-haspopup", "menu");
            if (!getElement().hasAttribute("aria-expanded")) {
                getElement().setAttribute("aria-expanded", "false");
            }
        } else {
            getElement().removeAttribute("aria-haspopup");
            getElement().removeAttribute("aria-expanded");
        }
    }

    /**
     * Updates {@code aria-expanded} to reflect the given popover state. Called from
     * the popover's {@code opened-changed} listener (see {@link #ensurePopover()}).
     * Package-private so tests can drive it directly without the real DOM event.
     */
    void syncAriaExpanded(boolean open) {
        if (getElement().hasAttribute("aria-haspopup")) {
            getElement().setAttribute("aria-expanded", String.valueOf(open));
        }
    }
```

Inside `ensurePopover()` (around line 368, after `populatePopover()`), hook the open-state event:

```java
        popover.addOpenedChangeListener(e -> syncAriaExpanded(e.isOpened()));
```

- [ ] **Step 4: Call `applyAriaAttributes` from `SideNavRail`**

In `SideNavRail.java`, inside `setRailMode` (around line 92), after `updatePopoverGating()` and `applyTooltips()`, add:

```java
        applyAriaToRootItems();
```

Add the helper method (near `applyTooltips` around line 305):

```java
    private void applyAriaToRootItems() {
        for (SideNavItem child : getItems()) {
            if (child instanceof SideNavRailItem rail) {
                rail.applyAriaAttributes(railMode);
            }
        }
    }
```

Also hook the helper into `addItem` and `addItemAsFirst` (near the existing `markAsRootItem` / `applyTooltipFor` calls), so items added **after** rail mode was enabled still pick up the ARIA state:

```java
        // in addItem(SideNavItem...):
        for (SideNavItem item : items) {
            markAsRootItem(item);
            applyTooltipFor(item);
            if (item instanceof SideNavRailItem rail) {
                rail.applyAriaAttributes(railMode);
            }
        }

        // in addItemAsFirst(SideNavItem):
        markAsRootItem(item);
        applyTooltipFor(item);
        if (item instanceof SideNavRailItem rail) {
            rail.applyAriaAttributes(railMode);
        }
```

Add a regression test to `AriaAttributesTest`:

```java
    @Test
    void addingItemWhileRailModeActiveAppliesAria() {
        SideNavRail nav = new SideNavRail();
        UI.getCurrent().add(nav);
        nav.setRailMode(true);

        SideNavRailItem parent = new SideNavRailItem("Late", "/late");
        parent.addItem(new SideNavRailItem("Sub", "/late/sub"));
        nav.addItem(parent);

        assertEquals("menu", parent.getElement().getAttribute("aria-haspopup"));
    }
```

Also add `role="menuitem"` on popover content. In `SideNavRailItem.java`, update `populatePopover()` (around line 437) so every copied item and its descendants get the role:

```java
    private void populatePopover() {
        popover.removeAll();
        renderHeaderIfConfigured();

        SideNav nested = new SideNav();
        for (SideNavItem child : getItems()) {
            SideNavItem copy = copyOf(child);
            tagAsMenuItem(copy);
            nested.addItem(copy);
        }
        popover.add(nested);
    }

    private static void tagAsMenuItem(SideNavItem item) {
        item.getElement().setAttribute("role", "menuitem");
        for (SideNavItem sub : item.getItems()) {
            tagAsMenuItem(sub);
        }
    }
```

Add to `AriaAttributesTest`:

```java
    @Test
    void popoverItemsReceiveMenuitemRole() {
        SideNavRail nav = parentAndLeafRail();
        UI.getCurrent().add(nav);
        nav.setRailMode(true);

        // Force popover creation by triggering the attach path — ensurePopover runs on attach.
        SideNavRailItem parent = (SideNavRailItem) nav.getItems().get(0);
        // getPopoverForTesting exists after Task 2; for Task 1 inspect via a lightweight hook.
        // We verify by walking the server-side children: populatePopover copies them and
        // tags the copy. Walk the popover's nested SideNav contents.
        com.vaadin.flow.component.popover.Popover popover = parent.getPopoverForTesting();
        com.vaadin.flow.component.sidenav.SideNav nested =
                (com.vaadin.flow.component.sidenav.SideNav) popover.getChildren()
                        .filter(c -> c instanceof com.vaadin.flow.component.sidenav.SideNav)
                        .findFirst().orElseThrow();
        nested.getItems().forEach(i ->
                assertEquals("menuitem", i.getElement().getAttribute("role")));
    }
```

> **Ordering note:** this test references `getPopoverForTesting`, which is added in Task 2. To keep tasks strictly ordered, either add this particular test in Task 2 instead, or land `getPopoverForTesting` as a tiny pre-step at the top of Task 1 (without the gating change, which still belongs in Task 2). Whichever is simpler for the implementer — the accessor is a one-liner.

- [ ] **Step 5: Run tests to verify they pass**

Run: `cd /workspace && ./mvnw -pl addon test -Dtest=AriaAttributesTest -q`
Expected: PASS (5 tests).

Then run the full addon test suite to check for regressions:
Run: `cd /workspace && ./mvnw -pl addon test -q`
Expected: all tests PASS (~72 total).

- [ ] **Step 6: Commit**

```bash
git add addon/src/main/java/org/vaadin/addons/componentfactory/sidenavrail/SideNavRail.java \
        addon/src/main/java/org/vaadin/addons/componentfactory/sidenavrail/SideNavRailItem.java \
        addon/src/test/java/org/vaadin/addons/componentfactory/sidenavrail/unit/AriaAttributesTest.java
git commit -m "feat(addon): aria-haspopup + aria-expanded on rail-mode root items"
```

---

## Task 2: setOpenOnFocus gated on rail mode

Currently `popover.setOpenOnFocus(false)` is hardcoded in `ensurePopover()`. §4.4.4 requires auto-open on keyboard focus **only while rail mode is active**. Outside rail mode the popover remains hover-only.

**Files:**
- Modify: `addon/src/main/java/org/vaadin/addons/componentfactory/sidenavrail/SideNavRailItem.java`
- Create: `addon/src/test/java/org/vaadin/addons/componentfactory/sidenavrail/unit/FocusTriggerTest.java`

- [ ] **Step 1: Write the failing test**

Create `FocusTriggerTest.java`:

```java
/*
 * Copyright 2026 Vaadin Ltd.
 * Licensed under the Apache License, Version 2.0.
 */
package org.vaadin.addons.componentfactory.sidenavrail.unit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.mvysny.kaributesting.v10.MockVaadin;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.popover.Popover;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.vaadin.addons.componentfactory.sidenavrail.SideNavRail;
import org.vaadin.addons.componentfactory.sidenavrail.SideNavRailItem;

/**
 * §4.2 / §4.4.4: popover auto-opens on keyboard focus only while rail mode is active.
 * Outside rail mode, focus-triggered opening is disabled — the popover stays hover-only.
 */
class FocusTriggerTest {

    @BeforeEach void setUp() { MockVaadin.setup(); }
    @AfterEach void tearDown() { MockVaadin.tearDown(); }

    @Test
    void openOnFocusFalseInNormalMode() {
        SideNavRail nav = railWithParent();
        UI.getCurrent().add(nav);

        Popover popover = popoverOf((SideNavRailItem) nav.getItems().get(0));
        assertFalse(popover.isOpenOnFocus());
    }

    @Test
    void openOnFocusTrueInRailMode() {
        SideNavRail nav = railWithParent();
        UI.getCurrent().add(nav);

        nav.setRailMode(true);

        Popover popover = popoverOf((SideNavRailItem) nav.getItems().get(0));
        assertTrue(popover.isOpenOnFocus());
    }

    @Test
    void leavingRailModeRestoresOpenOnFocusFalse() {
        SideNavRail nav = railWithParent();
        UI.getCurrent().add(nav);

        nav.setRailMode(true);
        nav.setRailMode(false);

        Popover popover = popoverOf((SideNavRailItem) nav.getItems().get(0));
        assertFalse(popover.isOpenOnFocus());
    }

    private static SideNavRail railWithParent() {
        SideNavRail nav = new SideNavRail();
        SideNavRailItem parent = new SideNavRailItem("Code", "/code");
        parent.addItem(new SideNavRailItem("Branches", "/code/branches"));
        nav.addItem(parent);
        return nav;
    }

    private static Popover popoverOf(SideNavRailItem item) {
        return item.getPopoverForTesting();  // added in Task 2
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd /workspace && ./mvnw -pl addon test -Dtest=FocusTriggerTest -q`
Expected: FAIL — `getPopoverForTesting` does not exist.

- [ ] **Step 3: Expose popover for testing + update setOpenOnFocus gate**

In `SideNavRailItem.java`, add a package-private accessor for tests (near the `popover` field):

```java
    /** Test-only accessor: do not call from production code. */
    Popover getPopoverForTesting() {
        return popover;
    }
```

In `ensurePopover()` (around line 357) change the hardcoded line:

```java
        popover.setOpenOnClick(false);
        popover.setOpenOnFocus(false);  // REMOVE this literal false
```

to:

```java
        popover.setOpenOnClick(false);
        SideNavRail owner = findOwnerRail();
        popover.setOpenOnFocus(owner != null && owner.isRailMode());
```

Note: `owner` is already computed a few lines below for the timing seed; pull that assignment above the `setOpenOnFocus` call and remove the duplicate.

Add a package-private refresh method (near `applyPopoverGating`):

```java
    /**
     * Updates the popover's focus-trigger according to rail state. Called by
     * {@link SideNavRail#setRailMode(boolean)} so the flag tracks live mode changes.
     */
    void applyFocusTrigger(boolean railMode) {
        if (popover != null) {
            popover.setOpenOnFocus(railMode);
        }
    }
```

In `SideNavRail.setRailMode` (after `applyAriaToRootItems()` added in Task 1) add:

```java
        applyFocusTriggerToRootItems();
```

And the helper:

```java
    private void applyFocusTriggerToRootItems() {
        for (SideNavItem child : getItems()) {
            if (child instanceof SideNavRailItem rail) {
                rail.applyFocusTrigger(railMode);
            }
        }
    }
```

- [ ] **Step 4: Run tests**

Run: `cd /workspace && ./mvnw -pl addon test -Dtest=FocusTriggerTest -q`
Expected: PASS (3 tests).

Run: `cd /workspace && ./mvnw -pl addon test -q`
Expected: full addon suite green.

- [ ] **Step 5: Commit**

```bash
git add addon/src/main/java/org/vaadin/addons/componentfactory/sidenavrail/SideNavRail.java \
        addon/src/main/java/org/vaadin/addons/componentfactory/sidenavrail/SideNavRailItem.java \
        addon/src/test/java/org/vaadin/addons/componentfactory/sidenavrail/unit/FocusTriggerTest.java
git commit -m "feat(addon): popover setOpenOnFocus tracks rail mode"
```

---

## Task 3: tabindex="-1" on nested items in rail mode

In rail mode, nested children are visually hidden (labels at `max-width: 0`) but still in DOM. Without intervention they remain in the browser's Tab order, so keyboard users would land on invisible items. Fix: set `tabindex="-1"` on every descendant of root items while rail mode is active.

**Files:**
- Modify: `addon/src/main/java/org/vaadin/addons/componentfactory/sidenavrail/SideNavRail.java`
- Create: `addon/src/test/java/org/vaadin/addons/componentfactory/sidenavrail/unit/NestedTabindexTest.java`

- [ ] **Step 1: Write the failing test**

```java
/*
 * Copyright 2026 Vaadin Ltd.
 * Licensed under the Apache License, Version 2.0.
 */
package org.vaadin.addons.componentfactory.sidenavrail.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.github.mvysny.kaributesting.v10.MockVaadin;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.sidenav.SideNavItem;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.vaadin.addons.componentfactory.sidenavrail.SideNavRail;
import org.vaadin.addons.componentfactory.sidenavrail.SideNavRailItem;

/**
 * §9.2: nested items must not be keyboard-focusable while rail mode is active, since
 * they are visually hidden. Root items keep their normal tab-order participation.
 */
class NestedTabindexTest {

    @BeforeEach void setUp() { MockVaadin.setup(); }
    @AfterEach void tearDown() { MockVaadin.tearDown(); }

    @Test
    void nestedItemsHaveNoTabindexInNormalMode() {
        SideNavRail nav = twoLevelRail();
        UI.getCurrent().add(nav);

        SideNavItem child = nav.getItems().get(0).getItems().get(0);
        assertFalse(child.getElement().hasAttribute("tabindex"));
    }

    @Test
    void railModeSetsTabindexMinusOneOnNestedItems() {
        SideNavRail nav = twoLevelRail();
        UI.getCurrent().add(nav);

        nav.setRailMode(true);

        SideNavItem child = nav.getItems().get(0).getItems().get(0);
        assertEquals("-1", child.getElement().getAttribute("tabindex"));
    }

    @Test
    void rootItemsKeepTheirNaturalTabindex() {
        SideNavRail nav = twoLevelRail();
        UI.getCurrent().add(nav);

        nav.setRailMode(true);

        // Root items must remain in tab order — we do NOT set tabindex on them.
        SideNavItem root = nav.getItems().get(0);
        assertFalse(root.getElement().hasAttribute("tabindex"));
    }

    @Test
    void leavingRailModeRestoresNestedFocusability() {
        SideNavRail nav = twoLevelRail();
        UI.getCurrent().add(nav);

        nav.setRailMode(true);
        nav.setRailMode(false);

        SideNavItem child = nav.getItems().get(0).getItems().get(0);
        assertFalse(child.getElement().hasAttribute("tabindex"));
    }

    private static SideNavRail twoLevelRail() {
        SideNavRail nav = new SideNavRail();
        SideNavRailItem parent = new SideNavRailItem("Code", "/code");
        parent.addItem(new SideNavRailItem("Branches", "/code/branches"));
        parent.addItem(new SideNavRailItem("Commits", "/code/commits"));
        nav.addItem(parent);
        return nav;
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd /workspace && ./mvnw -pl addon test -Dtest=NestedTabindexTest -q`
Expected: FAIL — tabindex not set.

- [ ] **Step 3: Implement tabindex toggling**

In `SideNavRail.java` add this helper (near `applyAriaToRootItems`):

```java
    private void applyNestedTabindex() {
        for (SideNavItem root : getItems()) {
            for (SideNavItem nested : root.getItems()) {
                applyNestedTabindexRecursive(nested);
            }
        }
    }

    private void applyNestedTabindexRecursive(SideNavItem item) {
        if (railMode) {
            item.getElement().setAttribute("tabindex", "-1");
        } else {
            item.getElement().removeAttribute("tabindex");
        }
        for (SideNavItem child : item.getItems()) {
            applyNestedTabindexRecursive(child);
        }
    }
```

Call it from `setRailMode` after the Task 2 additions:

```java
        applyFocusTriggerToRootItems();
        applyNestedTabindex();
```

- [ ] **Step 4: Run tests**

Run: `cd /workspace && ./mvnw -pl addon test -Dtest=NestedTabindexTest -q`
Expected: PASS (4 tests).

Run: `cd /workspace && ./mvnw -pl addon test -q`
Expected: full addon suite green.

- [ ] **Step 5: Commit**

```bash
git add addon/src/main/java/org/vaadin/addons/componentfactory/sidenavrail/SideNavRail.java \
        addon/src/test/java/org/vaadin/addons/componentfactory/sidenavrail/unit/NestedTabindexTest.java
git commit -m "feat(addon): tabindex=-1 on nested items while rail mode is active"
```

---

## Task 4: JS adapter skeleton + @JsModule wiring + E2E smoke test

Ship the empty-ish adapter file, wire it up via `@JsModule`, call `initKeyboardNavigation(rootElement)` on attach. Verify via an E2E smoke test that the adapter installed itself (adapter sets a `data-keyboard-ready="1"` attribute on the rail it's attached to).

**Files:**
- Create: `addon/src/main/resources/META-INF/resources/frontend/side-nav-rail-keyboard.js`
- Modify: `addon/src/main/java/org/vaadin/addons/componentfactory/sidenavrail/SideNavRail.java`
- Create: `e2e/src/main/java/org/vaadin/addons/componentfactory/sidenavrail/e2e/views/KeyboardNavigationView.java`
- Create: `e2e/src/test/playwright/tests/keyboard-navigation.spec.ts`

- [ ] **Step 1: Create the adapter skeleton**

Create `addon/src/main/resources/META-INF/resources/frontend/side-nav-rail-keyboard.js`:

```javascript
/*
 * Copyright 2026 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */

/**
 * Keyboard-navigation adapter for <vaadin-side-nav> inside a SideNavRail.
 * Installs one delegated keydown listener at the document level so events
 * originating in popover overlays (which live outside the rail's DOM subtree)
 * are still handled. Spec: §4.4 of side-nav-rail-design.md.
 */

const ATTACHED = new WeakSet();

/**
 * Initializes keyboard handling for a given <vaadin-side-nav> element owned
 * by a SideNavRail. Safe to call multiple times — a WeakSet guard dedupes.
 *
 * @param {HTMLElement} rail — the <vaadin-side-nav> root element
 */
export function initKeyboardNavigation(rail) {
    if (!rail || ATTACHED.has(rail)) {
        return;
    }
    ATTACHED.add(rail);
    document.addEventListener('keydown', (e) => handleKeydown(e, rail), true);
    rail.setAttribute('data-keyboard-ready', '1');
}

function handleKeydown(event, rail) {
    // Tasks 5–9 fill in per-key handlers. For Task 4 this is a pure no-op —
    // the smoke test only verifies the attribute is set after init runs.
}
```

- [ ] **Step 2: Add @JsModule and init call in SideNavRail**

In `SideNavRail.java`:

Add the import near the existing `@CssImport` import:

```java
import com.vaadin.flow.component.dependency.JsModule;
```

Annotate the class (next to `@CssImport`):

```java
@CssImport("./side-nav-rail.css")
@JsModule("./side-nav-rail-keyboard.js")
public class SideNavRail extends SideNav {
```

Override `onAttach` to invoke the init function (add near the constructors):

```java
    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        attachEvent.getUI().getPage().executeJs(
                "import('./side-nav-rail-keyboard.js').then(m => m.initKeyboardNavigation($0));",
                getElement());
    }
```

Add the imports if not already present:

```java
import com.vaadin.flow.component.AttachEvent;
```

- [ ] **Step 3: Create the E2E view**

Create `e2e/src/main/java/org/vaadin/addons/componentfactory/sidenavrail/e2e/views/KeyboardNavigationView.java`:

```java
/*
 * Copyright 2026 Vaadin Ltd.
 * Licensed under the Apache License, Version 2.0.
 */
package org.vaadin.addons.componentfactory.sidenavrail.e2e.views;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import org.vaadin.addons.componentfactory.sidenavrail.SideNavRail;
import org.vaadin.addons.componentfactory.sidenavrail.SideNavRailItem;

/**
 * Covers §9.2 keyboard navigation. Three root items: "Dashboard" (leaf),
 * "Code" (has two flat children), "Admin" (has one parent-child + a leaf).
 * The Admin subtree lets us exercise tree-like expand/collapse via arrow keys.
 */
@Route("keyboard-navigation")
public class KeyboardNavigationView extends VerticalLayout {

    public KeyboardNavigationView() {
        SideNavRail rail = new SideNavRail();
        rail.setId("rail");

        SideNavRailItem dashboard = new SideNavRailItem("Dashboard", "/", VaadinIcon.DASHBOARD.create());
        SideNavRailItem code = new SideNavRailItem("Code", "/code", VaadinIcon.CODE.create());
        code.addItem(new SideNavRailItem("Branches", "/code/branches"));
        code.addItem(new SideNavRailItem("Commits", "/code/commits"));

        SideNavRailItem admin = new SideNavRailItem("Admin", "/admin", VaadinIcon.COG.create());
        SideNavRailItem users = new SideNavRailItem("Users", "/admin/users");
        users.addItem(new SideNavRailItem("Active", "/admin/users/active"));
        users.addItem(new SideNavRailItem("Archived", "/admin/users/archived"));
        admin.addItem(users);
        admin.addItem(new SideNavRailItem("Roles", "/admin/roles"));

        rail.addItem(dashboard, code, admin);

        Button toggle = new Button("Toggle rail",
                e -> rail.setRailMode(!rail.isRailMode()));
        toggle.setId("toggle-rail");

        add(new HorizontalLayout(rail, toggle));
    }
}
```

- [ ] **Step 4: Create the E2E smoke test**

Create `e2e/src/test/playwright/tests/keyboard-navigation.spec.ts`:

```typescript
import { test, expect } from '@playwright/test';

test.describe('keyboard navigation adapter', () => {
    test('adapter marks the rail as keyboard-ready on attach', async ({ page }) => {
        await page.goto('/keyboard-navigation');

        await expect(page.locator('#rail')).toHaveAttribute(
            'data-keyboard-ready', '1', { timeout: 5_000 });
    });
});
```

- [ ] **Step 5: Run the addon build + E2E**

Run: `cd /workspace && ./mvnw -pl addon install -DskipTests -q`
(Required so the e2e module picks up the new frontend resource.)

Run: `cd /workspace && ./mvnw -pl e2e verify -q`
Expected: E2E green, new smoke test passes.

If the prod bundle cache interferes (previous phases hit this — see the note in `plans/2026-04-21-side-nav-rail-plan.md`):
```bash
rm -rf /workspace/e2e/src/main/bundles/prod.bundle /workspace/e2e/target
```
and rerun.

- [ ] **Step 6: Commit**

```bash
git add addon/src/main/java/org/vaadin/addons/componentfactory/sidenavrail/SideNavRail.java \
        addon/src/main/resources/META-INF/resources/frontend/side-nav-rail-keyboard.js \
        e2e/src/main/java/org/vaadin/addons/componentfactory/sidenavrail/e2e/views/KeyboardNavigationView.java \
        e2e/src/test/playwright/tests/keyboard-navigation.spec.ts
git commit -m "feat(addon): scaffold keyboard adapter JS module + E2E view"
```

---

## Task 5: Normal-mode Arrow-Up/Down (walk visible items, stop at boundaries)

Implement per §4.4.1: Arrow-Down moves focus to the next **visible** item (skipping descendants of any collapsed ancestor); Arrow-Up the reverse. Stop at boundaries.

**Files:**
- Modify: `addon/src/main/resources/META-INF/resources/frontend/side-nav-rail-keyboard.js`
- Modify: `e2e/src/test/playwright/tests/keyboard-navigation.spec.ts`

- [ ] **Step 1: Add failing E2E tests**

Append to `keyboard-navigation.spec.ts`:

```typescript
test.describe('normal mode — Arrow-Up/Down', () => {
    test('Arrow-Down walks root items in order and stops at end', async ({ page }) => {
        await page.goto('/keyboard-navigation');

        await page.locator('vaadin-side-nav-item[path="/"]').focus();
        await expect(page.locator('vaadin-side-nav-item[path="/"]')).toBeFocused();

        await page.keyboard.press('ArrowDown');
        await expect(page.locator('vaadin-side-nav-item[path="code"]')).toBeFocused();

        await page.keyboard.press('ArrowDown');
        await expect(page.locator('vaadin-side-nav-item[path="admin"]')).toBeFocused();

        // At the last item — stop, don't wrap.
        await page.keyboard.press('ArrowDown');
        await expect(page.locator('vaadin-side-nav-item[path="admin"]')).toBeFocused();
    });

    test('Arrow-Up walks backward and stops at first', async ({ page }) => {
        await page.goto('/keyboard-navigation');

        await page.locator('vaadin-side-nav-item[path="admin"]').focus();
        await page.keyboard.press('ArrowUp');
        await expect(page.locator('vaadin-side-nav-item[path="code"]')).toBeFocused();

        await page.keyboard.press('ArrowUp');
        await expect(page.locator('vaadin-side-nav-item[path="/"]')).toBeFocused();

        await page.keyboard.press('ArrowUp');
        await expect(page.locator('vaadin-side-nav-item[path="/"]')).toBeFocused();
    });

    test('Arrow-Down walks into expanded children (visible subtree)', async ({ page }) => {
        await page.goto('/keyboard-navigation');

        // Expand Code by clicking its toggle (or by using the built-in expanded attribute).
        await page.locator('vaadin-side-nav-item[path="code"]').evaluate(
            (el: any) => { el.expanded = true; });

        await page.locator('vaadin-side-nav-item[path="/"]').focus();
        await page.keyboard.press('ArrowDown');
        await expect(page.locator('vaadin-side-nav-item[path="code"]')).toBeFocused();

        await page.keyboard.press('ArrowDown');
        await expect(page.locator('vaadin-side-nav-item[path="code/branches"]')).toBeFocused();

        await page.keyboard.press('ArrowDown');
        await expect(page.locator('vaadin-side-nav-item[path="code/commits"]')).toBeFocused();

        await page.keyboard.press('ArrowDown');
        await expect(page.locator('vaadin-side-nav-item[path="admin"]')).toBeFocused();
    });

    test('Arrow-Down skips collapsed subtrees', async ({ page }) => {
        await page.goto('/keyboard-navigation');

        // Code collapsed by default — children must be skipped.
        await page.locator('vaadin-side-nav-item[path="/"]').focus();
        await page.keyboard.press('ArrowDown');
        await expect(page.locator('vaadin-side-nav-item[path="code"]')).toBeFocused();

        await page.keyboard.press('ArrowDown');
        // Should NOT land on code/branches — jumps directly to Admin.
        await expect(page.locator('vaadin-side-nav-item[path="admin"]')).toBeFocused();
    });
});
```

Run: `cd /workspace && ./mvnw -pl e2e verify -q`
Expected: FAIL — focus doesn't move on Arrow keys yet.

- [ ] **Step 2: Implement Up/Down in the adapter**

Replace the `handleKeydown` function in `side-nav-rail-keyboard.js` with:

```javascript
function handleKeydown(event, rail) {
    const target = document.activeElement;
    if (!isItemInScope(target, rail)) {
        return;
    }

    switch (event.key) {
        case 'ArrowDown':
            event.preventDefault();
            moveFocusSibling(target, rail, +1);
            break;
        case 'ArrowUp':
            event.preventDefault();
            moveFocusSibling(target, rail, -1);
            break;
    }
}

function isItemInScope(el, rail) {
    if (!el || el.localName !== 'vaadin-side-nav-item') {
        return false;
    }
    // For Task 5: only items directly inside the rail tree. Popover scoping
    // is added in Task 8.
    return rail.contains(el);
}

/**
 * Returns all visible items in document order: items whose ancestors are all
 * expanded. Root items of the rail are always visible.
 */
function visibleItems(rail) {
    const all = [...rail.querySelectorAll('vaadin-side-nav-item')];
    return all.filter(item => {
        let parent = item.parentElement;
        while (parent && parent !== rail) {
            if (parent.localName === 'vaadin-side-nav-item' && !parent.expanded) {
                return false;
            }
            parent = parent.parentElement;
        }
        return true;
    });
}

function moveFocusSibling(current, rail, direction) {
    const items = visibleItems(rail);
    const idx = items.indexOf(current);
    if (idx < 0) return;
    const next = items[idx + direction];
    if (next) {
        next.focus();
    }
    // else: stop at boundary (no-op)
}
```

- [ ] **Step 3: Run tests**

Run: `cd /workspace && ./mvnw -pl addon install -DskipTests -q && ./mvnw -pl e2e verify -q`
Expected: new Up/Down tests PASS.

- [ ] **Step 4: Commit**

```bash
git add addon/src/main/resources/META-INF/resources/frontend/side-nav-rail-keyboard.js \
        e2e/src/test/playwright/tests/keyboard-navigation.spec.ts
git commit -m "feat(addon): normal-mode Arrow-Up/Down keyboard navigation"
```

---

## Task 6: Normal-mode Arrow-Right/Left (expand/collapse + focus-to-child/parent)

Per §4.4.1 table:
- Arrow-Right on collapsed item with children: expand. On expanded item with children: move focus to first child. On leaf: no-op.
- Arrow-Left on expanded item: collapse. On collapsed/leaf item: move focus to parent. Top-level no-op.

**Files:**
- Modify: `addon/src/main/resources/META-INF/resources/frontend/side-nav-rail-keyboard.js`
- Modify: `e2e/src/test/playwright/tests/keyboard-navigation.spec.ts`

- [ ] **Step 1: Add failing E2E tests**

Append to `keyboard-navigation.spec.ts`:

```typescript
test.describe('normal mode — Arrow-Right/Left', () => {
    test('Arrow-Right on collapsed parent expands it', async ({ page }) => {
        await page.goto('/keyboard-navigation');

        await page.locator('vaadin-side-nav-item[path="code"]').focus();
        await page.keyboard.press('ArrowRight');

        await expect(page.locator('vaadin-side-nav-item[path="code"]'))
            .toHaveJSProperty('expanded', true);
        // Focus must remain on parent, not jump into children on first right-press.
        await expect(page.locator('vaadin-side-nav-item[path="code"]')).toBeFocused();
    });

    test('Arrow-Right on expanded parent moves focus to first child', async ({ page }) => {
        await page.goto('/keyboard-navigation');

        await page.locator('vaadin-side-nav-item[path="code"]').evaluate(
            (el: any) => { el.expanded = true; });
        await page.locator('vaadin-side-nav-item[path="code"]').focus();

        await page.keyboard.press('ArrowRight');
        await expect(page.locator('vaadin-side-nav-item[path="code/branches"]')).toBeFocused();
    });

    test('Arrow-Right on leaf is a no-op', async ({ page }) => {
        await page.goto('/keyboard-navigation');

        await page.locator('vaadin-side-nav-item[path="/"]').focus();
        await page.keyboard.press('ArrowRight');
        await expect(page.locator('vaadin-side-nav-item[path="/"]')).toBeFocused();
    });

    test('Arrow-Left on expanded parent collapses it', async ({ page }) => {
        await page.goto('/keyboard-navigation');

        await page.locator('vaadin-side-nav-item[path="code"]').evaluate(
            (el: any) => { el.expanded = true; });
        await page.locator('vaadin-side-nav-item[path="code"]').focus();

        await page.keyboard.press('ArrowLeft');
        await expect(page.locator('vaadin-side-nav-item[path="code"]'))
            .toHaveJSProperty('expanded', false);
    });

    test('Arrow-Left on child moves focus to parent', async ({ page }) => {
        await page.goto('/keyboard-navigation');

        await page.locator('vaadin-side-nav-item[path="code"]').evaluate(
            (el: any) => { el.expanded = true; });
        await page.locator('vaadin-side-nav-item[path="code/branches"]').focus();

        await page.keyboard.press('ArrowLeft');
        await expect(page.locator('vaadin-side-nav-item[path="code"]')).toBeFocused();
    });

    test('Arrow-Left on top-level leaf is a no-op', async ({ page }) => {
        await page.goto('/keyboard-navigation');

        await page.locator('vaadin-side-nav-item[path="/"]').focus();
        await page.keyboard.press('ArrowLeft');
        await expect(page.locator('vaadin-side-nav-item[path="/"]')).toBeFocused();
    });
});
```

Run: `cd /workspace && ./mvnw -pl e2e verify -q`
Expected: FAIL — Right/Left do nothing yet.

- [ ] **Step 2: Extend the adapter**

Add two cases to the `switch` in `handleKeydown`:

```javascript
        case 'ArrowRight':
            event.preventDefault();
            moveFocusRight(target);
            break;
        case 'ArrowLeft':
            event.preventDefault();
            moveFocusLeft(target, rail);
            break;
```

Add the helpers below `moveFocusSibling`:

```javascript
function hasChildren(item) {
    return item.querySelector(':scope > vaadin-side-nav-item') !== null;
}

function firstChild(item) {
    return item.querySelector(':scope > vaadin-side-nav-item');
}

function parentItem(item, rail) {
    let p = item.parentElement;
    while (p && p !== rail) {
        if (p.localName === 'vaadin-side-nav-item') {
            return p;
        }
        p = p.parentElement;
    }
    return null;
}

function moveFocusRight(item) {
    if (!hasChildren(item)) {
        return;
    }
    if (!item.expanded) {
        item.expanded = true;
    } else {
        const child = firstChild(item);
        if (child) child.focus();
    }
}

function moveFocusLeft(item, rail) {
    if (item.expanded && hasChildren(item)) {
        item.expanded = false;
        return;
    }
    const parent = parentItem(item, rail);
    if (parent) {
        parent.focus();
    }
}
```

- [ ] **Step 3: Run tests**

Run: `cd /workspace && ./mvnw -pl addon install -DskipTests -q && ./mvnw -pl e2e verify -q`
Expected: new Right/Left tests PASS.

- [ ] **Step 4: Commit**

```bash
git add addon/src/main/resources/META-INF/resources/frontend/side-nav-rail-keyboard.js \
        e2e/src/test/playwright/tests/keyboard-navigation.spec.ts
git commit -m "feat(addon): normal-mode Arrow-Right/Left expand/collapse + focus"
```

---

## Task 7: Rail-mode Arrow-Up/Down between root items + Esc closes popover

Per §4.4.2: in rail mode with focus on a root item, Arrow-Up/Down walks root items only (nested subtree — even if still DOM-visible — is not traversed). Esc closes the popover but keeps focus on the root.

**Files:**
- Modify: `addon/src/main/resources/META-INF/resources/frontend/side-nav-rail-keyboard.js`
- Modify: `e2e/src/test/playwright/tests/keyboard-navigation.spec.ts`

- [ ] **Step 1: Add failing E2E tests**

Append to `keyboard-navigation.spec.ts`:

```typescript
test.describe('rail mode — root navigation + Esc', () => {
    test('Arrow-Down walks root items only, skipping hidden children', async ({ page }) => {
        await page.goto('/keyboard-navigation');
        await page.locator('#toggle-rail').click();

        await page.locator('vaadin-side-nav-item[path="/"]').focus();
        await page.keyboard.press('ArrowDown');
        await expect(page.locator('vaadin-side-nav-item[path="code"]')).toBeFocused();

        // Would be Branches in normal mode if Code were expanded — rail mode skips.
        await page.keyboard.press('ArrowDown');
        await expect(page.locator('vaadin-side-nav-item[path="admin"]')).toBeFocused();
    });

    test('Esc closes the auto-opened popover but keeps focus on root', async ({ page }) => {
        await page.goto('/keyboard-navigation');
        await page.locator('#toggle-rail').click();

        await page.locator('vaadin-side-nav-item[path="code"]').focus();
        // Popover auto-opens on focus (Task 2 wired this up).
        await expect(page.locator('vaadin-popover-overlay[opened]')).toBeVisible();

        await page.keyboard.press('Escape');
        await expect(page.locator('vaadin-popover-overlay[opened]')).toHaveCount(0);
        await expect(page.locator('vaadin-side-nav-item[path="code"]')).toBeFocused();
    });
});
```

Run: expected FAIL — rail-mode Up/Down still goes through nested items, Esc doesn't close adapter-side.

- [ ] **Step 2: Scope item-walk to root-only in rail mode**

Modify `visibleItems` in `side-nav-rail-keyboard.js` to filter by rail mode:

```javascript
function visibleItems(rail, target) {
    const railMode = rail.hasAttribute('theme') &&
        rail.getAttribute('theme').split(/\s+/).includes('rail');

    if (railMode && target && target.hasAttribute('root-item')) {
        // Rail mode + focus on root → walk root items only.
        return [...rail.querySelectorAll(':scope > vaadin-side-nav-item[root-item]')];
    }

    // Normal mode (or any non-root focus): full visible walk.
    const all = [...rail.querySelectorAll('vaadin-side-nav-item')];
    return all.filter(item => {
        let parent = item.parentElement;
        while (parent && parent !== rail) {
            if (parent.localName === 'vaadin-side-nav-item' && !parent.expanded) {
                return false;
            }
            parent = parent.parentElement;
        }
        return true;
    });
}
```

Update `moveFocusSibling` to pass `current`:

```javascript
function moveFocusSibling(current, rail, direction) {
    const items = visibleItems(rail, current);
    const idx = items.indexOf(current);
    if (idx < 0) return;
    const next = items[idx + direction];
    if (next) next.focus();
}
```

Add an `Escape` case to `handleKeydown`:

```javascript
        case 'Escape':
            if (handleEscape(target, rail)) {
                event.preventDefault();
            }
            break;
```

Add the helper — covers both "focus on root item" and "focus inside popover":

```javascript
function handleEscape(target, rail) {
    if (!target) return false;

    // Case A: focus is inside a popover — close it and return focus to the owning root.
    const overlay = target.closest('vaadin-popover-overlay');
    if (overlay) {
        const owner = overlay.positionTarget;
        overlay.opened = false;
        if (owner) owner.focus();
        return true;
    }

    // Case B: focus is on a rail root — close its popover if open; focus stays on root.
    if (target.hasAttribute('root-item')) {
        const popoverOverlay = findOpenPopoverForTarget(target);
        if (popoverOverlay) {
            popoverOverlay.opened = false;
            target.focus();
            return true;
        }
    }
    return false;
}

function findOpenPopoverForTarget(rootItem) {
    return [...document.querySelectorAll('vaadin-popover-overlay[opened]')]
        .find(o => o.positionTarget === rootItem) || null;
}
```

Add an E2E test for the popover-interior Esc case (append to the "rail mode — root navigation + Esc" describe block):

```typescript
    test('Esc from inside popover closes it and returns focus to root', async ({ page }) => {
        await page.goto('/keyboard-navigation');
        await page.locator('#toggle-rail').click();

        await page.locator('vaadin-side-nav-item[path="code"]').focus();
        await page.keyboard.press('ArrowRight');  // focus moves into popover
        await expect(page.locator(
            'vaadin-popover-overlay[opened] vaadin-side-nav-item[path="code/branches"]'))
            .toBeFocused();

        await page.keyboard.press('Escape');
        await expect(page.locator('vaadin-popover-overlay[opened]')).toHaveCount(0);
        await expect(page.locator('vaadin-side-nav-item[path="code"]')).toBeFocused();
    });
```

- [ ] **Step 3: Run tests**

Run: `cd /workspace && ./mvnw -pl addon install -DskipTests -q && ./mvnw -pl e2e verify -q`
Expected: new Task 7 tests PASS. All prior tests stay green.

- [ ] **Step 4: Commit**

```bash
git add addon/src/main/resources/META-INF/resources/frontend/side-nav-rail-keyboard.js \
        e2e/src/test/playwright/tests/keyboard-navigation.spec.ts
git commit -m "feat(addon): rail-mode Arrow-Up/Down across root items + Esc closes popover"
```

---

## Task 8: Rail-mode Arrow-Right enters popover; Arrow-Up/Down within popover

Per §4.4.2 (Arrow-Right) and §4.4.3 (Up/Down inside popover): Arrow-Right on the root item opens the popover if closed and moves focus to the first popover menu item; once in the popover, Up/Down navigate between menu items at the current level.

**Files:**
- Modify: `addon/src/main/resources/META-INF/resources/frontend/side-nav-rail-keyboard.js`
- Modify: `e2e/src/test/playwright/tests/keyboard-navigation.spec.ts`

- [ ] **Step 1: Add failing E2E tests**

Append to `keyboard-navigation.spec.ts`:

```typescript
test.describe('rail mode — Arrow-Right into popover + in-popover navigation', () => {
    test('Arrow-Right on root moves focus to first popover item', async ({ page }) => {
        await page.goto('/keyboard-navigation');
        await page.locator('#toggle-rail').click();

        await page.locator('vaadin-side-nav-item[path="code"]').focus();
        await expect(page.locator('vaadin-popover-overlay[opened]')).toBeVisible();

        await page.keyboard.press('ArrowRight');

        await expect(page.locator(
            'vaadin-popover-overlay[opened] vaadin-side-nav-item[path="code/branches"]'))
            .toBeFocused();
    });

    test('Arrow-Right reopens popover after Esc', async ({ page }) => {
        await page.goto('/keyboard-navigation');
        await page.locator('#toggle-rail').click();

        await page.locator('vaadin-side-nav-item[path="code"]').focus();
        await page.keyboard.press('Escape');
        await expect(page.locator('vaadin-popover-overlay[opened]')).toHaveCount(0);

        await page.keyboard.press('ArrowRight');
        await expect(page.locator('vaadin-popover-overlay[opened]')).toBeVisible();
        await expect(page.locator(
            'vaadin-popover-overlay[opened] vaadin-side-nav-item[path="code/branches"]'))
            .toBeFocused();
    });

    test('Arrow-Down inside popover walks menu items', async ({ page }) => {
        await page.goto('/keyboard-navigation');
        await page.locator('#toggle-rail').click();

        await page.locator('vaadin-side-nav-item[path="code"]').focus();
        await page.keyboard.press('ArrowRight');
        // Now on first popover item (Branches)

        await page.keyboard.press('ArrowDown');
        await expect(page.locator(
            'vaadin-popover-overlay[opened] vaadin-side-nav-item[path="code/commits"]'))
            .toBeFocused();

        // Stop at last
        await page.keyboard.press('ArrowDown');
        await expect(page.locator(
            'vaadin-popover-overlay[opened] vaadin-side-nav-item[path="code/commits"]'))
            .toBeFocused();
    });

    test('Arrow-Up inside popover walks back', async ({ page }) => {
        await page.goto('/keyboard-navigation');
        await page.locator('#toggle-rail').click();

        await page.locator('vaadin-side-nav-item[path="code"]').focus();
        await page.keyboard.press('ArrowRight');
        await page.keyboard.press('ArrowDown');  // on Commits

        await page.keyboard.press('ArrowUp');
        await expect(page.locator(
            'vaadin-popover-overlay[opened] vaadin-side-nav-item[path="code/branches"]'))
            .toBeFocused();

        // Stop at first
        await page.keyboard.press('ArrowUp');
        await expect(page.locator(
            'vaadin-popover-overlay[opened] vaadin-side-nav-item[path="code/branches"]'))
            .toBeFocused();
    });
});
```

Run: expected FAIL — Arrow-Right doesn't enter popover yet; Up/Down don't work inside popover.

- [ ] **Step 2: Extend the adapter**

In `isItemInScope`, extend to recognize popover membership owned by the rail:

```javascript
function isItemInScope(el, rail) {
    if (!el || el.localName !== 'vaadin-side-nav-item') {
        return false;
    }
    if (rail.contains(el)) {
        return true;
    }
    const owningRoot = popoverOwnerForItem(el);
    return owningRoot && rail.contains(owningRoot);
}

function popoverOwnerForItem(item) {
    const overlay = item.closest('vaadin-popover-overlay');
    return overlay ? overlay.positionTarget : null;
}
```

Extend Arrow-Right in `moveFocusRight` to handle the rail-root case:

```javascript
function moveFocusRight(item) {
    if (item.hasAttribute('root-item') && isInRailMode(item)) {
        return moveFocusRightOnRailRoot(item);
    }
    // Existing normal-mode / popover-nested behaviour:
    if (!hasChildren(item)) {
        return;
    }
    if (!item.expanded) {
        item.expanded = true;
    } else {
        const child = firstChild(item);
        if (child) child.focus();
    }
}

function isInRailMode(item) {
    const rail = item.closest('vaadin-side-nav');
    return rail && rail.hasAttribute('theme') &&
        rail.getAttribute('theme').split(/\s+/).includes('rail');
}

function moveFocusRightOnRailRoot(item) {
    if (!hasChildren(item)) {
        return;
    }
    let overlay = findOpenPopoverForTarget(item);
    if (!overlay) {
        // Trigger open via the existing popover (setOpenOnFocus relationship).
        // Simpler: find the popover element associated with this item in the DOM
        // and open it explicitly.
        const popover = findPopoverForTarget(item);
        if (!popover) return;
        popover.opened = true;
        overlay = findOpenPopoverForTarget(item);
    }
    if (!overlay) return;
    // Wait for the overlay to render its first item, then move focus.
    requestAnimationFrame(() => {
        const first = overlay.querySelector('vaadin-side-nav-item');
        if (first) first.focus();
    });
}

function findPopoverForTarget(item) {
    // Vaadin popovers are attached as siblings to the app root; they expose
    // `target` on the Popover element itself (not on the overlay).
    return [...document.querySelectorAll('vaadin-popover')]
        .find(p => p.target === item) || null;
}
```

Extend `visibleItems` to handle the popover-item case: when the focused item lives inside a popover overlay, the "walk set" is the sibling items within the same popover level.

```javascript
function visibleItems(rail, target) {
    const railMode = rail.hasAttribute('theme') &&
        rail.getAttribute('theme').split(/\s+/).includes('rail');

    // Focus inside a popover: walk siblings at the same level within the overlay.
    const overlay = target && target.closest('vaadin-popover-overlay');
    if (overlay) {
        const levelParent = target.parentElement;
        return [...levelParent.querySelectorAll(':scope > vaadin-side-nav-item')];
    }

    if (railMode && target && target.hasAttribute('root-item')) {
        return [...rail.querySelectorAll(':scope > vaadin-side-nav-item[root-item]')];
    }

    const all = [...rail.querySelectorAll('vaadin-side-nav-item')];
    return all.filter(item => {
        let parent = item.parentElement;
        while (parent && parent !== rail) {
            if (parent.localName === 'vaadin-side-nav-item' && !parent.expanded) {
                return false;
            }
            parent = parent.parentElement;
        }
        return true;
    });
}
```

- [ ] **Step 3: Run tests**

Run: `cd /workspace && ./mvnw -pl addon install -DskipTests -q && ./mvnw -pl e2e verify -q`
Expected: Task 8 tests PASS, prior tests still green. If the "first popover item" focus test is flaky due to timing, the `requestAnimationFrame` may need to become `setTimeout(..., 0)` or a small chained `await popover.updateComplete`. Debug against the actual DOM in the running dev server if needed.

- [ ] **Step 4: Commit**

```bash
git add addon/src/main/resources/META-INF/resources/frontend/side-nav-rail-keyboard.js \
        e2e/src/test/playwright/tests/keyboard-navigation.spec.ts
git commit -m "feat(addon): Arrow-Right enters popover + Arrow-Up/Down inside popover"
```

---

## Task 9: Popover Arrow-Right expand/descend + Arrow-Left collapse/back/close

Per §4.4.3 final rows:
- Arrow-Right on a nested-in-popover item: expand if collapsed + has children; if expanded, descend to first child.
- Arrow-Left: collapse if expanded; else if nested, focus popover-parent; else (top-level popover item) close popover and return focus to owning rail-root.

**Files:**
- Modify: `addon/src/main/resources/META-INF/resources/frontend/side-nav-rail-keyboard.js`
- Modify: `e2e/src/test/playwright/tests/keyboard-navigation.spec.ts`

- [ ] **Step 1: Add failing E2E tests**

Append to `keyboard-navigation.spec.ts`:

```typescript
test.describe('rail mode — popover tree navigation (Arrow-Right/Left)', () => {
    test('Arrow-Right on collapsed nested parent expands it (focus stays)', async ({ page }) => {
        await page.goto('/keyboard-navigation');
        await page.locator('#toggle-rail').click();

        await page.locator('vaadin-side-nav-item[path="admin"]').focus();
        await page.keyboard.press('ArrowRight');  // into popover — on "Users"

        await page.keyboard.press('ArrowRight');
        const users = page.locator(
            'vaadin-popover-overlay[opened] vaadin-side-nav-item[path="admin/users"]');
        await expect(users).toHaveJSProperty('expanded', true);
        await expect(users).toBeFocused();
    });

    test('Arrow-Right on expanded nested parent descends to first child', async ({ page }) => {
        await page.goto('/keyboard-navigation');
        await page.locator('#toggle-rail').click();

        await page.locator('vaadin-side-nav-item[path="admin"]').focus();
        await page.keyboard.press('ArrowRight');  // on Users

        // First Right expands, second Right descends.
        await page.keyboard.press('ArrowRight');
        await page.keyboard.press('ArrowRight');

        await expect(page.locator(
            'vaadin-popover-overlay[opened] vaadin-side-nav-item[path="admin/users/active"]'))
            .toBeFocused();
    });

    test('Arrow-Left on expanded nested parent collapses it', async ({ page }) => {
        await page.goto('/keyboard-navigation');
        await page.locator('#toggle-rail').click();

        await page.locator('vaadin-side-nav-item[path="admin"]').focus();
        await page.keyboard.press('ArrowRight');  // on Users
        await page.keyboard.press('ArrowRight');  // expanded

        await page.keyboard.press('ArrowLeft');
        await expect(page.locator(
            'vaadin-popover-overlay[opened] vaadin-side-nav-item[path="admin/users"]'))
            .toHaveJSProperty('expanded', false);
    });

    test('Arrow-Left on nested child (collapsed parent) moves to popover parent', async ({ page }) => {
        await page.goto('/keyboard-navigation');
        await page.locator('#toggle-rail').click();

        await page.locator('vaadin-side-nav-item[path="admin"]').focus();
        await page.keyboard.press('ArrowRight');  // on Users
        await page.keyboard.press('ArrowRight');  // expanded, still on Users
        await page.keyboard.press('ArrowRight');  // on Active

        await page.keyboard.press('ArrowLeft');  // collapse Users (expanded state)
        // Active is child of expanded Users; first ArrowLeft collapses Users
        // and focus stays on Users per §4.4.3.
        await expect(page.locator(
            'vaadin-popover-overlay[opened] vaadin-side-nav-item[path="admin/users"]'))
            .toBeFocused();
    });

    test('Arrow-Left on top-level popover item closes popover + focuses root', async ({ page }) => {
        await page.goto('/keyboard-navigation');
        await page.locator('#toggle-rail').click();

        await page.locator('vaadin-side-nav-item[path="admin"]').focus();
        await page.keyboard.press('ArrowRight');  // into popover, on Users (top-level in popover)

        await page.keyboard.press('ArrowLeft');
        await expect(page.locator('vaadin-popover-overlay[opened]')).toHaveCount(0);
        await expect(page.locator('vaadin-side-nav-item[path="admin"]')).toBeFocused();
    });
});
```

Run: expected FAIL — Left-at-popover-top doesn't close popover yet; Right-inside-popover doesn't expand.

- [ ] **Step 2: Extend the adapter**

Rewrite `moveFocusLeft` to handle the three popover cases:

```javascript
function moveFocusLeft(item, rail) {
    // 1. Expanded → collapse (both in rail tree and inside popover)
    if (item.expanded && hasChildren(item)) {
        item.expanded = false;
        return;
    }

    // 2. Inside a popover
    const overlay = item.closest('vaadin-popover-overlay');
    if (overlay) {
        const popoverParent = parentItemWithin(item, overlay);
        if (popoverParent) {
            popoverParent.focus();
            return;
        }
        // Top-level in popover → close popover and return focus to owning root.
        const owner = overlay.positionTarget;
        overlay.opened = false;
        if (owner) owner.focus();
        return;
    }

    // 3. Normal rail tree → parent item
    const parent = parentItem(item, rail);
    if (parent) {
        parent.focus();
    }
}

function parentItemWithin(item, scope) {
    let p = item.parentElement;
    while (p && p !== scope) {
        if (p.localName === 'vaadin-side-nav-item') {
            return p;
        }
        p = p.parentElement;
    }
    return null;
}
```

`moveFocusRight` already handles expand-then-descend correctly for nested items — it doesn't need changes, because the existing logic (`if (!item.expanded) item.expanded = true; else focus first child`) works identically for popover-nested items. Verify by rereading the function.

- [ ] **Step 3: Run tests**

Run: `cd /workspace && ./mvnw -pl addon install -DskipTests -q && ./mvnw -pl e2e verify -q`
Expected: all Task 9 tests PASS. All prior tests green.

- [ ] **Step 4: Commit**

```bash
git add addon/src/main/resources/META-INF/resources/frontend/side-nav-rail-keyboard.js \
        e2e/src/test/playwright/tests/keyboard-navigation.spec.ts
git commit -m "feat(addon): popover tree navigation via Arrow-Right/Left"
```

---

## Task 10: Verify full build + update docs

Run the full `./mvnw clean verify` in a fresh state to catch any cached-bundle issues, then update `plans/2026-04-21-side-nav-rail-plan.md` and `CLAUDE.md` to reflect shipped state.

**Files:**
- Modify: `plans/2026-04-21-side-nav-rail-plan.md`
- Modify: `CLAUDE.md` (local, untracked — update for current-state accuracy)

- [ ] **Step 1: Run full clean verify**

```bash
cd /workspace
rm -rf e2e/src/main/bundles/prod.bundle e2e/target
./mvnw clean verify
```

Expected: all tests pass — addon unit suite (original 67 + 12 new = ~79) + E2E (original 25 + new keyboard-nav tests, roughly 22 → ~47 total).

If anything fails, diagnose and fix before proceeding. Do not update docs against a red build.

- [ ] **Step 2: Update the master plan's status block**

In `plans/2026-04-21-side-nav-rail-plan.md`, update the "Implementation status" block at the top to reflect phase 9.2 completion. Follow the pattern established by the §9.1 entry; include:
- branch name (`phase9.2/accessibility`) and whether it's been merged
- new test counts (~79 unit + ~47 E2E)
- a short bullet list of what §9.2 delivered (keyboard nav in both modes, ARIA sync, focus management)

- [ ] **Step 3: Update CLAUDE.md**

Update the "Current project: SideNav Rail addon" section at the top of `CLAUDE.md` with the new completion status and test counts; mention the new JS module `side-nav-rail-keyboard.js` under highlights.

- [ ] **Step 4: Commit**

```bash
git add plans/2026-04-21-side-nav-rail-plan.md CLAUDE.md
git commit -m "docs: record §9.2 accessibility completion"
```

- [ ] **Step 5: Final verification**

```bash
git log --oneline phase9.2/accessibility ^main
```

Expected: ~10 commits (1 spec + 9 tasks + 1 docs).

Hand off to `superpowers:finishing-a-development-branch` for the merge/PR decision.
