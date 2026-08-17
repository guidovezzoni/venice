#!/usr/bin/env bash
# Scenario: All doctor checks pass.
# Fixture: complete SDLC tooling — all files present, all config correct.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/../lib/eval_helpers.sh"

SCENARIO_NAME="doctor_all_pass"

# --- Setup ---
FIXTURE_DIR=$(create_fixture_dir "$SCENARIO_NAME")
create_doctor_base_fixture "$FIXTURE_DIR"
snapshot_fixture "$FIXTURE_DIR"

# --- Run ---
OUTPUT_FILE="$FIXTURE_DIR/eval_output.txt"
echo -e "  ${DIM}Running $EVAL_TOOL for $SCENARIO_NAME...${RESET}"
run_eval "$FIXTURE_DIR" "$COMMANDS_DIR/sdlc_doctor.md" "$OUTPUT_FILE" || true

# --- Assert ---
begin_assertions

# OpenSpec checks
assert_check_passes "openspec/config.yaml" "openspec/config.yaml passes"
assert_check_passes "explore.md" "opsx/explore.md passes"
assert_check_passes "propose.md" "opsx/propose.md passes"
assert_check_passes "apply.md" "opsx/apply.md passes"
assert_check_passes "verify.md" "opsx/verify.md passes"
assert_check_passes "sync.md" "opsx/sync.md passes"
assert_check_passes "archive.md" "opsx/archive.md passes"

# Security check
assert_check_passes "security" "security plugin passes"

# SDLC command checks
assert_check_passes "sdlc_open_story" "sdlc_open_story passes"
assert_check_passes "sdlc_propose_change" "sdlc_propose_change passes"
assert_check_passes "sdlc_implement_change" "sdlc_implement_change passes"
assert_check_passes "sdlc_verify_story" "sdlc_verify_story passes"
assert_check_passes "sdlc_exp_four_in_one" "sdlc_exp_four_in_one passes"
assert_check_passes "sdlc_exp_vibe_a_story" "sdlc_exp_vibe_a_story passes"
assert_check_passes "sdlc_doctor" "sdlc_doctor passes"
assert_check_passes "sdlc_project_doctor" "sdlc_project_doctor passes"

# Summary
assert_output_contains "All checks passed" "summary shows all passed"

# Read-only guardrail
assert_fixture_unchanged "$FIXTURE_DIR"

# --- Report & cleanup ---
report_scenario_result "$SCENARIO_NAME"
RESULT=$?
cleanup_fixture "$FIXTURE_DIR"
exit $RESULT
