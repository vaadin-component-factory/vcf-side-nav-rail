# Tooltip / Popover Merge — Design

**Status:** Draft
**Date:** 2026-04-29
**Branch:** `feat/tooltip-popover-merge`

## Motivation

Today the SideNav Rail addon offers two largely independent hover affordances on rail-mode root items:

1. **Popover** — opens for items *with* children, optionally rendering a header that identifies the parent (`PopoverParentLabelMode`).
2. **Tooltip** — opens for root items, identifying them by label (`RailTooltipMode` × `setRailTooltipNative`).

Combining them gives the user a single visual mechanism: every hover surfaces a popover-styled affordance — just with a header for leaves and a header + nested children for parents. This eliminates the awkward overlap (tooltip + popover both opening on the same item, mitigated upstream by `vaadin-tooltip-mixin` but still a visual blip) and gives users a fully Vaadin-themed, consistent hover treatment without the Lumo-vs-pseudo-element split.

## Public API changes

### `RailTooltipMode` — redefined

```
NONE            // no tooltips in rail mode
BROWSER_NATIVE  // browser-native title attribute
STYLED          // CSS pseudo-element, Lumo-themed (the current default)  -- new default
POPOVER         // tooltip rendered as a Popover with header
```

Removed: `ONLY_WITHOUT_CHILDREN`, `ALL`. The new semantics is "tooltip applies to every rail-mode root item, except in `NONE`". The previous distinction between "items with children get no tooltip" and "all items get a tooltip" is gone — the addon no longer pretends the tooltip and the popover are independent affordances. With `STYLED` or `BROWSER_NATIVE`, both can still appear simultaneously on parents (current behaviour preserved); with `POPOVER`, the parent's existing popover *is* the tooltip, so there is exactly one overlay.

### `setRailTooltipNative(boolean)` — removed

Replaced by `RailTooltipMode.BROWSER_NATIVE`. The boolean is gone from both the setter and the getter (`isRailTooltipNative()`).

### `PopoverParentLabelMode` → `PopoverHeaderMode`

Pure rename — same four enum values (`NONE`, `LABEL_ONLY`, `ICON_ONLY`, `FULL`). "Parent" is redundant in the popover context: the header always identifies the item that owns the popover, whether that's a parent or a leaf with `POPOVER` tooltip. "Header" matches Vaadin Popover's existing structural vocabulary (`setHeaderTitle`, header slot).

Setters and getters are renamed accordingly:

| Old | New |
|---|---|
| `setPopoverParentLabelMode(...)` | `setPopoverHeaderMode(...)` |
| `getPopoverParentLabelMode()` | `getPopoverHeaderMode()` |
| `setPopoverParentLabelOnlyInRailMode(boolean)` | `setPopoverHeaderOnlyInRailMode(boolean)` |
| `isPopoverParentLabelOnlyInRailMode()` | `isPopoverHeaderOnlyInRailMode()` |

Defaults are unchanged (`PopoverHeaderMode.NONE`, `popoverHeaderOnlyInRailMode = true`).

### `RailTooltipMode` default

The default changes from `RailTooltipMode.ALL` to `RailTooltipMode.STYLED`. Visually identical for users on the existing default (`ALL` + non-native = same Lumo-themed CSS pseudo-element on every root item in rail mode).

## Behaviour matrix

| Item | Rail mode | `RailTooltipMode` | Affordance |
|---|---|---|---|
| Root with children | yes | any | Parent popover (with header per `PopoverHeaderMode` × `OnlyInRailMode`). With `STYLED`/`BROWSER_NATIVE`, an additional tooltip is rendered alongside the popover (status quo). With `POPOVER`, the popover *is* the tooltip — only one overlay. |
| Root with children | no | any | No tooltip. Popover behaviour driven by `PopoverOn` and `setChildrenOnlyInPopover` as today. |
| Root without children (leaf) | yes | `NONE` | Nothing. |
| Root without children (leaf) | yes | `BROWSER_NATIVE` | `title="<label>"` attribute. Hover-only (browser-decided behaviour); does not surface on keyboard focus. |
| Root without children (leaf) | yes | `STYLED` | CSS pseudo-element tooltip on hover and `:focus-within`. Status quo. |
| Root without children (leaf) | yes | `POPOVER` | **New.** A Popover with the configured header opens on hover and on focus. Inherits hover/hide delays, position, and arrow visibility from the rail's existing popover settings. |
| Root without children (leaf) | no | any | Nothing — the label is fully visible. |
| Nested item | any | any | Never gets a rail-applied tooltip (status quo). |

