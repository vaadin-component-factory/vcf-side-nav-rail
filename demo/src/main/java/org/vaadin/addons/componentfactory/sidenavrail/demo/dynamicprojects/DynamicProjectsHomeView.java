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
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

@Route("dynamic-projects")
public class DynamicProjectsHomeView extends VerticalLayout {

    public DynamicProjectsHomeView() {
        setPadding(false);
        setSpacing(false);
        add(new H2("Dynamic projects demo"));
        add(
                new Paragraph(
                        "Activate one or more projects via the multiselect at the top "
                                + "right of the navbar. Each activated project appears under "
                                + "\"Projects\" in the rail with its own Overview / Issues / "
                                + "Settings pages. Deactivating removes the rail entries and "
                                + "blocks direct URL access to that project's pages."));
    }
}
