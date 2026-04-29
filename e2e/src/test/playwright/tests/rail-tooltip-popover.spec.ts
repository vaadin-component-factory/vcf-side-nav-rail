import { test, expect, Page } from '@playwright/test';
import { hoverItem, openPopover, popoverDescendant } from '../lib/popover';

/**
 * Coverage for {@code RailTooltipMode.POPOVER}: leaf and parent root items both
 * produce a popover on hover/focus while in rail mode. The view boots in normal
 * mode and {@code beforeEach} toggles rail mode on (matches the pattern in the
 * other rail-mode specs and ensures the rail's first paint runs without the
 * theme=rail attribute, which avoids a Flow client-init race observed when the
 * view boots already in rail mode).
 */

const ROUTE = '/rail-tooltip-popover';

const DASHBOARD = '#rail vaadin-side-nav-item[path="dashboard"]';
const CODE = '#rail vaadin-side-nav-item[path="code"]';

const HEADER_LOCATOR =
    'vaadin-popover-overlay[opened] .side-nav-rail-popover-header,'
    + ' vaadin-popover[opened] .side-nav-rail-popover-header';

/**
 * Focus the inner {@code <a>} of a rail-scoped {@code vaadin-side-nav-item}.
 * Same pattern as keyboard-navigation.spec.ts / accessibility.spec.ts — the
 * custom element delegates focus to a shadow-DOM anchor.
 */
async function focusRailItem(page: Page, path: string): Promise<void> {
    await page.locator(`#rail vaadin-side-nav-item[path="${path}"]`).evaluate(
        (el: HTMLElement) => {
            const anchor = (el.shadowRoot?.querySelector('a')
                ?? el.querySelector('a')) as HTMLElement | null;
            (anchor ?? el).focus();
        });
}

/**
 * Click {@code #toggle-rail} and wait until at least one {@code <vaadin-popover>}
 * carries the 'focus' value in its {@code trigger} property — same pattern as
 * accessibility.spec.ts / keyboard-navigation.spec.ts. setRailMode → focus-trigger
 * sync is a server roundtrip; without the wait a hover/focus action immediately
 * after the click can race the trigger update.
 */
async function enableRailMode(page: Page): Promise<void> {
    await page.locator('#toggle-rail').click();
    await page.waitForSelector('vaadin-side-nav[theme~="rail"]');
    await page.waitForFunction(() => {
        return [...document.querySelectorAll('vaadin-popover')]
            .some((p: any) => Array.isArray(p.trigger) && p.trigger.includes('focus'));
    }, undefined, { timeout: 10_000 });
}

test.describe('RailTooltipMode.POPOVER', () => {
    test.beforeEach(async ({ page }) => {
        await page.goto(ROUTE);
        await enableRailMode(page);
    });

    test('leaf hover opens popover with header label', async ({ page }) => {
        await hoverItem(page, DASHBOARD);

        await expect(openPopover(page)).toBeVisible({ timeout: 2_000 });
        await expect(page.locator(HEADER_LOCATOR)).toBeVisible();
        await expect(page.locator(HEADER_LOCATOR)).toContainText('Dashboard');
    });

    test('parent hover opens popover with header + children', async ({ page }) => {
        await hoverItem(page, CODE);

        await expect(openPopover(page)).toBeVisible({ timeout: 2_000 });
        await expect(page.locator(HEADER_LOCATOR)).toContainText('Code');
        // The child item is slotted inside the popover.
        await expect(
            page.locator(popoverDescendant('vaadin-side-nav-item[path="code/branches"]'))
        ).toBeVisible();
    });

    test('keyboard focus on a leaf opens its popover', async ({ page }) => {
        // beforeEach already waited for the focus trigger to land.
        await focusRailItem(page, 'dashboard');

        await expect(openPopover(page)).toBeVisible({ timeout: 2_000 });
        await expect(page.locator(HEADER_LOCATOR)).toContainText('Dashboard');
    });

    test('normal mode: no popover on a leaf', async ({ page }) => {
        // beforeEach turned rail mode on; toggle back to normal mode.
        await page.locator('#toggle-rail').click();
        await expect(page.locator('vaadin-side-nav[theme~="rail"]')).toHaveCount(0);

        await hoverItem(page, DASHBOARD);
        // Default hover delay is 200 ms; allow it to elapse before asserting absence.
        await page.waitForTimeout(500);
        await expect(openPopover(page)).toHaveCount(0);
    });

    test('POPOVER mode does not set data-rail-tooltip or title on root items', async ({ page }) => {
        // beforeEach put us in rail mode; verify both root items are clean.
        for (const path of ['dashboard', 'code']) {
            const item = page.locator(`#rail vaadin-side-nav-item[path="${path}"]`);
            await expect(item).not.toHaveAttribute('data-rail-tooltip', /.*/);
            await expect(item).not.toHaveAttribute('title', /.*/);
        }
    });

    test('PopoverHeaderMode = ICON_ONLY removes label text from the popover header', async ({ page }) => {
        await page.locator('#header-icon-only').click();
        await hoverItem(page, DASHBOARD);

        await expect(openPopover(page)).toBeVisible({ timeout: 2_000 });
        // The header DIV exists and renders an icon — but on V25 its light-DOM
        // bounding box collapses to 0×0 (the actual visual lives in the popover's
        // shadow overlay), so Playwright's `toBeVisible` is unreliable here.
        // Asserting on `toContainText` is enough: ICON_ONLY must NOT render the
        // label, and we still assert the icon is present below.
        const header = page.locator(HEADER_LOCATOR);
        await expect(header).toHaveCount(1);
        await expect(header).not.toContainText('Dashboard');
        await expect(header.locator('vaadin-icon')).toHaveCount(1);
    });
});
