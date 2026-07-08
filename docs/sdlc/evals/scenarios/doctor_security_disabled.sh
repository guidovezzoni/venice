#!/usr/bin/env bash
# Scenario: Security review plugin is disabled in settings.
# Fixture: complete except security-guidance plugin set to false.
# Expected: ❌ for security check, all others ✅, summary shows 1 failure.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/../lib/eval_helpers.sh"

SCENARIO_NAME="doctor_security_disabled"

# --- Setup ---
FIXTURE_DIR=$(create_fixture_dir "$SCENARIO_NAME")
create_doctor_base_fixture "$FIXTURE_DIR"

# Override settings.json with plugin disabled
cat > "$FIXTURE_DIR/.claude/settings.json" << 'EOF'
{
  "enabledPlugins": {
    "security-guidance@claude-plugins-official": false
  }
}
EOF

snapshot_fixture "$FIXTURE_DIR"

# --- Run ---
OUTPUT_FILE="$FIXTURE_DIR/eval_output.txt"
echo -e "  ${DIM}Running $EVAL_TOOL for $SCENARIO_NAME...${RESET}"
run_eval "$FIXTURE_DIR" "$COMMANDS_DIR/sdlc_doctor.md" "$OUTPUT_FILE" || true

# --- Assert ---
begin_assertions

# Security check should fail
assert_check_fails "security" "security plugin check fails"

# OpenSpec checks should pass
assert_check_passes "openspec/config.yaml" "openspec/config.yaml passes"
assert_check_passes "explore.md" "opsx/explore.md passes"
assert_check_passes "archive.md" "opsx/archive.md passes"

# SDLC commands should pass
assert_check_passes "sdlc_open_story" "sdlc_open_story passes"
assert_check_passes "sdlc_doctor" "sdlc_doctor passes"

# Summary should show failure
assert_output_contains "failed" "summary mentions failure"
assert_min_fail_count 1

# Read-only guardrail
assert_fixture_unchanged "$FIXTURE_DIR"

# --- Report & cleanup ---
report_scenario_result "$SCENARIO_NAME"
RESULT=$?
cleanup_fixture "$FIXTURE_DIR"
exit $RESULT
