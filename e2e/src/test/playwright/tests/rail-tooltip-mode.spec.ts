import { test, expect, Page } from '@playwright/test';

/**
 * After toggling rail mode or changing the tooltip mode, the server applies
 * setTooltipText via a roundtrip. Playwright's click() resolves before that
 * roundtrip lands, so hovering immediately finds the tooltip still unconfigured.
 * This helper polls the slotted vaadin-tooltip until its text property matches.
 */
async function waitForTooltipText(
    page: Page, path: string, expected: string | null): Promise<void> {
    await page.waitForFunction(
        ({ p, exp }: { p: string; exp: string | null }) => {
            const el = document.querySelector(`vaadin-side-nav-item[path="${p}"]`);
            const tip = el?.querySelector(':scope > vaadin-tooltip') as any;
            const actual = (tip?.text as string | undefined) ?? null;
            return actual === exp;
        },
        { p: path, exp: expected },
        { timeout: 5_000 });
}

test.describe('rail tooltip', () => {
    test('no tooltip in normal mode even when mode is ALL', async ({ page }) => {
        await page.goto('/rail-tooltip-mode');

        const dashboard = page.locator('vaadin-side-nav-item[path="dashboard"]');
        await dashboard.hover();
        await page.waitForTimeout(800);

        // Default mode is ALL but rail mode is off — no tooltip overlay should open.
        await expect(page.locator('vaadin-tooltip-overlay[opened]')).toHaveCount(0);
    });

    test('ALL shows a tooltip for leaf items in rail mode', async ({ page }) => {
        await page.goto('/rail-tooltip-mode');
        await page.locator('#toggle-rail').click();
        await waitForTooltipText(page, 'dashboard', 'Dashboard');

        await page.locator('vaadin-side-nav-item[path="dashboard"]').hover();

        const tooltip = page.locator('vaadin-tooltip-overlay[opened]');
        await expect(tooltip).toBeVisible({ timeout: 4_000 });
        await expect(tooltip).toContainText('Dashboard');
    });

    test('ONLY_WITHOUT_CHILDREN suppresses tooltips on items with children', async ({ page }) => {
        await page.goto('/rail-tooltip-mode');
        await page.locator('#mode-without-children').click();
        await page.locator('#toggle-rail').click();
        await waitForTooltipText(page, 'dashboard', 'Dashboard');
        await waitForTooltipText(page, 'code', null);

        // Leaf: tooltip opens
        await page.locator('vaadin-side-nav-item[path="dashboard"]').hover();
        await expect(page.locator('vaadin-tooltip-overlay[opened]'))
            .toBeVisible({ timeout: 4_000 });

        // Move away; then hover Code (has children) — no tooltip should open
        await page.mouse.move(0, 0);
        await expect(page.locator('vaadin-tooltip-overlay[opened]'))
            .toHaveCount(0, { timeout: 2_000 });
        await page.locator('vaadin-side-nav-item[path="code"]').hover();
        await page.waitForTimeout(800);
        await expect(page.locator('vaadin-tooltip-overlay[opened]')).toHaveCount(0);
    });

    test('NONE suppresses tooltips entirely', async ({ page }) => {
        await page.goto('/rail-tooltip-mode');
        await page.locator('#mode-none').click();
        await page.locator('#toggle-rail').click();

        await page.locator('vaadin-side-nav-item[path="dashboard"]').hover();
        await page.waitForTimeout(800);
        await expect(page.locator('vaadin-tooltip-overlay[opened]')).toHaveCount(0);
    });

    test('leaving rail mode clears the tooltip', async ({ page }) => {
        await page.goto('/rail-tooltip-mode');
        await page.locator('#toggle-rail').click();
        await waitForTooltipText(page, 'dashboard', 'Dashboard');

        await page.locator('vaadin-side-nav-item[path="dashboard"]').hover();
        await expect(page.locator('vaadin-tooltip-overlay[opened]'))
            .toBeVisible({ timeout: 4_000 });

        // Leave rail mode; the server-side apply clears the text
        await page.mouse.move(0, 0);
        await page.locator('#toggle-rail').click();
        await waitForTooltipText(page, 'dashboard', null);

        // Next hover stays silent
        await page.locator('vaadin-side-nav-item[path="dashboard"]').hover();
        await page.waitForTimeout(800);
        await expect(page.locator('vaadin-tooltip-overlay[opened]')).toHaveCount(0);
    });
});
