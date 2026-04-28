import { test, expect, Page } from '@playwright/test';

/**
 * Block actual navigation so the popover-close assertion is not racing the
 * route change. The capture-phase preventDefault stops the browser default
 * action, and Vaadin's client-side router honours `defaultPrevented`. Our
 * addon's close-on-activate listener also runs at capture phase but is not
 * stopped by preventDefault — preventDefault only suppresses the default
 * action, not other listeners.
 */
async function blockAnchorNavigation(page: Page): Promise<void> {
    await page.evaluate(() => {
        document.addEventListener('click', (e) => {
            for (const t of e.composedPath()) {
                if (t instanceof Element && t.localName === 'a') {
                    e.preventDefault();
                    return;
                }
            }
        }, true);
    });
}

async function enableRailMode(page: Page): Promise<void> {
    await page.locator('#toggle-rail').click();
    await page.waitForFunction(() => {
        const rail = document.querySelector('#rail');
        return (rail?.getAttribute('theme') || '').split(/\s+/).includes('rail');
    });
}

async function enableChildrenOnlyInPopover(page: Page): Promise<void> {
    await page.locator('#toggle-popover-only').click();
    await page.waitForFunction(() => {
        const rail = document.querySelector('#rail');
        return (rail?.getAttribute('theme') || '').split(/\s+/).includes('inline-children-hidden');
    });
}

async function openPopoverOnCode(page: Page): Promise<void> {
    await page.locator('#rail vaadin-side-nav-item[path="code"]').hover();
    await expect(page.locator('vaadin-popover-overlay[opened]'))
        .toBeVisible({ timeout: 3_000 });
}

const popoverChild = (path: string) =>
    `vaadin-popover-overlay[opened] vaadin-side-nav-item[path="${path}"], vaadin-popover[opened] vaadin-side-nav-item[path="${path}"]`;

test.describe('popover closes when an item inside it is activated', () => {
    test('rail mode — mouse click on popover item closes the popover', async ({ page }) => {
        await page.goto('/keyboard-navigation');
        await blockAnchorNavigation(page);
        await enableRailMode(page);

        await openPopoverOnCode(page);
        await page.locator(popoverChild('code/branches')).click();

        await expect(page.locator('vaadin-popover-overlay[opened]'))
            .not.toBeVisible({ timeout: 2_000 });
    });

    test('rail mode — Enter on focused popover item closes the popover', async ({ page }) => {
        await page.goto('/keyboard-navigation');
        await blockAnchorNavigation(page);
        await enableRailMode(page);

        await openPopoverOnCode(page);

        // Move focus into the popover and onto the first child anchor.
        await page.locator(popoverChild('code/branches')).evaluate((el: HTMLElement) => {
            const a = el.shadowRoot?.querySelector('a') ?? el.querySelector('a');
            (a as HTMLElement | null)?.focus();
        });
        await page.keyboard.press('Enter');

        await expect(page.locator('vaadin-popover-overlay[opened]'))
            .not.toBeVisible({ timeout: 2_000 });
    });

    test('childrenOnlyInPopover (normal mode) — click on popover item closes the popover', async ({ page }) => {
        await page.goto('/keyboard-navigation');
        await blockAnchorNavigation(page);
        await enableChildrenOnlyInPopover(page);

        await openPopoverOnCode(page);
        await page.locator(popoverChild('code/branches')).click();

        await expect(page.locator('vaadin-popover-overlay[opened]'))
            .not.toBeVisible({ timeout: 2_000 });
    });

    test('normal mode (ALL_COLLAPSED_ITEMS) — click on popover item closes the popover', async ({ page }) => {
        await page.goto('/keyboard-navigation');
        await blockAnchorNavigation(page);

        await openPopoverOnCode(page);
        await page.locator(popoverChild('code/branches')).click();

        await expect(page.locator('vaadin-popover-overlay[opened]'))
            .not.toBeVisible({ timeout: 2_000 });
    });
});
