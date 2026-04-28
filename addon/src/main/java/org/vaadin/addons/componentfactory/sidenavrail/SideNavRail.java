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

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.dependency.JsModule;
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
@JsModule("./side-nav-rail.js")
public class SideNavRail extends SideNav {

    private static final String RAIL_THEME = "rail";
    private static final String CHILDREN_ONLY_IN_POPOVER_THEME = "inline-children-hidden";

    private static final int DEFAULT_POPOVER_HOVER_DELAY_MS = 200;
    private static final int DEFAULT_POPOVER_HIDE_DELAY_MS = 300;
    private static final PopoverPosition DEFAULT_POPOVER_POSITION = PopoverPosition.END_TOP;

    private boolean railMode = false;
    private PopoverOn popoverOn = PopoverOn.ALL_COLLAPSED_ITEMS;
    private PopoverParentLabelMode popoverParentLabelMode = PopoverParentLabelMode.NONE;
    private RailTooltipMode railTooltipMode = RailTooltipMode.ALL;
    private int popoverHoverDelay = DEFAULT_POPOVER_HOVER_DELAY_MS;
    private int popoverHideDelay = DEFAULT_POPOVER_HIDE_DELAY_MS;
    private PopoverPosition popoverPosition = DEFAULT_POPOVER_POSITION;
    private boolean railTooltipNative = false;
    private boolean popoverArrowVisible = true;
    private RootMatchNested rootMatchNested = RootMatchNested.NONE;
    private boolean childrenOnlyInPopover = false;

    /** Creates an unlabelled rail. */
    public SideNavRail() {
        super();
    }

    /**
     * Creates a rail with a header label. See
     * {@link SideNav#setLabel(String)} for the semantics — the label renders above the
     * item list and can be used to group multiple {@code SideNav} instances visually.
     *
     * @param label the header label rendered above the item list; may be {@code null}
     *     for an unlabelled rail
     */
    public SideNavRail(String label) {
        super(label);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        // The JS module registers itself on window.vaadinAddonsSideNavRail at
        // @JsModule import time (see side-nav-rail.js). We call the global
        // directly instead of a dynamic import() because dynamic imports
        // inside executeJs do not resolve reliably in Vaadin's production bundle.
        attachEvent.getUI().getPage().executeJs(
                "window.vaadinAddonsSideNavRail.init($0);",
                getElement());
    }

    /**
     * Switches the rail between normal mode and icon-only rail mode. Fires a {@link RailModeChangedEvent}.
     */
    public void toggleRailMode() {
        setRailMode(!isRailMode());
    }


    /**
     * Switches the rail between normal mode and icon-only rail mode. No-op if the
     * state is unchanged; otherwise fires a {@link RailModeChangedEvent}.
     *
     * @param railMode {@code true} to enter rail mode (icon-only), {@code false} to
     *     return to normal mode (full-width with labels)
     */
    public void setRailMode(boolean railMode) {
        if (this.railMode == railMode) {
            return;
        }
        this.railMode = railMode;
        if (railMode) {
            getElement().getThemeList().add(RAIL_THEME);
        } else {
            getElement().getThemeList().remove(RAIL_THEME);
        }
        updatePopoverGating();
        applyTooltips();
        applyAriaToRootItems();
        applyFocusTriggerToRootItems();
        applyNestedTabindex();
        applyRootMatchNested();
        ComponentUtil.fireEvent(this, new RailModeChangedEvent(this, false, railMode));
    }

    /**
     * Whether the rail is currently in icon-only mode.
     *
     * @return {@code true} if the rail is in rail mode, {@code false} in normal mode
     */
    public boolean isRailMode() {
        return railMode;
    }

    /**
     * The current popover mode. Default: {@link PopoverOn#ALL_COLLAPSED_ITEMS}.
     * See {@link PopoverOn} for the full behaviour matrix.
     *
     * @return the active {@link PopoverOn}; never {@code null}
     */
    public PopoverOn getPopoverOn() {
        return popoverOn;
    }

    /**
     * Sets the popover mode. Rewires all child items' popover eligibility immediately
     * so open popovers that are no longer eligible close right away.
     *
     * @param mode the new {@link PopoverOn}; must not be {@code null}
     * @throws NullPointerException if {@code mode} is {@code null}
     */
    public void setPopoverOn(PopoverOn mode) {
        this.popoverOn = java.util.Objects.requireNonNull(mode, "PopoverOn must not be null");
        updatePopoverGating();
    }

