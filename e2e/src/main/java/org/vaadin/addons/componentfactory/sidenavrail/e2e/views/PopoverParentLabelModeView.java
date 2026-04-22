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
import org.vaadin.addons.componentfactory.sidenavrail.PopoverParentLabelMode;
import org.vaadin.addons.componentfactory.sidenavrail.SideNavRail;
import org.vaadin.addons.componentfactory.sidenavrail.SideNavRailItem;

/**
 * Exercises all four {@link PopoverParentLabelMode} values via buttons so Playwright can
 * switch at runtime and verify the header (or its absence) directly.
 */
@Route("popover-parent-label-mode")
public class PopoverParentLabelModeView extends VerticalLayout {

    public PopoverParentLabelModeView() {
        SideNavRail rail = new SideNavRail();
        rail.setId("rail");

        SideNavRailItem code = new SideNavRailItem(
                "Code", "/code", VaadinIcon.CODE.create());
        code.addItem(new SideNavRailItem("Branches", "/code/branches"));
        code.addItem(new SideNavRailItem("Tags", "/code/tags"));
        rail.addItem(code);

        HorizontalLayout modeButtons = new HorizontalLayout(
                modeButton(rail, PopoverParentLabelMode.NONE, "mode-none"),
                modeButton(rail, PopoverParentLabelMode.LABEL_ONLY, "mode-label"),
                modeButton(rail, PopoverParentLabelMode.ICON_ONLY, "mode-icon"),
                modeButton(rail, PopoverParentLabelMode.FULL, "mode-full"));

        add(new HorizontalLayout(rail, modeButtons));
    }

    private static Button modeButton(SideNavRail rail, PopoverParentLabelMode mode, String id) {
        Button button = new Button(mode.name(), e -> rail.setPopoverParentLabelMode(mode));
        button.setId(id);
        return button;
    }
}
