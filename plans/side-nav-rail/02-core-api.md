# Phase 2 — Core API

**Prereqs:** Phase 1 complete. Read [`../2026-04-21-side-nav-rail-plan.md`](../2026-04-21-side-nav-rail-plan.md) for cross-phase conventions.

**Output of this phase:** public API surface of the addon is in place — `PopoverMode` enum, `RailModeChangedEvent`, `SideNavRail` with `setRailMode`/`isRailMode`/event-firing/`PopoverMode` getter and setter. Unit tests cover each.

---

## Task 4: `PopoverMode` enum

**Files:**
- Create: `addon/src/main/java/org/vaadin/addons/componentfactory/sidenavrail/PopoverMode.java`

Enum is trivial — no dedicated test (enum semantics are covered by later integration tests).

- [ ] **Step 1: Write the enum**

Create the file with the full Apache 2.0 header, then:
```java
package org.vaadin.addons.componentfactory.sidenavrail;

/**
 * Controls when the hover popover on a {@link SideNavRailItem} with children appears.
 */
public enum PopoverMode {

    /**
     * Popover appears for every non-expanded item with children, regardless of whether
     * the nav is in rail mode. This is the default and mirrors the customer-requested
     * behaviour of {@link SideNavRail}.
     */
    COLLAPSED_ITEM,

    /**
     * Popover appears only when the nav as a whole is in rail mode
     * ({@link SideNavRail#isRailMode()} {@code == true}). Inline-closed items in normal
     * mode behave like a standard {@link com.vaadin.flow.component.sidenav.SideNav} —
     * they only open on click.
     */
    RAIL_ONLY
}
```

- [ ] **Step 2: Compile**

Run: `./mvnw -pl addon -am compile`
Expected: `BUILD SUCCESS`.

- [ ] **Step 3: Commit**

```bash
git add addon/src/main/java/org/vaadin/addons/componentfactory/sidenavrail/PopoverMode.java
git commit -m "feat: add PopoverMode enum"
```

---

## Task 5: `RailModeChangedEvent` class

**Files:**
- Create: `addon/src/main/java/org/vaadin/addons/componentfactory/sidenavrail/RailModeChangedEvent.java`

- [ ] **Step 1: Write the event class**

With the full Apache 2.0 header:
```java
package org.vaadin.addons.componentfactory.sidenavrail;

import com.vaadin.flow.component.ComponentEvent;

/** Fired whenever {@link SideNavRail#setRailMode(boolean)} changes the rail-mode state. */
public class RailModeChangedEvent extends ComponentEvent<SideNavRail> {

    private final boolean railMode;

    public RailModeChangedEvent(SideNavRail source, boolean fromClient, boolean railMode) {
        super(source, fromClient);
        this.railMode = railMode;
    }

    public boolean isRailMode() {
        return railMode;
    }
}
```

> **Note on the signature:** the spec shows a two-argument constructor, but we need the event to carry the new state — hence `boolean railMode` as a third parameter. The firing site in `SideNavRail` passes `isRailMode()` at the time of firing.

- [ ] **Step 2: Compile**

Run: `./mvnw -pl addon -am compile`
Expected: `BUILD SUCCESS`.

- [ ] **Step 3: Commit**

```bash
git add addon/src/main/java/org/vaadin/addons/componentfactory/sidenavrail/RailModeChangedEvent.java
git commit -m "feat: add RailModeChangedEvent"
```

---

## Task 6: `SideNavRail` — rail-mode state (TDD)

**Files:**
- Create: `addon/src/main/java/org/vaadin/addons/componentfactory/sidenavrail/SideNavRail.java`
- Create: `addon/src/test/java/org/vaadin/addons/componentfactory/sidenavrail/unit/RailModeStateTest.java`

- [ ] **Step 1: Write the failing test**