    /**
     * The current parent-label mode for popover headers. Default:
     * {@link PopoverParentLabelMode#NONE}. See {@link PopoverParentLabelMode} for the
     * rendering rules per value.
     *
     * @return the active {@link PopoverParentLabelMode}; never {@code null}
     */
    public PopoverParentLabelMode getPopoverParentLabelMode() {
        return popoverParentLabelMode;
    }

    /**
     * Sets whether (and how) each popover renders a header identifying its parent item.
     * Rebuilds the content of all existing popovers so the change is visible immediately.
     *
     * @param mode the new {@link PopoverParentLabelMode}; must not be {@code null}
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
     *
     * @return the active {@link RailTooltipMode}; never {@code null}
     */
    public RailTooltipMode getRailTooltipMode() {
        return railTooltipMode;
    }

    /**
     * Controls which root items surface their label as a tooltip while the rail is in
     * rail mode. Re-applies the tooltip state to every root item immediately.
     *
     * @param mode the new {@link RailTooltipMode}; must not be {@code null}
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
     *
     * @return {@code true} if tooltips are rendered via the native {@code title}
     *     attribute, {@code false} if via the addon's CSS pseudo-element
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
     * {@code title} works, including assistive technologies. Native tooltips do not
     * appear on keyboard focus.
     *
     * @param useNative {@code true} to render tooltips via the browser's {@code title}
     *     attribute, {@code false} to use the addon's CSS pseudo-element
     */
    public void setRailTooltipNative(boolean useNative) {
        this.railTooltipNative = useNative;
        applyTooltips();
    }

    /**
     * The hover delay (ms) before the popover opens. Default: 200&nbsp;ms (Lumo-typical).
     *
     * @return the hover delay in milliseconds
     */
    public int getPopoverHoverDelay() {
        return popoverHoverDelay;
    }

    /**
     * Sets the hover delay (ms) before the popover opens. Applied to every existing
     * popover immediately. Negative values behave as Vaadin's {@code Popover} defines
     * them — the addon does not validate.
     *
     * @param hoverDelayMs the new hover delay in milliseconds
     */
    public void setPopoverHoverDelay(int hoverDelayMs) {
        this.popoverHoverDelay = hoverDelayMs;
        applyPopoverSettings();
    }

    /**
     * The hide delay (ms) after the pointer leaves the target before the popover closes.
     * Default: 300&nbsp;ms (Lumo-typical).
     *
     * @return the hide delay in milliseconds
     */
    public int getPopoverHideDelay() {
        return popoverHideDelay;
    }

    /**
     * Sets the hide delay (ms) after the pointer leaves the target before the popover
     * closes. Applied to every existing popover immediately.
     *
     * @param hideDelayMs the new hide delay in milliseconds
     */
    public void setPopoverHideDelay(int hideDelayMs) {
        this.popoverHideDelay = hideDelayMs;
        applyPopoverSettings();
    }

    /**
     * The position the popover opens at relative to its item. Default:
     * {@link PopoverPosition#END_TOP} — top-aligned, to the inline-end of the item
     * (right in an LTR layout). Suitable for a rail pinned to the inline-start edge.
     *
     * @return the active {@link PopoverPosition}; never {@code null}
     */
    public PopoverPosition getPopoverPosition() {
        return popoverPosition;
    }

    /**
     * Sets the popover position. Applied to every existing popover immediately.
     *
     * @param position the new {@link PopoverPosition}; must not be {@code null}
     * @throws NullPointerException if {@code position} is {@code null}
     */
    public void setPopoverPosition(PopoverPosition position) {
        this.popoverPosition = java.util.Objects.requireNonNull(
                position, "PopoverPosition must not be null");
        applyPopoverSettings();
    }

    /**
     * Whether each popover renders the small Lumo arrow pointing back at its target
     * item. Default: {@code true} (arrow visible — applies the {@code arrow} theme
     * variant of {@code <vaadin-popover>}).
     *
     * @return {@code true} if the arrow is shown, {@code false} if hidden
     */
    public boolean isPopoverArrowVisible() {
        return popoverArrowVisible;
    }

    /**
     * Sets whether each popover renders the small Lumo arrow pointing back at its
     * target item. Applied to every existing popover immediately.
     *
     * @param visible {@code true} to show the arrow (default), {@code false} to hide
     *     it (cleaner look when popovers are tightly packed against the rail)
     */
    public void setPopoverArrowVisible(boolean visible) {
        this.popoverArrowVisible = visible;
        applyPopoverSettings();
    }

