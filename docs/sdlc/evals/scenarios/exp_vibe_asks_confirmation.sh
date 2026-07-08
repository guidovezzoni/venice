#!/usr/bin/env bash
# Scenario: sdlc_exp_vibe_a_story asks for user confirmation before proceeding.
# Fixture: git repo on main with a story in New state.
# Expected: output shows warning about reduced ceremony and asks for
#           confirmation. Does NOT proceed to Phase 1 (no sub-agent spawning).
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/../lib/eval_helpers.sh"

SCENARIO_NAME="exp_vibe_asks_confirmation"

# --- Setup ---
FIXTURE_DIR=$(create_fixture_dir "$SCENARIO_NAME")
create_story_command_base_fixture "$FIXTURE_DIR"
create_sample_story_new "$FIXTURE_DIR" "99.1.1" "test-feature"

git -C "$FIXTURE_DIR" add -A
git -C "$FIXTURE_DIR" commit -m "Add user story" --quiet

# --- Run ---
# Low max-turns: the command should ask for confirmation and stop there
# since there is no interactive user to say "yes".
OUTPUT_FILE="$FIXTURE_DIR/eval_output.txt"
echo -e "  ${DIM}Running $EVAL_TOOL for $SCENARIO_NAME...${RESET}"
run_eval_with_args "$FIXTURE_DIR" "$COMMANDS_DIR/sdlc_exp_vibe_a_story.md" "$OUTPUT_FILE" "99.1.1 test feature" "5" || true

# --- Assert ---
begin_assertions

# The command should present the warning about streamlined phases
assert_output_contains "three phase|streamline|Analyze.*Build.*Check|skip|OpenSpec" \
    "Output warns about streamlined flow or skipped artifacts"

# The command should ask for confirmation
assert_output_contains "proceed|continue|yes.*no|confirm" \
    "Output asks user to confirm before proceeding"

# The command should NOT have completed Phase 1 (no user said "yes")
assert_output_not_contains "Phase 1.*complete|Analyze.*complete|Analyze.*DONE" \
    "Phase 1 did not complete (no user confirmation given)"

# --- Report & cleanup ---
report_scenario_result "$SCENARIO_NAME"
RESULT=$?
cleanup_fixture "$FIXTURE_DIR"
exit "$RESULT"
