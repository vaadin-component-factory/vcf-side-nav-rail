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
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.mvysny.kaributesting.v10.MockVaadin;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.sidenav.SideNavItem;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.vaadin.addons.componentfactory.sidenavrail.PopoverHeaderMode;
import org.vaadin.addons.componentfactory.sidenavrail.RailTooltipMode;
import org.vaadin.addons.componentfactory.sidenavrail.SideNavRail;
import org.vaadin.addons.componentfactory.sidenavrail.SideNavRailItem;

/**
 * Verifies the {@link RailTooltipMode} behaviour: depending on the active mode the rail
 * sets either a {@code data-rail-tooltip} attribute (STYLED, default — CSS turns it
 * into a pseudo-element tooltip), the native {@code title} attribute (BROWSER_NATIVE),
 * neither (NONE / POPOVER — POPOVER is wired up via the parent / leaf popover, not via
 * an attribute). Tooltips are only applied while rail mode is engaged.
 */
class RailTooltipModeTest {

    private static final String STYLED_ATTR = "data-rail-tooltip";
    private static final String NATIVE_ATTR = "title";

    @BeforeEach
    void setUp() {
        MockVaadin.setup();
    }

    @AfterEach
    void tearDown() {
        MockVaadin.tearDown();
    }

    @Test
    void defaultIsStyled() {
        SideNavRail nav = new SideNavRail();
        assertEquals(RailTooltipMode.STYLED, nav.getRailTooltipMode());
    }

    @Test
    void styledSetsCustomAttributeInRailMode() {
        SideNavRail nav = railWithItem("Dashboard");
        UI.getCurrent().add(nav);
        nav.setRailMode(true);
        SideNavItem item = nav.getItems().get(0);
        assertEquals("Dashboard", item.getElement().getAttribute(STYLED_ATTR));
        assertFalse(item.getElement().hasAttribute(NATIVE_ATTR));
    }

    @Test
    void browserNativeSetsTitleAttributeInRailMode() {
        SideNavRail nav = railWithItem("Dashboard");
        nav.setRailTooltipMode(RailTooltipMode.BROWSER_NATIVE);
        UI.getCurrent().add(nav);
        nav.setRailMode(true);
        SideNavItem item = nav.getItems().get(0);
        assertEquals("Dashboard", item.getElement().getAttribute(NATIVE_ATTR));
        assertFalse(item.getElement().hasAttribute(STYLED_ATTR));
    }

    @Test
    void popoverModeClearsBothTooltipAttributes() {
        SideNavRail nav = railWithItem("Dashboard");
        nav.setPopoverHeaderMode(PopoverHeaderMode.LABEL_ONLY);  // valid combo
        nav.setRailTooltipMode(RailTooltipMode.POPOVER);
        UI.getCurrent().add(nav);
        nav.setRailMode(true);
        SideNavItem item = nav.getItems().get(0);
        assertFalse(item.getElement().hasAttribute(STYLED_ATTR));
        assertFalse(item.getElement().hasAttribute(NATIVE_ATTR));
    }

    @Test
    void noneRemovesBothAttributes() {
        SideNavRail nav = railWithItem("Dashboard");
        UI.getCurrent().add(nav);
        nav.setRailMode(true);
        nav.setRailTooltipMode(RailTooltipMode.NONE);
        SideNavItem item = nav.getItems().get(0);
        assertFalse(item.getElement().hasAttribute(STYLED_ATTR));
        assertFalse(item.getElement().hasAttribute(NATIVE_ATTR));
    }

    @Test
    void normalModeDoesNotShowTooltipRegardlessOfMode() {
        SideNavRail nav = railWithItem("Dashboard");
        UI.getCurrent().add(nav);
        nav.setRailMode(false);
        for (RailTooltipMode mode : RailTooltipMode.values()) {
            nav.setRailTooltipMode(mode);
            SideNavItem item = nav.getItems().get(0);
            assertFalse(item.getElement().hasAttribute(STYLED_ATTR));
            assertFalse(item.getElement().hasAttribute(NATIVE_ATTR));
        }
    }

    @Test
    void switchingFromNativeToStyledClearsTitleAttribute() {
        SideNavRail nav = railWithItem("Dashboard");
        nav.setRailTooltipMode(RailTooltipMode.BROWSER_NATIVE);
        UI.getCurrent().add(nav);
        nav.setRailMode(true);
        nav.setRailTooltipMode(RailTooltipMode.STYLED);
        SideNavItem item = nav.getItems().get(0);
        assertEquals("Dashboard", item.getElement().getAttribute(STYLED_ATTR));
        assertFalse(item.getElement().hasAttribute(NATIVE_ATTR));
    }

    @Test
    void nullModeThrows() {
        SideNavRail nav = new SideNavRail();
        assertThrows(NullPointerException.class, () -> nav.setRailTooltipMode(null));
    }

    @Test
    void blankLabelDoesNotProduceTooltipAttribute() {
        SideNavRail nav = railWithItem("");
        UI.getCurrent().add(nav);
        nav.setRailMode(true);
        SideNavItem item = nav.getItems().get(0);
        assertFalse(item.getElement().hasAttribute(STYLED_ATTR));
    }

    @Test
    void isLeafPopoverActiveReflectsRailModeAndPopoverMode() {
        SideNavRail nav = new SideNavRail();
        assertFalse(nav.isLeafPopoverActive());  // not in rail mode

        UI.getCurrent().add(nav);
        nav.setPopoverHeaderMode(PopoverHeaderMode.LABEL_ONLY);
        nav.setRailTooltipMode(RailTooltipMode.POPOVER);
        nav.setRailMode(true);
        assertTrue(nav.isLeafPopoverActive());

        nav.setRailMode(false);
        assertFalse(nav.isLeafPopoverActive());

        nav.setRailMode(true);
        nav.setRailTooltipMode(RailTooltipMode.STYLED);
        assertFalse(nav.isLeafPopoverActive());
    }

    private static SideNavRail railWithItem(String label) {
        SideNavRail nav = new SideNavRail();
        nav.addItem(new SideNavRailItem(label, "/x"));
        return nav;
    }
}
