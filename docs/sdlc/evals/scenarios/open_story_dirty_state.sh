#!/usr/bin/env bash
# Scenario: sdlc_open_story detects uncommitted changes.
# Fixture: git repo on main with a modified tracked file (dirty working tree).
# Expected: output warns about uncommitted/dirty changes before proceeding.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/../lib/eval_helpers.sh"

SCENARIO_NAME="open_story_dirty_state"

# --- Setup ---
FIXTURE_DIR=$(create_fixture_dir "$SCENARIO_NAME")
create_story_command_base_fixture "$FIXTURE_DIR"
create_sample_story_new "$FIXTURE_DIR" "99.1.1" "test-feature"

# Commit the story files so they're tracked
git -C "$FIXTURE_DIR" add -A
git -C "$FIXTURE_DIR" commit -m "Add user story" --quiet

# Create dirty state: modify a tracked file without committing
echo "modified content" >> "$FIXTURE_DIR/AGENTS.md"

# --- Run ---
OUTPUT_FILE="$FIXTURE_DIR/eval_output.txt"
echo -e "  ${DIM}Running $EVAL_TOOL for $SCENARIO_NAME...${RESET}"
run_eval_with_args "$FIXTURE_DIR" "$COMMANDS_DIR/sdlc_open_story.md" "$OUTPUT_FILE" "" "10" || true

# --- Assert ---
begin_assertions

# The command should detect and report dirty/uncommitted state
assert_output_contains "uncommitted|dirty|modified|unstaged|changes" \
    "Output mentions uncommitted/dirty changes"

assert_output_contains "AGENTS.md" \
    "Output mentions the specific dirty file"

assert_output_contains "git status" \
    "Output mentions running git status"

# The command should warn/ask the user about proceeding
assert_output_contains "proceed|continue|warning|warn" \
    "Output warns about or asks about proceeding"

# --- Report & cleanup ---
report_scenario_result "$SCENARIO_NAME"
RESULT=$?
cleanup_fixture "$FIXTURE_DIR"
exit $RESULT
