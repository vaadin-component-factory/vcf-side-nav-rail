import { test, expect } from '@playwright/test';

test.describe('letter-avatar fallback for items without a prefix icon', () => {
  test('avatar is hidden in normal mode', async ({ page }) => {
    await page.goto('/letter-avatar-fallback');

    const admin = page.locator('vaadin-side-nav-item[path="admin"]');
    await expect(admin).toBeVisible();

    // The avatar lives in the DOM as a slot='prefix' child of admin, but CSS hides it
    // outside rail mode.
    const avatar = admin.locator('vaadin-avatar.side-nav-rail-letter-avatar');
    await expect(avatar).toBeHidden();
  });

  test('avatar shows the uppercase first letter of the label in rail mode', async ({ page }) => {
    await page.goto('/letter-avatar-fallback');
    await page.locator('#toggle-rail').click();

    const avatar = page.locator(
        'vaadin-side-nav-item[path="admin"] vaadin-avatar.side-nav-rail-letter-avatar');
    await expect(avatar).toBeVisible();
    await expect(avatar).toHaveAttribute('abbr', 'A');
  });

  test('items with a real prefix icon do not get a fallback avatar', async ({ page }) => {
    await page.goto('/letter-avatar-fallback');
    await page.locator('#toggle-rail').click();

    // "Dashboard" is the first rail child and has a real icon; no fallback expected.
    const dashboard = page.locator('#rail > vaadin-side-nav-item').first();
    await expect(dashboard).toBeVisible();
    await expect(dashboard.locator('vaadin-avatar.side-nav-rail-letter-avatar'))
        .toHaveCount(0);
  });
});
