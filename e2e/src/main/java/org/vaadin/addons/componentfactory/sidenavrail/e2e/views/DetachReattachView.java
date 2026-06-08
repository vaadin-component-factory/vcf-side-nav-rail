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
 * View for detach/reattach + setVisible E2E tests. Exposes buttons to: - detach the rail from its
 * parent layout and reattach it - flip rail.setVisible - flip a specific item's setVisible - mutate
 * popover settings while the rail is detached, so a follow-up reattach can verify the new settings
 * actually took effect
 */
@Route("detach-reattach")
public class DetachReattachView extends VerticalLayout {

    private final HorizontalLayout railHolder = new HorizontalLayout();
    private final SideNavRail rail = new SideNavRail();
    private final SideNavRailItem code;

    public DetachReattachView() {
        rail.setId("rail");

        SideNavRailItem dashboard =
                new SideNavRailItem("Dashboard", "/dashboard", VaadinIcon.DASHBOARD.create());
        code = new SideNavRailItem("Code", "/code", VaadinIcon.CODE.create());
        code.addItem(new SideNavRailItem("Branches", "/code/branches"));
        code.addItem(new SideNavRailItem("Commits", "/code/commits"));
        SideNavRailItem admin = new SideNavRailItem("Admin", "/admin", VaadinIcon.COG.create());
        admin.addItem(new SideNavRailItem("Users", "/admin/users"));
        rail.addItem(dashboard, code, admin);

        railHolder.add(rail);

        Button toggleRail =
                new Button("Toggle rail mode", e -> rail.setRailMode(!rail.isRailMode()));
        toggleRail.setId("toggle-rail");

        Button detach = new Button("Detach rail", e -> railHolder.remove(rail));
        detach.setId("detach-rail");

        Button reattach =
                new Button(
                        "Reattach rail",
                        e -> {
                            if (rail.getParent().isEmpty()) {
                                railHolder.add(rail);
                            }
                        });
        reattach.setId("reattach-rail");

        Button toggleRailVisible =
                new Button("Toggle rail visible", e -> rail.setVisible(!rail.isVisible()));
        toggleRailVisible.setId("toggle-rail-visible");

        Button toggleCodeVisible =
                new Button("Toggle Code visible", e -> code.setVisible(!code.isVisible()));
        toggleCodeVisible.setId("toggle-code-visible");

        Button changeHoverDelay =
                new Button("Hover delay 750", e -> rail.setPopoverHoverDelay(750));
        changeHoverDelay.setId("change-hover-delay");

        HorizontalLayout buttons =
                new HorizontalLayout(
                        toggleRail,
                        detach,
                        reattach,
                        toggleRailVisible,
                        toggleCodeVisible,
                        changeHoverDelay);
        add(railHolder, buttons);
    }
}
