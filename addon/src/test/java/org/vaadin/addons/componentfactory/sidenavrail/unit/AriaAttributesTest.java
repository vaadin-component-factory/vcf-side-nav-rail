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

import com.github.mvysny.kaributesting.v10.MockVaadin;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.sidenav.SideNavItem;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.vaadin.addons.componentfactory.sidenavrail.SideNavRail;
import org.vaadin.addons.componentfactory.sidenavrail.SideNavRailItem;

/**
 * §9.2 / §4.4.5: aria-haspopup + aria-expanded on root items with children while
 * rail mode is active; cleared on exit. role="menuitem" on popover items.
 */
class AriaAttributesTest {

    @BeforeEach void setUp() { MockVaadin.setup(); }
    @AfterEach void tearDown() { MockVaadin.tearDown(); }

    @Test
    void noAriaAttributesInNormalMode() {
        SideNavRail nav = parentAndLeafRail();
        UI.getCurrent().add(nav);

        for (SideNavItem item : nav.getItems()) {
            assertFalse(item.getElement().hasAttribute("aria-haspopup"));
            assertFalse(item.getElement().hasAttribute("aria-expanded"));
        }
    }

    @Test
    void railModeSetsHaspopupOnParentOnly() {
        SideNavRail nav = parentAndLeafRail();
        UI.getCurrent().add(nav);

        nav.setRailMode(true);

        assertEquals("menu", nav.getItems().get(0).getElement().getAttribute("aria-haspopup"));
        assertFalse(nav.getItems().get(1).getElement().hasAttribute("aria-haspopup"));
    }

    @Test
    void railModeSetsAriaExpandedFalseInitially() {
        SideNavRail nav = parentAndLeafRail();
        UI.getCurrent().add(nav);

        nav.setRailMode(true);

        assertEquals("false", nav.getItems().get(0).getElement().getAttribute("aria-expanded"));
    }

    @Test
    void leavingRailModeClearsAriaAttributes() {
        SideNavRail nav = parentAndLeafRail();
        UI.getCurrent().add(nav);

        nav.setRailMode(true);
        nav.setRailMode(false);

        SideNavItem parent = nav.getItems().get(0);
        assertFalse(parent.getElement().hasAttribute("aria-haspopup"));
        assertFalse(parent.getElement().hasAttribute("aria-expanded"));
    }

    @Test
    void ariaExpandedTracksPopoverOpenState() {
        SideNavRail nav = parentAndLeafRail();
        UI.getCurrent().add(nav);
        nav.setRailMode(true);

        SideNavRailItem parent = (SideNavRailItem) nav.getItems().get(0);

        parent.syncAriaExpanded(true);
        assertEquals("true", parent.getElement().getAttribute("aria-expanded"));

        parent.syncAriaExpanded(false);
        assertEquals("false", parent.getElement().getAttribute("aria-expanded"));
    }

    @Test
    void syncAriaExpandedReappliesHaspopupMenu() {
        // Reproduces the §9.4 regression: the stock <vaadin-side-nav-item> web
        // component overwrites aria-haspopup to the generic "true" whenever the
        // popover opens. The listener must put "menu" back on every transition.
        SideNavRail nav = parentAndLeafRail();
        UI.getCurrent().add(nav);
        nav.setRailMode(true);

        SideNavRailItem parent = (SideNavRailItem) nav.getItems().get(0);
        assertEquals("menu", parent.getElement().getAttribute("aria-haspopup"));

        // Simulate Vaadin's client-side override on popover open.
        parent.getElement().setAttribute("aria-haspopup", "true");

        parent.syncAriaExpanded(true);
        assertEquals("menu", parent.getElement().getAttribute("aria-haspopup"));
        assertEquals("true", parent.getElement().getAttribute("aria-expanded"));

        // Same on close.
        parent.getElement().setAttribute("aria-haspopup", "true");
        parent.syncAriaExpanded(false);
        assertEquals("menu", parent.getElement().getAttribute("aria-haspopup"));
        assertEquals("false", parent.getElement().getAttribute("aria-expanded"));
    }

    @Test
    void addingItemWhileRailModeActiveAppliesAria() {
        SideNavRail nav = new SideNavRail();
        UI.getCurrent().add(nav);
        nav.setRailMode(true);

        SideNavRailItem parent = new SideNavRailItem("Late", "/late");
        parent.addItem(new SideNavRailItem("Sub", "/late/sub"));
        nav.addItem(parent);

        assertEquals("menu", parent.getElement().getAttribute("aria-haspopup"));
    }

    @Test
    void popoverItemsReceiveMenuitemRole() {
        SideNavRail nav = parentAndLeafRail();
        UI.getCurrent().add(nav);
        nav.setRailMode(true);

        // Walk the popover's nested SideNav contents — every item should have role=menuitem.
        SideNavRailItem parent = (SideNavRailItem) nav.getItems().get(0);
        com.vaadin.flow.component.popover.Popover popover = parent.getPopover().orElseThrow();
        com.vaadin.flow.component.sidenav.SideNav nested =
                (com.vaadin.flow.component.sidenav.SideNav) popover.getChildren()
                        .filter(c -> c instanceof com.vaadin.flow.component.sidenav.SideNav)
                        .findFirst().orElseThrow();
        nested.getItems().forEach(i ->
                assertEquals("menuitem", i.getElement().getAttribute("role")));
    }

    private static SideNavRail parentAndLeafRail() {
        SideNavRail nav = new SideNavRail();
        SideNavRailItem parent = new SideNavRailItem("Code", "/code");
        parent.addItem(new SideNavRailItem("Branches", "/code/branches"));
        SideNavRailItem leaf = new SideNavRailItem("Dashboard", "/");
        nav.addItem(parent, leaf);
        return nav;
    }
}
