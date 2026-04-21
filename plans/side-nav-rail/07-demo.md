# Phase 7 — Demo

**Prereqs:** Phase 4 complete (Phase 7 is independent of Phases 5–6 — it only needs the component itself). Read [`../2026-04-21-side-nav-rail-plan.md`](../2026-04-21-side-nav-rail-plan.md) for cross-phase conventions.

**Output of this phase:** the demo module gets a realistic showcase view and a dedicated smoke view that puts `SideNav` and `SideNavRail` side by side so we can verify the label-wrap is visually neutral (spec §10, §5.3).

---

## Task 23: Demo module — showcase view

**Files:**
- Create: `demo/src/main/java/org/vaadin/addons/componentfactory/sidenavrail/demo/MainLayout.java`
- Create: `demo/src/main/java/org/vaadin/addons/componentfactory/sidenavrail/demo/views/ShowcaseView.java`

- [ ] **Step 1: Write the demo `MainLayout`**

Create the file with the full Apache 2.0 header, then:
```java
package org.vaadin.addons.componentfactory.sidenavrail.demo;

import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.router.Layout;
import org.vaadin.addons.componentfactory.sidenavrail.SideNavRail;
import org.vaadin.addons.componentfactory.sidenavrail.SideNavRailItem;

@Layout
public class MainLayout extends AppLayout {

    public MainLayout() {
        SideNavRail nav = new SideNavRail();

        SideNavRailItem dashboard =
                new SideNavRailItem("Dashboard", "/", VaadinIcon.DASHBOARD.create());

        SideNavRailItem code = new SideNavRailItem(
                "Code", "/code", VaadinIcon.CODE.create());
        code.addItem(new SideNavRailItem("Branches", "/code/branches"));
        code.addItem(new SideNavRailItem("Commits", "/code/commits"));
        code.addItem(new SideNavRailItem("Tags", "/code/tags"));

        SideNavRailItem operate = new SideNavRailItem(
                "Operate", "/operate", VaadinIcon.COGS.create());
        operate.addItem(new SideNavRailItem("Environments", "/operate/environments"));
        operate.addItem(new SideNavRailItem("Releases", "/operate/releases"));

        nav.addItem(dashboard, code, operate);

        Button toggle = new Button(VaadinIcon.CHEVRON_LEFT_SMALL.create(),
                e -> nav.setRailMode(!nav.isRailMode()));
        addToNavbar(toggle);
        addToDrawer(nav);
    }
}
```

- [ ] **Step 2: Write the showcase view**

Create `/workspace/demo/src/main/java/org/vaadin/addons/componentfactory/sidenavrail/demo/views/ShowcaseView.java` with the full Apache 2.0 header, then:
```java
package org.vaadin.addons.componentfactory.sidenavrail.demo.views;

import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

@Route("")
public class ShowcaseView extends VerticalLayout {
    public ShowcaseView() {
        add(new H1("SideNav Rail"));
        add(new Paragraph(
                "Toggle the rail button in the header. Hover over \"Code\" or \"Operate\" "
                        + "while in rail mode to see the popover."));
    }
}
```

- [ ] **Step 3: Boot the demo manually**

Run:
```bash
cd /workspace/demo
../mvnw spring-boot:run
```
Open `http://localhost:8080/` in a browser. Verify:
- Drawer nav with three items (Dashboard, Code, Operate).
- Toggle button in the header shrinks the drawer to rail width.
- Hover over Code or Operate reveals the popover.

Ctrl+C to stop.

- [ ] **Step 4: Commit**

```bash
git add demo/src/main/java/
git commit -m "feat(demo): add showcase view and app layout"
```

---

## Task 24: Label-wrap rendering neutrality smoke test

**Files:**
- Create: `demo/src/main/java/org/vaadin/addons/componentfactory/sidenavrail/demo/views/LabelWrapSmokeView.java`

Spec §10 flags a manual verification step: confirm the `<span class="label">` wrap produces visually identical rendering to a standard `SideNavItem`. We add a demo view that puts a `SideNavRail` and a plain `SideNav` side by side with the same items.

- [ ] **Step 1: Write the smoke view**

Create the file with the full Apache 2.0 header, then:
```java
package org.vaadin.addons.componentfactory.sidenavrail.demo.views;

import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.router.Route;
import org.vaadin.addons.componentfactory.sidenavrail.SideNavRail;
import org.vaadin.addons.componentfactory.sidenavrail.SideNavRailItem;

/**
 * Compare the rendering of a standard {@link SideNav} with {@link SideNavRail} side by
 * side — the text in both should be pixel-identical.
 */
@Route("smoke/label-wrap")
public class LabelWrapSmokeView extends HorizontalLayout {

    public LabelWrapSmokeView() {
        setPadding(true);
        setSpacing(true);

        VerticalLayout left = new VerticalLayout(new H2("SideNav (standard)"), standardSideNav());
        VerticalLayout right = new VerticalLayout(new H2("SideNavRail"), sideNavRail());

        add(left, right);
    }

    private SideNav standardSideNav() {
        SideNav nav = new SideNav();
        nav.addItem(new SideNavItem("Dashboard", "/", VaadinIcon.DASHBOARD.create()));
        nav.addItem(new SideNavItem("Code", "/code", VaadinIcon.CODE.create()));
        return nav;
    }

    private SideNavRail sideNavRail() {
        SideNavRail nav = new SideNavRail();
        nav.addItem(new SideNavRailItem("Dashboard", "/", VaadinIcon.DASHBOARD.create()));
        nav.addItem(new SideNavRailItem("Code", "/code", VaadinIcon.CODE.create()));
        return nav;
    }
}
```

- [ ] **Step 2: Boot the demo and compare visually**

Run:
```bash
cd /workspace/demo
../mvnw spring-boot:run
```
Open `http://localhost:8080/smoke/label-wrap`. Inspect the two navs side by side. The text size, weight, colour, spacing, and line height of each label must be indistinguishable.

If a difference is visible, the `<span>` wrap introduced a regression — most likely a `display: inline-block` or default browser style. Add `display: inline` to a `.label` rule in `side-nav-rail.css` as an explicit reset and re-verify.

- [ ] **Step 3: Optional — screenshot for the design record**

If the visual check passes, take a screenshot and save it to `docs/design-verification/2026-04-21-label-wrap.png` (create the folder if missing). Useful as a Phase B / README asset later.

- [ ] **Step 4: Commit**

```bash
git add demo/src/main/java/
git commit -m "test: add label-wrap smoke view for visual verification"
```

---

## Phase 7 complete when

- `http://localhost:8080/` shows the showcase drawer-nav with working toggle and popovers.
- `http://localhost:8080/smoke/label-wrap` shows two navs rendering identically.
- Two green commits added in Phase 7.

---

## MVP complete when

All seven phases done and `./mvnw verify` is green from a clean checkout. At this point:

- The addon has its public API (`SideNavRail`, `SideNavRailItem`, `PopoverMode`, `RailModeChangedEvent`).
- Rail mode toggles via CSS, labels are wrapped for selectability, popovers gate correctly by mode and state.
- 16+ green commits in the log.
- The next iteration (Phase B — publishing prep) starts from this baseline: README, addon metadata, CI/release pipeline.
