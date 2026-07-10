# SideNav Rail

A Vaadin Component Factory addon that adds a toggleable **rail mode** to `<vaadin-side-nav>`: rail mode shrinks the navigation down to a narrow, icon-only strip, and toggles back to full labels whenever you want.

On top of the toggle you get on-demand hover popovers for nested items, full keyboard support, and tooltips styled to match your Vaadin theme.


Please check the Github Repository for the full readme to get details on usage and configuration.

## Features

- **Rail mode toggle** — flip the nav between full-width and rail mode.
- **Hover popovers** for items with children — you decide which items get one, with adjustable hover/hide delays, position, and arrow visibility.
- **Rail-mode tooltips** — show each icon's label on hover or keyboard focus so collapsed icons stay legible, in your choice of styles.
- **Letter-avatar fallback** for root items without an icon (rail mode).
- **Subitem indicator** — visual cue on parent items, with full CSS-property control for glyph, color, and size.
- **Children-only-in-popover layout** — flat rail with descendants reachable only via the hover popover.
- **Highlight roots with an active sub-item** — optionally give a root item the `[current]` marker whenever one of its child pages is the active page, either only in rail mode or always.
- **Full keyboard navigation** — Arrow keys, Tab, Escape; ARIA roles and focus management handled for you.
- **Mode-change event** for downstream code that needs to react to the toggle.

## Quick start

Use `SideNavRail` like Vaadin's native `SideNav`. Populate it with `SideNavRailItem`s — the addon's own item type, which adds the rail-specific behaviour (icon in the rail, popover header, tooltip) on top of the native `SideNavItem`.

The items you add directly to the rail are its **root items** — the icons you see in rail mode; their children appear nested inline or in a popover.

```java
SideNavRail rail = new SideNavRail();

SideNavRailItem dashboard = new SideNavRailItem(
        "Dashboard", DashboardView.class, VaadinIcon.DASHBOARD.create());

SideNavRailItem code = new SideNavRailItem(
        "Code", CodeView.class, VaadinIcon.CODE.create());
code.addItem(new SideNavRailItem("Branches", BranchesView.class));
code.addItem(new SideNavRailItem("Pull requests", PullRequestsView.class));

rail.addItem(dashboard, code);

Button toggle = new Button(VaadinIcon.CHEVRON_LEFT_SMALL.create(), e -> {
    rail.toggleRailMode();
    e.getSource().setIcon((rail.isRailMode()
            ? VaadinIcon.CHEVRON_RIGHT_SMALL
            : VaadinIcon.CHEVRON_LEFT_SMALL).create());
});

add(toggle, rail);
```

All public types live in the `org.vaadin.addons.componentfactory.sidenavrail` package.

### Can I use normal `SideNavItem`s?

No. The rail only accepts its own `SideNavRailItem` type — passing a plain `SideNavItem` to `addItem(...)` throws `IllegalArgumentException`. `SideNavRailItem` extends `SideNavItem`, so you keep the full native API and gain the rail-specific behaviour (rail icon, popover header, tooltip) on top.

### No toggle button?

The addon intentionally ships without a built-in toggle button — applications differ too much in layout for a one-size-fits-all default. Wire your own button that calls `setRailMode(boolean)` or `toggleRailMode()`.
