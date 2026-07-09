#!/usr/bin/env bash
# Scenario: implement_change detects infrastructure failure in connectedDebugAndroidTest
# Fixture:  All OpenSpec tasks complete, no TODOs, adb shows device connected,
#           gradlew connectedDebugAndroidTest returns "device offline" error
# Expected: Agent informs user of infrastructure failure and asks them to fix it;
#           it does NOT attempt to fix test or production code
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/../lib/eval_helpers.sh"

SCENARIO_NAME="implement_change_infra_fail"

# --- Setup ---
FIXTURE_DIR=$(create_fixture_dir "$SCENARIO_NAME")
create_story_command_base_fixture "$FIXTURE_DIR"

# Minimal Android source tree with no TODO comments (so the TODO scan finishes quickly)
mkdir -p "$FIXTURE_DIR/app/src/main/java"
cat > "$FIXTURE_DIR/app/src/main/java/MainActivity.kt" << 'EOF'
class MainActivity
EOF

# Stub executables placed in bin/ — exported to PATH so Claude subprocess finds them
mkdir -p "$FIXTURE_DIR/bin"

# openspec stub: status → active change; instructions apply → all tasks complete
cat > "$FIXTURE_DIR/bin/openspec" << 'OPENSPEC_EOF'
#!/usr/bin/env bash
case "$1 $2" in
  "status --json")
    echo '{"change": "test-infra-feature", "status": "active"}'
    ;;
  "instructions apply")
    echo '{"contextFiles": [], "tasks": [{"id": "1", "description": "Implement feature", "complete": true}], "progress": {"total": 1, "complete": 1, "remaining": 0}}'
    ;;
  *)
    echo '{}'
    ;;
esac
OPENSPEC_EOF
chmod +x "$FIXTURE_DIR/bin/openspec"

# adb stub: reports a connected emulator so the device gate passes
cat > "$FIXTURE_DIR/bin/adb" << 'ADB_EOF'
#!/usr/bin/env bash
if [[ "${1:-}" == "devices" ]]; then
  echo "List of devices attached"
  echo "emulator-5554	device"
fi
ADB_EOF
chmod +x "$FIXTURE_DIR/bin/adb"

# gradlew stub: infrastructure failure for connectedDebugAndroidTest; success for everything else
cat > "$FIXTURE_DIR/gradlew" << 'GRADLEW_EOF'
#!/usr/bin/env bash
case "${1:-}" in
  connectedDebugAndroidTest)
    echo "FAILURE: Build failed with an exception."
    echo ""
    echo "* What went wrong:"
    echo "com.android.ddmlib.AdbCommandRejectedException: device offline"
    echo "No connected devices found. Network connection to device unavailable."
    exit 1
    ;;
  *)
    echo "BUILD SUCCESSFUL"
    exit 0
    ;;
esac
GRADLEW_EOF
chmod +x "$FIXTURE_DIR/gradlew"

# Prepend fixture bin/ to PATH so the Claude subprocess finds the openspec and adb stubs
export PATH="$FIXTURE_DIR/bin:$PATH"

# --- Run ---
OUTPUT_FILE="$FIXTURE_DIR/eval_output.txt"
echo -e "  ${DIM}Running $EVAL_TOOL for $SCENARIO_NAME...${RESET}"
run_eval_with_args "$FIXTURE_DIR" "$COMMANDS_DIR/sdlc_implement_change.md" "$OUTPUT_FILE" "" "20" || true

# --- Assert ---
begin_assertions

assert_output_contains \
    "infrastructure|device offline|network|connectivity" \
    "agent identifies the failure as infrastructure-related"

assert_output_contains \
    "please|can you|fix|resolve" \
    "agent asks user to address the infrastructure issue"

# --- Report & cleanup ---
report_scenario_result "$SCENARIO_NAME"
RESULT=$?
cleanup_fixture "$FIXTURE_DIR"
exit $RESULT
