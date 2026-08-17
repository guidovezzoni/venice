## Warning

> **This is an experimental command.**
>
> It compresses the entire SDLC into two phases (Think, Build & Check) with minimal ceremony.
> Analysis is merged into a single sub-agent call. Verification is folded into the build phase
> with no formal Definition of Done check. No OpenSpec artifacts, no reports.
>
> Best for: small features, spikes, well-understood changes, or solo projects where
> speed matters more than traceability.
>
> For more guardrails, use the individual commands (`/sdlc_open_story`, `/sdlc_propose_change`, `/sdlc_implement_change`, `/sdlc_verify_story`) instead.

---

Please vibe the user story end-to-end: $ARGUMENTS.

This command compresses the SDLC into two phases with auto-commits. It merges story refinement, codebase exploration, and task planning into a single sub-agent call, then implements and verifies in one phase.

## Model Assignment

| Phase | Step | Sub-agent model |
|-------|------|----------------|
| 1. Think | Open story | haiku |
| 1. Think | Analyze, explore, and plan | opus |
| 2. Build & Check | Implement sections | sonnet / haiku per section |
| 2. Build & Check | Build + tests | haiku |
| 2. Build & Check | Security review | sonnet |
| 2. Build & Check | On-device tests | haiku |
| 2. Build & Check | Close story | haiku |

## Commit Policy

| Phase | Commit message |
|-------|---------------|
| 1 | `{REF} Analyze and plan` |
| 2 | `{REF} Implement and verify` |

Where `{REF}` is the story/ticket reference extracted from the branch name (e.g. `PL013`).

## Task List Convention

Task list stored at:
```
vibe/{STORY_ID}-tasks.md
```

The orchestrator creates the `vibe/` directory if it doesn't exist.

## Steps

### Phase 1 — Think

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

#### Step 1.5: Analyze, explore, and plan (sub-agent)

Single Opus sub-agent that combines story refinement, codebase exploration, and task list generation into one pass.

```
Agent(
  description: "Analyze, explore, and plan",
  model: "opus",
  prompt: "<constructed prompt>"
)
```

**Sub-agent prompt:**
```
You are an expert Product Manager, Business Analyst, and Software Architect with deep
expertise in Android development, GDPR compliance, and security. You will analyse a
user story, explore the codebase, and produce a task list — all in one pass.

## Part 1: Refine the User Story

Read the user story file at: {USER_STORY_FILE_PATH}
Read: docs/guidelines/guidelines-android.md
Read: docs/guidelines/guidelines-process.md

Enrich the story with implementation-ready detail covering:
1. Full functionality description
2. Fields and data structures needed
3. Endpoint URLs / API contracts (if applicable)
4. Files to modify (with brief rationale)
5. Unit test strategy
6. Security, performance, and GDPR concerns
7. Non-functional requirements

Write the refinement at the top of the story file. Preserve original content under
"## Original user story".

Critical: stay at the "what" level, not the "how."

## Part 2: Explore the Codebase

Investigate the existing codebase to understand:
1. Architecture relevant to this story
2. Integration points where new code will connect
3. Patterns already in use that should be followed
4. Hidden complexity, risks, or dependencies

Read AGENTS.md for project overview.

## Part 3: Generate Task List

Based on the refinement and exploration, create a task list file at:
  vibe/{STORY_ID}-tasks.md

Create the `vibe/` directory if it doesn't exist.

Use this format:

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

Structuring rules:
- Group tasks into logical sections, each independently implementable by a sub-agent
  with a fresh context window
- Order: prerequisites -> core implementation (with tests) -> UI -> integration
- Tests and implementation can be in the same task — no need for separate test-first pairs
- Each task must be specific enough to implement without ambiguity
- Include target class names, method signatures, or patterns to follow where helpful

## Part 4: Surface Questions

List any ambiguities or decisions that need human input.
DO NOT MAKE ASSUMPTIONS — surface everything that is unclear.

## When Done

Report:
1. Refinement summary
2. Architecture findings and integration points
3. Task file location with section/task counts
4. Questions for the user (if any)
5. Risks identified
```

**Verification:** Confirm story file has the refinement structure and task file exists at `vibe/{STORY_ID}-tasks.md` with properly structured sections.

