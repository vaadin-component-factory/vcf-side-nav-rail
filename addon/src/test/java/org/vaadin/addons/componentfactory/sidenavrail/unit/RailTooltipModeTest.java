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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.github.mvysny.kaributesting.v10.MockVaadin;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.sidenav.SideNavItem;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.vaadin.addons.componentfactory.sidenavrail.RailTooltipMode;
import org.vaadin.addons.componentfactory.sidenavrail.SideNavRail;
import org.vaadin.addons.componentfactory.sidenavrail.SideNavRailItem;

/**
 * Verifies the {@link RailTooltipMode} behaviour: tooltips are applied to root items
 * only while the rail is in rail mode, and the mode decides which items are eligible.
 */
class RailTooltipModeTest {

    @BeforeEach
    void setUp() {
        MockVaadin.setup();
    }

    @AfterEach
    void tearDown() {
        MockVaadin.tearDown();
    }

    @Test
    void defaultIsAll() {
        SideNavRail nav = new SideNavRail();
        assertEquals(RailTooltipMode.ALL, nav.getRailTooltipMode());
    }

    @Test
    void nullIsRejected() {
        SideNavRail nav = new SideNavRail();
        assertThrows(NullPointerException.class, () -> nav.setRailTooltipMode(null));
    }

    @Test
    void noTooltipInNormalMode() {
        SideNavRail nav = twoRootRail();
        UI.getCurrent().add(nav);

        for (SideNavItem item : nav.getItems()) {
            assertNull(tooltipTextOf(item),
                    "No tooltip expected before rail mode is engaged on: " + item.getLabel());
        }
    }

    @Test
    void allModeSetsTooltipsOnAllRootItemsInRailMode() {
        SideNavRail nav = twoRootRail();  // "Dashboard" (leaf), "Code" (has children)
        UI.getCurrent().add(nav);

        nav.setRailMode(true);

        assertEquals("Dashboard", tooltipTextOf(nav.getItems().get(0)));
        assertEquals("Code", tooltipTextOf(nav.getItems().get(1)));
    }

    @Test
    void onlyWithoutChildrenSkipsParents() {
        SideNavRail nav = twoRootRail();
        nav.setRailTooltipMode(RailTooltipMode.ONLY_WITHOUT_CHILDREN);
        UI.getCurrent().add(nav);

        nav.setRailMode(true);

        assertEquals("Dashboard", tooltipTextOf(nav.getItems().get(0)),
                "Leaf item must get a tooltip in ONLY_WITHOUT_CHILDREN");
        assertNull(tooltipTextOf(nav.getItems().get(1)),
                "Item with children must NOT get a tooltip in ONLY_WITHOUT_CHILDREN");
    }

    @Test
    void noneSuppressesAllTooltips() {
        SideNavRail nav = twoRootRail();
        nav.setRailTooltipMode(RailTooltipMode.NONE);
        UI.getCurrent().add(nav);

        nav.setRailMode(true);

        for (SideNavItem item : nav.getItems()) {
            assertNull(tooltipTextOf(item),
                    "NONE must suppress tooltips even in rail mode: " + item.getLabel());
        }
    }

    @Test
    void leavingRailModeClearsTooltips() {
        SideNavRail nav = twoRootRail();
        UI.getCurrent().add(nav);

        nav.setRailMode(true);
        assertEquals("Dashboard", tooltipTextOf(nav.getItems().get(0)));

        nav.setRailMode(false);
        assertNull(tooltipTextOf(nav.getItems().get(0)),
                "Leaving rail mode must clear the previously applied tooltip text");
    }

    @Test
    void liveModeSwitchRefreshesExistingTooltips() {
        SideNavRail nav = twoRootRail();
        UI.getCurrent().add(nav);

        nav.setRailMode(true);
        assertEquals("Code", tooltipTextOf(nav.getItems().get(1)));

        nav.setRailTooltipMode(RailTooltipMode.ONLY_WITHOUT_CHILDREN);
        assertNull(tooltipTextOf(nav.getItems().get(1)),
                "Switching to ONLY_WITHOUT_CHILDREN must drop the tooltip on items with children");

        nav.setRailTooltipMode(RailTooltipMode.ALL);
        assertEquals("Code", tooltipTextOf(nav.getItems().get(1)),
                "Switching back to ALL must restore the tooltip");
    }

    @Test
    void setLabelUpdatesActiveTooltip() {
        SideNavRail nav = twoRootRail();
        UI.getCurrent().add(nav);
        nav.setRailMode(true);

        SideNavRailItem dashboard = (SideNavRailItem) nav.getItems().get(0);
        assertEquals("Dashboard", tooltipTextOf(dashboard));

        dashboard.setLabel("Home");
        assertEquals("Home", tooltipTextOf(dashboard),
                "A relabelled root item must have its tooltip text refreshed");
    }

    @Test
    void nestedItemsDoNotReceiveTooltips() {
        SideNavRail nav = new SideNavRail();
        SideNavRailItem parent = new SideNavRailItem("Code", "/code");
        SideNavRailItem child = new SideNavRailItem("Branches", "/code/branches");
        parent.addItem(child);
        nav.addItem(parent);
        UI.getCurrent().add(nav);
        nav.setRailMode(true);

        assertNull(tooltipTextOf(child),
                "Nested items must not get a tooltip — the rail targets root items only");
    }

    private static SideNavRail twoRootRail() {
        SideNavRail nav = new SideNavRail();
        SideNavRailItem dashboard = new SideNavRailItem("Dashboard", "/");
        SideNavRailItem code = new SideNavRailItem("Code", "/code");
        code.addItem(new SideNavRailItem("Branches", "/code/branches"));
        nav.addItem(dashboard, code);
        return nav;
    }

    private static String tooltipTextOf(SideNavItem item) {
        if (item.getTooltip() == null) {
            return null;
        }
        String text = item.getTooltip().getText();
        return (text == null || text.isEmpty()) ? null : text;
    }
}
