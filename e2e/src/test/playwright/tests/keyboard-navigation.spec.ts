import { test, expect } from '@playwright/test';

test.describe('keyboard navigation adapter', () => {
    test('adapter marks the rail as keyboard-ready on attach', async ({ page }) => {
        await page.goto('/keyboard-navigation');

        await expect(page.locator('#rail')).toHaveAttribute(
            'data-keyboard-ready', '1', { timeout: 5_000 });
    });
});
