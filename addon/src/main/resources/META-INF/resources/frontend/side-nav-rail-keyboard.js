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
        case 'ArrowRight':
            event.preventDefault();
            moveFocusRight(item);
            break;
        case 'ArrowLeft':
            event.preventDefault();
            moveFocusLeft(item, rail);
            break;
        case 'Escape':
            if (handleEscape(item, rail)) {
                event.preventDefault();
            }
            break;
    }
}

function resolveItem(el) {
    if (!el || !el.closest) return null;
    return el.closest('vaadin-side-nav-item');
}

function isItemInScope(item, rail) {
    if (rail.contains(item)) return true;
    // Focus inside a popover overlay counts if the popover's target is a rail-root.
    const overlay = item.closest('vaadin-popover-overlay');
    if (overlay && overlay.positionTarget && rail.contains(overlay.positionTarget)) {
        return true;
    }
    return false;
}

function isRailActive(rail) {
    const theme = rail.getAttribute('theme') || '';
    return theme.split(/\s+/).includes('rail');
}

/**
 * Returns all visible items in document order: items whose ancestors (up to the
 * given scope element) are all expanded. Root items of the scope are always
 * visible; collapsed subtrees are skipped from the walk.
 */
function visibleItemsInScope(scope) {
    const all = [...scope.querySelectorAll('vaadin-side-nav-item')];
    return all.filter(item => {
        let parent = item.parentElement;
        while (parent && parent !== scope) {
            if (parent.localName === 'vaadin-side-nav-item' && !parent.expanded) {
                return false;
            }
            parent = parent.parentElement;
        }
        return true;
    });
}

/**
 * Picks the right walk-set for the currently focused item. Three cases:
 *   1. Focus inside a popover overlay → visible-item walk rooted at the overlay
 *      (same tree semantics as normal mode — Arrow-Down on an expanded parent
 *      descends into the first child, stops at boundaries inside the overlay).
 *   2. Rail mode + focus on a root item → walk root items only (nested items
 *      are still in the DOM but hidden under the rail — per §4.4.2).
 *   3. Otherwise → visible-item walk across the whole rail.
 */
function visibleItems(rail, target) {
    const overlay = target && target.closest && target.closest('vaadin-popover-overlay');
    if (overlay) {
        return visibleItemsInScope(overlay);
    }

    const railMode = isRailActive(rail);
    if (railMode && target && target.hasAttribute('root-item')) {
        return [...rail.querySelectorAll(':scope > vaadin-side-nav-item[root-item]')];
    }

    return visibleItemsInScope(rail);
}

function moveFocusSibling(current, rail, direction) {
    const items = visibleItems(rail, current);
    const idx = items.indexOf(current);
    if (idx < 0) return;
    const next = items[idx + direction];
    if (next) {
        focusItem(next);
    }
    // else: stop at boundary (no-op)
}

function hasChildren(item) {
    return item.querySelector(':scope > vaadin-side-nav-item') !== null;
}

function firstChild(item) {
    return item.querySelector(':scope > vaadin-side-nav-item');
}

function parentItem(item, rail) {
    let p = item.parentElement;
    while (p && p !== rail) {
        if (p.localName === 'vaadin-side-nav-item') {
            return p;
        }
        p = p.parentElement;
    }
    return null;
}

function moveFocusRight(item) {
    // Rail-root case: open the popover (if closed) and move focus into it.
    // This is the universal "into the popover" action in rail mode.
    if (item.hasAttribute('root-item') && isItemRailMode(item)) {
        moveFocusRightOnRailRoot(item);
        return;
    }
    // Normal-mode / popover-nested behaviour: expand-or-descend.
    if (!hasChildren(item)) {
        return;
    }
    if (!item.expanded) {
        item.expanded = true;
    } else {
        const child = firstChild(item);
        if (child) focusItem(child);
    }
}

