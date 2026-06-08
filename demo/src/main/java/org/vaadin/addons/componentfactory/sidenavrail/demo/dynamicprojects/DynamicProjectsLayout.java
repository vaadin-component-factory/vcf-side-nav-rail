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
 * Layout for the dynamic-projects demo. Hosts the rail and a navbar with a {@link
 * MultiSelectComboBox} that drives a {@link ActiveProjectsRegistry session-scoped registry of
 * active projects}. All projects are always present in the rail as childless leaves under the
 * "Projects" parent; activation adds three sub-items (Overview / Issues / Settings) wired to the
 * resolved {@code :projectId} route paths, deactivation removes them again. The project leaf itself
 * is never added or removed at runtime — only its children list mutates.
 */
@Layout("dynamic-projects")
@CssImport("./demo-styles.css")
public class DynamicProjectsLayout extends VerticalLayout
        implements RouterLayout, AfterNavigationObserver {

    private final Div contentArea = new Div();
    private final Span activeItemBreadcrumb = new Span();
    private final SideNavRail nav = new SideNavRail();
    private final SideNavRailItem projectsParent = new SideNavRailItem("Projects");

    /** Project leaf for each project ID, created upfront and reused across activate/deactivate. */
    private final Map<String, SideNavRailItem> projectItems = new HashMap<>();

    private final MultiSelectComboBox<Project> projectSelect = new MultiSelectComboBox<>();

    private Registration registryListenerHandle;

    public DynamicProjectsLayout() {
        setSizeFull();
        setPadding(false);
        setSpacing(false);

        SideNavRailItem home =
                new SideNavRailItem("Home", "dynamic-projects", VaadinIcon.HOME.create());
        projectsParent.setPrefixComponent(VaadinIcon.FOLDER_O.create());
        nav.addItem(home, projectsParent);

        // Pre-populate the Projects parent with all known projects as childless
        // leaves. Children (Overview/Issues/Settings) are added on activation and
        // removed on deactivation, but the project leaf itself stays put.
        for (Project p : Project.ALL) {
            SideNavRailItem item = new SideNavRailItem(p.label());
            projectsParent.addItem(item);
            projectItems.put(p.id(), item);
        }

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

        VerticalLayout sidebar = new VerticalLayout(toggle, nav);
        sidebar.setPadding(false);
        sidebar.setSpacing(false);
        sidebar.setWidth(null);
        sidebar.getStyle().set("border-right", "1px solid var(--lumo-contrast-10pct)");

        activeItemBreadcrumb.setId("active-item-breadcrumb");
        activeItemBreadcrumb
                .getStyle()
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
        title.getStyle().set("font-weight", "600").set("font-size", "var(--lumo-font-size-l)");

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
        // Sync each project leaf's children with the registry. addProjectChildren
        // / removeProjectChildren are idempotent, so we can blindly diff the full
        // ALL set against the active set.
        Set<String> active = registry.getActive();
        Set<Project> currentlyActive = new LinkedHashSet<>();
        for (Project p : Project.ALL) {
            if (active.contains(p.id())) {
                addProjectChildren(p.id());
                currentlyActive.add(p);
            } else {
                removeProjectChildren(p.id());
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
        // Project leaves persist across detach/reattach — children are reapplied
        // from the registry on every {@link #bindToRegistry()} so no cleanup needed.
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
        if (event.active()) {
            addProjectChildren(event.projectId());
        } else {
            removeProjectChildren(event.projectId());
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

    private void addProjectChildren(String projectId) {
        SideNavRailItem item = projectItems.get(projectId);
        if (item == null || !item.getItems().isEmpty()) return;
        RouteParameters params = new RouteParameters("projectId", projectId);
        item.addItem(
                projectChild("Overview", ProjectOverviewView.class, params),
                projectChild("Issues", ProjectIssuesView.class, params),
                projectChild("Settings", ProjectSettingsView.class, params));
    }

    private void removeProjectChildren(String projectId) {
        SideNavRailItem item = projectItems.get(projectId);
        if (item == null || item.getItems().isEmpty()) return;
        item.removeAll();
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
     * Mirrors the original {@code MainLayout} breadcrumb pattern — re-render the active item's
     * label after every navigation. Fires for the initial navigation too, so the first paint
     * already shows the resolved label.
     */
    @Override
    public void afterNavigation(AfterNavigationEvent event) {
        activeItemBreadcrumb.setText(
                nav.getActiveViewItem()
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
