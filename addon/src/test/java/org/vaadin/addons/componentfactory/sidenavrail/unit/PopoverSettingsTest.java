/*
 * Copyright 2026 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.vaadin.addons.componentfactory.sidenavrail.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.github.mvysny.kaributesting.v10.MockVaadin;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.popover.Popover;
import com.vaadin.flow.component.popover.PopoverPosition;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.vaadin.addons.componentfactory.sidenavrail.SideNavRail;
import org.vaadin.addons.componentfactory.sidenavrail.SideNavRailItem;

/**
 * Covers the nav-level configuration of popover timings and position: defaults,
 * null-guard on position, live propagation to existing popovers, and seeding of
 * popovers created after a settings change.
 */
class PopoverSettingsTest {

    @BeforeEach
    void setUp() {
        MockVaadin.setup();
    }

    @AfterEach
    void tearDown() {
        MockVaadin.tearDown();
    }

    @Test
    void defaults() {
        SideNavRail nav = new SideNavRail();
        assertEquals(200, nav.getPopoverHoverDelay());
        assertEquals(300, nav.getPopoverHideDelay());
        assertEquals(PopoverPosition.END_TOP, nav.getPopoverPosition());
    }

    @Test
    void nullPositionRejected() {
        SideNavRail nav = new SideNavRail();
        assertThrows(NullPointerException.class, () -> nav.setPopoverPosition(null));
    }

    @Test
    void newPopoverPicksUpConfiguredValues() {
        SideNavRail nav = new SideNavRail();
        nav.setPopoverHoverDelay(50);
        nav.setPopoverHideDelay(100);
        nav.setPopoverPosition(PopoverPosition.BOTTOM_START);

        SideNavRailItem parent = parentWithChildren();
        nav.addItem(parent);
        UI.getCurrent().add(nav);

        Popover popover = findPopover();
        assertEquals(50, popover.getHoverDelay());
        assertEquals(100, popover.getHideDelay());
        assertEquals(PopoverPosition.BOTTOM_START, popover.getPosition());
    }

    @Test
    void liveSwitchPropagatesToExistingPopover() {
        SideNavRail nav = new SideNavRail();
        SideNavRailItem parent = parentWithChildren();
        nav.addItem(parent);
        UI.getCurrent().add(nav);

        Popover popover = findPopover();
        assertEquals(200, popover.getHoverDelay(), "precondition: default hover delay");

        nav.setPopoverHoverDelay(10);
        assertEquals(10, popover.getHoverDelay(),
                "Changing the nav's hover delay must update the existing popover");

        nav.setPopoverHideDelay(20);
        assertEquals(20, popover.getHideDelay());

        nav.setPopoverPosition(PopoverPosition.BOTTOM);
        assertEquals(PopoverPosition.BOTTOM, popover.getPosition());
    }

    @Test
    void liveSwitchPropagatesToNestedPopovers() {
        // A nested parent (with children) has its own popover; the nav-level settings
        // must reach it too, not just the root-level popovers.
        SideNavRail nav = new SideNavRail();
        SideNavRailItem root = new SideNavRailItem("Root", "/root");
        SideNavRailItem nested = new SideNavRailItem("Nested", "/root/nested");
        nested.addItem(new SideNavRailItem("Deep", "/root/nested/deep"));
        root.addItem(nested);
        nav.addItem(root);
        UI.getCurrent().add(nav);

        nav.setPopoverHoverDelay(7);
        nav.setPopoverHideDelay(11);
        nav.setPopoverPosition(PopoverPosition.START);

        for (Popover popover : allPopoversInUi()) {
            assertEquals(7, popover.getHoverDelay(),
                    "All popovers, including nested, must receive the new hover delay");
            assertEquals(11, popover.getHideDelay());
            assertEquals(PopoverPosition.START, popover.getPosition());
        }
    }

    private static SideNavRailItem parentWithChildren() {
        SideNavRailItem parent = new SideNavRailItem("Code", "/code");
        parent.addItem(new SideNavRailItem("Branches", "/code/branches"));
        return parent;
    }

    private static Popover findPopover() {
        return UI.getCurrent().getChildren()
                .filter(c -> c instanceof Popover)
                .map(c -> (Popover) c)
                .findFirst()
                .orElseThrow(() -> new AssertionError("No popover attached to UI"));
    }

    private static java.util.List<Popover> allPopoversInUi() {
        return UI.getCurrent().getChildren()
                .filter(c -> c instanceof Popover)
                .map(c -> (Popover) c)
                .toList();
    }
}
