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
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.dom.ThemeList;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.AfterNavigationObserver;
import com.vaadin.flow.router.Layout;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.theme.lumo.Lumo;
import org.vaadin.addons.componentfactory.sidenavrail.*;
import org.vaadin.addons.componentfactory.sidenavrail.demo.views.*;

/**
 * Plain layout frame instead of {@code AppLayout} — the rail mode is the point of the demo and an
 * AppLayout drawer + navbar adds chrome that obscures the effect. A thin top navbar hosts a {@link
 * Select} for live-switching the rail's {@link PopoverOn}; the sidebar with the rail sits below it.
 */
@Layout
@CssImport("./demo-styles.css")
public class MainLayout extends VerticalLayout implements RouterLayout, AfterNavigationObserver {

    private final Div contentArea = new Div();
    private final Span activeItemBreadcrumb = new Span();
    private final SideNavRail nav;

    public MainLayout() {
        setSizeFull();
        setPadding(false);
        setSpacing(false);

        nav = new SideNavRail();

        SideNavRailItem dashboard =
                new SideNavRailItem("Dashboard", ShowcaseView.class, VaadinIcon.DASHBOARD.create());

        SideNavRailItem branches = new SideNavRailItem("Branches", BranchesView.class);

        SideNavRailItem activeBranches = new SideNavRailItem("Active", ActiveBranchesView.class);
        activeBranches.addItem(new SideNavRailItem("main", MainBranchView.class));
        activeBranches.addItem(new SideNavRailItem("develop", DevelopBranchView.class));
        activeBranches.addItem(new SideNavRailItem("feature/*", FeatureBranchesView.class));
        branches.addItem(activeBranches);

        branches.addItem(new SideNavRailItem("Stale", StaleBranchesView.class));
        branches.addItem(new SideNavRailItem("Archived", ArchivedBranchesView.class));

        SideNavRailItem code =
                new SideNavRailItem("Code", CodeView.class, VaadinIcon.CODE.create());
        code.addItem(branches);

        SideNavRailItem commits = new SideNavRailItem("Commits", CommitsView.class);
        code.addItem(commits);
        code.addItem(new SideNavRailItem("Tags", TagsView.class));

        SideNavRailItem environments = new SideNavRailItem("Environments", EnvironmentsView.class);
        environments.addItem(new SideNavRailItem("Production", ProductionView.class));
        environments.addItem(new SideNavRailItem("Staging", StagingView.class));
        environments.addItem(new SideNavRailItem("Development", DevelopmentView.class));

        SideNavRailItem operate =
                new SideNavRailItem("Operate", OperateView.class, VaadinIcon.COGS.create());
        operate.addItem(environments);
        operate.addItem(new SideNavRailItem("Releases", ReleasesView.class));

        // Intentionally icon-less — demonstrates the letter-avatar fallback in rail mode.
        SideNavRailItem admin = new SideNavRailItem("Admin", AdminView.class);

        nav.addItem(dashboard, code, operate, admin);

        Button toggle =
                new Button(
                        VaadinIcon.CHEVRON_LEFT_SMALL.create(),
                        e -> {
                            boolean railMode = !nav.isRailMode();
                            nav.setRailMode(railMode);
                            e.getSource()
                                    .setIcon(
                                            (railMode
                                                            ? VaadinIcon.CHEVRON_RIGHT_SMALL
                                                            : VaadinIcon.CHEVRON_LEFT_SMALL)
                                                    .create());
                        });

        VerticalLayout sidebar = new VerticalLayout(toggle, nav);
        sidebar.setPadding(false);
        sidebar.setSpacing(false);
        sidebar.setWidth(null);
        sidebar.getStyle().set("border-right", "1px solid var(--lumo-contrast-10pct)");

        activeItemBreadcrumb.setId("active-item-breadcrumb");
        activeItemBreadcrumb
                .getStyle()
                .set("font-size", "var(--lumo-font-size-s)")
                .set("color", "var(--lumo-secondary-text-color)")
                .set("margin-bottom", "var(--lumo-space-s)");

        VerticalLayout content = new VerticalLayout(activeItemBreadcrumb, contentArea);
        content.setPadding(false);
        content.setSpacing(false);
        content.setSizeFull();
        content.getStyle().set("padding", "var(--lumo-space-m)");
        content.setFlexGrow(1, contentArea);

        HorizontalLayout body = new HorizontalLayout(sidebar, content);
        body.setSizeFull();
        body.setPadding(false);
        body.setSpacing(false);
        body.setFlexGrow(1, content);

        add(buildNavbar(nav), body);
        setFlexGrow(1, body);
    }

    /**
     * Demonstrates {@link SideNavRail#getActiveViewItem()}: re-render the breadcrumb after each
     * navigation. {@code AfterNavigationObserver} fires for the initial navigation too, so the
     * first paint already shows the resolved label — no flash through "(no match)". Match is
     * path-equality only; {@code matchNested} is intentionally ignored, so a parent doesn't surface
     * as active.
     */
    @Override
    public void afterNavigation(AfterNavigationEvent event) {
        activeItemBreadcrumb.setText(
                nav.getActiveViewItem()
                        .map(item -> "Active: " + item.getLabel())
                        .orElse("Active: (no match)"));
    }

