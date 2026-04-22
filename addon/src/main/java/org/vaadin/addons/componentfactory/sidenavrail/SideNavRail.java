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
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.shared.Registration;

/**
 * A {@link SideNav} that can be switched into a compact icon-only <em>rail</em> mode
 * and shows a hover popover for items with children. Use with {@link SideNavRailItem}
 * for the item tree; plain {@link com.vaadin.flow.component.sidenav.SideNavItem}s work
 * but won't participate in the CSS label-wrap or popover gating.
 *
 * <p>The component toggles between two visual states:
 *
 * <ul>
 *   <li><b>Normal mode</b> — behaves like a standard {@code SideNav}.
 *   <li><b>Rail mode</b> — the {@code theme="rail"} attribute is set on the root
 *       element and bundled CSS collapses the nav to a narrow icon-only column.
 *       Inline expansion state is preserved (just visually suppressed) and restored
 *       when switching back.
 * </ul>
 *
 * <p>Typical usage inside a layout:
 *
 * <pre>{@code
 * SideNavRail rail = new SideNavRail();
 * rail.addItem(new SideNavRailItem("Dashboard", DashboardView.class, VaadinIcon.DASHBOARD.create()));
 * // ...
 * Button toggle = new Button("Rail", e -> rail.setRailMode(!rail.isRailMode()));
 * }</pre>
 *
 * <p>The component does not ship with a toggle button — applications integrate one where
 * it fits their layout. This is intentional: auto-collapse, persistent state, and
 * responsive breakpoint handling are the application's concern.
 */
@CssImport("./side-nav-rail.css")
public class SideNavRail extends SideNav {

    private static final String RAIL_THEME = "rail";

    private boolean railMode = false;
    private PopoverMode popoverMode = PopoverMode.ALL_COLLAPSED_ITEMS;
    private PopoverParentLabelMode popoverParentLabelMode = PopoverParentLabelMode.NONE;

    /** Creates an unlabelled rail. */
    public SideNavRail() {
        super();
    }

    /**
     * Creates a rail with a header label. See
     * {@link SideNav#setLabel(String)} for the semantics — the label renders above the
     * item list and can be used to group multiple {@code SideNav} instances visually.
     */
    public SideNavRail(String label) {
        super(label);
    }

    /**
     * Switches the rail between normal mode and icon-only rail mode. No-op if the
     * state is unchanged; otherwise fires a {@link RailModeChangedEvent}.
     */
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

    /** Whether the rail is currently in icon-only mode. */
    public boolean isRailMode() {
        return railMode;
    }

    /**
     * The current popover mode. Default: {@link PopoverMode#ALL_COLLAPSED_ITEMS}.
     * See {@link PopoverMode} for the full behaviour matrix.
     */
    public PopoverMode getPopoverMode() {
        return popoverMode;
    }

    /**
     * Sets the popover mode. Rewires all child items' popover eligibility immediately
     * so open popovers that are no longer eligible close right away.
     *
     * @throws NullPointerException if {@code mode} is {@code null}
     */
    public void setPopoverMode(PopoverMode mode) {
        this.popoverMode = java.util.Objects.requireNonNull(mode, "PopoverMode must not be null");
        updatePopoverGating();
    }

    /**
     * The current parent-label mode for popover headers. Default:
     * {@link PopoverParentLabelMode#NONE}. See {@link PopoverParentLabelMode} for the
     * rendering rules per value.
     */
    public PopoverParentLabelMode getPopoverParentLabelMode() {
        return popoverParentLabelMode;
    }

    /**
     * Sets whether (and how) each popover renders a header identifying its parent item.
     * Rebuilds the content of all existing popovers so the change is visible immediately.
     *
     * @throws NullPointerException if {@code mode} is {@code null}
     */
    public void setPopoverParentLabelMode(PopoverParentLabelMode mode) {
        this.popoverParentLabelMode = java.util.Objects.requireNonNull(
                mode, "PopoverParentLabelMode must not be null");
        rebuildPopoverContents();
    }

    private void updatePopoverGating() {
        for (SideNavItem child : getItems()) {
            if (child instanceof SideNavRailItem rail) {
                applyGatingRecursively(rail);
            }
        }
    }

    private void applyGatingRecursively(SideNavRailItem item) {
        item.applyPopoverGating(popoverMode, railMode);
        for (SideNavItem child : item.getItems()) {
            if (child instanceof SideNavRailItem rail) {
                applyGatingRecursively(rail);
            }
        }
    }

    private void rebuildPopoverContents() {
        for (SideNavItem child : getItems()) {
            if (child instanceof SideNavRailItem rail) {
                rebuildPopoverContentsRecursively(rail);
            }
        }
    }

    private void rebuildPopoverContentsRecursively(SideNavRailItem item) {
        item.rebuildPopoverContent();
        for (SideNavItem child : item.getItems()) {
            if (child instanceof SideNavRailItem rail) {
                rebuildPopoverContentsRecursively(rail);
            }
        }
    }

    /**
     * Registers a listener for {@link RailModeChangedEvent}. The event fires whenever
     * {@link #setRailMode(boolean)} actually changes the state (no-ops don't fire).
     *
     * @return registration that can be used to remove the listener
     */
    public Registration addRailModeChangedListener(
            ComponentEventListener<RailModeChangedEvent> listener) {
        return ComponentUtil.addListener(this, RailModeChangedEvent.class, listener);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Only accepts {@link SideNavRailItem} instances. Passing a plain
     * {@link SideNavItem} throws {@link IllegalArgumentException} — the label wrap and
     * popover lifecycle depend on {@code SideNavRailItem}'s overrides and cannot be
     * retrofitted onto a parent-class instance.
     *
     * @throws IllegalArgumentException if any item is not a {@link SideNavRailItem}
     */
    @Override
    public void addItem(SideNavItem... items) {
        for (SideNavItem item : items) {
            requireRailItem(item);
        }
        super.addItem(items);
        for (SideNavItem item : items) {
            markAsRootItem(item);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @throws IllegalArgumentException if the item is not a {@link SideNavRailItem};
     *     see {@link #addItem(SideNavItem...)}
     */
    @Override
    public void addItemAsFirst(SideNavItem item) {
        requireRailItem(item);
        super.addItemAsFirst(item);
        markAsRootItem(item);
    }

    static void requireRailItem(SideNavItem item) {
        if (!(item instanceof SideNavRailItem)) {
            throw new IllegalArgumentException(
                    "SideNavRail accepts only SideNavRailItem children — got "
                            + item.getClass().getName()
                            + ". Use new SideNavRailItem(...) instead.");
        }
    }

    /**
     * Marks an item as a direct child of the rail so app-level CSS can target it
     * separately from nested items. The attribute is set on the item's root element
     * and is consumed exclusively by consumer stylesheets — the addon itself does
     * not style it. Typical use:
     *
     * <pre>{@code
     * vaadin-side-nav-item[root-item]:has([current]) > vaadin-icon {
     *     color: var(--lumo-primary-color);
     * }
     * }</pre>
     *
     * <p>Combine with {@link com.vaadin.flow.component.sidenav.SideNavItem#setMatchNested}
     * if you want the root to carry {@code [current]} when a descendant route is active.
     */
    private static void markAsRootItem(SideNavItem item) {
        item.getElement().setAttribute("root-item", "");
    }
}
