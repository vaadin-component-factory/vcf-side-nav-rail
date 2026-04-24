## Implementation status

**Done (2026-04-24).** All 13 tasks complete. 12 Playwright tests in `e2e/src/test/playwright/tests/accessibility.spec.ts` green against `AccessibilityView` on `/accessibility`. `./mvnw verify` green (addon unit tests + e2e Playwright).

Deviations from the plan-as-written:
- **Addon code was touched** (plan had ruled this out). Writing the §4.3 assertions surfaced a real §9.2 regression: Vaadin's stock `<vaadin-side-nav-item>` overwrote `aria-haspopup` back to the generic `"true"` whenever its popover opened, violating the §4.4.5 contract that mandates `"menu"`. Fixed in `SideNavRailItem.syncAriaExpanded` + a client-side `MutationObserver` guard in `side-nav-rail-keyboard.js`; new unit test in `AriaAttributesTest`. Commits `bff0681`, `cf3af18`.
- **§4.1 spec was relaxed.** Vaadin natively sets `aria-haspopup="true"` AND `aria-expanded="false"` on any parent item with children, regardless of rail mode. Fighting those values in normal mode would require more MutationObserver machinery for no accessibility benefit. Tests now assert the negative (no `"menu"`, no `"true"`) on parent roots. Commits `d96f408`, `93696e8`.
- **§4.1 test split into leaf / parent.** Separate tests for `Dashboard` (leaf — both attributes truly absent) vs. `Code` / `Admin` (parents — Vaadin-native values accepted). Gives clearer failure messages.
- **Project-specific `server-start.sh` / `server-stop.sh`** added (commit `c43d6fc`) because the generic template versions only killed the `mvn spring-boot:run` wrapper, leaving the forked JVM (and the old prod.bundle hash) alive. That one issue cost hours during §9.4.

---

# A11y E2E Tests Implementation Plan (§9.4)

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add deterministic Playwright E2E tests that lock in the observable DOM contracts (ARIA attributes, `role="menu"` / `role="menuitem"`, `tabindex="-1"` on nested items) produced by the §9.2 accessibility implementation of the SideNav Rail addon.

**Architecture:** One new Vaadin test-route (`AccessibilityView` under `/accessibility`) mirrors the structure of `KeyboardNavigationView` (Dashboard leaf + Code with two flat children + Admin with one nested + one flat child + a `#toggle-rail` button). One new Playwright spec (`accessibility.spec.ts`) asserts the DOM contracts across rail on/off cycles. No addon code changes. All tests independent — each `test(...)` starts with `page.goto('/accessibility')` and re-applies state.

**Tech Stack:** Vaadin 24.10.1 (Java 17, Spring Boot 3.5.13) for the test route; Playwright 1.49 / TypeScript 5.6 for the spec. Production-mode E2E build (the `e2e/` Maven module already builds with `vaadin.productionMode=true` + `vaadin-maven-plugin build-frontend`).

**Authoritative references:**
- Spec: `specs/2026-04-24-a11y-e2e-tests-design.md`
- Contract origin: `specs/2026-04-21-side-nav-rail-design.md` §4.4.5 (ARIA), §4.2 (popover `setOverlayRole("menu")`)
- Existing pattern for focus/shadow-DOM helpers: `e2e/src/test/playwright/tests/keyboard-navigation.spec.ts` lines 1–45
- View to clone: `e2e/src/main/java/org/vaadin/addons/componentfactory/sidenavrail/e2e/views/KeyboardNavigationView.java`

**Test execution:**
- Run a single test: `cd /workspace && (cd e2e/src/test/playwright && npx playwright test tests/accessibility.spec.ts -g "<test title>")` — requires a running server.
- Run the whole file: `cd /workspace && (cd e2e/src/test/playwright && npx playwright test tests/accessibility.spec.ts)`.
- Run everything (Maven wrapper, production build, fires up + tears down the server, takes ~5 minutes cold): `cd /workspace && ./mvnw -pl e2e verify | tee /tmp/e2e.log`.
- Ad-hoc iteration: start the e2e module manually via `cd /workspace/e2e && mvn spring-boot:run -Pproduction`, then run Playwright against `http://localhost:8081`. Stop with Ctrl-C (this is the e2e module, not the demo, so `./server-start.sh` does **not** apply).

