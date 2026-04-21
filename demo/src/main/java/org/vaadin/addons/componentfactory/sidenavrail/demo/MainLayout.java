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
package org.vaadin.addons.componentfactory.sidenavrail.demo;

import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.router.Layout;
import org.vaadin.addons.componentfactory.sidenavrail.SideNavRail;
import org.vaadin.addons.componentfactory.sidenavrail.SideNavRailItem;

@Layout
public class MainLayout extends AppLayout {

    public MainLayout() {
        SideNavRail nav = new SideNavRail();

        SideNavRailItem dashboard =
                new SideNavRailItem("Dashboard", "/", VaadinIcon.DASHBOARD.create());

        SideNavRailItem code = new SideNavRailItem(
                "Code", "/code", VaadinIcon.CODE.create());
        code.addItem(new SideNavRailItem("Branches", "/code/branches"));
        code.addItem(new SideNavRailItem("Commits", "/code/commits"));
        code.addItem(new SideNavRailItem("Tags", "/code/tags"));

        SideNavRailItem operate = new SideNavRailItem(
                "Operate", "/operate", VaadinIcon.COGS.create());
        operate.addItem(new SideNavRailItem("Environments", "/operate/environments"));
        operate.addItem(new SideNavRailItem("Releases", "/operate/releases"));

        nav.addItem(dashboard, code, operate);

        Button toggle = new Button(VaadinIcon.CHEVRON_LEFT_SMALL.create(),
                e -> nav.setRailMode(!nav.isRailMode()));
        addToNavbar(toggle);
        addToDrawer(nav);
    }
}
