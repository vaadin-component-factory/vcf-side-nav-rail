# Aura theme support for SideNav Rail (V25 cross-version styling)

**Status:** analysis complete + verified, implementation NOT started.
**Date:** 2026-07-09

## Problem (root cause, verified)

The addon's `addon/.../frontend/side-nav-rail.css` styles everything with `--lumo-*`
tokens (36 usages, 19 distinct). It sets no `@Theme`. On **V24** the framework default
is **Lumo** → tokens resolve. On **V25** the default is **Aura** → **all `--lumo-*` are
undefined**, so the addon's own CSS falls back to browser defaults (no padding, no border,
no colors, chevron gone). The failing E2E test
`rail-tooltip-popover.spec.ts:130` (popover-header `border-bottom` = `0px`) is just the one
spot a test asserts; the degradation is stylesheet-wide.

Verified live: on V25 `getComputedStyle(html).getPropertyValue('--lumo-*')` → `""` for every
token (at `html`, `body`, rail item, popover). The `:has(+ vaadin-side-nav)` selector itself
matches fine — the border collapses only because `var(--lumo-contrast-10pct)` is empty, which
makes the whole `border-bottom` shorthand invalid → `border-style:none` → `0px`.

## Fix strategy

Make the stylesheet theme-agnostic via a fallback chain, so V24 stays **pixel-identical**
(Lumo wins when present) and V25/Aura gets correct values:

```
var(--lumo-X, var(--vaadin-Y, HARD))         # generic base token exists
var(--lumo-X, var(--aura-Z, HARD))           # only an Aura-specific token exists
var(--lumo-X, HARD)                          # no generic token at all
```

- V24: `--lumo-*` present → used. (`--vaadin-*`/`--aura-*` are empty on V24 — verified.)
- V25 Aura: `--lumo-*` empty → `--vaadin-*`/`--aura-*` used.
- V25 non-Aura / neither present: HARD fallback.

**HARD-fallback policy:** always the **Lumo value** (known-good baseline, consistent with the
V24 design). It is only ever reached on V25 with a theme that is *neither Lumo nor Aura*
(e.g. a base-only custom theme) — under Lumo/Aura the earlier links in the chain win. Note this
means fonts/spacing already differ V24↔V25-Aura by design (Lumo vs Aura native scales); the hard
value does not affect those two mainline cases.

**Typography specifically:** base styles have **no** `--vaadin-*` font-size / line-height token,
so the chain is `var(--lumo-…, var(--aura-…, <lumo-hard>))` (Aura tier, not Vaadin-base tier).

## Overridability principle (API) — required

Every touched declaration must expose an **addon-owned `--side-nav-rail-*` custom property**
whose *default* is the fallback chain, so consumers can override cleanly from outside without
`!important` or reaching into internals:

```
prop: var(--side-nav-rail-XXX, var(--lumo-…, var(--aura-…/--vaadin-…, <hard>)));
```

This especially matters for the Group C / Aura-only tokens (no generic Vaadin var): the combined
`--lumo-…/--aura-…` chain is fine **as long as it lives inside an overridable `--side-nav-rail-*`
var**. Some usages already follow this (`--side-nav-rail-width`, `--side-nav-rail-subitem-indicator-*`,
`--side-nav-rail-tooltip-*`) → just deepen their inner fallback. The following are currently **raw
inline** and must get a NEW exposed var when touched:

| CSS line(s) | new custom property | default = chain for |
|---|---|---|
| 182 (gap) | `--side-nav-rail-popover-header-gap` | `--lumo-space-s` |
| 183 (padding) | `--side-nav-rail-popover-header-padding` | `--lumo-space-s --lumo-space-m` |
| 184 (color) | `--side-nav-rail-popover-header-color` | `--lumo-secondary-text-color` |
| 186 (font-size) | `--side-nav-rail-popover-header-font-size` | `--lumo-font-size-s` |
| 187 (line-height) | `--side-nav-rail-popover-header-line-height` | `--lumo-line-height-s` |
| 196 (border-bottom) | `--side-nav-rail-popover-header-border-color` | `--lumo-contrast-10pct` |
| 200,201 (icon size) | `--side-nav-rail-popover-header-icon-size` | `--lumo-icon-size-s` |
| 202 (icon color) | `--side-nav-rail-popover-header-icon-color` | `--lumo-secondary-text-color` |
| 227 (avatar letter) | `--side-nav-rail-letter-avatar-font-size` | `--lumo-font-size-l` |
| 239 (avatar current) | `--side-nav-rail-letter-avatar-current-color` | `--lumo-primary-color` |

