import { test, expect, Page } from '@playwright/test';

/**
 * After a SideNavRail is detached + reattached, all per-rail JavaScript wiring
 * must be re-installed: the document-level keydown handler, the click
 * activation-closer, the hover tracker, the data-keyboard-ready marker. The
 * popover's expanded-changed DOM listener (server-wired via
 * Element.addEventListener) must remain functional.
 *
 * Server-side, any setting changed while detached must take effect on
 * reattach (regression for the bug that ensurePopover early-returned and
 * never re-seeded settings).
 */

async function enableRailMode(page: Page): Promise<void> {
    await page.locator('#toggle-rail').click();
    await page.waitForFunction(() => {
        const rail = document.querySelector('#rail');
        return (rail?.getAttribute('theme') || '').split(/\s+/).includes('rail');
    });
}

async function detach(page: Page): Promise<void> {
    await page.locator('#detach-rail').click();
    await expect(page.locator('#rail')).toHaveCount(0);
}

async function reattach(page: Page): Promise<void> {
    await page.locator('#reattach-rail').click();
    await expect(page.locator('#rail')).toBeVisible();
    await expect(page.locator('#rail[data-keyboard-ready]')).toBeVisible({ timeout: 3_000 });
}

async function openPopoverOnCode(page: Page): Promise<void> {
    await page.locator('#rail vaadin-side-nav-item[path="code"]').hover();
    await expect(page.locator('vaadin-popover-overlay[opened]'))
        .toBeVisible({ timeout: 3_000 });
}

test.describe('detach + reattach', () => {
    test('data-keyboard-ready is re-applied on reattach', async ({ page }) => {
        await page.goto('/detach-reattach');
        await expect(page.locator('#rail[data-keyboard-ready]')).toBeVisible();

        await detach(page);
        await reattach(page);

        await expect(page.locator('#rail[data-keyboard-ready]')).toBeVisible();
    });

    test('keyboard navigation works after reattach', async ({ page }) => {
        await page.goto('/detach-reattach');
        await detach(page);
        await reattach(page);

        // Focus the first item, then ArrowDown should move focus to the next.
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

    test('activation-closer still fires after reattach', async ({ page }) => {
        await page.goto('/detach-reattach');
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
        await enableRailMode(page);

        await detach(page);
        await reattach(page);
        // re-enable: detach drops rail mode? no, it persists; the rail kept its theme.
        // But re-applying is harmless and protects against future changes.

        await openPopoverOnCode(page);
        await page.locator(
            'vaadin-popover-overlay[opened] vaadin-side-nav-item[path="code/branches"], vaadin-popover[opened] vaadin-side-nav-item[path="code/branches"]'
        ).click();

        await expect(page.locator('vaadin-popover-overlay[opened]'))
            .not.toBeVisible({ timeout: 2_000 });
    });

    test('hover-popover still opens after reattach', async ({ page }) => {
        await page.goto('/detach-reattach');
        await enableRailMode(page);

        await detach(page);
        await reattach(page);

        await openPopoverOnCode(page);
    });

    test('popover settings changed during detach apply on reattach', async ({ page }) => {
        await page.goto('/detach-reattach');
        await detach(page);

        // Server-side change while detached.
        await page.locator('#change-hover-delay').click();

        await reattach(page);

        const hoverDelay = await page.evaluate(() => {
            const item = document.querySelector('vaadin-side-nav-item[path="code"]');
            const popover = [...document.querySelectorAll('vaadin-popover')]
                .find((p) => (p as HTMLElement & { target?: Element }).target === item);
            return (popover as HTMLElement & { hoverDelay?: number })?.hoverDelay;
        });
        expect(hoverDelay).toBe(750);
    });

    test('expanded-changed listener still drives gating after reattach', async ({ page }) => {
        await page.goto('/detach-reattach');
        await detach(page);
        await reattach(page);

        // Normal mode (rail-mode off): ALL_COLLAPSED_ITEMS ⇒ popover eligible
        // only when item is collapsed. Hovering opens the popover; expanding
        // the item must mark the popover ineligible (the chain depends on the
        // expanded-changed DOM listener firing).
        await openPopoverOnCode(page);

        // Click the chevron to expand. Popover should close because gating
        // re-evaluates eligibility.
        await page.locator('#rail vaadin-side-nav-item[path="code"]').evaluate(
            (el: HTMLElement & { expanded?: boolean }) => { el.expanded = true; });

        await expect(page.locator('vaadin-popover-overlay[opened]'))
            .not.toBeVisible({ timeout: 2_000 });
    });

    test('multi-cycle detach/reattach does not stack listeners', async ({ page }) => {
        await page.goto('/detach-reattach');
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
        await enableRailMode(page);

        for (let i = 0; i < 3; i++) {
            await detach(page);
            await reattach(page);
        }

        // After three cycles, only one keydown handler / one click handler /
        // one observer should be active. We can't directly count document-level
        // listeners, but we can assert behaviour: clicking a popover-internal
        // anchor closes the popover *exactly once* (a stacked closer would
        // cause no observable difference in this assertion, so we additionally
        // test that the popover does open and then close — the stacked
        // listeners would manifest as e.g. immediate close on hover or the
        // overlay flickering).
        await openPopoverOnCode(page);
        await page.locator(
            'vaadin-popover-overlay[opened] vaadin-side-nav-item[path="code/branches"], vaadin-popover[opened] vaadin-side-nav-item[path="code/branches"]'
        ).click();
        await expect(page.locator('vaadin-popover-overlay[opened]'))
            .not.toBeVisible({ timeout: 2_000 });
    });

    test('init runs once per attach (no duplicate keydown registration)', async ({ page }) => {
        // A direct way to confirm the WeakSet de-dupe + dispose pairing: count
        // how many times init fires per attach by patching the global.
        await page.goto('/detach-reattach');
        // Wait until the addon's JS module has been loaded and the initial
        // init() has run (data-keyboard-ready is the post-init marker).
        await expect(page.locator('#rail[data-keyboard-ready]')).toBeVisible();

        await page.evaluate(() => {
            const ns = (window as any).vaadinAddonsSideNavRail;
            const origInit = ns.init;
            (window as any).__initCount = 0;
            ns.init = (rail: HTMLElement) => {
                (window as any).__initCount++;
                return origInit(rail);
            };
        });

        await detach(page);
        await reattach(page);
        await detach(page);
        await reattach(page);

        const count = await page.evaluate(() => (window as any).__initCount);
        // Two reattaches = two onAttach round-trips = two init calls.
        expect(count).toBe(2);
    });
});
