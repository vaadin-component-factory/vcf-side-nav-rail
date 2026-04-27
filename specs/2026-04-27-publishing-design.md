# Publishing to Vaadin Directory — Design (§9.5)

**Status:** design, ready for implementation
**Scope:** preparation work to publish the SideNav Rail addon to [vaadin.com/directory](https://vaadin.com/directory). Covers metadata, build artifacts, release pipeline, versioning, and maintainer documentation. Closes [§9.5 of the main design spec](2026-04-21-side-nav-rail-design.md#95-phase-b--c--publishing).

## 1. Goal

Make the addon publishable on Vaadin Directory with a single tag-push, and give a future maintainer a deterministic checklist to cut subsequent releases.

Out of scope:

- No automated upload to Vaadin Directory. Vaadin's web UI is the publish surface; the CI pipeline only produces the ZIP artifact.
- No second publish target (Maven Central, GitHub Packages, JitPack). Those remain available paths but require their own design.
- No release-automation tooling (Conventional Commits parsers, semantic-release, etc.). Release notes are curated by hand on each release (see §5.2).
- No SBOM, sigstore, or supply-chain attestation. Out of scope for the first release; can be added later without breaking the release flow.

## 2. Files & layout

| Path | Status | Purpose |
|---|---|---|
| `README.md` | new | Top-level addon README. The previous (devcontainer-template) README has been removed; this is a fresh write. |
| `RELEASING.md` | new | Maintainer checklist for cutting a release (see §5). |
| `addon/assembly/MANIFEST.MF` | new | Vaadin-Directory metadata headers. Lives next to `addon/pom.xml` so paths in the assembly descriptor stay short. |
| `addon/assembly/assembly.xml` | new | `maven-assembly-plugin` descriptor that bundles the addon JAR + `MANIFEST.MF` into the Directory ZIP. |
| `.github/workflows/build.yml` | new | CI for PRs and `main` pushes — runs `./mvnw clean verify`. |
| `.github/workflows/release.yml` | new | Release pipeline — triggered on `v*` tags, builds the Directory ZIP, attaches it to a GitHub Release. |
| `addon/pom.xml` | modify | Bump version from `0.1.0-SNAPSHOT` to `1.0.0-SNAPSHOT` (the first release tag drops the SNAPSHOT — see §5.3); add `<scm>`, `<organization>`, `<developers>`, source + javadoc plugins, `directory` build profile (see §3). |

The `e2e/` and `demo/` modules are not part of the published artifact and stay unchanged structurally — only the demo grows a small showcase view used for the README screenshots (§4).

## 3. `addon/pom.xml` & Directory metadata

### 3.1 POM additions

The current `addon/pom.xml` is minimal and at version `0.1.0-SNAPSHOT`. The version is bumped to `1.0.0-SNAPSHOT` (the first release per `RELEASING.md` will drop the SNAPSHOT to produce `1.0.0`). Beyond the version, three blocks need to be added or extended for a Directory-publishable artifact:

**Project metadata** (required by Vaadin Directory and standard Maven OSS practice):

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

**Source + javadoc JARs** — released alongside the main JAR so consumers see Javadoc in their IDE:

```xml
<plugin>
  <groupId>org.apache.maven.plugins</groupId>
  <artifactId>maven-source-plugin</artifactId>
  <executions>
    <execution>
      <id>attach-sources</id>
      <goals><goal>jar-no-fork</goal></goals>
    </execution>
  </executions>
</plugin>
<plugin>
  <groupId>org.apache.maven.plugins</groupId>
  <artifactId>maven-javadoc-plugin</artifactId>
  <executions>
    <execution>
      <id>attach-javadocs</id>
      <goals><goal>jar</goal></goals>
    </execution>
  </executions>
</plugin>
```

**`directory` build profile** — only active when explicitly requested (`./mvnw -Pdirectory ...`). Builds the upload ZIP via `maven-assembly-plugin` from `addon/assembly/assembly.xml`:

```xml
<profile>
  <id>directory</id>
  <build>
    <plugins>
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-assembly-plugin</artifactId>
        <executions>
          <execution>
            <id>directory-bundle</id>
            <phase>package</phase>
            <goals><goal>single</goal></goals>
            <configuration>
              <descriptors>
                <descriptor>assembly/assembly.xml</descriptor>
              </descriptors>
              <archive>
                <manifestFile>assembly/MANIFEST.MF</manifestFile>
              </archive>
            </configuration>
          </execution>
        </executions>
      </plugin>
    </plugins>
  </build>
</profile>
```

### 3.2 Directory metadata — `addon/assembly/MANIFEST.MF`

Vaadin Directory reads custom JAR-manifest headers to populate the listing page. Required headers:

```
Manifest-Version: 1.0
Vaadin-Package-Version: 1
Implementation-Title: SideNav Rail
Implementation-Version: ${project.version}
Implementation-Vendor: Vaadin Ltd.
Vaadin-Addon: vcf-side-nav-rail-${project.version}.jar
```

`${project.version}` is interpolated by `maven-assembly-plugin` at build time. `Vaadin-Package-Version: 1` is the addon-package format version (always `1` today).

### 3.3 Directory metadata — `addon/assembly/assembly.xml`

The assembly descriptor produces a ZIP containing the addon JAR (and the manifest is applied to the ZIP itself):

```xml
<assembly>
  <id>directory</id>
  <formats><format>zip</format></formats>
  <includeBaseDirectory>false</includeBaseDirectory>
  <files>
    <file>
      <source>${project.build.directory}/${project.build.finalName}.jar</source>
      <outputDirectory>/</outputDirectory>
    </file>
  </files>
</assembly>
```

The resulting `addon/target/vcf-side-nav-rail-X.Y.Z-directory.zip` is what gets uploaded.

## 4. README

### 4.1 Constraints

- **Self-contained.** No links to `specs/*` or `plans/*` — those documents are volatile and may move or change wording. References are allowed only to: GitHub repo (issues, releases), Vaadin Directory listing, Vaadin docs, and the LICENSE file.
- **Standard structure.** Fits the conventions of other Vaadin Component Factory addons (lookup target ≈ `vcf-enhanced-richtexteditor`, `vcf-enhanced-grid`).
- **Length:** roughly 150-250 lines. Enough for an evaluator to decide "yes, this fits my use case" without scrolling forever.
- **Replaces** the existing top-level `README.md` (currently the devcontainer-template doc).

### 4.2 Section outline

1. **Title + tagline** — one sentence: "Vaadin SideNav with collapsible rail mode and hover popovers."
2. **Vaadin Directory badge** — links to the listing once published. Placeholder URL until first release.
3. **Screenshots** — two images (rail off / rail on with popover). Sourced from a dedicated `demo/` showcase view (§4.3). Stored in `docs/screenshots/` and referenced via relative paths.
4. **Features** — bulleted list, ~6-8 items: rail mode toggle, hover popover, keyboard navigation, ARIA contracts, customizable tooltips, letter-avatar fallback, lifecycle event.
5. **Compatibility** — Vaadin 24.9 or later, Java 17+. (Audience is assumed to be familiar with Vaadin basics — no Vaadin Core explainers in the README.)
6. **Installation** — `<dependency>` snippet pointing at the Directory-published version.
7. **Quick start** — ≤ 25-line code example: create `SideNavRail`, add a few `SideNavRailItem`s, wire a toggle button.
8. **API overview** — short prose paragraph per main type: `SideNavRail`, `SideNavRailItem`, the four enums (`PopoverOn`, `PopoverParentLabelMode`, `RailTooltipMode`), and `RailModeChangedEvent`. Anything deeper goes in javadoc, not the README.
9. **Building from source / Running the demo** — `./mvnw clean verify` and `./mvnw -pl demo spring-boot:run`.
10. **License** — Apache-2.0 + link to `LICENSE`.
11. **Contributing** — short paragraph: open an issue first; PRs welcome.

### 4.3 Screenshot showcase view (in `demo/`)

A new route `/showcase` in the existing `demo/` module that renders a representative rail layout and is laid out specifically for screenshotting (clean background, no toggle button, sensible item set). Captured images live in `docs/screenshots/rail-off.png` and `docs/screenshots/rail-on.png`.

The view itself is small (one Java file, no Java tests required); image capture is a manual one-off step using the existing `ui-explorer` agent or a Playwright snippet — not automated as part of every build.

## 5. Versioning, release notes, RELEASING.md

### 5.1 Versioning

Strict [SemVer 2.0.0](https://semver.org/spec/v2.0.0.html).

- **First release:** `1.0.0`. The addon completed §9.4 (a11y E2E tests) on `main`, the public Java API has been stable across phase 2 work, and the §9.5 fix-on-focus tooltip change shipped on this branch is the last known issue.
- **Working version after each release:** `X.Y.(Z+1)-SNAPSHOT` (patch bump). The next change is statistically more often a fix than a feature; the SNAPSHOT bump is mechanical and uncoupled from the actual *next* release, which can be re-cut as minor or major when the tag is set.
- **Patch (`1.0.x`):** bug fixes, no API change, no behavioural change visible to consumers.
- **Minor (`1.x.0`):** additive API or additive behaviour. Existing usage continues to compile and behave the same.
- **Major (`x.0.0`):** any incompatible change.

### 5.2 Release notes

Release notes live on **GitHub Releases**, not in an in-repo `CHANGELOG.md`. The audience for this addon is developers who already navigate to GitHub for source and issues, and Vaadin Directory's listing page links to the GitHub repo for change history. Maintaining a duplicate in-repo CHANGELOG would be redundant and create a forking surface (release body vs CHANGELOG could drift).

Workflow:

- During the release cut (§5.3), the maintainer drafts the release body manually, optionally seeded by GitHub's *auto-generated release notes* (Settings → "Generate release notes from PRs/commits"), and edits it down to a human-readable summary grouped loosely by `Added` / `Changed` / `Fixed` / etc.
- The first release (`1.0.0`) summarises the work delivered through phase 9.4 + the keyboard-tooltip fix from this branch — a few high-level bullets per category, not a commit dump.

Trade-off accepted: contributors who clone the repo offline cannot read past release notes from the working tree. They are one click away on the repo's *Releases* page in any normal workflow; the in-repo file would only marginally help in air-gapped scenarios that do not apply to a Vaadin Directory addon.

### 5.3 RELEASING.md

Maintainer-facing checklist. Eleven steps, no shortcuts:

1. Edit `addon/pom.xml` `<version>` to `X.Y.Z` (drop `-SNAPSHOT`).
2. `./mvnw clean verify` locally — must be green.
3. Commit: `release: vX.Y.Z`. Push to `main`.
4. Create + push tag `vX.Y.Z` → triggers `release.yml`.
5. Wait for the release workflow to finish green. The workflow creates a *draft* GitHub Release with the ZIP, sources, and javadoc attached and auto-generated notes filled in.
6. Open the draft release, edit the notes into a human-readable summary (group as `Added` / `Changed` / `Fixed`), then publish.
7. Download the `vcf-side-nav-rail-X.Y.Z-directory.zip` artifact from the published release.
8. Manually upload the ZIP at [vaadin.com/directory](https://vaadin.com/directory) (login → "Add new component" or "New version" on the existing listing).
9. Verify the new version shows up on the Directory listing page; smoke-test the install snippet.
10. Update the `v-herd-demo` branch (see §5.4): merge tag `vX.Y.Z` into `v-herd-demo` and push. For the very first release (`1.0.0`), `v-herd-demo` does not yet exist — create it from the `v1.0.0` tag (`git branch v-herd-demo v1.0.0 && git push -u origin v-herd-demo`) instead of merging. The branch is auto-deployed as the live demo, so this must happen *before* the snapshot bump in step 11 — otherwise the deployed demo would show snapshot code instead of the released version.
11. Edit `addon/pom.xml` to `X.Y.(Z+1)-SNAPSHOT`; commit `chore: bump to X.Y.(Z+1)-SNAPSHOT`; push. If the next planned change is not a patch, bump appropriately at the next tag instead.

A maintainer reading this file should be able to execute a release without rediscovering tribal knowledge.

### 5.4 `v-herd-demo` branch

A long-lived branch tracking the most recently released version of the addon. Vaadin's herd-demo infrastructure auto-deploys whatever sits on this branch, so it must always point at a tagged-and-published version, never at a snapshot.

- After each release tag, the maintainer merges the tag into `v-herd-demo` (step 10 of §5.3).
- The branch only ever advances at release time. No direct commits, no snapshot work lands here.
- For the first `1.0.0` release the branch does not exist yet and is created from the `v1.0.0` tag rather than merged into.

## 6. GitHub Actions workflows

### 6.1 `build.yml`

**Trigger:** pull requests against `main`, and `push` events on `main`.

**Steps:**

1. Checkout (full history not needed — `fetch-depth: 1`).
2. Set up Java 17 (Temurin or Adoptium).
3. Cache `~/.m2/repository`.
4. Run `./mvnw -B clean verify`.

The full `verify` runs the unit tests, Karibu UI tests, and the production-mode Playwright suite (the e2e module is configured to always build the production bundle; no separate IT profile). A failed run blocks the PR.

Pre-commit hook parity, code style, and linting are not part of CI today — they are local concerns. Adding spotless-check is a one-line follow-up if needed but is out of scope here.

### 6.2 `release.yml`

**Trigger:** `push` events on tags matching `v*` (i.e., `v1.0.0`).

**Steps:**

1. Checkout the tagged commit.
2. Set up Java 17.
3. Cache `~/.m2/repository`.
4. Run `./mvnw -B -Pdirectory clean verify`. The `directory` profile produces `addon/target/vcf-side-nav-rail-X.Y.Z-directory.zip` on top of the normal verify.
5. Create a **draft** GitHub Release for the tag using `softprops/action-gh-release@v2` with `draft: true` and `generate_release_notes: true`; attach the ZIP and the source/javadoc JARs from `addon/target/`. The maintainer edits the auto-generated body and publishes the release manually (step 6 of `RELEASING.md`).

**No automated Directory upload.** Vaadin Directory does not expose an upload API; the maintainer uploads via the web UI in step 7 of `RELEASING.md`. Building the upload artifact in CI guarantees reproducibility — no "it built on my laptop" surprises.

## 7. Non-requirements

- No new Java API. The addon code does not change; only metadata, build, and docs.
- No `e2e/` or `demo/` runtime changes (the demo gains one new view for screenshots; that view does not run in tests).
- No in-repo `CHANGELOG.md`. Release notes live on GitHub Releases (see §5.2).
- No multi-version test matrix in CI. The `pom.xml` declares Vaadin 24.10.x; testing across versions is a deliberate Phase 3+ concern.
