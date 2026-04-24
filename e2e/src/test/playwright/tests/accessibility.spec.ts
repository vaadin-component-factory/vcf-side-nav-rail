import { test, expect, Page } from '@playwright/test';

/**
 * Focus the inner <a id="link"> of a rail-scoped vaadin-side-nav-item.
 * Matches the pattern used in keyboard-navigation.spec.ts — see that
 * file for the rationale (custom element delegates focus; light-DOM
 * clones inside popovers require scoping to #rail).
 */
async function focusRailItem(page: Page, path: string): Promise<void> {
    await page.locator(`#rail vaadin-side-nav-item[path="${path}"]`).evaluate(
        (el: HTMLElement) => {
            const anchor = (el.shadowRoot?.querySelector('a')
                ?? el.querySelector('a')) as HTMLElement | null;
            (anchor ?? el).focus();
        });
}

test.describe('rail off — baseline', () => {
    test('rail off — leaf root has no aria-haspopup / aria-expanded', async ({ page }) => {
        await page.goto('/accessibility');

        const dashboard = page.locator('#rail vaadin-side-nav-item[path="dashboard"]');
        await expect(dashboard).not.toHaveAttribute('aria-haspopup', /.*/);
        await expect(dashboard).not.toHaveAttribute('aria-expanded', /.*/);
    });

    test('rail off — parent roots have no aria-haspopup="menu" and no aria-expanded="true"', async ({ page }) => {
        // Vaadin's <vaadin-side-nav-item> natively sets aria-haspopup="true"
        // AND aria-expanded="false" on items with children, regardless of
        // rail mode. We cannot fight those in normal mode; we only assert
        // the negative — the addon's rail-mode-specific "menu" / "true"
        // values must NOT be present outside rail mode.
        await page.goto('/accessibility');

        for (const path of ['code', 'admin']) {
            const item = page.locator(`#rail vaadin-side-nav-item[path="${path}"]`);
            await expect(item).not.toHaveAttribute('aria-haspopup', 'menu');
            await expect(item).not.toHaveAttribute('aria-expanded', 'true');
        }
    });

    test('rail off — nested items have no tabindex', async ({ page }) => {
        await page.goto('/accessibility');

        const nestedPaths = [
            'code/branches', 'code/commits',
            'admin/users', 'admin/users/active', 'admin/users/archived',
            'admin/roles',
        ];
        for (const path of nestedPaths) {
            const item = page.locator(`#rail vaadin-side-nav-item[path="${path}"]`);
            await expect(item).not.toHaveAttribute('tabindex', /.*/);
        }
    });
});

test.describe('rail on, popover closed', () => {
    test('rail on, popover closed — roots with children get aria-haspopup=menu, leaf untouched', async ({ page }) => {
        await page.goto('/accessibility');
        await page.locator('#toggle-rail').click();

        // Roots with children
        for (const path of ['code', 'admin']) {
            const item = page.locator(`#rail vaadin-side-nav-item[path="${path}"]`);
            await expect(item).toHaveAttribute('aria-haspopup', 'menu');
            await expect(item).toHaveAttribute('aria-expanded', 'false');
        }

        // Leaf
        const dashboard = page.locator('#rail vaadin-side-nav-item[path="dashboard"]');
        await expect(dashboard).not.toHaveAttribute('aria-haspopup', /.*/);
        await expect(dashboard).not.toHaveAttribute('aria-expanded', /.*/);
    });

    test('rail on, popover closed — nested items have tabindex=-1', async ({ page }) => {
        await page.goto('/accessibility');
        await page.locator('#toggle-rail').click();

        const nestedPaths = [
            'code/branches', 'code/commits',
            'admin/users', 'admin/users/active', 'admin/users/archived',
            'admin/roles',
        ];
        for (const path of nestedPaths) {
            const item = page.locator(`#rail vaadin-side-nav-item[path="${path}"]`);
            await expect(item).toHaveAttribute('tabindex', '-1');
        }
    });
});

test.describe('rail on, popover open (Code)', () => {
    test('rail on, popover open (Code) — aria-expanded=true on focused root', async ({ page }) => {
        await page.goto('/accessibility');
        await page.locator('#toggle-rail').click();

        await focusRailItem(page, 'code');
        // Popover auto-opens on focus in rail mode (setOpenOnFocus=true).
        await expect(page.locator('vaadin-popover-overlay[opened]')).toHaveCount(1);

        const code = page.locator('#rail vaadin-side-nav-item[path="code"]');
        await expect(code).toHaveAttribute('aria-expanded', 'true');
        await expect(code).toHaveAttribute('aria-haspopup', 'menu');
    });

    test('rail on, popover open (Code) — overlay has role=menu', async ({ page }) => {
        await page.goto('/accessibility');
        await page.locator('#toggle-rail').click();

        await focusRailItem(page, 'code');
        const overlay = page.locator('vaadin-popover-overlay[opened]');
        await expect(overlay).toHaveCount(1);
        await expect(overlay).toHaveAttribute('role', 'menu');
    });

    test('rail on, popover open (Code) — flat children have role=menuitem', async ({ page }) => {
        await page.goto('/accessibility');
        await page.locator('#toggle-rail').click();

        await focusRailItem(page, 'code');
        const overlay = page.locator('vaadin-popover-overlay[opened]');
        await expect(overlay).toHaveCount(1);

        // Locators MUST be scoped to the overlay: the rail DOM still contains
        // these items with tabindex="-1" and no role, so an unscoped selector
        // could hit the wrong copy and produce a false green.
        for (const path of ['code/branches', 'code/commits']) {
            const item = overlay.locator(`vaadin-side-nav-item[path="${path}"]`);
            await expect(item).toHaveCount(1);
            await expect(item).toHaveAttribute('role', 'menuitem');
        }

        // The Code root itself is the popover's target and is NOT duplicated
        // inside the overlay — verify the overlay does not contain it.
        await expect(
            overlay.locator('vaadin-side-nav-item[path="code"]')
        ).toHaveCount(0);
    });
});
