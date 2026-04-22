import { test, expect } from '@playwright/test';

test.describe('popover parent-label header', () => {
  test('default NONE renders no header', async ({ page }) => {
    await page.goto('/popover-parent-label-mode');

    const root = page.locator('#rail > vaadin-side-nav-item').first();
    await root.hover();

    const popover = page.locator('vaadin-popover-overlay[opened]');
    await expect(popover).toBeVisible({ timeout: 2_000 });
    await expect(popover.locator('.side-nav-rail-popover-header')).toHaveCount(0);
  });

  test('LABEL_ONLY renders the parent label as text only', async ({ page }) => {
    await page.goto('/popover-parent-label-mode');
    await page.locator('#mode-label').click();

    const root = page.locator('#rail > vaadin-side-nav-item').first();
    await root.hover();

    const header = page
        .locator('vaadin-popover-overlay[opened] .side-nav-rail-popover-header');
    await expect(header).toBeVisible({ timeout: 2_000 });
    await expect(header).toContainText('Code');
    await expect(header.locator('vaadin-icon')).toHaveCount(0);
  });

  test('FULL renders icon + label', async ({ page }) => {
    await page.goto('/popover-parent-label-mode');
    await page.locator('#mode-full').click();

    const root = page.locator('#rail > vaadin-side-nav-item').first();
    await root.hover();

    const header = page
        .locator('vaadin-popover-overlay[opened] .side-nav-rail-popover-header');
    await expect(header).toBeVisible({ timeout: 2_000 });
    await expect(header).toContainText('Code');
    await expect(header.locator('vaadin-icon')).toHaveCount(1);
  });

  // Note: a live mode switch while the popover is open is covered by the Karibu
  // unit test `liveSwitchRebuildsExistingPopover`. Reproducing it through Playwright
  // is brittle — clicking a button outside the rail moves the mouse away from the
  // hovered item, which closes the hover-open popover before the new content is
  // visible. The three happy paths above (NONE / LABEL_ONLY / FULL with a
  // pre-selected mode) give us the user-facing coverage.
});
