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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.github.mvysny.kaributesting.v10.MockVaadin;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.popover.Popover;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.vaadin.addons.componentfactory.sidenavrail.PopoverHeaderMode;
import org.vaadin.addons.componentfactory.sidenavrail.SideNavRail;
import org.vaadin.addons.componentfactory.sidenavrail.SideNavRailItem;

class PopoverHeaderOnlyInRailModeTest {

    @BeforeEach
    void setUp() {
        MockVaadin.setup();
    }

    @AfterEach
    void tearDown() {
        MockVaadin.tearDown();
    }

    @Test
    void defaultHidesHeaderInNormalMode() {
        SideNavRail nav = newRail();
        nav.setPopoverHeaderMode(PopoverHeaderMode.FULL);
        UI.getCurrent().add(nav);

        assertNull(findHeader(parentPopover()),
                "Default (only-in-rail-mode) must hide the header in normal mode");
    }

    @Test
    void defaultShowsHeaderInRailMode() {
        SideNavRail nav = newRail();
        nav.setPopoverHeaderMode(PopoverHeaderMode.FULL);
        nav.setRailMode(true);
        UI.getCurrent().add(nav);

        assertNotNull(findHeader(parentPopover()),
                "Default (only-in-rail-mode) must render the header in rail mode");
    }

    @Test
    void disabledShowsHeaderInBothModes() {
        SideNavRail nav = newRail();
        nav.setPopoverHeaderMode(PopoverHeaderMode.FULL);
        nav.setPopoverHeaderOnlyInRailMode(false);
        UI.getCurrent().add(nav);

        assertNotNull(findHeader(parentPopover()),
                "Disabling the only-in-rail flag must render the header in normal mode");

        nav.setRailMode(true);
        assertNotNull(findHeader(parentPopover()),
                "Disabling the flag must render the header in rail mode too");
    }

    @Test
    void liveSwitchOfFlagRebuildsExistingPopover() {
        // Popover is created on attach with default (rail-mode-only) and rail off,
        // so the header should be absent. Flipping the flag should add it.
        SideNavRail nav = newRail();
        nav.setPopoverHeaderMode(PopoverHeaderMode.FULL);
        UI.getCurrent().add(nav);

        Popover popover = parentPopover();
        assertNull(findHeader(popover), "precondition: header hidden in normal mode");

        nav.setPopoverHeaderOnlyInRailMode(false);
        assertNotNull(findHeader(popover),
                "Flipping the flag to false must add the header live");

        nav.setPopoverHeaderOnlyInRailMode(true);
        assertNull(findHeader(popover),
                "Flipping the flag back to true must remove the header live");
    }

    @Test
    void railModeToggleRebuildsHeader() {
        // With the default flag, toggling rail mode must add/remove the header on the
        // existing popover (without requiring a reattach).
        SideNavRail nav = newRail();
        nav.setPopoverHeaderMode(PopoverHeaderMode.FULL);
        UI.getCurrent().add(nav);

        Popover popover = parentPopover();
        assertNull(findHeader(popover), "precondition: hidden in normal mode");

        nav.setRailMode(true);
        assertNotNull(findHeader(popover),
                "Entering rail mode must add the header live");

        nav.setRailMode(false);
        assertNull(findHeader(popover),
                "Leaving rail mode must remove the header live");
    }

    @Test
    void hasNoEffectWhenModeIsNone() {
        // Flag is meaningful only when a non-NONE mode is set; with NONE no header is
        // rendered regardless of flag/rail-mode.
        SideNavRail nav = newRail();
        nav.setPopoverHeaderMode(PopoverHeaderMode.NONE);
        nav.setPopoverHeaderOnlyInRailMode(false);
        nav.setRailMode(true);
        UI.getCurrent().add(nav);

        assertNull(findHeader(parentPopover()),
                "NONE must not render a header even with the flag disabled");
    }

    // ---- helpers ----

    private static SideNavRail newRail() {
        SideNavRail nav = new SideNavRail();
        SideNavRailItem parent = new SideNavRailItem("Code", "/code", VaadinIcon.CODE.create());
        parent.addItem(new SideNavRailItem("Child", "/code/child"));
        nav.addItem(parent);
        return nav;
    }

    private static Popover parentPopover() {
        // Filter to popovers whose target is a parent item (has children). With the
        // RailTooltipMode.POPOVER_HEADER default, leaves can also have popovers in
        // rail mode; this helper would otherwise pick the wrong one when iteration
        // order put a leaf first.
        return UI.getCurrent().getChildren()
                .filter(c -> c instanceof Popover)
                .map(c -> (Popover) c)
                .filter(p -> p.getTarget() instanceof SideNavRailItem item
                        && !item.getItems().isEmpty())
                .findFirst()
                .orElseThrow(() -> new AssertionError("No popover targets a parent item"));
    }

    private static Div findHeader(Popover popover) {
        return popover.getChildren()
                .filter(c -> c instanceof Div)
                .map(c -> (Div) c)
                .filter(d -> d.getClassNames().contains("side-nav-rail-popover-header"))
                .findFirst()
                .orElse(null);
    }
}
