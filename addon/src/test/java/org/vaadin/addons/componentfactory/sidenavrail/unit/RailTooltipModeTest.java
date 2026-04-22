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
import static org.junit.jupiter.api.Assertions.assertFalse;
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
 * Verifies the {@link RailTooltipMode} behaviour: the rail sets the
 * {@code data-rail-tooltip} DOM attribute on eligible root items while rail mode is
 * active; CSS turns that attribute into a visible pseudo-element tooltip. We
 * moved away from Vaadin's native tooltip because the tooltip-mixin auto-dismisses
 * when a peer overlay (our hover popover) opens — a pure-CSS pseudo-element
 * doesn't participate in the overlay system and stays visible.
 */
class RailTooltipModeTest {

    private static final String ATTR = "data-rail-tooltip";

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
            assertFalse(item.getElement().hasAttribute(ATTR),
                    "No tooltip expected before rail mode is engaged on: " + item.getLabel());
        }
    }

    @Test
    void allModeSetsTooltipsOnAllRootItemsInRailMode() {
        SideNavRail nav = twoRootRail();  // "Dashboard" (leaf), "Code" (has children)
        UI.getCurrent().add(nav);

        nav.setRailMode(true);

        assertEquals("Dashboard", nav.getItems().get(0).getElement().getAttribute(ATTR));
        assertEquals("Code", nav.getItems().get(1).getElement().getAttribute(ATTR));
    }

    @Test
    void onlyWithoutChildrenSkipsParents() {
        SideNavRail nav = twoRootRail();
        nav.setRailTooltipMode(RailTooltipMode.ONLY_WITHOUT_CHILDREN);
        UI.getCurrent().add(nav);

        nav.setRailMode(true);

        assertEquals("Dashboard", nav.getItems().get(0).getElement().getAttribute(ATTR),
                "Leaf item must get a tooltip in ONLY_WITHOUT_CHILDREN");
        assertFalse(nav.getItems().get(1).getElement().hasAttribute(ATTR),
                "Item with children must NOT get a tooltip in ONLY_WITHOUT_CHILDREN");
    }

    @Test
    void noneSuppressesAllTooltips() {
        SideNavRail nav = twoRootRail();
        nav.setRailTooltipMode(RailTooltipMode.NONE);
        UI.getCurrent().add(nav);

        nav.setRailMode(true);

        for (SideNavItem item : nav.getItems()) {
            assertFalse(item.getElement().hasAttribute(ATTR),
                    "NONE must suppress tooltips even in rail mode: " + item.getLabel());
        }
    }

    @Test
    void leavingRailModeClearsTooltips() {
        SideNavRail nav = twoRootRail();
        UI.getCurrent().add(nav);

        nav.setRailMode(true);
        assertEquals("Dashboard", nav.getItems().get(0).getElement().getAttribute(ATTR));

        nav.setRailMode(false);
        assertFalse(nav.getItems().get(0).getElement().hasAttribute(ATTR),
                "Leaving rail mode must clear the tooltip attribute");
    }

    @Test
    void liveModeSwitchRefreshesExistingTooltips() {
        SideNavRail nav = twoRootRail();
        UI.getCurrent().add(nav);

        nav.setRailMode(true);
        assertEquals("Code", nav.getItems().get(1).getElement().getAttribute(ATTR));

        nav.setRailTooltipMode(RailTooltipMode.ONLY_WITHOUT_CHILDREN);
        assertFalse(nav.getItems().get(1).getElement().hasAttribute(ATTR),
                "Switching to ONLY_WITHOUT_CHILDREN must drop the tooltip on parents");

        nav.setRailTooltipMode(RailTooltipMode.ALL);
        assertEquals("Code", nav.getItems().get(1).getElement().getAttribute(ATTR),
                "Switching back to ALL must restore the tooltip");
    }

    @Test
    void setLabelUpdatesActiveTooltip() {
        SideNavRail nav = twoRootRail();
        UI.getCurrent().add(nav);
        nav.setRailMode(true);

        SideNavRailItem dashboard = (SideNavRailItem) nav.getItems().get(0);
        assertEquals("Dashboard", dashboard.getElement().getAttribute(ATTR));

        dashboard.setLabel("Home");
        assertEquals("Home", dashboard.getElement().getAttribute(ATTR),
                "A relabelled root item must have its tooltip attribute refreshed");
    }

    @Test
    void nativeFlagSwitchesAttributeToTitle() {
        SideNavRail nav = twoRootRail();
        nav.setRailTooltipNative(true);
        UI.getCurrent().add(nav);
        nav.setRailMode(true);

        // Native mode uses the HTML title attribute instead of our custom data-*.
        assertFalse(nav.getItems().get(0).getElement().hasAttribute(ATTR),
                "Custom pseudo-element attribute must not be set in native mode");
        assertEquals("Dashboard",
                nav.getItems().get(0).getElement().getAttribute("title"));
    }

    @Test
    void switchingNativeFlagCleansUpTheOtherAttribute() {
        SideNavRail nav = twoRootRail();
        UI.getCurrent().add(nav);
        nav.setRailMode(true);

        // Start with pseudo-element attribute
        assertEquals("Dashboard", nav.getItems().get(0).getElement().getAttribute(ATTR));
        assertFalse(nav.getItems().get(0).getElement().hasAttribute("title"));

        // Flip to native — pseudo-element attr should be gone, title should appear
        nav.setRailTooltipNative(true);
        assertFalse(nav.getItems().get(0).getElement().hasAttribute(ATTR),
                "Flipping to native must clear the pseudo-element attribute");
        assertEquals("Dashboard", nav.getItems().get(0).getElement().getAttribute("title"));

        // Flip back — the reverse
        nav.setRailTooltipNative(false);
        assertEquals("Dashboard", nav.getItems().get(0).getElement().getAttribute(ATTR));
        assertFalse(nav.getItems().get(0).getElement().hasAttribute("title"),
                "Flipping back to custom must clear the native title");
    }

    @Test
    void nativeFlagDefaultIsFalse() {
        SideNavRail nav = new SideNavRail();
        assertFalse(nav.isRailTooltipNative());
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

        assertFalse(child.getElement().hasAttribute(ATTR),
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
}
