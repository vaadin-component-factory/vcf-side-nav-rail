# Tooltip / Popover Merge — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add `RailTooltipMode.POPOVER` (popover-as-tooltip), replace the old `ALL`/`ONLY_WITHOUT_CHILDREN`/`setRailTooltipNative` triad with a clean four-value enum (`NONE`/`BROWSER_NATIVE`/`STYLED`/`POPOVER`), and rename `PopoverParentLabelMode` to `PopoverHeaderMode`.

**Architecture:** Pre-1.0 breaking renames, no deprecation cycle. Leaf popovers reuse the existing `Popover` lifecycle in `SideNavRailItem` — the only structural change is relaxing the `ensurePopover()` early-return when the owning rail signals leaf-popover-as-tooltip is active. Header content for leaves goes through the same `PopoverHeaderMode` mechanism that already drives the parent-popover header.

**Tech Stack:** Vaadin 24 (target compatibility floor), V25-tested, Java 17, JUnit 5 + KariBu Testing for unit tests, Playwright (TypeScript) for E2E. Relevant spec: [`specs/2026-04-29-tooltip-popover-merge-design.md`](../specs/2026-04-29-tooltip-popover-merge-design.md).

**Branch:** `feat/tooltip-popover-merge` (already created).

---

## File Map

**Renames:**
- `addon/src/main/java/org/vaadin/addons/componentfactory/sidenavrail/PopoverParentLabelMode.java` → `…/PopoverHeaderMode.java`
- `addon/src/test/java/…/unit/PopoverParentLabelModeTest.java` → `…/unit/PopoverHeaderModeTest.java`
- `addon/src/test/java/…/unit/PopoverParentLabelOnlyInRailModeTest.java` → `…/unit/PopoverHeaderOnlyInRailModeTest.java`
- `e2e/src/main/java/…/views/PopoverParentLabelModeView.java` → `…/views/PopoverHeaderModeView.java`
- `e2e/src/main/java/…/views/PopoverParentLabelOnlyInRailModeView.java` → `…/views/PopoverHeaderOnlyInRailModeView.java`
- `e2e/src/test/playwright/tests/popover-parent-label-mode.spec.ts` → `…/popover-header-mode.spec.ts`
- `e2e/src/test/playwright/tests/popover-parent-label-only-in-rail-mode.spec.ts` → `…/popover-header-only-in-rail-mode.spec.ts`

**Modifies:**
- `addon/src/main/java/…/RailTooltipMode.java` — enum values + JavaDoc rewritten
- `addon/src/main/java/…/SideNavRail.java` — field rename, default flip, `applyTooltipFor` switch, new `isLeafPopoverActive()`, attach-time auto-coerce, runtime mode-flip refresh, removal of native flag
- `addon/src/main/java/…/SideNavRailItem.java` — `ensurePopover()` leaf-gate relaxation, `populatePopover()` header-only branch for leaves
- `addon/src/test/java/…/unit/RailTooltipModeTest.java` — rewritten for new enum values, plus added cases
- `e2e/src/main/java/…/views/RailTooltipModeView.java` — new mode buttons
- `e2e/src/test/playwright/tests/rail-tooltip-mode.spec.ts` — new mode coverage
- `demo/src/main/java/…/MainLayout.java` — adapt API calls
- `README.md`, `CLAUDE.md` — API surface highlights

**Creates:**
- `e2e/src/main/java/…/views/RailTooltipPopoverModeView.java` — dedicated test view for `POPOVER` mode
- `e2e/src/test/playwright/tests/rail-tooltip-popover.spec.ts` — Playwright spec for `POPOVER` mode

**Out of scope (untouched):**
- Old plans/specs in `plans/2026-04-2*-…` and `specs/2026-04-2*-…` (historical artifacts; the new spec stands separate)
- `frontend/side-nav-rail.js`, `side-nav-rail.css` — no changes needed; existing V24/V25 popover abstractions and `:focus-within` CSS already cover the new behaviour

---

## Task 1: Rename `PopoverParentLabelMode` → `PopoverHeaderMode`

Pure mechanical rename. Class, file, all references, test/view/spec filenames. The enum *values* stay (`NONE`, `LABEL_ONLY`, `ICON_ONLY`, `FULL`).

**Files:**
- Move: `addon/src/main/java/…/PopoverParentLabelMode.java` → `…/PopoverHeaderMode.java`
- Move: `addon/src/test/java/…/unit/PopoverParentLabelModeTest.java` → `…/unit/PopoverHeaderModeTest.java`
- Move: `addon/src/test/java/…/unit/PopoverParentLabelOnlyInRailModeTest.java` → `…/unit/PopoverHeaderOnlyInRailModeTest.java`
- Move: `e2e/src/main/java/…/views/PopoverParentLabelModeView.java` → `…/views/PopoverHeaderModeView.java`
- Move: `e2e/src/main/java/…/views/PopoverParentLabelOnlyInRailModeView.java` → `…/views/PopoverHeaderOnlyInRailModeView.java`
- Move: `e2e/src/test/playwright/tests/popover-parent-label-mode.spec.ts` → `…/popover-header-mode.spec.ts`
- Move: `e2e/src/test/playwright/tests/popover-parent-label-only-in-rail-mode.spec.ts` → `…/popover-header-only-in-rail-mode.spec.ts`
- Modify: `addon/src/main/java/…/SideNavRail.java`
- Modify: `addon/src/main/java/…/SideNavRailItem.java` (if it references the old class)
- Modify: `demo/src/main/java/…/MainLayout.java`

- [ ] **Step 1.1: Rename source file**

```bash
git mv addon/src/main/java/org/vaadin/addons/componentfactory/sidenavrail/PopoverParentLabelMode.java \
       addon/src/main/java/org/vaadin/addons/componentfactory/sidenavrail/PopoverHeaderMode.java
```

- [ ] **Step 1.2: Rename the class identifier inside the file**

In `addon/src/main/java/…/PopoverHeaderMode.java`: change `public enum PopoverParentLabelMode` to `public enum PopoverHeaderMode`. Update JavaDoc that says "Controls whether (and how) the popover opened on a SideNavRailItem with children renders a header that identifies its parent" — the *with children* qualifier is no longer accurate (the header is also used for leaves under `RailTooltipMode.POPOVER`). Replace with:

```java
/**
 * Controls whether (and how) a {@link SideNavRailItem}'s popover renders a header that
 * identifies the item itself. Opt-in — the default is {@link #NONE}.
 *
 * <p>The header is rendered above the nested {@code SideNav} that shows children, when
 * the item has children. For leaf items shown via {@link RailTooltipMode#POPOVER}, the
 * header is the only content of the popover.
 *
 * <p>If the configured mode would produce an empty header (e.g. {@link #ICON_ONLY} on a
 * parent without a prefix component), the header is omitted entirely rather than rendered
 * blank. Note: rail-mode root items always carry a prefix component (the auto-generated
 * letter avatar), so {@link #ICON_ONLY} never produces an empty header in practice.
 *
 * <p>Changes made after the popover has been rendered take effect immediately — the rail
 * rewires all existing popovers when {@link SideNavRail#setPopoverHeaderMode} is called.
 * Changes to the parent's label or prefix component <em>after</em> the popover exists are
 * picked up only on the next call to {@code setPopoverHeaderMode}; update the item
 * before rendering or re-trigger the mode to refresh.
 */
```

- [ ] **Step 1.3: Sweep all references across the codebase**

```bash
grep -rln 'PopoverParentLabelMode' /workspace --include='*.java' --include='*.ts' --include='*.md'
```

