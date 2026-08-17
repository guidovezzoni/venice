#!/usr/bin/env bash
# SDLC Execution Evals — Phase 2 & 3
#
# Runs SDLC commands against controlled fixtures via Claude Code CLI
# and asserts on actual behavior.
#
# Usage:
#   ./docs/sdlc/evals/run_evals.sh                          # Run all evals
#   ./docs/sdlc/evals/run_evals.sh doctor                   # Run only doctor evals
#   ./docs/sdlc/evals/run_evals.sh project_doctor            # Run only project_doctor evals
#   ./docs/sdlc/evals/run_evals.sh open_story                # Run only open_story evals
#   ./docs/sdlc/evals/run_evals.sh propose_change            # Run only propose_change evals
#   ./docs/sdlc/evals/run_evals.sh verify_story              # Run only verify_story evals
#   ./docs/sdlc/evals/run_evals.sh exp_four_in_one           # Run only exp_four_in_one evals
#   ./docs/sdlc/evals/run_evals.sh exp_vibe_a_story          # Run only exp_vibe_a_story evals
#   ./docs/sdlc/evals/run_evals.sh phase2                    # Run all Phase 2 evals
#   ./docs/sdlc/evals/run_evals.sh phase3                    # Run all Phase 3 evals
#   ./docs/sdlc/evals/run_evals.sh doctor_all_pass           # Run a single scenario
#   ./docs/sdlc/evals/run_evals.sh --tool opencode           # Run with OpenCode
#   ./docs/sdlc/evals/run_evals.sh --tool opencode doctor    # Filter + tool
#   ./docs/sdlc/evals/run_evals.sh --output results/run1.json  # Custom output path
#
# Prerequisites:
#   - claude CLI installed and authenticated
#   - openspec CLI installed (for doctor openspec CLI check)
#   - fastlane, bundle installed (for project_doctor CLI checks)
#
# Environment variables:
#   EVAL_TOOL    CLI tool to use (default: claude, overridden by --tool)

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SCENARIOS_DIR="$SCRIPT_DIR/scenarios"
RESULTS_DIR="$SCRIPT_DIR/results"

export EVAL_TOOL="${EVAL_TOOL:-claude}"
export RESULTS_DIR

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
BOLD='\033[1m'
DIM='\033[2m'
RESET='\033[0m'

# ---------------------------------------------------------------------------
# CLI argument parsing
# ---------------------------------------------------------------------------

FILTER=""
CUSTOM_OUTPUT=""

while [[ $# -gt 0 ]]; do
    case "$1" in
        --tool)
            EVAL_TOOL="$2"
            export EVAL_TOOL
            shift 2
            ;;
        --output)
            CUSTOM_OUTPUT="$2"
            shift 2
            ;;
        -h|--help)
            echo "Usage: $(basename "$0") [--tool <name>] [--output <path>] [<filter>]"
            echo ""
            echo "Options:"
            echo "  --tool <name>    CLI tool to invoke (default: claude)"
            echo "  --output <path>  Write aggregate results to a custom path"
            echo ""
            echo "Filters: doctor, project_doctor, open_story, propose_change,"
            echo "         verify_story, exp_four_in_one, exp_vibe_a_story,"
            echo "         phase2, phase3, or a specific scenario name"
            exit 0
            ;;
        *)
            FILTER="$1"
            shift
            ;;
    esac
done

# ---------------------------------------------------------------------------
# Preflight checks
# ---------------------------------------------------------------------------

if ! command -v "$EVAL_TOOL" > /dev/null 2>&1; then
    echo -e "${RED}ERROR${RESET}: '$EVAL_TOOL' not found in PATH."
    echo "Install Claude Code or set EVAL_TOOL to the correct CLI."
    exit 1
fi

# Verify --dangerously-skip-permissions is accepted
if ! "$EVAL_TOOL" --help 2>&1 | grep -q "dangerously-skip-permissions"; then
    echo -e "${YELLOW}WARNING${RESET}: '$EVAL_TOOL' may not support --dangerously-skip-permissions."
    echo "Evals require non-interactive execution. Proceed with caution."
fi

