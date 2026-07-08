## Warning

> **This is an experimental command.**
>
> It combines analysis, implementation, and verification into a streamlined three-phase flow. It produces a lightweight task list instead of full OpenSpec artifacts, and consolidates verification into a single pass. Human review gates between phases are reduced to a single Q&A checkpoint during analysis.
>
> For full traceability and formal spec management, use the individual commands (`/sdlc_open_story`, `/sdlc_propose_change`, `/sdlc_implement_change`, `/sdlc_verify_story`) instead.

**Before proceeding, ask the user:**
> "This command streamlines the SDLC into three phases (Analyze, Build, Check) with a single Q&A gate. It skips OpenSpec artifacts, reports, and some verification gates in favor of speed. Do you want to proceed? (yes/no)"

If the answer is anything other than an explicit **yes**, stop immediately and suggest running the individual commands instead.

---

Please vibe the user story end-to-end: $ARGUMENTS.

This command chains analysis, implementation, and verification into three phases with auto-commits between them. It preserves the core analytical value (story refinement, codebase exploration, user Q&A) while cutting ceremony (OpenSpec artifacts, reports, BDD pairing, redundant scans).

## Model Assignment

| Phase | Step | Sub-agent model |
|-------|------|----------------|
| 1. Analyze | Open story | haiku |
| 1. Analyze | Refine story | opus |
| 1. Analyze | Explore codebase | sonnet |
| 1. Analyze | Generate task list | sonnet |
| 2. Build | Implement sections | sonnet / haiku per section |
| 3. Check | Build + tests | haiku |
| 3. Check | Security review | sonnet |
| 3. Check | On-device tests | haiku |
| 3. Check | Definition of Done | sonnet |
| 3. Check | Close story | haiku |

## Commit Policy

After each phase completes successfully, the orchestrator commits all changes before launching the next phase.

| Phase | Commit message |
|-------|---------------|
| 1 | `{REF} Analyze user story` |
| 2 | `{REF} Implement change` |
| 3 | `{REF} Verify and close story` |

Where `{REF}` is the story/ticket reference extracted from the branch name (e.g. `PL013`).

## Task List Convention

This command generates a lightweight task list instead of full OpenSpec artifacts. The task list is stored at:
```
vibe/{STORY_ID}-tasks.md
```

The orchestrator creates the `vibe/` directory if it doesn't exist.

## Steps

### Phase 1 — Analyze

This phase combines story opening, refinement, codebase exploration, and task planning.

#### Step 1.1: Git safety checks

1. **Check for uncommitted changes.** Run `git status`. If changes exist, warn the user listing the changes and ask if they want to proceed. DO NOT MAKE ASSUMPTIONS and DO NOT DELETE DATA.

2. **Handle current branch.** Run `git branch --show-current`.
   - **If on `main`:** Fetch and pull latest changes from remote. If conflicts, inform user and ask.
   - **If NOT on `main`:** Ask whether to proceed on current branch or switch to `main`.
     - If proceeding on current branch: note that branch creation in step 1.3 must be skipped.
     - If switching to `main`: switch, fetch, and pull.

#### Step 1.2: Resolve the user story

If `$ARGUMENTS` is empty, identify the **Next User Story** per @docs/guidelines/guidelines-userstories.md. Inform the user which story was auto-selected. If no actionable story exists, inform the user and stop.

Match the argument against user story files by number or partial name. If no match, ask the user. Validate the **preconditions for Opening** per @docs/guidelines/guidelines-userstories.md. If unmet, inform the user and stop.

#### Step 1.3: Create a feature branch

Skip if the user chose to proceed on their current non-main branch in step 1.1. Otherwise create a branch under `feature/` prefixed with the ticket number.

#### Step 1.4: Open the user story (sub-agent)

Spawn a sub-agent to rename the story file and update the index.

```
Agent(
  description: "Open user story",
  model: "haiku",
  prompt: "<constructed prompt>"
)
```

**Sub-agent prompt:**
```
You are performing a file-management operation to open a user story for development.

## Task

Perform the Opening operation as defined in the user story guidelines.

Read the guidelines at: docs/guidelines/guidelines-userstories.md

Then apply the Opening operation to the user story file at:
  {USER_STORY_FILE_PATH}

Update the index file at: docs/userstories/index.md to reflect the new status/filename.

## Rules
- Use `git mv` (not plain mv) so git tracks renames
- Follow the exact Opening procedure from the guidelines
- Update the index link to match the new filename

## When Done
Report:
1. What operation was performed
2. New file path
3. Confirm the index was updated
```

**Verification:** Confirm the story file exists at its `-WIP` path and the index is updated.

**Failure handling:** Retry once. If Haiku fails, escalate to Sonnet.

#### Step 1.5: Refine the user story (sub-agent)

