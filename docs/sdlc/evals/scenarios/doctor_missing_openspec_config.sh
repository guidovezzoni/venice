#!/usr/bin/env bash
# Scenario: openspec/config.yaml is missing.
# Fixture: complete except openspec config removed.
# Expected: ❌ for config.yaml, all others ✅, summary shows 1 failure.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/../lib/eval_helpers.sh"

SCENARIO_NAME="doctor_missing_openspec_config"

# --- Setup ---
FIXTURE_DIR=$(create_fixture_dir "$SCENARIO_NAME")
create_doctor_base_fixture "$FIXTURE_DIR"
rm "$FIXTURE_DIR/openspec/config.yaml"
snapshot_fixture "$FIXTURE_DIR"

# --- Run ---
OUTPUT_FILE="$FIXTURE_DIR/eval_output.txt"
echo -e "  ${DIM}Running $EVAL_TOOL for $SCENARIO_NAME...${RESET}"
run_eval "$FIXTURE_DIR" "$COMMANDS_DIR/sdlc_doctor.md" "$OUTPUT_FILE" || true

# --- Assert ---
begin_assertions

# The missing config should fail
assert_check_fails "openspec/config.yaml" "openspec/config.yaml fails"

# Other OpenSpec checks should still pass
assert_check_passes "explore.md" "opsx/explore.md passes"
assert_check_passes "propose.md" "opsx/propose.md passes"
assert_check_passes "apply.md" "opsx/apply.md passes"
assert_check_passes "verify.md" "opsx/verify.md passes"
assert_check_passes "sync.md" "opsx/sync.md passes"
assert_check_passes "archive.md" "opsx/archive.md passes"

# Security and SDLC commands should pass
assert_check_passes "security" "security plugin passes"
assert_check_passes "sdlc_open_story" "sdlc_open_story passes"
assert_check_passes "sdlc_propose_change" "sdlc_propose_change passes"
assert_check_passes "sdlc_implement_change" "sdlc_implement_change passes"
assert_check_passes "sdlc_verify_story" "sdlc_verify_story passes"
assert_check_passes "sdlc_exp_four_in_one" "sdlc_exp_four_in_one passes"
assert_check_passes "sdlc_exp_vibe_a_story" "sdlc_exp_vibe_a_story passes"
assert_check_passes "sdlc_doctor" "sdlc_doctor passes"
assert_check_passes "sdlc_project_doctor" "sdlc_project_doctor passes"

# Summary should show failure
assert_output_contains "failed" "summary mentions failure"
assert_min_fail_count 1 # at least 1 ❌

# Read-only guardrail
assert_fixture_unchanged "$FIXTURE_DIR"

# --- Report & cleanup ---
report_scenario_result "$SCENARIO_NAME"
RESULT=$?
cleanup_fixture "$FIXTURE_DIR"
exit $RESULT
