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
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.popover.PopoverPosition;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.shared.Registration;
import java.util.function.Consumer;

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

    static final String RAIL_TOOLTIP_ATTRIBUTE = "data-rail-tooltip";

    private static final int DEFAULT_POPOVER_HOVER_DELAY_MS = 200;
    private static final int DEFAULT_POPOVER_HIDE_DELAY_MS = 300;
    private static final PopoverPosition DEFAULT_POPOVER_POSITION = PopoverPosition.END_TOP;

    private boolean railMode = false;
    private PopoverOn popoverOn = PopoverOn.ALL_COLLAPSED_ITEMS;
    private PopoverHeaderMode popoverHeaderMode = PopoverHeaderMode.LABEL_ONLY;
    private boolean popoverHeaderOnlyInRailMode = true;
    private RailTooltipMode railTooltipMode = RailTooltipMode.POPOVER_HEADER;
    private int popoverHoverDelay = DEFAULT_POPOVER_HOVER_DELAY_MS;
    private int popoverHideDelay = DEFAULT_POPOVER_HIDE_DELAY_MS;
    private PopoverPosition popoverPosition = DEFAULT_POPOVER_POSITION;
    private boolean popoverArrowVisible = true;
    private RootMatchNested rootMatchNested = RootMatchNested.ONLY_RAIL;
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
        // Auto-coerce: a leaf popover with no header would have no content, so
        // when POPOVER_HEADER mode is configured against the default NONE header,
        // we silently upgrade the header to LABEL_ONLY at attach time. Runtime
        // setters remain un-validated; the demo prevents the invalid combo
        // via disabled select options.
        if (railTooltipMode == RailTooltipMode.POPOVER_HEADER
                && popoverHeaderMode == PopoverHeaderMode.NONE) {
            popoverHeaderMode = PopoverHeaderMode.LABEL_ONLY;
        }
        // The JS module registers itself on window.vaadinAddonsSideNavRail at
        // @JsModule import time (see side-nav-rail.js). We call the global
        // directly instead of a dynamic import() because dynamic imports
        // inside executeJs do not resolve reliably in Vaadin's production bundle.
        attachEvent.getUI().getPage().executeJs(
                "window.vaadinAddonsSideNavRail.init($0);",
                getElement());
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        super.onDetach(detachEvent);
        detachEvent.getUI().getPage().executeJs(
                "window.vaadinAddonsSideNavRail.dispose($0);",
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
        refreshAllPopoversFromOwner();
        applyAriaToRootItems();
        applyFocusTriggerToRootItems();
        applyNestedTabindex();
        applyRootMatchNested();
        if (popoverHeaderMode != PopoverHeaderMode.NONE
                && popoverHeaderOnlyInRailMode) {
            rebuildPopoverContents();
        }
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
     * <p>Has no effect on root items with children while
     * {@link #setChildrenOnlyInPopover(boolean)} is enabled — that mode forces the
     * popover on regardless of this setting.</p>
     *
     * @param mode the new {@link PopoverOn}; must not be {@code null}
     * @throws NullPointerException if {@code mode} is {@code null}
     */
    public void setPopoverOn(PopoverOn mode) {
        this.popoverOn = java.util.Objects.requireNonNull(mode, "PopoverOn must not be null");
        updatePopoverGating();
    }

    /**
     * The current header mode for popovers. Default:
     * {@link PopoverHeaderMode#LABEL_ONLY}. See {@link PopoverHeaderMode} for
     * the rendering rules per value.
     *
     * @return the active {@link PopoverHeaderMode}; never {@code null}
     */
    public PopoverHeaderMode getPopoverHeaderMode() {
        return popoverHeaderMode;
    }

    /**
     * Sets whether (and how) each popover renders a header identifying the
     * owning item. Rebuilds the content of all existing popovers so the change
     * is visible immediately.
     *
     * <p>The header is rail-mode-only by default — see
     * {@link #setPopoverHeaderOnlyInRailMode(boolean)} to also show it in normal
     * mode (e.g. together with {@link #setChildrenOnlyInPopover(boolean)}).
     *
     * @param mode the new {@link PopoverHeaderMode}; must not be {@code null}
     * @throws NullPointerException if {@code mode} is {@code null}
     */
    public void setPopoverHeaderMode(PopoverHeaderMode mode) {
        this.popoverHeaderMode = java.util.Objects.requireNonNull(
                mode, "PopoverHeaderMode must not be null");
        rebuildPopoverContents();
    }

    /**
     * Whether the popover header is rendered only while the rail is in
     * rail mode. Default: {@code true} (header is hidden in normal mode, where the
     * item's label is already visible inline). Has no effect while the
     * {@link #getPopoverHeaderMode() header mode} is
     * {@link PopoverHeaderMode#NONE}.
     *
     * @return {@code true} if the header is restricted to rail mode, {@code false} if
     *     it is rendered in both modes
     */
    public boolean isPopoverHeaderOnlyInRailMode() {
        return popoverHeaderOnlyInRailMode;
    }

    /**
     * Restricts the popover header to rail mode, or allows it in both
     * modes. Default: {@code true}. Useful in combination with
     * {@link #setChildrenOnlyInPopover(boolean)}, where the popover is the only place
     * children appear even in normal mode and an item-identifying header may still be
     * desired.
     *
     * <p>Has no visible effect while {@link #getPopoverHeaderMode()} is
     * {@link PopoverHeaderMode#NONE}. Rebuilds the content of all existing
     * popovers so the change is visible immediately.
     *
     * @param onlyInRailMode {@code true} to render the header only in rail mode,
     *     {@code false} to render it in both modes
     */
    public void setPopoverHeaderOnlyInRailMode(boolean onlyInRailMode) {
        if (this.popoverHeaderOnlyInRailMode == onlyInRailMode) {
            return;
        }
        this.popoverHeaderOnlyInRailMode = onlyInRailMode;
        rebuildPopoverContents();
    }

    /**
     * The current rail-tooltip mode. Default: {@link RailTooltipMode#POPOVER_HEADER}.
     * Tooltips are only shown while the rail is in rail mode; see
     * {@link RailTooltipMode} for the per-value semantics.
     *
     * @return the active {@link RailTooltipMode}; never {@code null}
     */
    public RailTooltipMode getRailTooltipMode() {
        return railTooltipMode;
    }

    /**
     * Controls how root items surface their label while the rail is in rail mode.
     * Re-applies the tooltip state to every root item immediately. See
     * {@link RailTooltipMode} for the per-value semantics.
     *
     * @param mode the new {@link RailTooltipMode}; must not be {@code null}
     * @throws NullPointerException if {@code mode} is {@code null}
     */
    public void setRailTooltipMode(RailTooltipMode mode) {
        this.railTooltipMode = java.util.Objects.requireNonNull(
                mode, "RailTooltipMode must not be null");
        applyTooltips();
        refreshAllPopoversFromOwner();
    }

    /**
     * Whether the rail currently asks {@link SideNavRailItem} to create a popover for a
     * leaf (childless) root item, so the popover can act as a Vaadin-themed tooltip.
     * True iff rail mode is active and {@link RailTooltipMode#POPOVER_HEADER} is selected.
     *
     * @return {@code true} if leaf items should have a popover, {@code false} otherwise
     */
    public boolean isLeafPopoverActive() {
        return railMode && railTooltipMode == RailTooltipMode.POPOVER_HEADER;
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
     * <p>While enabled, this overrides {@link #setPopoverOn(PopoverOn)}: the hover
     * popover is always shown on root items with children (in both rail and normal
     * mode), since it is the only path to their children.</p>
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

    private void forEachRootRailItem(Consumer<SideNavRailItem> action) {
        for (SideNavItem child : getItems()) {
            if (child instanceof SideNavRailItem rail) {
                action.accept(rail);
            }
        }
    }

    private void forEachRailItemRecursive(Consumer<SideNavRailItem> action) {
        forEachRootRailItem(root -> applyRecursive(root, action));
    }

    private static void applyRecursive(SideNavRailItem item, Consumer<SideNavRailItem> action) {
        action.accept(item);
        for (SideNavItem child : item.getItems()) {
            if (child instanceof SideNavRailItem rail) {
                applyRecursive(rail, action);
            }
        }
    }

    private void updatePopoverGating() {
        forEachRailItemRecursive(SideNavRailItem::applyPopoverGating);
    }

    private void rebuildPopoverContents() {
        forEachRailItemRecursive(SideNavRailItem::rebuildPopoverContent);
    }

    private void applyPopoverSettings() {
        forEachRailItemRecursive(SideNavRailItem::applyPopoverSettings);
    }

    private void applyTooltips() {
        forEachRootRailItem(this::applyTooltipFor);
    }

    private void refreshAllPopoversFromOwner() {
        forEachRootRailItem(SideNavRailItem::refreshPopoverFromOwner);
    }

    private void applyRootMatchNested() {
        boolean override = isRootMatchNestedActive();
        forEachRootRailItem(item -> item.applyRailMatchNestedOverride(override));
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
        forEachRootRailItem(item -> item.applyAriaAttributes(railMode));
    }

    private void applyFocusTriggerToRootItems() {
        forEachRootRailItem(item -> item.applyFocusTrigger(railMode));
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
     * rail-mode and {@link RailTooltipMode}. Always wipes the {@code data-rail-tooltip}
     * attribute first so flipping the mode doesn't leave a stale attribute on the item.
     */
    private void applyTooltipFor(SideNavItem item) {
        item.getElement().removeAttribute(RAIL_TOOLTIP_ATTRIBUTE);
        if (!railMode || railTooltipMode == RailTooltipMode.NONE) {
            return;
        }
        String label = item.getLabel();
        if (label == null || label.isBlank()) {
            return;
        }
        switch (railTooltipMode) {
            case SIMPLE -> item.getElement().setAttribute(RAIL_TOOLTIP_ATTRIBUTE, label);
            case POPOVER_HEADER -> { /* No attribute; leaf-popover wiring handles this. */ }
            case NONE -> { /* Already short-circuited by the early return above. */ }
        }
    }

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
            wireRootItem(item, overrideMatchNested);
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
        wireRootItem(item, isRootMatchNestedActive());
    }

    /**
     * {@inheritDoc}
     *
     * @param index where to insert the item
     * @param item the {@link SideNavRailItem} to insert
     * @throws IllegalArgumentException if the item is not a {@link SideNavRailItem};
     *     see {@link #addItem(SideNavItem...)}
     */
    @Override
    public void addItemAtIndex(int index, SideNavItem item) {
        requireRailItem(item);
        super.addItemAtIndex(index, item);
        wireRootItem(item, isRootMatchNestedActive());
    }

    private void wireRootItem(SideNavItem item, boolean overrideMatchNested) {
        markAsRootItem(item);
        applyTooltipFor(item);
        if (item instanceof SideNavRailItem rail) {
            rail.applyAriaAttributes(railMode);
            if (overrideMatchNested) {
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

    /** Sets the {@code [root-item]} attribute used as a CSS hook (see README). */
    private static void markAsRootItem(SideNavItem item) {
        item.getElement().setAttribute("root-item", "");
    }
}
