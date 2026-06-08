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
 * Installs a delegated click handler at the document level that handles two
 * cases:
 *
 * Case 1 — click INSIDE a popover overlay owned by this rail:
 *
 *   A) Releases focus that the click landed inside the popover. Browsers
 *      shift focus to native focusables (<a href>, <button>, anything with
 *      a positive tabindex) on mousedown. vaadin-popover's
 *      `__onOverlayFocusIn` then sets its private `__focusInside` flag, and
 *      `__handleMouseLeave` will refuse to auto-close while that flag is
 *      true and the `focus` trigger is active:
 *
 *          if (this.__hasTrigger('focus') && this.__focusInside) return;
 *
 *      In rail mode the rail enables the focus trigger (so keyboard nav
 *      works), so once focus enters the overlay it sticks until something
 *      moves focus out. Without this blur, the popover stays open after
 *      the cursor leaves it — moving from one rail-root to another opens
 *      the second popover while the first stays stuck on screen.
 *
 *   B) Additionally closes the popover when the click activated a
 *      navigating <a href>. Without this, clicking (or pressing Enter on)
 *      a link inside the popover routes the app but leaves the popover
 *      visible — vaadin-popover only auto-closes on outside click, not
 *      on inside-clicks of its own content. In normal mode with
 *      PopoverOn.ALL_COLLAPSED_ITEMS the popover happens to close as a
 *      side effect of the parent inline-expanding on route match (gating
 *      re-evaluates to "not eligible"), but in rail mode and in
 *      childrenOnlyInPopover that indirect path doesn't fire.
 *
 *   Two trigger shapes covered by (A):
 *   - <a href> activation, mouse or keyboard. Enter on a focused <a href>
 *     dispatches a click event natively in every major browser, so the
 *     single click handler covers both. (B) also fires for this case.
 *   - <button>/<button part="toggle-button"> activation, e.g. the chevron
 *     of a parent item rendered inside the popover (`Branches` →
 *     `Active` etc.). Modern browsers focus native buttons on mousedown.
 *     (B) does NOT fire for this case — we want the popover to stay open
 *     so the user can see the just-expanded sub-tree.
 *
 * Case 2 — navigation click on a rail item OUTSIDE any popover overlay:
 *
 *   Leaf items in RailTooltipMode.POPOVER_HEADER carry a tooltip popover with
 *   openOnFocus=true (rail mode). Clicking such an item to navigate gives the
 *   item's <a> focus, keeping the tooltip open via the focus trigger even after
 *   the SPA navigation completes (the layout is never torn down). The handler
 *   detects an <a href> click that is inside this rail but outside any overlay
 *   and blurs focus, releasing the focus trigger so the tooltip closes normally.
 *
 * V24/V25 cross-version note: in V24 the position target is overlay.positionTarget;
 * in V25 the overlay lives in vaadin-popover.shadowRoot and the target is
 * popoverHost.target. The handler walks the composedPath past the overlay to
 * capture the popoverHost and uses whichever property is present.
 *
 * The handler runs at document-level capture because popover overlays may
 * be teleported to <body> (V24) or live in vaadin-popover's shadowRoot
 * (V25), so a rail-scoped listener could miss them. Each rail's listener
 * filters by overlay ownership (positionTarget inside this rail), so
 * multiple rails on the same page don't blur or close each other's
 * popovers.
 *
 * Setting `overlay.opened = false` propagates back to the server: the
 * overlay fires `opened-changed`, <vaadin-popover>.__onOpenedChanged
 * mirrors it onto the popover element, which is annotated
 * @DomEvent("opened-changed") on the Flow side — so Popover.isOpened()
 * and any registered OpenedChangeListener stay in sync.
 *
 * @param {HTMLElement} rail — the <vaadin-side-nav> root element
 */
function installPopoverActivationCloser(rail) {
    const handler = (event) => {
        const path = event.composedPath ? event.composedPath() : [];
        let overlay = null;
        let popoverHost = null;
        let anchor = null;
        let focusable = null;
        for (const el of path) {
            if (!(el instanceof Element)) continue;
            if (!anchor && el.localName === 'a' && el.hasAttribute('href')) {
                anchor = el;
            }
            if (!focusable && isClickFocusable(el)) {
                focusable = el;
            }
            if (!overlay && el.localName === 'vaadin-popover-overlay') {
                overlay = el;
                // Do NOT break: in V25 the vaadin-popover host follows the overlay
                // in the composed path (overlay lives in vaadin-popover.shadowRoot)
                // and carries the position target as .target instead of
                // overlay.positionTarget. Keep walking to capture the host.
            }
            if (overlay && el.localName === 'vaadin-popover') {
                popoverHost = el;
                break;
            }
        }
        if (!overlay) {
            // Navigation click on a rail item outside any popover overlay.
            // openOnFocus=true on tooltip popovers (rail mode) means the tooltip
            // stays open as long as the item's link holds focus — which persists
            // across SPA navigation because the layout is never torn down.
            // Blur focus here so the focus trigger releases and the tooltip closes
            // normally via its hide-delay.
            // Use path.includes(rail) rather than rail.contains(anchor): the
            // anchor lives in the shadow DOM of vaadin-side-nav-item, so contains()
            // (light-DOM only) returns false even for clicks inside the rail.
            if (anchor && path.includes(rail) && focusable
                    && typeof focusable.blur === 'function') {
                focusable.blur();
            }
            return;
        }
        // V24: positionTarget on the overlay; V25: target on the popover host.
        const posTarget = overlay.positionTarget ?? popoverHost?.target ?? null;
        if (!posTarget || !rail.contains(posTarget)) {
            return;
        }
        // (A) Release any focus the click landed inside the popover so
        // vaadin-popover's __focusInside flag clears synchronously via
        // its focusout handler. Must happen BEFORE we yank the overlay
        // (anchor case below) — once the overlay is detached, focusout
        // fires asynchronously into a no-op and the flag stays stuck.
        if (focusable && typeof focusable.blur === 'function') {
            focusable.blur();
        }
        // (B) Close the popover too on a navigating click. Toggle-button
        // and other non-navigating focusable activations fall through
        // here untouched — popover stays open, user sees the result of
        // their interaction (e.g. an expanded sub-tree).
        if (anchor) {
            overlay.opened = false;
        }
    };
    document.addEventListener('click', handler, true);
    return handler;
}

