import { test, expect } from '@playwright/test';
import { hoverItem, leaveItem, openPopover } from '../lib/popover';

/**
 * Regression test for the runtime-leaf-becomes-parent bug:
 *
 *   1. A SideNavRailItem with no children is added to the rail upfront.
 *      Its onAttach early-returns wireExpandedListener() because
 *      getItems().isEmpty() at that point.
 *   2. Children are added at runtime (e.g. dynamic-projects demo activating
 *      a project). handleChildrenMutation() materializes the popover.
 *      Without the fix, the expand-event listener was never wired on this
 *      path, so applyPopoverGating never re-ran on chevron-toggle.
 *   3. User hovers the parent -> popover opens (correct).
 *   4. User expands the parent via chevron -> children visible inline.
 *      Without the fix: popover stayed `openOnHover=true`, so the next
 *      hover re-opened the popover even though children are already
 *      visible inline.
 *
 * Expected outcome with the fix: after expanding, hover no longer opens
 * the popover.
 */
test('runtime-added first child wires the expand listener so popover gating reacts to chevron toggle', async ({ page }) => {
    await page.goto('/runtime-first-child');

    // The parent is the top-level "Projects" item — no path attribute,
    // so target it by [root-item] (set by SideNavRail on direct children).
    const parentSelector =
        '#rail vaadin-side-nav-item[root-item]:has(.label:text-is("Projects"))';
    const parent = page.locator(parentSelector);

    // 1. Add three children at runtime to the previously-empty parent.
    //    Wait for an inline (slot="children") child to confirm the mutation
    //    landed — popover-internal copies of Phoenix exist too, so filter
    //    by slot to disambiguate.
    await page.locator('#add-children').click();
    await page.locator(
        '#rail vaadin-side-nav-item[slot="children"]:has(.label:text-is("Phoenix"))',
    ).first().waitFor({ state: 'attached' });

    // 2. Hover the parent — popover opens.
    await hoverItem(page, parentSelector);
    await expect(openPopover(page)).toBeVisible({ timeout: 3_000 });

    // 3. Move the cursor away so the popover closes before we expand,
    //    otherwise the expand → applyPopoverGating → close path overlaps
    //    with the hover-driven open and is hard to assert deterministically.
    await leaveItem(page, parentSelector);
    await expect(openPopover(page)).toHaveCount(0, { timeout: 3_000 });

    // 4. Click the parent's chevron toggle (inside vaadin-side-nav-item's
    //    shadow root) to expand it. The item has no path, so a click on the
    //    item itself would also toggle, but going through the toggle button
    //    keeps the test independent of the no-path click semantics.
    await parent.evaluate((el: HTMLElement) => {
        const toggle = el.shadowRoot?.querySelector<HTMLButtonElement>(
            'button[part="toggle-button"]',
        );
        toggle?.click();
    });
    await expect(parent).toHaveAttribute('expanded', '');

    // 5. Hover the parent again — without the fix, the popover would still
    //    open because openOnHover was never reset. With the fix, the expand
    //    event re-ran applyPopoverGating which set openOnHover=false.
    await hoverItem(page, parentSelector);

    // Wait long enough that a would-be open had time to fire (default
    // hoverDelay is 200 ms).
    await page.waitForTimeout(800);

    await expect(openPopover(page)).toHaveCount(0);
});
