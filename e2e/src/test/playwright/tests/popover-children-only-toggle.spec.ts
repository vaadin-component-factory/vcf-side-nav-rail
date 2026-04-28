import { test, expect, Page } from '@playwright/test';

/**
 * Toggling setChildrenOnlyInPopover while a popover is open must leave the
 * UI in a sensible state — no double-display of children (inline + popover),
 * no stranded popover after the mode switches off.
 *
 * Currently covered: the "all-collapsed-items" view exposes the toggle.
 */

const isRailActive = (page: Page) =>
    page.evaluate(() => {
        const r = document.querySelector('#rail');
        return (r?.getAttribute('theme') || '').split(/\s+/).includes('rail');
    });

test.describe('childrenOnlyInPopover toggle', () => {
    test('toggle ON applies children-only mode: theme set, inline children hidden', async ({ page }) => {
        await page.goto('/keyboard-navigation');
        await page.locator('#toggle-popover-only').click();
        await expect.poll(() => page.evaluate(() => {
            const r = document.querySelector('#rail');
            return (r?.getAttribute('theme') || '').split(/\s+/).includes('inline-children-hidden');
        })).toBe(true);

        // Inline children are CSS-hidden under the children-only theme.
        const inlineChildVisible = await page.locator(
            '#rail vaadin-side-nav-item[path="code"] > vaadin-side-nav-item[slot="children"]'
        ).first().isVisible();
        expect(inlineChildVisible).toBe(false);

        // Hovering still opens the popover — that's the only path to children now.
        await page.locator('#rail vaadin-side-nav-item[path="code"]').hover();
        await expect(page.locator('vaadin-popover-overlay[opened]'))
            .toBeVisible({ timeout: 3_000 });
    });

    test('toggle OFF reverts: theme cleared, inline children visible again on expand', async ({ page }) => {
        await page.goto('/keyboard-navigation');
        await page.locator('#toggle-popover-only').click();
        await page.locator('#toggle-popover-only').click();

        await expect.poll(() => page.evaluate(() => {
            const r = document.querySelector('#rail');
            return (r?.getAttribute('theme') || '').split(/\s+/).includes('inline-children-hidden');
        })).toBe(false);

        // Expand Code so the inline children render. Use evaluate — the
        // toggle button is a shadow-DOM ::part on the side-nav-item.
        await page.locator('#rail vaadin-side-nav-item[path="code"]').evaluate(
            (el: HTMLElement & { expanded?: boolean }) => { el.expanded = true; });

        await expect(page.locator(
            '#rail vaadin-side-nav-item[path="code"] > vaadin-side-nav-item[slot="children"]'
        ).first()).toBeVisible();
    });
});