Spawn an Opus sub-agent for deep product and technical analysis.

```
Agent(
  description: "Refine user story",
  model: "opus",
  prompt: "<constructed prompt>"
)
```

**Sub-agent prompt:**
```
You are an expert Product Manager, Business Analyst, and Software Architect with deep
expertise in Android development, GDPR compliance, and security.

## Task

Read and deeply analyse this user story, then enrich it with implementation-ready detail.

## User Story

Read the user story file at: {USER_STORY_FILE_PATH}

## Guidelines

Read: docs/guidelines/guidelines-android.md
Read: docs/guidelines/guidelines-process.md

## Analysis Checklist

Produce a comprehensive refinement covering:
1. Full functionality description
2. Fields and data structures needed
3. Endpoint URLs / API contracts (if applicable)
4. Files to modify (with brief rationale)
5. Unit test strategy
6. Documentation updates needed
7. Security concerns
8. Performance concerns
9. GDPR / privacy implications
10. Non-functional requirements
11. Completion steps (what "done" looks like)

## Critical Constraints
- Stay at the "what" level, NOT the "how" — architecture decisions come later
- Do NOT make assumptions about unclear requirements — surface them as questions
- Write the refinement at the top of the story file
- Preserve the original story content below under "## Original user story"

## When Done
Report:
1. Summary of the refinement
2. Questions that need user input (if any)
3. Any concerns or risks identified
```

**Verification:** Read the story file and confirm the refinement structure is present.

**Failure handling:** Retry once with Opus.

#### Step 1.6: Explore the codebase (sub-agent)

Spawn a Sonnet sub-agent to investigate integration points and risks.

```
Agent(
  description: "Explore codebase for user story",
  model: "sonnet",
  prompt: "<constructed prompt>"
)
```

**Sub-agent prompt:**
```
You are exploring a codebase to prepare for implementing a user story.

## User Story

{PASTE FULL USER STORY CONTENT — including refinement from step 1.5}

## Your Mission

Investigate the codebase to understand:
1. Existing architecture relevant to this story
2. Integration points where new code will connect
3. Patterns already in use that should be followed
4. Hidden complexity or risks
5. Dependencies between components

## Context Files
- Android guidelines: docs/guidelines/guidelines-android.md
- Process guidelines: docs/guidelines/guidelines-process.md
- AGENTS.md for project overview

## Output Format

### Architecture Findings
- Relevant modules, classes, patterns found

### Integration Points
- Where new code connects to existing code

### Questions for the User
- Ambiguities or decisions needing human input
- Each question should explain WHY it matters
- DO NOT MAKE ASSUMPTIONS — surface everything unclear

### Risks and Concerns
- Technical risks, complexity warnings

### Recommendations
- Suggested approach based on existing patterns

## Guardrails
- Read-only exploration — do NOT create or modify files
- Do NOT make assumptions — surface them as questions
- Be thorough but focused on what's relevant to this story
```

**Verification:** Confirm the report includes all five sections.

**Failure handling:** If incomplete, retry with Sonnet. If still empty, orchestrator explores itself.

#### Step 1.7: Clarify doubts (interactive)

Present ALL questions from steps 1.5 and 1.6 to the user. Ensure all ambiguities are resolved. Ask additional questions if needed. No assumptions or unresolved doubts should be carried forward. DO NOT MAKE ASSUMPTIONS.

#### Step 1.8: Generate task list (sub-agent)

Spawn a Sonnet sub-agent to create the task list.

```
Agent(
  description: "Generate task list",
  model: "sonnet",
  prompt: "<constructed prompt>"
)
```

**Sub-agent prompt:**
```
You are creating a structured task list for implementing a user story in an Android project.

## User Story

{PASTE FULL USER STORY CONTENT — including refinement}

## Exploration Findings

{PASTE OUTPUT FROM STEP 1.6}

## User Clarifications

{PASTE Q&A FROM STEP 1.7}

## Task

Create a task list file at: vibe/{STORY_ID}-tasks.md

Create the `vibe/` directory if it doesn't exist.

## Task List Format

Use this exact format:

    # Tasks: {STORY_ID} — {STORY_TITLE}

    ## Design Context
    Brief design notes: key decisions, patterns to follow, integration points.
    Keep this concise — enough context for implementation, not a design document.

    ## 1. Section Name
    - [ ] 1.1 Task description
    - [ ] 1.2 Task description

    ## 2. Section Name
    - [ ] 2.1 Task description
    ...

## Structuring Rules

Group tasks into logical sections. Each section should be independently implementable
by a sub-agent with a fresh context window.

Ordering:
- Prerequisites (data models, interfaces, dependencies) first
- Core implementation (use cases, repositories, ViewModels) in the middle
  - Include test tasks alongside implementation: "Implement X and write tests"
  - Tests and implementation can be in the same task — no need for separate test-first pairs
- UI tasks (composables, previews, navigation) after core
- Integration tasks (DI wiring, string resources) near the end
- Final verification section at the end (if needed)

Each task description should be specific enough for an agent to implement without ambiguity.
Include target class names, method signatures, or patterns to follow where helpful.

## Context Files
- Android guidelines: docs/guidelines/guidelines-android.md
- Process guidelines: docs/guidelines/guidelines-process.md

## When Done
Report:
1. Task file location
2. Total sections and task count
3. Any issues encountered
```

