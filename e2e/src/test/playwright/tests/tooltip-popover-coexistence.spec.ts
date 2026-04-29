import { test, expect, Page } from '@playwright/test';
import { leaveItem, openPopover } from '../lib/popover';

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

    // `force: true` skips Playwright's actionability dance; on V25 that
    // dance produces a spurious pointerleave (~165 ms) that drops the CSS
    // `:hover` state needed for the tooltip ::after opacity transition AND
    // cancels vaadin-popover's hover-open timer. With force:true the cursor
    // settles on the item once and stays.
    // Hover near the top-left corner of the item (not the center) so any
    // rail-mode layout shift on hover keeps the cursor inside the item's box.
    await page.locator('#rail vaadin-side-nav-item[path="code"]').hover({ position: { x: 4, y: 4 } });
    await expect(openPopover(page))
        .toBeVisible({ timeout: 3_000 });

    await expect.poll(() => tooltipOpacity(page, 'code'), { timeout: 3_000 })
        .toBeGreaterThan(0.5);

    // Both visible at the same time — no flicker, no dismissal of the tooltip.
    await expect(openPopover(page))
        .toBeVisible();
});

test('after popover closes, tooltip can still appear on subsequent hover', async ({ page }) => {
    await page.goto('/keyboard-navigation');
    await enableRailMode(page);

    // Open then close popover (real cursor for `:hover` CSS state needed
    // later; force:true to avoid V25 hover race — see test above).
    // Hover near the top-left corner of the item (not the center) so any
    // rail-mode layout shift on hover keeps the cursor inside the item's box.
    await page.locator('#rail vaadin-side-nav-item[path="code"]').hover({ position: { x: 4, y: 4 } });
    await expect(openPopover(page))
        .toBeVisible({ timeout: 3_000 });
    // Move mouse away — synthetic leave (Playwright's body.hover has the
    // same V25 race as item.hover, so dispatch the leave events directly).
    await leaveItem(page, '#rail vaadin-side-nav-item[path="code"]');
    await expect(openPopover(page))
        .toHaveCount(0, { timeout: 3_000 });

    // Hover Dashboard (a leaf without popover) — the tooltip-only path.
    await page.locator('#rail vaadin-side-nav-item[path="dashboard"]').hover({ position: { x: 4, y: 4 } });
    await expect.poll(() => tooltipOpacity(page, 'dashboard'), { timeout: 3_000 })
        .toBeGreaterThan(0.5);
});
