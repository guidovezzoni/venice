#!/usr/bin/env bash
# Shared helper library for SDLC execution evals (Phase 2).
# Sourced by scenario scripts — not executed directly.

set -euo pipefail

EVAL_LIB_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
EVAL_ROOT="$(cd "$EVAL_LIB_DIR/.." && pwd)"
PROJECT_ROOT="$(cd "$EVAL_ROOT/../../.." && pwd)"
COMMANDS_DIR="$PROJECT_ROOT/docs/sdlc/commands"

EVAL_TOOL="${EVAL_TOOL:-claude}"

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
BOLD='\033[1m'
DIM='\033[2m'
RESET='\033[0m'

ASSERTIONS_PASSED=0
ASSERTIONS_FAILED=0
ASSERTIONS_TOTAL=0
OUTPUT_FILE=""
FIXTURE_SNAPSHOT=""

# ---------------------------------------------------------------------------
# Fixture lifecycle
# ---------------------------------------------------------------------------

create_fixture_dir() {
    local scenario_name="$1"
    local fixture_dir
    fixture_dir=$(mktemp -d "/tmp/sdlc_eval_${scenario_name}_XXXXXX")
    git -C "$fixture_dir" init --quiet
    echo "$fixture_dir"
}

cleanup_fixture() {
    local fixture_dir="$1"
    if [[ -d "$fixture_dir" && "$fixture_dir" == /tmp/sdlc_eval_* ]]; then
        rm -rf "$fixture_dir"
    fi
}

snapshot_fixture() {
    local fixture_dir="$1"
    FIXTURE_SNAPSHOT=$(mktemp "/tmp/sdlc_eval_snapshot_XXXXXX")
    find "$fixture_dir" -not -path '*/.git/*' -not -path '*/.git' -not -name 'eval_output.txt' -type f | sort > "$FIXTURE_SNAPSHOT"
}

# ---------------------------------------------------------------------------
# Base fixture: sdlc_doctor (all checks passing)
# ---------------------------------------------------------------------------

create_doctor_base_fixture() {
    local dir="$1"

    mkdir -p "$dir/openspec"
    cat > "$dir/openspec/config.yaml" << 'EOF'
schema: spec-driven
EOF

    mkdir -p "$dir/.claude"
    cat > "$dir/.claude/settings.json" << 'EOF'
{
  "enabledPlugins": {
    "security-guidance@claude-plugins-official": true
  }
}
EOF

    local opsx_dir="$dir/.claude/commands/opsx"
    mkdir -p "$opsx_dir"
    for cmd in explore propose apply verify sync archive; do
        echo "# $cmd command placeholder" > "$opsx_dir/$cmd.md"
    done

    local sdlc_dir="$dir/.claude/commands/sdlc"
    mkdir -p "$sdlc_dir"
    for cmd in sdlc_open_story sdlc_propose_change sdlc_implement_change sdlc_verify_story sdlc_doctor sdlc_project_doctor; do
        echo "# $cmd command placeholder" > "$sdlc_dir/$cmd.md"
    done
}

# ---------------------------------------------------------------------------
# Base fixture: sdlc_project_doctor (all checks passing)
# ---------------------------------------------------------------------------

create_project_doctor_base_fixture() {
    local dir="$1"

    mkdir -p "$dir/gradle"
    cat > "$dir/gradle/libs.versions.toml" << 'EOF'
[versions]
detekt = "1.23.8"
composeRules = "0.6.2"
kover = "0.9.8"
junit = "4.13.2"
mockk = "1.14.9"
coroutinesTest = "1.10.2"

[libraries]
junit = { group = "junit", name = "junit", version.ref = "junit" }
mockk = { group = "io.mockk", name = "mockk", version.ref = "mockk" }
kotlinx-coroutines-test = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-test", version.ref = "coroutinesTest" }
detekt-compose-rules = { group = "io.nlopez.compose.rules", name = "detekt", version.ref = "composeRules" }

[plugins]
detekt = { id = "io.gitlab.arturbosch.detekt", version.ref = "detekt" }
kover = { id = "org.jetbrains.kotlinx.kover", version.ref = "kover" }
EOF

    mkdir -p "$dir/app"
    cat > "$dir/app/build.gradle.kts" << 'EOF'
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
                    minBound(95)
                }
            }
        }
    }
}
EOF

    mkdir -p "$dir/config/detekt"
    cat > "$dir/config/detekt/detekt.yml" << 'EOF'
