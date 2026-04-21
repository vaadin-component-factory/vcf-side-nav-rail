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
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.mvysny.kaributesting.v10.MockVaadin;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.popover.Popover;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.vaadin.addons.componentfactory.sidenavrail.SideNavRail;
import org.vaadin.addons.componentfactory.sidenavrail.SideNavRailItem;

class PopoverLifecycleTest {

    @BeforeEach
    void setUp() {
        MockVaadin.setup();
    }

    @AfterEach
    void tearDown() {
        MockVaadin.tearDown();
    }

    @Test
    void itemWithoutChildrenDoesNotAttachPopover() {
        SideNavRail nav = new SideNavRail();
        SideNavRailItem leaf = new SideNavRailItem("Dashboard", "/");
        nav.addItem(leaf);
        UI.getCurrent().add(nav);

        long popovers = UI.getCurrent().getChildren()
                .flatMap(c -> c.getChildren())
                .filter(c -> c instanceof Popover)
                .count();
        assertEquals(0L, popovers);
    }

    @Test
    void itemWithChildrenAttachesPopoverOnFirstAttach() {
        SideNavRail nav = new SideNavRail();
        SideNavRailItem parent = new SideNavRailItem("Code");
        parent.addItem(new SideNavRailItem("Branches", "/branches"));
        parent.addItem(new SideNavRailItem("Tags", "/tags"));
        nav.addItem(parent);
        UI.getCurrent().add(nav);

        Popover popover = findPopoverTargeting(parent);
        assertNotNull(popover, "Popover should be attached for parent with children");
        assertEquals("menu", popover.getOverlayRole());
        assertTrue(popover.isOpenOnHover());
        assertEquals(200, popover.getHoverDelay());
        assertEquals(300, popover.getHideDelay());
    }

    private static Popover findPopoverTargeting(SideNavRailItem item) {
        return UI.getCurrent().getChildren()
                .flatMap(c -> c.getChildren())
                .filter(c -> c instanceof Popover)
                .map(c -> (Popover) c)
                .filter(p -> p.getTarget() == item)
                .findFirst()
                .orElse(null);
    }
}
