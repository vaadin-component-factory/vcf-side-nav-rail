# Phase 6 — E2E tests

**Prereqs:** Phase 5 complete. Read [`../2026-04-21-side-nav-rail-plan.md`](../2026-04-21-side-nav-rail-plan.md) for cross-phase conventions.

**Output of this phase:** four Playwright `*.spec.ts` files covering the behaviour matrix from the spec, plus a green `./mvnw verify` — the go/no-go gate for the MVP.

**Local iteration recipe** (used by Tasks 18–21 for manual runs before committing):

Shell 1:
```bash
cd /workspace/addon
../mvnw spring-boot:start \
    -Dspring-boot.run.mainClass=org.vaadin.addons.componentfactory.sidenavrail.app.TestApplication
```
Shell 2:
```bash
cd /workspace/addon/src/test/playwright
npx playwright test <spec-file>.spec.ts
```
After: in Shell 1, `../mvnw spring-boot:stop`.

---

## Task 18: Playwright — basic smoke test

**Files:**
- Create: `addon/src/test/playwright/tests/basic.spec.ts`

- [ ] **Step 1: Write the test**

Create `/workspace/addon/src/test/playwright/tests/basic.spec.ts`:
```ts
import { test, expect } from '@playwright/test';

test.describe('basic rail mode', () => {
  test('rail renders and toggles between full and rail mode', async ({ page }) => {
    await page.goto('/basic');

    const nav = page.locator('vaadin-side-nav').first();
    await expect(nav).toBeVisible();
    await expect(nav).not.toHaveAttribute('theme', /rail/);

    await page.locator('#toggle-rail').click();
    await expect(nav).toHaveAttribute('theme', /rail/);

    // In rail mode: labels hidden, icons still there
    const labels = nav.locator('vaadin-side-nav-item span.label');
    for (const label of await labels.all()) {
      await expect(label).toBeHidden();
    }

    await page.locator('#toggle-rail').click();
    await expect(nav).not.toHaveAttribute('theme', /rail/);
  });
});
```

- [ ] **Step 2: Run it locally (per recipe above)**

Expected: PASS.

- [ ] **Step 3: Commit**

```bash
git add addon/src/test/playwright/tests/basic.spec.ts
git commit -m "test(e2e): basic rail render + toggle"
```

---

## Task 19: Playwright — popover in `COLLAPSED_ITEM` mode

**Files:**
- Create: `addon/src/test/playwright/tests/popover-collapsed-item.spec.ts`

- [ ] **Step 1: Write the test**

Create `/workspace/addon/src/test/playwright/tests/popover-collapsed-item.spec.ts`:
```ts
import { test, expect } from '@playwright/test';

test.describe('popover in COLLAPSED_ITEM mode', () => {
  test('popover opens on hover over inline-closed parent in normal mode', async ({ page }) => {
    await page.goto('/collapsed-item');

    const parent = page.locator('#rail vaadin-side-nav-item').first();
    await parent.hover();

    const popover = page.locator('vaadin-popover-overlay[opened]');
    await expect(popover).toBeVisible({ timeout: 2_000 });

    await expect(popover.locator('vaadin-side-nav-item')).toHaveCount(2);
  });

  test('popover also opens in rail mode', async ({ page }) => {
    await page.goto('/collapsed-item');

    await page.locator('#toggle-rail').click();

    const parent = page.locator('#rail vaadin-side-nav-item').first();
    await parent.hover();

    const popover = page.locator('vaadin-popover-overlay[opened]');
    await expect(popover).toBeVisible({ timeout: 2_000 });
  });
});
```

- [ ] **Step 2: Run it locally**

Expected: both tests pass.

- [ ] **Step 3: Commit**

```bash
git add addon/src/test/playwright/tests/popover-collapsed-item.spec.ts
git commit -m "test(e2e): COLLAPSED_ITEM mode popover behaviour"
```

---

## Task 20: Playwright — popover in `RAIL_ONLY` mode

**Files:**
- Create: `addon/src/test/playwright/tests/popover-rail-only.spec.ts`

- [ ] **Step 1: Write the test**

