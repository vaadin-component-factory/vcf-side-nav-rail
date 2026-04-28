/*
 * Copyright 2026 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */
package org.vaadin.addons.componentfactory.sidenavrail.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.mvysny.kaributesting.v10.MockVaadin;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.popover.Popover;
import com.vaadin.flow.component.popover.PopoverPosition;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.vaadin.addons.componentfactory.sidenavrail.PopoverOn;
import org.vaadin.addons.componentfactory.sidenavrail.SideNavRail;
import org.vaadin.addons.componentfactory.sidenavrail.SideNavRailItem;

/**
 * Verifies that a SideNavRailItem's server-side state stays consistent across detach
 * and reattach cycles — including reattach to a different rail. Several pieces of
 * state are cached on the item (owner reference, popover instance, listener-wired flag);
 * the contract is that nothing goes stale silently after a Vaadin lifecycle round-trip.
 */
class ReattachStateTest {

    @BeforeEach void setUp() { MockVaadin.setup(); }
    @AfterEach void tearDown() { MockVaadin.tearDown(); }

    private static SideNavRailItem parentItem() {
        SideNavRailItem parent = new SideNavRailItem("Code", "/code");
        parent.addItem(new SideNavRailItem("Branches", "/code/branches"));
        return parent;
    }

    @Test
    void ariaUpdatesAfterDetachReattachOnSameRail() {
        SideNavRail nav = new SideNavRail();
        SideNavRailItem parent = parentItem();
        nav.addItem(parent);
        UI.getCurrent().add(nav);

        UI.getCurrent().remove(nav);
        UI.getCurrent().add(nav);

        // Switch to rail mode after the reattach. If the cached owner rail had not
        // been refreshed, applyAriaAttributes would silently no-op.
        nav.setRailMode(true);
        assertEquals("menu", parent.getElement().getAttribute("aria-haspopup"),
                "aria-haspopup must follow setRailMode after a reattach");
    }

    @Test
    void popoverGatingUpdatesAfterDetachReattach() {
        SideNavRail nav = new SideNavRail();
        SideNavRailItem parent = parentItem();
        nav.addItem(parent);
        UI.getCurrent().add(nav);
        nav.setRailMode(true);

        Popover popover = parent.getPopover()
                .orElseThrow(() -> new AssertionError("parent should have a popover"));
        assertTrue(popover.isOpenOnHover(), "precondition: rail mode → eligible");

        UI.getCurrent().remove(nav);
        UI.getCurrent().add(nav);

        nav.setPopoverOn(PopoverOn.ONLY_RAIL_MODE);
        nav.setRailMode(false);
        assertFalse(popover.isOpenOnHover(),
                "ONLY_RAIL_MODE outside rail mode must disable hover trigger after reattach");
    }

    @Test
    void popoverSettingsChangedDuringDetachAreInEffectAfterReattach() {
        SideNavRail nav = new SideNavRail();
        SideNavRailItem parent = parentItem();
        nav.addItem(parent);
        UI.getCurrent().add(nav);
        Popover popover = parent.getPopover()
                .orElseThrow(() -> new AssertionError("parent should have a popover"));

        UI.getCurrent().remove(nav);

        // Mid-detach: change settings on the rail. On reattach the existing popover
        // must reflect the new values — not the stale ones it was created with.
        nav.setPopoverHoverDelay(750);
        nav.setPopoverHideDelay(1500);
        nav.setPopoverPosition(PopoverPosition.BOTTOM_END);

        UI.getCurrent().add(nav);

        assertEquals(750, popover.getHoverDelay(),
                "hover delay set while detached must apply on reattach");
        assertEquals(1500, popover.getHideDelay(),
                "hide delay set while detached must apply on reattach");
        assertEquals(PopoverPosition.BOTTOM_END, popover.getPosition(),
                "position set while detached must apply on reattach");
    }

    @Test
    void rootMatchNestedToggledWhileDetachedAppliesAfterReattach() {
        SideNavRail nav = new SideNavRail();
        SideNavRailItem parent = parentItem();
        nav.addItem(parent);
        UI.getCurrent().add(nav);

        UI.getCurrent().remove(nav);
        nav.setRootMatchNested(
                org.vaadin.addons.componentfactory.sidenavrail.RootMatchNested.ALL);
        UI.getCurrent().add(nav);

        assertTrue(parent.isMatchNested(),
                "RootMatchNested set while detached must apply on reattach");
    }

    @Test
    void itemMovedToDifferentRailPicksUpNewRailSettings() {
        SideNavRail railA = new SideNavRail();
        railA.setPopoverHoverDelay(200);
        SideNavRail railB = new SideNavRail();
        railB.setPopoverHoverDelay(900);

        SideNavRailItem parent = parentItem();
        railA.addItem(parent);
        UI.getCurrent().add(railA);
        UI.getCurrent().add(railB);

        Popover popover = parent.getPopover()
                .orElseThrow(() -> new AssertionError("parent should have a popover"));
        assertEquals(200, popover.getHoverDelay(), "precondition: rail-A's delay");

        // Move the item to rail-B. With the cached ownerRail correctly reset on
        // detach, subsequent setting changes on rail-B propagate to the popover.
        railA.remove(parent);
        railB.addItem(parent);

        railB.setPopoverHoverDelay(1234);
        assertEquals(1234, popover.getHoverDelay(),
                "after re-parenting, rail-B's settings must drive the popover");
    }

    @Test
    void railModeChangesAfterReparentingApplyToMovedItem() {
        SideNavRail railA = new SideNavRail();
        SideNavRail railB = new SideNavRail();

        SideNavRailItem parent = parentItem();
        railA.addItem(parent);
        UI.getCurrent().add(railA);
        UI.getCurrent().add(railB);

        railA.remove(parent);
        railB.addItem(parent);

        railB.setRailMode(true);
        assertEquals("menu", parent.getElement().getAttribute("aria-haspopup"),
                "rail-B's rail mode must reach a re-parented item");

        railA.setRailMode(true);
        // setting rail-A to rail mode must NOT touch parent — it now belongs to rail-B
        // (assertion is implicit in the next: rail-B off should clear the attribute)
        railB.setRailMode(false);
        assertFalse(parent.getElement().hasAttribute("aria-haspopup"),
                "rail-B exiting rail mode must clear haspopup on the moved item");
    }

    // The expanded-changed DOM event survival across reattach cannot be tested
    // unit-style — the event fires from the real <vaadin-side-nav-item> web
    // component, which doesn't exist in MockVaadin. See the equivalent E2E
    // coverage in popover-detach-reattach.spec.ts.
}
