# Phase 4 — Popover

**Prereqs:** Phase 3 complete. Read [`../2026-04-21-side-nav-rail-plan.md`](../2026-04-21-side-nav-rail-plan.md) for cross-phase conventions.

**Output of this phase:** `SideNavRailItem` lazily attaches a `Popover` for items that have children; the popover content is a copy of the children (original tree untouched); the popover is gated by `PopoverMode` + current rail state.

---

## Task 11: Popover lifecycle on `SideNavRailItem` (TDD, structural test only)

**Files:**
- Modify: `addon/src/main/java/org/vaadin/addons/componentfactory/sidenavrail/SideNavRailItem.java`
- Create: `addon/src/test/java/org/vaadin/addons/componentfactory/sidenavrail/unit/PopoverLifecycleTest.java`

Covers: a `SideNavRailItem` that has at least one child creates and attaches a `Popover` targeting itself on first attach. The popover is configured for hover with the correct timings and overlay role. Hover behaviour itself is **not** verified here — that requires a real browser and is covered by E2E tests in Phase 6.

- [ ] **Step 1: Write the failing test**

Create the file with the full Apache 2.0 header, then:
```java
package org.vaadin.addons.componentfactory.sidenavrail.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.mvysny.kaributesting.v10.MockVaadin;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.popover.Popover;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.vaadin.addons.componentfactory.sidenavrail.SideNavRail;
import org.vaadin.addons.componentfactory.sidenavrail.SideNavRailItem;

class PopoverLifecycleTest {

    @BeforeEach
    void setUp() {
        MockVaadin.setup();
    }

    @AfterEach
    void tearDown() {
        MockVaadin.tearDown();
    }

    @Test
    void itemWithoutChildrenDoesNotAttachPopover() {
        SideNavRail nav = new SideNavRail();
        SideNavRailItem leaf = new SideNavRailItem("Dashboard", "/");
        nav.addItem(leaf);
        UI.getCurrent().add(nav);

        long popovers = UI.getCurrent().getChildren()
                .flatMap(c -> c.getChildren())
                .filter(c -> c instanceof Popover)
                .count();
        assertEquals(0L, popovers);
    }

    @Test
    void itemWithChildrenAttachesPopoverOnFirstAttach() {
        SideNavRail nav = new SideNavRail();
        SideNavRailItem parent = new SideNavRailItem("Code");
        parent.addItem(new SideNavRailItem("Branches", "/branches"));
        parent.addItem(new SideNavRailItem("Tags", "/tags"));
        nav.addItem(parent);
        UI.getCurrent().add(nav);

        Popover popover = findPopoverTargeting(parent);
        assertNotNull(popover, "Popover should be attached for parent with children");
        assertEquals("menu", popover.getOverlayRole());
        assertTrue(popover.isOpenOnHover());
        assertEquals(200, popover.getHoverDelay());
        assertEquals(300, popover.getHideDelay());
    }

    private static Popover findPopoverTargeting(SideNavRailItem item) {
        return UI.getCurrent().getChildren()
                .flatMap(c -> c.getChildren())
                .filter(c -> c instanceof Popover)
                .map(c -> (Popover) c)
                .filter(p -> p.getTarget() == item)
                .findFirst()
                .orElse(null);
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./mvnw -pl addon test`
Expected: compilation succeeds, but tests fail — no popover is created.

- [ ] **Step 3: Add popover lifecycle to `SideNavRailItem`**

Edit `/workspace/addon/src/main/java/org/vaadin/addons/componentfactory/sidenavrail/SideNavRailItem.java`.

Add imports:
```java
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.popover.Popover;
import com.vaadin.flow.component.popover.PopoverPosition;
import com.vaadin.flow.component.sidenav.SideNav;
```

Add field:
```java
    private Popover popover;
```