**Failure handling:** Retry once with Opus.

#### Step 1.6: Clarify doubts (interactive)

If the sub-agent surfaced questions, present them ALL to the user. Ensure all ambiguities are resolved. Ask additional questions if needed. DO NOT MAKE ASSUMPTIONS.

If answers require changes to the task list, update `vibe/{STORY_ID}-tasks.md` directly.

#### Step 1.7: Commit

```
git add -A
git commit -m "{REF} Analyze and plan

Co-Authored-By: Claude <noreply@anthropic.com>"
```

If any step reported a blocker, relay it to the user and STOP.

---

### Phase 2 — Build & Check

This phase implements all tasks and runs verification in one pass.

#### Device connectivity

**Early reminder (non-blocking):** Inform the user that a connected Android device will be needed later for on-device tests. Do not block. Proceed immediately.

**Device gate (blocking):** Whenever a step requires a connected Android device:

1. Run `adb devices` to check for a connected device.
2. If no device is listed:
   a. Ask the user to connect one.
   b. **BLOCK here.** Wait for the user to confirm.
   c. Re-run `adb devices` to verify. If still not listed, repeat from (a).
3. Only proceed once a device is confirmed connected.

#### Step 2.1: Parse and classify task list

Read `vibe/{STORY_ID}-tasks.md`. Parse into sections delimited by `## N.` headings. For each section, record section number, title, task lines, and completion status. Skip fully completed sections.

Classify each section:

| Section pattern | Model |
|---|---|
| Contains implementation + test tasks | **Sonnet** (`sonnet`) |
| All tasks are mechanical (wiring, strings, config) | **Haiku** (`haiku`) |
| All tasks are Compose previews or read-only verification | **Haiku** (`haiku`) |
| Unclear or mixed | **Sonnet** (`sonnet`) — safe default |

Display the execution plan. Proceed without waiting for user confirmation.

#### Step 2.2: Execute sections sequentially

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

**iv. Verify completion.** Read tasks file, count `- [x]` lines in this section.

**v. Handle results:**
- **All tasks checked:** Display `Section N complete (N/N tasks)` and proceed
- **Some unchecked, no errors:** Spawn a Haiku sub-agent to fix checkboxes
- **Error or blocker:** Retry once (upgrade Haiku to Sonnet). If still failing, PAUSE and report to user

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

#### Step 2.3: Verify all tasks complete

Read the tasks file. Confirm every task is `[x]`. Report progress.

#### Step 2.4: Build, static analysis, and unit tests

Run:
```
./gradlew clean check test
```

If failure: present errors, fix iteratively, and re-run. If stuck after two attempts, STOP and report to user.

#### Step 2.5: Security review

Execute the `/security-review` skill. If critical or high-severity findings:
1. Present findings to user for awareness.
2. Fix all reported issues.
3. Re-run `/security-review`.
4. Repeat until clean.

#### Step 2.6: On-device tests

Apply the **device gate** before proceeding.

1. Run instrumented tests:
   ```
   ./gradlew connectedDebugAndroidTest
   ```
   If tests fail, fix iteratively and re-run.

2. If tests pass, install and launch the app:
   ```
   ./gradlew installDebug
   adb shell am start -n <applicationId>/.ui.MainActivity
   ```

3. Exercise the feature under test via adb, following the UIAutomator workflow from @docs/guidelines/guidelines-process.md (max 3 interactions).

4. If NOT_FEASIBLE (too complex for adb), ask user for manual verification. **BLOCK** until confirmed.

5. If FAIL, present to user and STOP.

#### Step 2.7: Close the user story (sub-agent)

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

#### Step 2.8: Commit

```
git add -A
git commit -m "{REF} Implement and verify

Co-Authored-By: Claude <noreply@anthropic.com>"
```

---

### Final Summary

Display:

```
## Vibe Complete (Quick)

Story: {STORY_ID} — {STORY_TITLE}
Branch: {BRANCH_NAME}

| Phase | Status | Commit |
|-------|--------|--------|
| 1. Think | DONE | {REF} Analyze and plan |
| 2. Build & Check | DONE | {REF} Implement and verify |

Total commits: 2
Ready for: PR / merge to main
```
