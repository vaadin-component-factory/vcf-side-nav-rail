import { test, expect } from '@playwright/test';

const ROUTE = '/popover-parent-label-only-in-rail-mode';
const HEADER_LOCATOR =
    'vaadin-popover-overlay[opened] .side-nav-rail-popover-header, vaadin-popover[opened] .side-nav-rail-popover-header';

test.describe('popover parent-label header — only-in-rail-mode flag', () => {
  test('default hides the header in normal mode', async ({ page }) => {
    await page.goto(ROUTE);

    const root = page.locator('#rail > vaadin-side-nav-item').first();
    await root.hover();

    const popover = page.locator('vaadin-popover-overlay[opened]');
    await expect(popover).toBeVisible({ timeout: 2_000 });
    await expect(popover.locator('.side-nav-rail-popover-header')).toHaveCount(0);
  });

  test('default shows the header in rail mode', async ({ page }) => {
    await page.goto(ROUTE);
    await page.locator('#toggle-rail').click();

    const root = page.locator('#rail > vaadin-side-nav-item').first();
    await root.hover();

    const header = page.locator(HEADER_LOCATOR);
    await expect(header).toBeVisible({ timeout: 2_000 });
    await expect(header).toContainText('Code');
  });

  test('disabling the flag shows the header in normal mode', async ({ page }) => {
    await page.goto(ROUTE);
    await page.locator('#flag-off').click();

    const root = page.locator('#rail > vaadin-side-nav-item').first();
    await root.hover();

    const header = page.locator(HEADER_LOCATOR);
    await expect(header).toBeVisible({ timeout: 2_000 });
    await expect(header).toContainText('Code');
  });
});
