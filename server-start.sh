#!/bin/bash
# Start the e2e test server (Spring Boot + Vaadin, production mode) on port 8081.
#
# Tailored to this project (SideNav Rail addon) — launches the `e2e/`
# Maven module, not the workspace root or `demo/`.
#
# Behaviour:
#   1. Stop any existing server via server-stop.sh (kills both the mvn
#      wrapper and the forked JVM holding port 8081).
#   2. If --rebuild: clean e2e/target + prod.bundle and repackage so
#      the production bundle gets regenerated (needed when new @Route
#      or @JsModule references are added).
#   3. Start Spring Boot with -Pproduction, stream to /tmp/claude-server.log.
#   4. Poll http://localhost:8081/ until ready or 120 s timeout.

set -u
cd /workspace

REBUILD=0
for arg in "$@"; do
    case "$arg" in
        --rebuild) REBUILD=1 ;;
        *) echo "unknown flag: $arg" >&2; exit 2 ;;
    esac
done

bash /workspace/server-stop.sh > /dev/null 2>&1 || true

if [ "$REBUILD" -eq 1 ]; then
    echo "Rebuilding production bundle (clean e2e/target + prod.bundle)..."
    rm -rf /workspace/e2e/target /workspace/e2e/src/main/bundles/prod.bundle
    ./mvnw -pl e2e -am -DskipTests package 2>&1 \
        | tee /tmp/claude-server-rebuild.log | grep -E "BUILD|ERROR" | tail -5
    if ! grep -q "BUILD SUCCESS" /tmp/claude-server-rebuild.log; then
        echo "Rebuild FAILED — see /tmp/claude-server-rebuild.log"
        exit 1
    fi
fi

# -Dspring-boot.run.fork=false keeps the JVM as a child of the mvn
# process so process tracking is reliable. Output goes to the same
# /tmp/claude-server.log file the existing print-server-logs.sh reads.
echo "Starting server (e2e module, production profile)..."
(
    cd /workspace/e2e && \
    mvn -Pproduction spring-boot:run -Dspring-boot.run.fork=false \
        > /tmp/claude-server.log 2>&1 &
    echo $! > /tmp/claude-server.pid
)

echo "Launched (wrapper PID $(cat /tmp/claude-server.pid 2>/dev/null || echo '?'))"
echo "Waiting for http://localhost:8081/ (up to 120 s)..."

DEADLINE=$(( $(date +%s) + 120 ))
while [ "$(date +%s)" -lt "$DEADLINE" ]; do
    if curl -sSf -o /dev/null http://localhost:8081/ 2>/dev/null; then
        echo "Server ready (http://localhost:8081/)."
        exit 0
    fi
    sleep 3
done

echo "Server did not come up within 120 s — see /tmp/claude-server.log"
exit 1