For each Java/TypeScript hit, replace `PopoverParentLabelMode` with `PopoverHeaderMode` (case-sensitive, identifier only — leave Markdown spec/plan files alone for now, they will be touched by Task 8). Also replace these method names:

| Old | New |
|---|---|
| `setPopoverParentLabelMode` | `setPopoverHeaderMode` |
| `getPopoverParentLabelMode` | `getPopoverHeaderMode` |
| `setPopoverParentLabelOnlyInRailMode` | `setPopoverHeaderOnlyInRailMode` |
| `isPopoverParentLabelOnlyInRailMode` | `isPopoverHeaderOnlyInRailMode` |
| `popoverParentLabelMode` (field/local) | `popoverHeaderMode` |
| `popoverParentLabelOnlyInRailMode` (field/local) | `popoverHeaderOnlyInRailMode` |

The grep above should give you the full list — `SideNavRail.java`, `SideNavRailItem.java` (only if it references the field/methods), demo `MainLayout.java`, the e2e views, the unit-test files, and any imports.

- [ ] **Step 1.4: Rename test files and the test classes inside**

```bash
git mv addon/src/test/java/org/vaadin/addons/componentfactory/sidenavrail/unit/PopoverParentLabelModeTest.java \
       addon/src/test/java/org/vaadin/addons/componentfactory/sidenavrail/unit/PopoverHeaderModeTest.java
git mv addon/src/test/java/org/vaadin/addons/componentfactory/sidenavrail/unit/PopoverParentLabelOnlyInRailModeTest.java \
       addon/src/test/java/org/vaadin/addons/componentfactory/sidenavrail/unit/PopoverHeaderOnlyInRailModeTest.java
```

In each renamed test file, change the `class` identifier to match: `class PopoverHeaderModeTest` and `class PopoverHeaderOnlyInRailModeTest`. The internal references to the type were already swept by Step 1.3.

- [ ] **Step 1.5: Rename the e2e views and Playwright specs**

```bash
git mv e2e/src/main/java/org/vaadin/addons/componentfactory/sidenavrail/e2e/views/PopoverParentLabelModeView.java \
       e2e/src/main/java/org/vaadin/addons/componentfactory/sidenavrail/e2e/views/PopoverHeaderModeView.java
git mv e2e/src/main/java/org/vaadin/addons/componentfactory/sidenavrail/e2e/views/PopoverParentLabelOnlyInRailModeView.java \
       e2e/src/main/java/org/vaadin/addons/componentfactory/sidenavrail/e2e/views/PopoverHeaderOnlyInRailModeView.java
git mv e2e/src/test/playwright/tests/popover-parent-label-mode.spec.ts \
       e2e/src/test/playwright/tests/popover-header-mode.spec.ts
git mv e2e/src/test/playwright/tests/popover-parent-label-only-in-rail-mode.spec.ts \
       e2e/src/test/playwright/tests/popover-header-only-in-rail-mode.spec.ts
```

In each renamed Java view, update the `public class …` identifier to match the file name. Also update `@Route("popover-parent-label-mode")` and `@Route("popover-parent-label-only-in-rail-mode")` to `@Route("popover-header-mode")` and `@Route("popover-header-only-in-rail-mode")` respectively.

In each renamed Playwright spec, update any `await page.goto('/popover-parent-label-mode')` / `'/popover-parent-label-only-in-rail-mode'` to the new routes. `describe(...)` titles can stay as descriptive prose, but if they reference the old enum name in code-style backticks, update them.

- [ ] **Step 1.6: Compile addon + run renamed unit tests**

```bash
./mvnw -pl addon clean test 2>&1 | tee /tmp/task1-test.log
```

Expected: BUILD SUCCESS, all 99 addon unit tests pass (including the renamed `PopoverHeaderModeTest` and `PopoverHeaderOnlyInRailModeTest`).

- [ ] **Step 1.7: Compile e2e module (without running browser tests)**

```bash
./mvnw -pl e2e -am compile 2>&1 | tee /tmp/task1-e2e-compile.log
```

Expected: BUILD SUCCESS. This catches any missed reference in the e2e Java views.

- [ ] **Step 1.8: Compile demo module**

```bash
./mvnw -pl demo -am compile 2>&1 | tee /tmp/task1-demo-compile.log
```

Expected: BUILD SUCCESS.

- [ ] **Step 1.9: Commit**

```bash
git add -A
git commit -m "$(cat <<'EOF'
refactor(addon): rename PopoverParentLabelMode to PopoverHeaderMode

Pure mechanical rename. Setters/getters and field names are renamed
accordingly. Enum values are unchanged. The "parent" qualifier is
dropped so the type can also describe the header used by leaf popovers
(RailTooltipMode.POPOVER, follow-up). JavaDoc updated to reflect both
parent and leaf use.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 2: Replace `RailTooltipMode` enum + drop `setRailTooltipNative`

Atomic update: the new enum values are not source-compatible with the old, so callers (tests, e2e views, demo) must be updated in the same commit.

The `POPOVER` value is wired in `applyTooltipFor` as a no-op-attribute branch (it just clears both tooltip attributes; the actual leaf-popover behaviour is added in Task 4). This keeps Task 2 focused on the type/API change.

**Files:**
- Modify: `addon/src/main/java/…/RailTooltipMode.java`
- Modify: `addon/src/main/java/…/SideNavRail.java` (default value, `applyTooltipFor`, `isRailTooltipNative`/`setRailTooltipNative` removal)
- Modify: `addon/src/test/java/…/unit/RailTooltipModeTest.java`
- Modify: `e2e/src/main/java/…/views/RailTooltipModeView.java`
- Modify: `e2e/src/test/playwright/tests/rail-tooltip-mode.spec.ts`
- Modify: `e2e/src/test/playwright/tests/tooltip-popover-coexistence.spec.ts` (if it uses removed values)
- Modify: `demo/src/main/java/…/MainLayout.java`
- Modify: `README.md` (just the Public API highlights paragraph — full doc update is in Task 8)

- [ ] **Step 2.1: Rewrite `RailTooltipMode.java`**

Replace the entire contents (preserve license header):

```java
package org.vaadin.addons.componentfactory.sidenavrail;

/**
 * Controls how root items of a {@link SideNavRail} surface their identity while the rail
 * is in rail mode. Tooltips are never shown in normal mode — the label text identifies
 * the item in that case.
 *
 * <p>The label source is the item's own {@code getLabel()}. Tooltips apply to direct
 * children of the rail only; nested items never get a tooltip from the rail.
 *
 * <p>Three of the four modes ({@link #BROWSER_NATIVE}, {@link #STYLED}, {@link #POPOVER})
 * apply to <em>every</em> rail-mode root item. The previous distinction between
 * "items with children" and "all items" is gone — combine with {@link PopoverHeaderMode}
 * if you want fine-grained control over what's shown.
 */
public enum RailTooltipMode {

    /** No tooltips on root items. */
    NONE,

    /**
     * Browser-native tooltip via the {@code title} HTML attribute. Hover-only — does not
     * appear on keyboard focus (browser limitation). Browser-decided delay (~500&nbsp;ms)
     * and styling, so it won't react to {@link SideNavRail#setPopoverHoverDelay(int)} and
     * may look inconsistent with the rest of the Vaadin UI. Carries zero overlay-interaction
     * risk and works everywhere {@code title} works, including assistive technologies.
     */
    BROWSER_NATIVE,

    /**
     * Lumo-themed CSS pseudo-element tooltip. Default. Reacts to both hover and keyboard
     * focus (via {@code :focus-within}). Immune to {@code vaadin-tooltip-mixin}'s overlay
     * dismissal because it does not participate in the overlay system.
     *
     * <p>When combined with a parent-popover, the tooltip and popover both appear on the
     * same item — the tooltip sits below the icon (default tooltip position), the popover
     * opens to the right, so they don't spatially overlap. Use {@link #POPOVER} if you
     * want a single overlay per item.
     */
    STYLED,

