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
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.vaadin.addons.componentfactory.sidenavrail.PopoverMode;
import org.vaadin.addons.componentfactory.sidenavrail.SideNavRail;

class PopoverModeTest {

    @Test
    void defaultModeIsCollapsedItem() {
        SideNavRail nav = new SideNavRail();
        assertEquals(PopoverMode.COLLAPSED_ITEM, nav.getPopoverMode());
    }

    @Test
    void modeCanBeChanged() {
        SideNavRail nav = new SideNavRail();
        nav.setPopoverMode(PopoverMode.RAIL_ONLY);
        assertEquals(PopoverMode.RAIL_ONLY, nav.getPopoverMode());
    }

    @Test
    void settingNullIsRejected() {
        SideNavRail nav = new SideNavRail();
        assertThrows(NullPointerException.class, () -> nav.setPopoverMode(null));
    }
}
