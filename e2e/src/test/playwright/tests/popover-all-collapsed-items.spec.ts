import { test, expect } from '@playwright/test';
import { openPopover, popoverDescendant } from '../lib/popover';

test.describe('popover in ALL_COLLAPSED_ITEMS mode', () => {
  test('popover opens on hover over inline-closed parent in normal mode', async ({ page }) => {
    await page.goto('/all-collapsed-items');

    const parent = page.locator('#rail vaadin-side-nav-item').first();
    await parent.hover();

    await expect(openPopover(page)).toBeVisible({ timeout: 2_000 });

    await expect(page.locator(popoverDescendant('vaadin-side-nav-item'))).toHaveCount(2);
  });

  test('popover also opens in rail mode', async ({ page }) => {
    await page.goto('/all-collapsed-items');

    await page.locator('#toggle-rail').click();

    const parent = page.locator('#rail vaadin-side-nav-item').first();
    await parent.hover();

    await expect(openPopover(page)).toBeVisible({ timeout: 2_000 });
  });

  test('popover closes when the item is expanded inline in normal mode', async ({ page }) => {
    await page.goto('/all-collapsed-items');

    const parent = page.locator('#rail vaadin-side-nav-item').first();
    await parent.hover();

    await expect(openPopover(page)).toBeVisible({ timeout: 2_000 });

    // Click the item's own toggle-button (in its shadow DOM) — inline-expands it.
    await parent.evaluate((el: HTMLElement) => {
      const toggle = el.shadowRoot?.querySelector<HTMLButtonElement>('button[part="toggle-button"]');
      toggle?.click();
    });

    // Since the children are now visible inline, the popover must close —
    // otherwise the user sees both the inline children and the popover copy.
    await expect(openPopover(page)).toHaveCount(0, { timeout: 2_000 });
  });

  test('popover reopens when the user collapses the item inline again', async ({ page }) => {
    await page.goto('/all-collapsed-items');

    const parent = page.locator('#rail vaadin-side-nav-item').first();

    // Setup: hover, expand (popover closes), user still on the item.
    await parent.hover();
    await expect(openPopover(page)).toBeVisible({ timeout: 2_000 });
    await parent.evaluate((el: HTMLElement) => {
      const toggle = el.shadowRoot?.querySelector<HTMLButtonElement>('button[part="toggle-button"]');
      toggle?.click();
    });
    await expect(openPopover(page)).toHaveCount(0, { timeout: 2_000 });

    // Now collapse again — the inverse click. Popover must reappear without
    // requiring the user to leave the item and re-enter.
    await parent.evaluate((el: HTMLElement) => {
      const toggle = el.shadowRoot?.querySelector<HTMLButtonElement>('button[part="toggle-button"]');
      toggle?.click();
    });
    await expect(openPopover(page)).toBeVisible({ timeout: 2_000 });
  });

  test('no popover is open immediately after page load', async ({ page }) => {
    await page.goto('/all-collapsed-items');

    // Give the app a moment to settle (client-side bootstrap + any initial
    // "expanded-changed" fires that might mistakenly open popovers).
    await page.waitForLoadState('networkidle');

    await expect(openPopover(page)).toHaveCount(0);
  });

  test('popover does not auto-open when the collapse is not driven by mouse hover',
      async ({ page }) => {
    // Counterpart to "popover reopens when the user collapses the item
    // inline again": when the inline-collapse is triggered without the
    // mouse being over the item (keyboard, programmatic, focus-driven),
    // the popover must stay closed. Otherwise the popover would surprise
    // the user during keyboard-driven inline navigation.
    //
    // The addon's auto-open path queries `rail._sideNavRailLastHovered`
    // via Element.executeJs from the server's expanded-changed listener;
    // since no mouseover has fired on the rail in this test, that field
    // is null and the open is suppressed.
    await page.goto('/all-collapsed-items');
    await page.waitForLoadState('networkidle');

    // Park the mouse outside the rail so no mouseover fires while we
    // toggle the item programmatically.
    await page.mouse.move(2000, 2000);

    const parent = page.locator('#rail vaadin-side-nav-item').first();

    // Drive expand → collapse via the web-component's `expanded` property
    // directly. This fires the same expanded-changed events that a click
    // would, but without any mouseover on the rail.
    await parent.evaluate((el: HTMLElement & { expanded: boolean }) => {
      el.expanded = true;
    });
    await page.waitForTimeout(200);
    await parent.evaluate((el: HTMLElement & { expanded: boolean }) => {
      el.expanded = false;
    });

    // Wait long enough for the server's executeJs roundtrip to complete
    // and (incorrectly) call popover.open() if the suppression failed.
    await page.waitForTimeout(1500);
    await expect(openPopover(page)).toHaveCount(0);
  });
});
