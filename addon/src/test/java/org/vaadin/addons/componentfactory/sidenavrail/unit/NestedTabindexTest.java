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
 * §9.2: nested items must not be keyboard-focusable while rail mode is active, since
 * they are visually hidden. Root items keep their normal tab-order participation.
 */
class NestedTabindexTest {

    @BeforeEach void setUp() { MockVaadin.setup(); }
    @AfterEach void tearDown() { MockVaadin.tearDown(); }

    @Test
    void nestedItemsHaveNoTabindexInNormalMode() {
        SideNavRail nav = twoLevelRail();
        UI.getCurrent().add(nav);

        SideNavItem child = nav.getItems().get(0).getItems().get(0);
        assertFalse(child.getElement().hasAttribute("tabindex"));
    }

    @Test
    void railModeSetsTabindexMinusOneOnNestedItems() {
        SideNavRail nav = twoLevelRail();
        UI.getCurrent().add(nav);

        nav.setRailMode(true);

        SideNavItem child = nav.getItems().get(0).getItems().get(0);
        assertEquals("-1", child.getElement().getAttribute("tabindex"));
    }

    @Test
    void rootItemsKeepTheirNaturalTabindex() {
        SideNavRail nav = twoLevelRail();
        UI.getCurrent().add(nav);

        nav.setRailMode(true);

        SideNavItem root = nav.getItems().get(0);
        assertFalse(root.getElement().hasAttribute("tabindex"));
    }

    @Test
    void leavingRailModeRestoresNestedFocusability() {
        SideNavRail nav = twoLevelRail();
        UI.getCurrent().add(nav);

        nav.setRailMode(true);
        nav.setRailMode(false);

        SideNavItem child = nav.getItems().get(0).getItems().get(0);
        assertFalse(child.getElement().hasAttribute("tabindex"));
    }

    private static SideNavRail twoLevelRail() {
        SideNavRail nav = new SideNavRail();
        SideNavRailItem parent = new SideNavRailItem("Code", "/code");
        parent.addItem(new SideNavRailItem("Branches", "/code/branches"));
        parent.addItem(new SideNavRailItem("Commits", "/code/commits"));
        nav.addItem(parent);
        return nav;
    }
}
