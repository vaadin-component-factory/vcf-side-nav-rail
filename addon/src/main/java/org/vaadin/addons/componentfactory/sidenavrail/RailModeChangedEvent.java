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

import com.vaadin.flow.component.ComponentEvent;

/**
 * Fired when {@link SideNavRail#setRailMode(boolean)} actually changes the rail-mode
 * state. No-op calls (same value) do not fire. Register via
 * {@link SideNavRail#addRailModeChangedListener}.
 */
public class RailModeChangedEvent extends ComponentEvent<SideNavRail> {

    private final boolean railMode;

    /**
     * @param source the nav that changed state
     * @param fromClient whether the change originated on the client (currently always
     *                   {@code false} since the mode is only driven server-side)
     * @param railMode the new rail-mode value
     */
    public RailModeChangedEvent(SideNavRail source, boolean fromClient,
            boolean railMode) {
        super(source, fromClient);
        this.railMode = railMode;
    }

    /**
     * The new rail-mode value after the change.
     *
     * @return {@code true} if the rail just switched into rail mode, {@code false} if
     *     it switched back to normal mode
     */
    public boolean isRailMode() {
        return railMode;
    }
}
