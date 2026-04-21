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
