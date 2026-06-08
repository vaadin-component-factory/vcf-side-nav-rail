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
 * Two SideNavRails on the same page so cross-rail JS-module interactions can be exercised: per-rail
 * keydown listeners, the document-level activation closer's ownership filter, and the document-wide
 * popover lookup in moveFocusRightOnRailRoot.
 */
@Route("multi-rail")
public class MultiRailView extends VerticalLayout {

    public MultiRailView() {
        SideNavRail railA = buildRail("rail-a", "code-a", "admin-a");
        SideNavRail railB = buildRail("rail-b", "code-b", "admin-b");

        Button toggleA = new Button("Toggle A", e -> railA.setRailMode(!railA.isRailMode()));
        toggleA.setId("toggle-rail-a");
        Button toggleB = new Button("Toggle B", e -> railB.setRailMode(!railB.isRailMode()));
        toggleB.setId("toggle-rail-b");

        add(new HorizontalLayout(railA, railB, toggleA, toggleB));
    }

    private static SideNavRail buildRail(String id, String codePath, String adminPath) {
        SideNavRail rail = new SideNavRail();
        rail.setId(id);

        SideNavRailItem code =
                new SideNavRailItem("Code", "/" + codePath, VaadinIcon.CODE.create());
        code.addItem(new SideNavRailItem("Branches", "/" + codePath + "/branches"));
        code.addItem(new SideNavRailItem("Commits", "/" + codePath + "/commits"));

        SideNavRailItem admin =
                new SideNavRailItem("Admin", "/" + adminPath, VaadinIcon.COG.create());
        admin.addItem(new SideNavRailItem("Users", "/" + adminPath + "/users"));
        rail.addItem(code, admin);
        return rail;
    }
}
