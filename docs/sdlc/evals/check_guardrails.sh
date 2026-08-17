#!/usr/bin/env bash
# Static guardrail assertions for SDLC command files.
# Phase 1 eval — no API calls, no dependencies, runs in <1 second.
#
# Usage:
#   ./docs/sdlc/evals/check_guardrails.sh              # Run from project root
#   ./docs/sdlc/evals/check_guardrails.sh --verbose     # Show assertion details on pass

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../../.." && pwd)"
COMMANDS_DIR="$PROJECT_ROOT/docs/sdlc/commands"

PASS=0
FAIL=0
TOTAL=0

RED='\033[0;31m'
GREEN='\033[0;32m'
BOLD='\033[1m'
RESET='\033[0m'

VERBOSE=false
if [[ "${1:-}" == "--verbose" ]]; then
    VERBOSE=true
fi

assert_contains() {
    local file="$COMMANDS_DIR/$1"
    local pattern="$2"
    local description="$3"
    TOTAL=$((TOTAL + 1))
    if grep -q "$pattern" "$file" 2>/dev/null; then
        PASS=$((PASS + 1))
        if $VERBOSE; then
            echo -e "  ${GREEN}✅${RESET} $description"
        fi
    else
        echo -e "  ${RED}❌${RESET} $description — pattern not found: $pattern"
        FAIL=$((FAIL + 1))
    fi
}

assert_contains_regex() {
    local file="$COMMANDS_DIR/$1"
    local pattern="$2"
    local description="$3"
    TOTAL=$((TOTAL + 1))
    if grep -qE "$pattern" "$file" 2>/dev/null; then
        PASS=$((PASS + 1))
        if $VERBOSE; then
            echo -e "  ${GREEN}✅${RESET} $description"
        fi
    else
        echo -e "  ${RED}❌${RESET} $description — pattern not found: $pattern"
        FAIL=$((FAIL + 1))
    fi
}

assert_not_contains() {
    local file="$COMMANDS_DIR/$1"
    local pattern="$2"
    local description="$3"
    TOTAL=$((TOTAL + 1))
    if grep -q "$pattern" "$file" 2>/dev/null; then
        echo -e "  ${RED}❌${RESET} $description — pattern should NOT be present: $pattern"
        FAIL=$((FAIL + 1))
    else
        PASS=$((PASS + 1))
        if $VERBOSE; then
            echo -e "  ${GREEN}✅${RESET} $description"
        fi
    fi
}

section() {
    echo ""
    echo -e "${BOLD}### $1${RESET}"
}

# ─────────────────────────────────────────────────────────────────────
# Verify all command files exist
# ─────────────────────────────────────────────────────────────────────
section "Command File Existence"

for cmd in sdlc_open_story.md sdlc_propose_change.md sdlc_implement_change.md \
           sdlc_verify_story.md sdlc_doctor.md sdlc_project_doctor.md \
           sdlc_exp_four_in_one.md sdlc_exp_vibe_a_story.md; do
    TOTAL=$((TOTAL + 1))
    if [[ -f "$COMMANDS_DIR/$cmd" ]]; then
        PASS=$((PASS + 1))
        if $VERBOSE; then
            echo -e "  ${GREEN}✅${RESET} $cmd exists"
        fi
    else
        echo -e "  ${RED}❌${RESET} $cmd exists — file not found"
        FAIL=$((FAIL + 1))
    fi
done

# ─────────────────────────────────────────────────────────────────────
# sdlc_open_story.md
# ─────────────────────────────────────────────────────────────────────
section "sdlc_open_story.md — Safety Guardrails"

assert_contains "sdlc_open_story.md" "DO NOT MAKE ASSUMPTIONS" \
    "Contains DO NOT MAKE ASSUMPTIONS guardrail"

assert_contains "sdlc_open_story.md" "DO NOT DELETE DATA" \
    "Contains DO NOT DELETE DATA guardrail"

assert_contains "sdlc_open_story.md" "git mv" \
    "Uses git mv (not plain mv) for file operations"

assert_contains "sdlc_open_story.md" "git status" \
    "Checks git status for uncommitted changes"