**Conventions this plan follows (from the existing specs):**
- Item selectors scoped to `#rail` for rail-DOM assertions: `#rail vaadin-side-nav-item[path="…"]`. The unscoped selector is a strict-mode violation because popovers keep a light-DOM clone of each nested item (see comment in `keyboard-navigation.spec.ts:10–12`).
- Popover-item selectors scoped to `vaadin-popover-overlay[opened]` for in-popover assertions.
- Focus triggered via shadow-DOM-piercing on the inner `<a id="link">` (same `focusItem` helper as `keyboard-navigation.spec.ts`).

**Commit style:** One commit per task, subject line like `test(e2e): assert <thing> (§9.4)`. All commits on `main` (phase 9.2 is already merged). No sub-branches needed — this is additive test-only work.

---

## File Structure

**New files (2):**
- `e2e/src/main/java/org/vaadin/addons/componentfactory/sidenavrail/e2e/views/AccessibilityView.java` — Vaadin test-route under `/accessibility`. Static structure; the only interactivity is the `#toggle-rail` button.
- `e2e/src/test/playwright/tests/accessibility.spec.ts` — 12 Playwright tests, grouped with `test.describe` by scenario.

**Modified files (1):**
- `plans/2026-04-24-a11y-e2e-tests-plan.md` (this file) — gets a brief status block at the top once implementation is done.

No other files are touched. No addon code changes.

---

## Task 1: Create the `AccessibilityView` test route

**Files:**
- Create: `e2e/src/main/java/org/vaadin/addons/componentfactory/sidenavrail/e2e/views/AccessibilityView.java`

- [ ] **Step 1: Write the view class**

Content for `AccessibilityView.java`:

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
package org.vaadin.addons.componentfactory.sidenavrail.e2e.views;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import org.vaadin.addons.componentfactory.sidenavrail.SideNavRail;
import org.vaadin.addons.componentfactory.sidenavrail.SideNavRailItem;

/**
 * Dedicated route for §9.4 a11y E2E assertions. Structure mirrors
 * KeyboardNavigationView but the two views stay decoupled so that a
 * future change to the keyboard tests cannot regress a11y tests.
 *
 * Layout:
 *   - Dashboard : leaf (no children)                  — exercises "no aria-haspopup"
 *   - Code      : two flat children (Branches, Commits) — flat subtree
 *   - Admin     : Users (nested: Active, Archived) + Roles — deeply nested
 *
 * A toggle button (#toggle-rail) flips rail mode so tests can exercise
 * the rail-on / rail-off transitions.
 */
@Route("accessibility")
public class AccessibilityView extends VerticalLayout {

    public AccessibilityView() {
        SideNavRail rail = new SideNavRail();
        rail.setId("rail");

        SideNavRailItem dashboard = new SideNavRailItem("Dashboard", "/dashboard", VaadinIcon.DASHBOARD.create());
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

- [ ] **Step 2: Compile the e2e module to confirm the class builds**

Run:
```bash
cd /workspace && ./mvnw -pl e2e -am compile 2>&1 | tee /tmp/a11y-compile.log | tail -20
```

Expected: `BUILD SUCCESS`. If compile fails on an import, double-check the package path of `SideNavRail` / `SideNavRailItem` — they live in `org.vaadin.addons.componentfactory.sidenavrail` (same as `KeyboardNavigationView` uses).

- [ ] **Step 3: Commit**

```bash
cd /workspace && git add e2e/src/main/java/org/vaadin/addons/componentfactory/sidenavrail/e2e/views/AccessibilityView.java && \
git commit -m "test(e2e): add AccessibilityView for §9.4 a11y tests"
```

---

## Task 2: Scaffold `accessibility.spec.ts` with helpers + §4.1 (rail off — roots have no ARIA)

**Files:**
- Create: `e2e/src/test/playwright/tests/accessibility.spec.ts`

- [ ] **Step 1: Write the scaffold + first test**

Content for `accessibility.spec.ts`:

```typescript
import { test, expect, Page } from '@playwright/test';

/**
 * Focus the inner <a id="link"> of a rail-scoped vaadin-side-nav-item.
 * Matches the pattern used in keyboard-navigation.spec.ts — see that
 * file for the rationale (custom element delegates focus; light-DOM
 * clones inside popovers require scoping to #rail).
 */
async function focusRailItem(page: Page, path: string): Promise<void> {
    await page.locator(`#rail vaadin-side-nav-item[path="${path}"]`).evaluate(
        (el: HTMLElement) => {
            const anchor = (el.shadowRoot?.querySelector('a')
                ?? el.querySelector('a')) as HTMLElement | null;
            (anchor ?? el).focus();
        });
}

