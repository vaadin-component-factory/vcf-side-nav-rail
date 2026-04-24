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
 * Dedicated route for §9.4 a11y E2E assertions. Structure mirrors
 * KeyboardNavigationView but the two views stay decoupled so that a
 * future change to the keyboard tests cannot regress a11y tests.
 *
 * Layout:
 *   - Dashboard : leaf (no children)                  — exercises "no aria-haspopup"
 *   - Code      : two flat children (Branches, Commits) — flat subtree
 *   - Admin     : Users (nested: Active, Archived) + Roles — deeply nested
 *
 * A toggle button (#toggle-rail) flips rail mode so tests can exercise
 * the rail-on / rail-off transitions.
 */
@Route("accessibility")
public class AccessibilityView extends VerticalLayout {

    public AccessibilityView() {
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

        add(new HorizontalLayout(rail, toggle));
    }
}
