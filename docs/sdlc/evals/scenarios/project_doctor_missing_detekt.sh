#!/usr/bin/env bash
# Scenario: Detekt config file removed.
# Fixture: complete except config/detekt/detekt.yml deleted.
# Expected: ❌ for detekt config (and possibly maxIssues check since it reads
#           the file), other categories pass.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/../lib/eval_helpers.sh"

SCENARIO_NAME="project_doctor_missing_detekt"

# --- Setup ---
FIXTURE_DIR=$(create_fixture_dir "$SCENARIO_NAME")
create_project_doctor_base_fixture "$FIXTURE_DIR"
rm "$FIXTURE_DIR/config/detekt/detekt.yml"
snapshot_fixture "$FIXTURE_DIR"

# --- Run ---
OUTPUT_FILE="$FIXTURE_DIR/eval_output.txt"
echo -e "  ${DIM}Running $EVAL_TOOL for $SCENARIO_NAME...${RESET}"
run_eval "$FIXTURE_DIR" "$COMMANDS_DIR/sdlc_project_doctor.md" "$OUTPUT_FILE" || true

# --- Assert ---
begin_assertions

# Detekt config check should fail
assert_check_fails "detekt.yml|detekt.*config|config.*detekt" \
    "detekt config file check fails"

# maxIssues check might also fail (reads the missing file)
# Don't assert it — it could go either way

# Other categories should pass
assert_check_passes "kover" "kover checks still pass"
assert_check_passes "junit|JUnit" "unit test checks still pass"
assert_check_passes "Fastfile|fastfile" "fastlane checks still pass"
assert_check_passes "ci.yml|workflow" "CI/CD checks still pass"

# Summary should indicate failures
assert_output_contains "failed" "summary mentions failure"
assert_min_fail_count 1

# Read-only guardrail
assert_fixture_unchanged "$FIXTURE_DIR"

# --- Report & cleanup ---
report_scenario_result "$SCENARIO_NAME"
RESULT=$?
cleanup_fixture "$FIXTURE_DIR"
exit $RESULT