    private HorizontalLayout buildNavbar(SideNavRail nav) {
        Span title = new Span("SideNav Rail — Demo");
        title.getStyle().set("font-weight", "600").set("font-size", "var(--lumo-font-size-l)");

        // Vaadin 24 has only Lumo (no Aura), so there's no theme picker here — just a
        // light/dark toggle. In V24 the color scheme is the Lumo.DARK theme variant on the
        // UI element (V25's demo twin uses Page.setColorScheme instead).
        Select<String> schemeSelect = new Select<>();
        schemeSelect.setId("color-scheme-select");
        schemeSelect.setLabel("Color scheme");
        schemeSelect.setItems("Light", "Dark");
        schemeSelect.setValue("Light");
        schemeSelect.addValueChangeListener(
                e -> {
                    boolean dark = "Dark".equals(e.getValue());
                    getUI().ifPresent(
                                    ui -> {
                                        ThemeList themes = ui.getElement().getThemeList();
                                        if (dark) {
                                            themes.add(Lumo.DARK);
                                        } else {
                                            themes.remove(Lumo.DARK);
                                        }
                                    });
                });

        Select<PopoverOn> modeSelect = new Select<>();
        modeSelect.setId("popover-mode-select");
        modeSelect.setLabel("Popover mode");
        modeSelect.setItems(PopoverOn.values());
        modeSelect.setItemLabelGenerator(MainLayout::humanize);
        modeSelect.setValue(nav.getPopoverOn());
        modeSelect.addValueChangeListener(
                e -> {
                    if (e.getValue() != null) {
                        nav.setPopoverOn(e.getValue());
                    }
                });

        Select<PopoverHeaderMode> headerSelect = new Select<>();
        headerSelect.setId("popover-header-select");
        headerSelect.setLabel("Popover header");
        headerSelect.setItems(PopoverHeaderMode.values());
        headerSelect.setItemLabelGenerator(MainLayout::humanize);
        headerSelect.setValue(nav.getPopoverHeaderMode());

        Select<RailTooltipMode> tooltipSelect = new Select<>();
        tooltipSelect.setId("rail-tooltip-select");
        tooltipSelect.setLabel("Rail tooltips");
        tooltipSelect.setItems(RailTooltipMode.values());
        tooltipSelect.setItemLabelGenerator(MainLayout::humanize);
        tooltipSelect.setValue(nav.getRailTooltipMode());

        // Mirror the addon's auto-coerce: RailTooltipMode.POPOVER_HEADER together with
        // PopoverHeaderMode.NONE would silently flip to LABEL_ONLY at attach.
        // Snap headerSelect to LABEL_ONLY whenever the user enters that combination
        // from either side, so the demo UI stays consistent with the rail.
        headerSelect.addValueChangeListener(
                e -> {
                    if (e.getValue() != null) {
                        nav.setPopoverHeaderMode(e.getValue());
                    }
                    if (tooltipSelect.getValue() == RailTooltipMode.POPOVER_HEADER
                            && e.getValue() == PopoverHeaderMode.NONE) {
                        headerSelect.setValue(PopoverHeaderMode.LABEL_ONLY);
                    }
                });

        tooltipSelect.addValueChangeListener(
                e -> {
                    if (e.getValue() != null) {
                        nav.setRailTooltipMode(e.getValue());
                    }
                    if (e.getValue() == RailTooltipMode.POPOVER_HEADER
                            && headerSelect.getValue() == PopoverHeaderMode.NONE) {
                        headerSelect.setValue(PopoverHeaderMode.LABEL_ONLY);
                    }
                });

        Checkbox childrenOnlyInPopoverCheckbox = new Checkbox("Children only in popover");
        childrenOnlyInPopoverCheckbox.setId("children-only-in-popover");
        childrenOnlyInPopoverCheckbox.setValue(nav.isChildrenOnlyInPopover());
        childrenOnlyInPopoverCheckbox.addValueChangeListener(
                e -> nav.setChildrenOnlyInPopover(Boolean.TRUE.equals(e.getValue())));

        Checkbox headerOnlyInRailModeCheckbox = new Checkbox("Header only in rail mode");
        headerOnlyInRailModeCheckbox.setId("popover-header-only-in-rail-mode");
        headerOnlyInRailModeCheckbox.setValue(nav.isPopoverHeaderOnlyInRailMode());
        headerOnlyInRailModeCheckbox.addValueChangeListener(
                e -> nav.setPopoverHeaderOnlyInRailMode(Boolean.TRUE.equals(e.getValue())));

        Select<RootMatchNested> rootMatchNestedSelect = new Select<>();
        rootMatchNestedSelect.setId("root-match-nested-select");
        rootMatchNestedSelect.setLabel("Root matchNested");
        rootMatchNestedSelect.setItems(RootMatchNested.values());
        rootMatchNestedSelect.setItemLabelGenerator(MainLayout::humanize);
        rootMatchNestedSelect.setValue(nav.getRootMatchNested());
        rootMatchNestedSelect.addValueChangeListener(
                e -> {
                    if (e.getValue() != null) {
                        nav.setRootMatchNested(e.getValue());
                    }
                });

        HorizontalLayout selects =
                new HorizontalLayout(
                        schemeSelect,
                        modeSelect,
                        headerSelect,
                        tooltipSelect,
                        childrenOnlyInPopoverCheckbox,
                        headerOnlyInRailModeCheckbox,
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

    private static String humanize(PopoverHeaderMode mode) {
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
            case SIMPLE -> "Simple";
            case POPOVER_HEADER -> "Popover header";
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
