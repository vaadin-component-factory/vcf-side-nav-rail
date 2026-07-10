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

  // Regression: on Aura (V25) vaadin-side-nav-item renders an icon-reservation
  // spacer as ::part(content)::before that only collapses for a real <vaadin-icon>.
  // A letter-avatar (<vaadin-avatar>) is not recognized, so the spacer stayed and
  // shoved the avatar ~26px out of the rail's icon column. The addon collapses that
  // spacer for avatar items in rail mode; assert the avatar lines up with the real
  // icons instead of being pushed aside. (No-op on Lumo, so this holds on both.)
  test('rail-mode letter-avatar aligns with real icons (no Aura spacer shove)', async ({
    page,
  }) => {
    await page.goto('/letter-avatar-fallback');
    await page.locator('#toggle-rail').click();

    // Dashboard is the first rail child and carries a real icon (its path is "/",
    // which Vaadin renders as an empty path attribute, so select it positionally).
    const iconPrefix = page
        .locator('#rail > vaadin-side-nav-item')
        .first()
        .locator('vaadin-icon[slot="prefix"]');
    const avatarPrefix = page.locator(
        'vaadin-side-nav-item[path="admin"] > vaadin-avatar.side-nav-rail-letter-avatar');
    await expect(iconPrefix).toBeVisible();
    await expect(avatarPrefix).toBeVisible();

    const iconBox = await iconPrefix.boundingBox();
    const avatarBox = await avatarPrefix.boundingBox();
    expect(iconBox).not.toBeNull();
    expect(avatarBox).not.toBeNull();

    const iconCenter = iconBox!.x + iconBox!.width / 2;
    const avatarCenter = avatarBox!.x + avatarBox!.width / 2;
    // Fixed state measures ~4px apart; the bug shoved it ~32px. 8px cleanly
    // separates the two without being brittle about sub-pixel centering.
    expect(Math.abs(avatarCenter - iconCenter)).toBeLessThanOrEqual(8);
  });
});
