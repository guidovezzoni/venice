#!/usr/bin/env bash
# Scenario: 2 of the 6 opsx command files are missing.
# Fixture: complete except apply.md and archive.md removed from opsx/.
# Expected: ❌ for those 2, all others ✅, summary shows 2 failures.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/../lib/eval_helpers.sh"

SCENARIO_NAME="doctor_missing_opsx_commands"

# --- Setup ---
FIXTURE_DIR=$(create_fixture_dir "$SCENARIO_NAME")
create_doctor_base_fixture "$FIXTURE_DIR"
rm "$FIXTURE_DIR/.claude/commands/opsx/apply.md"
rm "$FIXTURE_DIR/.claude/commands/opsx/archive.md"
snapshot_fixture "$FIXTURE_DIR"

# --- Run ---
OUTPUT_FILE="$FIXTURE_DIR/eval_output.txt"
echo -e "  ${DIM}Running $EVAL_TOOL for $SCENARIO_NAME...${RESET}"
run_eval "$FIXTURE_DIR" "$COMMANDS_DIR/sdlc_doctor.md" "$OUTPUT_FILE" || true

# --- Assert ---
begin_assertions

# Removed opsx commands should fail
assert_check_fails "apply.md" "opsx/apply.md fails"
assert_check_fails "archive.md" "opsx/archive.md fails"

# Remaining opsx commands should pass
assert_check_passes "explore.md" "opsx/explore.md passes"
assert_check_passes "propose.md" "opsx/propose.md passes"
assert_check_passes "verify.md" "opsx/verify.md passes"
assert_check_passes "sync.md" "opsx/sync.md passes"

# Other categories should pass
assert_check_passes "openspec/config.yaml" "openspec/config.yaml passes"
assert_check_passes "security" "security plugin passes"
assert_check_passes "sdlc_open_story" "sdlc_open_story passes"
assert_check_passes "sdlc_doctor" "sdlc_doctor passes"

# Summary should show failures
assert_output_contains "failed" "summary mentions failure"
assert_min_fail_count 2 # at least 2 ❌

# Read-only guardrail
assert_fixture_unchanged "$FIXTURE_DIR"

# --- Report & cleanup ---
report_scenario_result "$SCENARIO_NAME"
RESULT=$?
cleanup_fixture "$FIXTURE_DIR"
exit $RESULT
