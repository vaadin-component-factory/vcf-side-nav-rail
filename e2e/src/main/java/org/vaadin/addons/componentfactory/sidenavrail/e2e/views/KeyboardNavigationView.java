/*
 * Copyright 2026 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */
package org.vaadin.addons.componentfactory.sidenavrail.e2e.views;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import org.vaadin.addons.componentfactory.sidenavrail.SideNavRail;
import org.vaadin.addons.componentfactory.sidenavrail.SideNavRailItem;

/**
 * Covers §9.2 keyboard navigation. Three root items: "Dashboard" (leaf),
 * "Code" (has two flat children), "Admin" (has one parent-child + a leaf).
 * The Admin subtree lets us exercise tree-like expand/collapse via arrow keys.
 */
@Route("keyboard-navigation")
public class KeyboardNavigationView extends VerticalLayout {

    public KeyboardNavigationView() {
        SideNavRail rail = new SideNavRail();
        rail.setId("rail");

        SideNavRailItem dashboard = new SideNavRailItem("Dashboard", "/dashboard", VaadinIcon.DASHBOARD.create());
        SideNavRailItem code = new SideNavRailItem("Code", "/code", VaadinIcon.CODE.create());
        code.addItem(new SideNavRailItem("Branches", "/code/branches"));
        code.addItem(new SideNavRailItem("Commits", "/code/commits"));

        SideNavRailItem admin = new SideNavRailItem("Admin", "/admin", VaadinIcon.COG.create());
        SideNavRailItem users = new SideNavRailItem("Users", "/admin/users");
        users.addItem(new SideNavRailItem("Active", "/admin/users/active"));
        users.addItem(new SideNavRailItem("Archived", "/admin/users/archived"));
        admin.addItem(users);
        admin.addItem(new SideNavRailItem("Roles", "/admin/roles"));

        rail.addItem(dashboard, code, admin);

        Button toggle = new Button("Toggle rail",
                e -> rail.setRailMode(!rail.isRailMode()));
        toggle.setId("toggle-rail");

        Button togglePopoverOnly = new Button("Toggle popover-only",
                e -> rail.setChildrenOnlyInPopover(!rail.isChildrenOnlyInPopover()));
        togglePopoverOnly.setId("toggle-popover-only");

        add(new HorizontalLayout(rail, toggle, togglePopoverOnly));
    }
}
