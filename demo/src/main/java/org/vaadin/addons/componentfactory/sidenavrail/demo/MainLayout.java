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
package org.vaadin.addons.componentfactory.sidenavrail.demo;

import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.router.Layout;
import com.vaadin.flow.router.RouterLayout;
import org.vaadin.addons.componentfactory.sidenavrail.PopoverOn;
import org.vaadin.addons.componentfactory.sidenavrail.PopoverParentLabelMode;
import org.vaadin.addons.componentfactory.sidenavrail.RailTooltipMode;
import org.vaadin.addons.componentfactory.sidenavrail.RootMatchNested;
import org.vaadin.addons.componentfactory.sidenavrail.SideNavRail;
import org.vaadin.addons.componentfactory.sidenavrail.SideNavRailItem;

/**
 * Plain layout frame instead of {@code AppLayout} — the rail mode is the point
 * of the demo and an AppLayout drawer + navbar adds chrome that obscures the
 * effect. A thin top navbar hosts a {@link Select} for live-switching the
 * rail's {@link PopoverOn}; the sidebar with the rail sits below it.
 */
@Layout
public class MainLayout extends VerticalLayout implements RouterLayout {

    private final Div contentArea = new Div();

    public MainLayout() {
        setSizeFull();
        setPadding(false);
        setSpacing(false);

        SideNavRail nav = new SideNavRail();

        SideNavRailItem dashboard =
                new SideNavRailItem("Dashboard", "/", VaadinIcon.DASHBOARD.create());

        SideNavRailItem code = new SideNavRailItem(
                "Code", "/code", VaadinIcon.CODE.create());

        SideNavRailItem branches = new SideNavRailItem("Branches", "/code/branches");
        SideNavRailItem activeBranches = new SideNavRailItem("Active", "/code/branches/active");
        activeBranches.addItem(new SideNavRailItem("main", "/code/branches/active/main"));
        activeBranches.addItem(new SideNavRailItem("develop", "/code/branches/active/develop"));
        activeBranches.addItem(new SideNavRailItem("feature/*", "/code/branches/active/features"));
        branches.addItem(activeBranches);
        branches.addItem(new SideNavRailItem("Stale", "/code/branches/stale"));
        branches.addItem(new SideNavRailItem("Archived", "/code/branches/archived"));
        code.addItem(branches);
        code.addItem(new SideNavRailItem("Commits", "/code/commits"));
        code.addItem(new SideNavRailItem("Tags", "/code/tags"));

        SideNavRailItem operate = new SideNavRailItem(
                "Operate", "/operate", VaadinIcon.COGS.create());

        SideNavRailItem environments = new SideNavRailItem("Environments", "/operate/environments");
        environments.addItem(new SideNavRailItem("Production", "/operate/environments/prod"));
        environments.addItem(new SideNavRailItem("Staging", "/operate/environments/staging"));
        environments.addItem(new SideNavRailItem("Development", "/operate/environments/dev"));
        operate.addItem(environments);
        operate.addItem(new SideNavRailItem("Releases", "/operate/releases"));

        // Intentionally icon-less — demonstrates the letter-avatar fallback in rail mode.
        SideNavRailItem admin = new SideNavRailItem("Admin", "/admin");

        nav.addItem(dashboard, code, operate, admin);

        Button toggle = new Button(VaadinIcon.CHEVRON_LEFT_SMALL.create(),
                e -> {
                    boolean railMode = !nav.isRailMode();
                    nav.setRailMode(railMode);
                    e.getSource().setIcon((railMode ? VaadinIcon.CHEVRON_RIGHT_SMALL : VaadinIcon.CHEVRON_LEFT_SMALL).create());
                });


        VerticalLayout sidebar = new VerticalLayout(toggle, nav);
        sidebar.setPadding(false);
        sidebar.setSpacing(false);
        sidebar.setWidth(null);
        sidebar.getStyle().set("border-right", "1px solid var(--lumo-contrast-10pct)");

        contentArea.getStyle().set("padding", "var(--lumo-space-m)");

        HorizontalLayout body = new HorizontalLayout(sidebar, contentArea);
        body.setSizeFull();
        body.setPadding(false);
        body.setSpacing(false);
        body.setFlexGrow(1, contentArea);

        add(buildNavbar(nav), body);
        setFlexGrow(1, body);
    }