Add method:
```java
    @Override
    protected void onAttach(AttachEvent event) {
        super.onAttach(event);
        ensurePopover();
    }

    private void ensurePopover() {
        if (popover != null) {
            return;
        }
        if (getItems().isEmpty()) {
            return;
        }
        popover = new Popover();
        popover.setTarget(this);
        popover.setOpenOnHover(true);
        popover.setOpenOnClick(false);
        popover.setOpenOnFocus(false);
        popover.setHoverDelay(200);
        popover.setHideDelay(300);
        popover.setOverlayRole("menu");
        popover.setPosition(resolveEndTopPosition());

        Element uiEl = getUI().orElseThrow(IllegalStateException::new).getElement();
        uiEl.appendChild(popover.getElement());

        populatePopover();
    }

    private void populatePopover() {
        SideNav nested = new SideNav();
        for (var child : getItems()) {
            nested.addItem(child);
        }
        popover.removeAll();
        popover.add(nested);
    }

    /**
     * Resolves the right-aligned, top-anchored popover position. The spec flags the exact
     * enum value as implementation-verified; try {@code END_TOP} first, then fall back to
     * {@code END}.
     */
    private static PopoverPosition resolveEndTopPosition() {
        try {
            return PopoverPosition.valueOf("END_TOP");
        } catch (IllegalArgumentException notFound) {
            return PopoverPosition.valueOf("END");
        }
    }
```

> **Note:** `populatePopover()` currently *moves* the children into the nested SideNav, which removes them from their parent in the outer nav. That's fine for the unit test (just verifies the popover is set up), but for the actual user experience we need the children to also stay in the outer nav for inline-expansion. Task 12 addresses the rendering strategy.

- [ ] **Step 4: Run tests**

Run: `./mvnw -pl addon test`
Expected: `BUILD SUCCESS`. All popover lifecycle tests pass.

- [ ] **Step 5: Commit**

```bash
git add addon/src/main/java/org/vaadin/addons/componentfactory/sidenavrail/SideNavRailItem.java \
        addon/src/test/java/org/vaadin/addons/componentfactory/sidenavrail/unit/PopoverLifecycleTest.java
git commit -m "feat(SideNavRailItem): attach Popover for items with children"
```

---

## Task 12: Popover content = children copy (rendering strategy)

**Files:**
- Modify: `addon/src/main/java/org/vaadin/addons/componentfactory/sidenavrail/SideNavRailItem.java`
- Modify: `addon/src/test/java/org/vaadin/addons/componentfactory/sidenavrail/unit/PopoverLifecycleTest.java`

The spec (§4.2) leaves the rendering strategy open. We pick **copy** over **DOM-reparenting**: the outer nav keeps its children (for inline expansion in normal mode), and the popover renders freshly-built clones based on the same label/path/icon metadata. This avoids cross-tree state drift and is simpler to reason about.

- [ ] **Step 1: Extend the test to assert both trees exist**

Append to `PopoverLifecycleTest.java` (inside the class):
```java
    @Test
    void outerChildrenSurvivePopoverPopulation() {
        SideNavRail nav = new SideNavRail();
        SideNavRailItem parent = new SideNavRailItem("Code");
        parent.addItem(new SideNavRailItem("Branches", "/branches"));
        parent.addItem(new SideNavRailItem("Tags", "/tags"));
        nav.addItem(parent);
        UI.getCurrent().add(nav);

        assertEquals(2, parent.getItems().size(),
                "Outer nav must retain its children for inline expansion");

        Popover popover = UI.getCurrent().getChildren()
                .flatMap(c -> c.getChildren())
                .filter(c -> c instanceof Popover)
                .map(c -> (Popover) c)
                .findFirst().orElseThrow();

        long nestedChildren = popover.getChildren()
                .flatMap(c -> c.getChildren())
                .count();
        assertEquals(2L, nestedChildren, "Popover must render a mirrored copy of the children");
    }
```

- [ ] **Step 2: Run tests to verify the failure**

Run: `./mvnw -pl addon test`
Expected: `outerChildrenSurvivePopoverPopulation` fails — `parent.getItems().size()` is 0 because Task 11's implementation moves them.

- [ ] **Step 3: Switch to a copy-based rendering strategy**

Replace the `populatePopover()` method in `SideNavRailItem`:
```java
    private void populatePopover() {
        SideNav nested = new SideNav();
        for (SideNavItem child : getItems()) {
            nested.addItem(copyOf(child));
        }
        popover.removeAll();
        popover.add(nested);
    }

    private static SideNavItem copyOf(SideNavItem source) {
        String label = source.getLabel();
        String path = source.getPath();
        Component prefix = source.getPrefixComponent();

        SideNavItem copy =
                prefix != null
                        ? new SideNavItem(label, path, copyComponent(prefix))
                        : new SideNavItem(label, path);

        for (SideNavItem grandchild : source.getItems()) {
            copy.addItem(copyOf(grandchild));
        }
        return copy;
    }

    /**
     * Clones a prefix component by reinstantiating via its class. Vaadin icons are the
     * overwhelmingly common case; for anything else (custom components), fall back to
     * sharing the original reference — rare in an icon-driven rail.
     */
    private static Component copyComponent(Component source) {
        if (source instanceof com.vaadin.flow.component.icon.Icon icon) {
            com.vaadin.flow.component.icon.Icon copy = new com.vaadin.flow.component.icon.Icon();
            copy.getElement().setAttribute("icon", icon.getElement().getAttribute("icon"));
            return copy;
        }
        return source;
    }
```

