#!/usr/bin/env bash
# Shared helper library for SDLC execution evals (Phase 2 & 3).
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
# Shared fixture: guidelines, AGENTS.md, tracking plan, and the SDLC command
# files that reference them.
#
# Both doctors need these: sdlc_doctor has a Guidelines category (files exist,
# AGENTS.md references them, the artefact guidelines are referenced by an SDLC
# command), and sdlc_project_doctor has an Analytics category (tracking plan
# exists with the expected headings, AGENTS.md references it, and
# sdlc_open_story.md carries the analytics refinement hook).
# ---------------------------------------------------------------------------

create_guidelines_fixture() {
    local dir="$1"

    mkdir -p "$dir/docs/guidelines"
    for guideline in android git process analytics userstories reports; do
        echo "# guidelines-${guideline} placeholder" > "$dir/docs/guidelines/guidelines-${guideline}.md"
    done

    cat > "$dir/AGENTS.md" << 'EOF'
# AGENTS.md

## External File Loading

### Android Guidelines

For Native Android code style and best practices: @docs/guidelines/guidelines-android.md

### Analytics Guidelines

For analytics event design, naming, and delivery: @docs/guidelines/guidelines-analytics.md
The event dictionary is the tracking plan: @docs/analytics/tracking-plan.md

### Git Guidelines

For git operations and commit conventions: @docs/guidelines/guidelines-git.md

### General Guidelines

Read the following file immediately as it's relevant to all workflows: @docs/guidelines/guidelines-process.md

## Project Overview

Eval fixture project.
EOF

    mkdir -p "$dir/docs/analytics"
    cat > "$dir/docs/analytics/tracking-plan.md" << 'EOF'
# Tracking Plan

## Event Dictionary

| Event | Parameters | Trigger |
|-------|-----------|---------|
| `item_added` | `item_count` | An item is persisted |

## Parameter Reference

| Parameter | Type | Allowed values |
|-----------|------|----------------|
| `item_count` | Int | 0–n |

## User Properties

| Property | Type | Allowed values |
|----------|------|----------------|
| `item_count_band` | String | `0`, `1`, `2_5`, `6_plus` |
EOF

    mkdir -p "$dir/docs/sdlc/commands"
    cat > "$dir/docs/sdlc/commands/sdlc_open_story.md" << 'EOF'
# sdlc_open_story placeholder

   ## Context Files
   - User story guidelines: docs/guidelines/guidelines-userstories.md
   - Analytics guidelines: docs/guidelines/guidelines-analytics.md
   - Tracking plan (event dictionary): docs/analytics/tracking-plan.md

   ## Refinement Requirements

   1. Acceptance criteria
   2. An `## Analytics` section defining the events this feature must emit —
      their names, parameters, and the product question each answers.
   3. The steps required for the task to be considered complete
EOF

    cat > "$dir/docs/sdlc/commands/sdlc_verify_story.md" << 'EOF'
# sdlc_verify_story placeholder

   ## Context Files
   - Report guidelines: docs/guidelines/guidelines-reports.md
   - User story guidelines: docs/guidelines/guidelines-userstories.md
EOF
}

# ---------------------------------------------------------------------------
# Base fixture: sdlc_doctor (all checks passing)
# ---------------------------------------------------------------------------

create_doctor_base_fixture() {
    local dir="$1"

    create_guidelines_fixture "$dir"

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
    for cmd in sdlc_open_story sdlc_propose_change sdlc_implement_change sdlc_verify_story sdlc_exp_four_in_one sdlc_exp_vibe_a_story sdlc_doctor sdlc_project_doctor; do
        echo "# $cmd command placeholder" > "$sdlc_dir/$cmd.md"
    done
}

# ---------------------------------------------------------------------------
# Base fixture: sdlc_project_doctor (all checks passing)
# ---------------------------------------------------------------------------

