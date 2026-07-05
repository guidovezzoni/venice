#!/usr/bin/env bash
# Scenario: sdlc_verify_story blocks on first gate failure and does not proceed.
# Fixture: WIP story present but no openspec change artifacts — OpenSpec verify
#          (the first blocking gate) should fail.
# Expected: command stops at gate failure, does NOT proceed to later gates.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/../lib/eval_helpers.sh"

SCENARIO_NAME="verify_story_blocks_on_gate_failure"

# --- Setup ---
FIXTURE_DIR=$(create_fixture_dir "$SCENARIO_NAME")
create_story_command_base_fixture "$FIXTURE_DIR"
create_sample_story_wip "$FIXTURE_DIR" "99.1.1" "test-feature"

# Create a feature branch (verify expects to be on a feature branch)
git -C "$FIXTURE_DIR" add -A
git -C "$FIXTURE_DIR" commit -m "Add WIP user story" --quiet
git -C "$FIXTURE_DIR" checkout -b feature/99.1.1-test-feature --quiet

# Do NOT create openspec change artifacts — this ensures the first
# blocking gate (OpenSpec verify) will fail.

# Create a minimal app/src directory so the command has something to scan
mkdir -p "$FIXTURE_DIR/app/src/main/java"
echo "// placeholder" > "$FIXTURE_DIR/app/src/main/java/Main.kt"
git -C "$FIXTURE_DIR" add -A
git -C "$FIXTURE_DIR" commit -m "Add source placeholder" --quiet

# --- Run ---
OUTPUT_FILE="$FIXTURE_DIR/eval_output.txt"
echo -e "  ${DIM}Running $EVAL_TOOL for $SCENARIO_NAME...${RESET}"
run_eval_with_args "$FIXTURE_DIR" "$COMMANDS_DIR/sdlc_verify_story.md" "$OUTPUT_FILE" "99.1.1" "15" || true

# --- Assert ---
begin_assertions

# The command should attempt and fail at an early gate
assert_output_contains "FAIL|fail|error|issue|stop|block" \
    "Output reports a gate failure"

# The blocking gate protocol should prevent proceeding
assert_output_not_contains "Definition of Done.*PASS|all gates passed|all.*verification.*passed" \
    "Command does not report all gates passed"

# Later gates should NOT be reached — if the first gate fails, the command
# should stop before reaching these downstream gates
assert_output_not_contains "Compose preview coverage.*PASS|preview coverage.*PASS" \
    "Compose preview coverage gate is not reached"

# The command should present the failure to the user
assert_output_contains "FAIL|failed|error|issue|cannot|stop" \
    "Output presents failure information to the user"

# --- Report & cleanup ---
report_scenario_result "$SCENARIO_NAME"
RESULT=$?
cleanup_fixture "$FIXTURE_DIR"
exit $RESULT