Create the test with the full Apache 2.0 header, then:
```java
package org.vaadin.addons.componentfactory.sidenavrail.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.vaadin.addons.componentfactory.sidenavrail.SideNavRail;

class RailModeStateTest {

    @Test
    void defaultRailModeIsFalse() {
        SideNavRail nav = new SideNavRail();
        assertFalse(nav.isRailMode());
        assertNull(nav.getElement().getAttribute("theme"));
    }

    @Test
    void enablingRailModeSetsThemeAttribute() {
        SideNavRail nav = new SideNavRail();
        nav.setRailMode(true);
        assertTrue(nav.isRailMode());
        assertEquals("rail", nav.getElement().getAttribute("theme"));
    }

    @Test
    void disablingRailModeRemovesThemeAttribute() {
        SideNavRail nav = new SideNavRail();
        nav.setRailMode(true);
        nav.setRailMode(false);
        assertFalse(nav.isRailMode());
        assertNull(nav.getElement().getAttribute("theme"));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./mvnw -pl addon test`
Expected: compilation failure (`SideNavRail` does not exist yet).

- [ ] **Step 3: Write the minimal implementation**

Create `SideNavRail.java` with the full Apache 2.0 header, then:
```java
package org.vaadin.addons.componentfactory.sidenavrail;

import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.sidenav.SideNav;

/**
 * A {@link SideNav} that can be switched between normal mode and a compact rail
 * (icon-only) mode. Items with children may show a hover popover — see
 * {@link PopoverMode}.
 */
@CssImport("./side-nav-rail.css")
public class SideNavRail extends SideNav {

    private static final String RAIL_THEME = "rail";

    private boolean railMode = false;

    public SideNavRail() {
        super();
    }

    public SideNavRail(String label) {
        super(label);
    }

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
    }

    public boolean isRailMode() {
        return railMode;
    }
}
```

> **Note on `@CssImport`:** the referenced CSS file does not exist yet (added in Phase 3 Task 10). The annotation still compiles — Vaadin resolves it only at runtime when the component is first mounted in a live UI, and tests in this task do not mount the component.

- [ ] **Step 4: Run the tests**

Run: `./mvnw -pl addon test`
Expected: `RailModeStateTest` all green, `BUILD SUCCESS`.

- [ ] **Step 5: Commit**

```bash
git add addon/src/main/java/org/vaadin/addons/componentfactory/sidenavrail/SideNavRail.java \
        addon/src/test/java/org/vaadin/addons/componentfactory/sidenavrail/unit/RailModeStateTest.java
git commit -m "feat(SideNavRail): setRailMode + isRailMode with theme attribute"
```

---

## Task 7: `SideNavRail` — event firing (TDD)

**Files:**
- Modify: `addon/src/main/java/org/vaadin/addons/componentfactory/sidenavrail/SideNavRail.java`
- Modify: `addon/src/test/java/org/vaadin/addons/componentfactory/sidenavrail/unit/RailModeStateTest.java`

- [ ] **Step 1: Extend the test with event-firing cases**

Append to `RailModeStateTest.java` (inside the class, before the closing `}`):
```java
    @Test
    void togglingFiresOneEventPerChange() {
        SideNavRail nav = new SideNavRail();
        java.util.List<Boolean> received = new java.util.ArrayList<>();
        nav.addRailModeChangedListener(e -> received.add(e.isRailMode()));

        nav.setRailMode(true);
        nav.setRailMode(true);   // no-op — same value
        nav.setRailMode(false);

        assertEquals(java.util.List.of(true, false), received);
    }

    @Test
    void eventReportsFromClientFalseForServerCalls() {
        SideNavRail nav = new SideNavRail();
        java.util.concurrent.atomic.AtomicBoolean fromClient =
                new java.util.concurrent.atomic.AtomicBoolean(true);
        nav.addRailModeChangedListener(e -> fromClient.set(e.isFromClient()));

        nav.setRailMode(true);

        assertFalse(fromClient.get());
    }
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./mvnw -pl addon test`
Expected: compilation failure (`addRailModeChangedListener` does not exist yet).

- [ ] **Step 3: Add the listener method and event firing**

Edit `SideNavRail.java`. Add imports at the top:
```java
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.shared.Registration;
```

