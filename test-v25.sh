#!/usr/bin/env bash
#
# Run the full test suite against the latest stable Vaadin 25 + Spring Boot 4
# (Java 21). Pinned versions live near the top of test.sh — bump them when new
# stable releases land.
#
# Auto-cleans Vaadin-generated frontend artifacts if the previous run was V24,
# so alternating ./test-v24.sh and ./test-v25.sh works without manual cleanup.
# JAVA_HOME defaults to the container's JDK 25 (/usr/lib/jvm/openjdk-25) when
# no JAVA_HOME is exported.
#
# See ./test.sh --help for additional flags (--addon, --wipe).

exec "$(dirname "$0")/test.sh" --v25 "$@"
