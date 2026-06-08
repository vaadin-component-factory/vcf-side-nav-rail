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
import org.vaadin.addons.componentfactory.sidenavrail.RailTooltipMode;
import org.vaadin.addons.componentfactory.sidenavrail.SideNavRail;
import org.vaadin.addons.componentfactory.sidenavrail.SideNavRailItem;

/**
 * Exercises {@link RailTooltipMode}: "Dashboard" is a leaf root item, "Code" has children. Buttons
 * switch the mode at runtime so Playwright can verify each combination of (rail mode × tooltip
 * mode).
 */
@Route("rail-tooltip-mode")
public class RailTooltipModeView extends VerticalLayout {

    public RailTooltipModeView() {
        SideNavRail rail = new SideNavRail();
        rail.setId("rail");

        rail.addItem(
                new SideNavRailItem("Dashboard", "/dashboard", VaadinIcon.DASHBOARD.create()),
                codeParent());

        Button toggle = new Button("Toggle rail", e -> rail.setRailMode(!rail.isRailMode()));
        toggle.setId("toggle-rail");

        HorizontalLayout modeButtons =
                new HorizontalLayout(
                        modeButton(rail, RailTooltipMode.NONE, "mode-none"),
                        modeButton(rail, RailTooltipMode.SIMPLE, "mode-simple"));

        add(new HorizontalLayout(rail, toggle, modeButtons));
    }

    private static SideNavRailItem codeParent() {
        SideNavRailItem code = new SideNavRailItem("Code", "/code", VaadinIcon.CODE.create());
        code.addItem(new SideNavRailItem("Branches", "/code/branches"));
        return code;
    }

    private static Button modeButton(SideNavRail rail, RailTooltipMode mode, String id) {
        Button button = new Button(mode.name(), e -> rail.setRailTooltipMode(mode));
        button.setId(id);
        return button;
    }
}