/**
 * Whether an element in a click event's composed path is one a browser
 * would focus on mousedown — the population that triggers vaadin-popover's
 * __focusInside flag and motivates the blur in
 * {@link installPopoverActivationCloser}.
 *
 * Intentionally narrow: we only blur things we're confident the click
 * itself focused. A broader "anything focusable" check (matching
 * `:focus-within`, `[contenteditable]`, form controls, …) would risk
 * blurring elements the user actually wanted focused — typed-into
 * inputs, programmatic-focus form fields. Today the popovers only
 * contain <vaadin-side-nav-item>s, which render as `<a>` for routed
 * items and a `<button part="toggle-button">` for parents — the cases
 * below cover both. Add new branches here if a future popover content
 * type lands focus on click (and update the JSDoc caveat above).
 */
function isClickFocusable(el) {
    if (!el || typeof el.localName !== 'string') return false;
    if (el.localName === 'a' && el.hasAttribute && el.hasAttribute('href')) {
        return true;
    }
    if (el.localName === 'button') {
        return true;
    }
    if (el.hasAttribute && el.hasAttribute('tabindex')
            && el.getAttribute('tabindex') !== '-1') {
        return true;
    }
    return false;
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
    // Focus inside a popover counts if the popover's target is a rail-root.
    const scope = closestPopoverScope(item);
    const positionTarget = popoverScopeTarget(scope);
    if (positionTarget && rail.contains(positionTarget)) {
        return true;
    }
    return false;
}

/**
 * Returns the nearest popover-scope ancestor of an element. Cross-Vaadin-version:
 * V24 teleports vaadin-popover-overlay to <body> (slotted content moves with it,
 * so closest finds the overlay), V25 keeps the overlay in vaadin-popover's
 * shadowRoot (the slotted content stays in the popover host's light DOM, so
 * closest finds the host instead). Both expose .opened, .querySelector for
 * inserted items, and a position-target accessor (see popoverScopeTarget).
 */
function closestPopoverScope(el) {
    if (!el || !el.closest) return null;
    return el.closest('vaadin-popover-overlay, vaadin-popover');
}

/** Position-target ("owning rail-root") of a popover scope element. */
function popoverScopeTarget(scope) {
    if (!scope) return null;
    return scope.localName === 'vaadin-popover'
        ? (scope.target ?? null)
        : (scope.positionTarget ?? null);
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
    const scope = closestPopoverScope(target);
    if (scope) {
        return visibleItemsInScope(scope);
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
    // Bail if the item left the DOM mid-poll (e.g. SPA navigation detached the
    // rail after ArrowRight). The rAF chain holds a closure over `item`, so
    // without this it would keep running for up to ~500ms and could call
    // focus() on a detached node, stealing focus from a freshly-rendered view.
    if (!document.contains(item)) {
        return;
    }
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
    const scope = closestPopoverScope(item);
    if (scope) {
        const popoverParent = parentItemWithin(item, scope);
        if (popoverParent) {
            // Nested popover item → focus popover-parent.
            focusItem(popoverParent);
            return;
        }
        // Top-level popover item → close popover, return focus to owning rail-root.
        const owner = popoverScopeTarget(scope);
        scope.opened = false;
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
    const scope = closestPopoverScope(item);
    if (scope) {
        const owner = popoverScopeTarget(scope);
        scope.opened = false;
        if (owner) focusItem(owner);
        return true;
    }
    if (item.hasAttribute('root-item')) {
        const openScope = findOpenPopoverForTarget(item);
        if (openScope) {
            openScope.opened = false;
            focusItem(item);
            return true;
        }
    }
    return false;
}

/**
 * Finds the open popover scope (V24: vaadin-popover-overlay in <body>;
 * V25: the vaadin-popover host itself, since the overlay lives in its
 * shadowRoot and document.querySelectorAll cannot pierce it) targeting
 * the given rail-root item.
 */
function findOpenPopoverForTarget(rootItem) {
    // V24 — overlay teleported to <body> reachable via document query.
    const overlay = [...document.querySelectorAll('vaadin-popover-overlay[opened]')]
        .find(o => o.positionTarget === rootItem);
    if (overlay) return overlay;
    // V25 — vaadin-popover hosts the overlay in shadow DOM; the host itself
    // exposes .opened and contains the popover content via slot, so it serves
    // as a usable scope for callers that need .opened, .querySelector, etc.
    // The shadow-overlay check is what distinguishes V25 from V24's transient
    // state right after popover.opened=true (where popover.opened is true but
    // the body overlay hasn't attached yet — V24 host has NO items in light
    // DOM, so callers' querySelector('vaadin-side-nav-item') would return
    // nothing and break focus advancement).
    return [...document.querySelectorAll('vaadin-popover')]
        .find(p => p.opened
            && p.target === rootItem
            && p.shadowRoot?.querySelector('vaadin-popover-overlay[opened]'))
        || null;
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
