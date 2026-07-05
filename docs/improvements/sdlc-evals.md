# SDLC Command Evals — Venice ("Heading to the Alps!")

## Context

The SDLC framework consists of 6 markdown command files that instruct AI coding agents (currently Claude Code) to drive the full lifecycle of user stories. These commands contain critical guardrails (e.g., "never delete data", "use git mv not mv", "blocking gate protocol"), orchestration patterns (parallel fan-out, sequential gates, model tier assignment), and decision logic.

Currently there is **zero testing infrastructure** for these commands. Changes are made and validated manually. This creates risk around:
- **Regressions**: editing a command might accidentally remove a guardrail or break orchestration logic
- **Model compatibility**: when swapping the underlying model or AI coding tool (e.g., Claude Code → OpenCode), behavior may differ
- **Benchmarking**: no way to measure whether a command change improves or degrades quality
- **Guardrail validation**: no systematic way to verify that safety constraints are honored

---

## What We're Testing

We are testing the **commands** (the instruction files), not the LLM itself. The commands are the program; the AI coding agent (Claude Code, OpenCode, etc.) is the runtime. An eval runs a command through the runtime against a controlled scenario and checks that the correct behavior happens.

This means:
- Evals run through **Claude Code itself** (via `claude -p`), not by calling the API directly
- When switching to OpenCode, the same evals run through OpenCode's CLI
- We observe **actual tool calls and side effects** (files created, git operations, outputs), not hypothetical "what would you do?" answers

---

## Rate Limits

All execution evals run under the Claude Code subscription — no extra API costs. The only constraint is subscription rate limits. Practical guidance:

