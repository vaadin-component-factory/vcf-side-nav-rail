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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.vaadin.addons.componentfactory.sidenavrail.SideNavRail;
import org.vaadin.addons.componentfactory.sidenavrail.SideNavRailItem;

/**
 * setVisible() on the rail or on individual items must keep the addon's state
 * consistent — popover hover-eligibility, ARIA attributes, and tooltip wiring.
 * setVisible(false) is implemented in Vaadin Flow as the element being marked
 * hidden client-side; server-side, the component is still attached. The addon
 * therefore does not need to clean up listeners — but it must not push state
 * onto an item the user explicitly hid, and it must restore everything when the
 * item becomes visible again.
 */
class VisibilityTest {

    @BeforeEach void setUp() { MockVaadin.setup(); }
    @AfterEach void tearDown() { MockVaadin.tearDown(); }

    private static SideNavRailItem parentWithChildren() {
        SideNavRailItem parent = new SideNavRailItem("Code", "/code");
        parent.addItem(new SideNavRailItem("Branches", "/code/branches"));
        return parent;
    }

    @Test
    void itemSetVisibleFalseDoesNotThrow() {
        SideNavRail nav = new SideNavRail();
        SideNavRailItem parent = parentWithChildren();
        nav.addItem(parent);
        UI.getCurrent().add(nav);

        // Should not throw — purely a flag flip.
        parent.setVisible(false);
        assertFalse(parent.isVisible());
    }

    @Test
    void itemSetVisibleFalseRetainsPopoverInstance() {
        SideNavRail nav = new SideNavRail();
        SideNavRailItem parent = parentWithChildren();
        nav.addItem(parent);
        UI.getCurrent().add(nav);

        Popover before = parent.getPopover()
                .orElseThrow(() -> new AssertionError("parent should have a popover"));

        parent.setVisible(false);

        // setVisible doesn't trigger onAttach/onDetach — the popover stays the
        // same instance and remains owned by the same item.
        assertTrue(parent.getPopover().isPresent());
        assertEquals(before, parent.getPopover().get());
    }

    @Test
    void itemSetVisibleTrueAfterFalseRestoresFullState() {
        SideNavRail nav = new SideNavRail();
        SideNavRailItem parent = parentWithChildren();
        nav.addItem(parent);
        UI.getCurrent().add(nav);
        nav.setRailMode(true);

        parent.setVisible(false);
        parent.setVisible(true);

        // ARIA attributes from rail mode should still be present (setVisible
        // doesn't drive a detach cycle, so they were never cleared).
        assertEquals("menu", parent.getElement().getAttribute("aria-haspopup"));
        Popover popover = parent.getPopover()
                .orElseThrow(() -> new AssertionError("parent should have a popover"));
        assertTrue(popover.isOpenOnHover(),
                "popover hover-eligibility must persist across setVisible cycle");
    }

    @Test
    void railSetVisibleFalseHidesAllItemsTransitively() {
        SideNavRail nav = new SideNavRail();
        SideNavRailItem dashboard = new SideNavRailItem("Dashboard", "/");
        SideNavRailItem parent = parentWithChildren();
        nav.addItem(dashboard);
        nav.addItem(parent);
        UI.getCurrent().add(nav);

        nav.setVisible(false);

        assertFalse(nav.isVisible());
        // Vaadin's setVisible cascades visually — children stay isVisible()=true
        // but inherit the hidden state from the parent. The contract here is
        // that flipping the rail off doesn't corrupt item-level state.
        assertTrue(dashboard.isVisible(), "child isVisible flag is unaffected");
        assertTrue(parent.isVisible(),    "child isVisible flag is unaffected");
    }

    @Test
    void railSetVisibleFalseDoesNotClearRailModeState() {
        SideNavRail nav = new SideNavRail();
        SideNavRailItem parent = parentWithChildren();
        nav.addItem(parent);
        UI.getCurrent().add(nav);
        nav.setRailMode(true);

        nav.setVisible(false);

        // Rail-mode state is server-side; setVisible doesn't undo it. ARIA
        // attributes from rail mode must still be on the item.
        assertEquals("menu", parent.getElement().getAttribute("aria-haspopup"));
        assertTrue(nav.isRailMode());
    }

    @Test
    void railSetVisibleTrueAfterFalseLeavesItemStateIntact() {
        SideNavRail nav = new SideNavRail();
        SideNavRailItem parent = parentWithChildren();
        nav.addItem(parent);
        UI.getCurrent().add(nav);
        nav.setRailMode(true);
        Popover popover = parent.getPopover()
                .orElseThrow(() -> new AssertionError("parent should have a popover"));
        int delayBefore = popover.getHoverDelay();

        nav.setVisible(false);
        nav.setVisible(true);

        assertEquals(delayBefore, popover.getHoverDelay(),
                "popover settings must survive a setVisible cycle on the rail");
        assertEquals("menu", parent.getElement().getAttribute("aria-haspopup"),
                "ARIA must survive a setVisible cycle on the rail");
    }

    @Test
    void itemSetVisibleFalseInRailModeKeepsRailToggleResponsive() {
        SideNavRail nav = new SideNavRail();
        SideNavRailItem parent = parentWithChildren();
        nav.addItem(parent);
        UI.getCurrent().add(nav);
        nav.setRailMode(true);

        parent.setVisible(false);

        nav.setRailMode(false);
        // Toggling rail mode on a hidden item must still update its server-side
        // state — when the user later flips visibility back on, the state is right.
        assertFalse(parent.getElement().hasAttribute("aria-haspopup"),
                "rail-mode toggle must reach hidden items");

        nav.setRailMode(true);
        assertEquals("menu", parent.getElement().getAttribute("aria-haspopup"),
                "rail-mode re-toggle must reach hidden items");
    }
}
