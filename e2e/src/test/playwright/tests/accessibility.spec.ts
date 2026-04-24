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
    test('rail off — roots have no aria-haspopup / aria-expanded', async ({ page }) => {
        await page.goto('/accessibility');

        for (const path of ['dashboard', 'code', 'admin']) {
            const item = page.locator(`#rail vaadin-side-nav-item[path="${path}"]`);
            await expect(item).not.toHaveAttribute('aria-haspopup', /.*/);
            await expect(item).not.toHaveAttribute('aria-expanded', /.*/);
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
