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

  test('popover closes when the item is expanded inline in normal mode', async ({ page }) => {
    await page.goto('/collapsed-item');

    const parent = page.locator('#rail vaadin-side-nav-item').first();
    await parent.hover();

    const popover = page.locator('vaadin-popover-overlay[opened]');
    await expect(popover).toBeVisible({ timeout: 2_000 });

    // Click the item's own toggle-button (in its shadow DOM) — inline-expands it.
    await parent.evaluate((el: HTMLElement) => {
      const toggle = el.shadowRoot?.querySelector<HTMLButtonElement>('button[part="toggle-button"]');
      toggle?.click();
    });

    // Since the children are now visible inline, the popover must close —
    // otherwise the user sees both the inline children and the popover copy.
    await expect(popover).not.toBeVisible({ timeout: 2_000 });
  });

  test('popover reopens when the user collapses the item inline again', async ({ page }) => {
    await page.goto('/collapsed-item');

    const parent = page.locator('#rail vaadin-side-nav-item').first();
    const popover = page.locator('vaadin-popover-overlay[opened]');

    // Setup: hover, expand (popover closes), user still on the item.
    await parent.hover();
    await expect(popover).toBeVisible({ timeout: 2_000 });
    await parent.evaluate((el: HTMLElement) => {
      const toggle = el.shadowRoot?.querySelector<HTMLButtonElement>('button[part="toggle-button"]');
      toggle?.click();
    });
    await expect(popover).not.toBeVisible({ timeout: 2_000 });

    // Now collapse again — the inverse click. Popover must reappear without
    // requiring the user to leave the item and re-enter.
    await parent.evaluate((el: HTMLElement) => {
      const toggle = el.shadowRoot?.querySelector<HTMLButtonElement>('button[part="toggle-button"]');
      toggle?.click();
    });
    await expect(popover).toBeVisible({ timeout: 2_000 });
  });

  test('no popover is open immediately after page load', async ({ page }) => {
    await page.goto('/collapsed-item');

    // Give the app a moment to settle (client-side bootstrap + any initial
    // "expanded-changed" fires that might mistakenly open popovers).
    await page.waitForLoadState('networkidle');

    await expect(page.locator('vaadin-popover-overlay[opened]')).toHaveCount(0);
  });
});
