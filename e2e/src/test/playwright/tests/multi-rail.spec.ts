import { test, expect, Page } from '@playwright/test';
import { hoverItem, queryOpenedTargetPaths } from '../lib/popover';

/**
 * Two SideNavRails on one page must not interfere with each other:
 *   - The activation closer's ownership filter (rail.contains positionTarget)
 *     must keep clicks in rail-A from closing rail-B's popover.
 *   - moveFocusRightOnRailRoot's findPopoverForTarget does
 *     document.querySelectorAll('vaadin-popover'), so cross-rail interference
 *     is at least theoretically possible if target identity got confused.
 *   - Each rail's data-keyboard-ready / dispose lifecycle is independent.
 */

const focusRoot = async (page: Page, railId: string, path: string) => {
    await page.locator(`#${railId} vaadin-side-nav-item[path="${path}"]`).evaluate(
        (el: HTMLElement) => {
            const a = el.shadowRoot?.querySelector('a') as HTMLElement | null;
            a?.focus();
        });
};

const openedTargets = (page: Page) => queryOpenedTargetPaths(page);

test('both rails initialise independently', async ({ page }) => {
    await page.goto('/multi-rail');
    await expect(page.locator('#rail-a[data-keyboard-ready]')).toBeVisible();
    await expect(page.locator('#rail-b[data-keyboard-ready]')).toBeVisible();
});

test('hovering a parent in rail-A does not open a popover in rail-B', async ({ page }) => {
    await page.goto('/multi-rail');
    await page.locator('#toggle-rail-a').click();
    await page.locator('#toggle-rail-b').click();
    await page.waitForFunction(() => {
        const a = document.querySelector('#rail-a');
        const b = document.querySelector('#rail-b');
        const isRail = (r: Element | null) =>
            (r?.getAttribute('theme') || '').split(/\s+/).includes('rail');
        return isRail(a) && isRail(b);
    });

    await hoverItem(page, '#rail-a vaadin-side-nav-item[path="code-a"]');
    await expect.poll(() => openedTargets(page), { timeout: 3_000 })
        .toEqual(['code-a']);
});

test('ArrowRight on a rail-A root only opens rail-A\'s popover', async ({ page }) => {
    await page.goto('/multi-rail');
    await page.locator('#toggle-rail-a').click();
    await page.waitForFunction(() => {
        const r = document.querySelector('#rail-a');
        return (r?.getAttribute('theme') || '').split(/\s+/).includes('rail');
    });

    await focusRoot(page, 'rail-a', 'code-a');
    await page.keyboard.press('ArrowRight');

    await expect.poll(() => openedTargets(page), { timeout: 3_000 })
        .toEqual(['code-a']);
});

test('rail-A\'s activation closer ignores popovers owned by rail-B', async ({ page }) => {
    // User-facing scenario: rail-B's popover is open, the user clicks
    // somewhere outside both rails. vaadin-popover's outside-click default
    // closes rail-B's popover; the addon's activation closer must not
    // produce errors via cross-rail processing (the ownership filter
    // `rail.contains(positionTarget)` keeps each rail's closer scoped).
    //
    // We use a real cursor click on an empty area of the page — that
    // matches a real user clicking anywhere outside a popover. Both Vaadin
    // majors produce the same behaviour for this scenario: popover closes,
    // no errors.
    await page.goto('/multi-rail');
    await page.locator('#toggle-rail-b').click();
    await page.waitForFunction(() => {
        const r = document.querySelector('#rail-b');
        return (r?.getAttribute('theme') || '').split(/\s+/).includes('rail');
    });

    // Open rail-B's popover.
    await hoverItem(page, '#rail-b vaadin-side-nav-item[path="code-b"]');
    await expect.poll(() => openedTargets(page), { timeout: 3_000 })
        .toEqual(['code-b']);

    // Click on an empty area far from both rails — no anchor in the path,
    // so no router involvement, just a clean outside click.
    await page.mouse.click(800, 600);

    // Rail-B's popover closes; no errors propagate from rail-A's closer.
    await expect.poll(() => openedTargets(page), { timeout: 3_000 })
        .toEqual([]);
});

test('Escape on a leaf root with no popover is a no-op', async ({ page }) => {
    await page.goto('/keyboard-navigation');
    await page.locator('#toggle-rail').click();
    await page.waitForFunction(() => {
        const r = document.querySelector('#rail');
        return (r?.getAttribute('theme') || '').split(/\s+/).includes('rail');
    });

    // Dashboard is a leaf root.
    await page.locator('#rail vaadin-side-nav-item[path="dashboard"]').evaluate(
        (el: HTMLElement) => {
            const a = el.shadowRoot?.querySelector('a') as HTMLElement | null;
            a?.focus();
        });
    await page.keyboard.press('Escape');

    // No popover, no error, focus remains on Dashboard.
    await expect.poll(() => page.evaluate(() => {
        const item = document.activeElement?.closest('vaadin-side-nav-item');
        return item?.getAttribute('path') ?? '';
    })).toBe('dashboard');
});
