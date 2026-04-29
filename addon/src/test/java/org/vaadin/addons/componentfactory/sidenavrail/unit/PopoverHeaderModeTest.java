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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.vaadin.addons.componentfactory.sidenavrail.PopoverHeaderMode;
import org.vaadin.addons.componentfactory.sidenavrail.SideNavRail;
import org.vaadin.addons.componentfactory.sidenavrail.SideNavRailItem;

class PopoverHeaderModeTest {

    @BeforeEach
    void setUp() {
        MockVaadin.setup();
    }

    @AfterEach
    void tearDown() {
        MockVaadin.tearDown();
    }

    @Test
    void defaultIsLabelOnly() {
        SideNavRail nav = new SideNavRail();
        assertEquals(PopoverHeaderMode.LABEL_ONLY, nav.getPopoverHeaderMode());
    }

    @Test
    void nullIsRejected() {
        SideNavRail nav = new SideNavRail();
        assertThrows(NullPointerException.class, () -> nav.setPopoverHeaderMode(null));
    }

    @Test
    void defaultIsRailModeOnly() {
        SideNavRail nav = new SideNavRail();
        assertTrue(nav.isPopoverHeaderOnlyInRailMode(),
                "Header is rail-mode-only by default");
    }

    @Test
    void noneRendersNoHeader() {
        SideNavRail nav = railWithParent("Code", VaadinIcon.CODE.create());
        nav.setPopoverHeaderMode(PopoverHeaderMode.NONE);
        UI.getCurrent().add(nav);

        assertNull(findHeader(parentPopover()), "NONE must not render a header");
    }

    @Test
    void labelOnlyRendersTextOnly() {
        SideNavRail nav = railWithParent("Code", VaadinIcon.CODE.create());
        nav.setPopoverHeaderMode(PopoverHeaderMode.LABEL_ONLY);
        UI.getCurrent().add(nav);

        Div header = findHeader(parentPopover());
        assertNotNull(header, "LABEL_ONLY must render a header");
        assertEquals(0L, iconsIn(header), "LABEL_ONLY must not render an icon");
        assertEquals("Code", textOf(header));
    }

    @Test
    void iconOnlyRendersIconOnly() {
        SideNavRail nav = railWithParent("Code", VaadinIcon.CODE.create());
        nav.setPopoverHeaderMode(PopoverHeaderMode.ICON_ONLY);
        UI.getCurrent().add(nav);

        Div header = findHeader(parentPopover());
        assertNotNull(header, "ICON_ONLY must render a header when a prefix icon is set");
        assertEquals(1L, iconsIn(header));
        assertEquals("", textOf(header));
    }

    @Test
    void fullRendersIconAndLabel() {
        SideNavRail nav = railWithParent("Code", VaadinIcon.CODE.create());
        nav.setPopoverHeaderMode(PopoverHeaderMode.FULL);
        UI.getCurrent().add(nav);

        Div header = findHeader(parentPopover());
        assertNotNull(header);
        assertEquals(1L, iconsIn(header));
        assertEquals("Code", textOf(header));
    }

    @Test
    void iconOnlyWithoutPrefixIconRendersNoHeader() {
        // Parent without a prefix icon — ICON_ONLY would produce an empty header, so the
        // whole header is skipped rather than rendered blank.
        SideNavRail nav = railWithParent("Code", null);
        nav.setPopoverHeaderMode(PopoverHeaderMode.ICON_ONLY);
        UI.getCurrent().add(nav);

        assertNull(findHeader(parentPopover()),
                "ICON_ONLY on a parent without a prefix icon must render no header");
    }

    @Test
    void labelOnlyFallsBackGracefullyWhenLabelIsBlank() {
        // Parent with an icon but no label — LABEL_ONLY wants text it doesn't have,
        // so no header (graceful empty-header suppression).
        SideNavRail nav = new SideNavRail();
        SideNavRailItem parent = new SideNavRailItem("");
        parent.setPrefixComponent(VaadinIcon.CODE.create());
        parent.addItem(new SideNavRailItem("Branches", "/branches"));
        nav.addItem(parent);
        nav.setPopoverHeaderMode(PopoverHeaderMode.LABEL_ONLY);
        nav.setRailMode(true);
        UI.getCurrent().add(nav);

        assertNull(findHeader(popoverTargeting(parent)),
                "LABEL_ONLY on a parent with a blank label must render no header");
    }

    @Test
    void liveSwitchRebuildsExistingPopover() {
        SideNavRail nav = railWithParent("Code", VaadinIcon.CODE.create());
        nav.setPopoverHeaderMode(PopoverHeaderMode.NONE);
        UI.getCurrent().add(nav);

        Popover popover = parentPopover();
        assertNull(findHeader(popover), "precondition: NONE renders no header");

        nav.setPopoverHeaderMode(PopoverHeaderMode.FULL);
        assertNotNull(findHeader(popover), "live switch to FULL must add a header");

        nav.setPopoverHeaderMode(PopoverHeaderMode.LABEL_ONLY);
        Div header = findHeader(popover);
        assertNotNull(header);
        assertEquals(0L, iconsIn(header), "switching back to LABEL_ONLY must drop the icon");

        nav.setPopoverHeaderMode(PopoverHeaderMode.NONE);
        assertNull(findHeader(popover), "switching back to NONE must remove the header");
    }

    // ---- helpers ----

    private static SideNavRail railWithParent(String label, Icon prefix) {
        SideNavRail nav = new SideNavRail();
        SideNavRailItem parent = (prefix != null)
                ? new SideNavRailItem(label, "/parent", prefix)
                : new SideNavRailItem(label, "/parent");
        parent.addItem(new SideNavRailItem("Child", "/parent/child"));
        nav.addItem(parent);
        // Header is rail-mode-only by default; the per-mode rendering tests below
        // expect a header to appear, so put the rail into rail mode up front.
        nav.setRailMode(true);
        return nav;
    }

    private static Popover parentPopover() {
        // Filter to popovers whose target is a parent item (has children). With the
        // RailTooltipMode.POPOVER_HEADER default, leaves can also have popovers; this
        // helper would otherwise pick the wrong one whenever the iteration order put
        // a leaf first.
        return UI.getCurrent().getChildren()
                .filter(c -> c instanceof Popover)
                .map(c -> (Popover) c)
                .filter(p -> p.getTarget() instanceof SideNavRailItem item
                        && !item.getItems().isEmpty())
                .findFirst()
                .orElseThrow(() -> new AssertionError("No popover targets a parent item"));
    }

    private static Popover popoverTargeting(SideNavRailItem target) {
        return UI.getCurrent().getChildren()
                .filter(c -> c instanceof Popover)
                .map(c -> (Popover) c)
                .filter(p -> p.getTarget() == target)
                .findFirst()
                .orElseThrow(() -> new AssertionError("No popover targets " + target));
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
        return parent.getChildren()
                .filter(c -> c instanceof Icon)
                .count();
    }

    private static String textOf(Component header) {
        return header.getChildren()
                .filter(c -> c instanceof Span)
                .map(c -> ((Span) c).getText())
                .findFirst()
                .orElse("");
    }
}