**Verification:** Confirm the task file exists at `vibe/{STORY_ID}-tasks.md` with properly structured sections.

**Failure handling:** Retry with Sonnet. If still failing, escalate to Opus.

#### Step 1.9: Commit

```
git add -A
git commit -m "{REF} Analyze user story

Co-Authored-By: Claude <noreply@anthropic.com>"
```

If any step in Phase 1 reported a blocker, relay it to the user and STOP.

---

### Phase 2 — Build

This phase implements all tasks from the task list using sub-agent orchestration.

#### Device connectivity

**Early reminder (non-blocking):** Inform the user that a connected Android device will be needed later for on-device tests. Do not block. Proceed immediately.

**Device gate (blocking):** Whenever a step requires a connected Android device:

1. Run `adb devices` to check for a connected device.
2. If no device is listed:
   a. Ask the user to connect one.
   b. **BLOCK here.** Wait for the user to confirm.
   c. Re-run `adb devices` to verify. If still not listed, repeat from (a).
3. Only proceed once a device is confirmed connected.

#### Step 2.1: Parse the task list

Read `vibe/{STORY_ID}-tasks.md`. Parse into sections delimited by `## N.` headings. For each section, record:
- Section number and title
- Task list (the `- [ ]` / `- [x]` lines)
- Completion status (for resume support)

Skip fully completed sections.

#### Step 2.2: Classify sections and assign models

For each section with pending tasks, determine the model tier:

| Section pattern | Model |
|---|---|
| Contains implementation + test tasks | **Sonnet** (`sonnet`) |
| All tasks are mechanical (wiring, strings, config) | **Haiku** (`haiku`) |
| All tasks are Compose previews or read-only verification | **Haiku** (`haiku`) |
| Unclear or mixed | **Sonnet** (`sonnet`) — safe default |

Display the execution plan before proceeding. Proceed without waiting for user confirmation.

#### Step 2.3: Execute sections sequentially

Sections MUST be executed in order — later sections may depend on code from earlier ones.

For each section with pending tasks:

**i. Check device gate.** If any task mentions on-device work, apply the device gate BEFORE spawning the sub-agent.

**ii. Build the sub-agent prompt** using the template below.

**iii. Spawn the sub-agent:**
```
Agent(
  description: "Section N: <title>",
  model: "<sonnet|haiku>",
  prompt: "<constructed prompt>"
)
```

**iv. Verify completion.** Read tasks file, count `- [x]` lines in this section, compare against expected total.

**v. Handle results:**
- **All tasks checked:** Display `Section N complete (N/N tasks)` and proceed
- **Some unchecked, no errors reported:** Spawn a Haiku sub-agent to fix checkboxes
- **Error or blocker:** Retry once (upgrade Haiku to Sonnet for retry). If still failing, PAUSE and report to user

#### Sub-agent prompt template

```
You are implementing section {SECTION_NUMBER} ("{SECTION_TITLE}") of a user story
in an Android project.

## Your Tasks

{PASTE THE EXACT TASK LINES FOR THIS SECTION, including the `- [ ]` prefix}

## Design Context

{PASTE THE DESIGN CONTEXT SECTION FROM THE TASKS FILE}

## Coding Guidelines

Read `docs/guidelines/guidelines-android.md` for code style, naming conventions,
and testing patterns. Follow them strictly.

## Task Completion — CRITICAL

After completing EACH task, you MUST immediately update the tasks file at:
  vibe/{STORY_ID}-tasks.md

Change the task's checkbox from `- [ ]` to `- [x]`.
Do this IMMEDIATELY after each task, BEFORE moving to the next task.
This is a hard requirement — your work is not considered complete without it.

## When Done

Report:
1. Which tasks you completed (list task IDs)
2. Any issues or blockers encountered
3. Confirm all checkboxes in your section are updated
```

#### Step 2.4: Final verification

After all sections are processed, read the tasks file one final time. Confirm every task is `[x]`. Report overall progress:
```
## All Sections Complete

Progress: N/N tasks complete
Sections processed: M (X with Sonnet, Y with Haiku)
```

#### Step 2.5: Review TODOs

