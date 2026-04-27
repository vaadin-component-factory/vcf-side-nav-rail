## Implementation status

**Done (2026-04-27).** All 13 tasks complete. Addon at `1.0.0-SNAPSHOT`; `./mvnw -B -Pdirectory clean install` green across the four reactor modules. The upload ZIP `vcf-side-nav-rail-1.0.0-SNAPSHOT.zip` contains addon JAR + sources + javadoc + LICENSE + README, with the Vaadin Directory headers interpolated correctly in `META-INF/MANIFEST.MF`. README + RELEASING.md present at the repo root. Build (`build.yml`) and release (`release.yml`) GitHub Actions workflows in place. `ScreenshotView` in the demo plus six contrast-composite screenshots in `docs/screenshots/` driving the README walkthrough.

Test counts on this branch:
- 82 addon unit tests
- e2e module: 1 Karibu UI test + 56 Playwright tests (52 from §9.4 + the keyboard-tooltip test added on this branch)

Deviations from the plan-as-written:
- **Spec/plan §3.1 `<manifestFile>` placement.** The spec attached the manifest to `maven-assembly-plugin`. ZIPs do not surface a manifest in a way Vaadin Directory can read. The user's directory profile additions instead use a filtered `META-INF/MANIFEST.MF` written *into* the ZIP via the assembly descriptor (`assembly/assembly.xml`) plus a profile-scoped `maven-jar-plugin` reconfig that adds `Vaadin-Package-Version` to the inner JAR's manifest. Cleaner — Vaadin Directory reads the ZIP manifest, the inner JAR has standard Maven defaults plus the package-version header.
- **`Implementation-Vendor: n/a` on the inner JAR.** The directory-profile `<manifestEntries>` carries a literal `n/a` vendor (template-derived) which overrides the auto-derived `Vaadin Ltd.` from `<organization>`. Cosmetic only — Vaadin Directory reads the ZIP manifest where the value is correctly interpolated.
- **Visual polish follow-ups.** Item label alignment and the rail-icon styling when an ancestor is current (vs the leaf itself being current) are deliberately deferred. See `plans/followups.md`.

The first `1.0.0` release itself (the act of bumping the POM, tagging, uploading) is **not** part of this plan — it is executed by the maintainer per `RELEASING.md` once this branch lands on `main`.

---

