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
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.avatar.AvatarVariant;
import com.vaadin.flow.component.popover.Popover;
import com.vaadin.flow.component.popover.PopoverPosition;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.dom.Element;
import java.util.List;
import java.util.Optional;

/**
 * {@link SideNavItem} variant that integrates with {@link SideNavRail}'s rail mode and
 * hover popover. API-compatible with {@code SideNavItem} — drop in anywhere you would
 * use {@code SideNavItem} under a {@link SideNavRail}.
 *
 * <p>Two things happen beyond the standard item:
 *
 * <ul>
 *   <li>The string label is wrapped in a {@code <span class="label">} so rail-mode CSS
 *       (which selects {@code vaadin-side-nav[theme~="rail"] vaadin-side-nav-item .label})
 *       can hide it. A bare text node cannot be targeted by CSS.
 *   <li>Items that have children lazily attach a {@link Popover} on first attach. The
 *       popover's hover-trigger eligibility is gated by the owning
 *       {@link SideNavRail}'s {@link PopoverMode} and rail state — see
 *       {@link SideNavRail#setPopoverMode(PopoverMode)} for the full behaviour matrix.
 * </ul>
 *
 * <p>Typical usage:
 *
 * <pre>{@code
 * SideNavRail rail = new SideNavRail();
 * SideNavRailItem code = new SideNavRailItem("Code", "/code", VaadinIcon.CODE.create());
 * code.addItem(new SideNavRailItem("Branches", "/code/branches"));
 * rail.addItem(code);
 * }</pre>
 */
public class SideNavRailItem extends SideNavItem {

    private static final String LABEL_SPAN_CLASS = "label";

    private Popover popover;
    private boolean expandedListenerWired = false;
    private boolean lastKnownExpanded = false;

    /** Test-only accessor: do not call from production code. */
    public Popover getPopoverForTesting() {
        return popover;
    }

    /** Non-navigating container item. Click does nothing; useful as a parent for children. */
    public SideNavRailItem(String label) {
        super(label);
        wrapLabel();
    }

    /** Item navigating to the given path (server- or client-side route). */
    public SideNavRailItem(String label, String path) {
        super(label, path);
        wrapLabel();
    }

    /** Item navigating to the given Flow route class. */
    public SideNavRailItem(String label, Class<? extends Component> view) {
        super(label, view);
        wrapLabel();
    }

    /** Item navigating to a path, with a prefix component rendered on the left (typically an icon). */
    public SideNavRailItem(String label, String path, Component prefixComponent) {
        super(label, path, prefixComponent);
        wrapLabel();
    }

