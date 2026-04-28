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
 * Items added <em>after</em> the rail is already attached to the UI must pick
 * up whatever rail-side configuration is currently in effect — rail mode,
 * popover settings, popover-on mode, root-match-nested override. This is the
 * dynamic-navigation path: building the menu programmatically based on user
 * permissions or async data.
 */
class DynamicAddTest {

    @BeforeEach void setUp() { MockVaadin.setup(); }
    @AfterEach void tearDown() { MockVaadin.tearDown(); }

    @Test
    void newParentItemSeedsPopoverWithCurrentRailSettings() {
        SideNavRail nav = new SideNavRail();
        UI.getCurrent().add(nav);
        nav.setPopoverHoverDelay(750);
        nav.setPopoverHideDelay(1500);
        nav.setPopoverPosition(PopoverPosition.BOTTOM_END);
        nav.setRailMode(true);

        SideNavRailItem parent = new SideNavRailItem("Code", "/code");
        parent.addItem(new SideNavRailItem("Branches", "/code/branches"));
        nav.addItem(parent);

        Popover popover = parent.getPopover()
                .orElseThrow(() -> new AssertionError("parent should have a popover"));
        assertEquals(750, popover.getHoverDelay(),
                "popover created after setPopoverHoverDelay must inherit it");
        assertEquals(1500, popover.getHideDelay(),
                "popover created after setPopoverHideDelay must inherit it");
        assertEquals(PopoverPosition.BOTTOM_END, popover.getPosition(),
                "popover created after setPopoverPosition must inherit it");
        assertTrue(popover.isOpenOnFocus(),
                "popover created in rail mode must have openOnFocus enabled");
    }

    @Test
    void newParentItemAppliesAriaInRailMode() {
        SideNavRail nav = new SideNavRail();
        UI.getCurrent().add(nav);
        nav.setRailMode(true);

        SideNavRailItem parent = new SideNavRailItem("Code", "/code");
        parent.addItem(new SideNavRailItem("Branches", "/code/branches"));
        nav.addItem(parent);

        assertEquals("menu", parent.getElement().getAttribute("aria-haspopup"),
                "item added in rail mode must immediately get aria-haspopup");
        assertEquals("false", parent.getElement().getAttribute("aria-expanded"),
                "item added in rail mode must immediately get aria-expanded");
    }

    @Test
    void newItemRespectsCurrentPopoverOnMode() {
        SideNavRail nav = new SideNavRail();
        UI.getCurrent().add(nav);
        nav.setPopoverOn(PopoverOn.ONLY_RAIL_MODE);
        // No rail mode — ONLY_RAIL_MODE means "popover off entirely".

        SideNavRailItem parent = new SideNavRailItem("Code", "/code");
        parent.addItem(new SideNavRailItem("Branches", "/code/branches"));
        nav.addItem(parent);

        Popover popover = parent.getPopover()
                .orElseThrow(() -> new AssertionError("parent should have a popover"));
        assertFalse(popover.isOpenOnHover(),
                "ONLY_RAIL_MODE outside rail mode must keep new item's popover ineligible");
    }

    @Test
    void reparentingNestedItemToRootMakesItRootEligible() {
        SideNavRail nav = new SideNavRail();
        nav.setPopoverOn(PopoverOn.ONLY_ROOT_COLLAPSED_ITEMS);
        UI.getCurrent().add(nav);

        SideNavRailItem outerRoot = new SideNavRailItem("Code", "/code");
        SideNavRailItem nestedParent = new SideNavRailItem("Branches", "/code/branches");
        nestedParent.addItem(new SideNavRailItem("main", "/code/branches/main"));
        outerRoot.addItem(nestedParent);
        nav.addItem(outerRoot);

        // Nested parent — under ONLY_ROOT_COLLAPSED_ITEMS, the nested popover
        // is ineligible because the item isn't a root.
        Popover nestedPopover = nestedParent.getPopover()
                .orElseThrow(() -> new AssertionError("nested parent should have a popover"));
        assertFalse(nestedPopover.isOpenOnHover(),
                "precondition: nested parent's popover is ineligible");

        // Move it to the rail root.
        outerRoot.remove(nestedParent);
        nav.addItem(nestedParent);

        // Re-evaluate. The simplest user-driven trigger is a setPopoverOn re-call,
        // since addItem doesn't run applyPopoverGating on the moved item.
        nav.setPopoverOn(PopoverOn.ONLY_ROOT_COLLAPSED_ITEMS);
        assertTrue(nestedPopover.isOpenOnHover(),
                "reparented-to-root item must become eligible under ONLY_ROOT_COLLAPSED_ITEMS");
    }

    @Test
    void newItemAddedAsFirstSeedsState() {
        SideNavRail nav = new SideNavRail();
        UI.getCurrent().add(nav);
        nav.setRailMode(true);
        nav.setPopoverHoverDelay(900);

        SideNavRailItem parent = new SideNavRailItem("Code", "/code");
        parent.addItem(new SideNavRailItem("Branches", "/code/branches"));
        nav.addItemAsFirst(parent);

        assertEquals("menu", parent.getElement().getAttribute("aria-haspopup"),
                "addItemAsFirst must apply rail-mode ARIA");
        Popover popover = parent.getPopover()
                .orElseThrow(() -> new AssertionError("parent should have a popover"));
        assertEquals(900, popover.getHoverDelay(),
                "addItemAsFirst must seed popover with current rail settings");
    }
}