Add missing imports to the file:
```java
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.sidenav.SideNavItem;
```

- [ ] **Step 4: Run tests**

Run: `./mvnw -pl addon test`
Expected: `BUILD SUCCESS`. All tests green.

- [ ] **Step 5: Commit**

```bash
git add addon/src/main/java/org/vaadin/addons/componentfactory/sidenavrail/SideNavRailItem.java \
        addon/src/test/java/org/vaadin/addons/componentfactory/sidenavrail/unit/PopoverLifecycleTest.java
git commit -m "feat(SideNavRailItem): render popover content as copy of children"
```

---

## Task 13: Popover gating by `PopoverMode` (TDD)

**Files:**
- Modify: `addon/src/main/java/org/vaadin/addons/componentfactory/sidenavrail/SideNavRail.java`
- Modify: `addon/src/main/java/org/vaadin/addons/componentfactory/sidenavrail/SideNavRailItem.java`
- Modify: `addon/src/test/java/org/vaadin/addons/componentfactory/sidenavrail/unit/PopoverModeTest.java`

The behaviour matrix in the spec (§4.1) says the popover is *attached* regardless of mode, but its *opening* is gated:

- In `COLLAPSED_ITEM` mode, the popover is open-eligible whenever the item is not inline-expanded.
- In `RAIL_ONLY` mode, the popover is open-eligible only while the nav is in rail mode.

Since Vaadin's `Popover` does not expose per-event hover filtering, we flip `openOnHover` on/off to gate it. State changes happen on: rail mode toggle, popover mode change.

- [ ] **Step 1: Extend `PopoverModeTest` with gating tests**

Append to `PopoverModeTest.java` (add these imports to the file if not already present, at the top):
```java
import com.github.mvysny.kaributesting.v10.MockVaadin;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.popover.Popover;
import org.vaadin.addons.componentfactory.sidenavrail.SideNavRailItem;
```

Then append these methods to the class:
```java
    @Test
    void collapsedItemMode_enablesHoverRegardlessOfRailMode() {
        MockVaadin.setup();
        try {
            SideNavRail nav = new SideNavRail();   // default COLLAPSED_ITEM
            SideNavRailItem parent = new SideNavRailItem("Code");
            parent.addItem(new SideNavRailItem("Branches", "/branches"));
            nav.addItem(parent);
            UI.getCurrent().add(nav);

            Popover p = locatePopover(parent);
            assertEquals(true, p.isOpenOnHover(),
                    "Popover should be hover-open-eligible in COLLAPSED_ITEM mode");
        } finally {
            MockVaadin.tearDown();
        }
    }

    @Test
    void railOnlyMode_disablesHoverUntilRailEngaged() {
        MockVaadin.setup();
        try {
            SideNavRail nav = new SideNavRail();
            nav.setPopoverMode(PopoverMode.RAIL_ONLY);
            SideNavRailItem parent = new SideNavRailItem("Code");
            parent.addItem(new SideNavRailItem("Branches", "/branches"));
            nav.addItem(parent);
            UI.getCurrent().add(nav);

            Popover p = locatePopover(parent);
            assertEquals(false, p.isOpenOnHover(),
                    "RAIL_ONLY mode in normal nav — hover disabled");

            nav.setRailMode(true);
            assertEquals(true, p.isOpenOnHover(),
                    "RAIL_ONLY mode engaged — hover enabled");

            nav.setRailMode(false);
            assertEquals(false, p.isOpenOnHover(),
                    "RAIL_ONLY mode disengaged — hover disabled again");
        } finally {
            MockVaadin.tearDown();
        }
    }

    private static Popover locatePopover(SideNavRailItem item) {
        return UI.getCurrent().getChildren()
                .flatMap(c -> c.getChildren())
                .filter(c -> c instanceof Popover)
                .map(c -> (Popover) c)
                .filter(p -> p.getTarget() == item)
                .findFirst()
                .orElseThrow();
    }
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./mvnw -pl addon test`
Expected: compilation succeeds; `railOnlyMode_*` test fails (popover always on-hover) while `collapsedItemMode_*` passes by luck.