assert_contains "sdlc_open_story.md" "git branch --show-current" \
    "Verifies current branch with git command"

section "sdlc_open_story.md — Sub-agent Orchestration"

assert_contains_regex "sdlc_open_story.md" 'model.*haiku|haiku.*model' \
    "Assigns haiku model to sub-agents"

assert_contains_regex "sdlc_open_story.md" 'model.*opus|opus.*model' \
    "Assigns opus model to refinement sub-agent"

assert_contains "sdlc_open_story.md" "Sub-agent orchestration" \
    "Documents sub-agent orchestration strategy"

assert_contains "sdlc_open_story.md" "Failure handling" \
    "Includes failure handling for sub-agents"

assert_contains "sdlc_open_story.md" "Verification" \
    "Includes verification after sub-agent returns"

section "sdlc_open_story.md — Workflow"

assert_contains "sdlc_open_story.md" "Suggest a commit message" \
    "Ends with commit message suggestion"

assert_contains "sdlc_open_story.md" "guidelines-userstories.md" \
    "References user story guidelines"

assert_contains "sdlc_open_story.md" "guidelines-git.md" \
    "References git guidelines"

assert_contains "sdlc_open_story.md" "guidelines-reports.md" \
    "References report guidelines"

# ─────────────────────────────────────────────────────────────────────
# sdlc_propose_change.md
# ─────────────────────────────────────────────────────────────────────
section "sdlc_propose_change.md — Safety Guardrails"

assert_contains "sdlc_propose_change.md" "DO NOT MAKE ASSUMPTIONS" \
    "Contains DO NOT MAKE ASSUMPTIONS guardrail"

assert_contains "sdlc_propose_change.md" "Do NOT write any code or create any files" \
    "Exploration sub-agent is read-only"

section "sdlc_propose_change.md — Sub-agent Orchestration"

assert_contains_regex "sdlc_propose_change.md" 'model.*sonnet|sonnet.*model' \
    "Assigns sonnet model to exploration/proposal sub-agents"

assert_contains_regex "sdlc_propose_change.md" 'model.*haiku|haiku.*model' \
    "Assigns haiku model to report sub-agent"

assert_contains "sdlc_propose_change.md" "Sub-agent orchestration" \
    "Documents sub-agent orchestration strategy"

assert_contains "sdlc_propose_change.md" "Failure handling" \
    "Includes failure handling for sub-agents"

section "sdlc_propose_change.md — BDD Task Structuring"

assert_contains "sdlc_propose_change.md" "BDD" \
    "References BDD methodology"

assert_contains "sdlc_propose_change.md" "GIVEN/WHEN/THEN" \
    "References GIVEN/WHEN/THEN test naming"

assert_contains "sdlc_propose_change.md" "test-first" \
    "Enforces test-first ordering"

assert_contains "sdlc_propose_change.md" "test task (write the test first)" \
    "Defines test-first task ordering"

assert_contains "sdlc_propose_change.md" "implementation task" \
    "Defines implementation tasks that follow tests"

section "sdlc_propose_change.md — Workflow"

assert_contains "sdlc_propose_change.md" "Suggest a commit message" \
    "Ends with commit message suggestion"

assert_contains "sdlc_propose_change.md" "guidelines-reports.md" \
    "References report guidelines"

assert_contains_regex "sdlc_propose_change.md" 'opsx:explore|openspec.*explore' \
    "Invokes OpenSpec explore"

assert_contains_regex "sdlc_propose_change.md" 'opsx:propose|openspec.*propose' \
    "Invokes OpenSpec propose"

# ─────────────────────────────────────────────────────────────────────
# sdlc_implement_change.md
# ─────────────────────────────────────────────────────────────────────
section "sdlc_implement_change.md — Safety Guardrails"

assert_contains "sdlc_implement_change.md" "adb devices" \
    "Checks device connectivity with adb devices"

assert_contains_regex "sdlc_implement_change.md" '[Dd]evice gate' \
    "Defines device gate protocol"

assert_contains "sdlc_implement_change.md" "BLOCK" \
    "Blocks on missing device"

