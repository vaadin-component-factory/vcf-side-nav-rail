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

> **Status (2026-04-21):** all 24 tasks complete on branch `mvp/iteration-1`. Final reactor `./mvnw clean verify` is green — 17/17 addon unit tests + 8/8 Playwright E2E tests pass.
>
> The spec has been updated in-place to reflect the final shape of the code. This plan document is kept as the historical record of *how* the work got there.
>
> **Key deviations from the plan as originally written:**
>
> - **Separate `e2e/` module.** The plan assumed the Spring Boot test runtime would live in `addon/src/test/java/.../app/`. In practice, `spring-boot-maven-plugin` + `classesDirectory` + `useTestClasspath` produced a tangle of workarounds that would not boot reliably. Phase 5 was effectively re-executed by moving TestApplication + test views + Playwright scaffold into a dedicated `e2e/` module where they live at compile scope. The addon POM is consequently much leaner (no Spring Boot test deps, no integration-test plugin chain). See commit `306a2a9`.
> - **Java 17, not 25.** Spring Boot 3.x's bundled ASM cannot parse Java 25 class files. The devcontainer Dockerfile and both POMs were changed to Java 17. See commit `27251c4`.
> - **Vaadin 24.10.1 + Spring Boot 3.5.13.** The plan's version pins (24.5.0 / 3.4.0) were placeholders; the final build uses the latest stable in each 24.x / 3.x line. See commit `4fb9bb4`.
> - **`TestMainLayout` removed.** The plan included a shared `@Layout` layout wrapping all test views. This caused duplicate `#toggle-rail` IDs when a view also declared its own toggle button; Playwright's strict mode rejected the ambiguous selectors. Resolved by removing the layout and giving `BasicTestView` its own rail + toggle.
> - **Popover attached to owning `SideNavRail` element.** The plan's code appended the popover to the UI root; the test expected it at depth 2. They were incompatible. The implementation attaches to the rail ancestor (walking the full parent chain). No visual impact — the overlay teleports to body either way.
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
