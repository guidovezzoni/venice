# SDLC — Agentic AI Software Development Lifecycle

SDLC is a set of six commands that let an AI coding agent autonomously drive the full lifecycle of a user story — from opening to verified delivery — while keeping the human developer in control of key decisions.

Built on top of [OpenSpec](https://github.com/Fission-AI/OpenSpec/) (Spec-Driven Development), SDLC replaces manual task management with agentic orchestration: the AI reads specifications, reasons about architecture, writes and verifies code, and produces auditable reports at every stage.

An example of its usage can be found in [venice](https://github.com/guidovezzoni/venice).

**Please note**: this is work-in-progress: it needs to be tested and tailored for your own needs.

## Features

| Capability                          | How SDLC uses it                                                                                                                      |
| ----------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------- |
| **Autonomous multi-step reasoning** | Each command chains 15+ sequential steps, making decisions at each gate without human intervention                                    |
| **Sub-agent orchestration**         | All four commands delegate self-contained steps to cheaper sub-agents (standard tier for reasoning, fast tier for mechanical tasks), each with a fresh context window |
| **Tool use**                        | The agent drives git, Gradle, adb, static analysis, and security scanners directly                                                    |
| **Self-healing loops**              | Failed tests or security findings trigger automatic fix-and-retry cycles                                                              |
| **On-device verification**          | The agent installs the app on a physical device, interacts with it via UIAutomator, and verifies behaviour autonomously               |
| **Specification grounding**         | All code generation is anchored to living specs and acceptance criteria — not just free-form prompts                                  |

## The Six Commands

### 1. `/sdlc_open_story` — Open and Refine

Prepares a user story for development.

**Recommended model tier:** reasoning (orchestrator) — handles interactive git operations and user decisions; delegates refinement to reasoning-tier sub-agent (fresh context window).

**What the agent does autonomously:**

- Switches to `main`, pulls latest changes, creates a feature branch
- Locates the next user story from the backlog (or accepts a specific one)
- Spawns a reasoning-tier sub-agent for full product analysis as an expert PM/BA: identifies fields, endpoints, files to modify, testing strategy, security/GDPR/performance concerns
- Enriches the story with implementation-ready detail so the developer (or subsequent agent) can work without ambiguity
- Spawns a fast-tier sub-agent for report generation

---

### 2. `/sdlc_propose_change` — Explore and Design

Generates the full technical design and task breakdown.

**Recommended model tier:** reasoning (orchestrator) — handles story location and user Q&A; delegates exploration and proposal generation to standard-tier sub-agents.

**What the agent does autonomously:**

- Spawns a standard-tier sub-agent to explore the codebase: identifies integration points, risks, and dependencies
- Presents surfaced questions to the user for clarification (the human-in-the-loop pause)
- Spawns a standard-tier sub-agent to generate all SDD artefacts: proposal, design document, delta specifications, and a structured task list
- Structures tasks using BDD (Behaviour-Driven Development): each testable unit becomes a **test-first pair** — write the failing test, then write the code that makes it pass
- Classifies tasks by complexity to enable cost-efficient sub-agent assignment in implementation

---

### 3. `/sdlc_implement_change` — Build with Sub-Agent Orchestration

Implements the entire change using multi-agent coordination.

**Recommended model tier:** standard (orchestrator) — orchestration is procedural; sub-agents handle the heavy reasoning.

**What the agent does autonomously:**

- Reads the task list and splits it into sections
- Assigns each section to the cheapest capable model tier (standard for BDD red/green cycles, fast for wiring and previews)
- Spawns a sub-agent per section with a self-contained prompt — each agent writes code, runs tests, and checks off its tasks
- Enforces the BDD Red/Green discipline: test must fail before implementation, then pass after
- Verifies every checkbox after each sub-agent returns; retries or escalates on failure
- Resolves outstanding TODOs whose preconditions are now met
- Runs instrumented UI tests on a connected device
- Executes a security review and fixes any findings
- Updates project documentation (README, AGENTS.md) if affected

---

### 4. `/sdlc_verify_story` — Verify and Archive

End-to-end quality gate before a story is considered done.

**Recommended model tier:** standard (orchestrator) — orchestrates 14 sub-agents through a sequential blocking-gate pipeline.

**What the agent does autonomously:**

- Spawns sub-agents for each verification gate (standard tier for reasoning-heavy checks, fast tier for mechanical commands)
- After each gate: reads PASS/FAIL result, stops and reports to user on failure
- Verification gates: OpenSpec verify, TODO scan, security review, build, unit tests, coverage report, test file coverage, Compose preview coverage, on-device tests, Definition of Done
- Post-verification: closes story, syncs specs, archives change, updates documentation
- Produces a detailed verification report with pass/fail status for each gate

---

### 5. `/sdlc_doctor` — SDLC Framework Health Check

Validates that the SDLC tooling is properly installed and configured.

**Recommended model tier:** standard — orchestration is procedural; fast-tier sub-agents run the checks.

**What the agent does autonomously:**

- Spawns parallel fast-tier sub-agents, one per check category, for maximum speed
- All checks are read-only — no files are modified, no builds are run (except a lightweight `./gradlew tasks`)
- Checks cover: OpenSpec configuration, security review tool, and Gradle project health
- Aggregates results into a summary report with `[PASS]`/`**[FAIL]**` status per check
- Reports which items need attention for full SDLC workflow compatibility

---

### 6. `/sdlc_project_doctor` — Project Configuration Health Check

Validates that the project's quality tooling is properly configured.

**Recommended model tier:** standard — orchestration is procedural; fast-tier sub-agents run the checks.

**What the agent does autonomously:**

- Spawns parallel fast-tier sub-agents, one per check category, for maximum speed
- All checks are read-only — no files are modified, no builds are run
- Checks cover: Detekt static analysis, Kover code coverage, unit test dependencies, Fastlane, and CI/CD pipeline
- Aggregates results into a summary report with `[PASS]`/`**[FAIL]**` status per check
- Reports which items need attention for full project quality compliance

---

## End-to-End Flow

```
          User Story (backlog)
                 │
                 ▼
┌──────────────────────────────────┐
│    /sdlc_open_story              │  Branch, refine, report
└────────────────┬─────────────────┘
                 ▼
┌──────────────────────────────────┐
│    /sdlc_propose_change          │  Explore, design, BDD tasks
└────────────────┬─────────────────┘
                 ▼
┌──────────────────────────────────┐
│    /sdlc_implement_change        │  Sub-agents build + test
└────────────────┬─────────────────┘
                 ▼
┌──────────────────────────────────┐
│    /sdlc_verify_story            │  Quality gates, archive
└────────────────┬─────────────────┘
                 ▼
          Done (merged)


┌──────────────────────────────────┐
│    /sdlc_doctor                  │  SDLC framework health check
└──────────────────────────────────┘

┌──────────────────────────────────┐
│    /sdlc_project_doctor          │  Project config health check
└──────────────────────────────────┘
```

## Key Design Decisions

- **Human stays in control**:
    - Each of the command stops when the AI has created an output that human should review: refined user story, SDD specifications, code implementation. Detecting an issue earlier on will prevent bigger changes down the line. Apart from this, the agent will try to complete the task autonomously.
    - By default none of the commands will automatically commit and push, to allow human to review the changes
    - There are several check points in which the AI will request human intervention if some error condition shows, f.i. creating a new branch with staged changes, doubts requiring clarifications, physical device not connected, etc.
- **Cost efficiency**: Mechanical tasks (DI wiring, string resources, previews) use cheaper models; only complex reasoning tasks use the full-capability model.
- **Auditable**: Every command appends to an HTML report, creating a complete audit trail of what was decided, built, and verified.
- **Spec-grounded**: Code generation is always anchored to explicit specifications and acceptance criteria, reducing hallucination and drift.
- **Self-correcting**: Failed tests, security findings, and missed checkboxes trigger automatic retry loops rather than silent failures.

## Quick start

1. Download the `docs` folder into your project

2. Run the init script for your AI coding tool:

   **Claude Code:**
    - **Linux / macOS**: `./docs/sdlc/sdlc_init_claude_code.sh`
    - **Windows** (PowerShell, Developer Mode or elevated): `.\docs\sdlc\sdlc_init_claude_code.ps1`
    - Creates a `CLAUDE.md → AGENTS.md` symlink in the project root and links SDLC command files into `.claude/commands/sdlc/`

   **Codex CLI:**
    - **Linux / macOS**: `./docs/sdlc/sdlc_init_codex.sh`
    - **Windows** (PowerShell, Developer Mode or elevated): `.\docs\sdlc\sdlc_init_codex.ps1`
    - Links SDLC command files as skills into `.codex/skills/sdlc-*/SKILL.md`

   **OpenCode:**
    - **Linux / macOS**: `./docs/sdlc/sdlc_init_opencode.sh`
    - **Windows** (PowerShell, Developer Mode or elevated): `.\docs\sdlc\sdlc_init_opencode.ps1`
    - Links SDLC command files into `.opencode/command/sdlc-*.md`

3. Install OpenSpec on your machine from https://github.com/Fission-AI/OpenSpec/ and:
    - Initialise it in your project folder: `openspec init`
    - Type `openspec config profile`, select "workflows only", and ensure these commands are available: explore, propose, apply, verify, sync, archive

4. Tailor user story management by customising guidelines-userstories.md, with an MCP or whatever you use to handle them. By default it's expecting a list of md files.

## Tier Mapping

The SDLC command files use abstract model tiers — each tool resolves them to its own models:

| Tier | Intent | Claude Code | Codex CLI | OpenCode |
|------|--------|-------------|-----------|----------|
| fast | Mechanical tasks | haiku | (tool's cheapest) | (tool's cheapest) |
| standard | Reasoning tasks | sonnet | (tool's default) | (tool's default) |
| reasoning | Deep analysis | opus | (tool's most capable) | (tool's most capable) |

## Additional customisations

The basic recommendation is that these files should be modified directly from the agent, as they reference each other and agents are great in spotting these details. Rather than adding a new guideline, explain the improvement to the agent and ask it where it should go, given the current structure.

### AGENTS.md

Contains basic info about the project for the agent, and it's usually automatically updated by the agent when a change affects its content. All tools read `AGENTS.md` directly (Claude Code also creates a `CLAUDE.md → AGENTS.md` symlink for compatibility).

Some info usually contained in AGENTS.md will not likely end up in there, as they are already documented in the guidelines files.

The initial part of the AGENTS.md instructs the agent how to selectively load some guidelines files depending on the task it's working on.

### Guidelines files

There are two type of guidelines files:

#### Loaded by AGENTS.md

Guidelines files collect directions for the agent to handle the project, grouped into specific topics, f.i. Android native, git, etc. These should collect the best practices already in use by the team or the project.

These files can and should be fully customised according to the project, so that the agent knows exactly how to handle it.

Currently we have these three files, but can obviously be extended.

- [guidelines-android](docs/guidelines/guidelines-android.md) for Android native code style and best practices
- [guidelines-git](docs/guidelines/guidelines-git.md) for git operations and commit conventions
- [guidelines-process](docs/guidelines/guidelines-process.md) for general guidelines

#### Loaded by SDLC commands

The other guidelines file are primarily used by the SDLC commands and describe how to handle specific inputs and outputs.

- [guidelines-userstories](docs/guidelines/guidelines-userstories.md) for handling the user story backlog, this currently uses md files, but can use an MCP to access jira or any other tool.
- [guidelines-reports](docs/guidelines/guidelines-reports.md) for auditing reports, currently uses an HTML file but can be customised in any way.

(These are a category on their own, so they will likely change name at some point.)

## TODOs and improvements

- Add support for Cursor
- Define how to handle changes/fix after propose change: every unexpected change on the code in an open story should update story and specs, or at least check if they are affected
- Learn a lesson from a failure: the agent should be able to update the structure in case it spots a failure
- create a guideline for readme

## Repeating errors to fix
- opsx new change: Change name can only contain lowercase letters, numbers, and hyphens
- openspec status --json 2>&1 -> ✖ Error: Missing required option --change. Available changes: see-leg-distance
- verification: Error: Refusing to write through symlink: CLAUDE.md. Resolve the symlink and pass the real target path explicitly.


## Guidelines TODO

- async operations should be wrapped in a loading state with a spinner, and if required disabling the button that triggered the operation, to avoid re-trigger. The spinner should have a minimum duration of 0.5 seconds to avoid a flickering UI.
- step for static checks:
  - unused import directive / deprecation
- Test: when writing a test:
  - use SUT to clarify what class is being tested
  - do not use for the expected value, the same internal function being tested -> this however conflicts with BDD's black box behaviour
  - insert a comment with the AAA?
- There are several decisions that have been taken just "because it's a small project": that should not happen: all the projects I start are small and they will likely  become bigger, so they should use the expected architecture and structures.

Not sure what's best yet:

- When both Domain and UI require the same data type, f.i. an enum, where should this be defined? In Domain? Should it be duplicated in UI? Should it be defined in another root package?
- add PR review - other  LLM provider???
