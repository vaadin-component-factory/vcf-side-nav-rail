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
import com.vaadin.flow.component.popover.PopoverPosition;
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

    private static final int DEFAULT_POPOVER_HOVER_DELAY_MS = 200;
    private static final int DEFAULT_POPOVER_HIDE_DELAY_MS = 300;
    private static final PopoverPosition DEFAULT_POPOVER_POSITION = PopoverPosition.END_TOP;

    private boolean railMode = false;
    private PopoverMode popoverMode = PopoverMode.ALL_COLLAPSED_ITEMS;
    private PopoverParentLabelMode popoverParentLabelMode = PopoverParentLabelMode.NONE;
    private RailTooltipMode railTooltipMode = RailTooltipMode.ALL;
    private int popoverHoverDelay = DEFAULT_POPOVER_HOVER_DELAY_MS;
    private int popoverHideDelay = DEFAULT_POPOVER_HIDE_DELAY_MS;
    private PopoverPosition popoverPosition = DEFAULT_POPOVER_POSITION;
    private boolean railTooltipNative = false;

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
        applyTooltips();
        applyAriaToRootItems();
        applyFocusTriggerToRootItems();
        applyNestedTabindex();
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

    /**
     * The current rail-tooltip mode. Default: {@link RailTooltipMode#ALL}. Tooltips
     * are only shown while the rail is in rail mode; see {@link RailTooltipMode} for
     * the per-value semantics.
     */
    public RailTooltipMode getRailTooltipMode() {
        return railTooltipMode;
    }

    /**
     * Controls which root items surface their label as a native tooltip while the rail
     * is in rail mode. Re-applies the tooltip state to every root item immediately.
     *
     * @throws NullPointerException if {@code mode} is {@code null}
     */
    public void setRailTooltipMode(RailTooltipMode mode) {
        this.railTooltipMode = java.util.Objects.requireNonNull(
                mode, "RailTooltipMode must not be null");
        applyTooltips();
    }

    /**
     * Whether rail-mode tooltips are rendered as browser-native tooltips (via the
     * {@code title} HTML attribute) rather than the addon's CSS pseudo-element.
     * Default: {@code false} (pseudo-element, Lumo-styled, immune to
     * {@code vaadin-tooltip-mixin}'s overlay dismissal).
     */
    public boolean isRailTooltipNative() {
        return railTooltipNative;
    }

    /**
     * Switches between the addon's pseudo-element tooltip ({@code false}, default) and
     * the browser-native {@code title} tooltip ({@code true}).
     *
     * <p>The native tooltip has a fixed browser-decided delay (roughly 500&nbsp;ms) and
     * browser-decided styling and position, so it won't react to
     * {@link #setPopoverHoverDelay(int)} and may look inconsistent with the rest of the
     * Vaadin UI — but it carries zero overlay-interaction risk and works everywhere
     * {@code title} works, including assistive technologies.
     */
    public void setRailTooltipNative(boolean native_) {
        this.railTooltipNative = native_;
        applyTooltips();
    }

    /**
     * The hover delay (ms) before the popover opens. Default: 200&nbsp;ms (Lumo-typical).
     */
    public int getPopoverHoverDelay() {
        return popoverHoverDelay;
    }

    /**
     * Sets the hover delay (ms) before the popover opens. Applied to every existing
     * popover immediately. Negative values behave as Vaadin's {@code Popover} defines
     * them — the addon does not validate.
     */
    public void setPopoverHoverDelay(int hoverDelayMs) {
        this.popoverHoverDelay = hoverDelayMs;
        applyPopoverSettings();
    }

    /**
     * The hide delay (ms) after the pointer leaves the target before the popover closes.
     * Default: 300&nbsp;ms (Lumo-typical).
     */
    public int getPopoverHideDelay() {
        return popoverHideDelay;
    }

    /**
     * Sets the hide delay (ms) after the pointer leaves the target before the popover
     * closes. Applied to every existing popover immediately.
     */
    public void setPopoverHideDelay(int hideDelayMs) {
        this.popoverHideDelay = hideDelayMs;
        applyPopoverSettings();
    }

    /**
     * The position the popover opens at relative to its item. Default:
     * {@link PopoverPosition#END_TOP} — top-aligned, to the inline-end of the item
     * (right in an LTR layout). Suitable for a rail pinned to the inline-start edge.
     */
    public PopoverPosition getPopoverPosition() {
        return popoverPosition;
    }

    /**
     * Sets the popover position. Applied to every existing popover immediately.
     *
     * @throws NullPointerException if {@code position} is {@code null}
     */
    public void setPopoverPosition(PopoverPosition position) {
        this.popoverPosition = java.util.Objects.requireNonNull(
                position, "PopoverPosition must not be null");
        applyPopoverSettings();
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

    private void applyPopoverSettings() {
        for (SideNavItem child : getItems()) {
            if (child instanceof SideNavRailItem rail) {
                applyPopoverSettingsRecursively(rail);
            }
        }
    }

    private void applyPopoverSettingsRecursively(SideNavRailItem item) {
        item.applyPopoverSettings(popoverHoverDelay, popoverHideDelay, popoverPosition);
        for (SideNavItem child : item.getItems()) {
            if (child instanceof SideNavRailItem rail) {
                applyPopoverSettingsRecursively(rail);
            }
        }
    }

    private void applyTooltips() {
        for (SideNavItem child : getItems()) {
            applyTooltipFor(child);
        }
    }

    private void applyAriaToRootItems() {
        for (SideNavItem child : getItems()) {
            if (child instanceof SideNavRailItem rail) {
                rail.applyAriaAttributes(railMode);
            }
        }
    }

    private void applyFocusTriggerToRootItems() {
        for (SideNavItem child : getItems()) {
            if (child instanceof SideNavRailItem rail) {
                rail.applyFocusTrigger(railMode);
            }
        }
    }

    private void applyNestedTabindex() {
        for (SideNavItem root : getItems()) {
            for (SideNavItem nested : root.getItems()) {
                applyNestedTabindexRecursive(nested);
            }
        }
    }

    private void applyNestedTabindexRecursive(SideNavItem item) {
        if (railMode) {
            item.getElement().setAttribute("tabindex", "-1");
        } else {
            item.getElement().removeAttribute("tabindex");
        }
        for (SideNavItem child : item.getItems()) {
            applyNestedTabindexRecursive(child);
        }
    }

    /**
     * Sets or clears the tooltip attribute on a single root item based on the current
     * rail-mode, {@link RailTooltipMode}, and native-vs-custom toggle. Always wipes
     * both the native {@code title} and the custom {@code data-rail-tooltip} first so
     * flipping the native flag doesn't leave the old attribute on the item.
     */
    private void applyTooltipFor(SideNavItem item) {
        item.getElement().removeAttribute(RAIL_TOOLTIP_ATTRIBUTE);
        item.getElement().removeAttribute(NATIVE_TOOLTIP_ATTRIBUTE);

        boolean eligible = railMode && switch (railTooltipMode) {
            case NONE -> false;
            case ONLY_WITHOUT_CHILDREN -> item.getItems().isEmpty();
            case ALL -> true;
        };
        String label = item.getLabel();
        if (eligible && label != null && !label.isBlank()) {
            String attr = railTooltipNative ? NATIVE_TOOLTIP_ATTRIBUTE : RAIL_TOOLTIP_ATTRIBUTE;
            item.getElement().setAttribute(attr, label);
        }
    }

    static final String RAIL_TOOLTIP_ATTRIBUTE = "data-rail-tooltip";
    static final String NATIVE_TOOLTIP_ATTRIBUTE = "title";

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
            applyTooltipFor(item);
            if (item instanceof SideNavRailItem rail) {
                rail.applyAriaAttributes(railMode);
            }
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
        applyTooltipFor(item);
        if (item instanceof SideNavRailItem rail) {
            rail.applyAriaAttributes(railMode);
        }
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
