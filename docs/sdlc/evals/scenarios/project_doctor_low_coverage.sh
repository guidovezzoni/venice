#!/usr/bin/env bash
# Scenario: Kover minBound set to 50 instead of 95.
# Fixture: complete except app/build.gradle.kts has minBound(50).
# Expected: ❌ for kover minBound check, other checks pass.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/../lib/eval_helpers.sh"

SCENARIO_NAME="project_doctor_low_coverage"

# --- Setup ---
FIXTURE_DIR=$(create_fixture_dir "$SCENARIO_NAME")
create_project_doctor_base_fixture "$FIXTURE_DIR"

# Override build.gradle.kts with low coverage threshold
cat > "$FIXTURE_DIR/app/build.gradle.kts" << 'EOF'
plugins {
    alias(libs.plugins.detekt)
    alias(libs.plugins.kover)
}

dependencies {
    detektPlugins(libs.detekt.compose.rules)
}

detekt {
    config.setFrom(files("$rootDir/config/detekt/detekt.yml"))
}

kover {
    reports {
        verify {
            rule {
                bound {
                    minBound(50)
                }
            }
        }
    }
}
EOF

snapshot_fixture "$FIXTURE_DIR"

# --- Run ---
OUTPUT_FILE="$FIXTURE_DIR/eval_output.txt"
echo -e "  ${DIM}Running $EVAL_TOOL for $SCENARIO_NAME...${RESET}"
run_eval "$FIXTURE_DIR" "$COMMANDS_DIR/sdlc_project_doctor.md" "$OUTPUT_FILE" || true

# --- Assert ---
begin_assertions

# Kover minBound check should fail
assert_check_fails "minBound|min.bound|bound.*95|coverage.*threshold|95" \
    "kover minBound check fails"

# Other kover checks should pass (plugin declared and applied)
assert_check_passes "kover.*plugin|plugin.*kover|kover.*toml|kover.*appl" \
    "kover plugin still passes"

# Non-kover categories should pass
assert_check_passes "detekt" "detekt checks still pass"
assert_check_passes "junit|JUnit" "unit test checks still pass"
assert_check_passes "ci.yml|workflow" "CI/CD checks still pass"

# Summary should indicate failure
assert_output_contains "failed" "summary mentions failure"
assert_min_fail_count 1

# Read-only guardrail
assert_fixture_unchanged "$FIXTURE_DIR"

# --- Report & cleanup ---
report_scenario_result "$SCENARIO_NAME"
RESULT=$?
cleanup_fixture "$FIXTURE_DIR"
exit $RESULT
