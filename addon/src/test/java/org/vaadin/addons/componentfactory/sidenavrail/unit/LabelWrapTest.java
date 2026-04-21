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

package org.vaadin.addons.componentfactory.sidenavrail.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vaadin.flow.dom.Element;
import org.junit.jupiter.api.Test;
import org.vaadin.addons.componentfactory.sidenavrail.SideNavRailItem;

class LabelWrapTest {

    @Test
    void constructorWrapsLabelInSpan() {
        SideNavRailItem item = new SideNavRailItem("Dashboard");
        Element span = findLabelSpan(item.getElement());
        assertEquals("span", span.getTag());
        assertEquals("label", span.getAttribute("class"));
        assertEquals("Dashboard", span.getText());
    }

    @Test
    void setLabelUpdatesExistingSpanWithoutDuplicating() {
        SideNavRailItem item = new SideNavRailItem("Dashboard");
        item.setLabel("Overview");

        long spanCount = item.getElement().getChildren()
                .filter(e -> "span".equals(e.getTag()))
                .filter(e -> "label".equals(e.getAttribute("class")))
                .count();
        assertEquals(1L, spanCount, "Expected exactly one label span");
        assertEquals("Overview", findLabelSpan(item.getElement()).getText());
    }

    @Test
    void wrappedLabelLeavesSuperLabelInSync() {
        SideNavRailItem item = new SideNavRailItem("Dashboard");
        assertEquals("Dashboard", item.getLabel());
        item.setLabel("Overview");
        assertEquals("Overview", item.getLabel());
    }

    @Test
    void prefixSlottedIconSurvivesLabelWrap() {
        com.vaadin.flow.component.icon.Icon icon =
                com.vaadin.flow.component.icon.VaadinIcon.DASHBOARD.create();
        SideNavRailItem item = new SideNavRailItem("Dashboard", "/", icon);

        boolean iconStillSlotted = item.getElement().getChildren()
                .anyMatch(e -> "prefix".equals(e.getAttribute("slot")));
        assertTrue(iconStillSlotted, "Prefix icon should survive the label wrap");
    }

    private static Element findLabelSpan(Element root) {
        return root.getChildren()
                .filter(e -> "span".equals(e.getTag()))
                .filter(e -> "label".equals(e.getAttribute("class")))
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "No <span class=\"label\"> found on element " + root.getOuterHTML()));
    }
}
