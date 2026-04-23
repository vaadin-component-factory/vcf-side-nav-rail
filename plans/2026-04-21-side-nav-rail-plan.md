# SideNav Rail Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan phase-by-phase. Each phase is its own file under [`side-nav-rail/`](./side-nav-rail/). Steps use checkbox (`- [ ]`) syntax for tracking.
>
> **For each subagent dispatch:** load *this index* plus *the single phase file* the task lives in — together they are self-contained.

**Goal:** Ship an MVP Vaadin 24 addon (`SideNavRail`) that extends `SideNav` with a togglable rail (icon-only) mode and a configurable hover popover for items with children. The addon sits in its own Maven module, the demo in a separate one, and the addon carries its own test pyramid (Karibu unit + Playwright E2E, all driven by `mvn verify`).

**Architecture:** Pure Java + CSS — no custom web component. `SideNavRail extends SideNav` sets a `theme="rail"` attribute that CSS uses to collapse labels and inline children. Items with children lazily attach a Vaadin `Popover` whose content is a secondary `SideNav` rendering the children. A `PopoverMode` enum picks when the popover is live (default: for every non-expanded item; alternative: only while the nav is in rail mode).

**Tech Stack:** Java 17, Maven (multi-module), Vaadin 24 (`vaadin-core` + `vaadin-spring-boot-starter`), Spring Boot for the test runtime, Karibu Testing v24 for unit tests, Playwright (TypeScript) driven by `frontend-maven-plugin` + `exec-maven-plugin` for E2E, Apache 2.0 license.

**Spec:** [`specs/2026-04-21-side-nav-rail-design.md`](../specs/2026-04-21-side-nav-rail-design.md) — authoritative for everything in this plan.

---

## Implementation status

