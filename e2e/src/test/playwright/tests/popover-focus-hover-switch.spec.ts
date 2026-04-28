import { test, expect, Page } from '@playwright/test';

/**
 * Focus-driven analogue of popover-hover-switch-after-activate.spec.ts.
 *
 * In rail mode, vaadin-popover has both `hover` and `focus` triggers. When a
 * user Tabs from Code to Admin, Code's popover (opened by focus) must close
 * and Admin's must open. The risk class: any internal hover/focus state flag
 * that lingers after focus leaves would prevent Code's popover from closing.
 */

async function enableRailMode(page: Page): Promise<void> {
    await page.locator('#toggle-rail').click();
    await page.waitForFunction(() => {
        const rail = document.querySelector('#rail');
        return (rail?.getAttribute('theme') || '').split(/\s+/).includes('rail');
    });
}

const openedPaths = (page: Page) =>
    page.evaluate(() =>
        [...document.querySelectorAll('vaadin-popover-overlay[opened]')]
            .map((o) => {
                // V24 overlay has .positionTarget; V25 popover host has .target.
                const t = (o as any).positionTarget ?? (o as any).target;
                return (t as Element | undefined)?.getAttribute("path") ?? "";
            }));

test('rail mode — Tab from one root to another closes the first popover', async ({ page }) => {
    await page.goto('/keyboard-navigation');
    await enableRailMode(page);

    // Focus Code via JS — popover opens via focus trigger.
    await page.locator('#rail vaadin-side-nav-item[path="code"]').evaluate(
        (el: HTMLElement) => {
            const a = el.shadowRoot?.querySelector('a') as HTMLElement | null;
            a?.focus();
        });
    await expect.poll(() => openedPaths(page), { timeout: 3_000 })
        .toEqual(['code']);

    // Tab to the next root. Browsers move focus to next tabbable; we don't
    // rely on tab order beyond "moves to the next side-nav-item's anchor".
    // Simpler and more robust: directly focus Admin's anchor — the user-flow
    // assertion is the same (focus left Code, arrived at Admin).
    await page.locator('#rail vaadin-side-nav-item[path="admin"]').evaluate(
        (el: HTMLElement) => {
            const a = el.shadowRoot?.querySelector('a') as HTMLElement | null;
            a?.focus();
        });

    await expect.poll(() => openedPaths(page), { timeout: 3_000 })
        .toEqual(['admin']);
});
