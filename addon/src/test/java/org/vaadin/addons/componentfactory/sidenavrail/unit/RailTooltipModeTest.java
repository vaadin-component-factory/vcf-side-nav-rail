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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.mvysny.kaributesting.v10.MockVaadin;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.popover.Popover;
import com.vaadin.flow.component.sidenav.SideNavItem;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.vaadin.addons.componentfactory.sidenavrail.PopoverHeaderMode;
import org.vaadin.addons.componentfactory.sidenavrail.RailTooltipMode;
import org.vaadin.addons.componentfactory.sidenavrail.SideNavRail;
import org.vaadin.addons.componentfactory.sidenavrail.SideNavRailItem;

/**
 * Verifies the {@link RailTooltipMode} behaviour: the rail sets a {@code data-rail-tooltip}
 * attribute on every root item when {@code SIMPLE} is active and rail mode is engaged — CSS turns
 * that attribute into a Lumo-themed pseudo-element tooltip. {@code POPOVER_HEADER} (the default)
 * uses the popover (parent or leaf) instead, so no attribute is written. {@code NONE} suppresses
 * tooltips entirely. Tooltips are only applied while rail mode is engaged.
 */
class RailTooltipModeTest {

    private static final String TOOLTIP_ATTR = "data-rail-tooltip";

    @BeforeEach
    void setUp() {
        MockVaadin.setup();
    }

    @AfterEach
    void tearDown() {
        MockVaadin.tearDown();
    }

    @Test
    void defaultIsPopoverHeader() {
        SideNavRail nav = new SideNavRail();
        assertEquals(RailTooltipMode.POPOVER_HEADER, nav.getRailTooltipMode());
    }

    @Test
    void simpleSetsTooltipAttributeInRailMode() {
        SideNavRail nav = railWithItem("Dashboard");
        nav.setRailTooltipMode(RailTooltipMode.SIMPLE);
        UI.getCurrent().add(nav);
        nav.setRailMode(true);
        SideNavItem item = nav.getItems().get(0);
        assertEquals("Dashboard", item.getElement().getAttribute(TOOLTIP_ATTR));
    }

    @Test
    void popoverModeClearsTooltipAttribute() {
        SideNavRail nav = railWithItem("Dashboard");
        nav.setPopoverHeaderMode(PopoverHeaderMode.LABEL_ONLY);
        nav.setRailTooltipMode(RailTooltipMode.POPOVER_HEADER);
        UI.getCurrent().add(nav);
        nav.setRailMode(true);
        SideNavItem item = nav.getItems().get(0);
        assertFalse(item.getElement().hasAttribute(TOOLTIP_ATTR));
    }

    @Test
    void noneRemovesTooltipAttribute() {
        SideNavRail nav = railWithItem("Dashboard");
        UI.getCurrent().add(nav);
        nav.setRailMode(true);
        nav.setRailTooltipMode(RailTooltipMode.NONE);
        SideNavItem item = nav.getItems().get(0);
        assertFalse(item.getElement().hasAttribute(TOOLTIP_ATTR));
    }

    @Test
    void normalModeDoesNotShowTooltipRegardlessOfMode() {
        SideNavRail nav = railWithItem("Dashboard");
        UI.getCurrent().add(nav);
        nav.setRailMode(false);
        for (RailTooltipMode mode : RailTooltipMode.values()) {
            nav.setRailTooltipMode(mode);
            SideNavItem item = nav.getItems().get(0);
            assertFalse(item.getElement().hasAttribute(TOOLTIP_ATTR));
        }
    }

    @Test
    void switchingFromPopoverToSimpleSetsTooltipAttribute() {
        SideNavRail nav = railWithItem("Dashboard");
        nav.setPopoverHeaderMode(PopoverHeaderMode.LABEL_ONLY);
        nav.setRailTooltipMode(RailTooltipMode.POPOVER_HEADER);
        UI.getCurrent().add(nav);
        nav.setRailMode(true);
        nav.setRailTooltipMode(RailTooltipMode.SIMPLE);
        SideNavItem item = nav.getItems().get(0);
        assertEquals("Dashboard", item.getElement().getAttribute(TOOLTIP_ATTR));
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
        assertFalse(item.getElement().hasAttribute(TOOLTIP_ATTR));
    }

