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

test.describe('rail mode — root navigation + Esc', () => {
    test('Arrow-Down walks root items only, skipping hidden children', async ({ page }) => {
        await page.goto('/keyboard-navigation');
        await page.locator('#toggle-rail').click();

        await focusItem(page, 'dashboard');
        await page.keyboard.press('ArrowDown');
        await expectFocusedPath(page, 'code');

        // In normal mode this step would depend on expansion, but in rail mode
        // the nested subtree is skipped even though DOM children exist.
        await page.keyboard.press('ArrowDown');
        await expectFocusedPath(page, 'admin');
    });

    test('Esc closes the auto-opened popover but keeps focus on root', async ({ page }) => {
        await page.goto('/keyboard-navigation');
        await page.locator('#toggle-rail').click();

        await focusItem(page, 'code');
        // Popover auto-opens on focus because setOpenOnFocus is enabled in rail mode.
        await expect(page.locator('vaadin-popover-overlay[opened]')).toBeVisible();

        await page.keyboard.press('Escape');
        await expect(page.locator('vaadin-popover-overlay[opened]')).toHaveCount(0);
        await expectFocusedPath(page, 'code');
    });

    test('Esc from inside popover closes it and returns focus to root', async ({ page }) => {
        await page.goto('/keyboard-navigation');
        await page.locator('#toggle-rail').click();

        await focusItem(page, 'code');
        // Focus into popover — Task 8 implements Arrow-Right as the keyboard route in.
        // For now we move focus manually inside the popover and verify Esc behavior.
        await page.locator('vaadin-popover-overlay[opened] vaadin-side-nav-item[path="code/branches"]')
            .evaluate((el: HTMLElement) => {
                const a = el.shadowRoot?.querySelector('a') as HTMLElement | null;
                (a ?? el).focus();
            });
        await expectFocusedPath(page, 'code/branches');

        await page.keyboard.press('Escape');
        await expect(page.locator('vaadin-popover-overlay[opened]')).toHaveCount(0);
        await expectFocusedPath(page, 'code');
    });
});

test.describe('rail mode — Arrow-Right into popover + in-popover navigation', () => {
    test('Arrow-Right on root moves focus to first popover item', async ({ page }) => {
        await page.goto('/keyboard-navigation');
        await page.locator('#toggle-rail').click();

        await focusItem(page, 'code');
        await expect(page.locator('vaadin-popover-overlay[opened]')).toBeVisible();

        await page.keyboard.press('ArrowRight');
        await expectFocusedPath(page, 'code/branches');
    });

    test('Arrow-Right reopens popover after Esc', async ({ page }) => {
        await page.goto('/keyboard-navigation');
        await page.locator('#toggle-rail').click();

        await focusItem(page, 'code');
        await page.keyboard.press('Escape');
        await expect(page.locator('vaadin-popover-overlay[opened]')).toHaveCount(0);

        await page.keyboard.press('ArrowRight');
        await expect(page.locator('vaadin-popover-overlay[opened]')).toBeVisible();
        await expectFocusedPath(page, 'code/branches');
    });

    test('Arrow-Down inside popover walks menu items', async ({ page }) => {
        await page.goto('/keyboard-navigation');
        await page.locator('#toggle-rail').click();

        await focusItem(page, 'code');
        await page.keyboard.press('ArrowRight');  // now on Branches

        await page.keyboard.press('ArrowDown');
        await expectFocusedPath(page, 'code/commits');

        // Stop at last
        await page.keyboard.press('ArrowDown');
        await expectFocusedPath(page, 'code/commits');
    });

    test('Arrow-Up inside popover walks back and stops at first', async ({ page }) => {
        await page.goto('/keyboard-navigation');
        await page.locator('#toggle-rail').click();

        await focusItem(page, 'code');
        await page.keyboard.press('ArrowRight');     // Branches
        await page.keyboard.press('ArrowDown');      // Commits

        await page.keyboard.press('ArrowUp');
        await expectFocusedPath(page, 'code/branches');

        await page.keyboard.press('ArrowUp');
        await expectFocusedPath(page, 'code/branches');
    });
});

