# Phase 3 — Item and styling

**Prereqs:** Phase 2 complete. Read [`../2026-04-21-side-nav-rail-plan.md`](../2026-04-21-side-nav-rail-plan.md) for cross-phase conventions.

**Output of this phase:** `SideNavRailItem` with idempotent label wrapping, plus the CSS module that makes rail mode actually look like rail mode.

---

## Task 9: `SideNavRailItem` — label wrap (TDD)

**Files:**
- Create: `addon/src/main/java/org/vaadin/addons/componentfactory/sidenavrail/SideNavRailItem.java`
- Create: `addon/src/test/java/org/vaadin/addons/componentfactory/sidenavrail/unit/LabelWrapTest.java`

This is the trickiest part of the addon. The standard `SideNavItem` stores its label as bare text content on its root element. Our override must wrap that text in a `<span class="label">` so CSS can target it, and must stay idempotent across multiple `setLabel(...)` calls.

- [ ] **Step 1: Write the failing test**

Create the file with the full Apache 2.0 header, then:
```java
package org.vaadin.addons.componentfactory.sidenavrail.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vaadin.flow.dom.Element;
import org.junit.jupiter.api.Test;
import org.vaadin.addons.componentfactory.sidenavrail.SideNavRailItem;

class LabelWrapTest {

    @Test
    void constructorWrapsLabelInSpan() {
        SideNavRailItem item = new SideNavRailItem("Dashboard");
        Element span = findLabelSpan(item.getElement());
        assertEquals("span", span.getTag());
        assertEquals("label", span.getAttribute("class"));
        assertEquals("Dashboard", span.getText());
    }

    @Test
    void setLabelUpdatesExistingSpanWithoutDuplicating() {
        SideNavRailItem item = new SideNavRailItem("Dashboard");
        item.setLabel("Overview");

        long spanCount = item.getElement().getChildren()
                .filter(e -> "span".equals(e.getTag()))
                .filter(e -> "label".equals(e.getAttribute("class")))
                .count();
        assertEquals(1L, spanCount, "Expected exactly one label span");
        assertEquals("Overview", findLabelSpan(item.getElement()).getText());
    }

    @Test
    void wrappedLabelLeavesSuperLabelInSync() {
        SideNavRailItem item = new SideNavRailItem("Dashboard");
        assertEquals("Dashboard", item.getLabel());
        item.setLabel("Overview");
        assertEquals("Overview", item.getLabel());
    }

    @Test
    void prefixSlottedIconSurvivesLabelWrap() {
        com.vaadin.flow.component.icon.Icon icon =
                com.vaadin.flow.component.icon.VaadinIcon.DASHBOARD.create();
        SideNavRailItem item = new SideNavRailItem("Dashboard", "/", icon);

        boolean iconStillSlotted = item.getElement().getChildren()
                .anyMatch(e -> "prefix".equals(e.getAttribute("slot")));
        assertTrue(iconStillSlotted, "Prefix icon should survive the label wrap");
    }

    private static Element findLabelSpan(Element root) {
        return root.getChildren()
                .filter(e -> "span".equals(e.getTag()))
                .filter(e -> "label".equals(e.getAttribute("class")))
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "No <span class=\"label\"> found on element " + root.getOuterHTML()));
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./mvnw -pl addon test`
Expected: compilation failure.

- [ ] **Step 3: Implement `SideNavRailItem`**

