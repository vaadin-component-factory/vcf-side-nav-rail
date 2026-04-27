# SideNav Rail

A Vaadin Component Factory addon that adds a togglable rail mode to `<vaadin-side-nav>` — collapsed icon-only navigation with on-demand hover popovers, full keyboard support, and Lumo-styled tooltips.

[![Vaadin Directory](https://img.shields.io/vaadin-directory/v/vcf-side-nav-rail.svg)](https://vaadin.com/directory/component/vcf-side-nav-rail)

![Normal mode (left) vs. rail mode with hover popover (right)](docs/screenshots/3-children.png)

## Features

- **Rail mode toggle** — flip the nav between full-width and rail mode.
- **Hover popovers** for items with children, configurable in scope.
- **Full keyboard navigation** — use arrow keys to navigate through items.
- **Accessible by default** — ARIA roles and focus management handled for you.
- **Tooltips** on root items (rail mode).
- **Letter-avatar fallback** for items without an icon (rail mode).
- **Mode-change event** for downstream code that needs to react to the toggle.

## Compatibility

- Vaadin Flow **24.9** or later — tested against 24.10. Hilla / client-side views are not in scope.
- Java **17** or later

## Installation

Add the dependency to your application's `pom.xml`:

```xml
<dependency>
    <groupId>org.vaadin.addons.componentfactory</groupId>
    <artifactId>vcf-side-nav-rail</artifactId>
    <version>1.0.0</version>
</dependency>
```

## Quick start

You can use the `SideNavRail` like Vaadin's native `SideNav` component. Add `SideNavRailItem`s to add navigational items. Please note that the normal `SideNavItem` is not suppoted.

```java
SideNavRail rail = new SideNavRail();

SideNavRailItem dashboard = new SideNavRailItem(
        "Dashboard", "/dashboard", VaadinIcon.DASHBOARD.create());

SideNavRailItem code = new SideNavRailItem(
        "Code", "/code", VaadinIcon.CODE.create());
code.addItem(new SideNavRailItem("Branches", "/code/branches"));
code.addItem(new SideNavRailItem("Pull requests", "/code/pulls"));

rail.addItem(dashboard, code);

Button toggle = new Button(VaadinIcon.CHEVRON_LEFT_SMALL.create(), e -> {
    boolean railMode = !rail.isRailMode();
    rail.setRailMode(railMode);
    e.getSource().setIcon((railMode
            ? VaadinIcon.CHEVRON_RIGHT_SMALL
            : VaadinIcon.CHEVRON_LEFT_SMALL).create());
});

add(toggle, rail);
```

### No toggle button?

The addon intentionally ships without a built-in toggle button — applications differ too much in layout for a one-size-fits-all default. Wire your own button that calls `setRailMode(boolean)` or `toggleRailMode()`.

## Configuration

Most features can be configured to your needs. Please see the following sections for how to modify the `SideNavRail` settings:

### Popover behaviour

The rail shows sub-items as a hover popover when the rail is in rail mode. For consistency, the same popover is also available in normal mode. `PopoverOn` controls when it appears:

- `ALL_COLLAPSED_ITEMS` (default) — popover for every collapsed parent, regardless of depth or rail mode.
- `ONLY_ROOT_COLLAPSED_ITEMS` — popover only for direct rail-children that are collapsed; nested levels behave like a stock `SideNav`.
- `ONLY_RAIL_MODE` — popovers only when rail mode is active; normal mode opens children inline on click.

```java
rail.setPopoverOn(PopoverOn.ONLY_ROOT_COLLAPSED_ITEMS);
```

### Popover header

`PopoverParentLabelMode` controls whether (and how) the parent's label appears as a header at the top of its popover:

- `NONE` (default) — no header.
- `LABEL_ONLY` — header shows the parent's text label only.
- `ICON_ONLY` — header shows a copy of the parent's prefix component (typically an icon) only.
- `FULL` — header shows both the prefix component and the label, icon first.

```java
rail.setPopoverParentLabelMode(PopoverParentLabelMode.FULL);
```

### Popover arrow

By default each popover renders the small Lumo arrow that points back at its target item. Toggle it off if you prefer a cleaner look — e.g. when popovers sit tightly against the rail and the arrow adds visual noise:

```java
rail.setPopoverArrowVisible(false);  // default: true
```

### Children only in popover

By default Vaadin's `<vaadin-side-nav-item>` auto-expands when a descendant route matches, so navigating to e.g. `/admin/users/active` shows the chain inline below the parent. If you want a flat, popover-driven look — children appear only in the hover popover, never inline — turn this on:

```java
rail.setChildrenOnlyInPopover(true);  // default: false
```

The native chevron toggle is hidden in this mode (it would have nothing to reveal in the rail itself). To preserve the visual hint that an item has more, the addon renders a small Lumo angle-right glyph next to parents. The same indicator appears automatically in **rail mode** for any parent — children there are always popover-only by design, so the cue is always relevant.

Customizable via CSS custom properties:

```css
vaadin-side-nav {
    /* glyph: any value valid for CSS `content`, default Lumo angle-right */
    --side-nav-rail-popover-indicator-content: var(--lumo-icons-angle-right);
    /* color, default `var(--lumo-tertiary-text-color)` */
    --side-nav-rail-popover-indicator-color: var(--lumo-tertiary-text-color);
    /* normal mode size, default `var(--lumo-font-size-l)` */
    --side-nav-rail-popover-indicator-size: var(--lumo-font-size-l);
    /* rail mode size, default 0.625rem (kept tiny so the rail does not widen) */
    --side-nav-rail-popover-indicator-rail-size: 0.625rem;
}
```

Turning `setChildrenOnlyInPopover(false)` restores the default tree appearance with whatever expanded state the items had accumulated.

### Rail-mode tooltip

Because rail mode shows only icons, users may not be able to tell what each icon represents. The rail-mode tooltip surfaces each root item's label on hover or keyboard focus. Tooltips apply to the root items of the rail only. 

`RailTooltipMode` controls which root items get one:

- `NONE` — no tooltips.
- `ONLY_WITHOUT_CHILDREN` — only root items that have no children (intended to be used with the **popover parent label mode** , where a tooltip would be redundant).
- `ALL` (default) — every root item, regardless of whether it has children.

```java
rail.setRailTooltipMode(RailTooltipMode.ONLY_WITHOUT_CHILDREN);
```

> The addon comes with its own tooltip implementation, since Vaadin's native tooltip auto-dismisses itself whenever a popover opens — see [vaadin/web-components#9768](https://github.com/vaadin/web-components/issues/9768).
>
> If you prefer browser-native tooltips instead, you can activate them via `setRailTooltipNative(true)`. Note that these cannot be styled and will also not appear when focusing the rail items using the keyboard.

### Reacting to mode changes

You can react to rail mode toggles, using a dedicated event listener (`addRailModeChangedListener(...)`). Whenever the rail mode is toggled, this event listener will be informed. 

```java
rail.addRailModeChangedListener(e ->
        log.info("rail mode now {}", e.isRailMode()));
```

### Active marker on rail icons

By default, Vaadin's `<vaadin-side-nav-item>` only flags an item as `current` when its own path matches the URL — so when a deeply nested route is active (e.g. `admin/users/active`), the rail-side root icon (Admin) does not pick up the active marker. Setting `matchNested(true)` on each root item flips this: the root also counts as `current` when any descendant matches.

`setRailRootItemsMatchNested(true)` opts into having the addon manage that flag automatically: while the rail is in rail mode, each root item's `matchNested` is forced to `true`; on leaving rail mode (or turning the feature back off), the original `matchNested` value the user had set is restored. Per-item snapshotting means a `setMatchNested(...)` call you made yourself is preserved across rail-mode cycles — the override is layered on top, never destructive.

```java
rail.setRailRootItemsMatchNested(true);  // default: false
```

### Items without an icon

A root `SideNavRailItem` without an icon gets a letter-avatar built from the label automatically so that the rail still has an icon to show. This avatar is only shown in the rail mode.

```java
new SideNavRailItem("Profile", "/profile");                         // → "P" letter-avatar
new SideNavRailItem("Profile", "/profile", new Avatar("Profile"));  // → "P" letter-avatar, that is also shown in normal mode (since set explicitly)
```

## Building from source

```bash
./mvnw clean verify
```

Runs the addon's unit tests (Karibu-Testing in `addon/`) and the production-mode Playwright E2E suite (in `e2e/`).

## Running the demo

```bash
./mvnw -pl demo spring-boot:run
```

The demo runs on [http://localhost:8080](http://localhost:8080).

## License

Apache 2.0 — see [`LICENSE`](LICENSE).

## Contributing

Bug reports and feature requests are welcome at [github.com/vaadin-component-factory/side-nav-rail/issues](https://github.com/vaadin-component-factory/side-nav-rail/issues). For larger changes, please open an issue first to discuss the approach.
