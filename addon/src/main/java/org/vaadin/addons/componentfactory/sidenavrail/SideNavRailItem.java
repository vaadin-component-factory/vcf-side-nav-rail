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
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.avatar.AvatarVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.popover.Popover;
import com.vaadin.flow.component.popover.PopoverPosition;
import com.vaadin.flow.component.popover.PopoverVariant;
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
 *       {@link SideNavRail}'s {@link PopoverOn} and rail state — see
 *       {@link SideNavRail#setPopoverOn(PopoverOn)} for the full behaviour matrix.
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
    private static final String ARIA_EXPANDED = "aria-expanded";
    private static final String MENU_ROLE = "menu";

    private Popover popover;
    private SideNavRail ownerRail;
    private boolean expandedListenerWired = false;
    private Boolean savedMatchNested = null;

    /**
     * Non-navigating container item. Click does nothing; useful as a parent for children.
     *
     * @param label the visible label of the item; may be {@code null} for an unlabelled
     *     item, in which case the rail-mode letter-avatar fallback is suppressed
     */
    public SideNavRailItem(String label) {
        super(label);
        wrapLabel();
    }

    /**
     * Item navigating to the given path (server- or client-side route).
     *
     * @param label the visible label of the item; may be {@code null}
     * @param path the route path the item navigates to on click; may be {@code null}
     *     for a non-navigating item
     */
    public SideNavRailItem(String label, String path) {
        super(label, path);
        wrapLabel();
    }

    /**
     * Item navigating to the given Flow route class.
     *
     * @param label the visible label of the item; may be {@code null}
     * @param view the Flow view class the item navigates to on click; must not be
     *     {@code null}
     */
    public SideNavRailItem(String label, Class<? extends Component> view) {
        super(label, view);
        wrapLabel();
    }

    /**
     * Item navigating to a path, with a prefix component rendered on the left
     * (typically an icon).
     *
     * @param label the visible label of the item; may be {@code null}
     * @param path the route path the item navigates to on click; may be {@code null}
     *     for a non-navigating item
     * @param prefixComponent the component rendered to the left of the label (typically
     *     a {@code VaadinIcon} or an {@code Avatar}); may be {@code null}, in which
     *     case the rail-mode letter-avatar fallback kicks in
     */
    public SideNavRailItem(String label, String path, Component prefixComponent) {
        super(label, path, prefixComponent);
        wrapLabel();
    }

    /**
     * Item navigating to a Flow route class, with a prefix component on the left.
     *
     * @param label the visible label of the item; may be {@code null}
     * @param view the Flow view class the item navigates to on click; must not be
     *     {@code null}
     * @param prefixComponent the component rendered to the left of the label (typically
     *     a {@code VaadinIcon} or an {@code Avatar}); may be {@code null}, in which
     *     case the rail-mode letter-avatar fallback kicks in
     */
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
     *
     * @param label the new label text; may be {@code null}
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
     * decides <em>whether</em> a tooltip is installed; we just refresh the text on the
     * {@code data-rail-tooltip} attribute if it is currently set so a relabelled item
     * doesn't carry a stale tooltip. No-op if no tooltip is installed.
     */
    private void refreshTooltipTextIfInstalled() {
        if (!getElement().hasAttribute(SideNavRail.RAIL_TOOLTIP_ATTRIBUTE)) {
            return;
        }
        String label = getLabel();
        if (label != null && !label.isBlank()) {
            getElement().setAttribute(SideNavRail.RAIL_TOOLTIP_ATTRIBUTE, label);
        } else {
            getElement().removeAttribute(SideNavRail.RAIL_TOOLTIP_ATTRIBUTE);
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>Overridden so that setting the prefix back to {@code null} re-generates the
     * letter-avatar fallback (otherwise a user-removed icon would leave the item
     * unmarked in rail mode). Passing a real component (icon, image, …) replaces the
     * avatar as usual — the override is a no-op in that case.
     *
     * @param prefix the new prefix component (typically a {@code VaadinIcon} or an
     *     {@code Avatar}); pass {@code null} to fall back to the auto-generated letter
     *     avatar
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
     * @param items the {@link SideNavRailItem} children to append below this item
     * @throws IllegalArgumentException if any item is not a {@link SideNavRailItem}
     */
    @Override
    public void addItem(SideNavItem... items) {
        for (SideNavItem item : items) {
            SideNavRail.requireRailItem(item);
        }
        super.addItem(items);
        handleChildrenMutation();
    }

    /**
     * {@inheritDoc}
     *
     * @param item the {@link SideNavRailItem} to prepend below this item
     * @throws IllegalArgumentException if the item is not a {@link SideNavRailItem};
     *     see {@link #addItem(SideNavItem...)}
     */
    @Override
    public void addItemAsFirst(SideNavItem item) {
        SideNavRail.requireRailItem(item);
        super.addItemAsFirst(item);
        handleChildrenMutation();
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
        SideNavRail.requireRailItem(item);
        super.addItemAtIndex(index, item);
        handleChildrenMutation();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Removed children are also reflected in the popover (if any).
     */
    @Override
    public void remove(SideNavItem... items) {
        super.remove(items);
        handleChildrenMutation();
    }

    /**
     * {@inheritDoc}
     *
     * <p>The popover (if any) is rebuilt with no children.
     */
    @Override
    public void removeAll() {
        super.removeAll();
        handleChildrenMutation();
    }

    /**
     * Reflects a runtime change of this item's children into the popover, if
     * the item is wired up to a rail. When a popover already exists, its
     * content is rebuilt so the new/removed child is reflected. When no
     * popover exists yet (item used to be a leaf), {@link #refreshPopoverFromOwner()}
     * materializes one if the owning rail's settings now warrant it. No-op
     * before attach — {@link #onAttach(AttachEvent)} runs the same path.
     */
    private void handleChildrenMutation() {
        if (popover != null) {
            rebuildPopoverContent();
        } else {
            refreshPopoverFromOwner();
        }
        // Ancestor popovers mirror the full subtree via copyOf(child), so a
        // change to a nested item's children isn't reflected by rebuilding only
        // this item's own popover — every ancestor with a popover needs the
        // same refresh. Walk up the parent chain.
        Component p = getParent().orElse(null);
        while (p != null) {
            if (p instanceof SideNavRailItem ancestor) {
                ancestor.rebuildPopoverContent();
            }
            p = p.getParent().orElse(null);
        }
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
        ownerRail = lookupOwnerRail();
        ensureLetterAvatar();
        // Materializes the popover (if warranted) AND re-applies owner-driven
        // settings + gating + open-on-focus. Single entry point so a fresh
        // attach and a detach/reattach cycle take the same code path.
        refreshPopoverFromOwner();
        wireExpandedListener();
    }

    void refreshPopoverFromOwner() {
        SideNavRail owner = findOwnerRail();
        if (owner == null) {
            // Pre-attach or standalone — owner-driven settings can't be applied,
            // and creating a popover here would skip the owner-aware initial
            // populate. The next onAttach will run ensurePopover()/this method
            // again with the owner in place.
            return;
        }
        // Owner-driven settings may now require a popover on a previously-bare leaf
        // (RailTooltipMode.POPOVER_HEADER ⇒ owner.isLeafPopoverActive()). ensurePopover()
        // is a no-op when one already exists.
        ensurePopover();
        if (popover == null) {
            return;
        }
        popover.setOpenOnFocus(owner.isRailMode());
        applyPopoverSettings();
        applyPopoverGating();
    }

    @Override
    protected void onDetach(DetachEvent event) {
        super.onDetach(event);
        ownerRail = null;
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
     * {@code <vaadin-side-nav-item>} web component and re-evaluates popover
     * gating on every inline-expand toggle:
     *
     * <ul>
     *   <li>expand ➜ inline children are now visible, so the popover would be
     *       redundant — {@code applyPopoverGating} flips {@code openOnHover}
     *       off and closes the popover if it was open.
     *   <li>collapse ➜ inline children are hidden again, so the popover is
     *       eligible — {@code applyPopoverGating} flips {@code openOnHover}
     *       back on. Vaadin's hover trigger reopens the popover on the next
     *       {@code mouseenter}; we don't try to anticipate that.
     * </ul>
     *
     * <p>Wired unconditionally on attach. The body's {@code popover == null}
     * guard handles leaves and the pre-attach window — wiring once on attach
     * is robust against runtime-add transitions (leaf gains children later).
     */
    private void wireExpandedListener() {
        if (expandedListenerWired) {
            return;
        }
        expandedListenerWired = true;

        getElement().addEventListener("expanded-changed", e -> {
            if (findOwnerRail() == null || popover == null) {
                return;
            }
            applyPopoverGating();
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
        if (getItems().isEmpty() && !ownerWantsLeafPopover()) {
            return;
        }
        popover = new Popover();
        popover.setTarget(this);
        popover.setOpenOnClick(false);
        SideNavRail owner = findOwnerRail();
        popover.setOpenOnFocus(owner != null && owner.isRailMode());
        popover.setOverlayRole(MENU_ROLE);

        // Seed from the owning rail's current settings so a popover created mid-session
        // picks up timings/position configured earlier. Fall back to Lumo-typical
        // defaults for popovers living outside a rail (rare but supported).
        if (owner != null) {
            applyPopoverSettings();
        } else {
            popover.setHoverDelay(200);
            popover.setHideDelay(300);
            popover.setPosition(PopoverPosition.END_TOP);
            popover.addThemeVariants(PopoverVariant.ARROW);
        }

        populatePopover();

        if (owner != null) {
            applyPopoverGating();
        } else {
            popover.setOpenOnHover(true);  // standalone item — default on
        }
    }

    private boolean ownerWantsLeafPopover() {
        SideNavRail owner = findOwnerRail();
        return owner != null && owner.isLeafPopoverActive();
    }

    /**
     * Pushes updated hover/hide delays, position, and arrow visibility to the existing
     * popover, if any. Called by {@link SideNavRail} when one of those settings changes
     * so a live rail reflects the new values without needing a reattach. No-op when
     * the popover has not been created yet or when the item has no owning rail.
     */
    void applyPopoverSettings() {
        if (popover == null) {
            return;
        }
        SideNavRail owner = findOwnerRail();
        if (owner == null) {
            return;
        }
        popover.setHoverDelay(owner.getPopoverHoverDelay());
        popover.setHideDelay(owner.getPopoverHideDelay());
        // Open immediately on focus, with no delay. The focus trigger is only
        // active in rail mode (see setOpenOnFocus). A mouse click focuses the
        // item on mousedown, which makes vaadin-popover schedule a *delayed*
        // focus-open (focusDelay, default 500 ms). If the cursor then leaves the
        // item before that timer fires, the popover's opened-state controller
        // takes the "still opening" branch of close() and only aborts the pending
        // open instead of scheduling a hide — so a popover that was already shown
        // on hover gets stuck open. With focusDelay=0 the focus-open fires
        // synchronously and no such timer is ever pending, so a subsequent
        // hover-leave closes the popover normally. Immediate open-on-focus is
        // also the better UX for keyboard-driven discovery of the popover.
        popover.setFocusDelay(0);
        popover.setPosition(owner.getPopoverPosition());
        if (owner.isPopoverArrowVisible()) {
            popover.addThemeVariants(PopoverVariant.ARROW);
        } else {
            popover.removeThemeVariants(PopoverVariant.ARROW);
        }
    }

    private SideNavRail findOwnerRail() {
        return ownerRail;
    }

    private SideNavRail lookupOwnerRail() {
        Component p = getParent().orElse(null);
        while (p != null) {
            if (p instanceof SideNavRail rail) {
                return rail;
            }
            p = p.getParent().orElse(null);
        }
        return null;
    }

    private boolean isRootItem() {
        return getParent().map(p -> p instanceof SideNavRail).orElse(false);
    }

    /**
     * Applies the open-eligibility of this item's popover based on the owning
     * {@link SideNavRail}'s current {@link PopoverOn}, rail state, and
     * children-only-in-popover flag. Package-private — external callers should
     * use {@link SideNavRail#setPopoverOn(PopoverOn)} or
     * {@link SideNavRail#setRailMode(boolean)} instead.
     */
    void applyPopoverGating() {
        if (popover == null) {
            return;
        }
        SideNavRail owner = findOwnerRail();
        if (owner == null) {
            return;
        }
        if (getItems().isEmpty()) {
            // Leaf popover — gated entirely by the rail's leaf-popover-active
            // predicate (RailTooltipMode.POPOVER_HEADER + rail mode active).
            // PopoverOn does not apply to leaves: it only governs items with children.
            boolean leafActive = owner.isLeafPopoverActive();
            popover.setOpenOnHover(leafActive);
            if (!leafActive && popover.isOpened()) {
                popover.close();
            }
            return;
        }
        boolean railMode = owner.isRailMode();
        // When inline children are CSS-hidden by childrenOnlyInPopover, the
        // popover is the only way to access the children. The item's expanded
        // state may still flip (Vaadin auto-expands on route match), but it
        // has no visual effect, so the popover stays eligible regardless.
        if (owner.isChildrenOnlyInPopover()) {
            popover.setOpenOnHover(true);
            return;
        }
        // An inline-expanded item already shows its children in the outer nav, so a
        // popover would be redundant. In rail mode the inline-expand is visually
        // suppressed anyway, so the popover is still wanted.
        boolean eligible =
                switch (owner.getPopoverOn()) {
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
     * Applies §4.4.5 ARIA attributes. {@code aria-expanded} is seeded to
     * {@code "false"} on rail-mode entry for items with children and then driven
     * by Vaadin's {@code <vaadin-popover>}, which sets the attribute on the
     * target via its {@code __updateAriaAttributes} observer whenever the popover
     * opens or closes — no server-side bookkeeping needed.
     * <p>
     * {@code aria-haspopup} is left to the popover too: it sets {@code "true"} on
     * the target, which together with {@code role="menu"} on the overlay (see
     * {@code setOverlayRole} in {@link #ensurePopover()}) is a valid pairing, so
     * the addon no longer forces a {@code "menu"} value of its own.
     * Package-private — called by {@link SideNavRail#setRailMode(boolean)}.
     */
    void applyAriaAttributes(boolean railMode) {
        boolean hasChildren = !getItems().isEmpty();
        if (railMode && hasChildren) {
            if (!getElement().hasAttribute(ARIA_EXPANDED)) {
                getElement().setAttribute(ARIA_EXPANDED, "false");
            }
        } else {
            getElement().removeAttribute(ARIA_EXPANDED);
        }
    }

    /**
     * Applies (or rolls back) the {@link SideNavRail}-managed {@code matchNested}
     * override on this item. Called by {@link SideNavRail} when rail-mode toggles or
     * {@link SideNavRail#setRootMatchNested(RootMatchNested)} flips the feature.
     * Snapshots the user's own {@code matchNested} value on the first activation so
     * deactivation restores it exactly — repeated activations do not overwrite the
     * snapshot, so a mid-rail-mode {@code setMatchNested(...)} call by the user is
     * still rolled back to whatever they had set before the rail mode entered.
     * Package-private — addon-internal.
     *
     * @param active {@code true} to force {@code matchNested = true} (snapshotting
     *     the current value if not yet snapshotted); {@code false} to restore the
     *     snapshotted value, if any
     */
    void applyRailMatchNestedOverride(boolean active) {
        if (active) {
            if (savedMatchNested == null) {
                savedMatchNested = isMatchNested();
            }
            setMatchNested(true);
        } else if (savedMatchNested != null) {
            setMatchNested(savedMatchNested);
            savedMatchNested = null;
        }
    }

    private void populatePopover() {
        popover.removeAll();
        renderHeaderIfConfigured();

        if (getItems().isEmpty()) {
            // Leaf popover (RailTooltipMode.POPOVER_HEADER): header-only content.
            return;
        }

        SideNav nested = new SideNav();
        for (SideNavItem child : getItems()) {
            nested.addItem(copyOf(child));
        }
        popover.add(nested);
    }

    /**
     * Rebuilds the popover's content using the owning rail's current
     * {@link PopoverHeaderMode}. Invoked by {@link SideNavRail} when the mode changes
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
        PopoverHeaderMode mode =
                (owner != null) ? owner.getPopoverHeaderMode() : PopoverHeaderMode.NONE;
        if (mode == PopoverHeaderMode.NONE) {
            return;
        }
        if (owner != null && owner.isPopoverHeaderOnlyInRailMode() && !owner.isRailMode()) {
            return;
        }

        boolean wantsIcon = mode == PopoverHeaderMode.ICON_ONLY
                || mode == PopoverHeaderMode.FULL;
        boolean wantsLabel = mode == PopoverHeaderMode.LABEL_ONLY
                || mode == PopoverHeaderMode.FULL;

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

        Div header = new Div();
        header.addClassName("side-nav-rail-popover-header");
        if (hasIcon) {
            header.add(copyComponent(prefix));
        }
        if (hasLabel) {
            Span text = new Span(label);
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
        copy.getElement().setAttribute("role", "menuitem");

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
        if (source instanceof Icon icon) {
            Icon copy = new Icon();
            copy.getElement().setAttribute("icon", icon.getElement().getAttribute("icon"));
            return copy;
        }
        return source;
    }

    /**
     * Returns the popover of this item, if one has been created. The popover is
     * lazily attached on first attach: items with children always get one, while
     * leaf items get one only when the owning rail's {@link RailTooltipMode} is
     * {@code POPOVER_HEADER} and rail-mode is active (see
     * {@link SideNavRail#isLeafPopoverActive()}). Items that have not been
     * attached yet, or that are not part of a rail, return {@link Optional#empty()}.
     *
     * @return an {@link Optional} containing the underlying {@link Popover}, or
     *     {@link Optional#empty()} if none has been created
     */
    public Optional<Popover> getPopover() {
        return Optional.ofNullable(popover);
    }
}
