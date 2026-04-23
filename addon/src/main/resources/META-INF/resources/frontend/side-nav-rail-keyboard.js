/*
 * Copyright 2026 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */

/**
 * Keyboard-navigation adapter for <vaadin-side-nav> inside a SideNavRail.
 * Installs one delegated keydown listener at the document level so events
 * originating in popover overlays (which live outside the rail's DOM subtree)
 * are still handled. Spec: §4.4 of side-nav-rail-design.md.
 */

const ATTACHED = new WeakSet();

/**
 * Initializes keyboard handling for a given <vaadin-side-nav> element owned
 * by a SideNavRail. Safe to call multiple times — a WeakSet guard dedupes.
 *
 * @param {HTMLElement} rail — the <vaadin-side-nav> root element
 */
export function initKeyboardNavigation(rail) {
    if (!rail || ATTACHED.has(rail)) {
        return;
    }
    ATTACHED.add(rail);
    document.addEventListener('keydown', (e) => handleKeydown(e, rail), true);
    rail.setAttribute('data-keyboard-ready', '1');
}

function handleKeydown(event, rail) {
    // document.activeElement lands on the inner <a> of vaadin-side-nav-item, not the
    // custom element itself. Walk up to the nearest side-nav-item to find our scope.
    const item = resolveItem(document.activeElement);
    if (!item || !isItemInScope(item, rail)) {
        return;
    }

    switch (event.key) {
        case 'ArrowDown':
            event.preventDefault();
            moveFocusSibling(item, rail, +1);
            break;
        case 'ArrowUp':
            event.preventDefault();
            moveFocusSibling(item, rail, -1);
            break;
    }
}

function resolveItem(el) {
    if (!el || !el.closest) return null;
    return el.closest('vaadin-side-nav-item');
}

function isItemInScope(item, rail) {
    // For Task 5: only items directly inside the rail tree. Popover scoping
    // is added in Task 8.
    return rail.contains(item);
}

/**
 * Returns all visible items in document order: items whose ancestors are all
 * expanded. Root items of the rail are always visible.
 */
function visibleItems(rail) {
    const all = [...rail.querySelectorAll('vaadin-side-nav-item')];
    return all.filter(item => {
        let parent = item.parentElement;
        while (parent && parent !== rail) {
            if (parent.localName === 'vaadin-side-nav-item' && !parent.expanded) {
                return false;
            }
            parent = parent.parentElement;
        }
        return true;
    });
}

function moveFocusSibling(current, rail, direction) {
    const items = visibleItems(rail);
    const idx = items.indexOf(current);
    if (idx < 0) return;
    const next = items[idx + direction];
    if (next) {
        focusItem(next);
    }
    // else: stop at boundary (no-op)
}

/**
 * Moves focus to a side-nav-item. Vaadin renders the item's anchor inside the
 * shadow root, so focusing the custom element directly is a no-op. We have to
 * reach into shadowRoot to find the <a>. Fall back to the custom element as a
 * last resort; that shouldn't happen for routed items but keeps us defensive.
 */
function focusItem(item) {
    const anchor = item.shadowRoot?.querySelector('a') || item.querySelector('a');
    (anchor || item).focus();
}

// Register as a global so Flow's Page.executeJs can invoke us without a
// dynamic `import()` call, which does not resolve reliably inside executeJs
// in Vaadin's production bundle. The module itself is loaded via @JsModule
// at app boot, so window.vaadinAddonsSideNavRail is ready when Flow needs it.
window.vaadinAddonsSideNavRail = window.vaadinAddonsSideNavRail || {};
window.vaadinAddonsSideNavRail.initKeyboardNavigation = initKeyboardNavigation;
