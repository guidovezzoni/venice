#!/usr/bin/env bash
# SDLC Eval Results Comparison — Phase 4
#
# Compares two aggregate JSON result files and highlights regressions,
# improvements, and new/removed scenarios.
#
# Usage:
#   ./docs/sdlc/evals/compare_results.sh results/baseline.json results/candidate.json
#   ./docs/sdlc/evals/compare_results.sh --tool-diff results/claude.json results/opencode.json
#
# Exit codes:
#   0 — no regressions
#   1 — regressions detected
#   2 — usage error

set -euo pipefail

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
CYAN='\033[0;36m'
BOLD='\033[1m'
DIM='\033[2m'
RESET='\033[0m'

# ---------------------------------------------------------------------------
# Usage
# ---------------------------------------------------------------------------

usage() {
    echo "Usage: $(basename "$0") [--tool-diff] <baseline.json> <candidate.json>"
    echo ""
    echo "Compares two aggregate eval result files."
    echo ""
    echo "Options:"
    echo "  --tool-diff    Label columns as tools instead of baseline/candidate"
    echo ""
    echo "Examples:"
    echo "  $(basename "$0") results/before.json results/after.json"
    echo "  $(basename "$0") --tool-diff results/claude.json results/opencode.json"
    exit 2
}

# ---------------------------------------------------------------------------
# Argument parsing
# ---------------------------------------------------------------------------

TOOL_DIFF=false
BASELINE=""
CANDIDATE=""

while [[ $# -gt 0 ]]; do
    case "$1" in
        --tool-diff)
            TOOL_DIFF=true
            shift
            ;;
        -h|--help)
            usage
            ;;
        *)
            if [[ -z "$BASELINE" ]]; then
                BASELINE="$1"
            elif [[ -z "$CANDIDATE" ]]; then
                CANDIDATE="$1"
            else
                echo "Error: unexpected argument '$1'"
                usage
            fi
            shift
            ;;
    esac
done

if [[ -z "$BASELINE" || -z "$CANDIDATE" ]]; then
    echo "Error: two result files required."
    usage
fi

if [[ ! -f "$BASELINE" ]]; then
    echo -e "${RED}ERROR${RESET}: baseline file not found: $BASELINE"
    exit 2
fi

if [[ ! -f "$CANDIDATE" ]]; then
    echo -e "${RED}ERROR${RESET}: candidate file not found: $CANDIDATE"
    exit 2
fi

# ---------------------------------------------------------------------------
# Parse JSON results into associative arrays
# ---------------------------------------------------------------------------

declare -A BASELINE_RESULTS
declare -A BASELINE_PASSED
declare -A BASELINE_FAILED
declare -A BASELINE_TOTAL
declare -A CANDIDATE_RESULTS
declare -A CANDIDATE_PASSED
declare -A CANDIDATE_FAILED
declare -A CANDIDATE_TOTAL

parse_results() {
    local file="$1"
    local prefix="$2"

    local scenarios
    scenarios=$(grep -o '"scenario": *"[^"]*"' "$file" | sed 's/"scenario": *"//;s/"//')

    local results
    results=$(grep -o '"result": *"[^"]*"' "$file" | sed 's/"result": *"//;s/"//')

    local passed_values
    passed_values=$(grep -o '"passed": *[0-9]*' "$file" | sed 's/"passed": *//')

    local failed_values
    failed_values=$(grep -o '"failed": *[0-9]*' "$file" | sed 's/"failed": *//')

    local total_values
    total_values=$(grep -o '"total": *[0-9]*' "$file" | sed 's/"total": *//')

    local -a scenario_arr
    mapfile -t scenario_arr <<< "$scenarios"

    local -a result_arr
    mapfile -t result_arr <<< "$results"

    local -a passed_arr
    mapfile -t passed_arr <<< "$passed_values"

    local -a failed_arr
    mapfile -t failed_arr <<< "$failed_values"

    local -a total_arr
    mapfile -t total_arr <<< "$total_values"

    for i in "${!scenario_arr[@]}"; do
        local scenario="${scenario_arr[$i]}"
        [[ -z "$scenario" ]] && continue

        if [[ "$prefix" == "BASELINE" ]]; then
            BASELINE_RESULTS["$scenario"]="${result_arr[$i]}"
            BASELINE_PASSED["$scenario"]="${passed_arr[$i]}"
            BASELINE_FAILED["$scenario"]="${failed_arr[$i]}"
            BASELINE_TOTAL["$scenario"]="${total_arr[$i]}"
        else
            CANDIDATE_RESULTS["$scenario"]="${result_arr[$i]}"
            CANDIDATE_PASSED["$scenario"]="${passed_arr[$i]}"
            CANDIDATE_FAILED["$scenario"]="${failed_arr[$i]}"
            CANDIDATE_TOTAL["$scenario"]="${total_arr[$i]}"
        fi
    done
}

