# SDLC — Agentic AI Software Development Lifecycle

SDLC is a set of four commands that let an AI coding agent autonomously drive the full lifecycle of a user story — from opening to verified delivery — while keeping the human developer in control of key decisions.

Built on top of [OpenSpec](https://github.com/Fission-AI/OpenSpec/) (Specification-Driven Development), SDLC replaces manual task management with agentic orchestration: the AI reads specifications, reasons about architecture, writes and verifies code, and produces auditable reports at every stage.

## What makes this agentic

| Capability | How SDLC uses it |
|---|---|
| **Autonomous multi-step reasoning** | Each command chains 7-17 sequential steps, making decisions at each gate without human intervention |
| **Sub-agent orchestration** | Implementation spawns parallel sub-agents (Sonnet for complex BDD work, Haiku for mechanical tasks), each with a fresh context window |
| **Tool use** | The agent drives git, Gradle, adb, static analysis, and security scanners directly |
| **Self-healing loops** | Failed tests or security findings trigger automatic fix-and-retry cycles |
| **On-device verification** | The agent installs the app on a physical device, interacts with it via UIAutomator, and verifies behaviour autonomously |
| **Specification grounding** | All code generation is anchored to living specs and acceptance criteria — not just free-form prompts |

## The Four Commands

### 1. `/sdlc_open_story` — Open and Refine

Prepares a user story for development.

**What the agent does autonomously:**
- Switches to `main`, pulls latest changes, creates a feature branch
- Locates the next user story from the backlog (or accepts a specific one)
- Performs a full product analysis as an expert PM/BA: identifies fields, endpoints, files to modify, testing strategy, security/GDPR/performance concerns
- Enriches the story with implementation-ready detail so the developer (or subsequent agent) can work without ambiguity
- Produces an HTML report summarising decisions made

---

### 2. `/sdlc_propose_change` — Explore and Design

Generates the full technical design and task breakdown.

**What the agent does autonomously:**
- Explores the codebase to identify integration points, risks, and dependencies
- Asks clarifying questions when requirements are ambiguous (the one human-in-the-loop pause)
- Generates all SDD artefacts: proposal, design document, delta specifications, and a structured task list
- Structures tasks using BDD (Behaviour-Driven Development): each testable unit becomes a **test-first pair** — write the failing test, then write the code that makes it pass
- Classifies tasks by complexity to enable cost-efficient sub-agent assignment later

---

### 3. `/sdlc_implement_change` — Build with Sub-Agent Orchestration

Implements the entire change using multi-agent coordination.

**What the agent does autonomously:**
- Reads the task list and splits it into sections
- Assigns each section to the cheapest capable model (Sonnet for BDD red/green cycles, Haiku for wiring and previews)
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

**What the agent does autonomously:**
- Verifies implementation matches specifications (OpenSpec verify)
- Scans for unresolved TODOs and classifies them as blocking or acknowledged
- Runs a security review on all pending changes
- Executes clean build + static analysis (lint, unused imports, deprecations)
- Runs the full unit test suite
- Checks that every new class has a corresponding test file
- Checks that every Compose screen has preview coverage for all UI state fields
- Runs instrumented tests on a connected device and exercises the app via adb/UIAutomator
- Validates every acceptance criterion from the user story against the actual codebase
- Closes the story, syncs specifications, and archives the change artefacts
- Produces a detailed verification report with pass/fail status for each gate

---

## End-to-End Flow

```
User Story (backlog)
       │
       ▼
┌─────────────────┐
│  /open_story    │  Branch, refine, report
└────────┬────────┘
         ▼
┌─────────────────┐
│  /propose_change│  Explore, design, BDD tasks
└────────┬────────┘
         ▼
┌─────────────────┐
│  /implement     │  Sub-agents build + test
└────────┬────────┘
         ▼
┌─────────────────┐
│  /verify_story  │  Quality gates, archive
└────────┬────────┘
         ▼
   Done (merged)
```

## Key Design Decisions

- **Human stays in control**: The agent pauses for clarification during design and for manual verification when adb-based testing is not feasible. Every other step runs autonomously.
- **Cost efficiency**: Mechanical tasks (DI wiring, string resources, previews) use cheaper models; only complex reasoning tasks use the full-capability model.
- **Auditable**: Every command appends to an HTML report, creating a complete audit trail of what was decided, built, and verified.
- **Spec-grounded**: Code generation is always anchored to explicit specifications and acceptance criteria, reducing hallucination and drift.
- **Self-correcting**: Failed tests, security findings, and missed checkboxes trigger automatic retry loops rather than silent failures.
