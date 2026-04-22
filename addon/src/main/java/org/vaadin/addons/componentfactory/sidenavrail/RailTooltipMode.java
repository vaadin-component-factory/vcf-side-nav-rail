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
 * Controls whether root items of a {@link SideNavRail} display a native tooltip
 * <em>while the rail is in rail mode</em>. Tooltips are never shown in normal mode —
 * the label text identifies the item in that case.
 *
 * <p>The tooltip text is the item's own {@code getLabel()}. It is applied to direct
 * children of the rail only; nested items never get a tooltip from the rail.
 */
public enum RailTooltipMode {

    /** No tooltips on root items. */
    NONE,

    /**
     * Only root items that have no children get a tooltip. Items with children still
     * surface their label via the hover popover, so a tooltip would be redundant. Use
     * this if you want to avoid a tooltip appearing alongside an open popover.
     */
    ONLY_WITHOUT_CHILDREN,

    /**
     * Every root item gets a tooltip. Default. Rationale: in a rail whose root items
     * are typically clickable links, the user often clicks directly on an icon rather
     * than pausing on it to explore the popover — the tooltip is the primary
     * identification cue and should be available on every icon regardless of whether
     * it has children. A tooltip on an item that also opens a popover sits below the
     * icon (default tooltip position), while the popover opens to the right, so they
     * don't spatially overlap.
     *
     * <p><b>Vaadin by-design behaviour:</b> if a tooltip is <em>already open</em> on
     * one root item and the pointer slides onto another root item that also opens a
     * popover, the tooltip switches to the new item's label and is then dismissed as
     * the popover opens. This is driven by {@code vaadin-tooltip-mixin} listening for
     * {@code vaadin-overlay-open} events on {@code document.body} and auto-closing
     * itself when a peer overlay appears (see
     * <a href="https://github.com/vaadin/web-components/issues/9768">web-components#9768</a>
     * for the upstream acknowledgement). The direct-hover case (no prior tooltip)
     * slips past this check because the tooltip has not yet reached the opened state
     * when the popover fires the event. Use {@link #ONLY_WITHOUT_CHILDREN} if you
     * don't want tooltips on items that also own a popover.
     */
    ALL
}
