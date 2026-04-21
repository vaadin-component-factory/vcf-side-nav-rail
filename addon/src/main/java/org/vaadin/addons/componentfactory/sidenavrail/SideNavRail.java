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

package org.vaadin.addons.componentfactory.sidenavrail;

import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.shared.Registration;

/**
 * A {@link SideNav} that can be switched between normal mode and a compact rail
 * (icon-only) mode. Items with children may show a hover popover — see
 * {@link PopoverMode}.
 */
@CssImport("./side-nav-rail.css")
public class SideNavRail extends SideNav {

    private static final String RAIL_THEME = "rail";

    private boolean railMode = false;
    private PopoverMode popoverMode = PopoverMode.COLLAPSED_ITEM;

    public SideNavRail() {
        super();
    }

    public SideNavRail(String label) {
        super(label);
    }

    public void setRailMode(boolean railMode) {
        if (this.railMode == railMode) {
            return;
        }
        this.railMode = railMode;
        if (railMode) {
            getElement().setAttribute("theme", RAIL_THEME);
        } else {
            getElement().removeAttribute("theme");
        }
        updatePopoverGating();
        ComponentUtil.fireEvent(this, new RailModeChangedEvent(this, false, railMode));
    }

    public boolean isRailMode() {
        return railMode;
    }

    public PopoverMode getPopoverMode() {
        return popoverMode;
    }

    public void setPopoverMode(PopoverMode mode) {
        this.popoverMode = java.util.Objects.requireNonNull(mode, "PopoverMode must not be null");
        updatePopoverGating();
    }

    private void updatePopoverGating() {
        getChildren()
                .filter(c -> c instanceof SideNavRailItem)
                .map(c -> (SideNavRailItem) c)
                .forEach(i -> i.applyPopoverGating(popoverMode, railMode));
    }

    public Registration addRailModeChangedListener(
            ComponentEventListener<RailModeChangedEvent> listener) {
        return ComponentUtil.addListener(this, RailModeChangedEvent.class, listener);
    }
}
