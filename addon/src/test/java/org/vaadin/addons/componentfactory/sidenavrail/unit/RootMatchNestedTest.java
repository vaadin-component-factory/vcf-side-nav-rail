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

import org.junit.jupiter.api.Test;
import org.vaadin.addons.componentfactory.sidenavrail.RootMatchNested;
import org.vaadin.addons.componentfactory.sidenavrail.SideNavRail;
import org.vaadin.addons.componentfactory.sidenavrail.SideNavRailItem;

class RootMatchNestedTest {

    private static SideNavRail navWithChild(SideNavRailItem child) {
        SideNavRail nav = new SideNavRail();
        nav.addItem(child);
        return nav;
    }

    @Test
    void defaultIsOnlyRail() {
        SideNavRail nav = new SideNavRail();
        assertEquals(RootMatchNested.ONLY_RAIL, nav.getRootMatchNested());
    }

    @Test
    void noneLeavesUserValueUntouched() {
        SideNavRailItem item = new SideNavRailItem("Code", "/code");
        item.setMatchNested(true);
        SideNavRail nav = navWithChild(item);
        nav.setRootMatchNested(RootMatchNested.NONE);

        // NONE: toggling rail mode must not touch matchNested.
        nav.setRailMode(true);
        assertTrue(item.isMatchNested());
        nav.setRailMode(false);
        assertTrue(item.isMatchNested());
    }

    @Test
    void onlyRailForcesMatchNestedWhileInRailMode() {
        SideNavRailItem item = new SideNavRailItem("Code", "/code");
        SideNavRail nav = navWithChild(item);
        nav.setRootMatchNested(RootMatchNested.ONLY_RAIL);

        // Off in normal mode.
        assertFalse(item.isMatchNested());

        nav.setRailMode(true);
        assertTrue(item.isMatchNested());

        nav.setRailMode(false);
        assertFalse(item.isMatchNested());
    }

    @Test
    void onlyRailRestoresUserValueOnRailModeExit() {
        SideNavRailItem item = new SideNavRailItem("Code", "/code");
        item.setMatchNested(true);
        SideNavRail nav = navWithChild(item);
        nav.setRootMatchNested(RootMatchNested.ONLY_RAIL);

        nav.setRailMode(true);
        assertTrue(item.isMatchNested()); // forced
        nav.setRailMode(false);
        assertTrue(item.isMatchNested()); // restored to user-set true
    }

    @Test
    void allForcesMatchNestedRegardlessOfRailMode() {
        SideNavRailItem item = new SideNavRailItem("Code", "/code");
        SideNavRail nav = navWithChild(item);
        nav.setRootMatchNested(RootMatchNested.ALL);

        assertTrue(item.isMatchNested());
        nav.setRailMode(true);
        assertTrue(item.isMatchNested());
        nav.setRailMode(false);
        assertTrue(item.isMatchNested());
    }

    @Test
    void switchingFromAllToNoneRestoresUserValue() {
        SideNavRailItem item = new SideNavRailItem("Code", "/code");
        item.setMatchNested(false);
        SideNavRail nav = navWithChild(item);

        nav.setRootMatchNested(RootMatchNested.ALL);
        assertTrue(item.isMatchNested());

        nav.setRootMatchNested(RootMatchNested.NONE);
        assertFalse(item.isMatchNested());
    }

    @Test
    void appliesToItemsAddedAfterModeIsSet() {
        SideNavRail nav = new SideNavRail();
        nav.setRootMatchNested(RootMatchNested.ALL);

        SideNavRailItem late = new SideNavRailItem("Code", "/code");
        nav.addItem(late);
        assertTrue(late.isMatchNested());
    }

    @Test
    void onlyRailAppliesToItemsAddedWhileInRailMode() {
        SideNavRail nav = new SideNavRail();
        nav.setRootMatchNested(RootMatchNested.ONLY_RAIL);
        nav.setRailMode(true);

        SideNavRailItem late = new SideNavRailItem("Code", "/code");
        nav.addItem(late);
        assertTrue(late.isMatchNested());

        nav.setRailMode(false);
        assertFalse(late.isMatchNested());
    }

    @Test
    void onlyRailDoesNotForceItemsAddedInNormalMode() {
        SideNavRail nav = new SideNavRail();
        nav.setRootMatchNested(RootMatchNested.ONLY_RAIL);

        SideNavRailItem late = new SideNavRailItem("Code", "/code");
        nav.addItem(late);
        assertFalse(late.isMatchNested());
    }

    @Test
    void addItemAsFirstHonoursOverride() {
        SideNavRail nav = new SideNavRail();
        nav.setRootMatchNested(RootMatchNested.ALL);

        SideNavRailItem first = new SideNavRailItem("Dashboard", "/");
        nav.addItemAsFirst(first);
        assertTrue(first.isMatchNested());
    }

    @Test
    void setRootMatchNestedRejectsNull() {
        SideNavRail nav = new SideNavRail();
        assertThrows(NullPointerException.class, () -> nav.setRootMatchNested(null));
    }
}
