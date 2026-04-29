import { test, expect, Page } from '@playwright/test';
import { openPopover, openPopoverWithMenuRole, popoverDescendant } from '../lib/popover';

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

/**
 * Click `#toggle-rail` and wait until at least one <vaadin-popover> has the
 * 'focus' value in its `trigger` property. setRailMode → applyFocusTrigger →
 * setOpenOnFocus(true) is a server roundtrip; Playwright's click() resolves
 * before that roundtrip completes, so a `focusRailItem` immediately afterwards
 * can race the trigger update and miss the auto-open.
 */
async function enableRailMode(page: Page): Promise<void> {
    await page.locator('#toggle-rail').click();
    await page.waitForFunction(() => {
        return [...document.querySelectorAll('vaadin-popover')]
            .some((p: any) => Array.isArray(p.trigger) && p.trigger.includes('focus'));
    }, undefined, { timeout: 10_000 });
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
        await enableRailMode(page);

        await focusRailItem(page, 'code');
        // Popover auto-opens on focus in rail mode (setOpenOnFocus=true).
        await expect(openPopover(page)).toHaveCount(1);

        const code = page.locator('#rail vaadin-side-nav-item[path="code"]');
        await expect(code).toHaveAttribute('aria-expanded', 'true');
        await expect(code).toHaveAttribute('aria-haspopup', 'menu');
    });

    test('rail on, popover open (Code) — overlay has role=menu', async ({ page }) => {
        await page.goto('/accessibility');
        await enableRailMode(page);

        await focusRailItem(page, 'code');
        // role="menu" lives on V24's overlay or V25's popover host — filtering by the
        // attribute disambiguates without strict-mode-tripping comma-form.
        await expect(openPopoverWithMenuRole(page)).toHaveCount(1);
    });

    test('rail on, popover open (Code) — flat children have role=menuitem', async ({ page }) => {
        await page.goto('/accessibility');
        await enableRailMode(page);

        await focusRailItem(page, 'code');
        await expect(openPopover(page)).toHaveCount(1);

        // Locators MUST be scoped inside the popover: the rail DOM still
        // contains these items with tabindex="-1" and no role, so an unscoped
        // selector could hit the wrong copy and produce a false green.
        for (const path of ['code/branches', 'code/commits']) {
            const item = page.locator(popoverDescendant(`vaadin-side-nav-item[path="${path}"]`));
            await expect(item).toHaveCount(1);
            await expect(item).toHaveAttribute('role', 'menuitem');
        }

        // The Code root itself is the popover's target and is NOT duplicated
        // inside the popover content — verify the popover does not contain it.
        await expect(
            page.locator(popoverDescendant('vaadin-side-nav-item[path="code"]'))
        ).toHaveCount(0);
    });
});

test.describe('rail on, popover open (Admin)', () => {
    test('rail on, popover open (Admin) — deeply nested children have role=menuitem', async ({ page }) => {
        await page.goto('/accessibility');
        await enableRailMode(page);

        await focusRailItem(page, 'admin');
        await expect(openPopoverWithMenuRole(page)).toHaveCount(1);

        // role="menuitem" is applied recursively at populate time by
        // SideNavRailItem.tagAsMenuItem() — expansion state of `users` does
        // not matter for this assertion.
        const nestedPaths = [
            'admin/users',
            'admin/users/active',
            'admin/users/archived',
            'admin/roles',
        ];
        for (const path of nestedPaths) {
            const item = page.locator(popoverDescendant(`vaadin-side-nav-item[path="${path}"]`));
            await expect(item).toHaveCount(1);
            await expect(item).toHaveAttribute('role', 'menuitem');
        }

        // Admin root itself is not duplicated into the popover content.
        await expect(
            page.locator(popoverDescendant('vaadin-side-nav-item[path="admin"]'))
        ).toHaveCount(0);
    });
});

test.describe('popover close — aria-expanded resets', () => {
    test('popover closed again — aria-expanded returns to false', async ({ page }) => {
        await page.goto('/accessibility');
        await enableRailMode(page);

        await focusRailItem(page, 'code');
        await expect(openPopover(page)).toHaveCount(1);

        await page.keyboard.press('Escape');
        await expect(openPopover(page)).toHaveCount(0);

        const code = page.locator('#rail vaadin-side-nav-item[path="code"]');
        await expect(code).toHaveAttribute('aria-expanded', 'false');
        await expect(code).toHaveAttribute('aria-haspopup', 'menu');
    });
});

test.describe('rail toggled off — cleanup', () => {
    test('rail toggled off — aria-haspopup="menu" / aria-expanded="true" cleared', async ({ page }) => {
        // §4.5: after rail-off, the addon's rail-mode-specific "menu"
        // override must be gone. Vaadin may still carry its native
        // aria-haspopup="true" / aria-expanded="false" on parents — we
        // only assert the negative (no "menu" / no "true").
        await page.goto('/accessibility');
        await page.locator('#toggle-rail').click(); // on
        await page.locator('#toggle-rail').click(); // off again

        for (const path of ['code', 'admin']) {
            const item = page.locator(`#rail vaadin-side-nav-item[path="${path}"]`);
            await expect(item).not.toHaveAttribute('aria-haspopup', 'menu');
            await expect(item).not.toHaveAttribute('aria-expanded', 'true');
        }
    });

    test('rail toggled off — tabindex cleared', async ({ page }) => {
        await page.goto('/accessibility');
        await page.locator('#toggle-rail').click(); // on
        await page.locator('#toggle-rail').click(); // off again

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

test.describe('rail off → on again — re-apply', () => {
    test('rail off → on again — contracts re-apply', async ({ page }) => {
        await page.goto('/accessibility');
        await page.locator('#toggle-rail').click(); // on
        await page.locator('#toggle-rail').click(); // off
        await page.locator('#toggle-rail').click(); // on again

        // Roots with children: ARIA restored
        for (const path of ['code', 'admin']) {
            const item = page.locator(`#rail vaadin-side-nav-item[path="${path}"]`);
            await expect(item).toHaveAttribute('aria-haspopup', 'menu');
            await expect(item).toHaveAttribute('aria-expanded', 'false');
        }

        // Nested items: tabindex restored
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