    @Test
    void isLeafPopoverActiveReflectsRailModeAndPopoverMode() {
        SideNavRail nav = new SideNavRail();
        assertFalse(nav.isLeafPopoverActive()); // not in rail mode

        UI.getCurrent().add(nav);
        nav.setPopoverHeaderMode(PopoverHeaderMode.LABEL_ONLY);
        nav.setRailTooltipMode(RailTooltipMode.POPOVER_HEADER);
        nav.setRailMode(true);
        assertTrue(nav.isLeafPopoverActive());

        nav.setRailMode(false);
        assertFalse(nav.isLeafPopoverActive());

        nav.setRailMode(true);
        nav.setRailTooltipMode(RailTooltipMode.SIMPLE);
        assertFalse(nav.isLeafPopoverActive());
    }

    // ---- Leaf-popover (RailTooltipMode.POPOVER_HEADER) tests ----

    @Test
    void popoverModeCreatesPopoverOnLeafInRailMode() {
        SideNavRail nav = new SideNavRail();
        SideNavRailItem leaf = new SideNavRailItem("Dashboard", "/dashboard");
        nav.addItem(leaf);
        nav.setPopoverHeaderMode(PopoverHeaderMode.LABEL_ONLY);
        nav.setRailTooltipMode(RailTooltipMode.POPOVER_HEADER);
        UI.getCurrent().add(nav);
        nav.setRailMode(true);

        Popover popover = findPopoverFor(leaf);
        assertNotNull(
                popover,
                "Expected a popover on the rail-mode leaf when POPOVER_HEADER mode is active");
        assertTrue(popover.isOpenOnHover(), "Leaf popover should be hover-triggered in rail mode");
    }

    @Test
    void leavingRailModeDisarmsLeafPopoverHover() {
        SideNavRail nav = new SideNavRail();
        SideNavRailItem leaf = new SideNavRailItem("Dashboard", "/dashboard");
        nav.addItem(leaf);
        nav.setPopoverHeaderMode(PopoverHeaderMode.LABEL_ONLY);
        nav.setRailTooltipMode(RailTooltipMode.POPOVER_HEADER);
        UI.getCurrent().add(nav);
        nav.setRailMode(true);
        nav.setRailMode(false);

        Popover popover = findPopoverFor(leaf);
        if (popover != null) {
            // Implementation may keep the instance around; if so, hover trigger must be off.
            assertFalse(
                    popover.isOpenOnHover(),
                    "Leaf popover hover trigger must be disarmed when leaving rail mode");
        }
        // Either no popover or a disarmed one is acceptable — both express "leaf-tooltip off".
    }

    @Test
    void switchingFromPopoverToSimpleRemovesLeafPopoverTrigger() {
        SideNavRail nav = new SideNavRail();
        SideNavRailItem leaf = new SideNavRailItem("Dashboard", "/dashboard");
        nav.addItem(leaf);
        nav.setPopoverHeaderMode(PopoverHeaderMode.LABEL_ONLY);
        nav.setRailTooltipMode(RailTooltipMode.POPOVER_HEADER);
        UI.getCurrent().add(nav);
        nav.setRailMode(true);
        nav.setRailTooltipMode(RailTooltipMode.SIMPLE);

        Popover popover = findPopoverFor(leaf);
        if (popover != null) {
            assertFalse(popover.isOpenOnHover());
        }
    }

    @Test
    void simpleModeCreatesNoPopoverOnLeaf() {
        SideNavRail nav = new SideNavRail();
        SideNavRailItem leaf = new SideNavRailItem("Dashboard", "/dashboard");
        nav.addItem(leaf);
        nav.setRailTooltipMode(RailTooltipMode.SIMPLE);
        UI.getCurrent().add(nav);
        nav.setRailMode(true);

        assertNull(findPopoverFor(leaf), "SIMPLE mode must not create a popover on a leaf item");
    }

    @Test
    void popoverModeOnLeafRendersLabelOnlyHeader() {
        SideNavRail nav = new SideNavRail();
        SideNavRailItem leaf =
                new SideNavRailItem("Dashboard", "/dashboard", VaadinIcon.DASHBOARD.create());
        nav.addItem(leaf);
        nav.setPopoverHeaderMode(PopoverHeaderMode.LABEL_ONLY);
        nav.setRailTooltipMode(RailTooltipMode.POPOVER_HEADER);
        UI.getCurrent().add(nav);
        nav.setRailMode(true);

        Popover popover = findPopoverFor(leaf);
        assertNotNull(popover);
        Div header = findHeader(popover);
        assertNotNull(header, "LABEL_ONLY must render a header");
        assertEquals(0L, iconsIn(header), "LABEL_ONLY must not render an icon");
        assertEquals("Dashboard", textOf(header));
    }