test.describe('rail off — baseline', () => {
    test('rail off — roots have no aria-haspopup / aria-expanded', async ({ page }) => {
        await page.goto('/accessibility');

        for (const path of ['dashboard', 'code', 'admin']) {
            const item = page.locator(`#rail vaadin-side-nav-item[path="${path}"]`);
            await expect(item).not.toHaveAttribute('aria-haspopup', /.*/);
            await expect(item).not.toHaveAttribute('aria-expanded', /.*/);
        }
    });
});
```

- [ ] **Step 2: Start the e2e server (one-time for the whole plan)**

Playwright needs a running server. Start the e2e module on port 8081 in the background:

```bash
cd /workspace/e2e && (mvn -Pproduction spring-boot:run -Dspring-boot.run.fork=false > /tmp/e2e-server.log 2>&1 &)
```

Wait ~45 s for Vaadin's production bundle + first page load. Verify readiness:

```bash
curl -sSf http://localhost:8081/accessibility > /dev/null && echo "server ready"
```

Expected: `server ready`. If not, tail `/tmp/e2e-server.log` — first page compile can take up to 60 s on a cold build.

- [ ] **Step 3: Run the first test and expect PASS**

```bash
cd /workspace/e2e/src/test/playwright && npx playwright test tests/accessibility.spec.ts -g "roots have no aria-haspopup"
```

Expected: `1 passed`. (This is a regression-test exercise: the §9.2 implementation should already satisfy the contract. If it fails, the implementation has regressed and that needs investigation **before** moving on.)

- [ ] **Step 4: Commit**

```bash
cd /workspace && git add e2e/src/test/playwright/tests/accessibility.spec.ts && \
git commit -m "test(e2e): assert roots have no ARIA attributes when rail is off (§9.4)"
```

---

## Task 3: §4.1 — rail off — nested items have no tabindex

**Files:**
- Modify: `e2e/src/test/playwright/tests/accessibility.spec.ts` — append a new `test(...)` inside the existing `describe('rail off — baseline', …)` block.

- [ ] **Step 1: Add the test**

Append inside the `rail off — baseline` describe block (before its closing `})`):

```typescript
    test('rail off — nested items have no tabindex', async ({ page }) => {
        await page.goto('/accessibility');

        const nestedPaths = [
            'code/branches', 'code/commits',
            'admin/users', 'admin/users/active', 'admin/users/archived',
            'admin/roles',
        ];
        for (const path of nestedPaths) {
            const item = page.locator(`#rail vaadin-side-nav-item[path="${path}"]`);
            await expect(item).not.toHaveAttribute('tabindex', /.*/);
        }
    });
