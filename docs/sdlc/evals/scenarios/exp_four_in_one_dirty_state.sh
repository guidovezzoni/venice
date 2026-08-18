#!/usr/bin/env bash
# Scenario: sdlc_exp_four_in_one detects uncommitted changes.
# Fixture: git repo on main with a modified tracked file (dirty working tree).
# Expected: the orchestrator detects dirty state and warns about uncommitted
#           changes before launching any phase sub-agent.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/../lib/eval_helpers.sh"

SCENARIO_NAME="exp_four_in_one_dirty_state"

# --- Setup ---
FIXTURE_DIR=$(create_fixture_dir "$SCENARIO_NAME")
create_story_command_base_fixture "$FIXTURE_DIR"
create_sample_story_new "$FIXTURE_DIR" "99.1.1" "test-feature"

git -C "$FIXTURE_DIR" add -A
git -C "$FIXTURE_DIR" commit -m "Add user story" --quiet

# Create dirty state: modify a tracked file without committing
echo "modified content" >> "$FIXTURE_DIR/AGENTS.md"

# --- Run ---
OUTPUT_FILE="$FIXTURE_DIR/eval_output.txt"
echo -e "  ${DIM}Running $EVAL_TOOL for $SCENARIO_NAME...${RESET}"

PROMPT="$(cat "$COMMANDS_DIR/sdlc_exp_four_in_one.md")"
PROMPT="${PROMPT//\$ARGUMENTS/99.1.1 test feature}"

local_exit_code=0
(cd "$FIXTURE_DIR" && "$EVAL_TOOL" \
    -p "$PROMPT" \
    --dangerously-skip-permissions \
    --no-session-persistence \
    --output-format text \
    --max-turns 10 \
) > "$OUTPUT_FILE" 2>&1 || local_exit_code=$?

# --- Assert ---
begin_assertions

# The orchestrator should detect and report dirty/uncommitted state
assert_output_contains "uncommitted|dirty|modified|unstaged|changes" \
    "Output mentions uncommitted/dirty changes"

assert_output_contains "AGENTS.md" \
    "Output mentions the specific dirty file"

# The orchestrator should warn/ask the user about proceeding
assert_output_contains "proceed|continue|warning|warn" \
    "Output warns about or asks about proceeding with dirty state"

# --- Report & cleanup ---
report_scenario_result "$SCENARIO_NAME"
RESULT=$?
cleanup_fixture "$FIXTURE_DIR"
exit "$RESULT"
