import { test, expect } from '@playwright/test';
import { openPopover, popoverDescendant } from '../lib/popover';

test.describe('popover in ALL_COLLAPSED_ITEMS mode', () => {
  test('popover opens on hover over inline-closed parent in normal mode', async ({ page }) => {
    await page.goto('/all-collapsed-items');

    const parent = page.locator('#rail vaadin-side-nav-item').first();
    await parent.hover();

    await expect(openPopover(page)).toBeVisible({ timeout: 2_000 });

    await expect(page.locator(popoverDescendant('vaadin-side-nav-item'))).toHaveCount(2);
  });

  test('popover also opens in rail mode', async ({ page }) => {
    await page.goto('/all-collapsed-items');

    await page.locator('#toggle-rail').click();

    const parent = page.locator('#rail vaadin-side-nav-item').first();
    await parent.hover();

    await expect(openPopover(page)).toBeVisible({ timeout: 2_000 });
  });

  test('popover closes when the item is expanded inline in normal mode', async ({ page }) => {
    await page.goto('/all-collapsed-items');

    const parent = page.locator('#rail vaadin-side-nav-item').first();
    await parent.hover();

    await expect(openPopover(page)).toBeVisible({ timeout: 2_000 });

    // Click the item's own toggle-button (in its shadow DOM) — inline-expands it.
    await parent.evaluate((el: HTMLElement) => {
      const toggle = el.shadowRoot?.querySelector<HTMLButtonElement>('button[part="toggle-button"]');
      toggle?.click();
    });

    // Since the children are now visible inline, the popover must close —
    // otherwise the user sees both the inline children and the popover copy.
    await expect(openPopover(page)).toHaveCount(0, { timeout: 2_000 });
  });

  test('no popover is open immediately after page load', async ({ page }) => {
    await page.goto('/all-collapsed-items');

    // Give the app a moment to settle (client-side bootstrap + any initial
    // "expanded-changed" fires that might mistakenly open popovers).
    await page.waitForLoadState('networkidle');

    await expect(openPopover(page)).toHaveCount(0);
  });
});
