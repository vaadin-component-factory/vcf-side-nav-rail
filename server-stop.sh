#!/bin/bash
# Stop the e2e test server on port 8081.
#
# The important wrinkle: `mvn spring-boot:run` is a WRAPPER around the
# real Spring Boot JVM. A plain `pkill -f spring-boot.run` (or killing
# the PID in /tmp/claude-server.pid) only kills the wrapper — the forked
# JVM survives and keeps port 8081 plus any stale classpath cached in
# memory. That caused several hours of mysterious test failures during
# §9.4 implementation when the prod.bundle hash had changed on disk but
# the running JVM still served the old hash.
#
# So this script kills aggressively:
#   1. The PID from /tmp/claude-server.pid (mvn wrapper).
#   2. Any TestApplication process (the forked Spring Boot main class).
#   3. Any process still holding port 8081.
#   4. kill -9 on whatever survives after 2 s.

set -u

if [ -f /tmp/claude-server.pid ]; then
    kill "$(cat /tmp/claude-server.pid)" 2>/dev/null || true
    rm -f /tmp/claude-server.pid
fi

pkill -f 'TestApplication'   2>/dev/null || true
pkill -f 'spring-boot.run'   2>/dev/null || true

sleep 1

PID_ON_PORT=$(ss -tlpn 2>/dev/null | awk '/:8081 / { match($0, /pid=[0-9]+/); if (RSTART) print substr($0, RSTART+4, RLENGTH-4) }' | head -1)
if [ -n "$PID_ON_PORT" ]; then
    kill "$PID_ON_PORT" 2>/dev/null || true
fi

sleep 2

pkill -9 -f 'TestApplication' 2>/dev/null || true
pkill -9 -f 'spring-boot.run' 2>/dev/null || true

if pgrep -f 'TestApplication\|spring-boot.run' >/dev/null 2>&1; then
    echo "WARN: a server process is still alive:" >&2
    pgrep -af 'TestApplication\|spring-boot.run' >&2
    exit 1
fi

echo "Server stopped."
