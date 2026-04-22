import { test, expect } from '@playwright/test';

test.describe('popover in ONLY_ROOT_COLLAPSED_ITEMS mode', () => {
  test('root item shows popover on hover', async ({ page }) => {
    await page.goto('/only-root-collapsed-items');

    // First child of #rail is "Code" — a root item with children
    const root = page.locator('#rail > vaadin-side-nav-item').first();
    await root.hover();

    const popover = page.locator('vaadin-popover-overlay[opened]');
    await expect(popover).toBeVisible({ timeout: 2_000 });
    // Semantic check: the two direct children of "Code" render inside the popover.
    // (Counting all vaadin-side-nav-item descendants would be brittle — hidden
    // grandchildren also live in the DOM.)
    await expect(popover.getByText('Branches')).toBeVisible();
    await expect(popover.getByText('Tags')).toBeVisible();
  });

  test('nested item does not show a popover on hover', async ({ page }) => {
    await page.goto('/only-root-collapsed-items');

    // Expand the root so its children are inline-visible and hoverable.
    const root = page.locator('#rail > vaadin-side-nav-item').first();
    await root.evaluate((el: HTMLElement) => {
      const toggle = el.shadowRoot?.querySelector<HTMLButtonElement>('button[part="toggle-button"]');
      toggle?.click();
    });

    // Hover the nested "Branches" — which itself has children but is NOT a root.
    const nested = page.locator('vaadin-side-nav-item[path="code/branches"]').first();
    await expect(nested).toBeVisible();
    await nested.hover();

    // No popover must open — only root items are eligible in this mode.
    await expect(page.locator('vaadin-popover-overlay[opened]'))
      .not.toBeVisible({ timeout: 1_500 });
  });
});
