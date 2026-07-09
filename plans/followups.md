# Follow-ups

Tracker for known polish items that surfaced during phase 9.5 (publishing) and should be addressed before the first `1.0.0` release — or in a `1.0.x` patch right after.

## Visual polish

- **Item label alignment.** Labels next to the icon column appear slightly off-baseline in normal mode (visible in `docs/screenshots/1-modes.png`, `3-children.png`, `5-active-root.png`, `6-active-deep.png`). Likely a vertical-alignment or line-height interaction between the icon container and the label `<span>`. Goal: pixel-perfect baseline alignment between icon center and label text.

- **Rail icon styling when an ancestor is current.** When a sub-sub item is the current route (e.g. `admin/users/active`), the rail-side root icon (Admin) currently inherits the full `current` styling — same blue + light-blue background as if Admin itself were the current page. The intent is a subtler "you are deep in this branch" indicator: e.g. only the icon coloured, no background highlight, or a smaller accent line. Today that's either the addon's CSS or Vaadin's stock side-nav `current` styling — we need to override per-mode without breaking the leaf's own `current` look. Visible in `docs/screenshots/6-active-deep.png` (the rail Admin icon looks identical to the popover's current Active leaf).

## Cross-version theming

- **Aura theme support (V25).** The stylesheet uses `--lumo-*` tokens throughout (19 distinct). On V25 the default theme is Aura, where `--lumo-*` are undefined → the addon's own CSS is effectively unstyled (the failing V25 E2E `rail-tooltip-popover.spec.ts:130` border-bottom test is the visible symptom). Full verified analysis + copy-ready per-token replacement (`var(--lumo-…, var(--vaadin-…/--aura-…, <hard>))`) and the chevron-indicator rework are written up in [`plans/2026-07-09-aura-theme-support.md`](2026-07-09-aura-theme-support.md). Implementation not started.

## E2E test hardening (do right AFTER the Aura/theming rework)

Context: the cross-version E2E suite currently couples to Vaadin's **own** `vaadin-popover` internals (overlay location, `role=menu` placement, `positionTarget`/`target`), abstracted behind the dual-form selectors in `e2e/src/test/playwright/lib/popover.ts`. That coupling is the price of E2E-asserting on a framework component that restructured between V24 and V25 — not sloppy test authoring — but a chunk of it is avoidable by giving the addon its own stable, version-independent test seams. Sequenced after the theming rework because that work already operates at the popover boundary.

- **Move behavioural assertions onto addon-owned hooks.** Where a test's real intent is *our* component's behaviour (not framework integration), assert on state the addon itself owns and reflects — existing classes (`.side-nav-rail-popover-header`), attributes (`[root-item]`), ARIA we set — instead of Vaadin's overlay subtree. Candidate: reflect a stable attribute on the rail `SideNavRailItem` when its popover is open (e.g. `data-rail-popover-open`), so "is the popover for item X open?" no longer needs `vaadin-popover-overlay[opened]`. This is partly an **addon** change (add the seam), not only a test change.
- **Quarantine the genuinely framework-dependent tests.** Keep a small, explicitly version-aware set of "popover integration" tests (does an overlay actually open, is `role=menu` present, is slotted content inside it) that legitimately touch `vaadin-popover`. Everything else should stop depending on its internal shape. Goal: shrink `lib/popover.ts` to that small surface.
- **`e2e-v24` split — verdict: not for the crutches.** A second version-pinned E2E module (mirroring `demo`/`demo-v24`) does **not** remove the popover coupling unless specs are forked (which doubles ~113 tests and their maintenance). Its only real win is build robustness: it would retire the `-Dvaadin.version` flip + frontend-wipe dance in `test.sh` and let one `mvn -Pe2e verify` cover both versions. Reconsider later purely as CI hygiene, and only with **shared** specs — after the hook migration above, since that is what actually reduces the pain.
