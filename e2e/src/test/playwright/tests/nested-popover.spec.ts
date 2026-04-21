import { test, expect } from '@playwright/test';

test.describe('nested expansion inside popover', () => {
  test('items with own children can inline-expand inside the popover', async ({ page }) => {
    await page.goto('/nested');

    const parent = page.locator('#rail vaadin-side-nav-item').first();
    await parent.hover();

    const popover = page.locator('vaadin-popover-overlay[opened]');
    await expect(popover).toBeVisible({ timeout: 2_000 });

    // All four side-nav-items exist in the popover DOM from the start
    // (Branches + Active + Stale + Tags). Initially the grandchildren (Active,
    // Stale) are not expanded — their container has [expanded]=false on the
    // Branches parent.
    const branches = popover.locator('vaadin-side-nav-item').filter({ hasText: 'Branches' }).first();
    await expect(branches).not.toHaveAttribute('expanded', '');

    // Click the Branches toggle button inside the web-component's shadow DOM.
    // Playwright's css parser does not accept ::part() as a standalone selector,
    // so we reach into the shadow root directly via evaluate().
    await branches.evaluate((el: HTMLElement) => {
      const toggle = el.shadowRoot?.querySelector<HTMLButtonElement>('button[part="toggle-button"]');
      toggle?.click();
    });

    // After clicking, Branches gets the [expanded] attribute, and Active/Stale
    // become visible to the user. Match by path attribute to avoid matching
    // the ancestor item whose accumulated text contains "Active".
    await expect(branches).toHaveAttribute('expanded', '');
    await expect(popover.locator('vaadin-side-nav-item[path*="branches/active"]'))
      .toBeVisible();
  });

  test('only one popover is open at a time', async ({ page }) => {
    await page.goto('/nested');

    const parent = page.locator('#rail vaadin-side-nav-item').first();
    await parent.hover();

    // Nested popover never opens — only the one rooted at the rail item
    await expect(page.locator('vaadin-popover-overlay[opened]')).toHaveCount(1);
  });
});