assert_contains "sdlc_implement_change.md" "security-review" \
    "Runs security review"

assert_contains_regex "sdlc_implement_change.md" 'spawn a sub-agent.*security|security.*sub-agent' \
    "Security review is delegated to a sub-agent (not invoked inline)"

assert_contains "sdlc_implement_change.md" "Do NOT invoke" \
    "Explicitly forbids direct inline invocation of security-review skill"

section "sdlc_implement_change.md — Sub-agent Orchestration"

assert_contains_regex "sdlc_implement_change.md" 'model.*sonnet|sonnet.*model' \
    "Assigns sonnet model to BDD sections"

assert_contains_regex "sdlc_implement_change.md" 'model.*haiku|haiku.*model' \
    "Assigns haiku model to mechanical sections"

assert_contains "sdlc_implement_change.md" "Sub-agent orchestration" \
    "Documents sub-agent orchestration strategy"

assert_contains_regex "sdlc_implement_change.md" "Retry the section|still failing" \
    "Includes retry/failure handling for sub-agents"

section "sdlc_implement_change.md — BDD Discipline"

assert_contains "sdlc_implement_change.md" "RED confirmed" \
    "Requires RED confirmation in BDD cycle"

assert_contains "sdlc_implement_change.md" "GREEN confirmed" \
    "Requires GREEN confirmation in BDD cycle"

assert_contains "sdlc_implement_change.md" "BDD Execution Discipline" \
    "Contains BDD execution discipline section"

assert_contains_regex "sdlc_implement_change.md" 'test.*fail|Verify RED' \
    "Verifies test fails before implementation (RED)"

assert_contains_regex "sdlc_implement_change.md" 'test.*pass|Verify GREEN' \
    "Verifies test passes after implementation (GREEN)"

section "sdlc_implement_change.md — Task Management"

assert_contains_regex "sdlc_implement_change.md" '\- \[ \].*\- \[x\]|\[x\]' \
    "Tracks task completion via checkboxes"

assert_contains "sdlc_implement_change.md" "openspec status" \
    "Reads active change from OpenSpec"

assert_contains "sdlc_implement_change.md" "TODO" \
    "Scans for unresolved TODOs"

assert_contains "sdlc_implement_change.md" "RESOLVE NOW" \
    "Classifies TODOs as RESOLVE NOW vs ACKNOWLEDGED"

section "sdlc_implement_change.md — Workflow"

assert_contains "sdlc_implement_change.md" "Suggest a commit message" \
    "Ends with commit message suggestion"

assert_contains "sdlc_implement_change.md" "guidelines-reports.md" \
    "References report guidelines"

assert_contains "sdlc_implement_change.md" "connectedDebugAndroidTest" \
    "Runs instrumented UI tests"

assert_contains "sdlc_implement_change.md" "README.md" \
    "Updates README.md if affected"

assert_contains "sdlc_implement_change.md" "AGENTS.md" \
    "Updates AGENTS.md if affected"

# ─────────────────────────────────────────────────────────────────────
# sdlc_verify_story.md
# ─────────────────────────────────────────────────────────────────────
section "sdlc_verify_story.md — Blocking Gate Protocol"

assert_contains "sdlc_verify_story.md" "BLOCKING GATE" \
    "Defines BLOCKING GATE protocol"

assert_contains "sdlc_verify_story.md" "blocking gate" \
    "References blocking gates in description"

assert_contains "sdlc_verify_story.md" "NOT_FEASIBLE" \
    "Handles NOT_FEASIBLE result for on-device tests"

assert_contains "sdlc_verify_story.md" "STOP" \
    "Stops on gate failure"

assert_contains_regex "sdlc_verify_story.md" 'PASS.*proceed|proceed.*PASS' \
    "Proceeds only on PASS"

assert_contains_regex "sdlc_verify_story.md" 'FAIL.*STOP|STOP.*FAIL' \
    "Stops on FAIL"

section "sdlc_verify_story.md — Device Connectivity"

assert_contains "sdlc_verify_story.md" "adb devices" \
    "Checks device connectivity with adb devices"

