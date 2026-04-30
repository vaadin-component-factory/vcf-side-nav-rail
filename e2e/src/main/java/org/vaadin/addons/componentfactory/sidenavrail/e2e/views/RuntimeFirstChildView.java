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
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import org.vaadin.addons.componentfactory.sidenavrail.SideNavRail;
import org.vaadin.addons.componentfactory.sidenavrail.SideNavRailItem;

/**
 * Fixture for the runtime-leaf-becomes-parent regression. {@code parent}
 * starts attached with no children — exactly the dynamic-projects-demo
 * pattern where a "Projects" container exists upfront and per-project
 * sub-items are activated later. The button adds three children at runtime
 * so the spec can verify the expand→popover-gating wiring kicks in for an
 * item that didn't have it at first attach.
 */
@Route("runtime-first-child")
public class RuntimeFirstChildView extends VerticalLayout {

    public RuntimeFirstChildView() {
        SideNavRail rail = new SideNavRail();
        rail.setId("rail");

        SideNavRailItem parent = new SideNavRailItem("Projects");
        rail.addItem(parent);

        Button addChildren = new Button("Add children", e -> {
            if (parent.getItems().isEmpty()) {
                parent.addItem(new SideNavRailItem("Phoenix"));
                parent.addItem(new SideNavRailItem("Atlas"));
                parent.addItem(new SideNavRailItem("Voyager"));
            }
        });
        addChildren.setId("add-children");

        add(new HorizontalLayout(rail, addChildren));
    }
}