Create `/workspace/addon/src/test/playwright/tests/popover-rail-only.spec.ts`:
```ts
import { test, expect } from '@playwright/test';

test.describe('popover in RAIL_ONLY mode', () => {
  test('no popover on inline-closed parent while nav is in normal mode', async ({ page }) => {
    await page.goto('/rail-only');

    const parent = page.locator('#rail vaadin-side-nav-item').first();
    await parent.hover();

    const popover = page.locator('vaadin-popover-overlay[opened]');
    await expect(popover).not.toBeVisible({ timeout: 1_000 });
  });

  test('popover appears once rail mode is engaged', async ({ page }) => {
    await page.goto('/rail-only');

    await page.locator('#toggle-rail').click();

    const parent = page.locator('#rail vaadin-side-nav-item').first();
    await parent.hover();

    const popover = page.locator('vaadin-popover-overlay[opened]');
    await expect(popover).toBeVisible({ timeout: 2_000 });
  });

  test('disengaging rail mode silences the popover again', async ({ page }) => {
    await page.goto('/rail-only');

    await page.locator('#toggle-rail').click();
    await page.locator('#toggle-rail').click();

    const parent = page.locator('#rail vaadin-side-nav-item').first();
    await parent.hover();

    const popover = page.locator('vaadin-popover-overlay[opened]');
    await expect(popover).not.toBeVisible({ timeout: 1_000 });
  });
});
```

- [ ] **Step 2: Run it locally**

Expected: all three pass.

- [ ] **Step 3: Commit**

```bash
git add addon/src/test/playwright/tests/popover-rail-only.spec.ts
git commit -m "test(e2e): RAIL_ONLY mode popover gating"
```

---

## Task 21: Playwright — nested expansion inside popover

**Files:**
- Create: `addon/src/test/playwright/tests/nested-popover.spec.ts`

- [ ] **Step 1: Write the test**

Create `/workspace/addon/src/test/playwright/tests/nested-popover.spec.ts`:
```ts
import { test, expect } from '@playwright/test';

test.describe('nested expansion inside popover', () => {
  test('items with own children can inline-expand inside the popover', async ({ page }) => {
    await page.goto('/nested');

    const parent = page.locator('#rail vaadin-side-nav-item').first();
    await parent.hover();

    const popover = page.locator('vaadin-popover-overlay[opened]');
    await expect(popover).toBeVisible({ timeout: 2_000 });

    // Two direct children inside the popover (Branches, Tags)
    const innerItems = popover.locator('vaadin-side-nav-item');
    await expect(innerItems).toHaveCount(2);

    // "Branches" has a toggle; clicking it reveals its grandchildren
    const branches = innerItems.filter({ hasText: 'Branches' }).first();
    await branches.locator('::part(toggle-button)').click();

    // After expansion, total items visible inside popover is 4 (Branches + Active + Stale + Tags)
    await expect(innerItems).toHaveCount(4);
  });

  test('only one popover is open at a time', async ({ page }) => {
    await page.goto('/nested');

    const parent = page.locator('#rail vaadin-side-nav-item').first();
    await parent.hover();

    // Nested popover never opens — only the one rooted at the rail item
    await expect(page.locator('vaadin-popover-overlay[opened]')).toHaveCount(1);
  });
});
```

- [ ] **Step 2: Run it locally**

Expected: both pass.

- [ ] **Step 3: Commit**

```bash
git add addon/src/test/playwright/tests/nested-popover.spec.ts
git commit -m "test(e2e): nested expansion inside popover"
```

---

## Task 22: Full `./mvnw verify` run — MVP gate

**Files:** none (verification only).

- [ ] **Step 1: Run the full pipeline**

Run from `/workspace`:
```bash
./mvnw verify
```

Expected phases:
1. `compile` — addon + demo compile.
2. `test` — unit tests in `unit/` pass.
3. `pre-integration-test` — Spring Boot test app starts on port 8081; `frontend-maven-plugin` installs Node + npm deps + Chromium.
4. `integration-test` — `npx playwright test` runs all four spec files, all pass.
5. `post-integration-test` — Spring Boot test app stops.
6. `verify` — overall `BUILD SUCCESS`.

- [ ] **Step 2: If anything fails, diagnose from the bottom up**

- Unit test failure → fix the regression, re-run `./mvnw -pl addon test`.
- `frontend-maven-plugin` `npm ci` failure → check `src/test/playwright/package-lock.json` exists (Task 16 Step 4 committed it).
- Spring Boot startup failure → check `target/spring-boot-*.log` (the plugin writes a startup log).
- Playwright failure → inspect `addon/src/test/playwright/test-results/` for traces and screenshots.

- [ ] **Step 3: No commit needed for a green pass** — nothing changes on disk on a successful run. The green state is the signal.

---

## Phase 6 complete when

- `./mvnw verify` is green from a clean checkout (`./mvnw clean verify` also green).
- Four Playwright spec files are committed and covered by the lifecycle.
- Four green commits added in Phase 6 (three for the spec files, one implied for the green MVP gate).

Next: [Phase 7 — Demo](./07-demo.md).
