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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.mvysny.kaributesting.v10.MockVaadin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.vaadin.addons.componentfactory.sidenavrail.SideNavRail;
import org.vaadin.addons.componentfactory.sidenavrail.SideNavRailItem;

/**
 * Exercises the {@code root-item} DOM attribute that SideNavRail sets on each direct child. The
 * attribute is a styling hook for consumer CSS (e.g. {@code
 * vaadin-side-nav-item[root-item]:has([current]) > vaadin-icon}) and is <em>only</em> set on direct
 * children — nested items must not carry it.
 */
class RootItemAttributeTest {

    private static final String ATTR = "root-item";

    @BeforeEach
    void setUp() {
        MockVaadin.setup();
    }

    @AfterEach
    void tearDown() {
        MockVaadin.tearDown();
    }

    @Test
    void directChildrenCarryTheAttribute() {
        SideNavRail nav = new SideNavRail();
        SideNavRailItem code = new SideNavRailItem("Code", "/code");
        SideNavRailItem ops = new SideNavRailItem("Operate", "/operate");
        nav.addItem(code, ops);

        assertTrue(code.getElement().hasAttribute(ATTR));
        assertTrue(ops.getElement().hasAttribute(ATTR));
    }

    @Test
    void addItemAsFirstAlsoSetsTheAttribute() {
        SideNavRail nav = new SideNavRail();
        SideNavRailItem first = new SideNavRailItem("Dashboard", "/");
        nav.addItemAsFirst(first);

        assertTrue(first.getElement().hasAttribute(ATTR));
    }

    @Test
    void nestedItemsDoNotCarryTheAttribute() {
        SideNavRail nav = new SideNavRail();
        SideNavRailItem code = new SideNavRailItem("Code", "/code");
        SideNavRailItem branches = new SideNavRailItem("Branches", "/code/branches");
        SideNavRailItem active = new SideNavRailItem("Active", "/code/branches/active");
        branches.addItem(active);
        code.addItem(branches);
        nav.addItem(code);

        assertTrue(code.getElement().hasAttribute(ATTR), "Root item must carry the attribute");
        assertFalse(
                branches.getElement().hasAttribute(ATTR),
                "Nested parent must not carry the attribute");
        assertFalse(
                active.getElement().hasAttribute(ATTR),
                "Deep nested leaf must not carry the attribute");
    }

    @Test
    void standaloneItemWithoutRailHasNoAttribute() {
        // Sanity check: an item created outside any rail has no root-item marker.
        SideNavRailItem lonely = new SideNavRailItem("Lonely", "/lonely");
        assertFalse(lonely.getElement().hasAttribute(ATTR));
    }
}
