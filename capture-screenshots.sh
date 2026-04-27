#!/bin/bash
# Re-capture the README screenshots in docs/screenshots/.
#
# Starts the demo (port 8080) just long enough to drive a Playwright
# session against /screenshot, then shuts it down. Output PNGs are
# overwritten in place; run `git diff docs/screenshots/` afterwards to
# see what changed.
#
# Prerequisites: Java 17, the addon installed in the local m2 repo
# (`./mvnw -pl addon install -DskipTests` once is enough), Node 20+,
# Playwright deps already installed in e2e/src/test/playwright/, and
# ImageMagick `convert` on PATH.

set -u
cd "$(dirname "$0")"

PID_FILE=/tmp/capture-screenshots-demo.pid
LOG_FILE=/tmp/capture-screenshots-demo.log
PORT=8080
URL="http://localhost:${PORT}/screenshot"
DEMO_TIMEOUT=180  # seconds to wait for first paint

cleanup() {
    if [ -f "$PID_FILE" ]; then
        local pid
        pid=$(cat "$PID_FILE" 2>/dev/null || true)
        if [ -n "$pid" ] && kill -0 "$pid" 2>/dev/null; then
            kill "$pid" 2>/dev/null || true
            # Demo's mvnw spring-boot:run forks a JVM as a child of the wrapper
            # in fork=true mode; with fork=false (used here) the JVM IS the
            # process, so a single kill is enough.
            sleep 1
        fi
        rm -f "$PID_FILE"
    fi
}
trap cleanup EXIT INT TERM

if ! command -v convert >/dev/null 2>&1; then
    echo "error: ImageMagick 'convert' not found on PATH" >&2
    exit 1
fi
if ! command -v node >/dev/null 2>&1; then
    echo "error: 'node' not found on PATH" >&2
    exit 1
fi

# Refuse to start a second demo on the same port — usually a dev server
# the user already has running. Better to bail than to silently use it
# and leak a process.
if curl -sSf -o /dev/null "http://localhost:${PORT}/" 2>/dev/null; then
    echo "error: something is already serving http://localhost:${PORT}/." >&2
    echo "Stop it first (or run capture-screenshots.cjs by hand)." >&2
    exit 1
fi

echo "Starting demo (mvn -pl demo spring-boot:run, log: ${LOG_FILE})..."
(
    cd demo && exec ../mvnw spring-boot:run \
        -Dspring-boot.run.fork=false > "$LOG_FILE" 2>&1
) &
echo $! > "$PID_FILE"

echo "Waiting for ${URL} (up to ${DEMO_TIMEOUT}s)..."
DEADLINE=$(( $(date +%s) + DEMO_TIMEOUT ))
while [ "$(date +%s)" -lt "$DEADLINE" ]; do
    if curl -sSf -o /dev/null "$URL" 2>/dev/null; then
        echo "Demo ready."
        break
    fi
    sleep 3
done

if ! curl -sSf -o /dev/null "$URL" 2>/dev/null; then
    echo "error: demo did not become ready within ${DEMO_TIMEOUT}s. See ${LOG_FILE}." >&2
    exit 1
fi

echo "Capturing screenshots..."
( cd e2e/src/test/playwright && node capture-screenshots.cjs )
RC=$?

if [ "$RC" -ne 0 ]; then
    echo "error: capture script exited with status ${RC}." >&2
    exit "$RC"
fi

echo
echo "Done. Screenshots written to docs/screenshots/."
echo "Run 'git diff --stat docs/screenshots/' to see what changed."
