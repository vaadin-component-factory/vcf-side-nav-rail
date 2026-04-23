import { test, expect, Page } from '@playwright/test';

/**
 * Helper: focuses the outer <vaadin-side-nav-item> via element.focus(). The
 * custom element delegates to its inner <a>, so document.activeElement lands
 * on the anchor. Using page.locator().focus() times out on these elements
 * because Playwright's focusability probe doesn't detect delegatesFocus well.
 */
async function focusItem(page: Page, path: string): Promise<void> {
    // Scope to #rail: popovers keep a hidden clone of each nested item in the
    // light DOM, so a bare vaadin-side-nav-item[path="..."] selector can
    // resolve to multiple elements (strict-mode violation).
    await page.locator(`#rail vaadin-side-nav-item[path="${path}"]`).evaluate(
        (el: HTMLElement) => {
            // Vaadin's <vaadin-side-nav-item> renders its anchor inside shadow DOM.
            // Focusing the custom element itself does not move document.activeElement;
            // we have to reach into shadowRoot to find the <a>.
            const anchor = (el.shadowRoot?.querySelector('a')
                ?? el.querySelector('a')) as HTMLElement | null;
            (anchor ?? el).focus();
        });
}

/**
 * Assert that the active element resolves (via closest()) to a
 * vaadin-side-nav-item with the given path attribute.
 */
async function expectFocusedPath(page: Page, expected: string): Promise<void> {
    await expect.poll(async () => {
        return await page.evaluate(() => {
            const active = document.activeElement;
            const item = active?.closest?.('vaadin-side-nav-item');
            return item ? item.getAttribute('path') : null;
        });
    }, { timeout: 5_000 }).toBe(expected);
}

test.describe('keyboard navigation adapter', () => {
    test('adapter marks the rail as keyboard-ready on attach', async ({ page }) => {
        await page.goto('/keyboard-navigation');

        await expect(page.locator('#rail')).toHaveAttribute(
            'data-keyboard-ready', '1', { timeout: 5_000 });
    });
});

test.describe('normal mode — Arrow-Up/Down', () => {
    test('Arrow-Down walks root items in order and stops at end', async ({ page }) => {
        await page.goto('/keyboard-navigation');

        await focusItem(page, 'dashboard');
        await expectFocusedPath(page, 'dashboard');

        await page.keyboard.press('ArrowDown');
        await expectFocusedPath(page, 'code');

        await page.keyboard.press('ArrowDown');
        await expectFocusedPath(page, 'admin');

        // At the last item — stop, don't wrap.
        await page.keyboard.press('ArrowDown');
        await expectFocusedPath(page, 'admin');
    });

    test('Arrow-Up walks backward and stops at first', async ({ page }) => {
        await page.goto('/keyboard-navigation');

        await focusItem(page, 'admin');
        await page.keyboard.press('ArrowUp');
        await expectFocusedPath(page, 'code');

        await page.keyboard.press('ArrowUp');
        await expectFocusedPath(page, 'dashboard');

        await page.keyboard.press('ArrowUp');
        await expectFocusedPath(page, 'dashboard');
    });

    test('Arrow-Down walks into expanded children (visible subtree)', async ({ page }) => {
        await page.goto('/keyboard-navigation');

        // Expand Code.
        await page.locator('vaadin-side-nav-item[path="code"]').evaluate(
            (el: any) => { el.expanded = true; });

        await focusItem(page, 'dashboard');
        await page.keyboard.press('ArrowDown');
        await expectFocusedPath(page, 'code');

        await page.keyboard.press('ArrowDown');
        await expectFocusedPath(page, 'code/branches');

        await page.keyboard.press('ArrowDown');
        await expectFocusedPath(page, 'code/commits');

        await page.keyboard.press('ArrowDown');
        await expectFocusedPath(page, 'admin');
    });

    test('Arrow-Down skips collapsed subtrees', async ({ page }) => {
        await page.goto('/keyboard-navigation');

        // Code collapsed by default — children must be skipped.
        await focusItem(page, 'dashboard');
        await page.keyboard.press('ArrowDown');
        await expectFocusedPath(page, 'code');

        await page.keyboard.press('ArrowDown');
        // Jumps directly to Admin without visiting code/branches.
        await expectFocusedPath(page, 'admin');
    });
});

test.describe('normal mode — Arrow-Right/Left', () => {
    test('Arrow-Right on collapsed parent expands it (focus stays on parent)', async ({ page }) => {
        await page.goto('/keyboard-navigation');

        await focusItem(page, 'code');
        await page.keyboard.press('ArrowRight');

        await expect(page.locator('vaadin-side-nav-item[path="code"]'))
            .toHaveJSProperty('expanded', true);
        await expectFocusedPath(page, 'code');
    });

    test('Arrow-Right on expanded parent moves focus to first child', async ({ page }) => {
        await page.goto('/keyboard-navigation');

        await page.locator('vaadin-side-nav-item[path="code"]').evaluate(
            (el: any) => { el.expanded = true; });
        await focusItem(page, 'code');

        await page.keyboard.press('ArrowRight');
        await expectFocusedPath(page, 'code/branches');
    });

    test('Arrow-Right on leaf is a no-op', async ({ page }) => {
        await page.goto('/keyboard-navigation');

        await focusItem(page, 'dashboard');
        await page.keyboard.press('ArrowRight');
        await expectFocusedPath(page, 'dashboard');
    });

    test('Arrow-Left on expanded parent collapses it', async ({ page }) => {
        await page.goto('/keyboard-navigation');

        await page.locator('vaadin-side-nav-item[path="code"]').evaluate(
            (el: any) => { el.expanded = true; });
        await focusItem(page, 'code');

        await page.keyboard.press('ArrowLeft');
        await expect(page.locator('vaadin-side-nav-item[path="code"]'))
            .toHaveJSProperty('expanded', false);
    });

    test('Arrow-Left on child moves focus to parent', async ({ page }) => {
        await page.goto('/keyboard-navigation');

        await page.locator('vaadin-side-nav-item[path="code"]').evaluate(
            (el: any) => { el.expanded = true; });
        await focusItem(page, 'code/branches');

        await page.keyboard.press('ArrowLeft');
        await expectFocusedPath(page, 'code');
    });

    test('Arrow-Left on top-level leaf is a no-op', async ({ page }) => {
        await page.goto('/keyboard-navigation');

        await focusItem(page, 'dashboard');
        await page.keyboard.press('ArrowLeft');
        await expectFocusedPath(page, 'dashboard');
    });
});