build:
  maxIssues: 0
EOF

    mkdir -p "$dir/app/src/test/java"
    cat > "$dir/app/src/test/java/SampleTest.kt" << 'EOF'
import org.junit.Test
class SampleTest {
    @Test fun placeholder() = Unit
}
EOF

    mkdir -p "$dir/fastlane"
    echo '# Fastfile placeholder' > "$dir/fastlane/Fastfile"

    echo 'source "https://rubygems.org"' > "$dir/Gemfile"

    mkdir -p "$dir/.github/workflows"
    cat > "$dir/.github/workflows/ci.yml" << 'EOF'
name: CI
on: [push]
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Run checks
        run: ./gradlew check
      - name: Verify coverage
        run: ./gradlew koverVerify
EOF

    cat > "$dir/gradlew" << 'GRADLEW'
#!/bin/bash
exit 0
GRADLEW
    chmod +x "$dir/gradlew"
}

# ---------------------------------------------------------------------------
# Eval execution
# ---------------------------------------------------------------------------

run_eval() {
    local fixture_dir="$1"
    local command_file="$2"
    local output_file="$3"

    OUTPUT_FILE="$output_file"

    local prompt
    prompt="$(cat "$command_file")"

    local exit_code=0
    (cd "$fixture_dir" && "$EVAL_TOOL" \
        -p "$prompt" \
        --dangerously-skip-permissions \
        --no-session-persistence \
        --output-format text \
    ) > "$output_file" 2>&1 || exit_code=$?

    return $exit_code
}

# ---------------------------------------------------------------------------
# Assertion helpers
# ---------------------------------------------------------------------------

begin_assertions() {
    ASSERTIONS_PASSED=0
    ASSERTIONS_FAILED=0
    ASSERTIONS_TOTAL=0
}

assertion_pass() {
    local description="$1"
    ASSERTIONS_TOTAL=$((ASSERTIONS_TOTAL + 1))
    ASSERTIONS_PASSED=$((ASSERTIONS_PASSED + 1))
    echo -e "    ${GREEN}✅${RESET} $description"
}

assertion_fail() {
    local description="$1"
    local detail="${2:-}"
    ASSERTIONS_TOTAL=$((ASSERTIONS_TOTAL + 1))
    ASSERTIONS_FAILED=$((ASSERTIONS_FAILED + 1))
    if [[ -n "$detail" ]]; then
        echo -e "    ${RED}❌${RESET} $description — $detail"
    else
        echo -e "    ${RED}❌${RESET} $description"
    fi
}

assert_check_passes() {
    local pattern="$1"
    local description="$2"

    if grep -q "✅" "$OUTPUT_FILE" && grep "✅" "$OUTPUT_FILE" | grep -qiE "$pattern"; then
        assertion_pass "$description"
    else
        assertion_fail "$description" "expected ✅ line containing '$pattern'"
    fi
}

assert_check_fails() {
    local pattern="$1"
    local description="$2"

    if grep -q "❌" "$OUTPUT_FILE" && grep "❌" "$OUTPUT_FILE" | grep -qiE "$pattern"; then
        assertion_pass "$description"
    else
        assertion_fail "$description" "expected ❌ line containing '$pattern'"
    fi
}

assert_output_contains() {
    local pattern="$1"
    local description="$2"

    if grep -qiE "$pattern" "$OUTPUT_FILE"; then
        assertion_pass "$description"
    else
        assertion_fail "$description" "expected output containing '$pattern'"
    fi
}

assert_output_not_contains() {
    local pattern="$1"
    local description="$2"

    if grep -qiE "$pattern" "$OUTPUT_FILE"; then
        assertion_fail "$description" "output should NOT contain '$pattern'"
    else
        assertion_pass "$description"
    fi
}