parse_results "$BASELINE" "BASELINE"
parse_results "$CANDIDATE" "CANDIDATE"

# ---------------------------------------------------------------------------
# Compute all unique scenarios
# ---------------------------------------------------------------------------

declare -A ALL_SCENARIOS
for scenario in "${!BASELINE_RESULTS[@]}"; do
    ALL_SCENARIOS["$scenario"]=1
done
for scenario in "${!CANDIDATE_RESULTS[@]}"; do
    ALL_SCENARIOS["$scenario"]=1
done

SORTED_SCENARIOS=$(printf '%s\n' "${!ALL_SCENARIOS[@]}" | sort)

# ---------------------------------------------------------------------------
# Classify changes
# ---------------------------------------------------------------------------

REGRESSIONS=()
IMPROVEMENTS=()
UNCHANGED=()
NEW_SCENARIOS=()
REMOVED_SCENARIOS=()

while IFS= read -r scenario; do
    [[ -z "$scenario" ]] && continue

    local_baseline="${BASELINE_RESULTS[$scenario]:-}"
    local_candidate="${CANDIDATE_RESULTS[$scenario]:-}"

    if [[ -z "$local_baseline" && -n "$local_candidate" ]]; then
        NEW_SCENARIOS+=("$scenario")
    elif [[ -n "$local_baseline" && -z "$local_candidate" ]]; then
        REMOVED_SCENARIOS+=("$scenario")
    elif [[ "$local_baseline" == "PASS" && "$local_candidate" == "FAIL" ]]; then
        REGRESSIONS+=("$scenario")
    elif [[ "$local_baseline" == "FAIL" && "$local_candidate" == "PASS" ]]; then
        IMPROVEMENTS+=("$scenario")
    else
        UNCHANGED+=("$scenario")
    fi
done <<< "$SORTED_SCENARIOS"

# ---------------------------------------------------------------------------
# Labels
# ---------------------------------------------------------------------------

if $TOOL_DIFF; then
    LABEL_BASELINE="$(basename "$BASELINE" .json)"
    LABEL_CANDIDATE="$(basename "$CANDIDATE" .json)"
else
    LABEL_BASELINE="baseline"
    LABEL_CANDIDATE="candidate"
fi

# ---------------------------------------------------------------------------
# Output report
# ---------------------------------------------------------------------------

echo ""
echo -e "${BOLD}SDLC Eval Comparison Report${RESET}"
echo -e "${DIM}$LABEL_BASELINE: $BASELINE${RESET}"
echo -e "${DIM}$LABEL_CANDIDATE: $CANDIDATE${RESET}"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