    /**
     * Whether nested items are rendered only inside the hover popover, never inline
     * below their parent. Default: {@code false} (children are visible inline and
     * mirrored into the popover — Vaadin's standard behaviour).
     *
     * @return {@code true} if inline children are suppressed
     */
    public boolean isChildrenOnlyInPopover() {
        return childrenOnlyInPopover;
    }

    /**
     * Switches between the standard inline-and-popover layout for nested items
     * ({@code false}, default) and a popover-only layout ({@code true}). When
     * enabled, nested {@code <vaadin-side-nav-item>}s are CSS-hidden and the
     * chevron toggle is suppressed, so the only path to a parent's children is
     * the hover popover. Useful for navigation designs that want a flat,
     * non-tree appearance in the rail itself.
     *
     * <p>Vaadin's auto-expand-on-route-match still fires server-side, but has no
     * visual effect while this is on; switching the flag back to {@code false}
     * restores the default tree appearance with whatever expanded state the
     * items had accumulated.
     *
     * @param enabled {@code true} to hide inline children and route everything
     *     through the popover, {@code false} to use the default layout
     */
    public void setChildrenOnlyInPopover(boolean enabled) {
        this.childrenOnlyInPopover = enabled;
        if (enabled) {
            getElement().getThemeList().add(CHILDREN_ONLY_IN_POPOVER_THEME);
        } else {
            getElement().getThemeList().remove(CHILDREN_ONLY_IN_POPOVER_THEME);
        }
        updatePopoverGating();
    }

    /**
     * The current {@link RootMatchNested} mode. Default: {@link RootMatchNested#NONE}.
     * See {@link RootMatchNested} for the per-value semantics.
     *
     * @return the active {@link RootMatchNested}; never {@code null}
     */
    public RootMatchNested getRootMatchNested() {
        return rootMatchNested;
    }

    /**
     * Sets the {@link RootMatchNested} mode. The override is applied (or rolled back)
     * on every root item immediately, snapshotting the original {@code matchNested}
     * value the first time it is forced so the user-set value can be restored when
     * the override is disabled again.
     *
     * @param mode the new {@link RootMatchNested}; must not be {@code null}
     * @throws NullPointerException if {@code mode} is {@code null}
     */
    public void setRootMatchNested(RootMatchNested mode) {
        this.rootMatchNested = java.util.Objects.requireNonNull(
                mode, "RootMatchNested must not be null");
        applyRootMatchNested();
    }

    private void updatePopoverGating() {
        for (SideNavItem child : getItems()) {
            if (child instanceof SideNavRailItem rail) {
                applyGatingRecursively(rail);
            }
        }
    }

    private void applyGatingRecursively(SideNavRailItem item) {
        item.applyPopoverGating(popoverOn, railMode, childrenOnlyInPopover);
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
        item.applyPopoverSettings(
                popoverHoverDelay, popoverHideDelay, popoverPosition, popoverArrowVisible);
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

    /**
     * Applies (or rolls back) the {@code matchNested} override on every root item
     * based on the current ({@link RootMatchNested}, {@code railMode}) pair.
     * Per-item snapshotting lives on {@link SideNavRailItem}.
     */
    private void applyRootMatchNested() {
        boolean override = isRootMatchNestedActive();
        for (SideNavItem child : getItems()) {
            if (child instanceof SideNavRailItem rail) {
                rail.applyRailMatchNestedOverride(override);
            }
        }
    }

    /**
     * Whether the {@link RootMatchNested} override should currently be active given
     * the configured mode and the current rail-mode state.
     */
    private boolean isRootMatchNestedActive() {
        return switch (rootMatchNested) {
            case NONE -> false;
            case ONLY_RAIL -> railMode;
            case ALL -> true;
        };
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
     * @param listener the listener to invoke on each rail-mode transition; must not be
     *     {@code null}
     * @return a {@link Registration} handle that can be used to remove the listener
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
     * @param items the {@link SideNavRailItem} children to append to the rail
     * @throws IllegalArgumentException if any item is not a {@link SideNavRailItem}
     */
    @Override
    public void addItem(SideNavItem... items) {
        for (SideNavItem item : items) {
            requireRailItem(item);
        }
        super.addItem(items);
        boolean overrideMatchNested = isRootMatchNestedActive();
        for (SideNavItem item : items) {
            markAsRootItem(item);
            applyTooltipFor(item);
            if (item instanceof SideNavRailItem rail) {
                rail.applyAriaAttributes(railMode);
                if (overrideMatchNested) {
                    rail.applyRailMatchNestedOverride(true);
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param item the {@link SideNavRailItem} to prepend to the rail
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
            if (isRootMatchNestedActive()) {
                rail.applyRailMatchNestedOverride(true);
            }
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
