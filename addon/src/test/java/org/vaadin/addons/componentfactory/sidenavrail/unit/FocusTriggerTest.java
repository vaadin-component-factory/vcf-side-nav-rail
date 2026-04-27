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
 * §4.2 / §4.4.4: popover auto-opens on keyboard focus only while rail mode is active.
 * Outside rail mode, focus-triggered opening is disabled — the popover stays hover-only.
 */
class FocusTriggerTest {

    @BeforeEach void setUp() { MockVaadin.setup(); }
    @AfterEach void tearDown() { MockVaadin.tearDown(); }

    @Test
    void openOnFocusFalseInNormalMode() {
        SideNavRail nav = railWithParent();
        UI.getCurrent().add(nav);

        Popover popover = ((SideNavRailItem) nav.getItems().get(0)).getPopover().orElseThrow();
        assertFalse(popover.isOpenOnFocus());
    }

    @Test
    void openOnFocusTrueInRailMode() {
        SideNavRail nav = railWithParent();
        UI.getCurrent().add(nav);

        nav.setRailMode(true);

        Popover popover = ((SideNavRailItem) nav.getItems().get(0)).getPopover().orElseThrow();
        assertTrue(popover.isOpenOnFocus());
    }

    @Test
    void leavingRailModeRestoresOpenOnFocusFalse() {
        SideNavRail nav = railWithParent();
        UI.getCurrent().add(nav);

        nav.setRailMode(true);
        nav.setRailMode(false);

        Popover popover = ((SideNavRailItem) nav.getItems().get(0)).getPopover().orElseThrow();
        assertFalse(popover.isOpenOnFocus());
    }

    private static SideNavRail railWithParent() {
        SideNavRail nav = new SideNavRail();
        SideNavRailItem parent = new SideNavRailItem("Code", "/code");
        parent.addItem(new SideNavRailItem("Branches", "/code/branches"));
        nav.addItem(parent);
        return nav;
    }
}
