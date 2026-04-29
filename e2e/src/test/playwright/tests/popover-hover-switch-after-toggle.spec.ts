import { test, expect, Page } from '@playwright/test';
import { hoverItem, openPopover, popoverDescendant, queryOpenedTargetPaths } from '../lib/popover';

/**
 * Regression test for the chevron-toggle variant of the "stuck-open" bug:
 *
 *   1. Hover Admin (rail mode) -> Admin's popover opens (Users, Roles).
 *   2. Click the chevron toggle of `Users` inside the popover. The toggle
 *      is a native <button part="toggle-button"> in vaadin-side-nav-item's
 *      shadow root; modern browsers focus native buttons on mousedown, so
 *      vaadin-popover sets __focusInside=true on the overlay focusin.
 *   3. Hover Code -> mouseleave fires on Admin's overlay, but
 *      vaadin-popover's __handleMouseLeave bails because __focusInside is
 *      stuck at true. Without the fix, Admin's popover stays open and
 *      Code's popover opens alongside.
 *
 * The matching click-on-anchor case is covered by
 * popover-hover-switch-after-activate.spec.ts; this file pins the same
 * symptom for the non-navigating focus trigger (toggle button).
 *
 * Expected outcome after the fix: only Code's popover stays open.
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

test('rail mode — popover auto-closes after hover-switch following an in-popover chevron toggle', async ({ page }) => {
    await page.goto('/keyboard-navigation');
    await blockAnchorNavigation(page);
    await enableRailMode(page);

    // 1. Hover Admin -> popover opens (Users + Roles).
    await hoverItem(page, '#rail vaadin-side-nav-item[path="admin"]');
    await expect(openPopover(page)).toBeVisible({ timeout: 3_000 });

    // 2. Click the chevron toggle of Users inside the popover via a real
    //    Playwright click — that moves the cursor inside the overlay, which
    //    matters for step 3: the popover's auto-close path is driven by
    //    `mouseleave` on the overlay, and only a real cursor (not a
    //    synthetic dispatchEvent) produces that leave when admin's popover
    //    layout shifts after code's opens. The real click also lands the
    //    browser-native focus on the <button>, setting vaadin-popover's
    //    __focusInside flag — which is the precondition for the bug we
    //    are pinning.
    //
    //    Playwright's default CSS engine pierces shadow DOM, so we can
    //    target the toggle button (a `<button part="toggle-button">` inside
    //    vaadin-side-nav-item's shadow root) directly through a chain
    //    locator.
    //    Note: Playwright's default CSS engine pierces shadow DOM, so a
    //    chained locator on `button[part="toggle-button"]` would also
    //    match the toggle buttons of nested items (Active, Archived) that
    //    sit inside Users in the popover. Pinning down Users' OWN toggle
    //    therefore needs a coordinate-based click via the bounding box of
    //    its shadow-root button.
    const toggleBox = await page.locator(popoverDescendant('vaadin-side-nav-item[path="admin/users"]'))
        .first()
        .evaluate((el: HTMLElement) => {
            const toggle = el.shadowRoot?.querySelector<HTMLButtonElement>(
                'button[part="toggle-button"]',
            );
            const rect = toggle?.getBoundingClientRect();
            return rect ? { x: rect.x, y: rect.y, width: rect.width, height: rect.height } : null;
        });
    expect(toggleBox).not.toBeNull();
    await page.mouse.click(
        toggleBox!.x + toggleBox!.width / 2,
        toggleBox!.y + toggleBox!.height / 2,
    );

    // 3. Hover Code -> Admin's popover should auto-close; only Code stays open.
    await hoverItem(page, '#rail vaadin-side-nav-item[path="code"]');

    await expect.poll(
        () => queryOpenedTargetPaths(page),
        { timeout: 3_000 }
    ).toEqual(['code']);
});