assert_contains_regex "sdlc_verify_story.md" '[Dd]evice gate' \
    "Defines device gate protocol"

section "sdlc_verify_story.md — Verification Gates"

assert_contains_regex "sdlc_verify_story.md" 'OpenSpec verify|opsx:verify' \
    "Gate: OpenSpec verify"

assert_contains "sdlc_verify_story.md" "TODO scan" \
    "Gate: TODO scan"

assert_contains "sdlc_verify_story.md" "security-review" \
    "Gate: security review"

assert_contains_regex "sdlc_verify_story.md" 'clean build|gradlew clean check' \
    "Gate: clean build and static analysis"

assert_contains_regex "sdlc_verify_story.md" 'unit test|gradlew test' \
    "Gate: unit tests"

assert_contains_regex "sdlc_verify_story.md" 'coverage report|koverXmlReport' \
    "Gate: coverage report"

assert_contains "sdlc_verify_story.md" "Test file coverage" \
    "Gate: test file coverage"

assert_contains_regex "sdlc_verify_story.md" 'Compose preview coverage|preview coverage' \
    "Gate: compose preview coverage"

assert_contains_regex "sdlc_verify_story.md" 'On-device tests|connectedDebugAndroidTest' \
    "Gate: on-device tests"

assert_contains "sdlc_verify_story.md" "Definition of Done" \
    "Gate: definition of done"

section "sdlc_verify_story.md — Sub-agent Orchestration"

assert_contains_regex "sdlc_verify_story.md" 'model.*sonnet|sonnet.*model' \
    "Assigns sonnet model to reasoning-heavy gates"

assert_contains_regex "sdlc_verify_story.md" 'model.*haiku|haiku.*model' \
    "Assigns haiku model to mechanical gates"

assert_contains "sdlc_verify_story.md" "Sub-agent orchestration" \
    "Documents sub-agent orchestration strategy"

assert_contains "sdlc_verify_story.md" "Failure handling" \
    "Includes failure handling for sub-agents"

section "sdlc_verify_story.md — Post-Verification"

assert_contains "sdlc_verify_story.md" "git mv" \
    "Uses git mv for file operations"

assert_contains_regex "sdlc_verify_story.md" 'opsx:sync|openspec.*sync' \
    "Syncs delta specs after verification"

assert_contains_regex "sdlc_verify_story.md" 'opsx:archive|openspec.*archive' \
    "Archives change after verification"

assert_contains "sdlc_verify_story.md" "Suggest a commit message" \
    "Ends with commit message suggestion"

assert_contains "sdlc_verify_story.md" "guidelines-reports.md" \
    "References report guidelines"

# ─────────────────────────────────────────────────────────────────────
# sdlc_doctor.md
# ─────────────────────────────────────────────────────────────────────
section "sdlc_doctor.md — Read-Only Guardrail"

assert_contains "sdlc_doctor.md" "must not modify any files" \
    "Sub-agents must not modify files"

assert_contains "sdlc_doctor.md" "Do NOT modify any files" \
    "Sub-agent prompt enforces read-only"

section "sdlc_doctor.md — Sub-agent Orchestration"

assert_contains_regex "sdlc_doctor.md" 'model.*haiku|haiku.*model' \
    "Assigns haiku model to check sub-agents"

assert_contains "sdlc_doctor.md" "parallel" \
    "Spawns check agents in parallel"

section "sdlc_doctor.md — Check Categories"

assert_contains "sdlc_doctor.md" "OpenSpec" \
    "Checks OpenSpec configuration"

assert_contains "sdlc_doctor.md" "Security Review" \
    "Checks security review plugin"

assert_contains "sdlc_doctor.md" "SDLC Commands" \
    "Checks SDLC command files"

assert_contains "sdlc_doctor.md" "openspec/config.yaml" \
    "Checks openspec/config.yaml exists"

assert_contains "sdlc_doctor.md" "security-guidance" \
    "Checks security-guidance plugin"

section "sdlc_doctor.md — Output Format"

assert_contains_regex "sdlc_doctor.md" 'Summary.*checks passed|checks passed.*failed' \
    "Outputs summary with pass/fail count"

