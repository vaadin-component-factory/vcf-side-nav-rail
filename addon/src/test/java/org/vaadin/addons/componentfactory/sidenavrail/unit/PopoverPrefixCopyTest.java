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
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.github.mvysny.kaributesting.v10.MockVaadin;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.icon.VaadinIcon;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.vaadin.addons.componentfactory.sidenavrail.PopoverHeaderMode;
import org.vaadin.addons.componentfactory.sidenavrail.SideNavRail;
import org.vaadin.addons.componentfactory.sidenavrail.SideNavRailItem;

/**
 * Regression tests for the popover prefix-copy path. When a parent item builds its popover it
 * clones each child into a nested {@code SideNav}, and the popover header clones the parent's own
 * prefix. The clone must be an independent component — sharing the live instance would reparent it
 * out of the visible item (a Flow {@code Element} has a single parent), blanking the on-screen item
 * and eventually orphaning the component on the next rebuild.
 */
class PopoverPrefixCopyTest {

    private static final String AVATAR_CLASS = "side-nav-rail-letter-avatar";

    @BeforeEach
    void setUp() {
        MockVaadin.setup();
    }

    @AfterEach
    void tearDown() {
        MockVaadin.tearDown();
    }

    @Test
    void nestedChildAutoAvatarSurvivesPopoverRebuild() {
        SideNavRail rail = new SideNavRail();
        SideNavRailItem parent =
                new SideNavRailItem("Parent", "/parent", VaadinIcon.FOLDER.create());
        // No icon → child relies on the auto-generated letter avatar.
        SideNavRailItem child = new SideNavRailItem("Child", "/parent/child");
        parent.addItem(child);
        rail.addItem(parent);
        UI.getCurrent().add(rail);

        assertAutoAvatar(child);

        // Forces the parent to rebuild its popover content (copyOf every child).
        rail.setPopoverHeaderMode(PopoverHeaderMode.FULL);

        Avatar afterRebuild = assertAutoAvatar(child);
        assertEquals(
                "C",
                afterRebuild.getAbbreviation(),
                "The child must keep its own letter avatar; the popover must get a clone");
    }

    @Test
    void rootCustomPrefixSurvivesIconOnlyHeader() {
        SideNavRail rail = new SideNavRail();
        Avatar custom = new Avatar("User");
        SideNavRailItem root = new SideNavRailItem("Account", "/account", custom);
        root.addItem(new SideNavRailItem("Child", "/account/child"));
        rail.addItem(root);
        UI.getCurrent().add(rail);

        // The header renders only in rail mode by default; enter it so ICON_ONLY
        // actually builds the header (and would steal the prefix without the fix).
        rail.setRailMode(true);
        rail.setPopoverHeaderMode(PopoverHeaderMode.ICON_ONLY);

        assertSame(
                custom,
                root.getPrefixComponent(),
                "The header must clone the root's prefix, not steal the live instance");
    }

    private static Avatar assertAutoAvatar(SideNavRailItem item) {
        Component prefix = item.getPrefixComponent();
        assertNotNull(prefix, "Expected the child to still own its auto-generated avatar");
        Avatar avatar = assertInstanceOf(Avatar.class, prefix, "Prefix must be a vaadin-avatar");
        if (!avatar.getClassNames().contains(AVATAR_CLASS)) {
            throw new AssertionError("Prefix avatar lost its auto-generated marker class");
        }
        return avatar;
    }
}