    /**
     * Tooltip is rendered as a {@link com.vaadin.flow.component.popover.Popover} with the
     * configured {@link PopoverHeaderMode} as its content. For items with children the
     * existing parent-popover doubles as the tooltip — there is exactly one overlay per
     * item. For leaf items a popover is created on demand whose only content is the
     * header.
     *
     * <p>Reacts to hover and keyboard focus (via {@code Popover.setOpenOnFocus}). Inherits
     * hover/hide delays, position, and arrow visibility from the rail's existing popover
     * settings.
     *
     * <p><b>Constraint:</b> requires a non-{@link PopoverHeaderMode#NONE} header — without
     * one the popover would have no content. If the rail is attached with
     * {@code RailTooltipMode.POPOVER} and {@code PopoverHeaderMode.NONE} configured, the
     * header mode is silently coerced to {@link PopoverHeaderMode#LABEL_ONLY}. Setting the
     * combination via runtime setters after attach is not validated and may produce an
     * empty popover.
     */
    POPOVER
}
```

- [ ] **Step 2.2: Update `SideNavRail.java` field default + remove native flag**

In `addon/src/main/java/…/SideNavRail.java`:

1. Change line 78: `private RailTooltipMode railTooltipMode = RailTooltipMode.ALL;` to `private RailTooltipMode railTooltipMode = RailTooltipMode.STYLED;`
2. Delete line 82: `private boolean railTooltipNative = false;`
3. Delete the `isRailTooltipNative()` method (around lines 282–292) and the `setRailTooltipNative(boolean)` method (around lines 294–311). Also delete the `NATIVE_TOOLTIP_ATTRIBUTE` constant if defined separately (keep for now if shared with the switch — see Step 2.3).

- [ ] **Step 2.3: Rewrite `applyTooltipFor` switch**

Locate the method around line 550:

```java
private void applyTooltipFor(SideNavItem item) {
    item.getElement().removeAttribute(RAIL_TOOLTIP_ATTRIBUTE);
    item.getElement().removeAttribute(NATIVE_TOOLTIP_ATTRIBUTE);
    if (!railMode || railTooltipMode == RailTooltipMode.NONE) {
        return;
    }
    String label = item.getLabel();
    if (label == null || label.isBlank()) {
        return;
    }
    switch (railTooltipMode) {
        case STYLED -> item.getElement().setAttribute(RAIL_TOOLTIP_ATTRIBUTE, label);
        case BROWSER_NATIVE -> item.getElement().setAttribute(NATIVE_TOOLTIP_ATTRIBUTE, label);
        case POPOVER -> { /* No attribute; leaf-popover wiring (Task 4) handles this. */ }
        default -> { /* NONE handled above */ }
    }
}
```

`NATIVE_TOOLTIP_ATTRIBUTE` — if it is currently defined as a private constant in `SideNavRail`, keep it. If it was inlined as `"title"`, replace the inline `setAttribute("title", label)` (used by the old `setRailTooltipNative` path) with the constant. Verify both attributes are wiped on every call.

- [ ] **Step 2.4: Update the `setRailTooltipMode` JavaDoc**

In `SideNavRail.java`, update the JavaDoc on `setRailTooltipMode` to drop the "see RailTooltipMode for the per-value semantics" sentence's reference to the old values; the new JavaDoc on the enum carries that detail. Same for `getRailTooltipMode`.

- [ ] **Step 2.5: Rewrite `RailTooltipModeTest.java`**

Replace the entire test class body (preserve license header + package + imports + `@BeforeEach`/`@AfterEach`). Tests to include:

```java
private static final String STYLED_ATTR = "data-rail-tooltip";
private static final String NATIVE_ATTR = "title";

@Test
void defaultIsStyled() {
    SideNavRail nav = new SideNavRail();
    assertEquals(RailTooltipMode.STYLED, nav.getRailTooltipMode());
}

@Test
void styledSetsCustomAttributeInRailMode() {
    SideNavRail nav = railWithItem("Dashboard");
    UI.getCurrent().add(nav);
    nav.setRailMode(true);
    SideNavItem item = nav.getItems().get(0);
    assertEquals("Dashboard", item.getElement().getAttribute(STYLED_ATTR));
    assertFalse(item.getElement().hasAttribute(NATIVE_ATTR));
}

@Test
void browserNativeSetsTitleAttributeInRailMode() {
    SideNavRail nav = railWithItem("Dashboard");
    nav.setRailTooltipMode(RailTooltipMode.BROWSER_NATIVE);
    UI.getCurrent().add(nav);
    nav.setRailMode(true);
    SideNavItem item = nav.getItems().get(0);
    assertEquals("Dashboard", item.getElement().getAttribute(NATIVE_ATTR));
    assertFalse(item.getElement().hasAttribute(STYLED_ATTR));
}

@Test
void popoverModeClearsBothTooltipAttributes() {
    SideNavRail nav = railWithItem("Dashboard");
    nav.setPopoverHeaderMode(PopoverHeaderMode.LABEL_ONLY);  // valid combo
    nav.setRailTooltipMode(RailTooltipMode.POPOVER);
    UI.getCurrent().add(nav);
    nav.setRailMode(true);
    SideNavItem item = nav.getItems().get(0);
    assertFalse(item.getElement().hasAttribute(STYLED_ATTR));
    assertFalse(item.getElement().hasAttribute(NATIVE_ATTR));
}

@Test
void noneRemovesBothAttributes() {
    SideNavRail nav = railWithItem("Dashboard");
    UI.getCurrent().add(nav);
    nav.setRailMode(true);
    nav.setRailTooltipMode(RailTooltipMode.NONE);
    SideNavItem item = nav.getItems().get(0);
    assertFalse(item.getElement().hasAttribute(STYLED_ATTR));
    assertFalse(item.getElement().hasAttribute(NATIVE_ATTR));
}

@Test
void normalModeDoesNotShowTooltipRegardlessOfMode() {
    SideNavRail nav = railWithItem("Dashboard");
    UI.getCurrent().add(nav);
    nav.setRailMode(false);
    for (RailTooltipMode mode : RailTooltipMode.values()) {
        nav.setRailTooltipMode(mode);
        SideNavItem item = nav.getItems().get(0);
        assertFalse(item.getElement().hasAttribute(STYLED_ATTR));
        assertFalse(item.getElement().hasAttribute(NATIVE_ATTR));
    }
}

@Test
void switchingFromNativeToStyledClearsTitleAttribute() {
    SideNavRail nav = railWithItem("Dashboard");
    nav.setRailTooltipMode(RailTooltipMode.BROWSER_NATIVE);
    UI.getCurrent().add(nav);
    nav.setRailMode(true);
    nav.setRailTooltipMode(RailTooltipMode.STYLED);
    SideNavItem item = nav.getItems().get(0);
    assertEquals("Dashboard", item.getElement().getAttribute(STYLED_ATTR));
    assertFalse(item.getElement().hasAttribute(NATIVE_ATTR));
}

@Test
void nullModeThrows() {
    SideNavRail nav = new SideNavRail();
    assertThrows(NullPointerException.class, () -> nav.setRailTooltipMode(null));
}

@Test
void blankLabelDoesNotProduceTooltipAttribute() {
    SideNavRail nav = railWithItem("");
    UI.getCurrent().add(nav);
    nav.setRailMode(true);
    SideNavItem item = nav.getItems().get(0);
    assertFalse(item.getElement().hasAttribute(STYLED_ATTR));
}