Create `SideNavRailItem.java` with the full Apache 2.0 header, then:
```java
package org.vaadin.addons.componentfactory.sidenavrail;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.dom.Element;
import java.util.List;
import java.util.Optional;

/**
 * {@link SideNavItem} variant that renders its string label inside a
 * {@code <span class="label">}. The wrap is what lets CSS hide the label in rail mode
 * ({@code vaadin-side-nav[theme~="rail"] vaadin-side-nav-item .label}); a bare text node
 * cannot be targeted by CSS.
 */
public class SideNavRailItem extends SideNavItem {

    private static final String LABEL_SPAN_CLASS = "label";

    public SideNavRailItem(String label) {
        super(label);
        wrapLabel();
    }

    public SideNavRailItem(String label, String path) {
        super(label, path);
        wrapLabel();
    }

    public SideNavRailItem(String label, Class<? extends Component> view) {
        super(label, view);
        wrapLabel();
    }

    public SideNavRailItem(String label, String path, Component prefixComponent) {
        super(label, path, prefixComponent);
        wrapLabel();
    }

    public SideNavRailItem(
            String label, Class<? extends Component> view, Component prefixComponent) {
        super(label, view, prefixComponent);
        wrapLabel();
    }

    @Override
    public void setLabel(String label) {
        super.setLabel(label);
        wrapLabel();
    }

    private void wrapLabel() {
        Element root = getElement();
        String label = super.getLabel();
        if (label == null) {
            return;
        }

        Optional<Element> existing = findLabelSpan(root);
        if (existing.isPresent()) {
            existing.get().setText(label);
            removeBareTextSiblings(root);
            return;
        }

        Element span = new Element("span");
        span.setAttribute("class", LABEL_SPAN_CLASS);
        span.setText(label);
        removeBareTextSiblings(root);
        root.appendChild(span);
    }

    private static Optional<Element> findLabelSpan(Element root) {
        return root.getChildren()
                .filter(e -> "span".equals(e.getTag()))
                .filter(e -> LABEL_SPAN_CLASS.equals(e.getAttribute("class")))
                .findFirst();
    }

    /**
     * Clears the bare text content that {@code super.setLabel(...)} leaves directly on
     * the root element (i.e. text that is neither a child element nor slotted). Slotted
     * children like {@code [slot="prefix"]} are untouched.
     */
    private static void removeBareTextSiblings(Element root) {
        if (!root.getText().isEmpty()) {
            // Snapshot child elements so we can re-attach them after the setText("") call
            // that would otherwise nuke them along with the text.
            List<Element> children = root.getChildren().toList();
            root.setText("");
            for (Element child : children) {
                root.appendChild(child);
            }
        }
    }
}
```

> **Note:** the `removeBareTextSiblings` helper works around a quirk of Vaadin's Element API: `Element.setText("")` clears both text *and* element children. The snapshot-and-reattach pattern preserves slotted children across the wipe. If `LabelWrapTest` fails in a way that points to element reordering (prefix icon moving to the end), swap the order: call `setText("")` first, then `appendChild` the saved children before `appendChild(span)`.

- [ ] **Step 4: Run tests**

Run: `./mvnw -pl addon test`
Expected: `LabelWrapTest` all green.

- [ ] **Step 5: Commit**

```bash
git add addon/src/main/java/org/vaadin/addons/componentfactory/sidenavrail/SideNavRailItem.java \
        addon/src/test/java/org/vaadin/addons/componentfactory/sidenavrail/unit/LabelWrapTest.java
git commit -m "feat(SideNavRailItem): wrap string label in <span class='label'>"
```

---

## Task 10: CSS module

**Files:**
- Create: `addon/src/main/resources/META-INF/resources/frontend/side-nav-rail.css`

- [ ] **Step 1: Write the CSS file**

Create `/workspace/addon/src/main/resources/META-INF/resources/frontend/side-nav-rail.css`:
```css
/*
 * Copyright 2026 Vaadin Ltd. Licensed under the Apache License, Version 2.0.
 *
 * Styles for the SideNavRail addon. Applied only when `theme="rail"` is set on the
 * root <vaadin-side-nav> element by SideNavRail#setRailMode(true).
 */

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

- [ ] **Step 2: Rebuild addon (verify resource is packaged)**

Run: `./mvnw -pl addon -am package -DskipTests`
Expected: `BUILD SUCCESS`. The CSS will be inside `target/side-nav-rail-0.1.0-SNAPSHOT.jar` under `META-INF/resources/frontend/`.

- [ ] **Step 3: Commit**

```bash
git add addon/src/main/resources/
git commit -m "feat: add rail-mode CSS module"
```

---

## Phase 3 complete when

- `./mvnw -pl addon test` is green, including `LabelWrapTest` all four cases.
- `side-nav-rail.css` is bundled into the addon jar under `META-INF/resources/frontend/`.
- Two green commits added in Phase 3.

Next: [Phase 4 — Popover](./04-popover.md).
