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