- [ ] **Step 3: Wire gating through `SideNavRail`**

In `SideNavRail.java`, extend both `setRailMode` and `setPopoverMode` to propagate state to child items:
```java
    public void setRailMode(boolean railMode) {
        if (this.railMode == railMode) {
            return;
        }
        this.railMode = railMode;
        if (railMode) {
            getElement().setAttribute("theme", RAIL_THEME);
        } else {
            getElement().removeAttribute("theme");
        }
        updatePopoverGating();
        ComponentUtil.fireEvent(this, new RailModeChangedEvent(this, false, railMode));
    }

    public void setPopoverMode(PopoverMode mode) {
        this.popoverMode = java.util.Objects.requireNonNull(mode, "PopoverMode must not be null");
        updatePopoverGating();
    }

    private void updatePopoverGating() {
        getChildren()
                .filter(c -> c instanceof SideNavRailItem)
                .map(c -> (SideNavRailItem) c)
                .forEach(i -> i.applyPopoverGating(popoverMode, railMode));
    }
```

Expose a package-private hook on `SideNavRailItem`:
```java
    /**
     * Applies the open-eligibility of this item's popover based on the owning
     * {@link SideNavRail}'s current {@link PopoverMode} and rail state. Package-private —
     * external callers should use {@link SideNavRail#setPopoverMode(PopoverMode)} or
     * {@link SideNavRail#setRailMode(boolean)} instead.
     */
    void applyPopoverGating(PopoverMode mode, boolean railMode) {
        if (popover == null) {
            return;
        }
        boolean eligible =
                switch (mode) {
                    case COLLAPSED_ITEM -> true;  // hover fires whenever the popover is attached
                    case RAIL_ONLY -> railMode;
                };
        popover.setOpenOnHover(eligible);
    }
```

Modify `ensurePopover()` so the gating is applied at creation time too. Replace the current body:
```java
    private void ensurePopover() {
        if (popover != null) {
            return;
        }
        if (getItems().isEmpty()) {
            return;
        }
        popover = new Popover();
        popover.setTarget(this);
        popover.setOpenOnClick(false);
        popover.setOpenOnFocus(false);
        popover.setHoverDelay(200);
        popover.setHideDelay(300);
        popover.setOverlayRole("menu");
        popover.setPosition(resolveEndTopPosition());

        Element uiEl = getUI().orElseThrow(IllegalStateException::new).getElement();
        uiEl.appendChild(popover.getElement());

        populatePopover();

        SideNavRail owner = findOwnerRail();
        if (owner != null) {
            applyPopoverGating(owner.getPopoverMode(), owner.isRailMode());
        } else {
            popover.setOpenOnHover(true);  // standalone item — default on
        }
    }

    private SideNavRail findOwnerRail() {
        com.vaadin.flow.component.Component p = getParent().orElse(null);
        while (p != null) {
            if (p instanceof SideNavRail rail) {
                return rail;
            }
            p = p.getParent().orElse(null);
        }
        return null;
    }
```

Note: the earlier standalone `popover.setOpenOnHover(true)` call inside `ensurePopover()` is gone — gating is now centralised in `applyPopoverGating`.

- [ ] **Step 4: Run tests**

Run: `./mvnw -pl addon test`
Expected: `BUILD SUCCESS`. All tests in `PopoverModeTest` + existing `PopoverLifecycleTest` green.

- [ ] **Step 5: Commit**

```bash
git add addon/src/main/java/org/vaadin/addons/componentfactory/sidenavrail/ \
        addon/src/test/java/org/vaadin/addons/componentfactory/sidenavrail/unit/PopoverModeTest.java
git commit -m "feat: gate popover hover by PopoverMode and rail state"
```

---

## Phase 4 complete when

- `./mvnw -pl addon test` green with all three unit-test classes (`RailModeStateTest`, `PopoverModeTest`, `LabelWrapTest`) plus the new `PopoverLifecycleTest`.
- Popover attachment and hover-gating covered by tests.
- Three green commits added in Phase 4.

Next: [Phase 5 — Test runtime](./05-test-runtime.md).
