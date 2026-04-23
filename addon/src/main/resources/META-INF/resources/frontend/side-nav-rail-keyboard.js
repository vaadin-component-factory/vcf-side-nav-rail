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
    // Tasks 5–9 fill in per-key handlers. For Task 4 this is a pure no-op —
    // the smoke test only verifies the attribute is set after init runs.
}

// Register as a global so Flow's Page.executeJs can invoke us without a
// dynamic `import()` call, which does not resolve reliably inside executeJs
// in Vaadin's production bundle. The module itself is loaded via @JsModule
// at app boot, so window.vaadinAddonsSideNavRail is ready when Flow needs it.
window.vaadinAddonsSideNavRail = window.vaadinAddonsSideNavRail || {};
window.vaadinAddonsSideNavRail.initKeyboardNavigation = initKeyboardNavigation;
