# Releasing

Maintainer-facing checklist for cutting a new release of `vcf-side-nav-rail`. Eleven steps; do not skip.

## Versioning

Strict [SemVer 2.0.0](https://semver.org/spec/v2.0.0.html):

- **Patch** (`1.0.x`) — bug fixes, no API or behavioural change.
- **Minor** (`1.x.0`) — additive changes; existing usage continues to compile and behave the same.
- **Major** (`x.0.0`) — incompatible changes.

The working version on `main` between releases is `X.Y.(Z+1)-SNAPSHOT` (a patch bump). The next change is statistically more often a fix; if it turns out to be a feature or breaking change, the version can be bumped at the next tag instead.

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

7. Download `vcf-side-nav-rail-X.Y.Z.zip` from the published release.

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
