# Follow-ups

Tracker for known polish items that surfaced during phase 9.5 (publishing) and should be addressed before the first `1.0.0` release — or in a `1.0.x` patch right after.

## Visual polish

- **Item label alignment.** Labels next to the icon column appear slightly off-baseline in normal mode (visible in `docs/screenshots/1-modes.png`, `3-children.png`, `5-active-root.png`, `6-active-deep.png`). Likely a vertical-alignment or line-height interaction between the icon container and the label `<span>`. Goal: pixel-perfect baseline alignment between icon center and label text.

- **Rail icon styling when an ancestor is current.** When a sub-sub item is the current route (e.g. `admin/users/active`), the rail-side root icon (Admin) currently inherits the full `current` styling — same blue + light-blue background as if Admin itself were the current page. The intent is a subtler "you are deep in this branch" indicator: e.g. only the icon coloured, no background highlight, or a smaller accent line. Today that's either the addon's CSS or Vaadin's stock side-nav `current` styling — we need to override per-mode without breaking the leaf's own `current` look. Visible in `docs/screenshots/6-active-deep.png` (the rail Admin icon looks identical to the popover's current Active leaf).