# ─────────────────────────────────────────────────────────────────────
# sdlc_project_doctor.md
# ─────────────────────────────────────────────────────────────────────
section "sdlc_project_doctor.md — Read-Only Guardrail"

assert_contains "sdlc_project_doctor.md" "must not modify any files" \
    "Sub-agents must not modify files"

assert_contains "sdlc_project_doctor.md" "Do NOT modify any files" \
    "Sub-agent prompt enforces read-only"

section "sdlc_project_doctor.md — Sub-agent Orchestration"

assert_contains_regex "sdlc_project_doctor.md" 'model.*haiku|haiku.*model' \
    "Assigns haiku model to check sub-agents"

assert_contains "sdlc_project_doctor.md" "parallel" \
    "Spawns check agents in parallel"

section "sdlc_project_doctor.md — Check Categories"

assert_contains "sdlc_project_doctor.md" "Detekt" \
    "Checks Detekt static analysis"

assert_contains "sdlc_project_doctor.md" "Kover" \
    "Checks Kover code coverage"

assert_contains "sdlc_project_doctor.md" "Unit Tests" \
    "Checks unit test dependencies"

assert_contains "sdlc_project_doctor.md" "Fastlane" \
    "Checks Fastlane configuration"

assert_contains "sdlc_project_doctor.md" "CI/CD" \
    "Checks CI/CD pipeline"

assert_contains "sdlc_project_doctor.md" "Gradle" \
    "Checks Gradle wrapper"

assert_contains "sdlc_project_doctor.md" "minBound" \
    "Verifies Kover minimum coverage bound"

section "sdlc_project_doctor.md — Output Format"

assert_contains_regex "sdlc_project_doctor.md" 'Summary.*checks passed|checks passed.*failed' \
    "Outputs summary with pass/fail count"

# ─────────────────────────────────────────────────────────────────────
# sdlc_exp_four_in_one.md
# ─────────────────────────────────────────────────────────────────────
section "sdlc_exp_four_in_one.md — Model Assignment"

assert_contains "sdlc_exp_four_in_one.md" 'model: "opus"' \
    "Phases 1 & 2 use opus model"

assert_contains "sdlc_exp_four_in_one.md" 'model: "sonnet"' \
    "Phases 3 & 4 use sonnet model"

section "sdlc_exp_four_in_one.md — Phase References"

assert_contains "sdlc_exp_four_in_one.md" "docs/sdlc/commands/sdlc_open_story.md" \
    "Phase 1 references sdlc_open_story.md command file"

assert_contains "sdlc_exp_four_in_one.md" "docs/sdlc/commands/sdlc_propose_change.md" \
    "Phase 2 references sdlc_propose_change.md command file"

assert_contains "sdlc_exp_four_in_one.md" "docs/sdlc/commands/sdlc_implement_change.md" \
    "Phase 3 references sdlc_implement_change.md command file"

assert_contains "sdlc_exp_four_in_one.md" "docs/sdlc/commands/sdlc_verify_story.md" \
    "Phase 4 references sdlc_verify_story.md command file"

section "sdlc_exp_four_in_one.md — Commit Policy"

assert_contains "sdlc_exp_four_in_one.md" "git add -A" \
    "Commits use git add -A"

assert_contains "sdlc_exp_four_in_one.md" "git commit" \
    "Commits after each phase"

assert_contains "sdlc_exp_four_in_one.md" "Co-Authored-By" \
    "Commit messages include Co-Authored-By"

assert_contains "sdlc_exp_four_in_one.md" "{REF}" \
    "Commit messages use {REF} prefix from branch name"

section "sdlc_exp_four_in_one.md — Blocker Handling"

assert_contains "sdlc_exp_four_in_one.md" "blocker" \
    "Handles blocker reports from sub-agents"

assert_contains "sdlc_exp_four_in_one.md" "STOP" \
    "Stops on blocker from any phase"

section "sdlc_exp_four_in_one.md — Verification After Phases"

assert_contains_regex "sdlc_exp_four_in_one.md" '\-WIP' \
    "Verifies story file renamed to -WIP after Phase 1"

