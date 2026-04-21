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
import com.vaadin.flow.component.popover.Popover;
import com.vaadin.flow.component.popover.PopoverPosition;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.dom.Element;
import java.util.List;
import java.util.Optional;

/**
 * {@link SideNavItem} variant that renders its string label inside a
 * {@code <span class="label">}. The wrap is what lets CSS hide the label in rail mode
 * ({@code vaadin-side-nav[theme~="rail"] vaadin-side-nav-item .label}); a bare text node
 * cannot be targeted by CSS.
 */
public class SideNavRailItem extends SideNavItem {

    private static final String LABEL_SPAN_CLASS = "label";

    private Popover popover;
    private boolean expandedListenerWired = false;
    private boolean lastKnownExpanded = false;

    public SideNavRailItem(String label) {
        super(label);
        wrapLabel();
    }

    public SideNavRailItem(String label, String path) {
        super(label, path);
        wrapLabel();
    }

    public SideNavRailItem(String label, Class<? extends Component> view) {
        super(label, view);
        wrapLabel();
    }

    public SideNavRailItem(String label, String path, Component prefixComponent) {
        super(label, path, prefixComponent);
        wrapLabel();
    }

    public SideNavRailItem(
            String label, Class<? extends Component> view, Component prefixComponent) {
        super(label, view, prefixComponent);
        wrapLabel();
    }

    @Override
    public void setLabel(String label) {
        super.setLabel(label);
        wrapLabel();
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
        ensurePopover();
        wireExpandedListener();
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
        popover.setOpenOnFocus(false);
        popover.setHoverDelay(200);
        popover.setHideDelay(300);
        popover.setOverlayRole("menu");
        popover.setPosition(resolveEndTopPosition());

        // Attach the popover — the target element lives inside the rail, so we place
        // the popover as a child of the rail (or the UI as fallback).
        Element attachPoint = findSideNavRailElement()
                .orElseGet(() -> getUI().orElseThrow(IllegalStateException::new).getElement());
        attachPoint.appendChild(popover.getElement());

        populatePopover();

        SideNavRail owner = findOwnerRail();
        if (owner != null) {
            applyPopoverGating(owner.getPopoverMode(), owner.isRailMode());
        } else {
            popover.setOpenOnHover(true);  // standalone item — default on
        }
    }

    private Optional<Element> findSideNavRailElement() {
        SideNavRail owner = findOwnerRail();
        return owner != null ? Optional.of(owner.getElement()) : Optional.empty();
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
                    case COLLAPSED_ITEM -> railMode || !isExpanded();
                    case RAIL_ONLY -> railMode;
                };
        popover.setOpenOnHover(eligible);
        if (!eligible && popover.isOpened()) {
            popover.close();
        }
    }

    private void populatePopover() {
        SideNav nested = new SideNav();
        for (SideNavItem child : getItems()) {
            nested.addItem(copyOf(child));
        }
        popover.removeAll();
        popover.add(nested);
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