    private HorizontalLayout buildNavbar(SideNavRail nav) {
        Span title = new Span("SideNav Rail — Demo");
        title.getStyle()
                .set("font-weight", "600")
                .set("font-size", "var(--lumo-font-size-l)");

        Select<PopoverOn> modeSelect = new Select<>();
        modeSelect.setId("popover-mode-select");
        modeSelect.setLabel("Popover mode");
        modeSelect.setItems(PopoverOn.values());
        modeSelect.setItemLabelGenerator(MainLayout::humanize);
        modeSelect.setValue(nav.getPopoverOn());
        modeSelect.addValueChangeListener(e -> {
            if (e.getValue() != null) {
                nav.setPopoverOn(e.getValue());
            }
        });

        Select<PopoverParentLabelMode> parentLabelSelect = new Select<>();
        parentLabelSelect.setId("popover-parent-label-select");
        parentLabelSelect.setLabel("Popover header");
        parentLabelSelect.setItems(PopoverParentLabelMode.values());
        parentLabelSelect.setItemLabelGenerator(MainLayout::humanize);
        parentLabelSelect.setValue(nav.getPopoverParentLabelMode());
        parentLabelSelect.addValueChangeListener(e -> {
            if (e.getValue() != null) {
                nav.setPopoverParentLabelMode(e.getValue());
            }
        });

        Select<RailTooltipMode> tooltipSelect = new Select<>();
        tooltipSelect.setId("rail-tooltip-select");
        tooltipSelect.setLabel("Rail tooltips");
        tooltipSelect.setItems(RailTooltipMode.values());
        tooltipSelect.setItemLabelGenerator(MainLayout::humanize);
        tooltipSelect.setValue(nav.getRailTooltipMode());
        tooltipSelect.addValueChangeListener(e -> {
            if (e.getValue() != null) {
                nav.setRailTooltipMode(e.getValue());
            }
        });

        Checkbox nativeTooltipCheckbox = new Checkbox("Native tooltip");
        nativeTooltipCheckbox.setId("rail-tooltip-native");
        nativeTooltipCheckbox.setValue(nav.isRailTooltipNative());
        nativeTooltipCheckbox.addValueChangeListener(
                e -> nav.setRailTooltipNative(Boolean.TRUE.equals(e.getValue())));

        Checkbox childrenOnlyInPopoverCheckbox = new Checkbox("Children only in popover");
        childrenOnlyInPopoverCheckbox.setId("children-only-in-popover");
        childrenOnlyInPopoverCheckbox.setValue(nav.isChildrenOnlyInPopover());
        childrenOnlyInPopoverCheckbox.addValueChangeListener(
                e -> nav.setChildrenOnlyInPopover(Boolean.TRUE.equals(e.getValue())));

        Select<RootMatchNested> rootMatchNestedSelect = new Select<>();
        rootMatchNestedSelect.setId("root-match-nested-select");
        rootMatchNestedSelect.setLabel("Root matchNested");
        rootMatchNestedSelect.setItems(RootMatchNested.values());
        rootMatchNestedSelect.setItemLabelGenerator(MainLayout::humanize);
        rootMatchNestedSelect.setValue(nav.getRootMatchNested());
        rootMatchNestedSelect.addValueChangeListener(e -> {
            if (e.getValue() != null) {
                nav.setRootMatchNested(e.getValue());
            }
        });

        HorizontalLayout selects = new HorizontalLayout(
                modeSelect, parentLabelSelect, tooltipSelect,
                nativeTooltipCheckbox, childrenOnlyInPopoverCheckbox,
                rootMatchNestedSelect);
        selects.setAlignItems(FlexComponent.Alignment.END);
        selects.setSpacing(true);

        HorizontalLayout navbar = new HorizontalLayout(title, selects);
        navbar.setWidthFull();
        navbar.setPadding(true);
        navbar.setSpacing(true);
        navbar.setAlignItems(FlexComponent.Alignment.CENTER);
        navbar.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        navbar.getStyle().set("border-bottom", "1px solid var(--lumo-contrast-10pct)");
        return navbar;
    }

    private static String humanize(PopoverOn mode) {
        return switch (mode) {
            case ALL_COLLAPSED_ITEMS -> "All collapsed items";
            case ONLY_ROOT_COLLAPSED_ITEMS -> "Only root collapsed items";
            case ONLY_RAIL_MODE -> "Only in rail mode";
        };
    }

    private static String humanize(PopoverParentLabelMode mode) {
        return switch (mode) {
            case NONE -> "None";
            case LABEL_ONLY -> "Label only";
            case ICON_ONLY -> "Icon only";
            case FULL -> "Icon + label";
        };
    }

    private static String humanize(RailTooltipMode mode) {
        return switch (mode) {
            case NONE -> "None";
            case ONLY_WITHOUT_CHILDREN -> "Only without children";
            case ALL -> "All root items";
        };
    }

    private static String humanize(RootMatchNested mode) {
        return switch (mode) {
            case NONE -> "Off";
            case ONLY_RAIL -> "Only in rail mode";
            case ALL -> "Always";
        };
    }

    @Override
    public void showRouterLayoutContent(HasElement content) {
        contentArea.removeAll();
        if (content != null) {
            contentArea.getElement().appendChild(content.getElement());
        }
    }
}
