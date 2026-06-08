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

        long popovers = UI.getCurrent().getChildren().filter(c -> c instanceof Popover).count();
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

    @Test
    void popoverBecomesDirectUiChildNotNavDescendant() {
        // The popover auto-adds itself to the UI root via Popover.setTarget(...).
        // Regression guard: we used to appendChild the popover onto the rail element
        // manually, which double-parented it and defeated the auto-detach. The popover
        // must land as a direct UI child, not as a child of the nav.
        SideNavRail nav = new SideNavRail();
        SideNavRailItem parent = new SideNavRailItem("Code");
        parent.addItem(new SideNavRailItem("Branches", "/branches"));
        nav.addItem(parent);
        UI.getCurrent().add(nav);

        long navChildPopovers = nav.getChildren().filter(c -> c instanceof Popover).count();
        assertEquals(
                0L,
                navChildPopovers,
                "Popover must NOT be a child of the nav (auto-add should place it on the UI)");

        long uiChildPopovers =
                UI.getCurrent().getChildren().filter(c -> c instanceof Popover).count();
        assertEquals(1L, uiChildPopovers, "Popover should auto-add itself as a direct UI child");
    }

    @Test
    void popoverDetachesWhenOwnerRailIsRemoved() {
        // When the rail (and therefore each SideNavRailItem target) detaches, the
        // Popover's detach listener calls removeFromUiIfAutoAdded() and the popover
        // disappears from the UI. Without this, every rail removal would leak a
        // stale popover into the UI.
        SideNavRail nav = new SideNavRail();
        SideNavRailItem parent = new SideNavRailItem("Code");
        parent.addItem(new SideNavRailItem("Branches", "/branches"));
        nav.addItem(parent);
        UI.getCurrent().add(nav);

        assertEquals(
                1L,
                UI.getCurrent().getChildren().filter(c -> c instanceof Popover).count(),
                "precondition: popover attached");

        UI.getCurrent().remove(nav);

        assertEquals(
                0L,
                UI.getCurrent().getChildren().filter(c -> c instanceof Popover).count(),
                "popover must be removed from UI when its target detaches");
    }

    @Test
    void outerChildrenSurvivePopoverPopulation() {
        SideNavRail nav = new SideNavRail();
        SideNavRailItem parent = new SideNavRailItem("Code");
        parent.addItem(new SideNavRailItem("Branches", "/branches"));
        parent.addItem(new SideNavRailItem("Tags", "/tags"));
        nav.addItem(parent);
        UI.getCurrent().add(nav);

        assertEquals(
                2,
                parent.getItems().size(),
                "Outer nav must retain its children for inline expansion");

        Popover popover =
                UI.getCurrent()
                        .getChildren()
                        .filter(c -> c instanceof Popover)
                        .map(c -> (Popover) c)
                        .findFirst()
                        .orElseThrow();

        long nestedChildren = popover.getChildren().flatMap(c -> c.getChildren()).count();
        assertEquals(2L, nestedChildren, "Popover must render a mirrored copy of the children");
    }

    private static Popover findPopoverTargeting(SideNavRailItem item) {
        return UI.getCurrent()
                .getChildren()
                .filter(c -> c instanceof Popover)
                .map(c -> (Popover) c)
                .filter(p -> p.getTarget() == item)
                .findFirst()
                .orElse(null);
    }
}
