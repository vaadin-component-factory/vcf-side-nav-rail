#!/usr/bin/env bash
#
# Run the full test suite against Vaadin 24 (the addon's compatibility floor):
# Java 17, Spring Boot 3.5.x, Vaadin 24.10.x — i.e. the build configured in
# the parent POM's default properties.
#
# Auto-cleans Vaadin-generated frontend artifacts if the previous run was V25,
# so alternating ./test-v24.sh and ./test-v25.sh works without manual cleanup.
# See ./test.sh --help for additional flags (--addon, --wipe).

exec "$(dirname "$0")/test.sh" --v24 "$@"
