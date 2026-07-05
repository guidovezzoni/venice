#!/usr/bin/env bash
# Scenario: sdlc_propose_change surfaces questions for ambiguous requirements.
# Fixture: WIP story with deliberately vague/ambiguous requirements.
# Expected: the command surfaces questions instead of making assumptions.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/../lib/eval_helpers.sh"

SCENARIO_NAME="propose_change_surfaces_questions"

# --- Setup ---
FIXTURE_DIR=$(create_fixture_dir "$SCENARIO_NAME")
create_story_command_base_fixture "$FIXTURE_DIR"
create_sample_story_ambiguous "$FIXTURE_DIR" "99.1.1" "ambiguous-feature"

# Create minimal project structure for exploration
mkdir -p "$FIXTURE_DIR/app/src/main/java/com/example/test"
cat > "$FIXTURE_DIR/app/src/main/java/com/example/test/MainActivity.kt" << 'KOTLIN_EOF'
package com.example.test

import android.os.Bundle
import androidx.activity.ComponentActivity

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }
}
KOTLIN_EOF

git -C "$FIXTURE_DIR" add -A
git -C "$FIXTURE_DIR" commit -m "Add WIP story and project structure" --quiet
git -C "$FIXTURE_DIR" checkout -b feature/99.1.1-ambiguous-feature --quiet

# --- Run ---
OUTPUT_FILE="$FIXTURE_DIR/eval_output.txt"
echo -e "  ${DIM}Running $EVAL_TOOL for $SCENARIO_NAME...${RESET}"
run_eval_with_args "$FIXTURE_DIR" "$COMMANDS_DIR/sdlc_propose_change.md" "$OUTPUT_FILE" "99.1.1" "15" || true

# --- Assert ---
begin_assertions

# The command should surface questions about the ambiguous requirements
assert_output_contains "question|clarif|ambig|unclear|what.*type|which.*storage|what.*layout" \
    "Output surfaces questions about ambiguous requirements"

# The "no assumptions" guardrail should be honoured
assert_output_not_contains "I will assume|I'll assume|assuming that|let's assume" \
    "Output does not make assumptions (guardrail honoured)"

# The command should identify the story
assert_output_contains "99.1.1|ambiguous|manage.*item" \
    "Output identifies the target user story"

# The exploration phase should produce findings
assert_output_contains "explor|investigat|codebase|finding|architecture|integration" \
    "Output shows exploration activity"

# --- Report & cleanup ---
report_scenario_result "$SCENARIO_NAME"
RESULT=$?
cleanup_fixture "$FIXTURE_DIR"
exit $RESULT