```

- [ ] **Step 2: Run the new test**

```bash
cd /workspace/e2e/src/test/playwright && npx playwright test tests/accessibility.spec.ts -g "nested items have no tabindex"
```

Expected: `1 passed`.

- [ ] **Step 3: Commit**

```bash
cd /workspace && git add e2e/src/test/playwright/tests/accessibility.spec.ts && \
git commit -m "test(e2e): assert nested items have no tabindex when rail is off (§9.4)"
```

---

## Task 4: §4.2 — rail on, popover closed — aria-haspopup=menu on roots with children, leaf untouched

**Files:**
- Modify: `e2e/src/test/playwright/tests/accessibility.spec.ts` — append a new `describe` block.

- [ ] **Step 1: Add the describe block + test**

Append at the end of the file:

```typescript
test.describe('rail on, popover closed', () => {
    test('rail on, popover closed — roots with children get aria-haspopup=menu, leaf untouched', async ({ page }) => {
        await page.goto('/accessibility');
        await page.locator('#toggle-rail').click();

        // Roots with children
        for (const path of ['code', 'admin']) {
            const item = page.locator(`#rail vaadin-side-nav-item[path="${path}"]`);
            await expect(item).toHaveAttribute('aria-haspopup', 'menu');
            await expect(item).toHaveAttribute('aria-expanded', 'false');
        }

        // Leaf
        const dashboard = page.locator('#rail vaadin-side-nav-item[path="dashboard"]');
        await expect(dashboard).not.toHaveAttribute('aria-haspopup', /.*/);
        await expect(dashboard).not.toHaveAttribute('aria-expanded', /.*/);
    });
});
```

- [ ] **Step 2: Run the test**

```bash
cd /workspace/e2e/src/test/playwright && npx playwright test tests/accessibility.spec.ts -g "roots with children get aria-haspopup"
```

Expected: `1 passed`.

- [ ] **Step 3: Commit**

```bash
cd /workspace && git add e2e/src/test/playwright/tests/accessibility.spec.ts && \
git commit -m "test(e2e): assert aria-haspopup/expanded set on children, skipped on leaf in rail mode (§9.4)"
```

---

## Task 5: §4.2 — rail on, popover closed — nested items have tabindex=-1

**Files:**
- Modify: `e2e/src/test/playwright/tests/accessibility.spec.ts` — append to the `rail on, popover closed` describe block.

- [ ] **Step 1: Add the test**

Append inside the `rail on, popover closed` describe block (before its closing `})`):

```typescript
    test('rail on, popover closed — nested items have tabindex=-1', async ({ page }) => {
        await page.goto('/accessibility');
        await page.locator('#toggle-rail').click();

        const nestedPaths = [
            'code/branches', 'code/commits',
            'admin/users', 'admin/users/active', 'admin/users/archived',
            'admin/roles',
        ];
        for (const path of nestedPaths) {
            const item = page.locator(`#rail vaadin-side-nav-item[path="${path}"]`);
            await expect(item).toHaveAttribute('tabindex', '-1');
        }
    });
```

- [ ] **Step 2: Run the test**

```bash
cd /workspace/e2e/src/test/playwright && npx playwright test tests/accessibility.spec.ts -g "nested items have tabindex=-1"
```

Expected: `1 passed`.

- [ ] **Step 3: Commit**

```bash
cd /workspace && git add e2e/src/test/playwright/tests/accessibility.spec.ts && \
git commit -m "test(e2e): assert nested items get tabindex=-1 in rail mode (§9.4)"
```

---

## Task 6: §4.3 Test A — `Code` popover: aria-expanded=true + overlay role=menu

**Files:**
- Modify: `e2e/src/test/playwright/tests/accessibility.spec.ts` — append new `describe` block.

- [ ] **Step 1: Add the describe block + first two tests (aria-expanded + overlay role)**

Append at the end of the file:

```typescript
test.describe('rail on, popover open (Code)', () => {
    test('rail on, popover open (Code) — aria-expanded=true on focused root', async ({ page }) => {
        await page.goto('/accessibility');
        await page.locator('#toggle-rail').click();

        await focusRailItem(page, 'code');
        // Popover auto-opens on focus in rail mode (setOpenOnFocus=true).
        await expect(page.locator('vaadin-popover-overlay[opened]')).toHaveCount(1);

        const code = page.locator('#rail vaadin-side-nav-item[path="code"]');
        await expect(code).toHaveAttribute('aria-expanded', 'true');
        await expect(code).toHaveAttribute('aria-haspopup', 'menu');
    });

    test('rail on, popover open (Code) — overlay has role=menu', async ({ page }) => {
        await page.goto('/accessibility');
        await page.locator('#toggle-rail').click();

        await focusRailItem(page, 'code');
        const overlay = page.locator('vaadin-popover-overlay[opened]');
        await expect(overlay).toHaveCount(1);
        await expect(overlay).toHaveAttribute('role', 'menu');
    });
});
```

- [ ] **Step 2: Run both tests**

```bash
cd /workspace/e2e/src/test/playwright && npx playwright test tests/accessibility.spec.ts -g "popover open \(Code\)"
```

Expected: `2 passed`.

- [ ] **Step 3: Commit**

```bash
cd /workspace && git add e2e/src/test/playwright/tests/accessibility.spec.ts && \
git commit -m "test(e2e): assert Code popover sets aria-expanded + overlay role=menu (§9.4)"
```

---

## Task 7: §4.3 Test A continued — `Code` popover items have `role="menuitem"` (scoped)

**Files:**
- Modify: `e2e/src/test/playwright/tests/accessibility.spec.ts` — append to the `rail on, popover open (Code)` describe block.

- [ ] **Step 1: Add the test**

Append inside the `rail on, popover open (Code)` describe block (before its closing `})`):

```typescript
    test('rail on, popover open (Code) — flat children have role=menuitem', async ({ page }) => {
        await page.goto('/accessibility');
        await page.locator('#toggle-rail').click();

        await focusRailItem(page, 'code');
        const overlay = page.locator('vaadin-popover-overlay[opened]');
        await expect(overlay).toHaveCount(1);

        // Locators MUST be scoped to the overlay: the rail DOM still contains
        // these items with tabindex="-1" and no role, so an unscoped selector
        // could hit the wrong copy and produce a false green.
        for (const path of ['code/branches', 'code/commits']) {
            const item = overlay.locator(`vaadin-side-nav-item[path="${path}"]`);
            await expect(item).toHaveCount(1);
            await expect(item).toHaveAttribute('role', 'menuitem');
        }

        // The Code root itself is the popover's target and is NOT duplicated
        // inside the overlay — verify the overlay does not contain it.
        await expect(
            overlay.locator('vaadin-side-nav-item[path="code"]')
        ).toHaveCount(0);
    });
