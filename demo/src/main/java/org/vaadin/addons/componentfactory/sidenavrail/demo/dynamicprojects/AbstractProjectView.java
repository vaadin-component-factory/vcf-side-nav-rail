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

import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;

/**
 * Base class for project sub-views. Pulls the {@code :projectId} route
 * parameter and renders the matching project's display label as the heading.
 * Implements {@link BeforeEnterObserver} as a guard: when the project ID is
 * not in the {@link ActiveProjectsRegistry session-scoped active set}, the
 * navigation is forwarded to the {@link DynamicProjectsHomeView} and a
 * transient notification explains why.
 */
abstract class AbstractProjectView extends VerticalLayout
        implements BeforeEnterObserver {

    private final String pageLabel;
    private final H2 heading = new H2();
    private final Paragraph body = new Paragraph();

    protected AbstractProjectView(String pageLabel) {
        this.pageLabel = pageLabel;
        setPadding(false);
        setSpacing(false);
        add(heading, body);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        String projectId = event.getRouteParameters().get("projectId").orElse(null);
        Project project = projectId == null ? null : Project.byId(projectId);
        if (project == null || !ActiveProjectsRegistry.current().isActive(projectId)) {
            Notification.show(
                    "Project '" + projectId + "' is not active. "
                            + "Activate it via the navbar to view its pages.",
                    3500, Notification.Position.MIDDLE);
            event.forwardTo(DynamicProjectsHomeView.class);
            return;
        }
        heading.setText(project.label() + " — " + pageLabel);
        body.setText("Stub content for " + project.label() + "'s "
                + pageLabel.toLowerCase() + " page.");
    }
}
