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

import java.util.List;

/**
 * Demo project — fixed catalogue of three entries used by the dynamic-projects demo. The {@code id}
 * field appears in URLs as the {@code :projectId} route parameter, the {@code label} is what users
 * see in the rail and the navbar's multiselect.
 */
public record Project(String id, String label) {

    public static final Project PHOENIX = new Project("phoenix", "Phoenix");
    public static final Project ATLAS = new Project("atlas", "Atlas");
    public static final Project VOYAGER = new Project("voyager", "Voyager");

    public static final List<Project> ALL = List.of(PHOENIX, ATLAS, VOYAGER);

    public static Project byId(String id) {
        return ALL.stream().filter(p -> p.id.equals(id)).findFirst().orElse(null);
    }
}