## Auto-coerce: `POPOVER` + `PopoverHeaderMode.NONE`

If `RailTooltipMode == POPOVER` and `PopoverHeaderMode == NONE` are both set when the rail is attached, the rail silently coerces `PopoverHeaderMode` to `LABEL_ONLY`. Without a header the popover-as-tooltip would be empty.

The coercion happens at attach only — runtime setters are not validated. The rationale is that this combination is invalid only in `POPOVER` mode (`NONE` is the natural default for the other tooltip modes, where it has no effect on the popover header), and runtime ordering of setters can briefly produce the invalid intermediate state. Throwing at runtime would crash the demo when the user clicks the wrong dropdown first; a silent attach-time coerce keeps the demo robust without hiding the constraint — it is documented on `RailTooltipMode.POPOVER`'s JavaDoc and in the README. No log message is emitted.

The demo prevents the invalid combination from being chosen in the first place by disabling the conflicting select option, so the auto-coerce path is a safety net, not the primary UX.

After attach, if the user sets the combination via runtime setters, no coerce fires and a leaf popover with no content can result. This is treated as a user-error case (parallel to other "garbage in, garbage out" runtime states); the README covers the constraint explicitly.

## Edge cases

**Leaf items without an explicit prefix component.** Not a special case: rail-mode root items always carry *some* prefix component (the auto-generated letter avatar, see `SideNavRailItem.ensureLetterAvatar()`), so `PopoverHeaderMode.ICON_ONLY` always has content to render in rail mode.

**Header copy is the same component reference logic** the existing parent-popover header already uses — no new copy/clone path. Whatever component is the prefix on the item (icon, image, auto-letter-avatar) gets copied into the popover header.

**Position and arrow.** Leaf popovers use the rail's configured `popoverPosition` (default `END_TOP`) and `popoverArrowVisible` (default `true`). They share the rail's `popoverHoverDelay` / `popoverHideDelay`. No tooltip-specific delay or position knobs are introduced.

**Gating.** Leaf popovers are gated solely by `railTooltipMode == POPOVER && railMode`. They are independent of `PopoverOn` (which controls *parent* popover eligibility) and of `setChildrenOnlyInPopover` (which only matters when an item has children). This keeps the two surfaces — "popover for items with children" and "popover-as-tooltip for leaves" — orthogonal.

**Keyboard focus.** All three "active" tooltip modes surface on keyboard focus:

- `STYLED` — already does, via the `:focus-within` selector in `side-nav-rail.css`.
- `POPOVER` — inherited from `Popover.setOpenOnFocus(railMode)`, applied to leaf popovers by the same code path that handles parent popovers.
- `BROWSER_NATIVE` — does *not* surface on focus (browser limitation). Documented as the trade-off of choosing this mode.

## Implementation outline

**`SideNavRail.java`:**

- Field: `railTooltipNative` removed.
- Field: `railTooltipMode` default flips to `STYLED`.
- Field rename: `popoverParentLabelMode` → `popoverHeaderMode`; `popoverParentLabelOnlyInRailMode` → `popoverHeaderOnlyInRailMode`.
- `applyTooltipFor(SideNavItem)` switches on the four-value enum:
  - Wipes both tooltip attributes (`data-rail-tooltip`, `title`) on every entry, as today.
  - `STYLED` → set `data-rail-tooltip="<label>"`.
  - `BROWSER_NATIVE` → set `title="<label>"`.
  - `POPOVER` → no attribute; instead asks the leaf item to ensure its popover (see `SideNavRailItem` below).
  - `NONE` or `!railMode` → leave attributes wiped.
- New method `boolean isLeafPopoverActive()` returning `railMode && railTooltipMode == POPOVER`. Used by `SideNavRailItem.ensurePopover()` to gate the leaf-popover branch.
- Attach-time auto-coerce: in the existing attach-listener path that calls `updatePopoverGating()` and `applyTooltips()`, add `if (railTooltipMode == POPOVER && popoverHeaderMode == NONE) popoverHeaderMode = LABEL_ONLY;` before the apply calls.
- Setter `setRailTooltipMode` now also calls `forEachRootRailItem(item -> item.refreshPopoverFromOwner())` so leaf popovers are created/destroyed when the mode flips at runtime.

**`SideNavRailItem.java`:**

