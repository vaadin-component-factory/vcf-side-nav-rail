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
package org.vaadin.addons.componentfactory.sidenavrail.demo.views;

import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import org.vaadin.addons.componentfactory.sidenavrail.SideNavRail;
import org.vaadin.addons.componentfactory.sidenavrail.SideNavRailItem;

/**
 * Screenshot-friendly composition for the README. Bypasses {@link
 * org.vaadin.addons.componentfactory.sidenavrail.demo.MainLayout} via {@code autoLayout = false} so
 * the captured frame contains only the rail + a clean content area — no toggle, no debug controls.
 */
@Route(value = "screenshot", autoLayout = false)
public class ScreenshotView extends HorizontalLayout {

    public ScreenshotView() {
        setPadding(false);
        setSpacing(false);
        setSizeFull();

        SideNavRail rail = new SideNavRail();
        rail.setId("screenshot-rail");
        rail.addItem(
                new SideNavRailItem("Dashboard", "/dashboard", VaadinIcon.DASHBOARD.create()),
                codeSection(),
                adminSection(),
                new SideNavRailItem("Settings", "/settings", VaadinIcon.COG.create()));

        VerticalLayout content = new VerticalLayout();
        content.setSizeFull();
        content.getStyle().set("background", "var(--lumo-shade-5pct)");

        add(rail, content);
        setFlexGrow(0, rail);
        setFlexGrow(1, content);
    }

    private static SideNavRailItem codeSection() {
        SideNavRailItem code = new SideNavRailItem("Code", "/code", VaadinIcon.CODE.create());
        code.addItem(new SideNavRailItem("Branches", "/code/branches"));
        code.addItem(new SideNavRailItem("Pull requests", "/code/pulls"));
        code.addItem(new SideNavRailItem("Releases", "/code/releases"));
        return code;
    }

    private static SideNavRailItem adminSection() {
        SideNavRailItem admin = new SideNavRailItem("Admin", "/admin", VaadinIcon.SHIELD.create());
        SideNavRailItem users = new SideNavRailItem("Users", "/admin/users");
        users.addItem(new SideNavRailItem("Active", "/admin/users/active"));
        users.addItem(new SideNavRailItem("Archived", "/admin/users/archived"));
        admin.addItem(users);
        admin.addItem(new SideNavRailItem("Roles", "/admin/roles"));
        return admin;
    }
}
