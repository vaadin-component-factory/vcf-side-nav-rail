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
     * Every root item gets a tooltip. Default. Keeps the label discoverable
     * consistently, even for items whose popover doesn't include a header — see
     * {@link PopoverParentLabelMode}. A tooltip on an item that also opens a popover
     * appears below the icon (default tooltip position), while the popover opens to
     * the right, so they don't spatially overlap.
     *
     * <p><b>Quirk to be aware of:</b> if a tooltip is <em>already open</em> on one root
     * item and the pointer moves to another root item that has a popover, the tooltip
     * briefly shows the new item's label and then disappears as the popover opens —
     * the popover's overlay dismisses the already-visible tooltip. If the pointer
     * lands on a root item <em>directly</em> (no prior tooltip), tooltip and popover
     * coexist cleanly. This is Vaadin's overlay-interaction behaviour; we cannot
     * influence it from the server without patching Vaadin internals. Use
     * {@link #ONLY_WITHOUT_CHILDREN} if you want to avoid the transient flicker.
     */
    ALL
}
