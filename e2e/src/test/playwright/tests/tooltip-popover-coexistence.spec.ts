import { test, expect, Page } from '@playwright/test';

/**
 * Migration rationale for the CSS pseudo-element rail-tooltip (instead of
 * vaadin-tooltip) was that vaadin-tooltip-mixin auto-dismisses when a peer
 * overlay (the rail's popover) opens. The CSS-only approach must be immune
 * to that — verify that hovering a parent in rail mode, opening its popover,
 * does NOT trigger any tooltip flicker.
 */

async function enableRailMode(page: Page): Promise<void> {
    await page.locator('#toggle-rail').click();
    await page.waitForFunction(() => {
        const rail = document.querySelector('#rail');
        return (rail?.getAttribute('theme') || '').split(/\s+/).includes('rail');
    });
}

const tooltipOpacity = (page: Page, path: string) =>
    page.evaluate((p) => {
        const el = document.querySelector(`#rail vaadin-side-nav-item[path="${p}"]`);
        if (!el) return null;
        return parseFloat(window.getComputedStyle(el, '::after').opacity);
    }, path);

test('rail-mode tooltip and popover overlay coexist for the same item', async ({ page }) => {
    // The CSS comment in side-nav-rail.css explicitly states that the
    // CSS-only tooltip stays visible alongside the popover (in contrast to
    // vaadin-tooltip-mixin, which auto-dismisses). Pin this contract: hover
    // a parent, after the tooltip's hover-delay (500ms) the ::after must
    // become visible AND the popover overlay must be present.
    await page.goto('/keyboard-navigation');
    await enableRailMode(page);

    await page.locator('#rail vaadin-side-nav-item[path="code"]').hover();
    await expect(page.locator('vaadin-popover-overlay[opened]'))
        .toBeVisible({ timeout: 3_000 });

    await expect.poll(() => tooltipOpacity(page, 'code'), { timeout: 3_000 })
        .toBeGreaterThan(0.5);

    // Both visible at the same time — no flicker, no dismissal of the tooltip.
    await expect(page.locator('vaadin-popover-overlay[opened]'))
        .toBeVisible();
});

test('after popover closes, tooltip can still appear on subsequent hover', async ({ page }) => {
    await page.goto('/keyboard-navigation');
    await enableRailMode(page);

    // Open then close popover.
    await page.locator('#rail vaadin-side-nav-item[path="code"]').hover();
    await expect(page.locator('vaadin-popover-overlay[opened]'))
        .toBeVisible({ timeout: 3_000 });
    // Move mouse away.
    await page.locator('body').hover({ position: { x: 0, y: 0 } });
    await expect(page.locator('vaadin-popover-overlay[opened]'))
        .toHaveCount(0, { timeout: 3_000 });

    // Hover Dashboard (a leaf without popover) — the tooltip-only path.
    await page.locator('#rail vaadin-side-nav-item[path="dashboard"]').hover();
    await expect.poll(() => tooltipOpacity(page, 'dashboard'), { timeout: 3_000 })
        .toBeGreaterThan(0.5);
});