function isItemRailMode(item) {
    const rail = item.closest('vaadin-side-nav');
    return rail ? isRailActive(rail) : false;
}

function moveFocusRightOnRailRoot(item) {
    if (!hasChildren(item)) {
        return;
    }

    // Fast path: popover is open and already populated → move focus
    // synchronously so a following ArrowDown lands inside the popover.
    let overlay = findOpenPopoverForTarget(item);
    if (overlay) {
        const first = overlay.querySelector('vaadin-side-nav-item');
        if (first) {
            focusItem(first);
            return;
        }
    } else {
        // Popover is closed (e.g., user pressed Esc earlier). Reopen it via
        // the associated <vaadin-popover>: Flow creates one per rail-root
        // with children, and toggling .opened reattaches the overlay.
        const popover = findPopoverForTarget(item);
        if (!popover) return;
        popover.opened = true;
    }

    // Slow path: overlay isn't attached or its slotted content hasn't been
    // populated yet. Poll until the first menu item shows up, then focus it.
    focusFirstPopoverItemWhenReady(item);
}

function focusFirstPopoverItemWhenReady(item, attempt = 0) {
    const overlay = findOpenPopoverForTarget(item);
    const first = overlay && overlay.querySelector('vaadin-side-nav-item');
    if (first) {
        focusItem(first);
        return;
    }
    if (attempt >= 30) {
        // ~500ms total — give up silently; user can retry.
        return;
    }
    requestAnimationFrame(() => focusFirstPopoverItemWhenReady(item, attempt + 1));
}

function findPopoverForTarget(item) {
    return [...document.querySelectorAll('vaadin-popover')]
        .find(p => p.target === item) || null;
}

function moveFocusLeft(item, rail) {
    // 1. Expanded item → collapse (applies in rail tree AND inside popover).
    if (item.expanded && hasChildren(item)) {
        item.expanded = false;
        return;
    }

    // 2. Inside a popover: two sub-cases.
    const overlay = item.closest('vaadin-popover-overlay');
    if (overlay) {
        const popoverParent = parentItemWithin(item, overlay);
        if (popoverParent) {
            // Nested popover item → focus popover-parent.
            focusItem(popoverParent);
            return;
        }
        // Top-level popover item → close popover, return focus to owning rail-root.
        const owner = overlay.positionTarget;
        overlay.opened = false;
        if (owner) focusItem(owner);
        return;
    }

    // 3. Normal rail tree → parent item.
    const parent = parentItem(item, rail);
    if (parent) {
        focusItem(parent);
    }
}

/**
 * Returns the nearest vaadin-side-nav-item ancestor of the given item, stopping at
 * the given scope (usually a popover overlay). Returns null if the item is already
 * at the top level of the scope.
 */
function parentItemWithin(item, scope) {
    let p = item.parentElement;
    while (p && p !== scope) {
        if (p.localName === 'vaadin-side-nav-item') return p;
        p = p.parentElement;
    }
    return null;
}

/**
 * Handles Escape: closes an open popover and returns focus to the owning
 * rail-root. Two cases:
 *   A) focus is inside a popover overlay → close it, focus the position target.
 *   B) focus is on a rail-root with an open popover → close it, keep focus.
 * Returns true if the event was handled (caller preventDefaults).
 */
function handleEscape(item, rail) {
    const overlay = item.closest('vaadin-popover-overlay');
    if (overlay) {
        const owner = overlay.positionTarget;
        overlay.opened = false;
        if (owner) focusItem(owner);
        return true;
    }
    if (item.hasAttribute('root-item')) {
        const popoverOverlay = findOpenPopoverForTarget(item);
        if (popoverOverlay) {
            popoverOverlay.opened = false;
            focusItem(item);
            return true;
        }
    }
    return false;
}

function findOpenPopoverForTarget(rootItem) {
    return [...document.querySelectorAll('vaadin-popover-overlay[opened]')]
        .find(o => o.positionTarget === rootItem) || null;
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