# Publishing to Vaadin Directory Implementation Plan (§9.5)

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Prepare the SideNav Rail addon for publishing on [vaadin.com/directory](https://vaadin.com/directory) — POM metadata, JAR-manifest headers, assembly profile that produces the upload ZIP, GitHub Actions for build + release, and the maintainer-facing docs (README + RELEASING.md) that frame how to consume the addon and how to cut subsequent releases.

**Architecture:** The addon JAR carries the Vaadin Directory headers in its own manifest (read by `maven-jar-plugin` from `addon/assembly/MANIFEST.MF`). A Maven profile `directory` activates `maven-assembly-plugin` to wrap the addon JAR + sources JAR + javadoc JAR into a single upload ZIP. CI runs `./mvnw clean verify` on PRs and `main` pushes; a release workflow on `v*` tags builds the upload ZIP and creates a *draft* GitHub Release with the artifacts attached + auto-generated release notes — the maintainer edits and publishes manually. README screenshots come from a dedicated Showcase view in the existing `demo/` module.

**Tech Stack:** Java 17 / Maven (addon = Vaadin 24.9 floor); GitHub Actions; Playwright (screenshot capture only); no new runtime dependencies.

**Authoritative references:**
- Spec: `specs/2026-04-27-publishing-design.md`
- Project conventions: `CLAUDE.md` (build commands, server scripts, agent registry)
- Existing addon POM: `addon/pom.xml` (current state — `0.1.0-SNAPSHOT`, no SCM, no source/javadoc plugins, no directory profile)

**Note on a small spec/implementation drift:** Spec §3.1 attaches `<manifestFile>` to `maven-assembly-plugin`. ZIP archives do not surface a manifest in a way Vaadin Directory can read; only JAR archives do. This plan therefore attaches the manifest to `maven-jar-plugin` (the addon JAR build) instead. Same `assembly/MANIFEST.MF` file, same headers — only the plugin that consumes the file changes. Documented here so the spec and code stay in sync; the spec can be amended in a follow-up commit.

---

## File Structure

| File | Status | Owns |
|---|---|---|
| `addon/pom.xml` | modify | Project metadata, version, build plugins, `directory` profile. |
| `addon/assembly/MANIFEST.MF` | new | Vaadin Directory manifest headers, applied to addon JAR via `maven-jar-plugin`. |
| `addon/assembly/assembly.xml` | new | `maven-assembly-plugin` descriptor for the upload ZIP. |
| `.github/workflows/build.yml` | new | PR + `main`-push CI: `./mvnw clean verify`. |
| `.github/workflows/release.yml` | new | Tag-triggered release pipeline: build ZIP, create draft GitHub Release. |
| `demo/src/main/java/org/vaadin/addons/componentfactory/sidenavrail/demo/ShowcaseView.java` | new | Screenshot-friendly demo view (`/showcase`). |
| `docs/screenshots/rail-off.png` | new | README screenshot (rail mode off). |
| `docs/screenshots/rail-on.png` | new | README screenshot (rail mode on, popover open). |
| `README.md` | new | Top-level addon README (replaces nothing — devcontainer-template README is gone). |
| `RELEASING.md` | new | 11-step maintainer checklist. |

The `addon/assembly/` directory and `docs/screenshots/` directory do not exist yet — they are created implicitly by the file writes.

---

## Task 1: Bump addon version + add project metadata

**Files:**
- Modify: `addon/pom.xml:11` (version), `addon/pom.xml:22-23` (insert `<scm>` / `<organization>` / `<developers>` after `<licenses>`)

- [ ] **Step 1: Bump `<version>` to `1.0.0-SNAPSHOT`**

In `addon/pom.xml`, replace the line:

```xml
    <version>0.1.0-SNAPSHOT</version>
```

with:

```xml
    <version>1.0.0-SNAPSHOT</version>
```

- [ ] **Step 2: Add `<scm>`, `<organization>`, `<developers>` after `<licenses>`**

In `addon/pom.xml`, insert these blocks immediately after the closing `</licenses>` tag (which is at line 22 in the current file):

```xml
    <scm>
        <connection>scm:git:git@github.com:vaadin-component-factory/side-nav-rail.git</connection>
        <developerConnection>scm:git:git@github.com:vaadin-component-factory/side-nav-rail.git</developerConnection>
        <url>https://github.com/vaadin-component-factory/side-nav-rail</url>
        <tag>HEAD</tag>
    </scm>

    <organization>
        <name>Vaadin Ltd.</name>
        <url>https://vaadin.com</url>
    </organization>

    <developers>
        <developer>
            <name>Vaadin Component Factory</name>
            <email>componentfactory@vaadin.com</email>
            <organization>Vaadin Ltd.</organization>
            <organizationUrl>https://vaadin.com</organizationUrl>
        </developer>
    </developers>
```

- [ ] **Step 3: Verify the POM still parses**

Run: `./mvnw -pl addon help:effective-pom -q | head -50`

Expected: prints the effective POM with the new metadata, exits 0. No XML parse errors.

- [ ] **Step 4: Run the addon unit tests**

Run: `./mvnw -pl addon test`

Expected: `BUILD SUCCESS`. Existing 81 tests still green; the metadata change has no behavioural impact.

- [ ] **Step 5: Commit**

```bash
git add addon/pom.xml
git commit -m "chore(addon): bump to 1.0.0-SNAPSHOT, add publishing metadata"
```

---

## Task 2: Add source + javadoc plugins to addon POM

**Files:**
- Modify: `addon/pom.xml:65-78` (insert two `<plugin>` blocks inside `<build><plugins>`)

- [ ] **Step 1: Insert `maven-source-plugin` block inside `<build><plugins>`**

In `addon/pom.xml`, add this `<plugin>` block immediately after the `maven-surefire-plugin` block (which ends with `</plugin>` around line 76):

```xml
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-source-plugin</artifactId>
                <version>3.3.1</version>
                <executions>
                    <execution>
                        <id>attach-sources</id>
                        <goals>
                            <goal>jar-no-fork</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
```

- [ ] **Step 2: Insert `maven-javadoc-plugin` block right after**

```xml
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-javadoc-plugin</artifactId>
                <version>3.10.1</version>
                <configuration>
                    <doclint>none</doclint>
                    <quiet>true</quiet>
                </configuration>
                <executions>
                    <execution>
                        <id>attach-javadocs</id>
                        <goals>
                            <goal>jar</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
```

`<doclint>none</doclint>` keeps existing javadoc comments from blocking the release if they have `@param` mismatches or similar. `<quiet>true</quiet>` suppresses noisy per-class progress output.

- [ ] **Step 3: Run a `package` build and verify the artifacts appear**

Run: `./mvnw -pl addon clean package -DskipTests`

Expected: `BUILD SUCCESS`, plus three JARs in `addon/target/`:

```bash
ls addon/target/vcf-side-nav-rail-1.0.0-SNAPSHOT*.jar
```

Expected output (three lines):
```
addon/target/vcf-side-nav-rail-1.0.0-SNAPSHOT-javadoc.jar
addon/target/vcf-side-nav-rail-1.0.0-SNAPSHOT-sources.jar
addon/target/vcf-side-nav-rail-1.0.0-SNAPSHOT.jar
```

- [ ] **Step 4: Commit**

```bash
git add addon/pom.xml
git commit -m "build(addon): publish sources and javadoc JARs"
```

---

## Task 3: Create the Vaadin Directory manifest file

**Files:**
- Create: `addon/assembly/MANIFEST.MF`

- [ ] **Step 1: Create the manifest file with Vaadin Directory headers**

```bash
mkdir -p addon/assembly
```

Then write `addon/assembly/MANIFEST.MF` with exactly this content (note the **trailing blank line** — JAR manifest format requires it):

```
Manifest-Version: 1.0
Vaadin-Package-Version: 1
Implementation-Title: SideNav Rail
Implementation-Version: ${project.version}
Implementation-Vendor: Vaadin Ltd.
Vaadin-Addon: vcf-side-nav-rail-${project.version}.jar

```

`${project.version}` is interpolated by `maven-jar-plugin` at build time (configured in Task 4).

- [ ] **Step 2: Verify the file ends with a blank line**

Run: `tail -c 2 addon/assembly/MANIFEST.MF | xxd`

Expected output: shows two `0a` bytes (i.e., `\n\n`) at the end — `0a 0a` in the hex column. If only one newline is present, the manifest will fail to parse. Append another `\n` if needed:

```bash
[ "$(tail -c 1 addon/assembly/MANIFEST.MF | xxd -p)" = "0a" ] && [ "$(tail -c 2 addon/assembly/MANIFEST.MF | head -c 1 | xxd -p)" != "0a" ] && printf '\n' >> addon/assembly/MANIFEST.MF
```

- [ ] **Step 3: Commit**

```bash
git add addon/assembly/MANIFEST.MF
git commit -m "build(addon): add Vaadin Directory manifest"
```

---

## Task 4: Wire the manifest into the addon JAR

**Files:**
- Modify: `addon/pom.xml` (insert `maven-jar-plugin` block inside `<build><plugins>`, before the source plugin from Task 2)

- [ ] **Step 1: Insert `maven-jar-plugin` block configured to read the manifest**

In `addon/pom.xml`, add this `<plugin>` block immediately before the `maven-source-plugin` block from Task 2:

```xml
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-jar-plugin</artifactId>
                <version>3.4.1</version>
                <configuration>
                    <archive>
                        <manifestFile>${project.basedir}/assembly/MANIFEST.MF</manifestFile>
                    </archive>
                </configuration>
            </plugin>
```

- [ ] **Step 2: Build the JAR and inspect its manifest**

Run:
```bash
./mvnw -pl addon clean package -DskipTests
unzip -p addon/target/vcf-side-nav-rail-1.0.0-SNAPSHOT.jar META-INF/MANIFEST.MF
```

Expected output (the build interpolated `${project.version}` to `1.0.0-SNAPSHOT`):

```
Manifest-Version: 1.0
Vaadin-Package-Version: 1
Implementation-Title: SideNav Rail
Implementation-Version: 1.0.0-SNAPSHOT
Implementation-Vendor: Vaadin Ltd.
Vaadin-Addon: vcf-side-nav-rail-1.0.0-SNAPSHOT.jar
```

(Maven also adds standard headers like `Created-By` and `Build-Jdk-Spec`; those are expected.)

- [ ] **Step 3: Commit**

```bash
git add addon/pom.xml
git commit -m "build(addon): apply Vaadin Directory headers to addon JAR manifest"
```

---

## Task 5: Create the assembly descriptor

**Files:**
- Create: `addon/assembly/assembly.xml`

- [ ] **Step 1: Create the assembly descriptor**

Write `addon/assembly/assembly.xml`:

```xml
<assembly xmlns="http://maven.apache.org/ASSEMBLY/2.1.1"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/ASSEMBLY/2.1.1 https://maven.apache.org/xsd/assembly-2.1.1.xsd">
    <id>directory</id>
    <formats>
        <format>zip</format>
    </formats>
    <includeBaseDirectory>false</includeBaseDirectory>
    <files>
        <file>
            <source>${project.build.directory}/${project.build.finalName}.jar</source>
            <outputDirectory>/</outputDirectory>
        </file>
        <file>
            <source>${project.build.directory}/${project.build.finalName}-sources.jar</source>
            <outputDirectory>/</outputDirectory>
        </file>
        <file>
            <source>${project.build.directory}/${project.build.finalName}-javadoc.jar</source>
            <outputDirectory>/</outputDirectory>
        </file>
    </files>
</assembly>
```

The descriptor produces a single ZIP containing the addon JAR + sources JAR + javadoc JAR at the ZIP's root.

- [ ] **Step 2: Commit**

```bash
git add addon/assembly/assembly.xml
git commit -m "build(addon): add assembly descriptor for directory ZIP"
```

---

## Task 6: Add the `directory` Maven profile

**Files:**
- Modify: `addon/pom.xml` (insert `<profiles>` block after `</build>`)

- [ ] **Step 1: Append the `<profiles>` block at the end of the project, just before `</project>`**

In `addon/pom.xml`, immediately after the closing `</build>` tag, add:

```xml
    <profiles>
        <profile>
            <id>directory</id>
            <build>
                <plugins>
                    <plugin>
                        <groupId>org.apache.maven.plugins</groupId>
                        <artifactId>maven-assembly-plugin</artifactId>
                        <version>3.7.1</version>
                        <executions>
                            <execution>
                                <id>directory-bundle</id>
                                <phase>package</phase>
                                <goals>
                                    <goal>single</goal>
                                </goals>
                                <configuration>
                                    <descriptors>
                                        <descriptor>assembly/assembly.xml</descriptor>
                                    </descriptors>
                                </configuration>
                            </execution>
                        </executions>
                    </plugin>
                </plugins>
            </build>
        </profile>
    </profiles>
```

The profile is opt-in — it runs only when `-Pdirectory` is passed.

- [ ] **Step 2: Build with the profile and verify the ZIP**

Run:
```bash
./mvnw -pl addon -Pdirectory clean package -DskipTests
ls addon/target/vcf-side-nav-rail-1.0.0-SNAPSHOT-directory.zip
unzip -l addon/target/vcf-side-nav-rail-1.0.0-SNAPSHOT-directory.zip
```

Expected `unzip -l` output (three entries, ZIP at the root, no subdirectories):

```
Archive:  addon/target/vcf-side-nav-rail-1.0.0-SNAPSHOT-directory.zip
  Length      Date    Time    Name
---------  ---------- -----   ----
   <size>  YYYY-MM-DD HH:MM   vcf-side-nav-rail-1.0.0-SNAPSHOT.jar
   <size>  YYYY-MM-DD HH:MM   vcf-side-nav-rail-1.0.0-SNAPSHOT-sources.jar
   <size>  YYYY-MM-DD HH:MM   vcf-side-nav-rail-1.0.0-SNAPSHOT-javadoc.jar
---------                     -------
```

- [ ] **Step 3: Verify the addon JAR inside the ZIP still has the Vaadin headers**

```bash
mkdir -p /tmp/dir-zip-check && cd /tmp/dir-zip-check && \
  unzip -o /workspace/addon/target/vcf-side-nav-rail-1.0.0-SNAPSHOT-directory.zip && \
  unzip -p vcf-side-nav-rail-1.0.0-SNAPSHOT.jar META-INF/MANIFEST.MF | grep "Vaadin-"
```

Expected output:
```
Vaadin-Package-Version: 1
Vaadin-Addon: vcf-side-nav-rail-1.0.0-SNAPSHOT.jar
```

- [ ] **Step 4: Commit**

```bash
git add addon/pom.xml
git commit -m "build(addon): add directory profile producing upload ZIP"
```

---

## Task 7: GitHub Actions — build workflow

**Files:**
- Create: `.github/workflows/build.yml`

- [ ] **Step 1: Create the workflow file**

```bash
mkdir -p .github/workflows
```

Then write `.github/workflows/build.yml`:

```yaml
name: Build

on:
  push:
    branches: [main]
  pull_request:
    branches: [main]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - name: Checkout
        uses: actions/checkout@v4

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '17'
          cache: maven

      - name: Cache Playwright browsers
        uses: actions/cache@v4
        with:
          path: ~/.cache/ms-playwright
          key: playwright-${{ runner.os }}-${{ hashFiles('e2e/src/test/playwright/package-lock.json') }}

      - name: Verify
        run: ./mvnw -B clean verify
```

`./mvnw verify` runs the full pipeline — addon unit tests, e2e module's Karibu UI tests, and the production-mode Playwright suite.

- [ ] **Step 2: Verify YAML parses (offline syntax check via Python)**

Run:
```bash
python3 -c "import yaml; yaml.safe_load(open('.github/workflows/build.yml'))" && echo OK
```

Expected: `OK`. Any YAML error would print a traceback.

- [ ] **Step 3: Commit**

```bash
git add .github/workflows/build.yml
git commit -m "ci: add build workflow for PRs and main pushes"
```

---

## Task 8: GitHub Actions — release workflow

**Files:**
- Create: `.github/workflows/release.yml`

- [ ] **Step 1: Create the workflow file**

Write `.github/workflows/release.yml`:

```yaml
name: Release

on:
  push:
    tags:
      - 'v*'

permissions:
  contents: write

jobs:
  release:
    runs-on: ubuntu-latest
    steps:
      - name: Checkout
        uses: actions/checkout@v4

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '17'
          cache: maven

      - name: Cache Playwright browsers
        uses: actions/cache@v4
        with:
          path: ~/.cache/ms-playwright
          key: playwright-${{ runner.os }}-${{ hashFiles('e2e/src/test/playwright/package-lock.json') }}

      - name: Build directory ZIP + verify
        run: ./mvnw -B -Pdirectory clean verify

      - name: Create draft GitHub Release
        uses: softprops/action-gh-release@v2
        with:
          draft: true
          generate_release_notes: true
          files: |
            addon/target/vcf-side-nav-rail-*-directory.zip
            addon/target/vcf-side-nav-rail-*-sources.jar
            addon/target/vcf-side-nav-rail-*-javadoc.jar
```

`draft: true` means the release is created but not visible to consumers until a maintainer clicks Publish (RELEASING.md step 6). `generate_release_notes: true` populates the body with auto-generated notes from PRs/commits since the previous tag — a starting point for the maintainer to edit.

- [ ] **Step 2: Verify YAML parses**

Run:
```bash
python3 -c "import yaml; yaml.safe_load(open('.github/workflows/release.yml'))" && echo OK
```

Expected: `OK`.

- [ ] **Step 3: Commit**

```bash
git add .github/workflows/release.yml
git commit -m "ci: add release workflow for v* tags"
```

---

## Task 9: Showcase view in `demo/` for screenshots

**Files:**
- Create: `demo/src/main/java/org/vaadin/addons/componentfactory/sidenavrail/demo/ShowcaseView.java`

- [ ] **Step 1: Create the showcase view**

Write `demo/src/main/java/org/vaadin/addons/componentfactory/sidenavrail/demo/ShowcaseView.java`:

```java
/*
 * Copyright 2026 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */
package org.vaadin.addons.componentfactory.sidenavrail.demo;

import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import org.vaadin.addons.componentfactory.sidenavrail.SideNavRail;
import org.vaadin.addons.componentfactory.sidenavrail.SideNavRailItem;

/**
 * Screenshot-friendly composition of {@link SideNavRail} for the README. Self-contained
 * (no toggle button, no debug controls) so the captured frame is clean and consistent
 * across re-captures.
 */
@Route("showcase")
public class ShowcaseView extends HorizontalLayout {

    public ShowcaseView() {
        setPadding(false);
        setSpacing(false);
        setSizeFull();

        SideNavRail rail = new SideNavRail();
        rail.setId("showcase-rail");
        rail.addItem(
                new SideNavRailItem("Dashboard", "/dashboard", VaadinIcon.DASHBOARD.create()),
                codeSection(),
                new SideNavRailItem("Reports", "/reports", VaadinIcon.CHART.create()),
                new SideNavRailItem("Settings", "/settings", VaadinIcon.COG.create()));

        VerticalLayout content = new VerticalLayout();
        content.setSizeFull();
        content.getStyle().set("background", "var(--lumo-shade-5pct)");

        add(rail, content);
        setFlexGrow(0, rail);
        setFlexGrow(1, content);
    }

    private static SideNavRailItem codeSection() {
        SideNavRailItem code = new SideNavRailItem("Code", "/code", VaadinIcon.CODE.create());
        code.addItem(new SideNavRailItem("Branches", "/code/branches"));
        code.addItem(new SideNavRailItem("Pull requests", "/code/pulls"));
        code.addItem(new SideNavRailItem("Releases", "/code/releases"));
        return code;
    }
}
```

- [ ] **Step 2: Compile the demo module**

Run: `./mvnw -pl demo compile`

Expected: `BUILD SUCCESS`.

- [ ] **Step 3: Commit**

```bash
git add demo/src/main/java/org/vaadin/addons/componentfactory/sidenavrail/demo/ShowcaseView.java
git commit -m "demo: add ShowcaseView for README screenshots"
```

---

## Task 10: Capture README screenshots

**Files:**
- Create: `docs/screenshots/rail-off.png`, `docs/screenshots/rail-on.png`

- [ ] **Step 1: Start the demo in dev mode**

Run (background):
```bash
./mvnw -pl demo spring-boot:run -Dspring-boot.run.fork=false > /tmp/demo-server.log 2>&1 &
echo $! > /tmp/demo-server.pid
```

Wait for readiness (poll `http://localhost:8080/showcase` until HTTP 200, up to 120 s):
```bash
until curl -sSf -o /dev/null http://localhost:8080/showcase; do sleep 3; done; echo "ready"
```

Expected: `ready`.

- [ ] **Step 2: Write the screenshot capture script**

Write `/tmp/capture-screenshots.cjs`:

```javascript
const { chromium } = require('playwright');

(async () => {
  const browser = await chromium.launch();
  const context = await browser.newContext({
    viewport: { width: 1280, height: 720 },
    deviceScaleFactor: 2,  // retina-quality screenshots
  });
  const page = await context.newPage();

  // Rail OFF (default state — full-width nav with labels)
  await page.goto('http://localhost:8080/showcase');
  await page.waitForSelector('vaadin-side-nav-item[path="dashboard"]');
  await page.waitForTimeout(500);  // settle Lumo transitions
  await page.screenshot({ path: '/workspace/docs/screenshots/rail-off.png', fullPage: false });

  // Rail ON with popover open on Code
  await page.evaluate(() => {
    const rail = document.querySelector('vaadin-side-nav#showcase-rail');
    rail.setAttribute('theme', 'rail');
  });
  await page.waitForTimeout(500);
  // Hover the Code item to open its popover
  await page.locator('vaadin-side-nav-item[path="code"]').hover();
  await page.waitForSelector('vaadin-popover-overlay[opened]', { timeout: 5000 });
  await page.waitForTimeout(800);  // hover delay + fade
  await page.screenshot({ path: '/workspace/docs/screenshots/rail-on.png', fullPage: false });

  await browser.close();
})();
```

The script flips rail mode by setting the `theme="rail"` attribute directly — that bypasses the (deliberately not-rendered) toggle button and gives a consistent state for the screenshot.

- [ ] **Step 3: Capture the screenshots**

```bash
mkdir -p /workspace/docs/screenshots
cd /workspace/e2e && node /tmp/capture-screenshots.cjs
```

Expected: no console output; both PNG files exist in `/workspace/docs/screenshots/`.

Verify:
```bash
file /workspace/docs/screenshots/rail-off.png /workspace/docs/screenshots/rail-on.png
```

Expected (both lines): `PNG image data, 2560 x 1440, 8-bit/color RGBA, non-interlaced` (or similar — width should be 2× the 1280 viewport because of `deviceScaleFactor: 2`).

- [ ] **Step 4: Stop the demo server**

```bash
kill "$(cat /tmp/demo-server.pid)" 2>/dev/null
```

- [ ] **Step 5: Commit**

```bash
git add docs/screenshots/rail-off.png docs/screenshots/rail-on.png
git commit -m "docs: add README screenshots from ShowcaseView"
```

---

## Task 11: README.md

**Files:**
- Create: `README.md` (top-level)

- [ ] **Step 1: Write the README**

Write `/workspace/README.md`:

````markdown
# SideNav Rail

A Vaadin Component Factory addon that adds a togglable rail mode to `<vaadin-side-nav>` — collapsed icon-only navigation with on-demand hover popovers, full keyboard support, and Lumo-styled tooltips.

[![Vaadin Directory](https://img.shields.io/vaadin-directory/v/vcf-side-nav-rail.svg)](https://vaadin.com/directory/component/vcf-side-nav-rail)

![Rail mode off](docs/screenshots/rail-off.png)
![Rail mode on with popover](docs/screenshots/rail-on.png)

## Features

- **Rail mode toggle** — flip between full-width and icon-only at runtime via `setRailMode(boolean)`.
- **Hover popovers** for items with children, configurable per item or globally.
- **Full keyboard navigation** in both modes — arrow keys, Enter, Esc, Tab.
- **ARIA contracts** maintained automatically: `aria-haspopup="menu"`, `aria-expanded`, `role="menuitem"` on popover items.
- **CSS pseudo-element tooltips** that coexist with popovers (Vaadin's native tooltip flickers when peer overlays open; ours doesn't).
- **Letter-avatar fallback** for items without an icon — first letter of the label, Lumo-styled.
- **Lifecycle event** (`RailModeChangedEvent`) for downstream code that needs to react to the toggle.

## Compatibility

- Vaadin **24.9** or later
- Java **17** or later

## Installation

Add the dependency to your application's `pom.xml`:

```xml
<dependency>
    <groupId>org.vaadin.addons.componentfactory</groupId>
    <artifactId>vcf-side-nav-rail</artifactId>
    <version>1.0.0</version>
</dependency>
```

## Quick start

```java
SideNavRail rail = new SideNavRail();

SideNavRailItem dashboard = new SideNavRailItem(
        "Dashboard", "/dashboard", VaadinIcon.DASHBOARD.create());
SideNavRailItem code = new SideNavRailItem(
        "Code", "/code", VaadinIcon.CODE.create());
code.addItem(new SideNavRailItem("Branches", "/code/branches"));
code.addItem(new SideNavRailItem("Pull requests", "/code/pulls"));

rail.addItem(dashboard, code);

Button toggle = new Button(VaadinIcon.CHEVRON_LEFT_SMALL.create(),
        e -> rail.setRailMode(!rail.isRailMode()));

add(toggle, rail);
```

## API overview

- **`SideNavRail`** — the navigation container. Subclass of `<vaadin-side-nav>` with rail-mode behaviour layered on top. Configures popover behaviour (`PopoverMode`), tooltip strategy (`RailTooltipMode`), and popover header text (`PopoverParentLabelMode`). Fires `RailModeChangedEvent` on every rail-mode toggle.
- **`SideNavRailItem`** — type-safe `<vaadin-side-nav-item>` extension. Constructors accept `(label)`, `(label, path)`, or `(label, path, icon)`. Items can carry an optional `Avatar` instead of an icon.
- **`PopoverMode`** — controls when popovers render. Default `ON_HOVER`; alternative `ONLY_ROOT_COLLAPSED_ITEMS` shows popovers only for collapsed root items, leaving expanded ones inline.
- **`PopoverParentLabelMode`** — controls whether the parent's label appears as a header inside the popover (`NONE`, `INLINE`, `BOLD`). Default `NONE`.
- **`RailTooltipMode`** — controls which root items get a tooltip in rail mode. Values `NONE`, `ONLY_WITHOUT_CHILDREN`, `ALL`. Default `ALL`. The tooltip is a CSS pseudo-element that fires on both hover and keyboard focus; `setRailTooltipNative(true)` swaps it for the browser-native `title` tooltip.
- **`RailModeChangedEvent`** — `ComponentEvent<SideNavRail>` carrying the new rail-mode boolean.

Full Javadoc is published with each release alongside the addon JAR.

## Building from source

```bash
./mvnw clean verify
```

Runs the addon's unit tests, the Karibu UI tests in `e2e/`, and the production-mode Playwright suite.

## Running the demo

```bash
./mvnw -pl demo spring-boot:run
```

The demo runs on [http://localhost:8080](http://localhost:8080) with hot reload enabled.

## License

Apache 2.0 — see [`LICENSE`](LICENSE).

## Contributing

Bug reports and feature requests are welcome at [github.com/vaadin-component-factory/side-nav-rail/issues](https://github.com/vaadin-component-factory/side-nav-rail/issues). For larger changes, please open an issue first to discuss the approach.
````

- [ ] **Step 2: Verify the README screenshot paths resolve**

```bash
[ -f docs/screenshots/rail-off.png ] && [ -f docs/screenshots/rail-on.png ] && echo OK
```

Expected: `OK`.

- [ ] **Step 3: Commit**

```bash
git add README.md
git commit -m "docs: add addon README"
```

---

## Task 12: RELEASING.md

**Files:**
- Create: `RELEASING.md` (top-level)

- [ ] **Step 1: Write the maintainer checklist**

Write `/workspace/RELEASING.md`:

````markdown
# Releasing

Maintainer-facing checklist for cutting a new release of `vcf-side-nav-rail`. Eleven steps; do not skip.

## Versioning

Strict [SemVer 2.0.0](https://semver.org/spec/v2.0.0.html):

- **Patch** (`1.0.x`) — bug fixes, no API or behavioural change.
- **Minor** (`1.x.0`) — additive changes; existing usage continues to compile and behave the same.
- **Major** (`x.0.0`) — incompatible changes.

The working version on `main` between releases is `X.Y.(Z+1)-SNAPSHOT` (a patch bump). The next change is statistically more likely a fix; if it turns out to be a feature or breaking change, the SNAPSHOT version can be bumped at the next tag instead.

## Steps

1. Edit `addon/pom.xml` `<version>` to `X.Y.Z` (drop `-SNAPSHOT`).

2. Run `./mvnw clean verify` locally — must be green.

3. Commit:
   ```bash
   git add addon/pom.xml
   git commit -m "release: vX.Y.Z"
   git push
   ```

4. Create and push the tag:
   ```bash
   git tag vX.Y.Z
   git push origin vX.Y.Z
   ```
   This triggers the `release.yml` workflow.

5. Wait for the release workflow to finish green. The workflow creates a **draft** GitHub Release with the addon ZIP, sources JAR, and javadoc JAR attached, plus auto-generated release notes filled in from PRs and commits since the previous tag.

6. Open the draft release on GitHub. Edit the auto-generated notes into a human-readable summary, grouping by category (`Added`, `Changed`, `Fixed`, `Security`). Publish the release.

7. Download `vcf-side-nav-rail-X.Y.Z-directory.zip` from the published release.

8. Upload the ZIP at [vaadin.com/directory](https://vaadin.com/directory):
   - First release: log in → "Add new component" → fill in the listing metadata → upload the ZIP.
   - Subsequent releases: open the existing listing → "New version" → upload the ZIP.

9. Verify the new version appears on the Directory listing page; smoke-test the `<dependency>` snippet in a fresh project.

10. Update the `v-herd-demo` branch (see below): merge the released tag and push.
   ```bash
   git checkout v-herd-demo
   git merge --ff-only vX.Y.Z
   git push origin v-herd-demo
   git checkout main
   ```
   For the very first release (`1.0.0`) the branch does not exist yet — create it from the tag instead:
   ```bash
   git branch v-herd-demo v1.0.0
   git push -u origin v-herd-demo
   ```
   The branch is auto-deployed as the live demo, so this **must** happen before step 11. Otherwise the deployed demo would show snapshot code instead of the released version.

11. Bump `addon/pom.xml` to `X.Y.(Z+1)-SNAPSHOT`, commit, push:
    ```bash
    # edit addon/pom.xml: <version>X.Y.(Z+1)-SNAPSHOT</version>
    git add addon/pom.xml
    git commit -m "chore: bump to X.Y.(Z+1)-SNAPSHOT"
    git push
    ```
    If the next planned change is a minor or major bump, set the appropriate version at the next tag rather than now.

## The `v-herd-demo` branch

A long-lived branch tracking the most recently released version of the addon. Vaadin's herd-demo infrastructure auto-deploys whatever sits on this branch, so it must always point at a tagged-and-published version, never at a snapshot.

- The branch only ever advances at release time (step 10 above). No direct commits, no snapshot work lands here.
- For the first `1.0.0` release the branch is created from the `v1.0.0` tag.
````

- [ ] **Step 2: Commit**

```bash
git add RELEASING.md
git commit -m "docs: add release checklist (RELEASING.md)"
```

---

## Task 13: Final verification

**Files:** none modified — this task only runs verifications and records the implementation status.

- [ ] **Step 1: Run the full multi-module verify with the directory profile**

Run: `./mvnw -B -Pdirectory clean verify`

Expected: `BUILD SUCCESS`. Three reactor builds green (addon → e2e/parent → e2e itself). Existing test counts:
- 81 addon unit tests
- e2e module: 1 Karibu UI test + 56 Playwright tests (52 from before + the keyboard-tooltip test added on this branch)

- [ ] **Step 2: Verify the upload ZIP exists and has the expected layout**

```bash
ls addon/target/vcf-side-nav-rail-1.0.0-SNAPSHOT-directory.zip
unzip -l addon/target/vcf-side-nav-rail-1.0.0-SNAPSHOT-directory.zip
```

Expected: ZIP exists; contents are exactly three JARs (addon + sources + javadoc) at the root.

- [ ] **Step 3: Verify the addon JAR's manifest has the Vaadin Directory headers**

```bash
unzip -p addon/target/vcf-side-nav-rail-1.0.0-SNAPSHOT.jar META-INF/MANIFEST.MF | grep -E "Vaadin-|Implementation-"
```

Expected output (in some order):
```
Vaadin-Package-Version: 1
Vaadin-Addon: vcf-side-nav-rail-1.0.0-SNAPSHOT.jar
Implementation-Title: SideNav Rail
Implementation-Version: 1.0.0-SNAPSHOT
Implementation-Vendor: Vaadin Ltd.
```

- [ ] **Step 4: Verify YAML workflows parse**

```bash
python3 -c "import yaml; yaml.safe_load(open('.github/workflows/build.yml')); yaml.safe_load(open('.github/workflows/release.yml'))" && echo OK
```

Expected: `OK`.

- [ ] **Step 5: Add the implementation status block to the top of this plan and commit**

Edit the very top of `plans/2026-04-27-publishing-plan.md` to insert (above the `# Publishing to Vaadin Directory Implementation Plan (§9.5)` heading):

```markdown
## Implementation status

**Done (YYYY-MM-DD).** All 13 tasks complete. Addon at `1.0.0-SNAPSHOT`; `./mvnw -Pdirectory clean verify` green; upload ZIP produces three JARs at the root with Vaadin Directory headers on the addon JAR's manifest. README + RELEASING.md present. Build and release GitHub Actions workflows in place. ShowcaseView in `demo/` plus rail-off / rail-on screenshots in `docs/screenshots/`.

The first `1.0.0` release itself (the act of bumping the POM, tagging, uploading) is **not** part of this plan — it is executed by the maintainer per `RELEASING.md` once this plan lands on `main`.

---

```

Replace `YYYY-MM-DD` with the actual completion date.

```bash
git add plans/2026-04-27-publishing-plan.md
git commit -m "docs(plan): record §9.5 publishing implementation completion"
```

---

## Self-review notes (kept inline for transparency)

- **Spec coverage:**
  - §2 (files): tasks 1-12 each create or modify exactly one of the listed files.
  - §3 (POM + Directory metadata): tasks 1-2 (metadata + plugins), 3-4 (manifest), 5-6 (assembly + profile).
  - §4 (README): task 11; screenshots from task 9-10.
  - §5 (versioning, release notes, RELEASING.md): task 12; CHANGELOG.md not created per §5.2.
  - §6 (GitHub Actions): tasks 7-8.
  - §7 (non-requirements): respected — no addon code change, no e2e/demo runtime change beyond the ShowcaseView.

- **Placeholder scan:** none. All paths, code, and commands are concrete.

- **Type/name consistency:** `vcf-side-nav-rail`, `1.0.0-SNAPSHOT`, `assembly/MANIFEST.MF`, `assembly/assembly.xml`, `directory` profile id, and the `directory-bundle` execution id appear identically across all tasks that reference them.

- **Spec/plan drift call-out:** the manifest-on-JAR vs manifest-on-assembly correction is documented at the top of this plan (the "Note on a small spec/implementation drift" paragraph) and matches the rationale in §3.2 of the spec ("Vaadin Directory reads custom JAR-manifest headers"). The spec text in §3.1 (`<archive><manifestFile>` on the assembly plugin) can be amended in a follow-up commit; the plan's approach is correct as written.
