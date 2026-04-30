/*
 * Copyright 2026 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */
package org.vaadin.addons.componentfactory.sidenavrail.demo.dynamicprojects;

import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.AfterNavigationObserver;
import com.vaadin.flow.router.Layout;
import com.vaadin.flow.router.RouteParameters;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.shared.Registration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import org.vaadin.addons.componentfactory.sidenavrail.SideNavRail;
import org.vaadin.addons.componentfactory.sidenavrail.SideNavRailItem;

/**
 * Layout for the dynamic-projects demo. Hosts the rail and a navbar with a
 * {@link MultiSelectComboBox} that drives a {@link ActiveProjectsRegistry
 * session-scoped registry of active projects}. The registry's change events
 * mutate the rail's "Projects" subtree at runtime: each activated project
 * gets its own {@link SideNavRailItem} with three sub-items (Overview /
 * Issues / Settings) wired to the resolved {@code :projectId} route paths.
 */
@Layout("dynamic-projects")
@CssImport("./demo-styles.css")
public class DynamicProjectsLayout extends VerticalLayout
        implements RouterLayout, AfterNavigationObserver {

    private final Div contentArea = new Div();
    private final Span activeItemBreadcrumb = new Span();
    private final SideNavRail nav = new SideNavRail();
    private final SideNavRailItem projectsParent = new SideNavRailItem("Projects");
    /** Tracks rail items per project ID so {@link #removeProject(String)} can remove them. */
    private final Map<String, SideNavRailItem> projectItems = new HashMap<>();
    private final MultiSelectComboBox<Project> projectSelect = new MultiSelectComboBox<>();

    private Registration registryListenerHandle;

    public DynamicProjectsLayout() {
        setSizeFull();
        setPadding(false);
        setSpacing(false);

        SideNavRailItem home = new SideNavRailItem(
                "Home", "dynamic-projects", VaadinIcon.HOME.create());
        projectsParent.setPrefixComponent(VaadinIcon.FOLDER_O.create());
        nav.addItem(home, projectsParent);

        Button toggle = new Button(VaadinIcon.CHEVRON_LEFT_SMALL.create(), e -> {
            boolean railMode = !nav.isRailMode();
            nav.setRailMode(railMode);
            e.getSource().setIcon((railMode
                    ? VaadinIcon.CHEVRON_RIGHT_SMALL
                    : VaadinIcon.CHEVRON_LEFT_SMALL).create());
        });

        VerticalLayout sidebar = new VerticalLayout(toggle, nav);
        sidebar.setPadding(false);
        sidebar.setSpacing(false);
        sidebar.setWidth(null);
        sidebar.getStyle().set("border-right", "1px solid var(--lumo-contrast-10pct)");

        activeItemBreadcrumb.setId("active-item-breadcrumb");
        activeItemBreadcrumb.getStyle()
                .set("font-size", "var(--lumo-font-size-s)")
                .set("color", "var(--lumo-secondary-text-color)")
                .set("margin-bottom", "var(--lumo-space-s)");

        VerticalLayout content = new VerticalLayout(activeItemBreadcrumb, contentArea);
        content.setPadding(false);
        content.setSpacing(false);
        content.setSizeFull();
        content.getStyle().set("padding", "var(--lumo-space-m)");
        content.setFlexGrow(1, contentArea);

        HorizontalLayout body = new HorizontalLayout(sidebar, content);
        body.setSizeFull();
        body.setPadding(false);
        body.setSpacing(false);
        body.setFlexGrow(1, content);

        add(buildNavbar(), body);
        setFlexGrow(1, body);

        addAttachListener(e -> bindToRegistry());
        addDetachListener(e -> unbindFromRegistry());
    }

    private HorizontalLayout buildNavbar() {
        Span title = new Span("Dynamic projects demo");
        title.getStyle()
                .set("font-weight", "600")
                .set("font-size", "var(--lumo-font-size-l)");

        projectSelect.setId("active-projects-select");
        projectSelect.setLabel("Active projects");
        projectSelect.setItems(Project.ALL);
        projectSelect.setItemLabelGenerator(Project::label);
        projectSelect.setPlaceholder("Activate projects…");
        projectSelect.addValueChangeListener(e -> syncRegistryFromSelection(e.getValue()));

        HorizontalLayout navbar = new HorizontalLayout(title, projectSelect);
        navbar.setWidthFull();
        navbar.setPadding(true);
        navbar.setSpacing(true);
        navbar.setAlignItems(FlexComponent.Alignment.CENTER);
        navbar.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        navbar.getStyle().set("border-bottom", "1px solid var(--lumo-contrast-10pct)");
        return navbar;
    }

    private void bindToRegistry() {
        ActiveProjectsRegistry registry = ActiveProjectsRegistry.current();
        // Snap the multiselect and rail to the registry's current state on attach,
        // so a session that already had projects active when the user navigates back
        // to this layout still shows them correctly.
        Set<Project> currentlyActive = new LinkedHashSet<>();
        for (String id : registry.getActive()) {
            Project p = Project.byId(id);
            if (p != null) {
                currentlyActive.add(p);
                addProject(p);
            }
        }
        projectSelect.setValue(currentlyActive);
        registryListenerHandle = registry.addChangeListener(this::onRegistryChange);
    }

    private void unbindFromRegistry() {
        if (registryListenerHandle != null) {
            registryListenerHandle.remove();
            registryListenerHandle = null;
        }
        // Drop the rail's project items so a future re-attach doesn't double-render
        // them on top of the bindToRegistry()-driven re-add.
        projectsParent.removeAll();
        projectItems.clear();
    }

    private void syncRegistryFromSelection(Set<Project> selected) {
        ActiveProjectsRegistry registry = ActiveProjectsRegistry.current();
        Set<String> selectedIds = new HashSet<>();
        for (Project p : selected) selectedIds.add(p.id());
        // Activate newly-selected, deactivate removed. The registry no-ops on
        // already-matching transitions, so order does not matter.
        for (Project p : Project.ALL) {
            if (selectedIds.contains(p.id())) {
                registry.activate(p.id());
            } else {
                registry.deactivate(p.id());
            }
        }
    }

    private void onRegistryChange(ActiveProjectsRegistry.ChangeEvent event) {
        Project project = Project.byId(event.projectId());
        if (project == null) return;
        if (event.active()) {
            addProject(project);
        } else {
            removeProject(event.projectId());
        }
        // Keep the multiselect's value in sync if the change was driven by
        // something other than the multiselect itself (e.g. another tab in
        // the same session, or programmatic activation).
        Set<Project> selected = new LinkedHashSet<>();
        for (String id : ActiveProjectsRegistry.current().getActive()) {
            Project p = Project.byId(id);
            if (p != null) selected.add(p);
        }
        if (!selected.equals(projectSelect.getValue())) {
            projectSelect.setValue(selected);
        }
    }

    private void addProject(Project project) {
        if (projectItems.containsKey(project.id())) return;
        SideNavRailItem item = new SideNavRailItem(project.label());
        RouteParameters params = new RouteParameters("projectId", project.id());
        item.addItem(
                projectChild("Overview", ProjectOverviewView.class, params),
                projectChild("Issues", ProjectIssuesView.class, params),
                projectChild("Settings", ProjectSettingsView.class, params));
        projectsParent.addItem(item);
        projectItems.put(project.id(), item);
    }

    private void removeProject(String projectId) {
        SideNavRailItem item = projectItems.remove(projectId);
        if (item != null) {
            projectsParent.remove(item);
        }
    }

    private static SideNavRailItem projectChild(
            String label,
            Class<? extends com.vaadin.flow.component.Component> view,
            RouteParameters params) {
        SideNavRailItem child = new SideNavRailItem(label);
        child.setPath(view, params);
        return child;
    }

    /**
     * Mirrors the original {@code MainLayout} breadcrumb pattern — re-render
     * the active item's label after every navigation. Fires for the initial
     * navigation too, so the first paint already shows the resolved label.
     */
    @Override
    public void afterNavigation(AfterNavigationEvent event) {
        activeItemBreadcrumb.setText(nav.getActiveViewItem()
                .map(item -> "Active: " + item.getLabel())
                .orElse("Active: (no match)"));
    }

    @Override
    public void showRouterLayoutContent(HasElement content) {
        contentArea.removeAll();
        if (content != null) {
            contentArea.getElement().appendChild(content.getElement());
        }
    }
}