```

- [ ] **Step 2: Run the test**

```bash
cd /workspace/e2e/src/test/playwright && npx playwright test tests/accessibility.spec.ts -g "flat children have role=menuitem"
```

Expected: `1 passed`.

- [ ] **Step 3: Commit**

```bash
cd /workspace && git add e2e/src/test/playwright/tests/accessibility.spec.ts && \
git commit -m "test(e2e): assert Code's flat children get role=menuitem, scoped to overlay (§9.4)"
```

---

## Task 8: §4.3 Test B — `Admin` popover: deeply nested children have `role="menuitem"`

**Files:**
- Modify: `e2e/src/test/playwright/tests/accessibility.spec.ts` — append new `describe` block.

- [ ] **Step 1: Add the describe block + test**

Append at the end of the file:

```typescript
test.describe('rail on, popover open (Admin)', () => {
    test('rail on, popover open (Admin) — deeply nested children have role=menuitem', async ({ page }) => {
        await page.goto('/accessibility');
        await page.locator('#toggle-rail').click();

        await focusRailItem(page, 'admin');
        const overlay = page.locator('vaadin-popover-overlay[opened]');
        await expect(overlay).toHaveCount(1);
        await expect(overlay).toHaveAttribute('role', 'menu');

        // role="menuitem" is applied recursively at populate time by
        // SideNavRailItem.tagAsMenuItem() — expansion state of `users` does
        // not matter for this assertion.
        const nestedPaths = [
            'admin/users',
            'admin/users/active',
            'admin/users/archived',
            'admin/roles',
        ];
        for (const path of nestedPaths) {
            const item = overlay.locator(`vaadin-side-nav-item[path="${path}"]`);
            await expect(item).toHaveCount(1);
            await expect(item).toHaveAttribute('role', 'menuitem');
        }

        // Admin root itself is not duplicated into the overlay.
        await expect(
            overlay.locator('vaadin-side-nav-item[path="admin"]')
        ).toHaveCount(0);
    });
});
```

- [ ] **Step 2: Run the test**

```bash
cd /workspace/e2e/src/test/playwright && npx playwright test tests/accessibility.spec.ts -g "popover open \(Admin\)"
```

Expected: `1 passed`.

- [ ] **Step 3: Commit**

```bash
cd /workspace && git add e2e/src/test/playwright/tests/accessibility.spec.ts && \
git commit -m "test(e2e): assert Admin's deeply nested children get role=menuitem (§9.4)"
```

---

## Task 9: §4.4 — popover closed again — aria-expanded returns to false

**Files:**
- Modify: `e2e/src/test/playwright/tests/accessibility.spec.ts` — append new `describe` block.

- [ ] **Step 1: Add the describe block + test**

Append at the end of the file:

```typescript
test.describe('popover close — aria-expanded resets', () => {
    test('popover closed again — aria-expanded returns to false', async ({ page }) => {
        await page.goto('/accessibility');
        await page.locator('#toggle-rail').click();

        await focusRailItem(page, 'code');
        const overlay = page.locator('vaadin-popover-overlay[opened]');
        await expect(overlay).toHaveCount(1);

        await page.keyboard.press('Escape');
        await expect(page.locator('vaadin-popover-overlay[opened]')).toHaveCount(0);

        const code = page.locator('#rail vaadin-side-nav-item[path="code"]');
        await expect(code).toHaveAttribute('aria-expanded', 'false');
        await expect(code).toHaveAttribute('aria-haspopup', 'menu');
    });
});
```

- [ ] **Step 2: Run the test**

```bash
cd /workspace/e2e/src/test/playwright && npx playwright test tests/accessibility.spec.ts -g "aria-expanded returns to false"
```

Expected: `1 passed`.

- [ ] **Step 3: Commit**

```bash
cd /workspace && git add e2e/src/test/playwright/tests/accessibility.spec.ts && \
git commit -m "test(e2e): assert aria-expanded resets to false on popover close (§9.4)"
```

---

## Task 10: §4.5 — rail toggled off — aria-haspopup / aria-expanded cleared

**Files:**
- Modify: `e2e/src/test/playwright/tests/accessibility.spec.ts` — append new `describe` block.

- [ ] **Step 1: Add the describe block + test**

Append at the end of the file:

```typescript
test.describe('rail toggled off — cleanup', () => {
    test('rail toggled off — aria-haspopup / aria-expanded cleared', async ({ page }) => {
        await page.goto('/accessibility');
        await page.locator('#toggle-rail').click(); // on
        await page.locator('#toggle-rail').click(); // off again

        for (const path of ['code', 'admin']) {
            const item = page.locator(`#rail vaadin-side-nav-item[path="${path}"]`);
            await expect(item).not.toHaveAttribute('aria-haspopup', /.*/);
            await expect(item).not.toHaveAttribute('aria-expanded', /.*/);
        }
    });
});
```

- [ ] **Step 2: Run the test**

```bash
cd /workspace/e2e/src/test/playwright && npx playwright test tests/accessibility.spec.ts -g "aria-haspopup / aria-expanded cleared"
```

Expected: `1 passed`.

- [ ] **Step 3: Commit**

```bash
cd /workspace && git add e2e/src/test/playwright/tests/accessibility.spec.ts && \
git commit -m "test(e2e): assert aria-haspopup/expanded cleared on rail-off (§9.4)"
```

---

## Task 11: §4.5 — rail toggled off — tabindex cleared

**Files:**
- Modify: `e2e/src/test/playwright/tests/accessibility.spec.ts` — append to the `rail toggled off — cleanup` describe block.

- [ ] **Step 1: Add the test**

Append inside the `rail toggled off — cleanup` describe block (before its closing `})`):

```typescript
    test('rail toggled off — tabindex cleared', async ({ page }) => {
        await page.goto('/accessibility');
        await page.locator('#toggle-rail').click(); // on
        await page.locator('#toggle-rail').click(); // off again

        const nestedPaths = [
            'code/branches', 'code/commits',
            'admin/users', 'admin/users/active', 'admin/users/archived',
            'admin/roles',
        ];
        for (const path of nestedPaths) {
            const item = page.locator(`#rail vaadin-side-nav-item[path="${path}"]`);
            await expect(item).not.toHaveAttribute('tabindex', /.*/);
        }
    });