private static SideNavRail railWithItem(String label) {
    SideNavRail nav = new SideNavRail();
    nav.addItem(new SideNavRailItem(label, "/x"));
    return nav;
}
```

Drop any test that references the removed `setRailTooltipNative`, `isRailTooltipNative`, `RailTooltipMode.ALL`, or `RailTooltipMode.ONLY_WITHOUT_CHILDREN`. Adjust import list to drop `assertTrue` if unused after the rewrite, and ensure `PopoverHeaderMode` is imported for the `popoverModeClearsBothTooltipAttributes` test.

- [ ] **Step 2.6: Update `RailTooltipModeView.java` (e2e view)**

In `e2e/src/main/java/…/views/RailTooltipModeView.java`, replace the three mode buttons:

```java
HorizontalLayout modeButtons = new HorizontalLayout(
        modeButton(rail, RailTooltipMode.NONE, "mode-none"),
        modeButton(rail, RailTooltipMode.BROWSER_NATIVE, "mode-browser-native"),
        modeButton(rail, RailTooltipMode.STYLED, "mode-styled"));
```

`POPOVER` mode gets its own dedicated view in Task 6 (because it requires `PopoverHeaderMode != NONE` and a different inspection path); leaving it out of this view keeps the existing rail-tooltip-mode spec focused on attribute-based modes.

- [ ] **Step 2.7: Update `rail-tooltip-mode.spec.ts`**

Locate any `data-test-id` / `id` references to `mode-without-children` / `mode-all` and replace them with `mode-browser-native` / `mode-styled`. Update assertions to match the new attribute behaviour:

- `mode-styled` ⇒ `data-rail-tooltip` set on every root item
- `mode-browser-native` ⇒ `title` set on every root item
- `mode-none` ⇒ neither attribute set

The "tooltip on items with children" cases that the old `ALL` value covered should be merged into the `STYLED` and `BROWSER_NATIVE` cases — they now apply to every root item. Drop any test asserting the absence of a tooltip on an item-with-children specifically tied to `ONLY_WITHOUT_CHILDREN`.

- [ ] **Step 2.8: Update `tooltip-popover-coexistence.spec.ts`**

Open the file and check whether it references `setRailTooltipNative` or the removed enum values via routes. If it tests "tooltip and popover both visible on a parent" using the old default (`ALL`), update the route/setup to use the new `STYLED` default explicitly so the test intent stays clear. The behavioural assertion (both visible together) is still valid for `STYLED` and `BROWSER_NATIVE`.

- [ ] **Step 2.9: Update `MainLayout.java` (demo)**

In `demo/src/main/java/…/MainLayout.java`, search for `setRailTooltipNative` and `RailTooltipMode.ALL` / `RailTooltipMode.ONLY_WITHOUT_CHILDREN`. Replace:
- `rail.setRailTooltipNative(true)` → `rail.setRailTooltipMode(RailTooltipMode.BROWSER_NATIVE)`
- `rail.setRailTooltipNative(false)` → no-op (default is now `STYLED`); remove the call.
- `RailTooltipMode.ALL` → `RailTooltipMode.STYLED`
- `RailTooltipMode.ONLY_WITHOUT_CHILDREN` → drop the call entirely; the new semantics doesn't have an equivalent.

The demo's tooltip-mode select (if any) needs to be rebuilt around the new four-value enum — see the Demo update in Task 7 for the full UX.

- [ ] **Step 2.10: Update `README.md` highlights paragraph (just the API list)**

In `README.md`, in the "Public API surface highlights" section, replace the `RailTooltipMode` line:

```markdown
- `RailTooltipMode` (`NONE` / `BROWSER_NATIVE` / `STYLED` (default) / `POPOVER`) — see `PopoverHeaderMode` for `POPOVER` content
```

Drop the `setRailTooltipNative` line. Rename `PopoverParentLabelMode` references to `PopoverHeaderMode`. Full README rewrite for the new feature happens in Task 8 — this step just unbreaks the highlights.

- [ ] **Step 2.11: Run unit tests**

```bash
./mvnw -pl addon clean test 2>&1 | tee /tmp/task2-unit.log
```

Expected: BUILD SUCCESS. The `RailTooltipModeTest` shows the new test count (~8 cases). The `popoverModeClearsBothTooltipAttributes` test passes because Task 2 wires `POPOVER` as a no-op-attribute branch (and the `NONE → LABEL_ONLY` auto-coerce isn't yet implemented — but the test sets `LABEL_ONLY` explicitly, which is the user-visible expected path).

- [ ] **Step 2.12: Compile e2e and demo**

```bash
./mvnw -pl e2e -am compile 2>&1 | tee /tmp/task2-e2e.log
./mvnw -pl demo -am compile 2>&1 | tee /tmp/task2-demo.log
```

Expected: BUILD SUCCESS for both.

- [ ] **Step 2.13: Commit**

```bash
git add -A
git commit -m "$(cat <<'EOF'
refactor(addon): redefine RailTooltipMode (NONE/BROWSER_NATIVE/STYLED/POPOVER)

Replaces the ALL / ONLY_WITHOUT_CHILDREN split + setRailTooltipNative
flag with a clean four-value enum. Default is now STYLED, which is
visually identical to the previous (non-native) ALL — so existing
default users see no behavioural change.

POPOVER is wired as a no-op-attribute branch in applyTooltipFor; the
actual leaf-popover support follows in subsequent commits.

Pre-1.0 breaking change.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 3: Add `isLeafPopoverActive()` + runtime mode-flip refresh

Adds the rail-side helper that `SideNavRailItem` queries to decide whether to create a popover for a childless item, and wires `setRailTooltipMode` to call `refreshPopoverFromOwner()` on every root so a runtime mode flip creates/destroys leaf popovers.

This task does *not* yet relax the leaf gate — that is Task 4. Splitting the rail-side wiring (this task) from the item-side gate (next task) keeps each commit independently reviewable.

**Files:**
- Modify: `addon/src/main/java/…/SideNavRail.java`
- Modify: `addon/src/test/java/…/unit/RailTooltipModeTest.java`

- [ ] **Step 3.1: Write failing test for `isLeafPopoverActive`**

Append to `RailTooltipModeTest.java`:

```java
@Test
void isLeafPopoverActiveReflectsRailModeAndPopoverMode() {
    SideNavRail nav = new SideNavRail();
    assertFalse(nav.isLeafPopoverActive());  // not in rail mode

    UI.getCurrent().add(nav);
    nav.setPopoverHeaderMode(PopoverHeaderMode.LABEL_ONLY);
    nav.setRailTooltipMode(RailTooltipMode.POPOVER);
    nav.setRailMode(true);
    assertTrue(nav.isLeafPopoverActive());

    nav.setRailMode(false);
    assertFalse(nav.isLeafPopoverActive());

    nav.setRailMode(true);
    nav.setRailTooltipMode(RailTooltipMode.STYLED);
    assertFalse(nav.isLeafPopoverActive());
}
```

Add `assertTrue` to the imports if missing.

- [ ] **Step 3.2: Run test to verify failure**

```bash
./mvnw -pl addon test -Dtest=RailTooltipModeTest#isLeafPopoverActiveReflectsRailModeAndPopoverMode 2>&1 | tee /tmp/task3-fail.log
```

Expected: FAIL — `cannot find symbol: method isLeafPopoverActive()`.

- [ ] **Step 3.3: Add `isLeafPopoverActive()` to `SideNavRail`**

Insert near the other "active mode" predicates (e.g. next to `isRootMatchNestedActive`) in `SideNavRail.java`:

