# SideNav Rail

A Vaadin Component Factory addon that adds a togglable rail mode to `<vaadin-side-nav>` — collapsed icon-only navigation with on-demand hover popovers, full keyboard support, and Lumo-styled tooltips.

[![Vaadin Directory](https://img.shields.io/vaadin-directory/v/vcf-side-nav-rail.svg)](https://vaadin.com/directory/component/vcf-side-nav-rail)

![Children inline vs in popover](docs/screenshots/3-children.png)

## Features

- **Rail mode toggle** — flip the nav between full-width and rail mode.
- **Hover popovers** for items with children, configurable in scope.
- **Full keyboard navigation** — use arrow keys to navigate through items.
- **Accessible by default** — ARIA roles and focus management handled for you.
- **Tooltips** on root items (rail mode).
- **Letter-avatar fallback** for items without an icon (rail mode).
- **Mode-change event** for downstream code that needs to react to the toggle.

## Compatibility

- Vaadin **24.9** or later
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

Please note, that the component iself does NOT come with a built-in toggle button. This is because each application is designed differently and thus we do not want to affect or dictate any design decisions.
Therefore you need to setup your own toggle button, which calls `setRailMode(boolean)` or `toggleRailMode()`. 

## Configuration

Most features can be configured to your needs. Please see the following sections for how to modify the `SideNavRail` settings:

### Popover behaviour

The `SideNavRail` shows sub items as a popover, when the rail mode is active. For consistent behavior, this feature is also available for the normal mode. With `PopoverMode` you can control
when the hover popover shall appear:

- `ALL_COLLAPSED_ITEMS` (default) — popover for every collapsed parent, regardless of depth or rail mode.
- `ONLY_ROOT_COLLAPSED_ITEMS` — popover only for direct rail-children that are collapsed; nested levels behave like a stock `SideNav`.
- `ONLY_RAIL_MODE` — popovers only when rail mode is active; normal mode opens children inline on click.

```java
rail.setPopoverMode(PopoverMode.ONLY_ROOT_COLLAPSED_ITEMS);
```

### Popover header

With `PopoverParentLabelMode`, you control whether (and how) the parent's label should be shown as the header of the popup:

- `NONE` (default) — no header.
- `LABEL_ONLY` — header shows the parent's text label only.
- `ICON_ONLY` — header shows a copy of the parent's prefix component (typically an icon) only.
- `FULL` — header shows both the prefix component and the label, icon first.

```java
rail.setPopoverParentLabelMode(PopoverParentLabelMode.FULL);
```

### Rail-mode tooltip

Since the rail mode only shows icons, it can be hard for users to determine, what each icon represents. With the rail mode 
tooltip, the component can be configured to show the respective item's label as a tooltip , when hovered / focused.

`RailTooltipMode`:

- `NONE` — no tooltips.
- `ONLY_WITHOUT_CHILDREN` — only leaf items (which have no popover) get a tooltip.
- `ALL` (default) — every root item gets a tooltip, regardless of whether it has children.

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

### Items without an icon

A `SideNavRailItem` constructed without a prefix component gets a Lumo letter-avatar built from the label automatically so that the rail still has an icon to show. 

```java
new SideNavRailItem("Profile", "/profile");                      // → "P" letter-avatar
new SideNavRailItem("Profile", "/profile", new Avatar("Jane"));  // → custom avatar
```

## Building from source

```bash
./mvnw clean verify
```

Runs the addon's unit tests, the Karibu UI tests in `e2e/`, and the production-mode Playwright suite.

## Running the demo

```bash
./mvnw -pl demo spring-boot:run
```

The demo runs on [http://localhost:8080](http://localhost:8080).

## License

Apache 2.0 — see [`LICENSE`](LICENSE).

## Contributing

Bug reports and feature requests are welcome at [github.com/vaadin-component-factory/side-nav-rail/issues](https://github.com/vaadin-component-factory/side-nav-rail/issues). For larger changes, please open an issue first to discuss the approach.
