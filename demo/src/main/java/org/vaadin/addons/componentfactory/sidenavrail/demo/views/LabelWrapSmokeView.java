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

import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.router.Route;
import org.vaadin.addons.componentfactory.sidenavrail.SideNavRail;
import org.vaadin.addons.componentfactory.sidenavrail.SideNavRailItem;

/**
 * Compare the rendering of a standard {@link SideNav} with {@link SideNavRail} side by
 * side — the text in both should be pixel-identical.
 */
@Route("smoke/label-wrap")
public class LabelWrapSmokeView extends HorizontalLayout {

    public LabelWrapSmokeView() {
        setPadding(true);
        setSpacing(true);

        VerticalLayout left = new VerticalLayout(new H2("SideNav (standard)"), standardSideNav());
        VerticalLayout right = new VerticalLayout(new H2("SideNavRail"), sideNavRail());

        add(left, right);
    }

    private SideNav standardSideNav() {
        SideNav nav = new SideNav();
        nav.addItem(new SideNavItem("Dashboard", "/", VaadinIcon.DASHBOARD.create()));
        nav.addItem(new SideNavItem("Code", "/code", VaadinIcon.CODE.create()));
        return nav;
    }

    private SideNavRail sideNavRail() {
        SideNavRail nav = new SideNavRail();
        nav.addItem(new SideNavRailItem("Dashboard", "/", VaadinIcon.DASHBOARD.create()));
        nav.addItem(new SideNavRailItem("Code", "/code", VaadinIcon.CODE.create()));
        return nav;
    }
}
