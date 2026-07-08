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

import com.vaadin.flow.component.sidenav.SideNavItem;

/**
 * Controls when the {@link SideNavRail} forces {@code matchNested = true} on every root item, so a
 * root carries the {@code [current]} highlight whenever any descendant route is active. The
 * original per-item value is snapshotted on activation and restored when the override is turned off
 * again, so a {@link SideNavItem#setMatchNested(boolean)} call the application made itself survives
 * toggling this feature on and off.
 */
public enum RootMatchNested {

    /**
     * No automatic override. Each root item keeps whatever {@code matchNested} value the
     * application set on it (default for plain {@link SideNavItem} is {@code false}). Matches the
     * standard {@link com.vaadin.flow.component.sidenav.SideNav} behaviour.
     */
    NONE,

    /**
     * Override is active only while the rail is in rail mode ({@link SideNavRail#isRailMode()}
     * {@code == true}). This is the default. Useful for the typical use case: in rail mode the
     * active route is no longer visually anchored to an expanded parent, so forcing the root to
     * highlight when a descendant route is current restores that orientation cue. The original
     * {@code matchNested} value is restored when leaving rail mode.
     */
    ONLY_RAIL,

    /**
     * Override is always active, regardless of rail mode. Useful in combination with {@link
     * SideNavRail#setChildrenOnlyInPopover(boolean)} or any other configuration where the visible
     * inline tree doesn't expose deeper levels, so the root needs to carry the {@code [current]}
     * marker even in normal mode.
     */
    ALL
}
