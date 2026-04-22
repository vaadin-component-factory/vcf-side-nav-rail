import { test, expect, Page } from '@playwright/test';

/**
 * Reads the text property set on the vaadin-tooltip slotted under a root side-nav-item.
 * This reflects what SideNavRail.applyTooltipFor(...) configured server-side; it is what
 * drives the native tooltip overlay at runtime. We intentionally read the JS property
 * rather than the opened overlay because a Vaadin popover bundle glitch on the current
 * stack (patchVirtualContainer error) can suppress the overlay while the configuration
 * itself is correct — the server-side behaviour is what this spec validates.
 */
async function tooltipText(page: Page, path: string): Promise<string | null> {
    return await page.evaluate((p: string) => {
        const el = document.querySelector(`vaadin-side-nav-item[path="${p}"]`);
        const tip = el?.querySelector(':scope > vaadin-tooltip') as any;
        return tip?.text ?? null;
    }, path);
}

async function waitForTooltipText(page: Page, path: string, expected: string | null): Promise<void> {
    await page.waitForFunction(
        ({ p, exp }: { p: string; exp: string | null }) => {
            const el = document.querySelector(`vaadin-side-nav-item[path="${p}"]`);
            const tip = el?.querySelector(':scope > vaadin-tooltip') as any;
            const actual = tip?.text ?? null;
            return actual === exp;
        },
        { p: path, exp: expected },
        { timeout: 5_000 });
}

test.describe('rail tooltip', () => {
    test('no tooltip text in normal mode even when mode is ALL', async ({ page }) => {
        await page.goto('/rail-tooltip-mode');

        // Default mode is ALL but rail mode is off — tooltip text must be empty.
        expect(await tooltipText(page, 'dashboard')).toBeFalsy();
        expect(await tooltipText(page, 'code')).toBeFalsy();
    });

    test('ALL sets tooltip text on all root items in rail mode', async ({ page }) => {
        await page.goto('/rail-tooltip-mode');
        await page.locator('#toggle-rail').click();

        await waitForTooltipText(page, 'dashboard', 'Dashboard');
        await waitForTooltipText(page, 'code', 'Code');
    });

    test('ONLY_WITHOUT_CHILDREN skips items with children', async ({ page }) => {
        await page.goto('/rail-tooltip-mode');
        await page.locator('#mode-without-children').click();
        await page.locator('#toggle-rail').click();

        await waitForTooltipText(page, 'dashboard', 'Dashboard');
        await waitForTooltipText(page, 'code', null);
    });

    test('NONE leaves tooltip text empty even in rail mode', async ({ page }) => {
        await page.goto('/rail-tooltip-mode');
        await page.locator('#mode-none').click();
        await page.locator('#toggle-rail').click();

        // Give the server roundtrip a moment even though we expect no text to appear.
        await page.waitForTimeout(500);
        expect(await tooltipText(page, 'dashboard')).toBeFalsy();
        expect(await tooltipText(page, 'code')).toBeFalsy();
    });

    test('leaving rail mode clears the tooltip text', async ({ page }) => {
        await page.goto('/rail-tooltip-mode');
        await page.locator('#toggle-rail').click();
        await waitForTooltipText(page, 'dashboard', 'Dashboard');

        await page.locator('#toggle-rail').click();
        await waitForTooltipText(page, 'dashboard', null);
    });
});
