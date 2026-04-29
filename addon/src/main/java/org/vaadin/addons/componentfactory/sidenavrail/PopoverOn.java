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

/**
 * Controls when the hover popover on a {@link SideNavRailItem} with children appears.
 *
 * <p>Has no effect on root items with children while
 * {@link SideNavRail#setChildrenOnlyInPopover(boolean)} is enabled — that mode
 * forces the popover on regardless of this enum.</p>
 */
public enum PopoverOn {

    /**
     * Popover appears for every non-expanded item with children, regardless of depth in
     * the hierarchy and regardless of whether the nav is in rail mode. This is the
     * default and mirrors the customer-requested behaviour of {@link SideNavRail}.
     */
    ALL_COLLAPSED_ITEMS,

    /**
     * Popover appears only for non-expanded items that are <em>direct children</em> of
     * the {@link SideNavRail} — not for nested items further down the tree. Useful when
     * only top-level navigation should get the hover preview but deeper levels should
     * behave like a plain {@link com.vaadin.flow.component.sidenav.SideNav}.
     *
     * <p>In rail mode this is effectively the same as {@link #ALL_COLLAPSED_ITEMS}
     * because rail mode hides nested items anyway.
     */
    ONLY_ROOT_COLLAPSED_ITEMS,

    /**
     * Popover appears only when the nav as a whole is in rail mode
     * ({@link SideNavRail#isRailMode()} {@code == true}). Inline-closed items in normal
     * mode behave like a standard {@link com.vaadin.flow.component.sidenav.SideNav} —
     * they only open on click.
     */
    ONLY_RAIL_MODE
}
