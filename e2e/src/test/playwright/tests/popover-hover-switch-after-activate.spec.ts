import { test, expect, Page } from '@playwright/test';

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

const openedPopoverPaths = (page: Page) =>
    page.evaluate(() =>
        [...document.querySelectorAll('vaadin-popover-overlay[opened]')]
            .map((o) => {
                // V24 overlay has .positionTarget; V25 popover host has .target.
                const t = (o as any).positionTarget ?? (o as any).target;
                return (t as Element | undefined)?.getAttribute("path") ?? "";
            }));

test('rail mode — popover auto-closes after hover-switch following an in-popover activation', async ({ page }) => {
    await page.goto('/keyboard-navigation');
    await blockAnchorNavigation(page);
    await enableRailMode(page);

    // 1. Hover Code -> popover opens.
    await page.locator('#rail vaadin-side-nav-item[path="code"]').hover();
    await expect(page.locator('vaadin-popover-overlay[opened]'))
        .toBeVisible({ timeout: 3_000 });

    // 2. Click an item inside the popover -> activation closer fires.
    await page.locator(
        'vaadin-popover-overlay[opened] vaadin-side-nav-item[path="code/commits"], vaadin-popover[opened] vaadin-side-nav-item[path="code/commits"]'
    ).click();
    await expect(page.locator('vaadin-popover-overlay[opened]'))
        .not.toBeVisible({ timeout: 2_000 });

    // 3. Re-hover Code -> popover opens again.
    await page.locator('#rail vaadin-side-nav-item[path="code"]').hover();
    await expect(page.locator('vaadin-popover-overlay[opened]'))
        .toBeVisible({ timeout: 3_000 });

    // 4. Hover a different parent (Admin) -> Code's popover should auto-close
    //    after the hover hide delay; only Admin's popover should remain.
    await page.locator('#rail vaadin-side-nav-item[path="admin"]').hover();

    await expect.poll(
        () => openedPopoverPaths(page),
        { timeout: 3_000 }
    ).toEqual(['admin']);
});
