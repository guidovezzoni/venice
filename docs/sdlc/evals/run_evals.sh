#!/usr/bin/env bash
# SDLC Execution Evals — Phase 2
#
# Runs doctor and project_doctor commands against controlled fixtures via
# Claude Code CLI and asserts on actual behavior.
#
# Usage:
#   ./docs/sdlc/evals/run_evals.sh                          # Run all evals
#   ./docs/sdlc/evals/run_evals.sh doctor                   # Run only doctor evals
#   ./docs/sdlc/evals/run_evals.sh project_doctor            # Run only project_doctor evals
#   ./docs/sdlc/evals/run_evals.sh doctor_all_pass           # Run a single scenario
#   EVAL_TOOL=opencode ./docs/sdlc/evals/run_evals.sh       # Run with OpenCode
#
# Prerequisites:
#   - claude CLI installed and authenticated
#   - openspec CLI installed (for doctor openspec CLI check)
#   - fastlane, bundle installed (for project_doctor CLI checks)
#
# Environment variables:
#   EVAL_TOOL    CLI tool to use (default: claude)

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

FILTER="${1:-}"

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
echo -e "${BOLD}SDLC Execution Evals (Phase 2)${RESET}"
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

exit "$FAILED_SCENARIOS"
