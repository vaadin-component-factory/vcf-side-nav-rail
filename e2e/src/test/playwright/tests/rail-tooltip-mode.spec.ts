import { test, expect, Page } from '@playwright/test';

/**
 * The server sets the `data-rail-tooltip` attribute on root items while rail mode
 * is active; CSS renders the attribute as a ::after pseudo-element tooltip.
 * Playwright can't query pseudo-elements directly, but it can observe both the
 * attribute state and the computed ::after styles — together those pin down the
 * behaviour.
 *
 * Background: we migrated away from Vaadin's native tooltip because
 * vaadin-tooltip-mixin auto-dismisses when a peer overlay (our popover) opens,
 * producing a flicker on items that have both a tooltip and a popover. The CSS
 * pseudo-element does not participate in the overlay system, so tooltip + popover
 * coexist cleanly.
 */
async function waitForTooltipAttribute(
    page: Page, path: string, expected: string | null): Promise<void> {
    await page.waitForFunction(
        ({ p, exp }: { p: string; exp: string | null }) => {
            const el = document.querySelector(`vaadin-side-nav-item[path="${p}"]`);
            const actual = el?.getAttribute('data-rail-tooltip') ?? null;
            return actual === exp;
        },
        { p: path, exp: expected },
        { timeout: 5_000 });
}

async function tooltipOpacity(page: Page, path: string): Promise<number> {
    return await page.evaluate((p: string) => {
        const el = document.querySelector(`vaadin-side-nav-item[path="${p}"]`);
        if (!el) return -1;
        const style = window.getComputedStyle(el, '::after');
        return parseFloat(style.opacity);
    }, path);
}

test.describe('rail tooltip (CSS pseudo-element)', () => {
    test('no tooltip attribute in normal mode even when mode is ALL', async ({ page }) => {
        await page.goto('/rail-tooltip-mode');

        // Default mode is ALL but rail mode is off — attribute must be absent.
        await waitForTooltipAttribute(page, 'dashboard', null);
        await waitForTooltipAttribute(page, 'code', null);
    });

    test('ALL writes tooltip attributes on all root items in rail mode', async ({ page }) => {
        await page.goto('/rail-tooltip-mode');
        await page.locator('#toggle-rail').click();

        await waitForTooltipAttribute(page, 'dashboard', 'Dashboard');
        await waitForTooltipAttribute(page, 'code', 'Code');
    });

    test('ALL tooltip becomes visible on hover (pseudo-element opacity)', async ({ page }) => {
        await page.goto('/rail-tooltip-mode');
        await page.locator('#toggle-rail').click();
        await waitForTooltipAttribute(page, 'dashboard', 'Dashboard');

        // Before hover: opacity 0 (the pseudo-element exists but is transparent)
        expect(await tooltipOpacity(page, 'dashboard')).toBe(0);

        // Hover near the top-left corner of the item (not the center) so
        // that any rail-mode layout shift on hover keeps the cursor inside
        // the item's bounding box. The CSS `:hover` rule only requires the
        // cursor to be over the item; position within the box doesn't matter.
        await page.locator('vaadin-side-nav-item[path="dashboard"]').hover({ position: { x: 4, y: 4 } });
        // Wait out the 500ms hover delay + 120ms fade.
        await page.waitForTimeout(800);
        expect(await tooltipOpacity(page, 'dashboard')).toBe(1);
    });

    test('ALL tooltip becomes visible on keyboard focus (pseudo-element opacity)', async ({ page }) => {
        await page.goto('/rail-tooltip-mode');
        await page.locator('#toggle-rail').click();
        await waitForTooltipAttribute(page, 'dashboard', 'Dashboard');

        expect(await tooltipOpacity(page, 'dashboard')).toBe(0);

        // Switch the page into keyboard input modality so :focus-visible engages
        // when we deliver focus programmatically. Without a prior keyboard event
        // the modality stays at "none" and Chromium does not match :focus-visible.
        await page.keyboard.press('Tab');

        // Focus the rail item's inner anchor (matches the keyboard adapter's
        // focusItem() — a host-level :focus-visible alone never matches because
        // focus lands on a descendant, not on the custom element).
        await page.locator('vaadin-side-nav-item[path="dashboard"]').evaluate(
            (el: HTMLElement) => {
                const anchor = (el.shadowRoot?.querySelector('a')
                    ?? el.querySelector('a')) as HTMLElement;
                anchor.focus();
            });

        // Same 500ms hover-delay + 120ms fade as the hover path.
        await page.waitForTimeout(800);
        expect(await tooltipOpacity(page, 'dashboard')).toBe(1);
    });

    test('ONLY_WITHOUT_CHILDREN skips items with children', async ({ page }) => {
        await page.goto('/rail-tooltip-mode');
        await page.locator('#mode-without-children').click();
        await page.locator('#toggle-rail').click();

        await waitForTooltipAttribute(page, 'dashboard', 'Dashboard');
        await waitForTooltipAttribute(page, 'code', null);
    });

    test('NONE leaves tooltip attribute empty even in rail mode', async ({ page }) => {
        await page.goto('/rail-tooltip-mode');
        await page.locator('#mode-none').click();
        await page.locator('#toggle-rail').click();

        // Give the server roundtrip a moment even though we expect no attribute.
        await page.waitForTimeout(500);
        await waitForTooltipAttribute(page, 'dashboard', null);
        await waitForTooltipAttribute(page, 'code', null);
    });

    test('leaving rail mode clears the tooltip attribute', async ({ page }) => {
        await page.goto('/rail-tooltip-mode');
        await page.locator('#toggle-rail').click();
        await waitForTooltipAttribute(page, 'dashboard', 'Dashboard');

        await page.locator('#toggle-rail').click();
        await waitForTooltipAttribute(page, 'dashboard', null);
    });
});
