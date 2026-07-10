# SideNav Rail

A Vaadin Component Factory addon that adds a toggleable **rail mode** to `<vaadin-side-nav>`: rail mode shrinks the navigation down to a narrow, icon-only strip, and toggles back to full labels whenever you want.

On top of the toggle you get on-demand hover popovers for nested items, full keyboard support, and tooltips styled to match your Vaadin theme.

[![Vaadin Directory](https://img.shields.io/vaadin-directory/v/vcf-side-nav-rail.svg)](https://vaadin.com/directory/component/vcf-side-nav-rail)

![Normal mode (left) vs. rail mode with hover popover (right)](docs/screenshots/3-children.png)

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

## Compatibility

- Vaadin Flow 24.9 or later
- Java 17 or later

## Installation

Add the dependency to your application's `pom.xml`:

```xml
<dependency>
    <groupId>org.vaadin.addons.componentfactory</groupId>
    <artifactId>vcf-side-nav-rail</artifactId>
    <version>1.0.0</version>
</dependency>
```

The artifact is published to the Vaadin Add-ons repository, so make sure that repository is on your project's resolution list:

```xml
<repositories>
    <repository>
        <id>vaadin-addons</id>
        <url>https://maven.vaadin.com/vaadin-addons</url>
    </repository>
</repositories>
```

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

## Configuration

Most behaviour can be configured. The sections below cover each setting:

### Popover behaviour


`PopoverOn` controls when the popover appears:

- `ALL_COLLAPSED_ITEMS` (default) — popover for every collapsed parent, regardless of depth or rail mode.
- `ONLY_ROOT_COLLAPSED_ITEMS` — popover only for direct rail-children that are collapsed; nested levels behave like a stock `SideNav`.
- `ONLY_RAIL_MODE` — popovers only when rail mode is active; normal mode opens children inline on click.

```java
rail.setPopoverOn(PopoverOn.ONLY_ROOT_COLLAPSED_ITEMS);
```

By default, any collapsed parent shows its children in a hover popover — in both rail mode and normal mode — so users see the same nested children whether or not the rail is collapsed.

That's a deliberate departure from a stock `SideNav`, which expands children inline on click. If you'd rather keep that inline behaviour whenever the rail isn't collapsed, reach for `ONLY_RAIL_MODE` below.

Note: [`setChildrenOnlyInPopover(true)`](#children-only-in-popover) overrides this setting — see that section for details.

### Popover header

`PopoverHeaderMode` controls whether (and how) the parent's label appears as a header at the top of its popover:

- `NONE` — no header.
- `LABEL_ONLY` (default) — header shows the parent's text label only.
- `ICON_ONLY` — header shows a copy of the parent's prefix component (typically an icon) only. If the item's only prefix is the auto-generated letter avatar (the fallback shown for [icon-less items](#items-without-an-icon)), no header is shown — the avatar is treated as "no icon".
- `FULL` — header shows both the prefix component and the label, icon first.

```java
rail.setPopoverHeaderMode(PopoverHeaderMode.FULL);
```

By default the header appears only in rail mode — in normal mode the parent label is already visible inline, so a header would be redundant.

If needed you can enable headers also in normal mode popovers, by calling 

```java
rail.setPopoverHeaderOnlyInRailMode(false);  // header in both modes
```

Please note that this flag has no effect while `PopoverHeaderMode` is `NONE`.

### Popover delays

You can adjust popover open and close delays with

```java
rail.setPopoverHoverDelay(150);  // ms before the popover opens on hover
rail.setPopoverHideDelay(400);   // ms before it closes after the pointer leaves
```

### Popover position

You can control the popover position (default is `END_TOP`) relative to the target item using

```java
rail.setPopoverPosition(PopoverPosition.START_TOP);  // for a right-edge rail in LTR
```

### Popover arrow

By default, the popover uses the arrow theme to "point" at its target. You can disable it by calling

```java
rail.setPopoverArrowVisible(false);  // default: true
```

### Children only in popover

By default the `SideNavRail` works the same as the native `SideNav` regarding auto expanding to the current view's item. 
Alternatively you can configure the rail in a way, that it shows sub items only in popovers. This will deactivate the inline displayment of sub items.

```java
rail.setChildrenOnlyInPopover(true);  // default: false
```

#### Styling hint
The native side nav toggle is hidden in this mode and instead shows a similar looking indicator (from the active theme's icon font) next to parents — see the [Subitem indicator CSS custom properties](#subitem-indicator) below for how to restyle it.

This setting overrides `PopoverOn`. While it's enabled, root items with children always show their hover popover — in both rail mode and normal mode — no matter what `PopoverOn` is set to. That's because the popover is now the only path to the children and would hide them entirely otherwise.

### Rail-mode tooltip

In rail mode only the icons are visible, so it isn't always clear what each one stands for. The rail-mode tooltip surfaces the item's label on hover or keyboard focus. 

Tooltips appear on the rail's **root items only**, and **only in rail mode** — in normal mode the label is already visible inline, so no tooltip is needed.

`RailTooltipMode` chooses how that label is surfaced:

- `NONE` — no tooltips.
- `SIMPLE` — a small, theme-styled tooltip drawn as a pure-CSS pseudo-element. Shows on hover and on keyboard focus.
- `POPOVER_HEADER` (default) — the tooltip is realized by using the `Popover` showing the item's [popover header](#popover-header).

```java
rail.setRailTooltipMode(RailTooltipMode.SIMPLE);  // default is POPOVER_HEADER
```

> Please note: the simple tooltip is NOT the Vaadin built-in tooltip mechanic, as Vaadin's tooltips are managed by the same overlay mechanism as popovers. When a popover is opened, the Vaadin tooltip would be automatically dismissed. This is a known issue.

### Reacting to mode changes

Register a listener with `addRailModeChangedListener(...)` to run your own code whenever the rail is switched between rail mode and normal mode — for example to swap a toggle button's icon or resize the surrounding layout.

```java
rail.addRailModeChangedListener(e ->
        log.info("rail mode now {}", e.isRailMode()));
```

### Looking up the active item

`getActiveViewItem()` and `getActiveViewItems()` return the item(s) whose own path points at the page you're currently on. Use them whenever you want to do something with that item, for example add a sub-page to it, highlight it, or read its label.

```java
// Add a sub-page under whichever item points at the current page
// (the child's path is independent of the parent's — use any route)
rail.getActiveViewItem().ifPresent(item ->
    item.addItem(new SideNavRailItem("New sub-page", "/example/new")));
```

A few things to know:

- **An item counts as active only when its own path matches the current page.** A parent is *not* active just because one of its children matches, so navigating to `/admin/users` gives you the `Users` item, never the `Admin` root. (This is separate from [`setRootMatchNested(...)`](#active-marker-on-rail-icons), which only changes the active marker shown on the rail icons.)
- **If more than one item matches**, `getActiveViewItems()` returns all of them (parents before their children); `getActiveViewItem()` returns the first.
- **The result is always up to date** — it's looked up fresh each time, never cached. To react whenever the page changes, combine it with `UI#addAfterNavigationListener`.

### Active marker on rail icons

By default, Vaadin's `<vaadin-side-nav-item>` flags an item as `current` only when its own path matches the URL. So if a deeply nested route like `admin/users/active` is active, the root rail icon (Admin) does *not* pick up the active marker. Vaadin's own `setMatchNested(true)` flips this for an individual item, but you'd have to call it on every root manually and decide when to toggle it back.

`setRootMatchNested(RootMatchNested)` lets the addon manage that flag for you across all root items. This is especially useful when you display children only in the popover.

| Value | Behaviour |
| --- | --- |
| `NONE` | The addon never touches `matchNested`. |
| `ONLY_RAIL` (default) | Each root's `matchNested` is forced to `true` while the rail is in rail mode and restored on leaving rail mode. Recommended for the typical use case. |
| `ALL` | Each root's `matchNested` is always forced to `true`, regardless of rail mode. Useful in combination with [Children only in popover](#children-only-in-popover) or any other configuration where the visible inline tree doesn't expose deeper levels. |

```java
rail.setRootMatchNested(RootMatchNested.ONLY_RAIL);  // this is the default
```

### Items without an icon

A root `SideNavRailItem` without an icon gets a letter-avatar built from the label automatically so that the rail still has an icon to show. This avatar is only shown in the rail mode.

```java
new SideNavRailItem("Profile", "/profile");                         // → "P" letter-avatar
new SideNavRailItem("Profile", "/profile", new Avatar("Profile"));  // → "P" letter-avatar that is also shown in normal mode (since set explicitly)
```

## Styling

The addon styles the underlying `<vaadin-side-nav>` so all of the [stock SideNav styling hooks](https://vaadin.com/docs/latest/components/side-nav/styling) — parts, slots, `[current]`, `[expanded]`, etc. — keep working. On top of that, rail mode and the addon's own additions (popover header, rail tooltip, letter avatar, subitem indicator) introduce a few extra hooks.

Styling is split into three layers, one per subsection below:

- **Style properties** — CSS custom properties for design tokens (colors, sizes, durations).
- **CSS selectors** — structural overrides.
- **Recipes** — worked examples for common visual patterns.

Every custom property resolves through a fallback chain that ends in the active theme's own token. That's why the addon needs no CSS in a stock app and renders correctly under both **Lumo** (the Vaadin 24 default) and **Aura** (the Vaadin 25 default).

To override a property, set it on `vaadin-side-nav` — or globally on `html`.

### Style properties

The **Default** column shows what each property resolves to in a Vaadin 24 (Lumo) app — usually a Lumo token, or a literal value where noted (e.g. `200ms`, `ease-out`).

Under Aura (Vaadin 25), any default that is a Lumo token resolves to the equivalent Aura token through the same fallback chain, so the defaults stay theme-appropriate with no override needed. An Aura value is spelled out in the table only where it differs from that straight equivalent.

#### Rail

| Property | Description | Default |
| --- | --- | --- |
| `--side-nav-rail-width` | Width of the rail in rail mode. | `var(--lumo-size-l)` |
| `--side-nav-rail-transition-duration` | Duration of the rail-mode collapse/expand animation. Set to `0s` to disable. | `200ms` |
| `--side-nav-rail-transition-easing` | Easing function for the same animation. | `ease-out` |

#### Subitem indicator

The visual cue that signals an item has children. Rendered as a `::before` pseudo-element on parent items. Shown for parents in rail mode, and in normal mode too when `setChildrenOnlyInPopover(true)` is on.

| Property | Description | Default |
| --- | --- | --- |
| `--side-nav-rail-subitem-indicator-content` | Glyph; any value valid for CSS `content` (e.g. another Lumo icon, an emoji, a string). | `var(--lumo-icons-angle-right)` |
| `--side-nav-rail-subitem-indicator-color` | Color of the indicator. | `var(--lumo-tertiary-text-color)` |
| `--side-nav-rail-subitem-indicator-size` | Indicator size in normal mode (rendered next to the label). | `var(--lumo-font-size-m)` (Lumo) / `var(--aura-font-size-l)` (Aura) |
| `--side-nav-rail-subitem-indicator-rail-size` | Indicator size in rail mode (rendered as a corner badge). | `0.625rem` (Lumo) / `var(--aura-font-size-m)` (Aura) |

#### Rail-mode tooltip

These properties style the CSS tooltip used by `RailTooltipMode.SIMPLE` (drawn as the `vaadin-side-nav-item[data-rail-tooltip]::after` pseudo-element). The default `POPOVER_HEADER` tooltip is a Vaadin popover instead — style it through the [Popover header selectors](#popover-header-opt-in-via-setpopoverheadermode).

Note that `--side-nav-rail-tooltip-hover-delay` below is the `SIMPLE` tooltip's delay, separate from `setPopoverHoverDelay` (which times the children popover).

| Property | Description | Default |
| --- | --- | --- |
| `--side-nav-rail-tooltip-hover-delay` | Delay before the tooltip appears on hover/focus. | `500ms` |
| `--side-nav-rail-tooltip-fade-duration` | Fade-in duration. | `120ms` |
| `--side-nav-rail-tooltip-background` | Background color. | `var(--lumo-contrast-90pct)` |
| `--side-nav-rail-tooltip-color` | Text color. | `var(--lumo-base-color)` |
| `--side-nav-rail-tooltip-padding` | Padding shorthand. | `0.1875em var(--lumo-space-xs)` |
| `--side-nav-rail-tooltip-border-radius` | Border radius. | `var(--lumo-border-radius-s)` |
| `--side-nav-rail-tooltip-font-size` | Font size. | `var(--lumo-font-size-s)` |

### CSS selectors

Use these in a global stylesheet (e.g. `frontend/themes/<my-theme>/styles.css`). They do not work in shadow-DOM-scoped sheets.

#### Rail states

| Selector | Description |
| --- | --- |
| `vaadin-side-nav[theme~="rail"]` | The rail itself, in rail mode. Set by `setRailMode(true)`. |
| `vaadin-side-nav[theme~="inline-children-hidden"]` | The rail with children-only-in-popover on. Set by `setChildrenOnlyInPopover(true)`. |

#### Items

| Selector | Description |
| --- | --- |
| `vaadin-side-nav-item[root-item]` | A direct child of the rail (top-level icon row in rail mode). Set by the addon. Useful for adding extra padding or a separator around top-level items. |
| `vaadin-side-nav-item[has-children]` | Any item that has nested items (Vaadin native, used by the addon's subitem indicator). |
| `vaadin-side-nav-item[data-rail-tooltip]` | Items that get a rail-mode tooltip (set when `RailTooltipMode` is `SIMPLE`). The attribute value is the tooltip text and is fed straight into the `::after` pseudo-element via CSS `attr()`. |

#### Popover header (opt-in via `setPopoverHeaderMode`)

The popover header renders inside the popover overlay, which lives outside the `<vaadin-side-nav>` subtree — so a descendant selector like `vaadin-side-nav .side-nav-rail-popover-header` does not match. Target the class globally instead.

| Selector | Description |
| --- | --- |
| `.side-nav-rail-popover-header` | The header container (icon + label, depending on the mode). |
| `.side-nav-rail-popover-header-label` | The text label inside the header. |
| `.side-nav-rail-popover-header vaadin-icon` | The icon inside the header (when shown). |

#### Letter avatar (auto-generated for items without an icon)

| Selector | Description |
| --- | --- |
| `vaadin-avatar.side-nav-rail-letter-avatar` | The auto-generated avatar (only visible in rail mode by default). |
| `vaadin-avatar.side-nav-rail-letter-avatar::part(abbr)` | The letter glyph inside the avatar. |

#### Subitem indicator pseudo-element

| Selector | Description |
| --- | --- |
| `vaadin-side-nav-item[has-children]::before` | The subitem indicator itself. Non-interactive. |

### Recipes

#### Styling root items differently when only a child is active

When you enable the [active marker on rail icons](#active-marker-on-rail-icons) (via `setRootMatchNested(...)`), a root item is marked `[current]` whenever any of its descendants matches the route — even if the root itself isn't the active page. By default Vaadin gives that root the same active highlight as the leaf, so both the root and the actually-active child end up looking equally "selected".

If you'd rather keep the active highlight on the leaf alone and tone the root down (or style it in a third, distinct way), the two cases can be told apart in pure CSS via `:has()`:

| Selector | Matches |
| --- | --- |
| `vaadin-side-nav-item[root-item][current]:not(:has(vaadin-side-nav-item[current]))` | The root itself is the active route. |
| `vaadin-side-nav-item[root-item][current]:has(vaadin-side-nav-item[current])` | The root is `[current]` only because a descendant is the active route. |

`[root-item]` is the addon's hook on direct children of `SideNavRail` (see [Items](#items)). The qualifier matters because `setRootMatchNested(...)` only forces `matchNested = true` on root items — nested parents in between keep their default behaviour and are never `[current]` purely because of a descendant route.

The addon deliberately ships **no** default de-emphasis here. The reason is that the active-item look differs by theme — Lumo uses a filled background with primary-colored text, while Aura can additionally draw a border — so no single override would cover both.

Tone the root down using each theme's own hooks instead, targeting the "ancestor-only" selector from the table above.

**Lumo** — override the content part directly; there are no per-item custom properties, so set `background`/`color` on `::part(content)`:

```css
/* Lumo */
vaadin-side-nav-item[root-item][current]:has(vaadin-side-nav-item[current])::part(content) {
    background-color: transparent;
    color: var(--lumo-body-text-color);
}
```

**Aura** — the item exposes dedicated custom properties (`--vaadin-side-nav-item-*`), and its active state can also carry a border, so neutralize background, text, and border through those props (set them on the item element, not `::part(content)`):

```css
/* Aura */
vaadin-side-nav-item[root-item][current]:has(vaadin-side-nav-item[current]) {
    --vaadin-side-nav-item-background: transparent;
    --vaadin-side-nav-item-text-color: var(--vaadin-text-color);
    --vaadin-side-nav-item-border-width: 0px;
    /* …or keep a subtle outline instead of the full highlight:
       --vaadin-side-nav-item-border-color: var(--vaadin-border-color-secondary); */
}
```

Those are just the common three. The full set (`--vaadin-side-nav-item-font-size`, `-padding`, `-border-radius`, …) is in the [Vaadin SideNav styling reference](https://vaadin.com/docs/latest/components/side-nav/styling). What "de-emphasized" should mean — transparent, a subtle outline, or an icon-only tint — is an app-and-theme decision, which is why this lives here as a recipe rather than a fixed default.

`:has()` is supported in all browsers Vaadin 24 targets (Chrome 105+, Safari 15.4+, Firefox 121+).

### Further reading

- [Vaadin SideNav — styling reference](https://vaadin.com/docs/latest/components/side-nav/styling) — the parts, slots and properties of the underlying component, all of which apply here too.
- [Vaadin Popover — styling reference](https://vaadin.com/docs/latest/components/popover/styling) — for styling the overlay that hosts subitems.
- [Vaadin Avatar — styling reference](https://vaadin.com/docs/latest/components/avatar/styling) — for styling the letter-avatar fallback.

## Keyboard navigation and accessibility

The rail handles keyboard navigation and ARIA wiring out of the box — no extra setup required.

In rail mode only the root items are visible; nested items stay in the DOM but get `tabindex="-1"`, so the tab order always matches what's on screen. The relevant ARIA attributes (`aria-haspopup`, `aria-expanded`, `aria-current`) stay in sync as you toggle rail mode.

| Key | Behaviour |
| --- | --- |
| `Tab` / `Shift+Tab` | Move focus into and out of the rail. Items hidden in rail mode are skipped. |
| `↓` / `↑` | Move focus to the next / previous visible item, both inside the rail and inside an open popover. Stops at boundaries. |
| `→` | On a root item with children *while the rail shows only root icons* (rail mode, or `setChildrenOnlyInPopover(true)`): open the hover popover and move focus into it. Elsewhere: expand a collapsed parent, or move focus to the first child of an expanded one. |
| `←` | If the focused item is expanded, collapse it — this applies both in the rail and inside a popover. Otherwise move focus to the parent item; and if you are already at the top level of a popover, close it and return focus to the root item that owns it. |
| `Esc` | Close the open popover and return focus to the root item that owns it. |

## Scope and known gaps

- **Dark mode**: supported automatically through the active theme (Lumo or Aura) — no extra wiring needed.
- **RTL layouts**: not currently validated. The rail uses logical CSS properties where possible, but RTL has not been exercised end-to-end.
- **Touch / mobile**: out of scope. On touch devices the rail keeps its desktop hover-popover behaviour, and because touch has no hover, a tap may not open the children popover — so a collapsed parent's children can be hard to reach in rail mode. If you target small screens, plan to surface navigation another way (for example, switching out of rail mode) instead of relying on the popover.
- **Inside an overflow container**: placing the rail inside a container with `overflow: auto` (e.g. a `VerticalLayout` with `setOverflow(Overflow.AUTO)`) can cause an unwanted horizontal scrollbar. The root cause is an internal margin on a slot element inside `vaadin-side-nav-item`'s shadow DOM that cannot be addressed from outside. Workaround — add the following to your theme CSS:
  ```css
  vaadin-side-nav[theme~="rail"] {
      overflow: hidden;
      max-width: 100%;
  }
  ```
  Be aware that `overflow: hidden` clips the `SIMPLE` tooltip pseudo-element, which extends beyond the rail's bounds. If you apply this workaround, switch to `RailTooltipMode.POPOVER_HEADER` or `RailTooltipMode.NONE` (and provide your own tooltip if needed).

## Building from source

```bash
./mvnw clean verify
```

Runs the addon's unit tests (Karibu-Testing in `addon/`) plus Spotless and Checkstyle. The production-mode Playwright E2E suite lives in the `e2e/` module, which is only built under the `e2e` profile — a plain `verify` does **not** run it. To include the E2E suite:

```bash
./mvnw -Pe2e -Dvaadin.force.production.build=true clean verify
```

## Running the demo

```bash
./mvnw -pl demo -am spring-boot:run
```

(`-am` builds the sibling `addon` module first; without it a fresh clone fails to resolve `vcf-side-nav-rail`.)

The demo runs on [http://localhost:8080](http://localhost:8080) and walks through the configuration options (popover modes, tooltip modes, `matchNested` variants, styling overrides) on a single live nav.

## License

Copyright © Vaadin Ltd. Licensed under Apache 2.0 — see [`LICENSE`](LICENSE).

## Contributing

Bug reports and feature requests are welcome at [github.com/vaadin-component-factory/vcf-side-nav-rail/issues](https://github.com/vaadin-component-factory/vcf-side-nav-rail/issues). For larger changes, please open an issue first to discuss the approach.