    /** Item navigating to a Flow route class, with a prefix component on the left. */
    public SideNavRailItem(
            String label, Class<? extends Component> view, Component prefixComponent) {
        super(label, view, prefixComponent);
        wrapLabel();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Overridden to re-wrap the label in {@code <span class="label">} so CSS can hide
     * it in rail mode, and to refresh the auto-generated letter-avatar fallback (see
     * {@link #ensureLetterAvatar()}) so it matches the new label's first letter.
     * Idempotent across repeated calls.
     */
    @Override
    public void setLabel(String label) {
        super.setLabel(label);
        wrapLabel();
        ensureLetterAvatar();
        refreshTooltipTextIfInstalled();
    }

    /**
     * Keeps the installed tooltip attribute in sync with the label. The owning rail
     * decides <em>whether</em> a tooltip is installed and <em>which</em> attribute
     * (custom pseudo-element vs. native {@code title}) is used; we don't change that
     * here, we just refresh the text on whichever attribute is currently set so a
     * relabelled item doesn't carry a stale tooltip. No-op if no tooltip is installed.
     */
    private void refreshTooltipTextIfInstalled() {
        String attr;
        if (getElement().hasAttribute(SideNavRail.RAIL_TOOLTIP_ATTRIBUTE)) {
            attr = SideNavRail.RAIL_TOOLTIP_ATTRIBUTE;
        } else if (getElement().hasAttribute(SideNavRail.NATIVE_TOOLTIP_ATTRIBUTE)) {
            attr = SideNavRail.NATIVE_TOOLTIP_ATTRIBUTE;
        } else {
            return;
        }
        String label = getLabel();
        if (label != null && !label.isBlank()) {
            getElement().setAttribute(attr, label);
        } else {
            getElement().removeAttribute(attr);
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>Overridden so that setting the prefix back to {@code null} re-generates the
     * letter-avatar fallback (otherwise a user-removed icon would leave the item
     * unmarked in rail mode). Passing a real component (icon, image, …) replaces the
     * avatar as usual — the override is a no-op in that case.
     */
    @Override
    public void setPrefixComponent(Component prefix) {
        super.setPrefixComponent(prefix);
        if (prefix == null) {
            ensureLetterAvatar();
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>Only accepts {@link SideNavRailItem} children. Passing a plain
     * {@link SideNavItem} throws {@link IllegalArgumentException} — nested items need
     * the same label-wrap + popover-gating wiring the parent has.
     *
     * @throws IllegalArgumentException if any item is not a {@link SideNavRailItem}
     */
    @Override
    public void addItem(SideNavItem... items) {
        for (SideNavItem item : items) {
            SideNavRail.requireRailItem(item);
        }
        super.addItem(items);
    }

    /**
     * {@inheritDoc}
     *
     * @throws IllegalArgumentException if the item is not a {@link SideNavRailItem};
     *     see {@link #addItem(SideNavItem...)}
     */
    @Override
    public void addItemAsFirst(SideNavItem item) {
        SideNavRail.requireRailItem(item);
        super.addItemAsFirst(item);
    }

    private void wrapLabel() {
        Element root = getElement();
        String label = super.getLabel();
        if (label == null) {
            return;
        }

        Optional<Element> existing = findLabelSpan(root);
        if (existing.isPresent()) {
            existing.get().setText(label);
            removeBareLabelTextNode(root);
            return;
        }

        Element span = new Element("span");
        span.setAttribute("class", LABEL_SPAN_CLASS);
        span.setText(label);
        removeBareLabelTextNode(root);
        root.appendChild(span);
    }

    private static Optional<Element> findLabelSpan(Element root) {
        return root.getChildren()
                .filter(e -> !e.isTextNode())
                .filter(e -> "span".equals(e.getTag()))
                .filter(e -> LABEL_SPAN_CLASS.equals(e.getAttribute("class")))
                .findFirst();
    }

    /**
     * Removes the bare text node that {@code super.setLabel(...)} appends directly on
     * the root element as a child text {@link Element}. Non-text element children
     * (e.g. prefix icon with {@code slot="prefix"}) are preserved by snapshotting and
     * re-attaching them around the {@code setText("")} call.
     */
    private static void removeBareLabelTextNode(Element root) {
        // Check whether there are any child text nodes to remove.
        boolean hasTextNodes = root.getChildren().anyMatch(Element::isTextNode);
        if (!hasTextNodes) {
            return;
        }

        // Snapshot non-text element children before setText("") detaches them.
        List<Element> elementChildren = root.getChildren()
                .filter(e -> !e.isTextNode())
                .toList();

        // setText("") removes text-node children and may also detach element children.
        root.setText("");

        // Re-attach any element children that were detached.
        for (Element child : elementChildren) {
            if (child.getParent() == null) {
                root.appendChild(child);
            }
        }
    }

    @Override
    protected void onAttach(AttachEvent event) {
        super.onAttach(event);
        ensureLetterAvatar();
        ensurePopover();
        wireExpandedListener();
    }

    private static final String AVATAR_CLASS = "side-nav-rail-letter-avatar";

    /**
     * Fills an empty prefix slot with a single-letter {@link Avatar} derived from the
     * label so rail mode is not reduced to a blank tile when the consumer forgot to
     * provide an icon. Uses {@link AvatarVariant#LUMO_SMALL} (24×24, matching the
     * standard Lumo icon size used elsewhere in the side nav) with a Lumo-typical
     * background and secondary text color out of the box.
     *
     * <p>Only runs when the prefix slot is empty or still holds the avatar we set
     * earlier — a user-provided prefix is always left alone. Blank labels produce no
     * avatar, and any stale avatar that ends up with a blank label is removed.
     */
    private void ensureLetterAvatar() {
        Component existing = getPrefixComponent();
        if (existing != null && !isOwnAvatar(existing)) {
            return;  // User-provided prefix — hands off.
        }

        String label = getLabel();
        boolean hasLabel = label != null && !label.isBlank();
        if (!hasLabel) {
            if (existing != null) {
                super.setPrefixComponent(null);
            }
            return;
        }

        String letter = String.valueOf(Character.toUpperCase(label.charAt(0)));
        if (existing instanceof Avatar avatar && isOwnAvatar(existing)) {
            avatar.setAbbreviation(letter);
            return;
        }
        Avatar avatar = new Avatar();
        avatar.addThemeVariants(AvatarVariant.LUMO_SMALL);
        avatar.setAbbreviation(letter);
        avatar.addClassName(AVATAR_CLASS);
        super.setPrefixComponent(avatar);
    }

    /**
     * Identity check for the auto-generated avatar, done via the CSS marker class.
     * Framework-agnostic (works whether the wrapped component is an {@link Avatar},
     * a {@code Span}, or anything else the consumer might have subclassed) — the
     * marker class is what the addon tracks.
     */
    private static boolean isOwnAvatar(Component c) {
        return c != null && c.getElement().getClassList().contains(AVATAR_CLASS);
    }

    /**
     * Listens for the {@code expanded-changed} event fired by the underlying
     * {@code <vaadin-side-nav-item>} web component. Re-evaluates popover gating on
     * every inline-expand toggle. Effects:
     * <ul>
     *   <li>expand ➜ the popover is now redundant — {@code applyPopoverGating} closes it;
     *   <li>collapse ➜ the children are hidden again; since the user's cursor is still
     *       on the item (they just clicked the toggle), open the popover explicitly.
     *       Vaadin's hover trigger would otherwise wait for the next {@code mouseenter}.
     * </ul>
     * The explicit open only fires on a {@code true → false} transition. The event
     * also fires once at initial attach with the same value the item already had;
     * without the transition guard that initial fire would pop every item's popover
     * open on page load.
     */
    private void wireExpandedListener() {
        if (expandedListenerWired) {
            return;
        }
        if (getItems().isEmpty()) {
            return;
        }
        expandedListenerWired = true;
        lastKnownExpanded = isExpanded();

        getElement().addEventListener("expanded-changed", e -> {
            SideNavRail owner = findOwnerRail();
            if (owner == null || popover == null) {
                return;
            }
            boolean wasExpanded = lastKnownExpanded;
            boolean nowExpanded = isExpanded();
            lastKnownExpanded = nowExpanded;

            applyPopoverGating(owner.getPopoverMode(), owner.isRailMode());

            if (wasExpanded && !nowExpanded && popover.isOpenOnHover()) {
                popover.open();
            }
        });
    }

    /**
     * Creates the item's popover on first attach. The popover is deliberately not
     * appended anywhere server-side: {@link Popover#setTarget(Component)} installs
     * attach/detach listeners on the target that auto-add the popover to the UI when
     * the target enters the tree and remove it again when the target leaves. Adding it
     * manually would double-parent the element, bypass the auto-remove on detach, and
     * leak a stale popover if the rail is later removed.
     */
    private void ensurePopover() {
        if (popover != null) {
            return;
        }
        if (getItems().isEmpty()) {
            return;
        }
        popover = new Popover();
        popover.setTarget(this);
        popover.setOpenOnClick(false);
        SideNavRail owner = findOwnerRail();
        popover.setOpenOnFocus(owner != null && owner.isRailMode());
        popover.setOverlayRole("menu");

        // Seed from the owning rail's current settings so a popover created mid-session
        // picks up timings/position configured earlier. Fall back to Lumo-typical
        // defaults for popovers living outside a rail (rare but supported).
        popover.setHoverDelay(owner != null ? owner.getPopoverHoverDelay() : 200);
        popover.setHideDelay(owner != null ? owner.getPopoverHideDelay() : 300);
        popover.setPosition(owner != null ? owner.getPopoverPosition() : resolveEndTopPosition());

        populatePopover();

        popover.addOpenedChangeListener(e -> syncAriaExpanded(e.isOpened()));

        if (owner != null) {
            applyPopoverGating(owner.getPopoverMode(), owner.isRailMode());
        } else {
            popover.setOpenOnHover(true);  // standalone item — default on
        }
    }

    /**
     * Updates the popover's focus-trigger according to rail state. Called by
     * {@link SideNavRail#setRailMode(boolean)} so the flag tracks live mode changes.
     * Public because it is invoked from the {@link SideNavRail} in the same package —
     * consider it addon-internal; user code should not depend on it.
     */
    public void applyFocusTrigger(boolean railMode) {
        if (popover != null) {
            popover.setOpenOnFocus(railMode);
        }
    }

    /**
     * Pushes updated hover/hide delays and position to the existing popover, if any.
     * Called by {@link SideNavRail} when one of those settings changes so a live rail
     * reflects the new values without needing a reattach. No-op when the popover has
     * not been created yet.
     */
    void applyPopoverSettings(int hoverDelay, int hideDelay, PopoverPosition position) {
        if (popover == null) {
            return;
        }
        popover.setHoverDelay(hoverDelay);
        popover.setHideDelay(hideDelay);
        popover.setPosition(position);
    }

    private SideNavRail findOwnerRail() {
        com.vaadin.flow.component.Component p = getParent().orElse(null);
        while (p != null) {
            if (p instanceof SideNavRail rail) {
                return rail;
            }
            p = p.getParent().orElse(null);
        }
        return null;
    }

    /**
     * Whether this item is a direct child of the owning {@link SideNavRail} rather than
     * nested inside another item. Used to gate {@link PopoverMode#ONLY_ROOT_COLLAPSED_ITEMS}.
     */
    private boolean isRootItem() {
        return getParent().map(p -> p instanceof SideNavRail).orElse(false);
    }

    /**
     * Applies the open-eligibility of this item's popover based on the owning
     * {@link SideNavRail}'s current {@link PopoverMode} and rail state. Package-private —
     * external callers should use {@link SideNavRail#setPopoverMode(PopoverMode)} or
     * {@link SideNavRail#setRailMode(boolean)} instead.
     */
    void applyPopoverGating(PopoverMode mode, boolean railMode) {
        if (popover == null) {
            return;
        }
        // An inline-expanded item already shows its children in the outer nav, so a
        // popover would be redundant. In rail mode the inline-expand is visually
        // suppressed anyway, so the popover is still wanted.
        boolean eligible =
                switch (mode) {
                    case ALL_COLLAPSED_ITEMS -> railMode || !isExpanded();
                    case ONLY_ROOT_COLLAPSED_ITEMS ->
                            isRootItem() && (railMode || !isExpanded());
                    case ONLY_RAIL_MODE -> railMode;
                };
        popover.setOpenOnHover(eligible);
        if (!eligible && popover.isOpened()) {
            popover.close();
        }
    }

    /**
     * Applies §4.4.5 ARIA attributes: {@code aria-haspopup="menu"} on items with children
     * while rail mode is active; cleared otherwise. {@code aria-expanded} is seeded to
     * "false" and then tracked via {@link #syncAriaExpanded(boolean)} as the popover
     * opens/closes. Package-private — called by {@link SideNavRail#setRailMode(boolean)}.
     */
    void applyAriaAttributes(boolean railMode) {
        boolean hasChildren = !getItems().isEmpty();
        if (railMode && hasChildren) {
            getElement().setAttribute("aria-haspopup", "menu");
            if (!getElement().hasAttribute("aria-expanded")) {
                getElement().setAttribute("aria-expanded", "false");
            }
        } else {
            getElement().removeAttribute("aria-haspopup");
            getElement().removeAttribute("aria-expanded");
        }
    }

    /**
     * Updates {@code aria-expanded} to reflect the given popover state. Called from
     * the popover's {@code opened-changed} listener (see {@link #ensurePopover()}).
     * Public so tests can drive it directly without the real DOM event; not intended
     * for production callers.
     */
    public void syncAriaExpanded(boolean open) {
        if (getElement().hasAttribute("aria-haspopup")) {
            getElement().setAttribute("aria-expanded", String.valueOf(open));
        }
    }

    private void populatePopover() {
        popover.removeAll();
        renderHeaderIfConfigured();

        SideNav nested = new SideNav();
        for (SideNavItem child : getItems()) {
            SideNavItem copy = copyOf(child);
            tagAsMenuItem(copy);
            nested.addItem(copy);
        }
        popover.add(nested);
    }

    private static void tagAsMenuItem(SideNavItem item) {
        item.getElement().setAttribute("role", "menuitem");
        for (SideNavItem sub : item.getItems()) {
            tagAsMenuItem(sub);
        }
    }

    /**
     * Rebuilds the popover's content using the owning rail's current
     * {@link PopoverParentLabelMode}. Invoked by {@link SideNavRail} when the mode changes
     * so the header toggles without requiring a full reattach. Safe to call before the
     * popover is attached — no-op.
     */
    void rebuildPopoverContent() {
        if (popover == null) {
            return;
        }
        populatePopover();
    }

    private void renderHeaderIfConfigured() {
        SideNavRail owner = findOwnerRail();
        PopoverParentLabelMode mode =
                (owner != null) ? owner.getPopoverParentLabelMode() : PopoverParentLabelMode.NONE;
        if (mode == PopoverParentLabelMode.NONE) {
            return;
        }

        boolean wantsIcon = mode == PopoverParentLabelMode.ICON_ONLY
                || mode == PopoverParentLabelMode.FULL;
        boolean wantsLabel = mode == PopoverParentLabelMode.LABEL_ONLY
                || mode == PopoverParentLabelMode.FULL;

        Component prefix = getPrefixComponent();
        String label = getLabel();
        // The letter-avatar fallback is a rail-mode visual crutch, not a real icon —
        // treat it as "no icon" for popover-header purposes (the header has the label
        // to identify the parent; repeating the letter avatar would be redundant).
        boolean hasIcon = wantsIcon && prefix != null && !isOwnAvatar(prefix);
        boolean hasLabel = wantsLabel && label != null && !label.isBlank();
        if (!hasIcon && !hasLabel) {
            return;  // Would produce an empty header — skip.
        }

        com.vaadin.flow.component.html.Div header = new com.vaadin.flow.component.html.Div();
        header.addClassName("side-nav-rail-popover-header");
        if (hasIcon) {
            header.add(copyComponent(prefix));
        }
        if (hasLabel) {
            com.vaadin.flow.component.html.Span text =
                    new com.vaadin.flow.component.html.Span(label);
            text.addClassName("side-nav-rail-popover-header-label");
            header.add(text);
        }
        popover.add(header);
    }

    private static SideNavItem copyOf(SideNavItem source) {
        String label = source.getLabel();
        String path = source.getPath();
        Component prefix = source.getPrefixComponent();

        SideNavItem copy;
        if (path != null && prefix != null) {
            copy = new SideNavItem(label, path, copyComponent(prefix));
        } else if (path != null) {
            copy = new SideNavItem(label, path);
        } else if (prefix != null) {
            copy = new SideNavItem(label);
            copy.setPrefixComponent(copyComponent(prefix));
        } else {
            copy = new SideNavItem(label);
        }

        for (SideNavItem grandchild : source.getItems()) {
            copy.addItem(copyOf(grandchild));
        }
        return copy;
    }

    /**
     * Clones a prefix component by reinstantiating via its class. Vaadin icons are the
     * overwhelmingly common case; for anything else (custom components), fall back to
     * sharing the original reference — rare in an icon-driven rail.
     */
    private static Component copyComponent(Component source) {
        if (source instanceof com.vaadin.flow.component.icon.Icon icon) {
            com.vaadin.flow.component.icon.Icon copy = new com.vaadin.flow.component.icon.Icon();
            copy.getElement().setAttribute("icon", icon.getElement().getAttribute("icon"));
            return copy;
        }
        return source;
    }

    /**
     * Resolves the right-aligned, top-anchored popover position. The spec flags the exact
     * enum value as implementation-verified; try {@code END_TOP} first, then fall back to
     * {@code END}.
     */
    private static PopoverPosition resolveEndTopPosition() {
        try {
            return PopoverPosition.valueOf("END_TOP");
        } catch (IllegalArgumentException notFound) {
            return PopoverPosition.valueOf("END");
        }
    }
}