mkdir -p "$RESULTS_DIR"
rm -f "$RESULTS_DIR"/*.json

# ---------------------------------------------------------------------------
# Discover and run scenarios
# ---------------------------------------------------------------------------

TOTAL_SCENARIOS=0
PASSED_SCENARIOS=0
FAILED_SCENARIOS=0
FAILED_NAMES=()

echo ""
echo -e "${BOLD}SDLC Execution Evals (Phase 2 & 3)${RESET}"
echo -e "${DIM}Tool: $EVAL_TOOL${RESET}"
echo -e "${DIM}Filter: ${FILTER:-all}${RESET}"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

for scenario_script in "$SCENARIOS_DIR"/*.sh; do
    scenario_name="$(basename "$scenario_script" .sh)"

    # Apply filter
    if [[ -n "$FILTER" ]]; then
        case "$FILTER" in
            doctor)
                [[ "$scenario_name" == doctor_* ]] || continue
                ;;
            project_doctor)
                [[ "$scenario_name" == project_doctor_* ]] || continue
                ;;
            open_story)
                [[ "$scenario_name" == open_story_* ]] || continue
                ;;
            propose_change)
                [[ "$scenario_name" == propose_change_* ]] || continue
                ;;
            verify_story)
                [[ "$scenario_name" == verify_story_* ]] || continue
                ;;
            implement_change)
                [[ "$scenario_name" == implement_change_* ]] || continue
                ;;
            exp_four_in_one)
                [[ "$scenario_name" == exp_four_in_one_* ]] || continue
                ;;
            exp_vibe_a_story)
                [[ "$scenario_name" == exp_vibe_a_story_* ]] || continue
                ;;
            phase2)
                [[ "$scenario_name" == doctor_* || "$scenario_name" == project_doctor_* ]] || continue
                ;;
            phase3)
                [[ "$scenario_name" == open_story_* || "$scenario_name" == propose_change_* || \
                   "$scenario_name" == verify_story_* || "$scenario_name" == implement_change_* || \
                   "$scenario_name" == exp_four_in_one_* || "$scenario_name" == exp_vibe_a_story_* ]] || continue
                ;;
            *)
                [[ "$scenario_name" == "$FILTER" ]] || continue
                ;;
        esac
    fi

    TOTAL_SCENARIOS=$((TOTAL_SCENARIOS + 1))
    echo ""
    echo -e "${BOLD}Scenario: $scenario_name${RESET}"

    if bash "$scenario_script"; then
        PASSED_SCENARIOS=$((PASSED_SCENARIOS + 1))
    else
        FAILED_SCENARIOS=$((FAILED_SCENARIOS + 1))
        FAILED_NAMES+=("$scenario_name")
    fi
done

# ---------------------------------------------------------------------------
# Summary
# ---------------------------------------------------------------------------

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

if [[ "$TOTAL_SCENARIOS" -eq 0 ]]; then
    echo -e "${YELLOW}No scenarios matched filter '${FILTER}'.${RESET}"
    echo "Available scenarios:"
    for s in "$SCENARIOS_DIR"/*.sh; do
        echo "  $(basename "$s" .sh)"
    done
    exit 1
fi

if [[ "$FAILED_SCENARIOS" -eq 0 ]]; then
    echo -e "${GREEN}${BOLD}All $TOTAL_SCENARIOS scenario(s) passed.${RESET}"
else
    echo -e "${RED}${BOLD}$FAILED_SCENARIOS/$TOTAL_SCENARIOS scenario(s) failed:${RESET}"
    for name in "${FAILED_NAMES[@]}"; do
        echo -e "  ${RED}•${RESET} $name"
    done
fi

echo ""
echo -e "${DIM}Results saved to: $RESULTS_DIR/${RESET}"

# Aggregate results into a single JSON file
AGGREGATE="$RESULTS_DIR/aggregate.json"
echo "[" > "$AGGREGATE"
FIRST=true
for result_file in "$RESULTS_DIR"/*.json; do
    [[ "$(basename "$result_file")" == "aggregate.json" ]] && continue
    [[ -f "$result_file" ]] || continue
    if $FIRST; then
        FIRST=false
    else
        echo "," >> "$AGGREGATE"
    fi
    cat "$result_file" >> "$AGGREGATE"
done
echo "" >> "$AGGREGATE"
echo "]" >> "$AGGREGATE"

# Copy aggregate to custom output path if specified
if [[ -n "$CUSTOM_OUTPUT" ]]; then
    mkdir -p "$(dirname "$CUSTOM_OUTPUT")"
    cp "$AGGREGATE" "$CUSTOM_OUTPUT"
    echo -e "${DIM}Custom output: $CUSTOM_OUTPUT${RESET}"
fi

exit "$FAILED_SCENARIOS"
