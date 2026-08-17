#!/usr/bin/env bash
# Scenario: All checked files removed.
# Fixture: empty project — no openspec config, no opsx commands, no settings,
#          no sdlc commands.
# Expected: all file-based checks ❌. CLI checks (command -v openspec) depend
#           on the host environment and are not asserted.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/../lib/eval_helpers.sh"

SCENARIO_NAME="doctor_everything_broken"

# --- Setup ---
FIXTURE_DIR=$(create_fixture_dir "$SCENARIO_NAME")
# Do NOT call create_doctor_base_fixture — start with an empty git repo
snapshot_fixture "$FIXTURE_DIR"

# --- Run ---
OUTPUT_FILE="$FIXTURE_DIR/eval_output.txt"
echo -e "  ${DIM}Running $EVAL_TOOL for $SCENARIO_NAME...${RESET}"
run_eval "$FIXTURE_DIR" "$COMMANDS_DIR/sdlc_doctor.md" "$OUTPUT_FILE" || true

# --- Assert ---
begin_assertions

# All file-based checks should fail
assert_check_fails "openspec/config.yaml" "openspec/config.yaml fails"
assert_check_fails "explore.md" "opsx/explore.md fails"
assert_check_fails "propose.md" "opsx/propose.md fails"
assert_check_fails "apply.md" "opsx/apply.md fails"
assert_check_fails "verify.md" "opsx/verify.md fails"
assert_check_fails "sync.md" "opsx/sync.md fails"
assert_check_fails "archive.md" "opsx/archive.md fails"
assert_check_fails "security" "security plugin check fails"
assert_check_fails "sdlc_open_story" "sdlc_open_story fails"
assert_check_fails "sdlc_propose_change" "sdlc_propose_change fails"
assert_check_fails "sdlc_implement_change" "sdlc_implement_change fails"
assert_check_fails "sdlc_verify_story" "sdlc_verify_story fails"
assert_check_fails "sdlc_exp_four_in_one" "sdlc_exp_four_in_one fails"
assert_check_fails "sdlc_exp_vibe_a_story" "sdlc_exp_vibe_a_story fails"
assert_check_fails "sdlc_doctor" "sdlc_doctor fails"
assert_check_fails "sdlc_project_doctor" "sdlc_project_doctor fails"

# At least 16 failures (file checks), possibly 17 if openspec CLI also missing
assert_min_fail_count 16

# Summary should indicate failures
assert_output_contains "failed" "summary mentions failure"

# Read-only guardrail
assert_fixture_unchanged "$FIXTURE_DIR"

# --- Report & cleanup ---
report_scenario_result "$SCENARIO_NAME"
RESULT=$?
cleanup_fixture "$FIXTURE_DIR"
exit $RESULT
