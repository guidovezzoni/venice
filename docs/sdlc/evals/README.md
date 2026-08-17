# SDLC Command Evals

Testing infrastructure for the SDLC command files that drive the AI coding agent lifecycle.

---

## Quick Intro

There are three user-facing scripts. Each serves a different purpose and has a different cost profile.

### `check_guardrails.sh` — Static assertions (free, instant)

Parses the command markdown files and asserts that required guardrail phrases, orchestration patterns, and cross-command consistency rules are present. No API calls, no runtime — just `grep`.

```bash
./docs/sdlc/evals/check_guardrails.sh
```

**When to use:** Always, before committing any change to command files. It runs in under 1 second.

---

### `run_evals.sh` — Execution evals (uses rate-limit budget)

Runs SDLC commands through Claude Code (or another tool) against controlled fixtures and asserts on actual behaviour: output content, file side effects, git operations.

```bash
# Run all evals
./docs/sdlc/evals/run_evals.sh

# Run only Phase 2 (doctor commands — lightweight)
./docs/sdlc/evals/run_evals.sh phase2

# Run only Phase 3 (story commands — heavier)
./docs/sdlc/evals/run_evals.sh phase3

# Run a specific command group
./docs/sdlc/evals/run_evals.sh doctor
./docs/sdlc/evals/run_evals.sh open_story
./docs/sdlc/evals/run_evals.sh verify_story
./docs/sdlc/evals/run_evals.sh propose_change

# Run a single scenario
./docs/sdlc/evals/run_evals.sh doctor_all_pass

# Use a different AI coding tool
./docs/sdlc/evals/run_evals.sh --tool opencode

# Save aggregate results to a specific path (for comparison)
./docs/sdlc/evals/run_evals.sh --output results/baseline.json
```

**When to use:** Run evals for the specific command you modified:

| Command modified | What to run | Rate-limit cost |
|---|---|---|
| `sdlc_doctor` | `./run_evals.sh doctor` | Low (Haiku sub-agents only) |
| `sdlc_project_doctor` | `./run_evals.sh project_doctor` | Low (Haiku sub-agents only) |
| `sdlc_open_story` | `./run_evals.sh open_story` | Moderate (Opus sub-agent) |
| `sdlc_propose_change` | `./run_evals.sh propose_change` | Moderate (Sonnet sub-agents) |
| `sdlc_verify_story` | `./run_evals.sh verify_story` | Moderate (multiple sub-agents) |
| `sdlc_exp_four_in_one` | `./run_evals.sh exp_four_in_one` | Low (git safety only) |
| `sdlc_exp_vibe_a_story` | `./run_evals.sh exp_vibe_a_story` | Low–Moderate (git safety) |
| `sdlc_implement_change` | No execution evals yet (work in progress) | — |

Run all with no filter if you touched shared infrastructure (e.g. `eval_helpers.sh`, guidelines files referenced by multiple commands).

---

### `compare_results.sh` — Regression comparison (no API calls)

Diffs two aggregate JSON result files and reports regressions (PASS to FAIL), improvements (FAIL to PASS), and new/removed scenarios.

```bash
# Compare before/after a command change
./docs/sdlc/evals/compare_results.sh results/baseline.json results/candidate.json

# Compare across tools (labels columns by tool name)
./docs/sdlc/evals/compare_results.sh --tool-diff results/claude.json results/opencode.json
```

**Exit codes:** `0` = no regressions, `1` = regressions detected, `2` = usage error.

**When to use:** After modifying commands, to verify no scenarios regressed. Also for cross-tool evaluation when testing a new AI coding tool.

---

## Recommended Workflow

1. **Always run `check_guardrails.sh`** before committing changes to command files — it's free, instant, and checks all 6 commands.
2. **Run execution evals for the command you modified** — see the table above for the correct filter and expected cost.
3. **Compare before/after** when modifying commands: save results with `--output` before changes, run again after, then use `compare_results.sh` to identify regressions.
4. **Cross-tool comparison** when evaluating a new tool: run the same scenarios with `--tool claude` and `--tool opencode`, then compare with `--tool-diff`.

Note: `sdlc_implement_change` does not have execution evals yet — it is only covered by static guardrails (`check_guardrails.sh`).

---

## What We're Testing

We are testing the **commands** (the instruction files), not the LLM itself. The commands are the program; the AI coding agent (Claude Code, OpenCode, etc.) is the runtime. An eval runs a command through the runtime against a controlled scenario and checks that the correct behaviour happens.

This means:
- Evals run through **Claude Code itself** (via `claude -p`), not by calling the API directly
- When switching to OpenCode, the same evals run through OpenCode's CLI
- We observe **actual tool calls and side effects** (files created, git operations, outputs), not hypothetical "what would you do?" answers

---

## Rate Limits

All execution evals run under the Claude Code subscription — no extra API costs. The only constraint is subscription rate limits.

