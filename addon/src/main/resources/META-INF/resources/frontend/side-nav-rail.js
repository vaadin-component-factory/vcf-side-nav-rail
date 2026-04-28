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
 * Client-side adapter for <vaadin-side-nav> inside a SideNavRail. Bundles the
 * pieces that need to live on the client because they touch DOM the server
 * doesn't see directly:
 *   - delegated keydown listener at the document level so events originating
 *     in popover overlays (outside the rail's DOM subtree) are still handled
 *     (spec §4.4),
 *   - hover tracker for the popover auto-reopen heuristic,
 *   - aria-haspopup guard against the stock <vaadin-side-nav-item> override,
 *   - popover close-on-activate (see installPopoverActivationCloser).
 */

const ATTACHED = new WeakSet();
const TEARDOWNS = new WeakMap();

/**
 * Initializes the client-side adapter for a given <vaadin-side-nav> element
 * owned by a SideNavRail. Safe to call multiple times — a WeakSet guard
 * dedupes. Pair with dispose() on detach so document-level listeners and
 * the MutationObserver don't outlive the rail.
 *
 * @param {HTMLElement} rail — the <vaadin-side-nav> root element
 */
export function init(rail) {
    if (!rail || ATTACHED.has(rail)) {
        return;
    }
    ATTACHED.add(rail);

    const teardowns = [];

    const keydownHandler = (e) => handleKeydown(e, rail);
    document.addEventListener('keydown', keydownHandler, true);
    teardowns.push(() => document.removeEventListener('keydown', keydownHandler, true));

    const observer = installHaspopupGuard(rail);
    teardowns.push(() => observer.disconnect());

    installHoverTracker(rail);

    const clickHandler = installPopoverActivationCloser(rail);
    teardowns.push(() => document.removeEventListener('click', clickHandler, true));

    rail.setAttribute('data-keyboard-ready', '1');
    TEARDOWNS.set(rail, teardowns);
}

/**
 * Releases document-level listeners and the MutationObserver registered by
 * init(rail). Idempotent — safe to call when init never ran or when called
 * twice. Called from SideNavRail#onDetach.
 *
 * @param {HTMLElement} rail — the <vaadin-side-nav> root element
 */
export function dispose(rail) {
    if (!rail) {
        return;
    }
    const teardowns = TEARDOWNS.get(rail);
    if (teardowns) {
        for (const fn of teardowns) {
            fn();
        }
        TEARDOWNS.delete(rail);
    }
    ATTACHED.delete(rail);
    rail.removeAttribute('data-keyboard-ready');
}

/**
 * Tracks the currently mouse-hovered <vaadin-side-nav-item> on the rail and
 * exposes it as `rail._sideNavRailLastHovered`. The server reads this via
 * Element.executeJs() when handling an `expanded-changed` event to decide
 * whether to auto-open the popover after an inline-collapse. Mouse-driven
 * collapse (chevron click while hovering) opens the popover; keyboard-driven
 * collapse (Arrow-Left, focus elsewhere) does not.
 *
 * `mouseover` (delegated, bubbles) is preferred over per-item `mouseenter`
 * so a single listener at the rail covers every item — including ones that
 * are added later. `mouseleave` on the rail clears the marker so the
 * server-side check returns false when the pointer has left the rail.
 *
 * @param {HTMLElement} rail — the <vaadin-side-nav> root element
 */
function installHoverTracker(rail) {
    rail.addEventListener('mouseover', (e) => {
        const target = e.target;
        if (!target || !target.closest) return;
        const item = target.closest('vaadin-side-nav-item');
        if (item && rail.contains(item)) {
            rail._sideNavRailLastHovered = item;
        }
    });
    rail.addEventListener('mouseleave', () => {
        rail._sideNavRailLastHovered = null;
    });
}

/**
 * Guards `aria-haspopup="menu"` against Vaadin's internal
 * `<vaadin-side-nav-item>` render logic, which otherwise overrides the value
 * back to the generic `"true"` whenever its popover opens. §4.4.5 of the
 * design spec mandates the specific `"menu"` value, so we re-apply it from a
 * MutationObserver each time we see a foreign override in rail mode.
 *
 * @param {HTMLElement} rail — the <vaadin-side-nav> root element
 */
function installHaspopupGuard(rail) {
    const obs = new MutationObserver((muts) => {
        // Only guard while rail mode is active — in normal mode Vaadin's
        // native aria-haspopup="true" on parent items is the expected value
        // and we must not override it.
        if (!isRailActive(rail)) return;
        for (const m of muts) {
            if (m.type !== 'attributes' || m.attributeName !== 'aria-haspopup') continue;
            const target = m.target;
            if (!(target instanceof Element) || target.localName !== 'vaadin-side-nav-item') continue;
            if (!target.hasAttribute('aria-haspopup')) continue;
            if (target.getAttribute('aria-haspopup') !== 'menu') {
                target.setAttribute('aria-haspopup', 'menu');
            }
        }
    });
    // Observe the whole rail subtree so newly-added root items are covered too.
    obs.observe(rail, {
        attributes: true,
        attributeFilter: ['aria-haspopup'],
        subtree: true,
    });
    return obs;
}

/**
 * Installs a delegated click listener that closes a popover overlay owned by
 * this rail when an anchor inside it is activated. Without this, clicking
 * (or pressing Enter on) a navigating item in the popover navigates the
 * route but leaves the popover visible — <vaadin-popover> only auto-closes
 * on outside click, not on inside-clicks of its own content. In normal mode
 * with PopoverOn.ALL_COLLAPSED_ITEMS the popover happens to close as a side
 * effect of the parent inline-expanding on route match (gating re-evaluates
 * to "not eligible"), but in rail mode and in childrenOnlyInPopover that
 * indirect path doesn't fire — hence this explicit closer.
 *
 * The listener runs at the document level (capture) because popover overlays
 * are teleported to <body>, outside the rail's subtree, so a rail-scoped
 * listener wouldn't see them. Each rail's listener filters by overlay
 * ownership (positionTarget inside this rail), so multiple rails on the
 * same page don't cross-close each other's popovers.
 *
 * Enter on a focused <a href> dispatches a click event natively in every
 * major browser, so this single handler covers both mouse activation and
 * keyboard Enter. Non-navigating items render an <a> without href (or no
 * <a> at all), so the href filter leaves expand-toggle clicks alone.
 *
 * Setting `overlay.opened = false` propagates back to the server: the
 * overlay fires `opened-changed`, <vaadin-popover>.__onOpenedChanged mirrors
 * it onto the popover element, which is annotated @DomEvent("opened-changed")
 * on the Flow side — so Popover.isOpened() and any registered
 * OpenedChangeListener stay in sync.
 *
 * @param {HTMLElement} rail — the <vaadin-side-nav> root element
 */
function installPopoverActivationCloser(rail) {
    const handler = (event) => {
        const path = event.composedPath ? event.composedPath() : [];
        let overlay = null;
        let anchor = null;
        for (const el of path) {
            if (!(el instanceof Element)) continue;
            if (!anchor && el.localName === 'a' && el.hasAttribute('href')) {
                anchor = el;
            }
            if (!overlay && el.localName === 'vaadin-popover-overlay') {
                overlay = el;
                break;  // overlay is the outer ancestor; no need to keep walking
            }
        }
        if (!overlay || !anchor) return;
        if (!overlay.positionTarget || !rail.contains(overlay.positionTarget)) {
            return;
        }
        // Blur the anchor first so vaadin-popover's overlay focusout handler
        // synchronously clears its internal __focusInside flag. Without this,
        // browsers focus the anchor on mousedown -> __focusInside=true, the
        // overlay is removed by the line below before focusout asynchronously
        // fires, the flag stays stuck, and the next hover-leave on the parent
        // refuses to auto-close (vaadin-popover thinks the focus trigger is
        // still active).
        if (typeof anchor.blur === 'function') {
            anchor.blur();
        }
        overlay.opened = false;
    };
    document.addEventListener('click', handler, true);
    return handler;
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
            moveFocusRight(item, rail);
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
 * Whether the rail is in "children only in popover" mode. The nested items are
 * still in the DOM but CSS-hidden, so for keyboard purposes the tree behaves
 * like rail mode: only root items participate in the up/down walk, and the
 * popover is the sole path to children. See spec §3.x / SideNavRail.setChildrenOnlyInPopover.
 */
function isPopoverOnlyMode(rail) {
    const theme = rail.getAttribute('theme') || '';
    return theme.split(/\s+/).includes('inline-children-hidden');
}

/**
 * True when the inline rail tree only exposes root items — either because
 * rail mode collapsed nested children, or because popover-only mode CSS-hides
 * them. Used to gate root-only navigation and the "ArrowRight opens popover"
 * shortcut.
 */
function isRootOnlyTree(rail) {
    return isRailActive(rail) || isPopoverOnlyMode(rail);
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
 *   2. Root-only tree (rail mode or popover-only mode) + focus on a root item
 *      → walk root items only. Nested items are still in the DOM but visually
 *      hidden under either the rail collapse (§4.4.2) or the
 *      inline-children-hidden CSS, so the user can't see where focus would
 *      otherwise go.
 *   3. Otherwise → visible-item walk across the whole rail.
 */
function visibleItems(rail, target) {
    const overlay = target && target.closest && target.closest('vaadin-popover-overlay');
    if (overlay) {
        return visibleItemsInScope(overlay);
    }

    if (isRootOnlyTree(rail) && target && target.hasAttribute('root-item')) {
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

function moveFocusRight(item, rail) {
    // Rail-root case: open the popover (if closed) and move focus into it.
    // This is the universal "into the popover" action whenever the rail tree
    // only exposes root items (rail mode or popover-only mode).
    if (item.hasAttribute('root-item') && isRootOnlyTree(rail)) {
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
    const parent = parentItemWithin(item, rail);
    if (parent) {
        focusItem(parent);
    }
}

/**
 * Returns the nearest vaadin-side-nav-item ancestor of the given item, stopping at
 * the given scope (a popover overlay or the rail itself). Returns null if the item
 * is already at the top level of the scope.
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
window.vaadinAddonsSideNavRail.init = init;
window.vaadinAddonsSideNavRail.dispose = dispose;
