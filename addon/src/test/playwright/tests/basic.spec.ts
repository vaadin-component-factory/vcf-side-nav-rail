import { test, expect } from '@playwright/test';

test.describe('basic rail mode', () => {
  test('rail renders and toggles between full and rail mode', async ({ page }) => {
    await page.goto('/basic');

    const nav = page.locator('vaadin-side-nav').first();
    await expect(nav).toBeVisible();
    await expect(nav).not.toHaveAttribute('theme', /rail/);

    await page.locator('#toggle-rail').click();
    await expect(nav).toHaveAttribute('theme', /rail/);

    // In rail mode: labels hidden, icons still there
    const labels = nav.locator('vaadin-side-nav-item span.label');
    for (const label of await labels.all()) {
      await expect(label).toBeHidden();
    }

    await page.locator('#toggle-rail').click();
    await expect(nav).not.toHaveAttribute('theme', /rail/);
  });
});