Internal layout offsets (154, 271, 272 — `right`/`left`/`top`) stay inline (not part of the public
styling API); just deepen their fallback to include the `--vaadin-*` tier. New public vars should be
documented in the CSS custom-property comment blocks (like the existing `--side-nav-rail-tooltip-*` list).

## Verified values (live-measured)

| Lumo token | Lumo value (V24) | Aura equivalent (V25) | Aura value (V25) |
|---|---|---|---|
| `--lumo-contrast-10pct` | `hsla(214,57%,24%,.1)` | `--vaadin-border-color-secondary` | resolves (light-dark, low-contrast) |
| `--lumo-contrast-90pct` | `hsla(214,40%,16%,.94)` | `--vaadin-text-color` | resolves (high-contrast) |
| `--lumo-secondary-text-color` | `hsla(214,42%,18%,.69)` | `--vaadin-text-color-secondary` | resolves |
| `--lumo-tertiary-text-color` | `hsla(214,45%,20%,.52)` | `--vaadin-text-color-secondary` (approx) | resolves |
| `--lumo-base-color` | `#fff` | `--vaadin-background-color` | resolves |
| `--lumo-primary-color` | `hsl(214,100%,48%)` | `--aura-accent-color` / `--vaadin-focus-ring-color` | `oklch(.55 .2 264)` |
| `--lumo-space-xs` | `0.25rem` (4px) | `--vaadin-gap-xs`/`--vaadin-padding-xs` | 4px |
| `--lumo-space-s` | `0.5rem` (8px) | `--vaadin-gap-s`/`--vaadin-padding-s` | 8px |
| `--lumo-space-m` | `1rem` (16px) | `--vaadin-padding-m`/`--vaadin-gap-m` | **12px** (≠ Lumo 16px) |
| `--lumo-font-size-s` | `0.875rem` (14px) | `--aura-font-size-s` | ~13px |
| `--lumo-font-size-m` | `1rem` (16px) | `--aura-font-size-m` | 14px (≠ Lumo 16px) |
| `--lumo-font-size-l` | `1.125rem` (18px) | `--aura-font-size-l` | ~16px |
| `--lumo-line-height-s` | `1.375` (ratio) | `--aura-line-height-s` | **18px** (length, not ratio) |
| `--lumo-line-height-xs` | `1.25` (ratio) | `--aura-line-height-xs` | 16px (length) |
| `--lumo-icon-size-s` | `1.25em` | — (`--vaadin-icon-size` is **empty** on Aura) | ❌ none |
| `--lumo-icon-size-m` | `1.5em` | — (`--vaadin-icon-size` empty) | ❌ none |
| `--lumo-size-l` | `2.75rem` (44px) | — (no `--vaadin`/`--aura` size token) | ❌ none |
| `--lumo-border-radius-s` | `0.25em` | `--vaadin-radius-s` | ~5px |
| `--lumo-icons-angle-right` (+`font-family:"lumo-icons"`) | Lumo icon glyph | — (font + glyph absent on Aura) | ❌ none → rework |

**Caveats the verification surfaced (docs alone would have been wrong):**
- `--vaadin-icon-size` is documented in base styles but resolves to **empty** on the Aura default.
- Aura's spacing scale differs from Lumo (`padding/gap-m` = 12px vs Lumo `space-m` = 16px).
- Font-size / line-height exist only as `--aura-*` (Aura-only), **not** `--vaadin-*` base;
  Aura line-heights are **lengths (px)**, Lumo are **unitless ratios**.

## Concrete replacement per token (copy-ready)

Group A — clean generic equivalent:
```
--lumo-contrast-10pct        → var(--lumo-contrast-10pct, var(--vaadin-border-color-secondary, rgba(0,0,0,.1)))
--lumo-contrast-90pct        → var(--lumo-contrast-90pct, var(--vaadin-text-color, #1f2933))
--lumo-secondary-text-color  → var(--lumo-secondary-text-color, var(--vaadin-text-color-secondary, rgba(0,0,0,.62)))
--lumo-base-color            → var(--lumo-base-color, var(--vaadin-background-color, #fff))
--lumo-border-radius-s       → var(--lumo-border-radius-s, var(--vaadin-radius-s, 0.25em))
```

