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
import org.vaadin.addons.componentfactory.sidenavrail.PopoverOn;
import org.vaadin.addons.componentfactory.sidenavrail.SideNavRail;
import org.vaadin.addons.componentfactory.sidenavrail.SideNavRailItem;

/**
 * ONLY_ROOT_COLLAPSED_ITEMS scenario — "Code" is a root item with children
 * (should get a popover); "Branches" is nested inside "Code" and itself has
 * children (must NOT get a popover because it is not a direct child of the
 * rail).
 */
@Route("only-root-collapsed-items")
public class PopoverOnlyRootCollapsedItemsView extends VerticalLayout {

    public PopoverOnlyRootCollapsedItemsView() {
        SideNavRail rail = new SideNavRail();
        rail.setPopoverOn(PopoverOn.ONLY_ROOT_COLLAPSED_ITEMS);
        rail.setId("rail");

        SideNavRailItem code = new SideNavRailItem(
                "Code", "/code", VaadinIcon.CODE.create());

        SideNavRailItem branches = new SideNavRailItem("Branches", "/code/branches");
        branches.addItem(new SideNavRailItem("Active", "/code/branches/active"));
        branches.addItem(new SideNavRailItem("Stale", "/code/branches/stale"));
        code.addItem(branches);
        code.addItem(new SideNavRailItem("Tags", "/code/tags"));

        rail.addItem(code);

        Button toggle = new Button("Toggle rail", e -> rail.setRailMode(!rail.isRailMode()));
        toggle.setId("toggle-rail");

        add(new HorizontalLayout(rail, toggle));
    }
}
