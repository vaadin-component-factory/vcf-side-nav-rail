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
import org.vaadin.addons.componentfactory.sidenavrail.SideNavRail;
import org.vaadin.addons.componentfactory.sidenavrail.SideNavRailItem;

/**
 * Exercises the letter-avatar fallback: a root item without a prefix icon ("Admin") should display
 * an auto-generated avatar in rail mode. A reference root item with a real icon ("Dashboard")
 * guards against the fallback leaking onto items that already have one.
 */
@Route("letter-avatar-fallback")
public class LetterAvatarFallbackView extends VerticalLayout {

    public LetterAvatarFallbackView() {
        SideNavRail rail = new SideNavRail();
        rail.setId("rail");

        rail.addItem(
                new SideNavRailItem("Dashboard", "/", VaadinIcon.DASHBOARD.create()),
                new SideNavRailItem("Admin", "/admin"));

        Button toggle = new Button("Toggle rail", e -> rail.setRailMode(!rail.isRailMode()));
        toggle.setId("toggle-rail");

        add(new HorizontalLayout(rail, toggle));
    }
}