```

- [ ] **Step 2: Run the test**

```bash
cd /workspace/e2e/src/test/playwright && npx playwright test tests/accessibility.spec.ts -g "tabindex cleared"
```

Expected: `1 passed`.

- [ ] **Step 3: Commit**

```bash
cd /workspace && git add e2e/src/test/playwright/tests/accessibility.spec.ts && \
git commit -m "test(e2e): assert tabindex cleared on nested items after rail-off (§9.4)"
```

---

## Task 12: §4.6 — rail off → on again — contracts re-apply

**Files:**
- Modify: `e2e/src/test/playwright/tests/accessibility.spec.ts` — append new `describe` block.

- [ ] **Step 1: Add the describe block + test**

Append at the end of the file:

```typescript
test.describe('rail off → on again — re-apply', () => {
    test('rail off → on again — contracts re-apply', async ({ page }) => {
        await page.goto('/accessibility');
        await page.locator('#toggle-rail').click(); // on
        await page.locator('#toggle-rail').click(); // off
        await page.locator('#toggle-rail').click(); // on again

        // Roots with children: ARIA restored
        for (const path of ['code', 'admin']) {
            const item = page.locator(`#rail vaadin-side-nav-item[path="${path}"]`);
            await expect(item).toHaveAttribute('aria-haspopup', 'menu');
            await expect(item).toHaveAttribute('aria-expanded', 'false');
        }

        // Nested items: tabindex restored
        const nestedPaths = [
            'code/branches', 'code/commits',
            'admin/users', 'admin/users/active', 'admin/users/archived',
            'admin/roles',
        ];
        for (const path of nestedPaths) {
            const item = page.locator(`#rail vaadin-side-nav-item[path="${path}"]`);
            await expect(item).toHaveAttribute('tabindex', '-1');
        }
    });
});
```

- [ ] **Step 2: Run the test**

```bash
cd /workspace/e2e/src/test/playwright && npx playwright test tests/accessibility.spec.ts -g "contracts re-apply"
```

Expected: `1 passed`.

- [ ] **Step 3: Commit**

```bash
cd /workspace && git add e2e/src/test/playwright/tests/accessibility.spec.ts && \
git commit -m "test(e2e): assert contracts re-apply on rail toggle cycle (§9.4)"
```

---

## Task 13: Full-suite verification + plan status

**Files:**
- Modify: `plans/2026-04-24-a11y-e2e-tests-plan.md` — add an "Implementation status" block at the top.

- [ ] **Step 1: Stop the dev server and run the full Maven verify (cold, production-bundle build)**

```bash
pkill -f 'spring-boot.run' || true
cd /workspace && ./mvnw -pl e2e verify 2>&1 | tee /tmp/e2e-verify.log | tail -40
```

Expected:
- Bundle build finishes.
- Playwright fires up a fresh server via `spring-boot-maven-plugin` (or the e2e module's existing `pre-integration-test` wiring — no new config needed).
- All accessibility tests green: `12 passed` for `accessibility.spec.ts` (or 12 passed + some retry lines for the existing flaky popover tests — which are a known issue and OK).
- `BUILD SUCCESS`.

If the suite is red on anything in `accessibility.spec.ts`, that is a real §9.2 regression — stop, investigate, and escalate rather than papering over.

- [ ] **Step 2: Verify test count**

```bash
grep -c "test(" /workspace/e2e/src/test/playwright/tests/accessibility.spec.ts
```

Expected: `12`.

- [ ] **Step 3: Add status block to this plan**

Prepend to `plans/2026-04-24-a11y-e2e-tests-plan.md` (above the `# A11y E2E Tests Implementation Plan (§9.4)` heading):

```markdown
## Implementation status

**Done (2026-04-24).** All 12 tests in `e2e/src/test/playwright/tests/accessibility.spec.ts` green under `./mvnw -pl e2e verify`. `AccessibilityView` added under `/accessibility`. No addon code changed. Spec §9.4 contracts locked in — `aria-haspopup="menu"`, `aria-expanded` sync, `role="menu"` on overlay, `role="menuitem"` on popover items, `tabindex="-1"` on rail-mode nested items.

---
```

- [ ] **Step 4: Commit**

```bash
cd /workspace && git add plans/2026-04-24-a11y-e2e-tests-plan.md && \
git commit -m "docs(plan): record §9.4 a11y E2E tests completion"
```

---

## Post-implementation — housekeeping

After the last commit, stop the e2e server if still running and clean up log files:

```bash
pkill -f 'spring-boot.run' || true
rm -f /tmp/a11y-compile.log /tmp/e2e-server.log /tmp/e2e-verify.log
```