# Summary counts
TOTAL_SCENARIOS=${#ALL_SCENARIOS[@]}
echo ""
echo -e "${BOLD}Summary:${RESET} $TOTAL_SCENARIOS scenario(s) compared"
echo -e "  Regressions:  ${#REGRESSIONS[@]}"
echo -e "  Improvements: ${#IMPROVEMENTS[@]}"
echo -e "  Unchanged:    ${#UNCHANGED[@]}"
echo -e "  New:          ${#NEW_SCENARIOS[@]}"
echo -e "  Removed:      ${#REMOVED_SCENARIOS[@]}"

# Regressions (most important)
if [[ ${#REGRESSIONS[@]} -gt 0 ]]; then
    echo ""
    echo -e "${RED}${BOLD}REGRESSIONS (PASS → FAIL):${RESET}"
    for scenario in "${REGRESSIONS[@]}"; do
        local_b_passed="${BASELINE_PASSED[$scenario]:-0}"
        local_b_total="${BASELINE_TOTAL[$scenario]:-0}"
        local_c_passed="${CANDIDATE_PASSED[$scenario]:-0}"
        local_c_total="${CANDIDATE_TOTAL[$scenario]:-0}"
        local_c_failed="${CANDIDATE_FAILED[$scenario]:-0}"
        echo -e "  ${RED}▼${RESET} $scenario"
        echo -e "    ${DIM}$LABEL_BASELINE: PASS ($local_b_passed/$local_b_total assertions)${RESET}"
        echo -e "    ${RED}$LABEL_CANDIDATE: FAIL ($local_c_passed/$local_c_total assertions, $local_c_failed failed)${RESET}"
    done
fi

# Improvements
if [[ ${#IMPROVEMENTS[@]} -gt 0 ]]; then
    echo ""
    echo -e "${GREEN}${BOLD}IMPROVEMENTS (FAIL → PASS):${RESET}"
    for scenario in "${IMPROVEMENTS[@]}"; do
        local_b_passed="${BASELINE_PASSED[$scenario]:-0}"
        local_b_total="${BASELINE_TOTAL[$scenario]:-0}"
        local_b_failed="${BASELINE_FAILED[$scenario]:-0}"
        local_c_passed="${CANDIDATE_PASSED[$scenario]:-0}"
        local_c_total="${CANDIDATE_TOTAL[$scenario]:-0}"
        echo -e "  ${GREEN}▲${RESET} $scenario"
        echo -e "    ${DIM}$LABEL_BASELINE: FAIL ($local_b_passed/$local_b_total assertions, $local_b_failed failed)${RESET}"
        echo -e "    ${GREEN}$LABEL_CANDIDATE: PASS ($local_c_passed/$local_c_total assertions)${RESET}"
    done
fi

# New scenarios
if [[ ${#NEW_SCENARIOS[@]} -gt 0 ]]; then
    echo ""
    echo -e "${CYAN}${BOLD}NEW SCENARIOS (only in $LABEL_CANDIDATE):${RESET}"
    for scenario in "${NEW_SCENARIOS[@]}"; do
        local_c_result="${CANDIDATE_RESULTS[$scenario]}"
        local_c_passed="${CANDIDATE_PASSED[$scenario]:-0}"
        local_c_total="${CANDIDATE_TOTAL[$scenario]:-0}"
        if [[ "$local_c_result" == "PASS" ]]; then
            echo -e "  ${CYAN}+${RESET} $scenario — ${GREEN}PASS${RESET} ($local_c_passed/$local_c_total)"
        else
            echo -e "  ${CYAN}+${RESET} $scenario — ${RED}FAIL${RESET} ($local_c_passed/$local_c_total)"
        fi
    done
fi

# Removed scenarios
if [[ ${#REMOVED_SCENARIOS[@]} -gt 0 ]]; then
    echo ""
    echo -e "${YELLOW}${BOLD}REMOVED SCENARIOS (only in $LABEL_BASELINE):${RESET}"
    for scenario in "${REMOVED_SCENARIOS[@]}"; do
        local_b_result="${BASELINE_RESULTS[$scenario]}"
        echo -e "  ${YELLOW}-${RESET} $scenario — was ${DIM}$local_b_result${RESET}"
    done
fi

# Unchanged (brief)
if [[ ${#UNCHANGED[@]} -gt 0 ]]; then
    echo ""
    echo -e "${DIM}${BOLD}UNCHANGED:${RESET}"
    for scenario in "${UNCHANGED[@]}"; do
        local_result="${CANDIDATE_RESULTS[$scenario]}"
        local_c_passed="${CANDIDATE_PASSED[$scenario]:-0}"
        local_c_total="${CANDIDATE_TOTAL[$scenario]:-0}"
        if [[ "$local_result" == "PASS" ]]; then
            echo -e "  ${DIM}  $scenario — PASS ($local_c_passed/$local_c_total)${RESET}"
        else
            echo -e "  ${DIM}  $scenario — FAIL ($local_c_passed/$local_c_total)${RESET}"
        fi
    done
fi

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

# Exit with regression count (capped at 1 for CI)
if [[ ${#REGRESSIONS[@]} -gt 0 ]]; then
    echo -e "${RED}${BOLD}${#REGRESSIONS[@]} regression(s) detected.${RESET}"
    exit 1
else
    echo -e "${GREEN}${BOLD}No regressions detected.${RESET}"
    exit 0
fi