assert_contains_regex "sdlc_exp_four_in_one.md" '\-DONE' \
    "Verifies story file renamed to -DONE after Phase 4"

assert_contains_regex "sdlc_exp_four_in_one.md" 'all tasks are.*\[x\]|\[x\]' \
    "Verifies all tasks completed after Phase 3"

assert_contains "sdlc_exp_four_in_one.md" "openspec/changes/" \
    "Verifies change directory exists after Phase 2"

section "sdlc_exp_four_in_one.md — Final Summary"

assert_contains "sdlc_exp_four_in_one.md" "Vibe Complete" \
    "Outputs final summary with Vibe Complete header"

assert_contains "sdlc_exp_four_in_one.md" "Total commits: 4" \
    "Summary shows 4 total commits"

assert_contains_regex "sdlc_exp_four_in_one.md" 'PR.*merge|merge.*main' \
    "Summary indicates ready for PR/merge"

# ─────────────────────────────────────────────────────────────────────
# sdlc_exp_vibe_a_story.md
# ─────────────────────────────────────────────────────────────────────
section "sdlc_exp_vibe_a_story.md — Git Safety Guardrails"

assert_contains "sdlc_exp_vibe_a_story.md" "DO NOT MAKE ASSUMPTIONS" \
    "Contains DO NOT MAKE ASSUMPTIONS guardrail"

assert_contains "sdlc_exp_vibe_a_story.md" "DO NOT DELETE DATA" \
    "Contains DO NOT DELETE DATA guardrail"

assert_contains "sdlc_exp_vibe_a_story.md" "git status" \
    "Checks git status for uncommitted changes"

assert_contains "sdlc_exp_vibe_a_story.md" "git branch --show-current" \
    "Verifies current branch with git command"

assert_contains "sdlc_exp_vibe_a_story.md" "git mv" \
    "Uses git mv for file operations"

section "sdlc_exp_vibe_a_story.md — Sub-agent Orchestration"

assert_contains "sdlc_exp_vibe_a_story.md" 'model: "haiku"' \
    "Assigns haiku model to sub-agents"

assert_contains "sdlc_exp_vibe_a_story.md" 'model: "opus"' \
    "Assigns opus model to combined analysis sub-agent"

assert_contains_regex "sdlc_exp_vibe_a_story.md" 'model.*sonnet|sonnet.*model' \
    "Assigns sonnet model to implementation sub-agents"

assert_contains "sdlc_exp_vibe_a_story.md" "Failure handling" \
    "Includes failure handling for sub-agents"

section "sdlc_exp_vibe_a_story.md — Two-Phase Structure"

assert_contains "sdlc_exp_vibe_a_story.md" "Phase 1" \
    "Defines Phase 1 (Think)"

assert_contains "sdlc_exp_vibe_a_story.md" "Phase 2" \
    "Defines Phase 2 (Build & Check)"

section "sdlc_exp_vibe_a_story.md — Task List and Build"

assert_contains_regex "sdlc_exp_vibe_a_story.md" 'vibe/.*tasks\.md' \
    "Task list stored in vibe/ directory"

assert_contains_regex "sdlc_exp_vibe_a_story.md" '\- \[ \].*\- \[x\]|\[x\]' \
    "Tracks task completion via checkboxes"

section "sdlc_exp_vibe_a_story.md — Device and Security"

assert_contains "sdlc_exp_vibe_a_story.md" "adb devices" \
    "Checks device connectivity with adb devices"

assert_contains_regex "sdlc_exp_vibe_a_story.md" '[Dd]evice gate' \
    "Defines device gate protocol"

assert_contains "sdlc_exp_vibe_a_story.md" "BLOCK" \
    "Blocks on missing device"

assert_contains "sdlc_exp_vibe_a_story.md" "security-review" \
    "Runs security review"

assert_contains "sdlc_exp_vibe_a_story.md" "connectedDebugAndroidTest" \
    "Runs instrumented UI tests"

section "sdlc_exp_vibe_a_story.md — Omissions"

