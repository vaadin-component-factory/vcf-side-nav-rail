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
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.router.RouterLayout;
import org.vaadin.addons.componentfactory.sidenavrail.SideNavRail;
import org.vaadin.addons.componentfactory.sidenavrail.SideNavRailItem;
import org.vaadin.addons.componentfactory.sidenavrail.demo.views.*;

/** This class shows how to use the SideNavRail in the app layout. */
// @Layout // commented out to not run into conflicts
public class AppLayoutSample extends AppLayout implements RouterLayout {

    private final SideNavRail nav;

    public AppLayoutSample() {
        getStyle()
                .set(
                        "--vaadin-app-layout-drawer-width",
                        "auto"); // important for LUMO, so that the drawer takes minimal space

        nav = new SideNavRail();

        SideNavRailItem dashboard =
                new SideNavRailItem("Dashboard", ShowcaseView.class, VaadinIcon.DASHBOARD.create());

        SideNavRailItem branches = new SideNavRailItem("Branches", BranchesView.class);

        SideNavRailItem activeBranches = new SideNavRailItem("Active", ActiveBranchesView.class);
        activeBranches.addItem(new SideNavRailItem("main", MainBranchView.class));
        activeBranches.addItem(new SideNavRailItem("develop", DevelopBranchView.class));
        activeBranches.addItem(new SideNavRailItem("feature/*", FeatureBranchesView.class));
        branches.addItem(activeBranches);

        branches.addItem(new SideNavRailItem("Stale", StaleBranchesView.class));
        branches.addItem(new SideNavRailItem("Archived", ArchivedBranchesView.class));

        SideNavRailItem code =
                new SideNavRailItem("Code", CodeView.class, VaadinIcon.CODE.create());
        code.addItem(branches);

        SideNavRailItem commits = new SideNavRailItem("Commits", CommitsView.class);
        code.addItem(commits);
        code.addItem(new SideNavRailItem("Tags", TagsView.class));

        nav.addItem(dashboard, branches, code);

        Button toggle =
                new Button(
                        VaadinIcon.CHEVRON_LEFT_SMALL.create(),
                        e -> {
                            boolean railMode = !nav.isRailMode();
                            nav.setRailMode(railMode);
                            e.getSource()
                                    .setIcon(
                                            (railMode
                                                            ? VaadinIcon.CHEVRON_RIGHT_SMALL
                                                            : VaadinIcon.CHEVRON_LEFT_SMALL)
                                                    .create());
                        });
        toggle.addThemeVariants(ButtonVariant.LUMO_SMALL);
        toggle.getStyle().setWidth("fit-content");

        addToNavbar(new DrawerToggle());
        addToDrawer(nav, toggle);
    }
}
