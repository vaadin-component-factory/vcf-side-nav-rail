# Active view item lookup — Implementation Plan

**Date:** 2026-04-30
**Status:** Spec'd, not implemented
**Spec:** [`specs/2026-04-21-side-nav-rail-design.md` §11](../specs/2026-04-21-side-nav-rail-design.md#11-active-view-item-lookup-post-95)

## Goal

Expose `SideNavRail#getActiveViewItem()` and `SideNavRail#getActiveViewItems()`
so application code can obtain the `SideNavRailItem` matching the currently
displayed view without re-implementing route matching. Path-equality only —
`matchNested` is intentionally ignored (a parent is not "active" because a
descendant matches). Aliases are honored.

## Background — design dialog (2026-04-30)

Captured here so the rationale survives the implementation step.

| Question | Decision |
|---|---|
| Match `matchNested` parents? | No — exact path or alias match only |
| Match path aliases? | Yes — keeps server answer aligned with the visual `[active]` highlight |
| Match query params / route templates? | No (V1) — simple string equality of resolved paths |
| Single-match return type | `Optional<SideNavRailItem>`; first DFS pre-order match on collisions |
| Multi-match return type | `List<SideNavRailItem>` (DFS pre-order) |
| Location source | `UI.getCurrent().getInternals().getActiveViewLocation()` — internal but stable on V24+V25 |
| Active-change listener | Out of scope for V1; future addition non-breaking |
| Tree scope | Real items only (popover clones excluded — they live in a separate nested `SideNav`) |

## Implementation tasks

- [ ] **Locate active location helper.** A private `currentLocationPath()`
      reading `UI.getCurrent().getInternals().getActiveViewLocation().getPath()`,
      tolerant of `null` UI (returns `Optional.empty()` style — caller short-
      circuits to empty list).
- [ ] **DFS walker over real items.** Reuse the existing
      `forEachRailItemRecursive` helper if it already supports collecting
      results; otherwise add a sibling collector.
- [ ] **Match predicate.** `matches(item, path)` returns true if
      `path.equals(item.getPath())` or `item.getPathAliases().contains(path)`.
      Pre-trim trailing slashes on both sides to align with Vaadin's
      `sanitizePath` behaviour.
- [ ] **Public API.** Two methods on `SideNavRail`:
      ```java
      public List<SideNavRailItem> getActiveViewItems();
      public Optional<SideNavRailItem> getActiveViewItem();
      ```
      Both compute on demand — no caching in V1.
- [ ] **Javadoc.** Document path-equality-only semantics, alias behaviour,
      multi-match behaviour, and the `matchNested`-ignored caveat.
- [ ] **Unit tests** (`ActiveViewItemTest`):
   - empty list when no item matches
   - single match by `getPath()`
   - single match by alias
   - empty list when only a `matchNested` parent would have matched
     (descendant matches → list contains descendant only, not parent)
   - multi-match: two items wired to the same path → both in list,
     `getActiveViewItem()` returns the DFS-first
   - alias-vs-path collision: returns both
   - popover clones are not returned
   - `getActiveViewItems()` is empty when no UI / no active location
- [ ] **README.** New subsection under the configuration API documenting
      both methods and the `matchNested`-ignored caveat.
- [ ] **Demo.** Minimal hook in the demo content area: render the active
      item's label as a breadcrumb-style heading. Validates the API in real
      use without bloating the UI.

## Out of scope — captured for later

- `findItemsForLocation(Location)` overload.
- `addActiveItemChangeListener` hook backed by an internal
  `AfterNavigationListener`.
- Route-template / query-parameter matching.

## Verification

- `./mvnw -pl addon test` — unit tests green.
- `./mvnw clean verify` — full reactor green (no E2E coverage planned;
  unit tests cover the matching logic exhaustively).