create_project_doctor_base_fixture() {
    local dir="$1"

    create_guidelines_fixture "$dir"

    # detekt must be 2.x: 1.x registers no variant tasks on a current AGP and
    # loads no custom ruleset, both silently. composeRules 0.6.x is the line
    # published against detekt 2.x, so the pair has to move together.
    mkdir -p "$dir/gradle"
    cat > "$dir/gradle/libs.versions.toml" << 'EOF'
[versions]
detekt = "2.0.0-alpha.5"
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
detekt = { id = "dev.detekt", version.ref = "detekt" }
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

    # No `build: maxIssues` — the key was removed in detekt 2.x, where the gate
    # is `failOnSeverity` (default Error) plus warningsAsErrors promoting
    # warnings to it. validation makes an unknown key fail the build.
    mkdir -p "$dir/config/detekt"
    cat > "$dir/config/detekt/detekt.yml" << 'EOF'
config:
  validation: true
  warningsAsErrors: true

naming:
  FunctionNaming:
    ignoreAnnotated:
      - 'Composable'

style:
  UnusedImport:
    active: true
  WildcardImport:
    active: true

Compose:
  active: true
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
# Phase 3: Base fixture for story-level commands (open, propose, verify)
# ---------------------------------------------------------------------------

create_story_command_base_fixture() {
    local dir="$1"

    git -C "$dir" config user.name "Eval Fixture"
    git -C "$dir" config user.email "eval@fixture.test"

    mkdir -p "$dir/docs/guidelines"

    cat > "$dir/docs/guidelines/guidelines-userstories.md" << 'GUIDELINE_EOF'
# User Story Management Guidelines

This file is the single source of truth for how user stories are managed.

## Storage

User stories are stored as Markdown files in `docs/userstories/`.

## Index

The file `docs/userstories/index.md` is the master index of all user stories.

## File Naming

```
{id}-{slug}[-STATE].md
```

- **id**: hierarchical identifier (e.g. `1.2.4`)
- **slug**: kebab-case short description
- **STATE**: optional lifecycle suffix (`WIP` or `DONE`)

## Lifecycle

| State       | Filename pattern        | Example                        |
|-------------|-------------------------|--------------------------------|
| New         | `{id}-{slug}.md`        | `1.2.5-edit-stop.md`          |
| In Progress | `{id}-{slug}-WIP.md`    | `1.2.5-edit-stop-WIP.md`      |
| Done        | `{id}-{slug}-DONE.md`   | `1.2.5-edit-stop-DONE.md`     |

### Next User Story

When no specific story is specified, the next story to open is the first one in the index that is in the **New** state (i.e. not `-WIP` or `-DONE`).

### Opening a User Story

Rename the file by appending `-WIP` before the `.md` extension.

**Precondition:** The file must be in the **New** state.

### Closing a User Story

Rename the file by replacing the `-WIP` suffix with `-DONE` before the `.md` extension.

**Precondition:** The file must be in the **In Progress** state (`-WIP`).

### Renaming

File renames must use `git mv` so the change is tracked in version control.

### Index Update

After any state transition, update the link in `docs/userstories/index.md`.
GUIDELINE_EOF

    cat > "$dir/docs/guidelines/guidelines-git.md" << 'GUIDELINE_EOF'
# Git Guidelines

## Commit Message Format
1. First line: simple and concise summary of the change
2. Body (optional): extra detail if required

## File Operations
- Always use `git mv` instead of plain `mv` when renaming or moving tracked files

## Branch Verification
- Always verify the current branch by running `git branch --show-current`
GUIDELINE_EOF

    cat > "$dir/docs/guidelines/guidelines-reports.md" << 'GUIDELINE_EOF'
# Report Guidelines

Reports are HTML files in `docs/reports/`.
File naming: `{id}-{slug}.html`
One report per user story.
GUIDELINE_EOF

    cat > "$dir/docs/guidelines/guidelines-android.md" << 'GUIDELINE_EOF'
# Android Guidelines

- MVI Architecture with Clean Architecture (data/domain/UI)
- Jetpack Compose with Material 3
- Dependency Injection with Hilt
GUIDELINE_EOF

    cat > "$dir/docs/guidelines/guidelines-process.md" << 'GUIDELINE_EOF'
# Process and Workflow Guidelines

- Run `./gradlew clean` before coding
- Run `./gradlew check` after changes
- Run `./gradlew test` for unit tests
GUIDELINE_EOF

    mkdir -p "$dir/docs/userstories"
    mkdir -p "$dir/docs/reports"

    mkdir -p "$dir/openspec"
    cat > "$dir/openspec/config.yaml" << 'EOF'
schema: spec-driven
EOF

    cat > "$dir/AGENTS.md" << 'EOF'
# AGENTS.md
## Project Overview
Test project for SDLC evals.
EOF

    cat > "$dir/gradlew" << 'GRADLEW'
#!/bin/bash
exit 0
GRADLEW
    chmod +x "$dir/gradlew"

    git -C "$dir" add -A
    git -C "$dir" commit -m "Initial fixture setup" --quiet
    git -C "$dir" branch -M main
}

create_sample_story_new() {
    local dir="$1"
    local story_id="${2:-99.1.1}"
    local story_slug="${3:-test-feature}"

    cat > "$dir/docs/userstories/${story_id}-${story_slug}.md" << STORY_EOF
# ${story_id} — Test Feature

**Epic:** Test Epic
**Feature:** Test Feature

## User Story

As a user, I want to test a feature so that I can verify it works.

## Acceptance Criteria

- The feature should work correctly.
- The UI should update appropriately.
- Error states should be handled gracefully.
STORY_EOF

    cat > "$dir/docs/userstories/index.md" << INDEX_EOF
# User Stories Index

## Epic 99: Test

### Feature 99.1: Test Feature

- [${story_id} — Test Feature](${story_id}-${story_slug}.md)
INDEX_EOF
}

create_sample_story_wip() {
    local dir="$1"
    local story_id="${2:-99.1.1}"
    local story_slug="${3:-test-feature}"

    cat > "$dir/docs/userstories/${story_id}-${story_slug}-WIP.md" << STORY_EOF
# ${story_id} — Test Feature

**Epic:** Test Epic
**Feature:** Test Feature

## User Story

As a user, I want to test a feature so that I can verify it works.

## Acceptance Criteria

- The feature should work correctly.
- The UI should update appropriately.
- Error states should be handled gracefully.

## Definition of Done

- All acceptance criteria are met.
- Unit tests pass with >95% coverage.
- UI tests pass on a connected device.
STORY_EOF

    cat > "$dir/docs/userstories/index.md" << INDEX_EOF
# User Stories Index

## Epic 99: Test

### Feature 99.1: Test Feature

- [${story_id} — Test Feature](${story_id}-${story_slug}-WIP.md)
INDEX_EOF
}

create_sample_story_ambiguous() {
    local dir="$1"
    local story_id="${2:-99.1.1}"
    local story_slug="${3:-ambiguous-feature}"

    cat > "$dir/docs/userstories/${story_id}-${story_slug}-WIP.md" << STORY_EOF
# ${story_id} — Ambiguous Feature

**Epic:** Test Epic
**Feature:** Ambiguous Feature

## User Story

As a user, I want to manage items so that they are organised.

## Description

The feature should allow managing items. Details about the specific item types,
storage mechanism, and UI layout are intentionally unspecified.

## Acceptance Criteria

- Items can be managed.
- The feature integrates with existing functionality.
STORY_EOF

    cat > "$dir/docs/userstories/index.md" << INDEX_EOF
# User Stories Index

## Epic 99: Test

### Feature 99.1: Ambiguous Feature

- [${story_id} — Ambiguous Feature](${story_id}-${story_slug}-WIP.md)
INDEX_EOF
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

run_eval_with_args() {
    local fixture_dir="$1"
    local command_file="$2"
    local output_file="$3"
    local args="${4:-}"
    local max_turns="${5:-}"

    OUTPUT_FILE="$output_file"

    local prompt
    prompt="$(cat "$command_file")"
    prompt="${prompt//\$ARGUMENTS/$args}"

    local tool_args=(
        -p "$prompt"
        --dangerously-skip-permissions
        --no-session-persistence
        --output-format text
    )

    if [[ -n "$max_turns" ]]; then
        tool_args+=(--max-turns "$max_turns")
    fi

    local exit_code=0
    (cd "$fixture_dir" && "$EVAL_TOOL" "${tool_args[@]}") > "$output_file" 2>&1 || exit_code=$?

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

assert_file_exists() {
    local file_path="$1"
    local description="$2"

    if [[ -f "$file_path" ]]; then
        assertion_pass "$description"
    else
        assertion_fail "$description" "file not found: $file_path"
    fi
}

assert_file_not_exists() {
    local file_path="$1"
    local description="$2"

    if [[ ! -f "$file_path" ]]; then
        assertion_pass "$description"
    else
        assertion_fail "$description" "file should not exist: $file_path"
    fi
}

assert_branch_exists() {
    local fixture_dir="$1"
    local branch_pattern="$2"
    local description="$3"

    if git -C "$fixture_dir" branch 2>/dev/null | grep -qE "$branch_pattern"; then
        assertion_pass "$description"
    else
        assertion_fail "$description" "no branch matching '$branch_pattern'"
    fi
}

assert_on_branch() {
    local fixture_dir="$1"
    local expected_branch="$2"
    local description="$3"

    local current_branch
    current_branch=$(git -C "$fixture_dir" branch --show-current 2>/dev/null)

    if [[ "$current_branch" == "$expected_branch" ]]; then
        assertion_pass "$description"
    else
        assertion_fail "$description" "expected branch '$expected_branch', got '$current_branch'"
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
  "tool": "$EVAL_TOOL",
  "timestamp": "$(date -Iseconds)"
}
RESULT_EOF
    fi

    if [[ -n "$FIXTURE_SNAPSHOT" && -f "$FIXTURE_SNAPSHOT" ]]; then
        rm -f "$FIXTURE_SNAPSHOT"
    fi

    return "$ASSERTIONS_FAILED"
}
