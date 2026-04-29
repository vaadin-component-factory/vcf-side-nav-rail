import { test, expect, Page } from '@playwright/test';
import { hoverItem, openPopover, popoverDescendant, queryOpenedTargetPaths } from '../lib/popover';

/**
 * Regression test for the "stuck-open" bug:
 *
 *   1. Hover a parent item -> popover opens.
 *   2. Click an item inside the popover -> our activation closer fires.
 *      During the click, the anchor inside the popover briefly receives focus
 *      (browser default on mousedown) so vaadin-popover sets __focusInside=true.
 *   3. The activation closer immediately removes the overlay from the DOM,
 *      so vaadin-popover's focusout handler never fires -> __focusInside stays
 *      stuck at true.
 *   4. The user re-hovers the same parent -> popover re-opens.
 *   5. The user hovers another parent -> mouse leaves the first parent,
 *      hideDelay should auto-close the first popover. With the focus flag
 *      still stuck, vaadin-popover thinks the focus trigger is still active
 *      and refuses to close.
 *
 * Expected outcome: after step 5, only the second parent's popover stays open.
 */
async function blockAnchorNavigation(page: Page): Promise<void> {
    await page.evaluate(() => {
        document.addEventListener('click', (e) => {
            for (const t of e.composedPath()) {
                if (t instanceof Element && t.localName === 'a') {
                    e.preventDefault();
                    return;
                }
            }
        }, true);
    });
}

async function enableRailMode(page: Page): Promise<void> {
    await page.locator('#toggle-rail').click();
    await page.waitForFunction(() => {
        const rail = document.querySelector('#rail');
        return (rail?.getAttribute('theme') || '').split(/\s+/).includes('rail');
    });
}

test('rail mode — popover auto-closes after hover-switch following an in-popover activation', async ({ page }) => {
    await page.goto('/keyboard-navigation');
    await blockAnchorNavigation(page);
    await enableRailMode(page);

    // 1. Hover Code -> popover opens.
    await hoverItem(page, '#rail vaadin-side-nav-item[path="code"]');
    await expect(openPopover(page)).toBeVisible({ timeout: 3_000 });

    // 2. Click an item inside the popover -> activation closer fires.
    await page.locator(popoverDescendant('vaadin-side-nav-item[path="code/commits"]')).click();
    await expect(openPopover(page)).toHaveCount(0, { timeout: 2_000 });

    // 3. Re-hover Code -> popover opens again.
    await hoverItem(page, '#rail vaadin-side-nav-item[path="code"]');
    await expect(openPopover(page)).toBeVisible({ timeout: 3_000 });

    // 4. Hover a different parent (Admin) -> Code's popover should auto-close
    //    after the hover hide delay; only Admin's popover should remain.
    await hoverItem(page, '#rail vaadin-side-nav-item[path="admin"]');

    await expect.poll(
        () => queryOpenedTargetPaths(page),
        { timeout: 3_000 }
    ).toEqual(['admin']);
});
