# SideNav Rail

A Vaadin Component Factory addon that adds a togglable rail mode to `<vaadin-side-nav>` — collapsed icon-only navigation with on-demand hover popovers, full keyboard support, and Lumo-styled tooltips.

[![Vaadin Directory](https://img.shields.io/vaadin-directory/v/vcf-side-nav-rail.svg)](https://vaadin.com/directory/component/vcf-side-nav-rail)

![Children inline vs in popover](docs/screenshots/3-children.png)

## Features

- **Rail mode toggle** — flip the nav between full-width and icon-only at runtime.
- **Hover popovers** for items with children, configurable in scope.
- **Full keyboard navigation** in both modes.
- **Accessible by default** — ARIA roles and focus management handled for you.
- **Tooltips** in rail mode that coexist with the popover without flickering.
- **Letter-avatar fallback** for items without an icon.
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

Button toggle = new Button(VaadinIcon.CHEVRON_LEFT_SMALL.create(),
        e -> rail.setRailMode(!rail.isRailMode()));

add(toggle, rail);
```

## Configuration

The defaults match the customer-requested behaviour of the original `SideNavRail` and need no extra code. Each of the following knobs is opt-in.

### Popover behaviour

`PopoverMode` controls when the hover popover appears for items with children. Three values:

- `ALL_COLLAPSED_ITEMS` (default) — popover for every collapsed parent, regardless of depth or rail mode.
- `ONLY_ROOT_COLLAPSED_ITEMS` — popover only for direct rail-children that are collapsed; nested levels behave like a stock `SideNav`.
- `ONLY_RAIL_MODE` — popovers only when rail mode is active; normal mode opens children inline on click.

```java
rail.setPopoverMode(PopoverMode.ONLY_ROOT_COLLAPSED_ITEMS);
```

### Popover header

`PopoverParentLabelMode` controls whether the parent's label appears at the top of its own popover — useful when the popover is the only place that label is visible (rail mode).

```java
rail.setPopoverParentLabelMode(PopoverParentLabelMode.BOLD);
// NONE (default), INLINE, BOLD
```

### Rail-mode tooltip

`RailTooltipMode` decides which rail icons surface their label as a tooltip on hover and on keyboard focus. The tooltip is a CSS pseudo-element that coexists with popovers — use `setRailTooltipNative(true)` to fall back to the browser-native `title` attribute.

```java
rail.setRailTooltipMode(RailTooltipMode.ONLY_WITHOUT_CHILDREN);
// NONE, ONLY_WITHOUT_CHILDREN, ALL (default)

rail.setRailTooltipNative(true);  // swap pseudo-element for title attribute
```

### Reacting to mode changes

`RailModeChangedEvent` fires on every actual `setRailMode(...)` transition (no-ops do not fire). Carries the new boolean.

```java
rail.addRailModeChangedListener(e ->
        log.info("rail mode now {}", e.isRailMode()));
```

### Items without an icon

A `SideNavRailItem` constructed without a prefix component gets a Lumo letter-avatar built from the label automatically (so the rail still has an icon column). Pass any `Component` as prefix to override:

```java
new SideNavRailItem("Profile", "/profile");                      // → "P" letter-avatar
new SideNavRailItem("Profile", "/profile", new Avatar("Jane"));  // → custom avatar
```

Full Javadoc is published with each release alongside the addon JAR.

## Building from source

```bash
./mvnw clean verify
```

Runs the addon's unit tests, the Karibu UI tests in `e2e/`, and the production-mode Playwright suite.

## Running the demo

```bash
./mvnw -pl demo spring-boot:run
```

The demo runs on [http://localhost:8080](http://localhost:8080) with hot reload enabled.

## License

Apache 2.0 — see [`LICENSE`](LICENSE).

## Contributing

Bug reports and feature requests are welcome at [github.com/vaadin-component-factory/side-nav-rail/issues](https://github.com/vaadin-component-factory/side-nav-rail/issues). For larger changes, please open an issue first to discuss the approach.
