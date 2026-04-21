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
});
