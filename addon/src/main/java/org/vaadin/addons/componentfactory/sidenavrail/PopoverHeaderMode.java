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
 * Controls whether (and how) a {@link SideNavRailItem}'s popover renders a header that
 * identifies the item itself. Opt-in — the default is {@link #NONE}.
 *
 * <p>The header is rendered above the nested {@code SideNav} that shows children, when
 * the item has children. For leaf items shown via {@link RailTooltipMode#POPOVER}, the
 * header is the only content of the popover.
 *
 * <p>If the configured mode would produce an empty header (e.g. {@link #ICON_ONLY} on a
 * parent without a prefix component), the header is omitted entirely rather than rendered
 * blank. Note: rail-mode root items always carry a prefix component (the auto-generated
 * letter avatar), so {@link #ICON_ONLY} never produces an empty header in practice.
 *
 * <p>Changes made after the popover has been rendered take effect immediately — the rail
 * rewires all existing popovers when {@link SideNavRail#setPopoverHeaderMode} is called.
 * Changes to the parent's label or prefix component <em>after</em> the popover exists are
 * picked up only on the next call to {@code setPopoverHeaderMode}; update the item
 * before rendering or re-trigger the mode to refresh.
 */
public enum PopoverHeaderMode {

    /** No header. Default. */
    NONE,

    /** Header shows the parent's text label only. */
    LABEL_ONLY,

    /** Header shows a copy of the parent's prefix component (typically an icon) only. */
    ICON_ONLY,

    /** Header shows both the prefix component and the label, icon first. */
    FULL
}