    @Test
    void popoverModeOnLeafRendersIconOnlyHeader() {
        SideNavRail nav = new SideNavRail();
        SideNavRailItem leaf =
                new SideNavRailItem("Dashboard", "/dashboard", VaadinIcon.DASHBOARD.create());
        nav.addItem(leaf);
        nav.setPopoverHeaderMode(PopoverHeaderMode.ICON_ONLY);
        nav.setRailTooltipMode(RailTooltipMode.POPOVER_HEADER);
        UI.getCurrent().add(nav);
        nav.setRailMode(true);

        Popover popover = findPopoverFor(leaf);
        assertNotNull(popover);
        Div header = findHeader(popover);
        assertNotNull(header, "ICON_ONLY must render a header when prefix icon is set");
        assertEquals(1L, iconsIn(header));
        assertEquals("", textOf(header));
    }

    @Test
    void popoverModeOnLeafRendersFullHeader() {
        SideNavRail nav = new SideNavRail();
        SideNavRailItem leaf =
                new SideNavRailItem("Dashboard", "/dashboard", VaadinIcon.DASHBOARD.create());
        nav.addItem(leaf);
        nav.setPopoverHeaderMode(PopoverHeaderMode.FULL);
        nav.setRailTooltipMode(RailTooltipMode.POPOVER_HEADER);
        UI.getCurrent().add(nav);
        nav.setRailMode(true);

        Popover popover = findPopoverFor(leaf);
        assertNotNull(popover);
        Div header = findHeader(popover);
        assertNotNull(header);
        assertEquals(1L, iconsIn(header));
        assertEquals("Dashboard", textOf(header));
    }

    @Test
    void attachWithPopoverAndNoneHeaderCoercesToLabelOnly() {
        SideNavRail nav = new SideNavRail();
        nav.addItem(new SideNavRailItem("Dashboard", "/dashboard"));
        nav.setRailTooltipMode(RailTooltipMode.POPOVER_HEADER);
        // Header mode left at default NONE.
        UI.getCurrent().add(nav); // attach

        assertEquals(PopoverHeaderMode.LABEL_ONLY, nav.getPopoverHeaderMode());
    }

    @Test
    void attachWithPopoverAndExplicitHeaderModeIsNotCoerced() {
        SideNavRail nav = new SideNavRail();
        nav.addItem(new SideNavRailItem("Dashboard", "/dashboard"));
        nav.setRailTooltipMode(RailTooltipMode.POPOVER_HEADER);
        nav.setPopoverHeaderMode(PopoverHeaderMode.ICON_ONLY);
        UI.getCurrent().add(nav);

        assertEquals(PopoverHeaderMode.ICON_ONLY, nav.getPopoverHeaderMode());
    }

    @Test
    void attachWithoutPopoverDoesNotCoerce() {
        // The auto-coerce in onAttach only fires for RailTooltipMode.POPOVER_HEADER
        // combined with PopoverHeaderMode.NONE — without it a leaf popover would have
        // empty content. Here we explicitly opt OUT of POPOVER_HEADER (and the
        // matching LABEL_ONLY default header) so no coerce should happen and NONE
        // sticks.
        SideNavRail nav = new SideNavRail();
        nav.addItem(new SideNavRailItem("Dashboard", "/dashboard"));
        nav.setRailTooltipMode(RailTooltipMode.SIMPLE);
        nav.setPopoverHeaderMode(PopoverHeaderMode.NONE);
        UI.getCurrent().add(nav);

        assertEquals(PopoverHeaderMode.NONE, nav.getPopoverHeaderMode());
    }

    private static SideNavRail railWithItem(String label) {
        SideNavRail nav = new SideNavRail();
        nav.addItem(new SideNavRailItem(label, "/x"));
        return nav;
    }

    private static Popover findPopoverFor(SideNavRailItem item) {
        return UI.getCurrent()
                .getChildren()
                .filter(c -> c instanceof Popover)
                .map(c -> (Popover) c)
                .filter(p -> p.getTarget() == item)
                .findFirst()
                .orElse(null);
    }

    private static Div findHeader(Popover popover) {
        return popover.getChildren()
                .filter(c -> c instanceof Div)
                .map(c -> (Div) c)
                .filter(d -> d.getClassNames().contains("side-nav-rail-popover-header"))
                .findFirst()
                .orElse(null);
    }

    private static long iconsIn(Component parent) {
        return parent.getChildren().filter(c -> c instanceof Icon).count();
    }

    private static String textOf(Component header) {
        return header.getChildren()
                .filter(c -> c instanceof Span)
                .map(c -> ((Span) c).getText())
                .findFirst()
                .orElse("");
    }
}
