#!/usr/bin/env bash
# Scenario: sdlc_open_story detects non-main branch.
# Fixture: git repo on a feature branch (not main).
# Expected: output detects non-main branch and asks whether to proceed or switch.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/../lib/eval_helpers.sh"

SCENARIO_NAME="open_story_not_on_main"

# --- Setup ---
FIXTURE_DIR=$(create_fixture_dir "$SCENARIO_NAME")
create_story_command_base_fixture "$FIXTURE_DIR"
create_sample_story_new "$FIXTURE_DIR" "99.1.1" "test-feature"

# Commit the story files
git -C "$FIXTURE_DIR" add -A
git -C "$FIXTURE_DIR" commit -m "Add user story" --quiet

# Switch to a non-main branch
git -C "$FIXTURE_DIR" checkout -b feature/existing-work --quiet

# --- Run ---
OUTPUT_FILE="$FIXTURE_DIR/eval_output.txt"
echo -e "  ${DIM}Running $EVAL_TOOL for $SCENARIO_NAME...${RESET}"
run_eval_with_args "$FIXTURE_DIR" "$COMMANDS_DIR/sdlc_open_story.md" "$OUTPUT_FILE" "" "10" || true

# --- Assert ---
begin_assertions

# The command should detect and report the current non-main branch
assert_output_contains "feature/existing-work|not on main|current branch" \
    "Output mentions the current non-main branch"

# The command should use git branch --show-current
assert_output_contains "branch" \
    "Output references branch detection"

# The command should ask the user about switching to main or staying
assert_output_contains "proceed|switch|main|continue|stay" \
    "Output asks about proceeding on current branch or switching to main"

# --- Report & cleanup ---
report_scenario_result "$SCENARIO_NAME"
RESULT=$?
cleanup_fixture "$FIXTURE_DIR"
exit $RESULT
