# SideNav Rail

A Vaadin Component Factory addon that adds a toggleable rail mode to `<vaadin-side-nav>` — collapsed icon-only navigation with on-demand hover popovers, full keyboard support, and tooltips styled to match your Vaadin theme. Rail mode shrinks the navigation to a narrow icon-only strip; toggle it back and the full labels return.

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
        "Dashboard", "/dashboard", VaadinIcon.DASHBOARD.create());

SideNavRailItem code = new SideNavRailItem(
        "Code", "/code", VaadinIcon.CODE.create());
code.addItem(new SideNavRailItem("Branches", "/code/branches"));
code.addItem(new SideNavRailItem("Pull requests", "/code/pulls"));

rail.addItem(dashboard, code);

Button toggle = new Button(VaadinIcon.CHEVRON_LEFT_SMALL.create(), e -> {
    rail.toggleRailMode();
    e.getSource().setIcon((rail.isRailMode()
            ? VaadinIcon.CHEVRON_RIGHT_SMALL
            : VaadinIcon.CHEVRON_LEFT_SMALL).create());
});

add(toggle, rail);
```

All public types live in the `org.vaadin.addons.componentfactory.sidenavrail` package. Note that only `SideNavRailItem`s can be added to the rail — passing a plain `SideNavItem` to `addItem(...)` throws `IllegalArgumentException`.

### No toggle button?

The addon intentionally ships without a built-in toggle button — applications differ too much in layout for a one-size-fits-all default. Wire your own button that calls `setRailMode(boolean)` or `toggleRailMode()`.

## Keyboard navigation and accessibility

The rail handles keyboard navigation and ARIA wiring out of the box; no extra setup is required. In rail mode only the root items are visible — nested items remain in the DOM but get `tabindex="-1"`, so the tab order matches what's on screen. The relevant ARIA attributes (`aria-haspopup`, `aria-expanded`, `aria-current`) are kept in sync as the rail mode toggles.

| Key | Behaviour |
| --- | --- |
| `Tab` / `Shift+Tab` | Move focus into and out of the rail. Items hidden in rail mode are skipped. |
| `↓` / `↑` | Move focus to the next / previous visible item, both inside the rail and inside an open popover. Stops at boundaries. |
| `→` | On a root item with children *while the rail shows only root icons* (rail mode, or `setChildrenOnlyInPopover(true)`): open the hover popover and move focus into it. Elsewhere: expand a collapsed parent, or move focus to the first child of an expanded one. |
| `←` | If the focused item is expanded, collapse it — this applies both in the rail and inside a popover. Otherwise move focus to the parent item; and if you are already at the top level of a popover, close it and return focus to the root item that owns it. |
| `Esc` | Close the open popover and return focus to the root item that owns it. |

## Configuration

Most behaviour can be configured. The sections below cover each setting:

### Popover behaviour

By default, any collapsed parent shows its children in a hover popover — in both rail mode and normal mode — so users see the same nested children whether or not the rail is collapsed.

This differs from a stock `SideNav`, which expands children inline on click; choose `ONLY_RAIL_MODE` below to keep that inline behaviour whenever the rail isn't collapsed. `PopoverOn` controls when the popover appears:

- `ALL_COLLAPSED_ITEMS` (default) — popover for every collapsed parent, regardless of depth or rail mode.
- `ONLY_ROOT_COLLAPSED_ITEMS` — popover only for direct rail-children that are collapsed; nested levels behave like a stock `SideNav`.
- `ONLY_RAIL_MODE` — popovers only when rail mode is active; normal mode opens children inline on click.

```java
rail.setPopoverOn(PopoverOn.ONLY_ROOT_COLLAPSED_ITEMS);
```

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

By default the header is rendered only while the rail is in rail mode — in normal mode the parent label is already visible inline, so the header would be redundant. When the rail uses `setChildrenOnlyInPopover(true)` (popover-only layout in normal mode too) you usually want the header in both modes; opt out via:

```java
rail.setPopoverHeaderOnlyInRailMode(false);  // header in both modes
```

This flag has no effect while `PopoverHeaderMode` is `NONE`.

### Popover delays

Popovers match Lumo's hover/hide-delay defaults (200 ms / 300 ms). Adjust them if your rail wants snappier or more forgiving timing:

```java
rail.setPopoverHoverDelay(150);  // ms before the popover opens
rail.setPopoverHideDelay(400);   // ms before it closes after the pointer leaves
```

### Popover position

Where the popover opens relative to its target item, expressed as Vaadin's `PopoverPosition`. Default: `END_TOP` — top-aligned, to the inline-end of the item (right in an LTR layout). Suitable for a rail pinned to the inline-start edge.

```java
rail.setPopoverPosition(PopoverPosition.START_TOP);  // for a right-edge rail in LTR
```

### Popover arrow

By default each popover renders the small theme arrow that points back at its target item. Toggle it off if you prefer a cleaner look — e.g. when popovers sit tightly against the rail and the arrow adds visual noise:

```java
rail.setPopoverArrowVisible(false);  // default: true
```

### Children only in popover

By default Vaadin's `<vaadin-side-nav-item>` auto-expands when a descendant route matches, so navigating to e.g. `/admin/users/active` shows the chain inline below the parent. If you want a flat, popover-driven look — children appear only in the hover popover, never inline — turn this on:

```java
rail.setChildrenOnlyInPopover(true);  // default: false
```

The native chevron toggle is hidden in this mode (it would have nothing to reveal in the rail itself). To preserve the visual hint that an item has more, the addon renders a small angle-right glyph (from the active theme's icon font) next to parents — see the [Subitem indicator CSS custom properties](#subitem-indicator) below for how to restyle it.

Turning `setChildrenOnlyInPopover(false)` restores Vaadin's auto-expanded inline tree for the active route.

This setting overrides `PopoverOn`: while it is enabled, the hover popover is shown on root items with children in both rail mode and normal mode, regardless of the configured `PopoverOn` value (the popover is the only path to the children, so gating it would hide them).

### Rail-mode tooltip

Because rail mode shows only icons, users may not be able to tell what each icon represents. The rail-mode tooltip surfaces each root item's label on hover or keyboard focus. Tooltips apply to the root items of the rail only.

`RailTooltipMode` selects how root items surface their label:

- `NONE` — no tooltips.
- `SIMPLE` — a small theme-styled tooltip drawn purely in CSS. Shows on hover and on keyboard focus, and stays visible even when a popover is open.
- `POPOVER_HEADER` (default) — uses a Vaadin `Popover` (with the configured `PopoverHeaderMode` as its header) as the tooltip — see `PopoverHeaderMode` for the per-item content options.

```java
rail.setRailTooltipMode(RailTooltipMode.NONE);  // disable tooltips entirely
```

> Why not use Vaadin's native tooltip? It dismisses itself whenever a popover opens ([vaadin/web-components#9768](https://github.com/vaadin/web-components/issues/9768)). `SIMPLE` is drawn in plain CSS instead, so it isn't affected and stays put next to an open popover.

`POPOVER_HEADER` reuses the configured Vaadin `Popover`'s header as a theme-styled tooltip. The header content is whatever `PopoverHeaderMode` produces, so the two knobs are paired:

```java
rail.setRailTooltipMode(RailTooltipMode.POPOVER_HEADER);
rail.setPopoverHeaderMode(PopoverHeaderMode.LABEL_ONLY);  // or ICON_ONLY / FULL
```

- **A header is required.** `POPOVER_HEADER` needs something to show, so it only works with a `PopoverHeaderMode` other than `NONE`. The default (`LABEL_ONLY`) is fine; but if you set `PopoverHeaderMode.NONE` while `POPOVER_HEADER` is active, the addon automatically uses `LABEL_ONLY` instead. If you change these values later at runtime, it's up to you to keep the combination valid.
- **`ICON_ONLY` needs a real icon.** The auto-coercion above only covers the `NONE` case. `ICON_ONLY` on an [icon-less root](#items-without-an-icon) still produces an empty header, so pair `ICON_ONLY` only with roots that have their own icon.
- **One popover or two.** With `POPOVER_HEADER`, a root item that already has a children popover reuses it as the tooltip — so there's just one popover per item; a leaf root (no children) gets a header-only popover as its tooltip. With `SIMPLE`, a parent item shows two things (its tooltip *and* its children popover); a leaf item only ever shows the tooltip.

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

By default, Vaadin's `<vaadin-side-nav-item>` only flags an item as `current` when its own path matches the URL — so when a deeply nested route is active (e.g. `admin/users/active`), the rail-side root icon (Admin) does not pick up the active marker. Vaadin's own `setMatchNested(true)` flips this for an individual item, but you'd have to call it on every root manually and decide when to toggle it back.

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

Styling is split into three layers: **style properties** (CSS custom properties) for tokens (colors, sizes, durations), **CSS selectors** for structural overrides, and **recipes** for common visual patterns. Every custom property resolves through a fallback chain that ends in the active theme's token, so the addon needs no CSS in a stock app and renders correctly under both **Lumo** (Vaadin 24 default) and **Aura** (Vaadin 25 default). Set them on `vaadin-side-nav` (or globally on `html`) to override.

### Style properties

The **Default** column below lists what each property resolves to in a Vaadin 24 app — a Lumo token in most cases, or a literal value where shown (e.g. `200ms`, `ease-out`). Where a default is a Lumo token, the same property resolves to the equivalent Aura token via the fallback chain under Aura (Vaadin 25), so the defaults stay theme-appropriate without any override. An Aura value is called out explicitly only where it differs from that straight token-equivalent.

#### Rail

| Property | Description | Default |
| --- | --- | --- |
| `--side-nav-rail-width` | Width of the rail in rail mode. | `var(--lumo-size-l)` |
| `--side-nav-rail-transition-duration` | Duration of the rail-mode collapse/expand animation. Set to `0s` to disable. | `200ms` |
| `--side-nav-rail-transition-easing` | Easing function for the same animation. | `ease-out` |

#### Subitem indicator

The visual cue that signals an item has children. Rendered as a `::before` pseudo-element on parent items. Shown in rail mode (always, for parents) and in normal mode when `setChildrenOnlyInPopover(true)` is on.

| Property | Description | Default |
| --- | --- | --- |
| `--side-nav-rail-subitem-indicator-content` | Glyph; any value valid for CSS `content` (e.g. another Lumo icon, an emoji, a string). | `var(--lumo-icons-angle-right)` |
| `--side-nav-rail-subitem-indicator-color` | Color of the indicator. | `var(--lumo-tertiary-text-color)` |
| `--side-nav-rail-subitem-indicator-size` | Indicator size in normal mode (rendered next to the label). | `var(--lumo-font-size-m)` (Lumo) / `var(--aura-font-size-l)` (Aura) |
| `--side-nav-rail-subitem-indicator-rail-size` | Indicator size in rail mode (rendered as a corner badge). | `0.625rem` (Lumo) / `var(--aura-font-size-m)` (Aura) |

#### Rail-mode tooltip

These properties style the CSS tooltip used by `RailTooltipMode.SIMPLE` (drawn as the `vaadin-side-nav-item[data-rail-tooltip]::after` pseudo-element). The default `POPOVER_HEADER` tooltip is a Vaadin popover instead — style it through the [Popover header selectors](#popover-header-opt-in-via-setpopoverheadermode). Note that `--side-nav-rail-tooltip-hover-delay` below is the `SIMPLE` tooltip's delay and is separate from `setPopoverHoverDelay` (which times the children popover).

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

Because the active-item look is themed differently in Lumo and Aura — Lumo uses a filled background with primary-colored text, while Aura can additionally draw a border — there is no single override that covers both, so the addon deliberately does **not** bake in a default. Tone the root down using each theme's own hooks, targeting the "ancestor-only" selector from the table above.

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

Those are just the common three; the full set (`--vaadin-side-nav-item-font-size`, `-padding`, `-border-radius`, …) is in the [Vaadin SideNav styling reference](https://vaadin.com/docs/latest/components/side-nav/styling). What "de-emphasized" should mean (transparent vs. a subtle outline vs. an icon-only tint) is an app-and-theme decision, which is why this lives here as a recipe rather than a fixed default.

`:has()` is supported in all browsers Vaadin 24 targets (Chrome 105+, Safari 15.4+, Firefox 121+).

### Further reading

- [Vaadin SideNav — styling reference](https://vaadin.com/docs/latest/components/side-nav/styling) — the parts, slots and properties of the underlying component, all of which apply here too.
- [Vaadin Popover — styling reference](https://vaadin.com/docs/latest/components/popover/styling) — for styling the overlay that hosts subitems.
- [Vaadin Avatar — styling reference](https://vaadin.com/docs/latest/components/avatar/styling) — for styling the letter-avatar fallback.

## Scope and known gaps

- **Dark mode**: supported automatically through the active theme (Lumo or Aura) — no extra wiring needed.
- **RTL layouts**: not currently validated. The rail uses logical CSS properties where possible, but RTL has not been exercised end-to-end.
- **Touch / mobile**: out of scope. The rail keeps its desktop hover-popover behaviour on touch devices. Since touch has no hover, the children popover may not open from a tap, so a collapsed parent's children can be hard to reach in rail mode — if you target small screens, plan to expose navigation another way (e.g. switch out of rail mode) rather than relying on the popover.
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
