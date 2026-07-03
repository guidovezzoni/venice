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

**Fixture design for git-based commands:**
```bash
# Setup script creates:
fixture_dir=$(mktemp -d)
cd "$fixture_dir"
git init
# Create known files, branches, user stories, openspec config
# Run eval
# Teardown
rm -rf "$fixture_dir"
```

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

## Files to Create

```
docs/sdlc/evals/
├── README.md                          # How evals work, how to run
├── check_guardrails.sh                # Phase 1: static assertions
├── run_evals.sh                       # Phase 2+: execution eval runner
├── compare_results.sh                 # Phase 4: regression comparison
├── fixtures/                          # Fixture setup scripts
│   ├── setup_doctor_all_pass.sh
│   ├── setup_doctor_missing_openspec.sh
│   ├── setup_doctor_missing_opsx.sh
│   ├── setup_doctor_security_disabled.sh
│   ├── setup_doctor_everything_broken.sh
│   ├── setup_project_doctor_all_pass.sh
│   ├── setup_project_doctor_missing_detekt.sh
│   └── setup_project_doctor_low_coverage.sh
├── assertions/                        # Expected outputs per scenario
│   ├── doctor_all_pass.assertions
│   ├── doctor_missing_openspec.assertions
│   └── ...
└── results/                           # Gitignored eval results
    └── .gitkeep
```

## Verification

- **Phase 1:** Run `./docs/sdlc/evals/check_guardrails.sh` — all assertions pass against current command files. Intentionally remove a guardrail phrase → re-run → verify it catches the regression.
- **Phase 2:** Run `./docs/sdlc/evals/run_evals.sh doctor` — all doctor scenarios pass. Intentionally break a fixture (remove a file that should exist) → verify the eval catches the changed behavior.
- **Phase 4:** Run before/after a command change → verify the comparison script correctly identifies regressions.

## Recommended Workflow

- **Always run Phase 1** before committing changes to command files — it's free and instant.
- **Run Phase 2** when touching any command — lightweight, ~8 scenarios.
- **Run Phase 3 selectively** — only the scenarios relevant to the specific command you changed. Avoid running the full suite in a tight loop to stay within rate limits.