```java
/**
 * Whether the rail currently asks {@link SideNavRailItem} to create a popover for a
 * leaf (childless) root item, so the popover can act as a Vaadin-themed tooltip.
 * True iff rail mode is active and {@link RailTooltipMode#POPOVER} is selected.
 *
 * @return {@code true} if leaf items should have a popover, {@code false} otherwise
 */
public boolean isLeafPopoverActive() {
    return railMode && railTooltipMode == RailTooltipMode.POPOVER;
}
```

- [ ] **Step 3.4: Verify the new test passes**

```bash
./mvnw -pl addon test -Dtest=RailTooltipModeTest#isLeafPopoverActiveReflectsRailModeAndPopoverMode 2>&1 | tee /tmp/task3-pass.log
```

Expected: PASS.

- [ ] **Step 3.5: Wire `setRailTooltipMode` to refresh popover state**

In `SideNavRail.java`, update `setRailTooltipMode`:

```java
public void setRailTooltipMode(RailTooltipMode mode) {
    this.railTooltipMode = java.util.Objects.requireNonNull(
            mode, "RailTooltipMode must not be null");
    applyTooltips();
    forEachRootRailItem(item -> {
        if (item instanceof SideNavRailItem rail) {
            rail.refreshPopoverFromOwner();
        }
    });
}
```

`refreshPopoverFromOwner` already exists in `SideNavRailItem` but is currently package-private. Verify the access — if private, raise it to package-private.

Also update `setRailMode` (or the rail-mode listener that calls `applyTooltips()`) to call the same refresh — leaf popovers must be created when entering rail mode and torn down (or hover-disabled) when leaving. Locate the rail-mode change handler around line 149 (`updatePopoverGating(); applyTooltips();`) and append the refresh:

```java
forEachRootRailItem(item -> {
    if (item instanceof SideNavRailItem rail) {
        rail.refreshPopoverFromOwner();
    }
});
```

- [ ] **Step 3.6: Run full unit test suite**

```bash
./mvnw -pl addon test 2>&1 | tee /tmp/task3-all.log
```

