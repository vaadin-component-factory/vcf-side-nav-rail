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
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import java.util.List;
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
    void addingChildToAttachedParentUpdatesPopoverContent() {
        SideNavRail nav = new SideNavRail();
        SideNavRailItem parent = new SideNavRailItem("Code", "/code");
        parent.addItem(new SideNavRailItem("Branches", "/code/branches"));
        nav.addItem(parent);
        UI.getCurrent().add(nav);

        // Sanity: popover already exists with one child copy.
        Popover popover = parent.getPopover()
                .orElseThrow(() -> new AssertionError("parent should have a popover"));
        assertEquals(1, popoverChildLabels(popover).size(),
                "precondition: popover starts with one child");

        // The bug: adding a child to an already-attached item used to leave the
        // popover stuck with the previous content.
        parent.addItem(new SideNavRailItem("Tags", "/code/tags"));

        List<String> labels = popoverChildLabels(popover);
        assertEquals(2, labels.size(), "popover must reflect the new child");
        assertTrue(labels.contains("Tags"), "popover must contain the newly added child");
    }

    @Test
    void addingGrandchildRefreshesAncestorPopoverContent() {
        // Regression: ancestor popovers mirror the full subtree via copyOf(child),
        // so adding a child to a nested item must rebuild the ancestor's popover
        // too. Without this, the dynamic-projects demo's "Projects" popover kept
        // showing the project leaves as childless even after a project was
        // activated and its sub-items appeared inline.
        SideNavRail nav = new SideNavRail();
        SideNavRailItem grandparent = new SideNavRailItem("Projects");
        SideNavRailItem child = new SideNavRailItem("Phoenix");
        grandparent.addItem(child);
        nav.addItem(grandparent);
        UI.getCurrent().add(nav);

        // Sanity: grandparent's popover has Phoenix as a childless leaf copy.
        Popover popover = grandparent.getPopover()
                .orElseThrow(() -> new AssertionError("grandparent should have a popover"));
        SideNav nested = popoverNestedSideNav(popover);
        assertEquals(1, nested.getItems().size());
        assertTrue(nested.getItems().get(0).getItems().isEmpty(),
                "precondition: Phoenix copy starts childless in popover");

        // Add grandchildren to Phoenix at runtime.
        child.addItem(new SideNavRailItem("Overview", "/projects/phoenix/overview"));

        // Ancestor popover must now show Phoenix WITH a child.
        SideNav refreshed = popoverNestedSideNav(popover);
        SideNavItem phoenixCopy = refreshed.getItems().get(0);
        assertEquals(1, phoenixCopy.getItems().size(),
                "grandchild must propagate into ancestor popover");
        assertEquals("Overview", phoenixCopy.getItems().get(0).getLabel());
    }

    private static SideNav popoverNestedSideNav(Popover popover) {
        return (SideNav) popover.getChildren()
                .filter(c -> c instanceof SideNav)
                .findFirst()
                .orElseThrow(() -> new AssertionError("popover must contain a nested SideNav"));
    }

    @Test
    void addingFirstChildToAttachedLeafMaterializesPopover() {
        SideNavRail nav = new SideNavRail();
        SideNavRailItem leaf = new SideNavRailItem("Code", "/code");
        nav.addItem(leaf);
        UI.getCurrent().add(nav);

        // Sanity: a leaf item with no children and no leaf-popover-active flag
        // has no popover.
        assertTrue(leaf.getPopover().isEmpty(),
                "precondition: leaf item has no popover");

        leaf.addItem(new SideNavRailItem("Branches", "/code/branches"));

        Popover popover = leaf.getPopover()
                .orElseThrow(() -> new AssertionError(
                        "first child added at runtime must materialize the popover"));
        assertEquals(1, popoverChildLabels(popover).size(),
                "newly materialized popover must contain the added child");
    }

    @Test
    void addItemAtIndexOnAttachedParentUpdatesPopoverContent() {
        SideNavRail nav = new SideNavRail();
        SideNavRailItem parent = new SideNavRailItem("Code", "/code");
        parent.addItem(new SideNavRailItem("Branches", "/code/branches"));
        parent.addItem(new SideNavRailItem("Tags", "/code/tags"));
        nav.addItem(parent);
        UI.getCurrent().add(nav);

        parent.addItemAtIndex(1, new SideNavRailItem("Commits", "/code/commits"));

        Popover popover = parent.getPopover()
                .orElseThrow(() -> new AssertionError("parent should have a popover"));
        List<String> labels = popoverChildLabels(popover);
        assertEquals(List.of("Branches", "Commits", "Tags"), labels,
                "addItemAtIndex must place the new child at the requested position");
    }

    @Test
    void removeOnAttachedParentUpdatesPopoverContent() {
        SideNavRail nav = new SideNavRail();
        SideNavRailItem parent = new SideNavRailItem("Code", "/code");
        SideNavRailItem branches = new SideNavRailItem("Branches", "/code/branches");
        SideNavRailItem tags = new SideNavRailItem("Tags", "/code/tags");
        parent.addItem(branches, tags);
        nav.addItem(parent);
        UI.getCurrent().add(nav);

        parent.remove(branches);

        Popover popover = parent.getPopover()
                .orElseThrow(() -> new AssertionError("parent should have a popover"));
        List<String> labels = popoverChildLabels(popover);
        assertEquals(List.of("Tags"), labels,
                "removed child must disappear from the popover");
    }

    @Test
    void removeAllOnAttachedParentEmptiesPopover() {
        SideNavRail nav = new SideNavRail();
        SideNavRailItem parent = new SideNavRailItem("Code", "/code");
        parent.addItem(new SideNavRailItem("Branches", "/code/branches"));
        parent.addItem(new SideNavRailItem("Tags", "/code/tags"));
        nav.addItem(parent);
        UI.getCurrent().add(nav);

        parent.removeAll();

        Popover popover = parent.getPopover()
                .orElseThrow(() -> new AssertionError("popover should still exist"));
        // After removeAll the popover's nested SideNav is no longer rendered;
        // populatePopover skips it when getItems() is empty.
        assertTrue(popover.getChildren().noneMatch(c -> c instanceof SideNav),
                "popover must contain no nested SideNav after removeAll");
    }

    @Test
    void addItemAsFirstOnAttachedParentUpdatesPopoverContent() {
        SideNavRail nav = new SideNavRail();
        SideNavRailItem parent = new SideNavRailItem("Code", "/code");
        parent.addItem(new SideNavRailItem("Branches", "/code/branches"));
        nav.addItem(parent);
        UI.getCurrent().add(nav);

        parent.addItemAsFirst(new SideNavRailItem("Tags", "/code/tags"));

        Popover popover = parent.getPopover()
                .orElseThrow(() -> new AssertionError("parent should have a popover"));
        List<String> labels = popoverChildLabels(popover);
        assertEquals(2, labels.size(), "popover must reflect the prepended child");
        assertEquals("Tags", labels.get(0),
                "addItemAsFirst must place the new child at the popover's start");
    }

    private static List<String> popoverChildLabels(Popover popover) {
        return popover.getChildren()
                .filter(c -> c instanceof SideNav)
                .findFirst()
                .map(c -> ((SideNav) c).getItems().stream()
                        .map(SideNavItem::getLabel)
                        .toList())
                .orElseThrow(() -> new AssertionError(
                        "popover must contain a SideNav with the children"));
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
