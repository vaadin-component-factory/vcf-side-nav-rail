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
import org.vaadin.addons.componentfactory.sidenavrail.RailTooltipMode;
import org.vaadin.addons.componentfactory.sidenavrail.SideNavRail;
import org.vaadin.addons.componentfactory.sidenavrail.SideNavRailItem;

/**
 * Exercises {@link RailTooltipMode#POPOVER_HEADER}. "Dashboard" is a leaf root item; "Code" has
 * children. Both should produce a popover on hover/focus while in rail mode.
 */
@Route("rail-tooltip-popover")
public class RailTooltipPopoverModeView extends VerticalLayout {

    public RailTooltipPopoverModeView() {
        SideNavRail rail = new SideNavRail();
        rail.setId("rail");
        rail.setPopoverHeaderMode(PopoverHeaderMode.LABEL_ONLY);
        rail.setRailTooltipMode(RailTooltipMode.POPOVER_HEADER);

        rail.addItem(
                new SideNavRailItem("Dashboard", "/dashboard", VaadinIcon.DASHBOARD.create()),
                codeParent());

        Button toggleRail = new Button("Toggle rail", e -> rail.setRailMode(!rail.isRailMode()));
        toggleRail.setId("toggle-rail");

        Button labelOnly =
                new Button(
                        "LABEL_ONLY", e -> rail.setPopoverHeaderMode(PopoverHeaderMode.LABEL_ONLY));
        labelOnly.setId("header-label-only");

        Button iconOnly =
                new Button(
                        "ICON_ONLY", e -> rail.setPopoverHeaderMode(PopoverHeaderMode.ICON_ONLY));
        iconOnly.setId("header-icon-only");

        Button full = new Button("FULL", e -> rail.setPopoverHeaderMode(PopoverHeaderMode.FULL));
        full.setId("header-full");

        add(new HorizontalLayout(rail, toggleRail, labelOnly, iconOnly, full));
    }

    private static SideNavRailItem codeParent() {
        SideNavRailItem code = new SideNavRailItem("Code", "/code", VaadinIcon.CODE.create());
        code.addItem(new SideNavRailItem("Branches", "/code/branches"));
        return code;
    }
}