- **Phase 1 (static checks):** No rate-limit concern — no API calls at all.
- **Phase 2 (doctor evals):** ~8 scenarios, each spawning ~3 Haiku sub-agents. Lightweight; unlikely to hit limits even if run repeatedly.
- **Phase 3 (higher commands):** Heavier commands (`sdlc_open_story` uses Opus, `sdlc_verify_story` spawns many sub-agents). Run selectively — only when touching the specific command under test. Avoid running the full suite in a tight loop.
- **General rule:** Run Phase 1 freely (it's free). Run Phase 2 when touching any command. Run Phase 3 scenarios individually for the command you changed.

---

## Incremental Implementation Plan

### Phase 1: Static Guardrail Assertions — zero cost, instant

A shell script that parses the command markdown files and asserts that required patterns are present. This catches accidental removal of guardrails during editing — the most common regression.

**What it tests:**
- Required guardrail phrases exist in each command (e.g., `sdlc_open_story.md` contains "DO NOT MAKE ASSUMPTIONS", "DO NOT DELETE DATA", "git mv")
- Orchestration patterns are intact (e.g., `sdlc_doctor.md` references "haiku" for sub-agents, `sdlc_verify_story.md` has all gate steps)
- Cross-command consistency (e.g., all commands that use sub-agents include failure handling, all commands end with "suggest a commit message")

**Implementation:**
- Single file: `docs/sdlc/evals/check_guardrails.sh`
- Assertions defined as `grep -q` checks with descriptive messages
- Same ✅/❌ output format as `sdlc_doctor`
- ~50-80 assertions across all 6 commands
- Runs in <1 second, no API calls, no dependencies

**Example assertions:**
```bash
# sdlc_open_story.md
assert_contains "sdlc_open_story.md" "DO NOT MAKE ASSUMPTIONS"
assert_contains "sdlc_open_story.md" "DO NOT DELETE DATA"
assert_contains "sdlc_open_story.md" "git mv"
assert_contains "sdlc_open_story.md" 'model.*haiku'
assert_contains "sdlc_open_story.md" 'model.*opus'

# sdlc_verify_story.md
assert_contains "sdlc_verify_story.md" "BLOCKING GATE"
assert_contains "sdlc_verify_story.md" "adb devices"
assert_contains "sdlc_verify_story.md" "NOT_FEASIBLE"

# All commands that produce output
for cmd in sdlc_open_story.md sdlc_propose_change.md sdlc_implement_change.md sdlc_verify_story.md; do
  assert_contains "$cmd" "suggest a commit message"
done
```

---

### Phase 2: Execution Evals via Claude Code CLI — low rate-limit impact

Run commands through Claude Code against a **controlled fixture** and assert on actual behavior. Start with the cheapest commands (`sdlc_doctor`, `sdlc_project_doctor`) since they only spawn Haiku sub-agents doing read-only checks.

**How it works:**
1. A setup script creates a temporary fixture directory with known state (specific files present/absent, specific config values)
2. The eval runner invokes Claude Code: `claude -p "/sdlc_doctor"` inside the fixture
3. A check script examines Claude Code's output for expected behavior (correct pass/fail for each check, correct output format, no file modifications)

**Why start with the doctor commands:**
- Lightest rate-limit footprint — only Haiku sub-agents, no Opus/Sonnet orchestration
- Most deterministic — read-only checks with binary pass/fail outcomes
- Fast — no builds, no git operations, no device connectivity
- Easy to create fixtures — just create/omit specific files

**Eval scenarios for `sdlc_doctor`:**

| Scenario | Fixture state | Expected behavior |
|----------|--------------|-------------------|
| `all_pass` | All files present, all config correct | All ✅, summary says "All checks passed" |
| `missing_openspec_config` | Remove `openspec/config.yaml` | ❌ for that check, others pass, summary shows 1 failure |
| `missing_opsx_commands` | Remove 2 of the 6 opsx command files | ❌ for those 2, others pass |
| `security_plugin_disabled` | Set plugin to `false` in settings | ❌ for security check |
| `everything_broken` | Remove all checked files | All ❌, correct failure count |

**Eval scenarios for `sdlc_project_doctor`:**

| Scenario | Fixture state | Expected behavior |
|----------|--------------|-------------------|
| `all_pass` | Full project config present | All ✅ |
| `missing_detekt` | Remove detekt config | ❌ for detekt checks only |
| `low_coverage_threshold` | Set minBound to 50 instead of 95 | ❌ for kover threshold check |

**What we assert on:**
- Output contains the expected ✅/❌ lines for each check
- Summary count is correct (e.g., "14/15 checks passed, 1 failed")
- No files were modified in the fixture (read-only guardrail)
- Output format matches the specified structure

**Runner script:** `docs/sdlc/evals/run_evals.sh`
```bash
# Usage:
./docs/sdlc/evals/run_evals.sh                    # Run all evals
./docs/sdlc/evals/run_evals.sh doctor              # Run only doctor evals
./docs/sdlc/evals/run_evals.sh --tool opencode     # Run with OpenCode instead
```

---

### Phase 3: Extend to Higher-Value Commands — moderate rate-limit impact

Once the doctor evals are stable, add evals for the commands with more complex behavior and critical guardrails. These use more rate-limit budget per run but test the most important behaviors.

**Priority order (by value-to-rate-limit-cost ratio):**

1. **`sdlc_open_story`** — test git safety guardrails
   - Fixture: a git repo with known state (dirty working tree, existing branches)
   - Key assertions: uses `git mv`, asks user on dirty state, creates correct branch name, doesn't delete data
   - Rate-limit note: Opus sub-agent for refinement is the heaviest part — could be skipped for guardrail-focused evals

2. **`sdlc_propose_change`** — test the "no assumptions" guardrail
   - Fixture: a repo with an open user story and ambiguous requirements
   - Key assertion: the command surfaces questions instead of making assumptions

3. **`sdlc_verify_story`** — test blocking gate protocol
   - Fixture: a repo where specific gates will fail
   - Key assertion: command stops on first FAIL, doesn't proceed to later gates
   - Rate-limit note: short-circuits on failure, so a FAIL-early scenario is cheaper than a full pass

4. **`sdlc_implement_change`** — test BDD discipline and model assignment
   - Most rate-limit-heavy to run fully; test individual sections rather than the whole command
   - Rate-limit note: defer or test subsections only. Run sparingly.

**Implemented scenarios:**

| Scenario | Command | Fixture state | Key assertions |
|----------|---------|--------------|----------------|
| `open_story_dirty_state` | `sdlc_open_story` | Git repo on main with uncommitted changes | Output warns about dirty state, mentions specific dirty file |
| `open_story_not_on_main` | `sdlc_open_story` | Git repo on `feature/existing-work` branch | Output detects non-main branch, asks about proceeding/switching |
| `verify_story_precondition_fail` | `sdlc_verify_story` | Story in "New" state (not WIP) | Stops before gates, doesn't reach later verification steps |
| `verify_story_blocks_on_gate_failure` | `sdlc_verify_story` | WIP story, no openspec change artifacts | Blocks at first gate, doesn't reach downstream gates |
| `propose_change_surfaces_questions` | `sdlc_propose_change` | WIP story with deliberately ambiguous requirements | Surfaces questions, does not make assumptions |

**Deferred:** `sdlc_implement_change` evals require a full openspec change with task artifacts and are the most rate-limit-heavy. To be added when the first 3 commands' evals are stable.

**Fixture design for git-based commands:**

All Phase 3 fixtures use `create_story_command_base_fixture()` which creates a git repo with:
- Git user config, initial commit on `main`
- Minimal guidelines files (userstories, git, reports, android, process)
- OpenSpec config, AGENTS.md, gradlew stub
- `docs/userstories/` and `docs/reports/` directories

Scenarios extend the base with `create_sample_story_new()`, `create_sample_story_wip()`, or `create_sample_story_ambiguous()` and then apply scenario-specific tweaks (dirty files, branch switches, etc.).

All Phase 3 scenarios use `run_eval_with_args()` which supports `$ARGUMENTS` substitution and `--max-turns` to control cost.

---

### Phase 4: Regression Comparison and Cross-Tool Testing — builds on Phase 2-3

**Regression comparison:**
- Each eval run outputs a JSON results file (scenario name, pass/fail, timestamp)
- A comparison script diffs two result files and highlights regressions
- Workflow: run before changes → make changes → run after → compare

```bash
./docs/sdlc/evals/run_evals.sh > results/baseline.json
# ... make changes to commands ...
./docs/sdlc/evals/run_evals.sh > results/candidate.json
./docs/sdlc/evals/compare_results.sh results/baseline.json results/candidate.json
```

**Cross-tool testing:**
- Same fixtures and assertions, different `--tool` flag
- `--tool claude` (default): runs via `claude -p`
- `--tool opencode`: runs via OpenCode's equivalent CLI
- Comparison script can diff results across tools

---

## Files

```
docs/sdlc/evals/
├── check_guardrails.sh                # Phase 1: static assertions (126 checks)
├── run_evals.sh                       # Phase 2 & 3: execution eval runner
├── lib/
│   └── eval_helpers.sh                # Shared fixture creators, assertion helpers
├── scenarios/                         # Phase 2: doctor scenarios
│   ├── doctor_all_pass.sh
│   ├── doctor_everything_broken.sh
│   ├── doctor_missing_openspec_config.sh
│   ├── doctor_missing_opsx_commands.sh
│   ├── doctor_security_disabled.sh
│   ├── project_doctor_all_pass.sh
│   ├── project_doctor_low_coverage.sh
│   ├── project_doctor_missing_detekt.sh
│   ├── open_story_dirty_state.sh      # Phase 3: git dirty state detection
│   ├── open_story_not_on_main.sh      # Phase 3: non-main branch detection
│   ├── verify_story_precondition_fail.sh      # Phase 3: precondition validation
│   ├── verify_story_blocks_on_gate_failure.sh # Phase 3: blocking gate protocol
│   └── propose_change_surfaces_questions.sh   # Phase 3: no-assumptions guardrail
├── compare_results.sh                 # Phase 4: regression comparison (planned)
└── results/                           # Gitignored eval results
    └── .gitkeep
```

## Verification

- **Phase 1:** Run `./docs/sdlc/evals/check_guardrails.sh` — all assertions pass against current command files. Intentionally remove a guardrail phrase → re-run → verify it catches the regression.
- **Phase 2:** Run `./docs/sdlc/evals/run_evals.sh phase2` — all doctor scenarios pass. Intentionally break a fixture (remove a file that should exist) → verify the eval catches the changed behavior.
- **Phase 3:** Run `./docs/sdlc/evals/run_evals.sh phase3` — all Phase 3 scenarios pass. Run individual commands selectively: `./docs/sdlc/evals/run_evals.sh open_story` or a single scenario: `./docs/sdlc/evals/run_evals.sh open_story_dirty_state`.
- **Phase 4:** Run before/after a command change → verify the comparison script correctly identifies regressions.

## Recommended Workflow

- **Always run Phase 1** before committing changes to command files — it's free and instant.
- **Run Phase 2** when touching any command — lightweight, ~8 scenarios: `./docs/sdlc/evals/run_evals.sh phase2`
- **Run Phase 3 selectively** — only the scenarios relevant to the specific command you changed: `./docs/sdlc/evals/run_evals.sh open_story` or `./docs/sdlc/evals/run_evals.sh verify_story`. Avoid running `phase3` in a tight loop to stay within rate limits.
