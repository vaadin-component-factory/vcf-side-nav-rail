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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.vaadin.addons.componentfactory.sidenavrail.SideNavRail;

class RailModeStateTest {

    @Test
    void defaultRailModeIsFalse() {
        SideNavRail nav = new SideNavRail();
        assertFalse(nav.isRailMode());
        assertNull(nav.getElement().getAttribute("theme"));
    }

    @Test
    void enablingRailModeSetsThemeAttribute() {
        SideNavRail nav = new SideNavRail();
        nav.setRailMode(true);
        assertTrue(nav.isRailMode());
        assertEquals("rail", nav.getElement().getAttribute("theme"));
    }

    @Test
    void disablingRailModeRemovesThemeAttribute() {
        SideNavRail nav = new SideNavRail();
        nav.setRailMode(true);
        nav.setRailMode(false);
        assertFalse(nav.isRailMode());
        assertNull(nav.getElement().getAttribute("theme"));
    }

    @Test
    void togglingFiresOneEventPerChange() {
        SideNavRail nav = new SideNavRail();
        java.util.List<Boolean> received = new java.util.ArrayList<>();
        nav.addRailModeChangedListener(e -> received.add(e.isRailMode()));

        nav.setRailMode(true);
        nav.setRailMode(true);   // no-op — same value
        nav.setRailMode(false);

        assertEquals(java.util.List.of(true, false), received);
    }

    @Test
    void eventReportsFromClientFalseForServerCalls() {
        SideNavRail nav = new SideNavRail();
        java.util.concurrent.atomic.AtomicBoolean fromClient =
                new java.util.concurrent.atomic.AtomicBoolean(true);
        nav.addRailModeChangedListener(e -> fromClient.set(e.isFromClient()));

        nav.setRailMode(true);

        assertFalse(fromClient.get());
    }
}