assert_pass_count() {
    local expected="$1"
    local actual
    actual=$(grep -c "✅" "$OUTPUT_FILE" 2>/dev/null || true)

    if [[ "$actual" -eq "$expected" ]]; then
        assertion_pass "pass count is $expected"
    else
        assertion_fail "pass count should be $expected" "got $actual"
    fi
}

assert_fail_count() {
    local expected="$1"
    local actual
    actual=$(grep -c "❌" "$OUTPUT_FILE" 2>/dev/null || true)

    if [[ "$actual" -eq "$expected" ]]; then
        assertion_pass "fail count is $expected"
    else
        assertion_fail "fail count should be $expected" "got $actual"
    fi
}

assert_min_pass_count() {
    local minimum="$1"
    local actual
    actual=$(grep -c "✅" "$OUTPUT_FILE" 2>/dev/null || true)

    if [[ "$actual" -ge "$minimum" ]]; then
        assertion_pass "pass count >= $minimum (got $actual)"
    else
        assertion_fail "pass count should be >= $minimum" "got $actual"
    fi
}

assert_min_fail_count() {
    local minimum="$1"
    local actual
    actual=$(grep -c "❌" "$OUTPUT_FILE" 2>/dev/null || true)

    if [[ "$actual" -ge "$minimum" ]]; then
        assertion_pass "fail count >= $minimum (got $actual)"
    else
        assertion_fail "fail count should be >= $minimum" "got $actual"
    fi
}

assert_fixture_unchanged() {
    local fixture_dir="$1"

    if [[ -z "$FIXTURE_SNAPSHOT" || ! -f "$FIXTURE_SNAPSHOT" ]]; then
        assertion_fail "fixture unchanged" "no snapshot taken"
        return
    fi

    local current
    current=$(mktemp "/tmp/sdlc_eval_current_XXXXXX")
    find "$fixture_dir" -not -path '*/.git/*' -not -path '*/.git' -not -name 'eval_output.txt' -type f | sort > "$current"

    if diff -q "$FIXTURE_SNAPSHOT" "$current" > /dev/null 2>&1; then
        assertion_pass "no files created or removed in fixture"
    else
        local diff_output
        diff_output=$(diff "$FIXTURE_SNAPSHOT" "$current" || true)
        assertion_fail "fixture was modified" "diff: $diff_output"
    fi
    rm -f "$current"
}

# ---------------------------------------------------------------------------
# Scenario result
# ---------------------------------------------------------------------------

report_scenario_result() {
    local scenario_name="$1"

    echo ""
    if [[ "$ASSERTIONS_FAILED" -eq 0 ]]; then
        echo -e "  ${GREEN}${BOLD}PASS${RESET} $scenario_name — $ASSERTIONS_PASSED/$ASSERTIONS_TOTAL assertions passed"
    else
        echo -e "  ${RED}${BOLD}FAIL${RESET} $scenario_name — $ASSERTIONS_PASSED/$ASSERTIONS_TOTAL passed, $ASSERTIONS_FAILED failed"
    fi

    # Output JSON result to results dir if RESULTS_DIR is set
    if [[ -n "${RESULTS_DIR:-}" ]]; then
        cat > "$RESULTS_DIR/${scenario_name}.json" << RESULT_EOF
{
  "scenario": "$scenario_name",
  "passed": $ASSERTIONS_PASSED,
  "failed": $ASSERTIONS_FAILED,
  "total": $ASSERTIONS_TOTAL,
  "result": "$([ "$ASSERTIONS_FAILED" -eq 0 ] && echo "PASS" || echo "FAIL")",
  "timestamp": "$(date -Iseconds)"
}
RESULT_EOF
    fi

    if [[ -n "$FIXTURE_SNAPSHOT" && -f "$FIXTURE_SNAPSHOT" ]]; then
        rm -f "$FIXTURE_SNAPSHOT"
    fi

    return "$ASSERTIONS_FAILED"
}