Scan all source files under `app/src/` for TODO comments (`// TODO`, `/* TODO`, `# TODO`). For each TODO found:
- Determine if it is related to the current story or if its precondition has been satisfied by this story's implementation.
- Classify as:
  - **RESOLVE NOW** — related to this story, or its stated precondition has been fulfilled.
  - **ACKNOWLEDGED** — unrelated and precondition unmet.

If any RESOLVE NOW TODOs:
1. Present to user for awareness.
2. Add new tasks to `vibe/{STORY_ID}-tasks.md`.
3. Re-run implementation for the new tasks (step 2.3 will skip already-completed sections).
4. Re-check TODOs. Loop until all resolved or acknowledged.

#### Step 2.6: Commit

```
git add -A
git commit -m "{REF} Implement change

Co-Authored-By: Claude <noreply@anthropic.com>"
```

If any step reported a blocker, relay to user and STOP.

---

### Phase 3 — Check

Consolidated verification pass. Each step is a blocking gate — if it fails, STOP and report.

#### Step 3.1: Build, static analysis, and unit tests

Run:
```
./gradlew clean check test
```

If it fails, present errors to the user and STOP.

#### Step 3.2: Security review

Execute the `/security-review` skill. If critical or high-severity findings exist:
1. Present findings to user for awareness.
2. Fix all reported issues.
3. Re-run `/security-review`.
4. Repeat until clean.

#### Step 3.3: On-device tests

Apply the **device gate** before proceeding.

1. Run instrumented tests:
   ```
   ./gradlew connectedDebugAndroidTest
   ```
   If tests fail, fix iteratively and re-run.

2. If tests pass, install and launch the app:
   ```
   ./gradlew installDebug
   adb shell am start -n com.guidovezzoni.venice/.ui.MainActivity
   ```

3. Exercise the feature under test via adb, following the UIAutomator workflow from @docs/guidelines/guidelines-process.md (max 3 interactions).

4. If NOT_FEASIBLE (too complex for adb), ask user for manual verification. **BLOCK** until confirmed.

5. If FAIL, present to user and STOP.

#### Step 3.4: Definition of Done (sub-agent)

```
Agent(
  description: "Definition of Done verification",
  model: "sonnet",
  prompt: "<constructed prompt>"
)
```

**Sub-agent prompt:**
```
You are verifying that all acceptance criteria from the user story are met.

## User Story

{PASTE ACCEPTANCE CRITERIA / DEFINITION OF DONE SECTION}

## Files Modified by This Story

{MODIFIED_FILES_LIST from git diff --name-only main...HEAD}

## Task

For each item in the "Acceptance Criteria" or "Definition of Done" section:
1. Search the codebase for evidence the criterion is met
2. Report each item as PASS or FAIL with justification referencing specific files or tests

## Output Format

### RESULT: PASS or FAIL

PASS if all items are confirmed met.
FAIL if any item cannot be confirmed.

### Definition of Done Checklist
| # | Criterion | Status | Justification |
|---|---|---|---|
| 1 | [criterion text] | PASS/FAIL | [evidence] |
```

**Gate decision:** If RESULT is FAIL, present the failing criteria to the user and STOP.

**Failure handling:** Retry once with Sonnet.

#### Step 3.5: Close the user story (sub-agent)

```
Agent(
  description: "Close user story",
  model: "haiku",
  prompt: "<constructed prompt>"
)
```

**Sub-agent prompt:**
```
You are closing a user story by performing the Closing operation.

## Task

Perform the Closing operation as defined in the user story guidelines.

Read the guidelines at: docs/guidelines/guidelines-userstories.md

Then apply the Closing operation to the user story file at:
  {USER_STORY_FILE_PATH}

Update the index file at: docs/userstories/index.md to reflect the new status/filename.

## Rules
- Use `git mv` (not plain mv) so git tracks renames
- Follow the exact Closing procedure from the guidelines
- Update the index link to match the new filename

## When Done
Report:
1. What operation was performed
2. New file path
3. Confirm the index was updated
```

**Verification:** Confirm story file is at its `-DONE` path and index is updated.

**Failure handling:** Retry once. Escalate Haiku to Sonnet.

#### Step 3.6: Commit

```
git add -A
git commit -m "{REF} Verify and close story

Co-Authored-By: Claude <noreply@anthropic.com>"
```

---

### Final Summary

Display:

```
## Vibe Complete

Story: {STORY_ID} — {STORY_TITLE}
Branch: {BRANCH_NAME}

| Phase | Status | Commit |
|-------|--------|--------|
| 1. Analyze | DONE | {REF} Analyze user story |
| 2. Build | DONE | {REF} Implement change |
| 3. Check | DONE | {REF} Verify and close story |

Total commits: 3
Ready for: PR / merge to main
```