- **Phase 1 (static checks):** No rate-limit concern — no API calls at all.
- **Phase 2 (doctor evals):** ~8 scenarios, each spawning ~3 Haiku sub-agents. Lightweight; unlikely to hit limits even if run repeatedly.
- **Phase 3 (higher commands):** Heavier commands (`sdlc_open_story` uses Opus, `sdlc_verify_story` spawns many sub-agents). Run selectively — only when touching the specific command under test.
- **General rule:** Run Phase 1 freely. Run Phase 2 when touching any command. Run Phase 3 scenarios individually for the command you changed.

---

## Phase 1: Static Guardrail Assertions

A shell script that parses the command markdown files and asserts that required patterns are present. This catches accidental removal of guardrails during editing — the most common regression.

**What it tests:**
- Required guardrail phrases exist in each command (e.g., `sdlc_open_story.md` contains "DO NOT MAKE ASSUMPTIONS", "DO NOT DELETE DATA", "git mv")
- Orchestration patterns are intact (e.g., `sdlc_doctor.md` references "haiku" for sub-agents, `sdlc_verify_story.md` has all gate steps)
- Cross-command consistency (e.g., all commands that use sub-agents include failure handling, all commands end with "suggest a commit message")

**Implementation:**
- Single file: `check_guardrails.sh`
- Assertions defined as `grep -q` checks with descriptive messages
- Same pass/fail output format as `sdlc_doctor`
- Runs in <1 second, no API calls, no dependencies

---

## Phase 2: Execution Evals — Doctor Commands

Runs commands through Claude Code against a **controlled fixture** and asserts on actual behaviour. Starts with the cheapest commands (`sdlc_doctor`, `sdlc_project_doctor`) since they only spawn Haiku sub-agents doing read-only checks.

**How it works:**
1. A setup script creates a temporary fixture directory with known state (specific files present/absent, specific config values)
2. The eval runner invokes Claude Code: `claude -p "/sdlc_doctor"` inside the fixture
3. A check script examines Claude Code's output for expected behaviour (correct pass/fail for each check, correct output format, no file modifications)

**Why start with the doctor commands:**
- Lightest rate-limit footprint — only Haiku sub-agents, no Opus/Sonnet orchestration
- Most deterministic — read-only checks with binary pass/fail outcomes
- Fast — no builds, no git operations, no device connectivity
- Easy to create fixtures — just create/omit specific files

### Scenarios for `sdlc_doctor`

| Scenario | Fixture state | Expected behaviour |
|----------|--------------|-------------------|
| `doctor_all_pass` | All files present, all config correct | All pass, summary says "All checks passed" |
| `doctor_missing_openspec_config` | Remove `openspec/config.yaml` | Fail for that check, others pass |
| `doctor_missing_opsx_commands` | Remove 2 of the 6 opsx command files | Fail for those 2, others pass |
| `doctor_security_disabled` | Set plugin to `false` in settings | Fail for security check |
| `doctor_everything_broken` | Remove all checked files | All fail, correct failure count |

### Scenarios for `sdlc_project_doctor`

| Scenario | Fixture state | Expected behaviour |
|----------|--------------|-------------------|
| `project_doctor_all_pass` | Full project config present | All pass |
| `project_doctor_missing_detekt` | Remove detekt config | Fail for detekt checks only |
| `project_doctor_low_coverage` | Set minBound to 50 instead of 95 | Fail for kover threshold check |

### What we assert on
- Output contains the expected pass/fail lines for each check
- Summary count is correct
- No files were modified in the fixture (read-only guardrail)
- Output format matches the specified structure

---

## Phase 3: Higher-Value Command Evals

Evals for commands with more complex behaviour and critical guardrails. These use more rate-limit budget per run but test the most important behaviours.

**Priority order (by value-to-rate-limit-cost ratio):**

1. **`sdlc_open_story`** — git safety guardrails
2. **`sdlc_propose_change`** — "no assumptions" guardrail
3. **`sdlc_verify_story`** — blocking gate protocol
4. **`sdlc_exp_four_in_one`** — git safety
5. **`sdlc_exp_vibe_a_story`** — git safety (shared patterns with open_story)
7. **`sdlc_implement_change`** — BDD discipline and model assignment (deferred)

### Implemented scenarios

| Scenario | Command | Fixture state | Key assertions |
|----------|---------|--------------|----------------|
| `open_story_dirty_state` | `sdlc_open_story` | Git repo on main with uncommitted changes | Output warns about dirty state, mentions specific dirty file |
| `open_story_not_on_main` | `sdlc_open_story` | Git repo on `feature/existing-work` branch | Output detects non-main branch, asks about proceeding/switching |
| `verify_story_precondition_fail` | `sdlc_verify_story` | Story in "New" state (not WIP) | Stops before gates, doesn't reach later verification steps |
| `verify_story_blocks_on_gate_failure` | `sdlc_verify_story` | WIP story, no openspec change artifacts | Blocks at first gate, doesn't reach downstream gates |
| `propose_change_surfaces_questions` | `sdlc_propose_change` | WIP story with deliberately ambiguous requirements | Surfaces questions, does not make assumptions |
| `exp_four_in_one_dirty_state` | `sdlc_exp_four_in_one` | Git repo on main with dirty working tree | Detects uncommitted changes, mentions specific dirty file, warns user |
| `exp_vibe_a_story_dirty_state` | `sdlc_exp_vibe_a_story` | Git repo on main with dirty working tree | Detects uncommitted changes, mentions specific dirty file, warns user |

