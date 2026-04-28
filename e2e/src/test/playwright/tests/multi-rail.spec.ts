import { test, expect, Page } from '@playwright/test';

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

const openedTargets = (page: Page) =>
    page.evaluate(() =>
        [...document.querySelectorAll('vaadin-popover-overlay[opened]')]
            .map((o) => (o as HTMLElement & { positionTarget?: Element })
                .positionTarget?.getAttribute('path') ?? ''));

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

    await page.locator('#rail-a vaadin-side-nav-item[path="code-a"]').hover();
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
    // Verify that the activation-closer's `rail.contains(positionTarget)`
    // ownership filter actually does its job — without it, a click anywhere
    // on the page that bubbles through any popover overlay would close
    // popovers on both rails. We can't easily have two popovers open at once
    // (one cursor), so dispatch a synthetic click instead.
    await page.goto('/multi-rail');
    await page.locator('#toggle-rail-b').click();
    await page.waitForFunction(() => {
        const r = document.querySelector('#rail-b');
        return (r?.getAttribute('theme') || '').split(/\s+/).includes('rail');
    });

    // Open rail-B's popover via hover on rail-B.
    await page.locator('#rail-b vaadin-side-nav-item[path="code-b"]').hover();
    await expect.poll(() => openedTargets(page), { timeout: 3_000 })
        .toEqual(['code-b']);

    // Synthetically dispatch a click that mimics activation inside a foreign
    // (rail-A) popover overlay. Rail-A has no open popover, but if rail-B's
    // closer were not ownership-filtered, processing such a click could
    // affect rail-B. Inject an anchor click event whose composedPath
    // includes a rail-A overlay (we forge a fake one).
    await page.evaluate(() => {
        const fakeOverlay = document.createElement('vaadin-popover-overlay');
        const railA = document.querySelector('#rail-a vaadin-side-nav-item[path="code-a"]');
        (fakeOverlay as any).positionTarget = railA;
        document.body.appendChild(fakeOverlay);
        const a = document.createElement('a');
        a.href = '/x';
        fakeOverlay.appendChild(a);
        a.click();
        fakeOverlay.remove();
    });

    // Rail-B's popover must still be open — its closer must reject the
    // click because the overlay's positionTarget belongs to rail-A.
    await expect.poll(() => openedTargets(page), { timeout: 1_000 })
        .toEqual(['code-b']);
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
