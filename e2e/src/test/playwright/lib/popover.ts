/**
 * Cross-version popover-DOM helpers for the SideNav Rail addon.
 *
 * The Vaadin `<vaadin-popover>` component's DOM differs between V24 and V25:
 *
 * - **V24**: when the popover opens, `<vaadin-popover-overlay>` is teleported
 *   to `<body>`. The overlay carries `[opened]` and `role="menu"`. Slotted
 *   content (the nested `<vaadin-side-nav>`) is moved into the overlay's
 *   subtree.
 * - **V25**: the overlay stays inside `<vaadin-popover>.shadowRoot` and never
 *   reaches top-level `document.querySelectorAll`. The `[opened]` attribute
 *   *and* `role="menu"` move to the popover **host**. Slotted content stays
 *   as a light-DOM child of the host.
 *
 * Empirical findings (V25.1.3, with prod bundle, in Chromium):
 *
 * | Selector                                              | V24 | V25 |
 * |-------------------------------------------------------|-----|-----|
 * | `page.locator('vaadin-popover-overlay[opened]')`      | 1   | 1   |  ← Playwright pierces shadow DOM
 * | `page.locator('vaadin-popover[opened]')`              | ?   | 1   |
 * | comma-form (overlay or host)                          | ?   | 2   |  ← strict-mode trip!
 * | `page.locator(overlay).locator('vaadin-side-nav-item')` | ✓ | 0   |  ← items aren't in V25 overlay subtree
 * | `page.locator(host).locator('vaadin-side-nav-item')`    | 0 | 1   |  ← items are light-DOM children of host
 * | overlay element's `role` attribute                    | menu| null|
 * | host element's `role` attribute                       | none| menu|
 * | `document.querySelectorAll('vaadin-popover-overlay[opened]')` (no Playwright pierce) | 1 | 0 |
 *
 * Rules these helpers encode:
 *
 * 1. **Visibility / count of an open popover** → V24 form alone
 *    (`vaadin-popover-overlay[opened]`). It works on both versions because
 *    Playwright's CSS engine pierces shadow DOM. Don't comma-form: on V25
 *    that matches both overlay (in shadow) and host (top-level), tripping
 *    strict-mode.
 * 2. **Descendant queries inside the popover** → dual-form, because slotted
 *    content lives in different subtrees.
 * 3. **Role-bearing element** → filter by the role attribute, which exists
 *    on the right element on each version.
 * 4. **`document.querySelectorAll` in `page.evaluate(...)`** → query both
 *    selectors; querySelectorAll does **not** pierce shadow DOM.
 */

import { Locator, Page } from '@playwright/test';

/**
 * Triggers a hover-driven popover open in a way that's reliable on both V24
 * and V25.
 *
 * Why a helper: Playwright's `locator.hover()` works on V24 but on V25 the
 * sequence of synthetic pointer events it produces ends with a
 * `pointerleave` ~165 ms after the initial `pointerenter` (faster than the
 * default 200 ms `hoverDelay`), so V25's vaadin-popover hover-controller
 * cancels its pending open and the popover stays closed. The trigger comes
 * from a CSS-driven re-layout on rail-mode hover (rail tooltip pseudo-element
 * /transition) — it's not under the test's control.
 *
 * Manually dispatching `pointerenter` + `mouseenter` on the target item
 * fires the same listeners the popover binds to but doesn't go through
 * Playwright's CDP mouse pipeline, so no spurious leave is emitted. Works
 * identically on V24.
 *
 * The function still uses the locator (so element-presence/visibility
 * actionability is checked) but ignores the actual cursor position.
 */
