import { test, expect, Page } from '@playwright/test';
import { hoverItem, leaveItem, openPopover } from '../lib/popover';

/**
 * setVisible() on the rail or on a parent item must keep the addon's behaviour
 * sane: hidden items are not interactable for hover, and re-showing them
 * restores full functionality. setVisible is implemented in Vaadin Flow as
 * `hidden=true` + `display: none` — server-side the component stays attached.
 */

async function enableRailMode(page: Page): Promise<void> {
    await page.locator('#toggle-rail').click();
    await page.waitForFunction(() => {
        const rail = document.querySelector('#rail');
        return (rail?.getAttribute('theme') || '').split(/\s+/).includes('rail');
    });
}

const codeItem = '#rail vaadin-side-nav-item[path="code"]';

test.describe('setVisible(false) on item', () => {
    test('hides the item visually', async ({ page }) => {
        await page.goto('/detach-reattach');
        await page.locator('#toggle-code-visible').click();

        await expect(page.locator(codeItem)).not.toBeVisible();
    });

    test('a hidden item produces no DOM-level :hover (popover stays closed even when force-hovered)', async ({ page }) => {
        await page.goto('/detach-reattach');
        await enableRailMode(page);

        await page.locator('#toggle-code-visible').click();
        await expect(page.locator(codeItem)).not.toBeVisible();

        // Force a synthetic mouseover at the hidden item via dispatchEvent.
        // A real cursor cannot reach a display:none element; this asserts
        // the addon doesn't somehow open a popover from the synthetic event.
        await page.locator(codeItem).evaluate((el: HTMLElement) =>
            el.dispatchEvent(new MouseEvent('mouseover', { bubbles: true })));
        await page.waitForTimeout(500);
        await expect(openPopover(page)).toHaveCount(0);
    });

    test('closes any open popover for that item', async ({ page }) => {
        await page.goto('/detach-reattach');
        await enableRailMode(page);

        // Open Code's popover.
        await hoverItem(page, codeItem);
        await expect(openPopover(page))
            .toBeVisible({ timeout: 3_000 });

        // Hide Code while popover is open. The popover overlay is bound to the
        // item via positionTarget. Setting display:none on the target makes
        // it un-hoverable; vaadin-popover should then auto-close on hover-leave.
        await page.locator('#toggle-code-visible').click();
        await leaveItem(page, codeItem);

        await expect(openPopover(page)).toHaveCount(0, { timeout: 3_000 });
    });

    test('setVisible(true) restores hover-popover', async ({ page }) => {
        await page.goto('/detach-reattach');
        await enableRailMode(page);

        await page.locator('#toggle-code-visible').click();
        await expect(page.locator(codeItem)).not.toBeVisible();

        await page.locator('#toggle-code-visible').click();
        await expect(page.locator(codeItem)).toBeVisible();

        await hoverItem(page, codeItem);
        await expect(openPopover(page))
            .toBeVisible({ timeout: 3_000 });
    });
});

test.describe('setVisible(false) on rail', () => {
    test('hides the entire rail', async ({ page }) => {
        await page.goto('/detach-reattach');
        await page.locator('#toggle-rail-visible').click();
        await expect(page.locator('#rail')).not.toBeVisible();
    });

    test('setVisible(true) restores rail and keyboard navigation', async ({ page }) => {
        await page.goto('/detach-reattach');
        await page.locator('#toggle-rail-visible').click();
        await expect(page.locator('#rail')).not.toBeVisible();
        await page.locator('#toggle-rail-visible').click();
        await expect(page.locator('#rail')).toBeVisible();

        // Keyboard nav still works — focus first item, ArrowDown.
        await page.locator('#rail vaadin-side-nav-item[path="dashboard"]').evaluate(
            (el: HTMLElement) => {
                const a = el.shadowRoot?.querySelector('a') as HTMLElement | null;
                a?.focus();
            });
        await page.keyboard.press('ArrowDown');
        await expect.poll(() => page.evaluate(() => {
            const item = document.activeElement?.closest('vaadin-side-nav-item');
            return item?.getAttribute('path') ?? '';
        })).toBe('code');
    });

    test('rail-mode state survives a setVisible cycle', async ({ page }) => {
        await page.goto('/detach-reattach');
        await enableRailMode(page);

        await page.locator('#toggle-rail-visible').click();
        await page.locator('#toggle-rail-visible').click();

        // The popover still targets the item, so aria-haspopup persists.
        await expect(page.locator(codeItem))
            .toHaveAttribute('aria-haspopup', 'true');
    });
});