test.describe('rail mode — popover tree navigation (Arrow-Right/Left)', () => {
    test('Arrow-Right on collapsed nested parent expands it (focus stays)', async ({ page }) => {
        await page.goto('/keyboard-navigation');
        await page.locator('#toggle-rail').click();

        await focusItem(page, 'admin');
        // Wait for auto-opened popover — first run can be slow after server warmup,
        // and pressing ArrowRight before the overlay is open re-enters the rail-root
        // branch on the next press instead of expanding the popover item.
        await expect(page.locator('vaadin-popover-overlay[opened]')).toBeVisible();
        await page.keyboard.press('ArrowRight');  // into popover — focus on Users

        await page.keyboard.press('ArrowRight');
        const users = page.locator(
            'vaadin-popover-overlay[opened] vaadin-side-nav-item[path="admin/users"]');
        await expect(users).toHaveJSProperty('expanded', true);
        await expectFocusedPath(page, 'admin/users');
    });

    test('Arrow-Right on expanded nested parent descends to first child', async ({ page }) => {
        await page.goto('/keyboard-navigation');
        await page.locator('#toggle-rail').click();

        await focusItem(page, 'admin');
        await expect(page.locator('vaadin-popover-overlay[opened]')).toBeVisible();
        await page.keyboard.press('ArrowRight');  // Users
        await page.keyboard.press('ArrowRight');  // expanded
        await page.keyboard.press('ArrowRight');  // descend to Active

        await expectFocusedPath(page, 'admin/users/active');
    });

    test('Arrow-Left on expanded nested parent collapses it', async ({ page }) => {
        await page.goto('/keyboard-navigation');
        await page.locator('#toggle-rail').click();

        await focusItem(page, 'admin');
        await expect(page.locator('vaadin-popover-overlay[opened]')).toBeVisible();
        await page.keyboard.press('ArrowRight');  // Users
        await page.keyboard.press('ArrowRight');  // Users expanded

        await page.keyboard.press('ArrowLeft');
        await expect(page.locator(
            'vaadin-popover-overlay[opened] vaadin-side-nav-item[path="admin/users"]'))
            .toHaveJSProperty('expanded', false);
        await expectFocusedPath(page, 'admin/users');
    });

    test('Arrow-Left on nested child focuses popover-parent', async ({ page }) => {
        await page.goto('/keyboard-navigation');
        await page.locator('#toggle-rail').click();

        await focusItem(page, 'admin');
        await expect(page.locator('vaadin-popover-overlay[opened]')).toBeVisible();
        await page.keyboard.press('ArrowRight');  // Users
        await page.keyboard.press('ArrowRight');  // expanded
        await page.keyboard.press('ArrowRight');  // on Active

        await page.keyboard.press('ArrowLeft');
        // Active is a leaf — Arrow-Left moves to popover-parent Users.
        await expectFocusedPath(page, 'admin/users');
    });

    test('Arrow-Left on top-level popover item closes popover + focuses rail-root', async ({ page }) => {
        await page.goto('/keyboard-navigation');
        await page.locator('#toggle-rail').click();

        await focusItem(page, 'admin');
        // Wait for auto-opened popover — first run can be slow after server warmup,
        // and pressing ArrowRight before the overlay is open re-enters the rail-root
        // branch on the next press instead of expanding the popover item.
        await expect(page.locator('vaadin-popover-overlay[opened]')).toBeVisible();
        await page.keyboard.press('ArrowRight');  // into popover, on Users (top-level)

        await page.keyboard.press('ArrowLeft');
        await expect(page.locator('vaadin-popover-overlay[opened]')).toHaveCount(0);
        await expectFocusedPath(page, 'admin');
    });
});
