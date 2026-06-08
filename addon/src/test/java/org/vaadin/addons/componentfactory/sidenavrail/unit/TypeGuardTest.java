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
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vaadin.flow.component.sidenav.SideNavItem;
import org.junit.jupiter.api.Test;
import org.vaadin.addons.componentfactory.sidenavrail.SideNavRail;
import org.vaadin.addons.componentfactory.sidenavrail.SideNavRailItem;

/**
 * Guards that {@link SideNavRail} and {@link SideNavRailItem} reject plain {@link SideNavItem}
 * children. A plain item lacks the label wrap and popover gating and would silently misbehave, so
 * we fail fast at add-time.
 */
class TypeGuardTest {

    @Test
    void sideNavRail_rejectsPlainSideNavItem_inAddItem() {
        SideNavRail nav = new SideNavRail();
        IllegalArgumentException ex =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> nav.addItem(new SideNavItem("Plain", "/plain")));
        assertTrue(
                ex.getMessage().contains("SideNavRailItem"),
                "Error message should point the user at SideNavRailItem; got: " + ex.getMessage());
    }

    @Test
    void sideNavRail_rejectsPlainSideNavItem_inAddItemAsFirst() {
        SideNavRail nav = new SideNavRail();
        assertThrows(
                IllegalArgumentException.class,
                () -> nav.addItemAsFirst(new SideNavItem("Plain", "/plain")));
    }

    @Test
    void sideNavRail_rejectsPlainSideNavItem_amongMultiple() {
        SideNavRail nav = new SideNavRail();
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        nav.addItem(
                                new SideNavRailItem("First", "/first"),
                                new SideNavItem("Plain", "/plain"),
                                new SideNavRailItem("Third", "/third")));
    }

    @Test
    void sideNavRail_acceptsSideNavRailItem() {
        SideNavRail nav = new SideNavRail();
        SideNavRailItem a = new SideNavRailItem("A", "/a");
        SideNavRailItem b = new SideNavRailItem("B", "/b");
        nav.addItem(a, b);
        assertEquals(2, nav.getItems().size());
    }

    @Test
    void sideNavRailItem_rejectsPlainSideNavItemAsChild() {
        SideNavRailItem parent = new SideNavRailItem("Parent");
        assertThrows(
                IllegalArgumentException.class,
                () -> parent.addItem(new SideNavItem("Plain", "/plain")));
        assertThrows(
                IllegalArgumentException.class,
                () -> parent.addItemAsFirst(new SideNavItem("Plain", "/plain")));
    }

    @Test
    void sideNavRail_rejectsPlainSideNavItem_inAddItemAtIndex() {
        SideNavRail nav = new SideNavRail();
        nav.addItem(new SideNavRailItem("Existing", "/existing"));
        assertThrows(
                IllegalArgumentException.class,
                () -> nav.addItemAtIndex(0, new SideNavItem("Plain", "/plain")));
    }

    @Test
    void sideNavRailItem_rejectsPlainSideNavItem_inAddItemAtIndex() {
        SideNavRailItem parent = new SideNavRailItem("Parent");
        parent.addItem(new SideNavRailItem("Existing", "/existing"));
        assertThrows(
                IllegalArgumentException.class,
                () -> parent.addItemAtIndex(0, new SideNavItem("Plain", "/plain")));
    }

    @Test
    void sideNavRailItem_acceptsSideNavRailItemAsChild() {
        SideNavRailItem parent = new SideNavRailItem("Parent");
        parent.addItem(new SideNavRailItem("Child", "/child"));
        assertEquals(1, parent.getItems().size());
    }
}