Group B — approximate / Aura-specific:
```
--lumo-tertiary-text-color   → var(--lumo-tertiary-text-color, var(--vaadin-text-color-secondary, rgba(0,0,0,.5)))
--lumo-primary-color         → var(--lumo-primary-color, var(--aura-accent-color, var(--vaadin-focus-ring-color, #1f6fff)))
--lumo-space-xs              → var(--lumo-space-xs, var(--vaadin-gap-xs, 0.25rem))      # 4px both
--lumo-space-s               → var(--lumo-space-s, var(--vaadin-gap-s, 0.5rem))         # 8px both
--lumo-space-m  (padding)    → var(--lumo-space-m, var(--vaadin-padding-m, 1rem))       # Aura=12px, hard=16px
--lumo-font-size-s           → var(--lumo-font-size-s, var(--aura-font-size-s, 0.875rem))    # hard = Lumo value
--lumo-font-size-m           → var(--lumo-font-size-m, var(--aura-font-size-m, 1rem))        # hard = Lumo value
--lumo-font-size-l           → var(--lumo-font-size-l, var(--aura-font-size-l, 1.125rem))    # hard = Lumo value
--lumo-line-height-s         → var(--lumo-line-height-s, var(--aura-line-height-s, 1.375))   # hard = Lumo value
--lumo-line-height-xs        → var(--lumo-line-height-xs, var(--aura-line-height-xs, 1.25))  # hard = Lumo value
```
> Note (B): `--vaadin-gap-*` for gaps/positioning, `--vaadin-padding-*` for padding — on Aura
> both are 4/8/12px, so interchangeable in value. The `space-m`/`font-size-m` Aura values are
> intentionally tighter than Lumo (Aura-native); accept as the deliberate V25 visual delta.

Group C — no generic token → hard fallback / rework:
```
--lumo-icon-size-s           → var(--lumo-icon-size-s, var(--vaadin-icon-size, 1.25em))   # vaadin-icon-size empty today; harmless future-proof
--lumo-icon-size-m           → var(--lumo-icon-size-m, var(--vaadin-icon-size, 1.5em))
--lumo-size-l  (rail width)  → var(--lumo-size-l, 2.75rem)
```
Chevron indicator (2 rules: `:not([theme~="rail"]) …::before` line ~149 and rail `::before` line ~162):
```
content:     var(--side-nav-rail-subitem-indicator-content, var(--lumo-icons-angle-right, "\203A"));
font-family: "lumo-icons", sans-serif;
```
> Why this works on both: V24 → `--lumo-icons-angle-right` glyph rendered in the loaded
> "lumo-icons" font. Aura → var empty → content becomes `"\203A"` (›), and since "lumo-icons"
> isn't loaded the browser per-glyph-falls-back to `sans-serif`, so "›" renders. Preserves V24
> exactly; gives a real chevron on Aura instead of nothing.

Already-safe (leave as-is, already have hard fallbacks): the `var(--lumo-space-s, 0.5rem)`
absolute offsets (lines ~154, 271, 272), `var(--lumo-space-xs, 0.25rem)` (279),
`var(--lumo-border-radius-s, 0.25em)` (280), `var(--lumo-line-height-xs, 1.2)` (282). These
already fall back on Aura — but bump their generic token in too for consistency if touching them.

## Affected CSS lines (side-nav-rail.css)

27 (size-l) · 149,150,151,152 + 162,163,165 (chevron rules) · 182,183,184,186,187 (popover header)
· 196 (border-bottom — the test) · 200,201,202 (header icon) · 227 (rail current font) · 239 (primary)
· 277,278,281 (rail tooltip). Doc-comment default listings (134–136, 254–258) should be updated to
name the new chains for accuracy (non-functional).

## Verification plan (after implementing)

1. `./test-v24.sh` — must stay green AND visually identical (Lumo path unchanged).
2. `JAVA_HOME=/usr/lib/jvm/temurin-21-jdk-amd64 PATH=/home/node/node24/bin:$PATH ./test-v25.sh`
   — `rail-tooltip-popover.spec.ts:130` must pass; full suite green.
3. Screenshot both versions (rail mode: popover header w/ border, rail tooltip, chevron indicator,
   letter-avatar) to confirm the Aura look is acceptable.

## Test-env note (see also memory `v25-test-environment`)

V25 run needs JDK 21 (`/usr/lib/jvm/temurin-21-jdk-amd64`, installed via Adoptium apt) + Node 24 on
PATH (`/home/node/node24`, copied out of the read-only `~/.vaadin` mount because Vaadin can't
download its pinned Node into that read-only dir).
