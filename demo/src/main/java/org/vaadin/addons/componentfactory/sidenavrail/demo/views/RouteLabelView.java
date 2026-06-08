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
package org.vaadin.addons.componentfactory.sidenavrail.demo.views;

import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;

/**
 * Shared base for the demo's placeholder views — every navigable route just renders "You are on
 * /&lt;path&gt;" so the SideNav's active-route highlight has something to land on and navigation
 * actually works without 404s.
 */
abstract class RouteLabelView extends VerticalLayout implements BeforeEnterObserver {

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        removeAll();
        add(new Span("You are on /" + event.getLocation().getPath()));
    }
}
