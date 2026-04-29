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
package org.vaadin.addons.componentfactory.sidenavrail.e2e.views;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import org.vaadin.addons.componentfactory.sidenavrail.PopoverHeaderMode;
import org.vaadin.addons.componentfactory.sidenavrail.SideNavRail;
import org.vaadin.addons.componentfactory.sidenavrail.SideNavRailItem;

/**
 * Exercises the {@link SideNavRail#setPopoverHeaderOnlyInRailMode(boolean) only-in-rail-mode}
 * gating: the parent-label header is configured (FULL) and toggles for rail-mode and the flag
 * let Playwright assert all four combinations.
 */
@Route("popover-header-only-in-rail-mode")
public class PopoverHeaderOnlyInRailModeView extends VerticalLayout {

    public PopoverHeaderOnlyInRailModeView() {
        SideNavRail rail = new SideNavRail();
        rail.setId("rail");
        rail.setPopoverHeaderMode(PopoverHeaderMode.FULL);

        SideNavRailItem code = new SideNavRailItem(
                "Code", "/code", VaadinIcon.CODE.create());
        code.addItem(new SideNavRailItem("Branches", "/code/branches"));
        code.addItem(new SideNavRailItem("Tags", "/code/tags"));
        rail.addItem(code);

        Button toggleRail = new Button("Toggle rail mode", e -> rail.toggleRailMode());
        toggleRail.setId("toggle-rail");
        Button flagOn = new Button("only-in-rail-mode = true",
                e -> rail.setPopoverHeaderOnlyInRailMode(true));
        flagOn.setId("flag-on");
        Button flagOff = new Button("only-in-rail-mode = false",
                e -> rail.setPopoverHeaderOnlyInRailMode(false));
        flagOff.setId("flag-off");

        add(new HorizontalLayout(rail, new HorizontalLayout(toggleRail, flagOn, flagOff)));
    }
}