- `ensurePopover()`: the early-return guard becomes `if (popover != null) return; if (getItems().isEmpty() && !ownerWantsLeafPopover()) return;` — where `ownerWantsLeafPopover()` checks `owner.isLeafPopoverActive()`.
- `populatePopover()`: when `getItems().isEmpty()`, render only the header (driven by `PopoverHeaderMode`); skip the nested `SideNav`. Otherwise render header + nested SideNav as today.
- Rail-mode toggle path (already in place via `refreshPopoverFromOwner()` and `applyPopoverGating()`): when leaving rail mode while in `POPOVER` tooltip mode, leaf popovers must be closed *and* their hover trigger disabled. The simplest implementation is to keep the leaf popover instance but call `popover.setOpenOnHover(false)` + `popover.close()` — same gating mechanism the parent popovers already use.

**Frontend (`side-nav-rail.js`, `side-nav-rail.css`):** no changes. Existing V24/V25 popover abstractions cover leaf popovers without modification.

## Breaking changes (pre-1.0)

The addon is at `1.0.0-SNAPSHOT` and not yet published to the Vaadin Directory, so source-incompatible API changes are acceptable. There is no deprecation cycle.

1. `RailTooltipMode.ONLY_WITHOUT_CHILDREN` and `RailTooltipMode.ALL` removed. Migration: pick `STYLED` (default), `BROWSER_NATIVE`, `POPOVER`, or `NONE`.
2. `setRailTooltipNative(boolean)` / `isRailTooltipNative()` removed. Migration: `setRailTooltipMode(RailTooltipMode.BROWSER_NATIVE)`.
3. `PopoverParentLabelMode` class renamed to `PopoverHeaderMode`. Setters/getters renamed accordingly. Migration: rename references; enum values are unchanged.

For users on the previous default (no explicit calls), behaviour is visually unchanged — the new `STYLED` default on every root item matches the previous `ALL` + non-native default.

## Test plan

**Unit (`addon/src/test/.../SideNavRailTest.java`, `SideNavRailItemTest.java`):**

- Rewrite existing `RailTooltipMode` tests against the new four values.
- Cover the attach-time auto-coerce (`POPOVER` + `NONE` ⇒ `LABEL_ONLY` after attach; explicit `LABEL_ONLY`/`ICON_ONLY`/`FULL` left untouched).
- Cover leaf-popover lifecycle: created on rail-mode entry when `POPOVER`, destroyed (or hover-disabled + closed) on rail-mode exit, no leaf popover in non-`POPOVER` modes.
- Cover header content for each `PopoverHeaderMode` on a leaf.
- Cover that nested items still get no rail-applied tooltip / leaf popover.

**E2E (`e2e/src/test/playwright/`):**

- New spec: `popover-tooltip.spec.ts` (or extend `tooltip.spec.ts` if it exists) covering `POPOVER` mode on a leaf — opens on hover, opens on focus (Tab into the rail), closes on pointer-out, no leaf popover in normal mode.
- Update existing tooltip specs to use the new enum values.
- Verify cross-version (V24 / V25) via the existing `test-v24.sh` / `test-v25.sh` runners — leaf popovers go through the same `closestPopoverScope`/`popoverScopeTarget` JS abstraction as parent popovers, so the existing `popover.ts` helpers should suffice.

**Demo (`demo/.../ScreenshotView` and main demo view):**

- Replace the "native tooltip" toggle with a `RailTooltipMode` select (NONE / BROWSER_NATIVE / STYLED / POPOVER).
- Replace the existing `PopoverParentLabelMode` select binding with the renamed `PopoverHeaderMode`.
- Add a value-change listener that disables the `NONE` option on the `PopoverHeaderMode` select while `POPOVER` is selected (or, equivalently, snaps it to `LABEL_ONLY` and visually marks the snap). Pick whichever fits the existing demo pattern best at implementation time.

## Documentation impact

- `README.md` — update the "Public API surface highlights" block: new `RailTooltipMode` values, drop `setRailTooltipNative`, rename `PopoverParentLabelMode`, document the auto-coerce.
- JavaDoc on `RailTooltipMode` (rewritten) and `PopoverHeaderMode` (renamed; doc text updated to reflect both leaf and parent header use).
- `CLAUDE.md` — adjust the "Public API surface highlights" snapshot.
- `specs/2026-04-21-side-nav-rail-design.md` — record the API evolution as a follow-up section so the historical spec stays coherent with the current code.

## Out of scope

- Tooltip-specific delay / position / arrow knobs separate from the popover settings. Sharing keeps the API small; introduce only if a real user need surfaces.
- Migration shims / `@Deprecated` annotations. The addon is pre-publish; flat rename is cheaper than a deprecation cycle and avoids confusing future readers.
- Changes to nested-item tooltip behaviour (still none; rail-applied tooltips remain a root-only concept).
