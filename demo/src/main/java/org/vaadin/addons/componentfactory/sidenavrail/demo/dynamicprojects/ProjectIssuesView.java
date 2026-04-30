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

import com.vaadin.flow.router.Route;

@Route("dynamic-projects/projects/:projectId/issues")
public class ProjectIssuesView extends AbstractProjectView {
    public ProjectIssuesView() {
        super("Issues");
    }
}
