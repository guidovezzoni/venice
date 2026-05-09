---
name: openspec-apply-change
description: Implement tasks from an OpenSpec change. Use when the user wants to start implementing, continue implementation, or work through tasks.
license: MIT
compatibility: Requires openspec CLI.
metadata:
  author: openspec
  version: "1.0"
  generatedBy: "1.2.0"
---

Implement tasks from an OpenSpec change.

**Input**: Optionally specify a change name. If omitted, check if it can be inferred from conversation context. If vague or ambiguous you MUST prompt for available changes.

**Steps**

1. **Select the change**

   If a name is provided, use it. Otherwise:
   - Infer from conversation context if the user mentioned a change
   - Auto-select if only one active change exists
   - If ambiguous, run `openspec list --json` to get available changes and use the **AskUserQuestion tool** to let the user select

   Always announce: "Using change: <name>" and how to override (e.g., `/opsx:apply <other>`).

2. **Check status to understand the schema**
   ```bash
   openspec status --change "<name>" --json
   ```
   Parse the JSON to understand:
   - `schemaName`: The workflow being used (e.g., "spec-driven")
   - Which artifact contains the tasks (typically "tasks" for spec-driven, check status for others)

3. **Get apply instructions**

   ```bash
   openspec instructions apply --change "<name>" --json
   ```

   This returns:
   - Context file paths (varies by schema - could be proposal/specs/design/tasks or spec/tests/implementation/docs)
   - Progress (total, complete, remaining)
   - Task list with status
   - Dynamic instruction based on current state

   **Handle states:**
   - If `state: "blocked"` (missing artifacts): show message, suggest using openspec-continue-change
   - If `state: "all_done"`: congratulate, suggest archive
   - Otherwise: proceed to implementation

4. **Read context files**

   Read the files listed in `contextFiles` from the apply instructions output.
   The files depend on the schema being used:
   - **spec-driven**: proposal, specs, design, tasks
   - Other schemas: follow the contextFiles from CLI output

5. **Show current progress**

   Display:
   - Schema being used
   - Progress: "N/M tasks complete"
   - Remaining tasks overview
   - Dynamic instruction from CLI

6. **Implement tasks using BDD Red/Green cycle (loop until done or blocked)**

   For each pending task, determine its type and follow the appropriate procedure:

   **A. Test task** (description starts with "Write test:" or "Add test:", or references a test class like `*Test`):

   1. Write the test code
   2. Run the specific test class: `./gradlew test --tests "*<TestClass>"` (derive class name from task description)
   3. **Verify RED**: Confirm the test fails (compilation error or assertion failure both count as red)
      - If the test unexpectedly passes: PAUSE — the test may be trivial, testing existing behaviour,
        or incorrectly written. Report this to the user before continuing.
      - If tests fail for unrelated reasons: PAUSE — report the pre-existing failure.
   4. Mark task complete: `- [ ]` → `- [x]`
   5. Show: `🔴 RED confirmed — test fails as expected`
   6. Continue to next task (which should be the implementation)

   **B. Implementation task following a test task** (the previous completed task was a test task in the same `## N.` section):

   1. Write the implementation code (minimal — just enough to make the test pass)
   2. Run the specific test class: `./gradlew test --tests "*<TestClass>"` (same class from the preceding test task)
   3. **Verify GREEN**: Confirm the test now passes
      - If this is an intermediate implementation task in the section and the test still fails: acceptable — note "Tests not yet green, continuing to next task in this section" and continue
      - If this is the last implementation task in the section and the test still fails: iterate on the implementation until it passes, or PAUSE if stuck
   4. Mark task complete: `- [ ]` → `- [x]`
   5. Show: `🟢 GREEN confirmed — test passes`
   6. Continue to next task

   **C. Non-test task** (prerequisites, strings, DI, composables, wiring, verification):

   1. Make the code changes required
   2. Keep changes minimal and focused
   3. Mark task complete: `- [ ]` → `- [x]`
   4. Continue to next task

   **Task type detection**:
   - A task is a "test task" if its description contains: "Write test", "Add test",
     or references a test class (e.g., `*Test`, `*Test.kt`)
   - A task is an "implementation after test" if the immediately preceding completed task
     in the same `## N.` section was a test task
   - All other tasks are "non-test tasks"

   **Pause if:**
   - Task is unclear → ask for clarification
   - Implementation reveals a design issue → suggest updating artifacts
   - Error or blocker encountered → report and wait for guidance
   - RED check unexpectedly passes → report before continuing
   - GREEN check fails after reasonable iteration → report and wait for guidance
   - User interrupts

7. **On completion or pause, show status**

   Display:
   - Tasks completed this session
   - Overall progress: "N/M tasks complete"
   - If all done: suggest archive
   - If paused: explain why and wait for guidance

**Output During Implementation**

```
## Implementing: <change-name> (schema: <schema-name>)

Working on task 2/12: Write test: GIVEN a trip id WHEN OnTripClicked...
[...writing test...]
Running ./gradlew test --tests "*TripListViewModelTest"...
🔴 RED confirmed — test fails (OnTripClicked not found in TripListUiIntent)
✓ Task complete

Working on task 3/12: Add OnTripClicked intent to TripListUiIntent
[...implementing...]
✓ Task complete

Working on task 4/12: Handle OnTripClicked in TripListViewModel
[...implementing...]
Running ./gradlew test --tests "*TripListViewModelTest"...
🟢 GREEN confirmed — all tests pass (including new OnTripClicked test)
✓ Task complete

Working on task 8/12: Update TripListScreen signature
[...implementing...]
✓ Task complete
```

**Output On Completion**

```
## Implementation Complete

**Change:** <change-name>
**Schema:** <schema-name>
**Progress:** 7/7 tasks complete ✓

### Completed This Session
- [x] Task 1
- [x] Task 2
...

All tasks complete! Ready to archive this change.
```

**Output On Pause (Issue Encountered)**

```
## Implementation Paused

**Change:** <change-name>
**Schema:** <schema-name>
**Progress:** 4/7 tasks complete

### Issue Encountered
<description of the issue>

**Options:**
1. <option 1>
2. <option 2>
3. Other approach

What would you like to do?
```

**Guardrails**
- Keep going through tasks until done or blocked
- Always read context files before starting (from the apply instructions output)
- If task is ambiguous, pause and ask before implementing
- If implementation reveals issues, pause and suggest artifact updates
- Keep code changes minimal and scoped to each task
- Update task checkbox immediately after completing each task
- Pause on errors, blockers, or unclear requirements - don't guess
- Use contextFiles from CLI output, don't assume specific file names

**Fluid Workflow Integration**

This skill supports the "actions on a change" model:

- **Can be invoked anytime**: Before all artifacts are done (if tasks exist), after partial implementation, interleaved with other actions
- **Allows artifact updates**: If implementation reveals design issues, suggest updating artifacts - not phase-locked, work fluidly