Expected: BUILD SUCCESS; existing tests still pass (the new wiring only triggers leaf-popover behaviour when `isLeafPopoverActive()` is true, and that branch hasn't been added to `ensurePopover` yet, so existing items see no change).

- [ ] **Step 3.7: Commit**

```bash
git add -A
git commit -m "$(cat <<'EOF'
feat(addon): expose isLeafPopoverActive() and refresh popovers on tooltip-mode change

Adds the SideNavRail-side predicate the items will query to decide
whether to create a popover for a childless leaf, and wires
setRailTooltipMode + setRailMode to refresh per-item popover state so
runtime mode flips take effect.

The item-side gate is still unrelaxed; the predicate is dormant for now.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 4: Implement leaf popover (relax `ensurePopover`, header-only `populatePopover`)

This is the meat of the feature. `SideNavRailItem.ensurePopover()` currently early-returns for leaves; we relax that gate when the owner reports `isLeafPopoverActive()`. `populatePopover()` already supports a header (today: only when item has children); we extend the header path to also fire for leaves.

**Files:**
- Modify: `addon/src/main/java/…/SideNavRailItem.java`
- Modify: `addon/src/test/java/…/unit/RailTooltipModeTest.java` (or new dedicated test file)

- [ ] **Step 4.1: Write failing test — leaf gets a popover when POPOVER active**

Append to `RailTooltipModeTest.java`:

```java
@Test
void popoverModeCreatesPopoverOnLeafInRailMode() {
    SideNavRail nav = new SideNavRail();
    SideNavRailItem leaf = new SideNavRailItem("Dashboard", "/dashboard");
    nav.addItem(leaf);
    nav.setPopoverHeaderMode(PopoverHeaderMode.LABEL_ONLY);
    nav.setRailTooltipMode(RailTooltipMode.POPOVER);
    UI.getCurrent().add(nav);
    nav.setRailMode(true);

    // Inspect the children of the UI to see whether a Popover targeting the leaf was
    // attached. The Popover.setTarget(...) wire-up auto-attaches when the target attaches.
    long popovers = UI.getCurrent().getChildren()
            .filter(c -> c instanceof com.vaadin.flow.component.popover.Popover)
            .count();
    assertEquals(1L, popovers, "Expected one popover on the rail-mode leaf");
}

@Test
void popoverModeRemovesLeafPopoverWhenLeavingRailMode() {
    SideNavRail nav = new SideNavRail();
    SideNavRailItem leaf = new SideNavRailItem("Dashboard", "/dashboard");
    nav.addItem(leaf);
    nav.setPopoverHeaderMode(PopoverHeaderMode.LABEL_ONLY);
    nav.setRailTooltipMode(RailTooltipMode.POPOVER);
    UI.getCurrent().add(nav);
    nav.setRailMode(true);
    nav.setRailMode(false);

    // The popover instance may still exist (kept around for cheap re-entry), but it must
    // not be open-on-hover, so leaving rail mode disarms the trigger.
    leaf.getChildren()
            .filter(c -> c instanceof com.vaadin.flow.component.popover.Popover)
            .map(c -> (com.vaadin.flow.component.popover.Popover) c)
            .forEach(p -> assertFalse(p.isOpenOnHover()));
}
```

Add the `assertEquals` and `assertTrue`/`assertFalse` imports as needed. `Popover` is in `com.vaadin.flow.component.popover.Popover`.

- [ ] **Step 4.2: Run tests to verify failure**

```bash
./mvnw -pl addon test -Dtest=RailTooltipModeTest#popoverModeCreatesPopoverOnLeafInRailMode 2>&1 | tee /tmp/task4-fail.log
```

Expected: FAIL — popover count is 0 (the leaf-gate in `ensurePopover()` returns early).

- [ ] **Step 4.3: Relax `ensurePopover()` leaf gate**

In `SideNavRailItem.java`, modify the early-return in `ensurePopover()` (around line 454):

```java
private void ensurePopover() {
    if (popover != null) {
        return;
    }
    if (getItems().isEmpty() && !ownerWantsLeafPopover()) {
        return;
    }
    popover = new Popover();
    // ... rest unchanged
}

private boolean ownerWantsLeafPopover() {
    SideNavRail owner = findOwnerRail();
    return owner != null && owner.isLeafPopoverActive();
}
```

- [ ] **Step 4.4: Update `populatePopover()` to render header-only for leaves**

In `SideNavRailItem.java`, locate `populatePopover()` (find by `populatePopover` symbol). Before the loop that adds the nested `SideNav`, ensure the header-render path runs even when `getItems().isEmpty()`. The existing logic likely looks like:

```java
private void populatePopover() {
    popover.removeAll();
    addHeaderIfApplicable();   // existing helper, conditional on PopoverHeaderMode etc.
    SideNav nested = new SideNav();
    for (SideNavItem child : getItems()) {
        nested.addItem(child);
    }
    popover.add(nested);
}
```

Modify to skip the nested-SideNav addition when there are no children:

```java
private void populatePopover() {
    popover.removeAll();
    addHeaderIfApplicable();
    if (!getItems().isEmpty()) {
        SideNav nested = new SideNav();
        for (SideNavItem child : getItems()) {
            nested.addItem(child);
        }
        popover.add(nested);
    }
}
```

(Adjust to match the actual symbol names in the codebase. The point is: skip the nested-SideNav block when childless; the header alone is the popover content.)

- [ ] **Step 4.5: Wire leaf popover hover-trigger gating in `applyPopoverGating`**

The existing `applyPopoverGating()` in `SideNavRailItem` decides `popover.setOpenOnHover(true|false)` based on `PopoverOn` and rail state. Leaf popovers must follow a separate rule: open-on-hover iff `owner.isLeafPopoverActive()`.

Locate `applyPopoverGating()` (search for `applyPopoverGating`) and prepend a leaf-shortcut:

```java
void applyPopoverGating() {
    if (popover == null) {
        return;
    }
    SideNavRail owner = findOwnerRail();
    if (owner == null) {
        popover.setOpenOnHover(true);
        return;
    }
    if (getItems().isEmpty()) {
        // Leaf popover — gated entirely by the rail's tooltip mode.
        boolean leafActive = owner.isLeafPopoverActive();
        popover.setOpenOnHover(leafActive);
        if (!leafActive) {
            popover.close();
        }
        return;
    }
    // ... existing logic for items with children unchanged
}
```

(Adjust to match the actual structure of the existing method; insert the leaf shortcut without disturbing the existing parent-popover gating.)

- [ ] **Step 4.6: Verify tests pass**

```bash
./mvnw -pl addon test -Dtest=RailTooltipModeTest 2>&1 | tee /tmp/task4-pass.log
```

Expected: PASS — both new leaf tests + all existing tests in the class.

- [ ] **Step 4.7: Add header-content tests for each `PopoverHeaderMode` on a leaf**

Append to `RailTooltipModeTest.java`:

```java
@Test
void popoverModeOnLeafRendersLabelOnlyHeader() {
    SideNavRail nav = new SideNavRail();
    SideNavRailItem leaf = new SideNavRailItem("Dashboard", "/dashboard",
            VaadinIcon.DASHBOARD.create());
    nav.addItem(leaf);
    nav.setPopoverHeaderMode(PopoverHeaderMode.LABEL_ONLY);
    nav.setRailTooltipMode(RailTooltipMode.POPOVER);
    UI.getCurrent().add(nav);
    nav.setRailMode(true);

    Popover popover = findPopoverFor(leaf);
    String html = popover.getElement().getOuterHTML();
    assertTrue(html.contains("Dashboard"), "Header should contain label");
    assertFalse(html.contains("dashboard-svg") || html.contains("vaadin-icon"),
            "ICON should not appear in LABEL_ONLY mode");
}
```

(Add similar tests for `ICON_ONLY` (asserts an `<vaadin-icon>` element child, no label text) and `FULL` (asserts both). Helper `findPopoverFor(item)` walks `UI.getCurrent().getChildren()` for a `Popover` whose target is `item`.)

Run:

```bash
./mvnw -pl addon test -Dtest=RailTooltipModeTest 2>&1 | tee /tmp/task4-headers.log
```

Expected: PASS.

- [ ] **Step 4.8: Commit**

```bash
git add -A
git commit -m "$(cat <<'EOF'
feat(addon): leaf popover support for RailTooltipMode.POPOVER

Relax ensurePopover()'s childless-item gate when the owning rail's
isLeafPopoverActive() reports true. populatePopover() now renders the
header alone when the item has no children. applyPopoverGating() gets
a leaf shortcut: hover-trigger follows isLeafPopoverActive() exactly,
independent of PopoverOn (which only governs items with children).

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 5: Attach-time auto-coerce (`POPOVER` + `HeaderMode.NONE` ⇒ `LABEL_ONLY`)

**Files:**
- Modify: `addon/src/main/java/…/SideNavRail.java`
- Modify: `addon/src/test/java/…/unit/RailTooltipModeTest.java`

- [ ] **Step 5.1: Write failing test**

Append to `RailTooltipModeTest.java`:

```java
@Test
void attachWithPopoverAndNoneHeaderCoercesToLabelOnly() {
    SideNavRail nav = new SideNavRail();
    nav.addItem(new SideNavRailItem("Dashboard", "/dashboard"));
    nav.setRailTooltipMode(RailTooltipMode.POPOVER);
    // Header mode left at default NONE.
    UI.getCurrent().add(nav);  // attach

    assertEquals(PopoverHeaderMode.LABEL_ONLY, nav.getPopoverHeaderMode());
}

@Test
void attachWithPopoverAndExplicitHeaderModeIsNotCoerced() {
    SideNavRail nav = new SideNavRail();
    nav.addItem(new SideNavRailItem("Dashboard", "/dashboard"));
    nav.setRailTooltipMode(RailTooltipMode.POPOVER);
    nav.setPopoverHeaderMode(PopoverHeaderMode.ICON_ONLY);
    UI.getCurrent().add(nav);

    assertEquals(PopoverHeaderMode.ICON_ONLY, nav.getPopoverHeaderMode());
}

@Test
void attachWithoutPopoverDoesNotCoerce() {
    SideNavRail nav = new SideNavRail();
    nav.addItem(new SideNavRailItem("Dashboard", "/dashboard"));
    // RailTooltipMode default STYLED, header default NONE.
    UI.getCurrent().add(nav);

    assertEquals(PopoverHeaderMode.NONE, nav.getPopoverHeaderMode());
}
```

- [ ] **Step 5.2: Run to verify failures**

```bash
./mvnw -pl addon test -Dtest=RailTooltipModeTest#attachWithPopoverAndNoneHeaderCoercesToLabelOnly 2>&1 | tee /tmp/task5-fail.log
```

Expected: FAIL — header is still `NONE`.

- [ ] **Step 5.3: Implement auto-coerce in the attach-listener path**

Locate the attach listener / lifecycle method in `SideNavRail.java` that already calls `updatePopoverGating()` and `applyTooltips()` (around line 149). Add the coerce *before* those calls:

```java
private void onAttach(/* … */) {
    if (railTooltipMode == RailTooltipMode.POPOVER
            && popoverHeaderMode == PopoverHeaderMode.NONE) {
        popoverHeaderMode = PopoverHeaderMode.LABEL_ONLY;
    }
    updatePopoverGating();
    applyTooltips();
    // ... rest of existing attach logic
}
```

(Adjust to match the actual method signature — likely `onAttach(AttachEvent event)` overriding the Vaadin lifecycle hook.)

- [ ] **Step 5.4: Verify the new tests pass**

```bash
./mvnw -pl addon test -Dtest=RailTooltipModeTest 2>&1 | tee /tmp/task5-pass.log
```

Expected: PASS for the three new attach tests + all earlier tests.

- [ ] **Step 5.5: Commit**

```bash
git add -A
git commit -m "$(cat <<'EOF'
feat(addon): attach-time auto-coerce for POPOVER + HeaderMode.NONE

Silently coerces PopoverHeaderMode.NONE to LABEL_ONLY at attach when
RailTooltipMode.POPOVER is active. Avoids configuring an empty leaf
popover; documented on RailTooltipMode.POPOVER's JavaDoc. Runtime
setters remain un-validated; the demo prevents the invalid combo via
disabled select options.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 6: New e2e view + Playwright spec for `POPOVER` mode

**Files:**
- Create: `e2e/src/main/java/…/views/RailTooltipPopoverModeView.java`
- Create: `e2e/src/test/playwright/tests/rail-tooltip-popover.spec.ts`

- [ ] **Step 6.1: Create the e2e view**

`e2e/src/main/java/org/vaadin/addons/componentfactory/sidenavrail/e2e/views/RailTooltipPopoverModeView.java`:

```java
package org.vaadin.addons.componentfactory.sidenavrail.e2e.views;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import org.vaadin.addons.componentfactory.sidenavrail.PopoverHeaderMode;
import org.vaadin.addons.componentfactory.sidenavrail.RailTooltipMode;
import org.vaadin.addons.componentfactory.sidenavrail.SideNavRail;
import org.vaadin.addons.componentfactory.sidenavrail.SideNavRailItem;

/**
 * Exercises {@link RailTooltipMode#POPOVER}. "Dashboard" is a leaf root item; "Code" has
 * children. Both should produce a popover on hover/focus while in rail mode.
 */
@Route("rail-tooltip-popover")
public class RailTooltipPopoverModeView extends VerticalLayout {

    public RailTooltipPopoverModeView() {
        SideNavRail rail = new SideNavRail();
        rail.setId("rail");
        rail.setPopoverHeaderMode(PopoverHeaderMode.LABEL_ONLY);
        rail.setRailTooltipMode(RailTooltipMode.POPOVER);
        rail.setRailMode(true);

        rail.addItem(
                new SideNavRailItem("Dashboard", "/dashboard", VaadinIcon.DASHBOARD.create()),
                codeParent());

        Button toggleRail = new Button("Toggle rail",
                e -> rail.setRailMode(!rail.isRailMode()));
        toggleRail.setId("toggle-rail");

        Button labelOnly = new Button("LABEL_ONLY",
                e -> rail.setPopoverHeaderMode(PopoverHeaderMode.LABEL_ONLY));
        labelOnly.setId("header-label-only");

        Button iconOnly = new Button("ICON_ONLY",
                e -> rail.setPopoverHeaderMode(PopoverHeaderMode.ICON_ONLY));
        iconOnly.setId("header-icon-only");

        Button full = new Button("FULL",
                e -> rail.setPopoverHeaderMode(PopoverHeaderMode.FULL));
        full.setId("header-full");

        add(new HorizontalLayout(rail, toggleRail, labelOnly, iconOnly, full));
    }

    private static SideNavRailItem codeParent() {
        SideNavRailItem code = new SideNavRailItem("Code", "/code", VaadinIcon.CODE.create());
        code.addItem(new SideNavRailItem("Branches", "/code/branches"));
        return code;
    }
}
```

- [ ] **Step 6.2: Create the Playwright spec**

`e2e/src/test/playwright/tests/rail-tooltip-popover.spec.ts`:

```typescript
import { test, expect, Locator, Page } from '@playwright/test';
import { openPopover, popoverDescendant } from '../lib/popover';

test.describe('RailTooltipMode.POPOVER', () => {
    test.beforeEach(async ({ page }) => {
        await page.goto('/rail-tooltip-popover');
        await page.waitForSelector('vaadin-side-nav[theme~="rail"]');
    });

    test('opens popover on hover for a leaf item', async ({ page }) => {
        const dashboard = page.locator('vaadin-side-nav-item').filter({ hasText: 'Dashboard' }).first();
        await dashboard.hover();
        await expect(openPopover(page)).toBeVisible();
        await expect(popoverDescendant(page, ':text("Dashboard")')).toBeVisible();
    });

    test('opens popover on hover for an item with children', async ({ page }) => {
        const code = page.locator('vaadin-side-nav-item').filter({ hasText: 'Code' }).first();
        await code.hover();
        await expect(openPopover(page)).toBeVisible();
        await expect(popoverDescendant(page, ':text("Code")')).toBeVisible();
        // Children are also rendered:
        await expect(popoverDescendant(page, ':text("Branches")')).toBeVisible();
    });

    test('opens popover on keyboard focus for a leaf item', async ({ page }) => {
        // Tab into the rail until Dashboard is focused.
        await page.keyboard.press('Tab');
        // Allow up to 5 tabs to reach Dashboard (depends on initial focus position):
        for (let i = 0; i < 5; i++) {
            const focused = await page.evaluate(() =>
                (document.activeElement?.textContent ?? '').includes('Dashboard'));
            if (focused) break;
            await page.keyboard.press('Tab');
        }
        await expect(openPopover(page)).toBeVisible();
    });

    test('does not show popover on a leaf item in normal mode', async ({ page }) => {
        await page.locator('#toggle-rail').click();
        const dashboard = page.locator('vaadin-side-nav-item').filter({ hasText: 'Dashboard' }).first();
        await dashboard.hover();
        // Wait the hover delay so a popover-if-it-were-coming would have opened by now:
        await page.waitForTimeout(400);
        await expect(openPopover(page)).toHaveCount(0);
    });

    test('does not set data-rail-tooltip or title on root items in POPOVER mode', async ({ page }) => {
        const dashboard = page.locator('vaadin-side-nav-item').filter({ hasText: 'Dashboard' }).first();
        await expect(dashboard).not.toHaveAttribute('data-rail-tooltip', /.*/);
        await expect(dashboard).not.toHaveAttribute('title', /.*/);
    });

    test('header switches to icon-only when mode is ICON_ONLY', async ({ page }) => {
        await page.locator('#header-icon-only').click();
        const dashboard = page.locator('vaadin-side-nav-item').filter({ hasText: 'Dashboard' }).first();
        await dashboard.hover();
        await expect(openPopover(page)).toBeVisible();
        // ICON_ONLY: no text content from the label.
        const popoverText = await popoverDescendant(page, '*').first().innerText().catch(() => '');
        expect(popoverText.trim()).not.toContain('Dashboard');
    });
});
```

(Verify `popoverDescendant` and `openPopover` signatures match the existing helpers in `e2e/src/test/playwright/lib/popover.ts`. If the helper takes a CSS string only and exposes the dual-form selector internally, adjust the call signature accordingly.)

- [ ] **Step 6.3: Run the Playwright spec on V24**

```bash
./test-v24.sh --e2e -- --grep 'RailTooltipMode.POPOVER' 2>&1 | tee /tmp/task6-v24.log
```

(If the test runner script does not support `--grep` passthrough, run the full e2e test phase and filter the log. Adjust to whatever invocation pattern the project uses for running a single Playwright spec.)

Expected: all six new tests pass.

- [ ] **Step 6.4: Run the Playwright spec on V25**

```bash
./test-v25.sh --e2e -- --grep 'RailTooltipMode.POPOVER' 2>&1 | tee /tmp/task6-v25.log
```

Expected: all six new tests pass.

- [ ] **Step 6.5: Commit**

```bash
git add -A
git commit -m "$(cat <<'EOF'
test(e2e): rail-tooltip-popover spec for RailTooltipMode.POPOVER

New view + spec covering:
 - leaf hover opens popover with header
 - parent hover opens popover with header + children
 - keyboard focus opens popover
 - normal mode leaves no popover on leaves
 - POPOVER mode does not set data-rail-tooltip / title attributes
 - PopoverHeaderMode change reflects in the open popover

Verified on V24 and V25.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 7: Demo update (`MainLayout`)

**Files:**
- Modify: `demo/src/main/java/org/vaadin/addons/componentfactory/sidenavrail/demo/MainLayout.java`

- [ ] **Step 7.1: Locate the tooltip-mode select in `MainLayout.java`**

```bash
grep -n 'RailTooltipMode\|setRailTooltipNative' /workspace/demo/src/main/java/org/vaadin/addons/componentfactory/sidenavrail/demo/MainLayout.java
```

Identify the `Select<RailTooltipMode>` (or equivalent) and any boolean toggle for "native tooltip".

- [ ] **Step 7.2: Replace the tooltip controls**

Drop the native-tooltip toggle. Update the tooltip-mode select to expose the four new values:

```java
Select<RailTooltipMode> tooltipMode = new Select<>();
tooltipMode.setLabel("Rail tooltip mode");
tooltipMode.setItems(RailTooltipMode.values());
tooltipMode.setValue(rail.getRailTooltipMode());
tooltipMode.addValueChangeListener(e -> rail.setRailTooltipMode(e.getValue()));
```

Add the constraint enforcement. When `tooltipMode == POPOVER`, the demo must not allow `headerMode == NONE` (since the auto-coerce would silently flip it). Two approaches; pick whichever fits the existing demo patterns:

**Approach A — disable the offending option:**

```java
Select<PopoverHeaderMode> headerMode = new Select<>();
headerMode.setLabel("Popover header mode");
headerMode.setItems(PopoverHeaderMode.values());
headerMode.setValue(rail.getPopoverHeaderMode());
headerMode.addValueChangeListener(e -> rail.setPopoverHeaderMode(e.getValue()));

tooltipMode.addValueChangeListener(e -> {
    boolean popoverActive = e.getValue() == RailTooltipMode.POPOVER;
    headerMode.setItemEnabledProvider(m -> !(popoverActive && m == PopoverHeaderMode.NONE));
    if (popoverActive && headerMode.getValue() == PopoverHeaderMode.NONE) {
        headerMode.setValue(PopoverHeaderMode.LABEL_ONLY);
    }
});
```

(The exact `setItemEnabledProvider` signature on Vaadin `Select` may differ — verify against the version in use; if not available, switch to Approach B.)

**Approach B — auto-snap on select:**

```java
tooltipMode.addValueChangeListener(e -> {
    if (e.getValue() == RailTooltipMode.POPOVER
            && headerMode.getValue() == PopoverHeaderMode.NONE) {
        headerMode.setValue(PopoverHeaderMode.LABEL_ONLY);
    }
});
headerMode.addValueChangeListener(e -> {
    if (tooltipMode.getValue() == RailTooltipMode.POPOVER
            && e.getValue() == PopoverHeaderMode.NONE) {
        headerMode.setValue(PopoverHeaderMode.LABEL_ONLY);  // snap back
    }
});
```

- [ ] **Step 7.3: Build the demo and start it manually**

```bash
./mvnw -pl demo -am compile 2>&1 | tee /tmp/task7-compile.log
```

Expected: BUILD SUCCESS.

Manual smoke check:

```bash
./server-start.sh
```

Open `http://localhost:8081/`, switch the tooltip mode to each of the four values, switch the header mode, verify the constraint is enforced (you cannot end up with `POPOVER` + `NONE`). Stop the server:

```bash
./server-stop.sh
```

(Note the user/assistant cannot directly view localhost from the devcontainer; if a screenshot is needed, take one via the housekeeper-managed Playwright path. For most reviewers, compile-clean + working unit/E2E tests is sufficient evidence.)

- [ ] **Step 7.4: Commit**

```bash
git add -A
git commit -m "$(cat <<'EOF'
chore(demo): expose RailTooltipMode.POPOVER, drop native-tooltip toggle

Tooltip-mode select now offers all four values (NONE / BROWSER_NATIVE /
STYLED / POPOVER). Adds constraint enforcement so POPOVER + HeaderMode.
NONE cannot be selected together (matches the addon's attach-time
auto-coerce, but produces no surprise jump for the user).

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 8: Documentation pass (README + CLAUDE.md + remaining JavaDoc)

**Files:**
- Modify: `README.md`
- Modify: `CLAUDE.md`
- (JavaDoc in `RailTooltipMode.java` and `PopoverHeaderMode.java` was updated in Tasks 1–2; this task only adds cross-references)

- [ ] **Step 8.1: Update `README.md`**

Locate the section that documents the rail-tooltip / popover-header API. Rewrite the relevant paragraphs to:

1. Mention `RailTooltipMode.POPOVER` as a separate mode that uses the popover instead of an attribute-based tooltip.
2. Show a small usage example combining `setRailTooltipMode(RailTooltipMode.POPOVER)` with `setPopoverHeaderMode(PopoverHeaderMode.LABEL_ONLY)`.
3. Document the attach-time auto-coerce (`POPOVER` + `NONE` ⇒ `LABEL_ONLY`).
4. Drop all references to `setRailTooltipNative`, `RailTooltipMode.ALL`, `RailTooltipMode.ONLY_WITHOUT_CHILDREN`, and `PopoverParentLabelMode`.
5. Update the cross-version testing note if it references any removed identifier.

Example snippet:

```markdown
### Tooltip modes

`RailTooltipMode` controls how a rail-mode root item is identified to the user:

| Mode | What it does |
|---|---|
| `NONE` | No tooltip. |
| `BROWSER_NATIVE` | Browser-native `title` attribute. Hover-only. |
| `STYLED` | Lumo-themed CSS pseudo-element. Reacts to hover and focus. **Default.** |
| `POPOVER` | A `Popover` with the configured `PopoverHeaderMode` is the tooltip. Reacts to hover and focus. |

When using `POPOVER`, set a non-`NONE` `PopoverHeaderMode` — otherwise the popover would have no content. If you don't, the rail silently coerces it to `LABEL_ONLY` on attach.

```java
rail.setRailTooltipMode(RailTooltipMode.POPOVER);
rail.setPopoverHeaderMode(PopoverHeaderMode.LABEL_ONLY);
```
```

- [ ] **Step 8.2: Update `CLAUDE.md`**

In the "Public API surface highlights" block at the top of `CLAUDE.md`, replace the obsolete lines:

- Drop the `RailTooltipMode` line that mentions `ONLY_WITHOUT_CHILDREN` / `ALL` / `setRailTooltipNative`.
- Drop the `PopoverParentLabelMode` line.

Insert in their place:

```markdown
- `RailTooltipMode` (`NONE` / `BROWSER_NATIVE` / `STYLED` (default) / `POPOVER`) — `POPOVER` reuses the popover as a Vaadin-themed tooltip; auto-coerces `PopoverHeaderMode.NONE` to `LABEL_ONLY` at attach
- `PopoverHeaderMode` (popover header: `NONE` / `LABEL_ONLY` / `ICON_ONLY` / `FULL`) + `setPopoverHeaderOnlyInRailMode` (default `true`; opt out to render the header in normal mode too, e.g. with `setChildrenOnlyInPopover`)
```

- [ ] **Step 8.3: Verify nothing else stale**

```bash
grep -rln 'PopoverParentLabelMode\|setRailTooltipNative\|RailTooltipMode\.ALL\|RailTooltipMode\.ONLY_WITHOUT_CHILDREN' /workspace --include='*.java' --include='*.ts' --include='*.md' 2>/dev/null | grep -v 'plans/2026-04-2[1-7]\|specs/2026-04-2[1-7]'
```

Expected: empty output. If anything besides historical plan/spec docs (which we deliberately leave untouched) shows up, fix it.

- [ ] **Step 8.4: Final full build + tests on V24**

```bash
./test-v24.sh 2>&1 | tee /tmp/task8-v24.log
```

Expected: BUILD SUCCESS. Counts in the summary should reflect the new tests:
- Addon unit tests: ~99 (existing) + ~7 new = ~106. Adjust expectation as actually counted.
- E2E tests: ~65 (existing) + 6 new = ~71.

- [ ] **Step 8.5: Final full build + tests on V25**

```bash
./test-v25.sh 2>&1 | tee /tmp/task8-v25.log
```

Expected: BUILD SUCCESS. Same counts.

- [ ] **Step 8.6: Commit**

```bash
git add -A
git commit -m "$(cat <<'EOF'
docs: tooltip / popover merge — README + CLAUDE.md

README documents the four RailTooltipMode values, the POPOVER mode +
PopoverHeaderMode interaction, and the attach-time auto-coerce.
CLAUDE.md's API surface highlights are refreshed.

Verified on V24 and V25 (all unit + E2E tests pass).

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## After all tasks

Branch state should be: 8 commits on top of `main`, full V24 + V25 test runs green, no stale references to the removed/renamed API. Ready for `superpowers:finishing-a-development-branch` (PR or merge decision).
