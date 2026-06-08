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
import org.vaadin.addons.componentfactory.sidenavrail.PopoverOn;
import org.vaadin.addons.componentfactory.sidenavrail.SideNavRail;
import org.vaadin.addons.componentfactory.sidenavrail.SideNavRailItem;

/**
 * Verifies the contract of {@link SideNavRail#setChildrenOnlyInPopover(boolean)}: the {@code
 * inline-children-hidden} theme is added to / removed from the rail element so the bundled CSS
 * hides nested items, and popover gating ignores the item's expanded state while the flag is on.
 */
class ChildrenOnlyInPopoverTest {

    @BeforeEach
    void setUp() {
        MockVaadin.setup();
    }

    @AfterEach
    void tearDown() {
        MockVaadin.tearDown();
    }

    @Test
    void disabledByDefault() {
        SideNavRail nav = new SideNavRail();
        assertFalse(nav.isChildrenOnlyInPopover());
        assertFalse(nav.getElement().getThemeList().contains("inline-children-hidden"));
    }

    @Test
    void enablingAddsTheme() {
        SideNavRail nav = new SideNavRail();
        UI.getCurrent().add(nav);

        nav.setChildrenOnlyInPopover(true);

        assertTrue(nav.isChildrenOnlyInPopover());
        assertTrue(nav.getElement().getThemeList().contains("inline-children-hidden"));
    }

    @Test
    void disablingRemovesTheme() {
        SideNavRail nav = new SideNavRail();
        UI.getCurrent().add(nav);
        nav.setChildrenOnlyInPopover(true);

        nav.setChildrenOnlyInPopover(false);

        assertFalse(nav.isChildrenOnlyInPopover());
        assertFalse(nav.getElement().getThemeList().contains("inline-children-hidden"));
    }

    @Test
    void railModeAndChildrenOnlyInPopoverCoexistOnTheme() {
        // setRailMode used to setAttribute("theme", "rail") which would clobber any
        // other theme. Both knobs must layer additively on the theme list.
        SideNavRail nav = new SideNavRail();
        UI.getCurrent().add(nav);

        nav.setChildrenOnlyInPopover(true);
        nav.setRailMode(true);

        assertTrue(nav.getElement().getThemeList().contains("rail"));
        assertTrue(nav.getElement().getThemeList().contains("inline-children-hidden"));
    }

    @Test
    void popoverStaysEligibleEvenWhenItemIsExpandedInline() {
        // In children-only-popover mode, Vaadin's auto-expand-on-route-match still
        // flips item.expanded=true, but the inline tree is CSS-hidden. Popover
        // gating must therefore not close the popover just because the item is
        // marked expanded — the popover is the sole way to access the children.
        SideNavRail nav = parentLeafRail();
        UI.getCurrent().add(nav);
        nav.setChildrenOnlyInPopover(true);

        SideNavRailItem parent = (SideNavRailItem) nav.getItems().get(0);
        Popover popover = parent.getPopover().orElseThrow();
        // Default state — popover eligible.
        assertTrue(popover.isOpenOnHover());

        parent.setExpanded(true);
        // applyPopoverGating is invoked from setRailMode/setPopoverOn/etc.; trigger
        // it explicitly here to mirror what would happen on a real expand event.
        nav.setChildrenOnlyInPopover(true);
        assertTrue(
                popover.isOpenOnHover(),
                "popover must remain hover-eligible even when parent is expanded "
                        + "while children-only-in-popover is on");
    }

    @Test
    void inExplicitlyOnlyRailModePopoverStillIneligibleInNormalMode_whenChildrenOnlyOff() {
        // Sanity: children-only-popover is the only knob that bypasses the
        // PopoverOn switch; with childrenOnlyInPopover=false, ONLY_RAIL_MODE
        // still gates popovers to rail mode only (regression guard).
        SideNavRail nav = parentLeafRail();
        UI.getCurrent().add(nav);
        nav.setPopoverOn(PopoverOn.ONLY_RAIL_MODE);

        SideNavRailItem parent = (SideNavRailItem) nav.getItems().get(0);
        Popover popover = parent.getPopover().orElseThrow();
        assertFalse(popover.isOpenOnHover());
    }

    private static SideNavRail parentLeafRail() {
        SideNavRail nav = new SideNavRail();
        SideNavRailItem parent = new SideNavRailItem("Code", "/code");
        parent.addItem(new SideNavRailItem("Branches", "/code/branches"));
        nav.addItem(parent);
        return nav;
    }
}
