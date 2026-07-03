#!/usr/bin/env bash
# Scenario: All project doctor checks pass.
# Fixture: complete project config — detekt, kover, unit tests, fastlane,
#          CI/CD, gradle wrapper.
# Note: CLI checks (command -v fastlane, command -v bundle) depend on the host.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/../lib/eval_helpers.sh"

SCENARIO_NAME="project_doctor_all_pass"

# --- Setup ---
FIXTURE_DIR=$(create_fixture_dir "$SCENARIO_NAME")
create_project_doctor_base_fixture "$FIXTURE_DIR"
snapshot_fixture "$FIXTURE_DIR"

# --- Run ---
OUTPUT_FILE="$FIXTURE_DIR/eval_output.txt"
echo -e "  ${DIM}Running $EVAL_TOOL for $SCENARIO_NAME...${RESET}"
run_eval "$FIXTURE_DIR" "$COMMANDS_DIR/sdlc_project_doctor.md" "$OUTPUT_FILE" || true

# --- Assert ---
begin_assertions

# Detekt category — at least one ✅ mentioning detekt
assert_check_passes "detekt" "detekt checks pass"
assert_check_passes "maxIssues|max.issues" "detekt maxIssues check passes"
assert_check_passes "compose.*rules|nlopez|compose.*detekt" \
    "compose detekt rules check passes"

# Kover category
assert_check_passes "kover" "kover checks pass"
assert_check_passes "minBound|min.bound|bound.*95|95" "kover minBound(95) passes"

# Unit test category
assert_check_passes "junit|JUnit" "junit declared"
assert_check_passes "mockk|MockK" "mockk declared"
assert_check_passes "coroutines" "coroutines-test declared"
assert_check_passes "test.*kt|\.kt.*test" "test files exist"

# Fastlane category
assert_check_passes "Fastfile|fastfile" "Fastfile exists"
assert_check_passes "Gemfile|gemfile" "Gemfile exists"

# CI/CD category
assert_check_passes "ci.yml|ci.*workflow|workflow" "ci.yml exists"
assert_check_passes "gradlew check|gradle.*check" "CI runs gradlew check"
assert_check_passes "koverVerify|kover.*[Vv]erify" "CI runs koverVerify"

# Gradle category
assert_check_passes "gradlew|[Gg]radle.*wrapper|[Gg]radle.*task" \
    "gradlew tasks succeeds"

# Summary
assert_output_contains "All checks passed|all.*pass" "summary shows all passed"

# Read-only guardrail
assert_fixture_unchanged "$FIXTURE_DIR"

# --- Report & cleanup ---
report_scenario_result "$SCENARIO_NAME"
RESULT=$?
cleanup_fixture "$FIXTURE_DIR"
exit $RESULT
