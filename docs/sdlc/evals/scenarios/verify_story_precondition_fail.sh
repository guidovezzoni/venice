#!/usr/bin/env bash
# Scenario: sdlc_verify_story stops when story preconditions are not met.
# Fixture: story in "New" state (not WIP) — closing precondition fails.
# Expected: command detects wrong state, informs user, stops before any gates.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/../lib/eval_helpers.sh"

SCENARIO_NAME="verify_story_precondition_fail"

# --- Setup ---
FIXTURE_DIR=$(create_fixture_dir "$SCENARIO_NAME")
create_story_command_base_fixture "$FIXTURE_DIR"

# Create story in "New" state — verify requires WIP for closing precondition
create_sample_story_new "$FIXTURE_DIR" "99.1.1" "test-feature"

git -C "$FIXTURE_DIR" add -A
git -C "$FIXTURE_DIR" commit -m "Add user story" --quiet

# --- Run ---
OUTPUT_FILE="$FIXTURE_DIR/eval_output.txt"
echo -e "  ${DIM}Running $EVAL_TOOL for $SCENARIO_NAME...${RESET}"
run_eval_with_args "$FIXTURE_DIR" "$COMMANDS_DIR/sdlc_verify_story.md" "$OUTPUT_FILE" "99.1.1" "10" || true

# --- Assert ---
begin_assertions

# The command should detect the wrong story state
assert_output_contains "precondition|not in.*WIP|not.*in progress|cannot.*verify|state|status" \
    "Output mentions precondition failure or wrong state"

# The command should NOT proceed to blocking gates
assert_output_not_contains "BLOCKING GATE.*PASS|Gate.*PASS|OpenSpec verify.*PASS" \
    "No blocking gates report PASS (command stopped early)"

# The command should NOT reach later verification steps
assert_output_not_contains "TODO scan|security review|clean build|unit test|coverage report" \
    "Later verification gates are not reached"

# --- Report & cleanup ---
report_scenario_result "$SCENARIO_NAME"
RESULT=$?
cleanup_fixture "$FIXTURE_DIR"
exit $RESULT
