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
import org.junit.jupiter.api.Test;
import org.vaadin.addons.componentfactory.sidenavrail.PopoverMode;
import org.vaadin.addons.componentfactory.sidenavrail.SideNavRail;
import org.vaadin.addons.componentfactory.sidenavrail.SideNavRailItem;

class PopoverModeTest {

    @Test
    void defaultModeIsCollapsedItem() {
        SideNavRail nav = new SideNavRail();
        assertEquals(PopoverMode.COLLAPSED_ITEM, nav.getPopoverMode());
    }

    @Test
    void modeCanBeChanged() {
        SideNavRail nav = new SideNavRail();
        nav.setPopoverMode(PopoverMode.RAIL_ONLY);
        assertEquals(PopoverMode.RAIL_ONLY, nav.getPopoverMode());
    }

    @Test
    void settingNullIsRejected() {
        SideNavRail nav = new SideNavRail();
        assertThrows(NullPointerException.class, () -> nav.setPopoverMode(null));
    }

    @Test
    void collapsedItemMode_enablesHoverRegardlessOfRailMode() {
        MockVaadin.setup();
        try {
            SideNavRail nav = new SideNavRail();   // default COLLAPSED_ITEM
            SideNavRailItem parent = new SideNavRailItem("Code");
            parent.addItem(new SideNavRailItem("Branches", "/branches"));
            nav.addItem(parent);
            UI.getCurrent().add(nav);

            Popover p = locatePopover(parent);
            assertEquals(true, p.isOpenOnHover(),
                    "Popover should be hover-open-eligible in COLLAPSED_ITEM mode");
        } finally {
            MockVaadin.tearDown();
        }
    }

    @Test
    void railOnlyMode_disablesHoverUntilRailEngaged() {
        MockVaadin.setup();
        try {
            SideNavRail nav = new SideNavRail();
            nav.setPopoverMode(PopoverMode.RAIL_ONLY);
            SideNavRailItem parent = new SideNavRailItem("Code");
            parent.addItem(new SideNavRailItem("Branches", "/branches"));
            nav.addItem(parent);
            UI.getCurrent().add(nav);

            Popover p = locatePopover(parent);
            assertEquals(false, p.isOpenOnHover(),
                    "RAIL_ONLY mode in normal nav — hover disabled");

            nav.setRailMode(true);
            assertEquals(true, p.isOpenOnHover(),
                    "RAIL_ONLY mode engaged — hover enabled");

            nav.setRailMode(false);
            assertEquals(false, p.isOpenOnHover(),
                    "RAIL_ONLY mode disengaged — hover disabled again");
        } finally {
            MockVaadin.tearDown();
        }
    }

    private static Popover locatePopover(SideNavRailItem item) {
        return UI.getCurrent().getChildren()
                .flatMap(c -> c.getChildren())
                .filter(c -> c instanceof Popover)
                .map(c -> (Popover) c)
                .filter(p -> p.getTarget() == item)
                .findFirst()
                .orElseThrow();
    }
}