assert_not_contains "sdlc_exp_vibe_a_story.md" "Definition of Done verification" \
    "Does NOT include Definition of Done verification step (intentionally omitted)"

assert_not_contains "sdlc_exp_vibe_a_story.md" "RESOLVE NOW" \
    "Does NOT include TODO resolution loop (intentionally omitted)"

section "sdlc_exp_vibe_a_story.md — Commit Policy"

assert_contains "sdlc_exp_vibe_a_story.md" "git add -A" \
    "Commits use git add -A"

assert_contains "sdlc_exp_vibe_a_story.md" "git commit" \
    "Commits after each phase"

assert_contains "sdlc_exp_vibe_a_story.md" "Co-Authored-By" \
    "Commit messages include Co-Authored-By"

assert_contains "sdlc_exp_vibe_a_story.md" "{REF}" \
    "Commit messages use {REF} prefix from branch name"

section "sdlc_exp_vibe_a_story.md — Lifecycle"

assert_contains_regex "sdlc_exp_vibe_a_story.md" '\-WIP' \
    "Manages -WIP story lifecycle"

assert_contains_regex "sdlc_exp_vibe_a_story.md" '\-DONE' \
    "Manages -DONE story lifecycle"

assert_contains "sdlc_exp_vibe_a_story.md" "guidelines-userstories.md" \
    "References user story guidelines"

section "sdlc_exp_vibe_a_story.md — Final Summary"

assert_contains "sdlc_exp_vibe_a_story.md" "Vibe Complete" \
    "Outputs final summary with Vibe Complete header"

assert_contains "sdlc_exp_vibe_a_story.md" "Total commits: 2" \
    "Summary shows 2 total commits"

assert_contains_regex "sdlc_exp_vibe_a_story.md" 'PR.*merge|merge.*main' \
    "Summary indicates ready for PR/merge"

# ─────────────────────────────────────────────────────────────────────
# Cross-Command Consistency Checks
# ─────────────────────────────────────────────────────────────────────
section "Cross-Command — Commit Message Suggestion"

for cmd in sdlc_open_story.md sdlc_propose_change.md sdlc_implement_change.md sdlc_verify_story.md; do
    assert_contains "$cmd" "Suggest a commit message" \
        "$cmd ends with commit message suggestion"
done

section "Cross-Command — Sub-agent Failure Handling"

for cmd in sdlc_open_story.md sdlc_propose_change.md sdlc_implement_change.md sdlc_verify_story.md; do
    assert_contains_regex "$cmd" "Failure handling|Retry the section|still failing" \
        "$cmd includes sub-agent failure handling"
done

section "Cross-Command — Report Generation"

for cmd in sdlc_open_story.md sdlc_propose_change.md sdlc_implement_change.md sdlc_verify_story.md; do
    assert_contains "$cmd" "guidelines-reports.md" \
        "$cmd references report guidelines"
done

section "Cross-Command — Sub-agent Orchestration Declaration"

for cmd in sdlc_open_story.md sdlc_propose_change.md sdlc_implement_change.md \
           sdlc_verify_story.md sdlc_doctor.md sdlc_project_doctor.md \
           sdlc_exp_four_in_one.md sdlc_exp_vibe_a_story.md; do
    assert_contains_regex "$cmd" '[Ss]ub-agent orchestration|sub-agent' \
        "$cmd uses sub-agent orchestration"
done

section "Cross-Command — Device Connectivity"

for cmd in sdlc_implement_change.md sdlc_verify_story.md \
           sdlc_exp_vibe_a_story.md; do
    assert_contains "$cmd" "adb devices" \
        "$cmd checks device connectivity"
done

# ─────────────────────────────────────────────────────────────────────
# Summary
# ─────────────────────────────────────────────────────────────────────
echo ""
echo "─────────────────────────────────────────────"
if [[ $FAIL -eq 0 ]]; then
    echo -e "${GREEN}${BOLD}All $TOTAL assertions passed.${RESET}"
else
    echo -e "${RED}${BOLD}$FAIL/$TOTAL assertions failed.${RESET} ($PASS passed)"
fi
echo "─────────────────────────────────────────────"

exit $FAIL
