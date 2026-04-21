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
package org.vaadin.addons.componentfactory.sidenavrail.app.views;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import org.vaadin.addons.componentfactory.sidenavrail.PopoverMode;
import org.vaadin.addons.componentfactory.sidenavrail.SideNavRail;
import org.vaadin.addons.componentfactory.sidenavrail.SideNavRailItem;

@Route("collapsed-item")
public class PopoverCollapsedItemView extends VerticalLayout {

    public PopoverCollapsedItemView() {
        SideNavRail rail = new SideNavRail();
        rail.setPopoverMode(PopoverMode.COLLAPSED_ITEM);
        rail.setId("rail");

        SideNavRailItem code = new SideNavRailItem(
                "Code", "/code", VaadinIcon.CODE.create());
        code.addItem(new SideNavRailItem("Branches", "/code/branches"));
        code.addItem(new SideNavRailItem("Tags", "/code/tags"));
        rail.addItem(code);

        Button toggle = new Button("Toggle rail", e -> rail.setRailMode(!rail.isRailMode()));
        toggle.setId("toggle-rail");

        add(new HorizontalLayout(rail, toggle));
    }
}
