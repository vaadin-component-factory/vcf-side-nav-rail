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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.vaadin.addons.componentfactory.sidenavrail.PopoverOn;
import org.vaadin.addons.componentfactory.sidenavrail.SideNavRail;
import org.vaadin.addons.componentfactory.sidenavrail.SideNavRailItem;

class PopoverOnTest {

    @BeforeEach
    void setUp() {
        MockVaadin.setup();
    }

    @AfterEach
    void tearDown() {
        MockVaadin.tearDown();
    }

    @Test
    void defaultModeIsAllCollapsedItems() {
        SideNavRail nav = new SideNavRail();
        assertEquals(PopoverOn.ALL_COLLAPSED_ITEMS, nav.getPopoverOn());
    }

    @Test
    void modeCanBeChanged() {
        SideNavRail nav = new SideNavRail();
        nav.setPopoverOn(PopoverOn.ONLY_RAIL_MODE);
        assertEquals(PopoverOn.ONLY_RAIL_MODE, nav.getPopoverOn());
        nav.setPopoverOn(PopoverOn.ONLY_ROOT_COLLAPSED_ITEMS);
        assertEquals(PopoverOn.ONLY_ROOT_COLLAPSED_ITEMS, nav.getPopoverOn());
    }

    @Test
    void settingNullIsRejected() {
        SideNavRail nav = new SideNavRail();
        assertThrows(NullPointerException.class, () -> nav.setPopoverOn(null));
    }

    @Test
    void allCollapsedItemsMode_enablesHoverRegardlessOfRailMode() {
        SideNavRail nav = new SideNavRail();   // default ALL_COLLAPSED_ITEMS
        SideNavRailItem parent = new SideNavRailItem("Code");
        parent.addItem(new SideNavRailItem("Branches", "/branches"));
        nav.addItem(parent);
        UI.getCurrent().add(nav);

        Popover p = locatePopover(parent);
        assertEquals(true, p.isOpenOnHover(),
                "Popover should be hover-open-eligible in ALL_COLLAPSED_ITEMS mode");
    }

    @Test
    void onlyRailMode_disablesHoverUntilRailEngaged() {
        SideNavRail nav = new SideNavRail();
        nav.setPopoverOn(PopoverOn.ONLY_RAIL_MODE);
        SideNavRailItem parent = new SideNavRailItem("Code");
        parent.addItem(new SideNavRailItem("Branches", "/branches"));
        nav.addItem(parent);
        UI.getCurrent().add(nav);

        Popover p = locatePopover(parent);
        assertEquals(false, p.isOpenOnHover(),
                "ONLY_RAIL_MODE in normal nav — hover disabled");

        nav.setRailMode(true);
        assertEquals(true, p.isOpenOnHover(),
                "ONLY_RAIL_MODE engaged — hover enabled");

        nav.setRailMode(false);
        assertEquals(false, p.isOpenOnHover(),
                "ONLY_RAIL_MODE disengaged — hover disabled again");
    }

    @Test
    void onlyRootCollapsedItems_rootItemIsEligible() {
        SideNavRail nav = new SideNavRail();
        nav.setPopoverOn(PopoverOn.ONLY_ROOT_COLLAPSED_ITEMS);
        SideNavRailItem root = new SideNavRailItem("Code");
        root.addItem(new SideNavRailItem("Branches", "/branches"));
        nav.addItem(root);
        UI.getCurrent().add(nav);

        Popover p = locatePopover(root);
        assertEquals(true, p.isOpenOnHover(),
                "Root item with children is eligible for popover in ONLY_ROOT_COLLAPSED_ITEMS");
    }

    @Test
    void onlyRootCollapsedItems_nestedItemIsNotEligible() {
        SideNavRail nav = new SideNavRail();
        nav.setPopoverOn(PopoverOn.ONLY_ROOT_COLLAPSED_ITEMS);

        SideNavRailItem root = new SideNavRailItem("Code");
        SideNavRailItem branches = new SideNavRailItem("Branches", "/branches");
        branches.addItem(new SideNavRailItem("Active", "/branches/active"));
        root.addItem(branches);
        nav.addItem(root);
        UI.getCurrent().add(nav);

        // root gets a popover, branches (nested) should NOT
        Popover nestedPopover = locatePopoverOrNull(branches);
        if (nestedPopover != null) {
            assertEquals(false, nestedPopover.isOpenOnHover(),
                    "Nested item must not be hover-eligible in ONLY_ROOT_COLLAPSED_ITEMS");
        }
    }

    @Test
    void onlyRootCollapsedItems_closesPopoverWhenModeSwitches() {
        SideNavRail nav = new SideNavRail();
        SideNavRailItem root = new SideNavRailItem("Code");
        SideNavRailItem branches = new SideNavRailItem("Branches", "/branches");
        branches.addItem(new SideNavRailItem("Active", "/branches/active"));
        root.addItem(branches);
        nav.addItem(root);
        UI.getCurrent().add(nav);

        // Default ALL_COLLAPSED_ITEMS: nested item is eligible.
        Popover nestedPopover = locatePopover(branches);
        assertEquals(true, nestedPopover.isOpenOnHover(),
                "Nested item eligible in default mode");

        // Switching to ONLY_ROOT_COLLAPSED_ITEMS must disqualify it.
        nav.setPopoverOn(PopoverOn.ONLY_ROOT_COLLAPSED_ITEMS);
        assertEquals(false, nestedPopover.isOpenOnHover(),
                "Nested item no longer eligible after mode switch");
    }

    private static Popover locatePopover(SideNavRailItem item) {
        Popover p = locatePopoverOrNull(item);
        if (p == null) {
            throw new AssertionError("No popover found targeting " + item);
        }
        return p;
    }

    private static Popover locatePopoverOrNull(SideNavRailItem item) {
        return UI.getCurrent().getChildren()
                .filter(c -> c instanceof Popover)
                .map(c -> (Popover) c)
                .filter(p -> p.getTarget() == item)
                .findFirst()
                .orElse(null);
    }
}
