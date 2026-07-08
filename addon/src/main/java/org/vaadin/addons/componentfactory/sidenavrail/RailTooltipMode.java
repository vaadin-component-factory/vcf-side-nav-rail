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
 * Controls how root items of a {@link SideNavRail} surface their identity while the rail is in rail
 * mode. Tooltips are never shown in normal mode — the label text identifies the item in that case.
 *
 * <p>The label source is the item's own {@code getLabel()}. Tooltips apply to direct children of
 * the rail only; nested items never get a tooltip from the rail.
 *
 * <p>Both active modes ({@link #SIMPLE}, {@link #POPOVER_HEADER}) apply to <em>every</em> rail-mode
 * root item. Combine with {@link PopoverHeaderMode} when {@link #POPOVER_HEADER} is selected to
 * control what the popover header shows.
 */
public enum RailTooltipMode {

    /** No tooltips on root items. */
    NONE,

    /**
     * Lumo-themed CSS pseudo-element tooltip. Reacts to both hover and keyboard focus (via {@code
     * :focus-within}). Immune to {@code vaadin-tooltip-mixin}'s overlay dismissal because it does
     * not participate in the overlay system.
     *
     * <p>When combined with a parent-popover, the tooltip and popover both appear on the same item
     * — the tooltip sits below the icon (default tooltip position), the popover opens to the right,
     * so they don't spatially overlap. Use {@link #POPOVER_HEADER} if you want a single overlay per
     * item.
     */
    SIMPLE,

    /**
     * Default. Tooltip is rendered as a {@link com.vaadin.flow.component.popover.Popover} with the
     * configured {@link PopoverHeaderMode} as its content. For items with children the existing
     * parent-popover doubles as the tooltip — there is exactly one overlay per item. For leaf items
     * a popover is created on demand whose only content is the header.
     *
     * <p>Reacts to hover and keyboard focus (via {@code Popover.setOpenOnFocus}). Inherits
     * hover/hide delays, position, and arrow visibility from the rail's existing popover settings.
     *
     * <p><b>Constraint:</b> requires a non-{@link PopoverHeaderMode#NONE} header — without one the
     * popover would have no content. If the rail is attached with {@code
     * RailTooltipMode.POPOVER_HEADER} and {@code PopoverHeaderMode.NONE} configured, the header
     * mode is silently coerced to {@link PopoverHeaderMode#LABEL_ONLY}. Setting the combination via
     * runtime setters after attach is not validated and may produce an empty popover.
     */
    POPOVER_HEADER
}