Replace the body of `setRailMode`:
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
        ComponentUtil.fireEvent(this, new RailModeChangedEvent(this, false, railMode));
    }
```

Add the listener method (anywhere in the class):
```java
    public Registration addRailModeChangedListener(
            ComponentEventListener<RailModeChangedEvent> listener) {
        return ComponentUtil.addListener(this, RailModeChangedEvent.class, listener);
    }
```

- [ ] **Step 4: Run tests**

Run: `./mvnw -pl addon test`
Expected: all `RailModeStateTest` tests green, `BUILD SUCCESS`.

- [ ] **Step 5: Commit**

```bash
git add addon/src/main/java/org/vaadin/addons/componentfactory/sidenavrail/SideNavRail.java \
        addon/src/test/java/org/vaadin/addons/componentfactory/sidenavrail/unit/RailModeStateTest.java
git commit -m "feat(SideNavRail): fire RailModeChangedEvent on state change"
```

---

## Task 8: `SideNavRail` — PopoverMode getter/setter (TDD)

**Files:**
- Modify: `addon/src/main/java/org/vaadin/addons/componentfactory/sidenavrail/SideNavRail.java`
- Create: `addon/src/test/java/org/vaadin/addons/componentfactory/sidenavrail/unit/PopoverModeTest.java`

- [ ] **Step 1: Write the failing test**

Create the file with the full Apache 2.0 header, then:
```java
package org.vaadin.addons.componentfactory.sidenavrail.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.vaadin.addons.componentfactory.sidenavrail.PopoverMode;
import org.vaadin.addons.componentfactory.sidenavrail.SideNavRail;

class PopoverModeTest {

    @Test
    void defaultModeIsCollapsedItem() {
        SideNavRail nav = new SideNavRail();
        assertEquals(PopoverMode.COLLAPSED_ITEM, nav.getPopoverMode());
    }

    @Test
    void modeCanBeChanged() {
        SideNavRail nav = new SideNavRail();
        nav.setPopoverMode(PopoverMode.RAIL_ONLY);
        assertEquals(PopoverMode.RAIL_ONLY, nav.getPopoverMode());
    }

    @Test
    void settingNullIsRejected() {
        SideNavRail nav = new SideNavRail();
        assertThrows(NullPointerException.class, () -> nav.setPopoverMode(null));
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./mvnw -pl addon test`
Expected: compilation failure.

- [ ] **Step 3: Add the getter/setter**

Edit `SideNavRail.java`. Add a field near `railMode`:
```java
    private PopoverMode popoverMode = PopoverMode.COLLAPSED_ITEM;
```

Add methods (below `isRailMode()`):
```java
    public PopoverMode getPopoverMode() {
        return popoverMode;
    }

    public void setPopoverMode(PopoverMode mode) {
        this.popoverMode = java.util.Objects.requireNonNull(mode, "PopoverMode must not be null");
    }
```

- [ ] **Step 4: Run tests**

Run: `./mvnw -pl addon test`
Expected: `BUILD SUCCESS`, all tests green.

- [ ] **Step 5: Commit**

```bash
git add addon/src/main/java/org/vaadin/addons/componentfactory/sidenavrail/SideNavRail.java \
        addon/src/test/java/org/vaadin/addons/componentfactory/sidenavrail/unit/PopoverModeTest.java
git commit -m "feat(SideNavRail): add PopoverMode getter and setter"
```

---

## Phase 2 complete when

- `./mvnw -pl addon test` is green — all tests in `RailModeStateTest` and `PopoverModeTest` pass.
- `SideNavRail` has `setRailMode`/`isRailMode`/`addRailModeChangedListener`/`setPopoverMode`/`getPopoverMode` public methods.
- `RailModeChangedEvent` carries `isRailMode()` + `isFromClient()`.
- `PopoverMode` has `COLLAPSED_ITEM` (default) and `RAIL_ONLY` values.
- Five green commits added in Phase 2.

Next: [Phase 3 — Item and styling](./03-item-and-styling.md).
