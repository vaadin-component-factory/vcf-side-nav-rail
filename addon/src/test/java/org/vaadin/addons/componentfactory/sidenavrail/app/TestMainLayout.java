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
package org.vaadin.addons.componentfactory.sidenavrail.app;

import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.Layout;
import org.vaadin.addons.componentfactory.sidenavrail.SideNavRail;
import org.vaadin.addons.componentfactory.sidenavrail.SideNavRailItem;

@Layout
public class TestMainLayout extends AppLayout {

    private final SideNavRail nav = new SideNavRail();

    public TestMainLayout() {
        nav.addItem(new SideNavRailItem("Basic", "basic"));
        nav.addItem(new SideNavRailItem("Collapsed Item", "collapsed-item"));
        nav.addItem(new SideNavRailItem("Rail Only", "rail-only"));
        nav.addItem(new SideNavRailItem("Nested", "nested"));

        Button toggle = new Button("Toggle rail",
                e -> nav.setRailMode(!nav.isRailMode()));
        toggle.setId("toggle-rail");

        addToNavbar(new HorizontalLayout(toggle));
        addToDrawer(nav);
    }
}
