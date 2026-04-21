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