### Fixture design for git-based commands

All Phase 3 fixtures use `create_story_command_base_fixture()` which creates a git repo with:
- Git user config, initial commit on `main`
- Minimal guidelines files (userstories, git, reports, android, process)
- OpenSpec config, AGENTS.md, gradlew stub
- `docs/userstories/` and `docs/reports/` directories

Scenarios extend the base with `create_sample_story_new()`, `create_sample_story_wip()`, or `create_sample_story_ambiguous()` and then apply scenario-specific tweaks (dirty files, branch switches, etc.).

All Phase 3 scenarios use `run_eval_with_args()` which supports `$ARGUMENTS` substitution and `--max-turns` to control cost.

### Deferred

`sdlc_implement_change` evals require a full openspec change with task artifacts and are the most rate-limit-heavy. To be added when the first 3 commands' evals are stable.

---

## Phase 4: Regression Comparison and Cross-Tool Testing

### Regression comparison

Each eval run outputs a JSON results file per scenario (scenario name, pass/fail, tool, timestamp), aggregated into a single file. The comparison script diffs two aggregate files and classifies each scenario as: regression, improvement, new, removed, or unchanged.

**Workflow:**
```bash
./docs/sdlc/evals/run_evals.sh --output results/baseline.json
# ... make changes to commands ...
./docs/sdlc/evals/run_evals.sh --output results/candidate.json
./docs/sdlc/evals/compare_results.sh results/baseline.json results/candidate.json
```

### Cross-tool testing

Same fixtures and assertions, different `--tool` flag:
- `--tool claude` (default): runs via `claude -p`
- `--tool opencode`: runs via OpenCode's equivalent CLI

```bash
./docs/sdlc/evals/run_evals.sh --tool claude --output results/claude.json
./docs/sdlc/evals/run_evals.sh --tool opencode --output results/opencode.json
./docs/sdlc/evals/compare_results.sh --tool-diff results/claude.json results/opencode.json
```

---

## File Structure

```
docs/sdlc/evals/
├── check_guardrails.sh                # Phase 1: static assertions
├── run_evals.sh                       # Phase 2 & 3: execution eval runner
├── compare_results.sh                 # Phase 4: regression comparison
├── lib/
│   └── eval_helpers.sh                # Shared fixture creators, assertion helpers
├── scenarios/
│   ├── doctor_all_pass.sh
│   ├── doctor_everything_broken.sh
│   ├── doctor_missing_openspec_config.sh
│   ├── doctor_missing_opsx_commands.sh
│   ├── doctor_security_disabled.sh
│   ├── project_doctor_all_pass.sh
│   ├── project_doctor_low_coverage.sh
│   ├── project_doctor_missing_detekt.sh
│   ├── open_story_dirty_state.sh
│   ├── open_story_not_on_main.sh
│   ├── verify_story_precondition_fail.sh
│   ├── verify_story_blocks_on_gate_failure.sh
│   ├── propose_change_surfaces_questions.sh
│   ├── exp_four_in_one_dirty_state.sh
│   └── exp_vibe_a_story_dirty_state.sh
└── results/                           # Gitignored eval results
    └── .gitkeep
```

---

## Prerequisites

- `claude` CLI installed and authenticated (or the tool specified via `--tool`)
- `openspec` CLI installed (for doctor openspec CLI check)
- `fastlane`, `bundle` installed (for project_doctor CLI checks)

---

## Verification

- **Phase 1:** Run `./docs/sdlc/evals/check_guardrails.sh` — all assertions pass. Intentionally remove a guardrail phrase, re-run, verify it catches the regression.
- **Phase 2:** Run `./docs/sdlc/evals/run_evals.sh phase2` — all doctor scenarios pass. Intentionally break a fixture (remove a file that should exist) — verify the eval catches the changed behaviour.
- **Phase 3:** Run `./docs/sdlc/evals/run_evals.sh phase3` — all Phase 3 scenarios pass. Run individual commands selectively: `./docs/sdlc/evals/run_evals.sh open_story` or a single scenario: `./docs/sdlc/evals/run_evals.sh open_story_dirty_state`.
- **Phase 4:** Run with `--output /tmp/before.json`, make a change, run again with `--output /tmp/after.json`, then `./docs/sdlc/evals/compare_results.sh /tmp/before.json /tmp/after.json` — verify regressions are correctly identified.