export async function hoverItem(page: Page, selector: string): Promise<void> {
    const locator = page.locator(selector);
    await locator.waitFor({ state: 'visible' });
    // Synthetic enter only — Playwright's real `.hover()` is intentionally
    // NOT called here. On V25, .hover()'s actionability dance produces a
    // spurious `pointerleave` ~165 ms in (faster than the 200 ms hoverDelay),
    // which cancels the pending open. The leave appears to be triggered by a
    // CSS-driven layout shift in rail mode (rail tooltip ::after / theme
    // transition) that re-positions the cursor relative to the target. Even
    // dispatching synthetic enters AFTER .hover() doesn't recover.
    //
    // The dispatched events hit the same listeners that vaadin-popover's
    // hover-controller binds to, so the open-on-hover contract is exercised
    // exactly as a real cursor enter would — minus Playwright's CDP cursor
    // pipeline. NOT a substitute for tests that assert on CSS `:hover` state
    // (e.g. rail tooltip ::after opacity) — those need real cursor positioning.
    await locator.evaluate((el: Element) => {
        el.dispatchEvent(new PointerEvent('pointerover', {
            bubbles: true,
            pointerType: 'mouse',
        }));
        el.dispatchEvent(new PointerEvent('pointerenter', {
            bubbles: true,
            pointerType: 'mouse',
        }));
        el.dispatchEvent(new MouseEvent('mouseover', { bubbles: true }));
        el.dispatchEvent(new MouseEvent('mouseenter', { bubbles: true }));
    });
}

/**
 * Inverse of {@link hoverItem} — fires `pointerleave` + `mouseleave` so the
 * popover's hover-controller starts the close timer (matches user moving the
 * cursor off the item).
 */
export async function leaveItem(page: Page, selector: string): Promise<void> {
    await page.locator(selector).evaluate((el: Element) => {
        el.dispatchEvent(new PointerEvent('pointerout', {
            bubbles: true,
            pointerType: 'mouse',
        }));
        el.dispatchEvent(new PointerEvent('pointerleave', {
            bubbles: true,
            pointerType: 'mouse',
        }));
        el.dispatchEvent(new MouseEvent('mouseout', { bubbles: true }));
        el.dispatchEvent(new MouseEvent('mouseleave', { bubbles: true }));
    });
}


/**
 * Locator for the open popover (V24 overlay; V25 also matches the same
 * overlay through Playwright's shadow-piercing CSS engine). Use this for
 * `toBeVisible` / `toHaveCount(1)` / `not.toBeVisible` / `toHaveCount(0)`.
 *
 * Don't use a comma-form (`overlay, popover[opened]`): on V25 that matches
 * both overlay and host and trips Playwright's strict mode.
 */
export function openPopover(page: Page): Locator {
    return page.locator('vaadin-popover-overlay[opened]');
}

/**
 * CSS selector for a descendant inside an open popover (dual-form). On V24
 * the slotted content lives in the teleported overlay's subtree; on V25 it
 * stays as a light-DOM child of the popover host. Always emit both branches:
 *
 * ```ts
 * await page.locator(popoverDescendant('vaadin-side-nav-item[path="x"]')).click();
 * ```
 */
export function popoverDescendant(cssSuffix: string): string {
    return `vaadin-popover-overlay[opened] ${cssSuffix}, vaadin-popover[opened] ${cssSuffix}`;
}

/**
 * Locator for the open popover element that carries the `role="menu"`
 * attribute. Filtering by `[role="menu"]` disambiguates V24 (role on overlay)
 * vs V25 (role on host) without strict-mode-tripping comma-form.
 */
export function openPopoverWithMenuRole(page: Page): Locator {
    return page.locator(
        'vaadin-popover-overlay[opened][role="menu"], vaadin-popover[opened][role="menu"]',
    );
}

/**
 * Returns the position-target `path` attribute of every currently-opened
 * popover, across V24 (overlays in `<body>`, `.positionTarget`) and V25
 * (popover hosts marked `[opened]`, `.target`).
 *
 * Used by multi-rail and hover-switch tests that count / identify open
 * popovers. `document.querySelectorAll` doesn't pierce shadow DOM, so on V25
 * the V24-form returns nothing — both queries are needed.
 */
export function queryOpenedTargetPaths(page: Page): Promise<string[]> {
    return page.evaluate(() => {
        const seen = new Set<Element>();
        const paths: string[] = [];
        // V24: overlays teleported to <body>.
        document.querySelectorAll('vaadin-popover-overlay[opened]').forEach((o) => {
            const t = (o as { positionTarget?: Element }).positionTarget;
            if (t && !seen.has(t)) {
                seen.add(t);
                paths.push(t.getAttribute('path') ?? '');
            }
        });
        // V25: popover hosts with [opened].
        document.querySelectorAll('vaadin-popover[opened]').forEach((p) => {
            const t = (p as { target?: Element }).target;
            if (t && !seen.has(t)) {
                seen.add(t);
                paths.push(t.getAttribute('path') ?? '');
            }
        });
        return paths;
    });
}