> **Status (2026-04-23):** MVP + phase 2 + phase 9.1 complete (incl. post-§9.1 tooltip-implementation refactor), merged into `main`. Reactor `./mvnw clean verify` is green — **67/67** addon unit tests + **25/25** Playwright E2E tests pass.
>
> The spec has been updated in-place to reflect the final shape of the code. This plan document is kept as the historical record of *how* the work got there.
>
> **Phase 9.1 additions — user-facing polish (iterative, in progress on `phase9.1/user-facing-polish`):**
>
> - **`PopoverParentLabelMode` (opt-in popover header).** New enum on `SideNavRail`: `NONE` (default), `LABEL_ONLY`, `ICON_ONLY`, `FULL`. Renders a header `Div.side-nav-rail-popover-header` above the nested `SideNav` inside the popover. Graceful empty handling — if the chosen mode would produce an empty header (no icon for `ICON_ONLY`, blank label for `LABEL_ONLY`), the header is skipped. `setPopoverParentLabelMode(...)` rebuilds all existing popovers so the switch is visible immediately. Lumo-style CSS (secondary text color, subtle bottom border). Tests: 9 unit + 3 Playwright (the live-switch case is covered in Karibu only; reproducing it via Playwright proved brittle because clicking a mode button outside the rail moves the mouse off the hovered item and closes the hover-open popover). Demo navbar gains a second `Select` to switch modes at runtime. Spec §3.4 + §4.2 updated, §9.1 bullet crossed out.
> - **`[root-item]` attribute as an active-descendant styling hook.** Originally §9.1 asked for an "active/selected indicator on the icon when a descendant of a parent hidden in rail mode is active". Vaadin already provides the detection side (`setMatchNested(true)` → `[current]` propagates from descendant routes), and the visual treatment is app-specific — so the addon stops at exposing a target selector: every direct child of a `SideNavRail` gets the `root-item` attribute set by `SideNavRail.addItem` / `addItemAsFirst`. Consumers can then write `vaadin-side-nav-item[root-item]:has([current]) > vaadin-icon { ... }` (example in spec §5.1). Tests: 4 unit cases in `RootItemAttributeTest` (direct children carry the attribute, `addItemAsFirst` too, nested items don't, standalone items don't). No CSS, no demo change, no E2E — the attribute is verifiable in a unit test and the presentation is consumer-owned.
> - **Letter-avatar fallback for icon-less items.** §9.1 item 3: rail mode reduces a no-icon item to a blank tile. Solution: `SideNavRailItem` auto-generates a `vaadin-avatar` (`LUMO_SMALL`, 24×24) with the label's first letter (uppercase) as abbreviation whenever the prefix slot is empty and the label is non-blank. The avatar carries the marker class `side-nav-rail-letter-avatar`; CSS hides it in normal mode and shows it only in rail mode — matches the 24×24 Lumo icon size used by actual icons elsewhere in the nav. Initially built as a styled `<span>`, swapped for `vaadin-avatar` on feedback (default Lumo colors and sizing come for free, result looks cleaner). `setLabel` updates the letter, user-provided prefix replaces the avatar, clearing the prefix to null regenerates it, blank label = no avatar. Tests: 8 Karibu unit cases in `LetterAvatarFallbackTest` + 3 Playwright cases (hidden in normal, visible with correct letter in rail, real-icon items unaffected). Demo gains a root-level "Admin" item without an icon so the fallback is visible in the running app. Visual verification via ad-hoc Playwright screenshot (devcontainer isolation — user can't open localhost directly). Spec §3.2 + §5.2 updated, §9.1 bullet crossed out.
> - **`RailTooltipMode` — native tooltips on root items in rail mode.** §9.1 item 4. Originally scoped as "tooltip for items without children"; extended to a three-valued enum on the user's request (`NONE`, `ONLY_WITHOUT_CHILDREN`, `ALL` — default `ALL`). Implemented via `SideNavItem.setTooltipText(label)`; applied on `setRailMode` / `setRailTooltipMode` / `addItem`. `SideNavRailItem.setLabel` keeps an active tooltip text in sync. Default `ALL` because with `PopoverParentLabelMode.NONE` the parent label is otherwise never surfaced, and the tooltip's `BOTTOM` position doesn't collide with the popover at `END_TOP`. Known Vaadin quirk: a tooltip already open on one item that then moves to another item with a popover briefly flashes the new label before the popover dismisses the tooltip — documented on the enum's Javadoc, not fixable server-side. Tests: 10 Karibu unit cases in `RailTooltipModeTest` (default, all modes, live switch, label refresh, nested items excluded) + 5 Playwright cases using overlay-visibility assertions. **Stale-bundle note:** an initial run of the Playwright cases failed with `patchVirtualContainer is not a function` in the prod bundle; wiping `e2e/src/main/bundles/prod.bundle` (the Flow-Maven-Plugin's production-bundle cache) fixed it. The cache usually diffs correctly, but it can go stale across heavy rework — good to know for future phases. Demo navbar gains a third Select. Spec §3.5 (new section, §3.5 RailModeChangedEvent renumbered to §3.6), §9.1 bullet crossed out.
> - **Rail-mode transition animation.** §9.1 item 5. Pure CSS — rail width, label text, suffix, and toggle-button chevron now fade and collapse together over a single duration + easing. Configurable via two custom properties on `vaadin-side-nav`: `--side-nav-rail-transition-duration` (default `200ms`), `--side-nav-rail-transition-easing` (default `ease-out`). Set duration to `0s` to disable. No Java API. Inline children (`[slot="children"]`) still snap away with `display: none` — there's no reliable height to animate, and the popover surfaces them in rail mode anyway. Verified via ad-hoc Playwright screenshot at 100 ms into the toggle (mid-transition frame shows labels partially faded). Spec §5.2 updated, §9.1 bullet crossed out.
> - **Tooltip moved from `vaadin-tooltip` to a CSS pseudo-element.** Post-§9.1 refactor. While documenting the `RailTooltipMode.ALL` Vaadin by-design quirk (tooltip-mixin auto-dismisses on peer overlay open, [web-components#9768](https://github.com/vaadin/web-components/issues/9768)), we agreed that sidestepping the overlay system altogether is cleaner. `SideNavRail.applyTooltipFor` now writes a `data-rail-tooltip` DOM attribute; CSS renders a `::after` pseudo-element (positioned top-left above the item so it coexists with the popover), styled to mirror `vaadin-tooltip-overlay`. The rail itself is hoisted into its own stacking context (`z-index: 10000`) so the pseudo renders above body-level overlays. A new `setRailTooltipNative(boolean)` (default `false`) lets consumers switch to the browser-native `title` tooltip instead — useful for assistive-tech flows. `SideNavRailItem.setLabel` now refreshes whichever attribute is currently installed. Tests: `RailTooltipModeTest` converted to attribute assertions + 3 new cases covering native-flag default, switch-cleans-up-other-attribute, native-mode-uses-title. E2E spec reworked to poll the `data-rail-tooltip` attribute and the `::after` opacity on hover (Playwright can't query pseudo-elements directly, but computed-style reads work). Spec §3.5 + §5.2 rewritten around the pseudo-element approach.
> - **Configurable popover timings + position.** §9.1 item 6. Three new setters on `SideNavRail`: `setPopoverHoverDelay(int)`, `setPopoverHideDelay(int)`, `setPopoverPosition(PopoverPosition)`. Defaults unchanged (200 ms / 300 ms / `END_TOP`). `SideNavRailItem.ensurePopover()` now seeds from the owning rail's values at creation time; a helper `applyPopoverSettings(hoverDelay, hideDelay, position)` on the item pushes live changes to the existing popover. Walk is recursive so nested-item popovers get the same values. Tests: 5 Karibu unit cases in `PopoverSettingsTest` (defaults, null rejection, seed-at-create, live switch on root popover, live switch on nested). No demo change (3 more navbar selects would overload the UI), no E2E (API-level change; unit tests cover it). Spec §3.1 + §4.2 updated, §9.1 last bullet crossed out — **phase 9.1 now complete**.
>
> **Phase 2 additions (post-merge of MVP):**
>
> - **`PopoverMode` renamed + expanded to three values.** `COLLAPSED_ITEM` → `ALL_COLLAPSED_ITEMS`, `RAIL_ONLY` → `ONLY_RAIL_MODE`, **new** `ONLY_ROOT_COLLAPSED_ITEMS` that restricts popovers to direct children of the `SideNavRail` (nested parents never open a popover). Spec §3.3 and §4.1 carry the new behaviour matrix. Default remains `ALL_COLLAPSED_ITEMS`.
> - **`updatePopoverGating` made recursive.** With `ONLY_ROOT_COLLAPSED_ITEMS`, eligibility depends on tree position, so a non-recursive walk would leave nested popovers mis-gated after a mode switch. New helper `applyGatingRecursively(SideNavRailItem)` traverses the full item tree on every rail-mode/popover-mode change.
> - **Type-safe children.** `SideNavRail.addItem(...)` / `addItemAsFirst(...)` and `SideNavRailItem.addItem(...)` / `addItemAsFirst(...)` now reject plain `SideNavItem` instances with `IllegalArgumentException`. Rationale: label-wrap and popover gating are implemented on `SideNavRailItem` overrides and cannot be retrofitted onto a parent-class instance — a silent accept would produce a partially-broken item that nobody can debug. Enforced at runtime (the parent class's generic signature prevents a compile-time guard without reshaping the public API).
> - **Full Javadocs on every public method.** MVP shipped without Javadocs; this gap was closed in phase 2. Tone targets existing Vaadin developers — we explain *why* and *when*, not what a `Component` is.
> - **New tests.** `PopoverModeTest` grew three cases for `ONLY_ROOT_COLLAPSED_ITEMS` (root eligible, nested not eligible, close-on-mode-switch). New `TypeGuardTest` has 6 cases covering both entry points on both classes. New E2E spec `popover-only-root-collapsed-items.spec.ts` — root opens popover; nested parent does not. The E2E spec uses **semantic assertions** (`popover.getByText('Branches')` + `popover.getByText('Tags')`) rather than DOM count checks, which would break on hidden-but-in-DOM grandchildren.
> - **New test view.** `PopoverOnlyRootCollapsedItemsView` at `/only-root-collapsed-items` — "Code" as root with "Branches" (itself a parent with children) + "Tags" nested inside.
>
> **Key deviations from the plan as originally written (MVP phase):**
>
> **Key deviations from the plan as originally written:**
>
> - **Separate `e2e/` module.** The plan assumed the Spring Boot test runtime would live in `addon/src/test/java/.../app/`. In practice, `spring-boot-maven-plugin` + `classesDirectory` + `useTestClasspath` produced a tangle of workarounds that would not boot reliably. Phase 5 was effectively re-executed by moving TestApplication + test views + Playwright scaffold into a dedicated `e2e/` module where they live at compile scope. The addon POM is consequently much leaner (no Spring Boot test deps, no integration-test plugin chain). See commit `306a2a9`.
> - **Java 17, not 25.** Spring Boot 3.x's bundled ASM cannot parse Java 25 class files. The devcontainer Dockerfile and both POMs were changed to Java 17. See commit `27251c4`.
> - **Vaadin 24.10.1 + Spring Boot 3.5.13.** The plan's version pins (24.5.0 / 3.4.0) were placeholders; the final build uses the latest stable in each 24.x / 3.x line. See commit `4fb9bb4`.
> - **`TestMainLayout` removed.** The plan included a shared `@Layout` layout wrapping all test views. This caused duplicate `#toggle-rail` IDs when a view also declared its own toggle button; Playwright's strict mode rejected the ambiguous selectors. Resolved by removing the layout and giving `BasicTestView` its own rail + toggle.
> - **Popover attachment left to Vaadin.** Earlier revisions manually called `rail.getElement().appendChild(popover.getElement())`; this was cargo-culted from a mismatch between the plan's code and an early test. The real contract is that `Popover.setTarget(...)` installs an attach/detach listener on the target that auto-adds the popover to the UI and auto-removes it when the target detaches (see `Popover#onTargetAttach` + `removeFromUiIfAutoAdded`). Manually appending breaks that lifecycle — the `autoAddedToTheUi` flag is set while the popover sits under the rail, so the auto-remove can't find it on detach. Phase 2.1 deletes the manual append; `PopoverLifecycleTest` now pins the expected lifecycle (popover becomes a direct UI child; it detaches when its owning rail is removed from the UI). The overlay still teleports to `body`, so no visual change.
> - **`PopoverPosition.END_TOP` exists in Vaadin 24.** The fallback to `END` in `resolveEndTopPosition()` was never exercised but is retained as a defensive lookup.
> - **Text-node filtering.** Several element streams need `.filter(e -> !e.isTextNode())` before calling `getTag()` — Vaadin's `Element.getTag()` throws on text nodes. This was discovered during Task 9 and applied consistently.
> - **E2E runs in production mode.** After the plan was written, the e2e module was flipped from dev-mode (with or without hotdeploy) to `vaadin.productionMode=true`, with `vaadin-maven-plugin`'s `build-frontend` goal bound to `compile`. Reasoning: E2E tests should verify the production artifact, and this eliminates the first-paint flakiness where the dev-bundle compile outran the default `expect` timeout. See commit `86447dd`. The demo module stays in dev-mode with `vaadin.frontend.hotdeploy=true` (HMR is useful there — it's bedient by humans, not CI).
> - **Playwright timeouts widened.** `playwright.config.ts` sets per-test timeout 120 s, navigation 60 s, action 15 s, `expect` 30 s, `retries: 1`. Defensively sized; actual wall-clock per test is sub-second now that prod-mode removes first-paint compile.
> - **Post-MVP polish round** (commits `8f70228`, `0090504`) — four coupled behaviour tweaks plus one failed CSS attempt, all found while exercising the component in the browser:
>   - **Popover must close on inline-expand** — user hovers → popover opens → user clicks the chevron to inline-expand → popover was lingering while inline children also appeared. Fixed via `expanded-changed` listener + `applyPopoverGating` close-on-ineligible. Spec §4.1 "Live state transitions" documents this.
>   - **Popover must reopen on inline-collapse** (symmetric bug) — after the close-on-expand fix landed, collapsing the item again left the popover closed because Vaadin's hover trigger waits for a fresh `mouseenter`. Fixed by calling `popover.open()` explicitly on the `true → false` transition.
>   - **No popover on page load** (regression from the previous fix) — the `expanded-changed` DOM event also fires once at initial attach with the current (unchanged) value, so the naive listener would open every item's popover on page load. Fixed by tracking the last-known expanded state server-side and only reacting to real transitions.
>   - **Demo multi-level nesting.** The demo `MainLayout` had only one-level children. Extended "Branches" and "Environments" with three grandchildren each so the full three-level flow (parent → popover child → inline-expand grandchildren) can actually be demonstrated.
>   - **Row-height parity — no fix needed.** Initial thought was that rail-mode rows looked shorter than normal-mode rows. A first CSS attempt (`::part(link) { min-height; justify-content: center }`) broke the rendering in the browser (off-center icons, misaligned active-highlight) and was reverted. User confirmed that the baseline (plain `display: none` on label/toggle/suffix/slotted-children) looks fine in practice — the perceived mismatch was overstated. No follow-up needed.
>   - Regression tests in `popover-collapsed-item.spec.ts`: *popover closes when the item is expanded inline*, *popover reopens when the user collapses the item inline again*, *no popover is open immediately after page load*. Suite: 11/11 green.

---

## Cross-phase conventions

These apply to every task in every phase. Don't repeat them inside individual task steps — rely on this section.

### Licensing and authorship
- Every Java source file begins with this header (adjust only the Javadoc block):
  ```java
  /*
   * Copyright 2026 Vaadin Ltd.
   *
   * Licensed under the Apache License, Version 2.0 (the "License");
   * you may not use this file except in compliance with the License.
   * You may obtain a copy of the License at
   *
   *     https://www.apache.org/licenses/LICENSE-2.0
   *
   * Unless required by applicable law or agreed to in writing, software
   * distributed under the License is distributed on an "AS IS" BASIS,
   * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   * See the License for the specific language governing permissions and
   * limitations under the License.
   */
  ```
  Tasks may show a collapsed form (`Copyright … Apache License, Version 2.0.`) for brevity — the full text above is what actually goes in the file.
- All project artifacts (code, Javadoc, commit messages, comments) are written in English. Chat with the user is German, the project is not.

### Build
- Use `./mvnw` exclusively once Task 1 sets it up. Don't fall back to bare `mvn`.
- `<vaadin.version>` is pinned to `24.5.0` throughout the MVP. Bumping happens outside this plan.
- Java target: `maven.compiler.release=17` (via devcontainer Dockerfile; matches the Dockerfile).

### Commits
- Conventional-commit-ish prefixes: `build:` (POMs, wrapper, plugins), `feat:` (user-facing component code), `test:` (anything under `src/test/`), `docs:`, `chore:`.
- One commit per task step that the phase file marks "Commit". Tasks are the review unit — each task = one green commit.

### Package layout
- Addon production code lives under `org.vaadin.addons.componentfactory.sidenavrail`.
- Addon test runtime and test views live under `org.vaadin.addons.componentfactory.sidenavrail.app` (and `.app.views`).
- Addon unit tests live under `org.vaadin.addons.componentfactory.sidenavrail.unit`.
- Demo code lives under `org.vaadin.addons.componentfactory.sidenavrail.demo` (and `.demo.views`).

### TDD discipline
- Write the failing test *first*, then run it to see it fail, then implement, then re-run. Phase files spell this out as separate steps — don't collapse them.

---

## Phases

Phases are the dispatch unit. Execute them in the listed order; within a phase, execute tasks in listed order. A fresh subagent should receive **this index file + the single phase file** — nothing else from the plan is needed.

| # | Phase | Tasks | File |
|---|---|---|---|
| 1 | Build infrastructure — parent POM, Maven wrapper, addon and demo skeletons | 1–3 | [`side-nav-rail/01-build-infrastructure.md`](./side-nav-rail/01-build-infrastructure.md) |
| 2 | Core API — `PopoverMode`, `RailModeChangedEvent`, `SideNavRail` state/events/mode | 4–8 | [`side-nav-rail/02-core-api.md`](./side-nav-rail/02-core-api.md) |
| 3 | Item and styling — `SideNavRailItem` label wrap, CSS module | 9–10 | [`side-nav-rail/03-item-and-styling.md`](./side-nav-rail/03-item-and-styling.md) |
| 4 | Popover — lifecycle, content copy, mode gating | 11–13 | [`side-nav-rail/04-popover.md`](./side-nav-rail/04-popover.md) |
| 5 | Test runtime — Spring Boot test app, test views, Playwright scaffold, Maven bindings | 14–17 | [`side-nav-rail/05-test-runtime.md`](./side-nav-rail/05-test-runtime.md) |
| 6 | E2E tests — four `*.spec.ts` files + full `mvn verify` | 18–22 | [`side-nav-rail/06-e2e-tests.md`](./side-nav-rail/06-e2e-tests.md) |
| 7 | Demo — showcase view + label-wrap rendering-neutrality smoke | 23–24 | [`side-nav-rail/07-demo.md`](./side-nav-rail/07-demo.md) |

### Phase dependencies

- Phases 1 → 2 → 3 → 4: strictly linear. Each phase depends on the compiling artifacts of its predecessors.
- Phase 5 depends on phases 1–4 (uses the addon code).
- Phase 6 depends on phase 5 (test views + Playwright scaffold).
- Phase 7 depends on phase 4 (needs the fully-working component); runs independently of phases 5–6 if desired.

Phase 6's final task (Task 22, full `mvn verify`) is the go/no-go gate for the MVP.

---

## Spec coverage map

Where each spec section lands in the plan:

| Spec section | Covered by |
|---|---|
| §2 module structure | Phase 1 (Tasks 1–3) |
| §3.1 `SideNavRail` API | Phase 2 (Tasks 6–8) |
| §3.2 `SideNavRailItem` label wrap | Phase 3 (Task 9) |
| §3.3 `PopoverMode` | Phase 2 (Task 4) |
| §3.4 `RailModeChangedEvent` | Phase 2 (Task 5) |
| §4.1 behaviour matrix | Phases 4 + 6 (Tasks 11–13, E2E Tasks 19–20) |
| §4.2 popover details | Phase 4 (Tasks 11–13) |
| §4.3 rail-mode behaviour | Phase 2 (Tasks 6–7) |
| §5.1–5.2 styling | Phase 3 (Task 10) |
| §5.3 label-wrap rendering neutrality | Phase 7 (Task 24) |
| §6 packaging details | Phases 1, 5 (Tasks 1–3, 17) |
| §7 tests | Phases 5–6 (Tasks 14–22) |
| §8 demo module | Phases 1, 7 (Tasks 3, 23) |
| §9 out of scope | — (intentionally not touched) |
| §10 open points | Phase 4 Task 11 (PopoverPosition enum fallback), Phase 7 Task 24 (label-wrap smoke test) |

---

## Self-review notes (from plan writing)

- No "TBD", "similar to Task N", or generic "handle edge cases" language anywhere — every code step carries full code.
- Type and API-name consistency spot-checked across phases: `RailModeChangedEvent` constructor signature, `SideNavRailItem#applyPopoverGating` package-private contract, `PopoverMode.COLLAPSED_ITEM`/`RAIL_ONLY` usage from unit tests through views to Playwright selectors.
- Spec §10 "open points at implementation time" both covered: `PopoverPosition.END_TOP` fallback lives in Phase 4 Task 11; label-wrap side-by-side smoke lives in Phase 7 Task 24.
