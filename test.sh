#!/usr/bin/env bash
#
# Run the full SideNav Rail addon build (mvn clean verify) against either
# Vaadin 24 (compatibility floor) or Vaadin 25 + Spring Boot 4 (latest).
#
# Usage:
#   ./test.sh             # Vaadin 24 (default)
#   ./test.sh --v24       # Vaadin 24, explicit
#   ./test.sh --v25       # Vaadin 25 latest stable + Spring Boot 4 + Java 21
#   ./test.sh --addon     # only addon module (Karibu unit tests, no E2E)
#   ./test.sh --wipe      # also force a fresh frontend bundle (slow)
#   ./test.sh --v25 --addon
#
# Prefer the named entry points: `./test-v24.sh`, `./test-v25.sh`.
#
# Cross-version safety:
#   Tracks the last-built Vaadin major in /tmp/sidenav-build-version and
#   automatically wipes Vaadin-generated frontend artifacts
#   (`e2e/src/main/frontend/`, `e2e/src/main/bundles/`) whenever the
#   requested major differs from the previous run, so alternating V24/V25
#   runs work without manual cleanup.
#
# Output is tee'd to /tmp/sidenav-build.log; PID stored in /tmp/sidenav-build.pid.

set -u
set -o pipefail

WORKSPACE="$(cd "$(dirname "$0")" && pwd)"
LOG=/tmp/sidenav-build.log
PIDFILE=/tmp/sidenav-build.pid
VERSIONFILE=/tmp/sidenav-build-version
PROD_BUNDLE_DIR="$WORKSPACE/e2e/src/main/bundles"
FRONTEND_GENERATED_DIR="$WORKSPACE/e2e/src/main/frontend"

# Vaadin 25 + Spring Boot 4 pinned versions. Bump when new stable releases land.
V25_VAADIN="25.1.3"
V25_SPRING_BOOT="4.0.6"
V25_KARIBU="2.7.0"
V25_JAVA_RELEASE="21"
V25_JAVA_MIN=21

VERSION="24"
WIPE=0
ADDON_ONLY=0
for arg in "$@"; do
  case "$arg" in
    --v24) VERSION="24" ;;
    --v25) VERSION="25" ;;
    --wipe) WIPE=1 ;;
    --addon) ADDON_ONLY=1 ;;
    -h|--help) sed -n '2,21p' "$0"; exit 0 ;;
    *) echo "Unknown arg: $arg" >&2; exit 2 ;;
  esac
done

cd "$WORKSPACE" || { echo "cannot cd to $WORKSPACE" >&2; exit 1; }

# Stop leftover Spring Boot test app from a prior run — its JMX port (9001)
# blocks the spring-boot:start plugin and leaks state across runs.
if pgrep -f "TestApplication" >/dev/null 2>&1; then
  echo "Killing leftover TestApplication process(es)..."
  pkill -f "TestApplication" || true
  sleep 1
fi
if pgrep -f "playwright test" >/dev/null 2>&1; then
  pkill -f "playwright test" || true
fi

# Cross-version contamination guard: src/main/{frontend,bundles}/ are
# Vaadin-generated and version-specific. `mvn clean` only nukes target/, so
# wipe these manually whenever the Vaadin major changes between runs.
PREV_VERSION=""
if [ -f "$VERSIONFILE" ]; then
  PREV_VERSION="$(cat "$VERSIONFILE")"
fi
if [ -n "$PREV_VERSION" ] && [ "$PREV_VERSION" != "$VERSION" ]; then
  echo "Switching Vaadin major: $PREV_VERSION → $VERSION; wiping generated frontend artifacts."
  rm -rf "$PROD_BUNDLE_DIR" "$FRONTEND_GENERATED_DIR"
elif [ "$WIPE" -eq 1 ]; then
  echo "Wiping production bundle (--wipe)."
  rm -rf "$PROD_BUNDLE_DIR"
fi

# Per-run cleanup of Playwright outputs so old failure artifacts don't leak in.
rm -rf "$WORKSPACE/e2e/src/test/playwright/test-results" \
       "$WORKSPACE/e2e/src/test/playwright/playwright-report" 2>/dev/null || true

# Mark the version up front so an interrupted run still leaves the workspace
# in a known state for the next invocation's contamination check.
echo "$VERSION" > "$VERSIONFILE"

MVN_ARGS=()
if [ "$VERSION" = "25" ]; then
  MVN_ARGS+=(
    "-Dvaadin.version=$V25_VAADIN"
    "-Dspring-boot.version=$V25_SPRING_BOOT"
    "-Dkaribu.version=$V25_KARIBU"
    "-Dmaven.compiler.release=$V25_JAVA_RELEASE"
  )
  # V25 needs Java 21+. Use whatever JAVA_HOME points to — but verify it's
  # new enough and bail with a clear instruction if not. Don't presume any
  # local path: the user/CI is responsible for pointing JAVA_HOME at a 21+ JDK.
  java_bin="${JAVA_HOME:+$JAVA_HOME/bin/java}"
  if [ -z "$java_bin" ] || [ ! -x "$java_bin" ]; then
    java_bin="$(command -v java || true)"
  fi
  if [ -z "$java_bin" ]; then
    echo "V25 requires Java $V25_JAVA_MIN+, but no java executable was found." >&2
    echo "Set JAVA_HOME to a JDK $V25_JAVA_MIN+ install and retry." >&2
    exit 3
  fi
  java_major="$("$java_bin" -version 2>&1 | head -1 \
      | sed -E 's/^[^"]+"([0-9]+).*/\1/')"
  if ! [ "$java_major" -ge "$V25_JAVA_MIN" ] 2>/dev/null; then
    echo "V25 requires Java $V25_JAVA_MIN+, but $java_bin is Java $java_major." >&2
    echo "Set JAVA_HOME to a JDK $V25_JAVA_MIN+ install and retry, e.g.:" >&2
    echo "  JAVA_HOME=/path/to/jdk-21 ./test-v25.sh" >&2
    exit 3
  fi
fi

if [ "$ADDON_ONLY" -eq 1 ]; then
  CMD=( ./mvnw -pl addon "${MVN_ARGS[@]}" clean test )
else
  CMD=( ./mvnw "${MVN_ARGS[@]}" clean verify )
fi

echo "==== $(date -Iseconds) ====" | tee "$LOG"
echo "Vaadin: $VERSION  JAVA_HOME: ${JAVA_HOME:-<default>}" | tee -a "$LOG"
echo "Running: ${CMD[*]}  (wipe=$WIPE, addon-only=$ADDON_ONLY)" | tee -a "$LOG"
echo "Log: $LOG" | tee -a "$LOG"

echo "$$" > "$PIDFILE"
"${CMD[@]}" 2>&1 | tee -a "$LOG"
EXIT=${PIPESTATUS[0]}

rm -f "$PIDFILE"
echo "==== exit=$EXIT $(date -Iseconds) ====" | tee -a "$LOG"
exit "$EXIT"
