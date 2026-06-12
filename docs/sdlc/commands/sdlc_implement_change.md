Please apply the changes for the current OpenSpec change and resolve any outstanding TODOs.

This command uses sub-agent orchestration: each task section is delegated to a separate sub-agent with a fresh context window, using cheaper models (Sonnet/Haiku) where appropriate. This prevents task checkboxes from being forgotten in long sessions and reduces cost.

Sub-agent orchestration is the default execution strategy for this command.

## Device connectivity

### Early reminder (non-blocking)

At the very start of this command — before executing any task — inform the user that a connected Android device (physical or emulator) will be needed later for instrumented tests and on-device verification. **Do not block.** Proceed immediately with the tasks.

### Device gate (blocking)

Whenever any step in this command — or any task in the task list — requires a connected Android device (instrumented tests, on-device verification, manual UI checks, etc.):

1. Run `adb devices` to check for a connected device (physical or emulator).
2. If no device is listed:
   a. Ask the user to connect a device — either a physical device via USB with USB debugging enabled, or an emulator.
   b. **BLOCK here. Do NOT continue to subsequent steps or tasks.** Wait for the user to respond confirming the device is available.
   c. Re-run `adb devices` to verify the device appeared. If still not listed, repeat from sub-step a.
3. Only proceed once a device is confirmed connected.

This gate applies everywhere a device is needed — it is not limited to a specific step.

## Steps

Follow these steps:

1. **Apply the OpenSpec change using sub-agent orchestration.**

   Instead of running all tasks in a single session, orchestrate sub-agents to implement each task section independently. Each sub-agent gets a fresh context window, implements its assigned tasks, checks them off, and returns.

   ### 1a. Identify the change

   Run `openspec status --json` to find the active change. Then run:
   ```bash
   openspec instructions apply --change "<name>" --json
   ```
   Parse the JSON to get:
   - `contextFiles`: paths to proposal, design, specs, and tasks files
   - Task list with current status
   - Progress (total, complete, remaining)

   If all tasks are already complete, skip to step 2.

   ### 1b. Read and parse the task list

   Read the tasks file. Parse it into sections delimited by `## N.` headings. For each section, record:
   - Section number and title
   - List of task IDs and descriptions (the `- [ ]` / `- [x]` lines)
   - Total task count in the section
   - How many are already checked `[x]` (for resume support)

   Skip any section where all tasks are already `[x]`.

   ### 1c. Classify each section and assign a model

   For each section with pending tasks, determine the model tier by examining the task descriptions:

   | Section pattern | Model |
   |---|---|
   | Contains tasks matching "Write test:" or "Add test:" paired with "Implement:" tasks | **Sonnet** (`sonnet`) |
   | ALL tasks are mechanical — matching patterns like "Pass", "Wire", "Wrap existing", "Disable" | **Haiku** (`haiku`) |
   | ALL tasks are Compose previews or read-only verification ("Add preview", "Verify existing") | **Haiku** (`haiku`) |
   | ALL tasks are command execution ("Run ./gradlew", "Update via /opsx:sync") | **Haiku** (`haiku`) |
   | Unclear or mixed task types | **Sonnet** (`sonnet`) — safe default |

   Display the execution plan before proceeding:
   ```
   ## Execution Plan
   
   Section 0: Minimum Duration Utility (3 tasks) → Sonnet
   Section 1: ViewModel isLoading Wrapping (17 tasks) → Sonnet
   Section 5: Screen Wiring TripDetailScreen (4 tasks) → Haiku
   ...
   ```

   Proceed without waiting for user confirmation.

   ### 1d. Execute sections sequentially

   Sections MUST be executed in order — later sections depend on code produced by earlier ones.

   For each section with pending tasks:

   **i. Check for device gate.** If any task in this section mentions on-device verification, instrumented tests, or manual UI checks, apply the **device gate** (see above) BEFORE spawning the sub-agent.

   **ii. Build the sub-agent prompt.** Construct a self-contained prompt using the template below.

   **iii. Spawn the sub-agent.** Use the `Agent` tool with the assigned `model` parameter:
   ```
   Agent(
     description: "Section N: <title>",
     model: "<sonnet|haiku>",
     prompt: "<constructed prompt>"
   )
   ```

   **iv. Verify completion.** After the sub-agent returns:
   - Read the tasks file
   - Count `- [x]` lines in this section
   - Compare against the expected total (previously pending tasks in this section)

   **v. Handle verification results:**
   - **All tasks checked**: Display `Section N complete (N/N tasks)` and proceed to next section
   - **Some tasks unchecked but sub-agent reported no errors**: Spawn a Haiku sub-agent with a narrow prompt to check off the missing boxes (the implementation exists but the checkboxes were missed)
   - **Sub-agent reported an error or blocker**: Retry the section once. If the original model was Haiku, upgrade to Sonnet for the retry. If still failing after retry, PAUSE and report the issue to the user

   ### 1e. Final verification

   After all sections are processed, read the tasks file one final time. Confirm every task is `[x]`. Report overall progress:
   ```
   ## All Sections Complete
   
   Progress: N/N tasks complete
   Sections processed: M (X with Sonnet, Y with Haiku)
   ```

   ### Sub-agent prompt template

   Use this template to construct the prompt for each sub-agent. Replace placeholders with actual values.

   ```
   You are implementing section {SECTION_NUMBER} ("{SECTION_TITLE}") of an OpenSpec change
   called "{CHANGE_NAME}" in an Android project.

   ## Your Tasks

   {PASTE THE EXACT TASK LINES FOR THIS SECTION, including the `- [ ]` prefix}

   ## Context Files

   Read these files before starting — they contain the design and specifications:
   {LIST EACH contextFile PATH, one per line, with a brief label like "- Design: <path>"}

   ## Coding Guidelines

   Read `docs/guidelines/guidelines-android.md` for code style, naming conventions,
   and testing patterns. Follow them strictly.

   {IF BDD SECTION, INCLUDE THE BDD RULES BLOCK BELOW}

   ## Task Completion — CRITICAL

   After completing EACH task, you MUST immediately update the tasks file at:
     {TASKS_FILE_PATH}

   Change the task's checkbox from `- [ ]` to `- [x]`.
   Do this IMMEDIATELY after each task, BEFORE moving to the next task.
   This is a hard requirement — your work is not considered complete without it.

   ## When Done

   Report:
   1. Which tasks you completed (list task IDs)
   2. Any issues or blockers encountered
   3. Confirm all checkboxes in your section are updated
   ```

   ### BDD rules block (include only for BDD sections)

   Include this block in the sub-agent prompt when the section contains test + implementation task pairs:

   ```
   ## BDD Execution Discipline

   **A. Test task** (description starts with "Write test:" or "Add test:"):

   1. Write the test code
   2. Run the specific test class: `./gradlew test --tests "*<TestClass>"`
      (derive the class name from the task description)
   3. **Verify RED**: Confirm the test fails (compilation error or assertion failure both count as red)
      - If the test unexpectedly passes: STOP and report this — the test may be trivial or incorrect
      - If tests fail for unrelated reasons: STOP and report the pre-existing failure
   4. After confirming RED and marking the task complete, note: `RED confirmed`

   **B. Implementation task following a test task**:

   1. Write the implementation code (minimal — just enough to make the test pass)
   2. Run the same test class from the preceding test task
   3. **Verify GREEN**: Confirm the test now passes
      - If this is an intermediate implementation task and the test still fails: note
        "Tests not yet green, continuing" and proceed
      - If this is the last implementation task in this section and the test still fails:
        iterate on the implementation until it passes, or STOP and report if stuck
   4. After confirming GREEN and marking the task complete, note: `GREEN confirmed`

   **C. Non-test, non-implementation task**: Just implement as normal.

   **Task type detection**:
   - "Test task": description contains "Write test", "Add test", or references a test class
   - "Implementation after test": the immediately preceding completed task was a test task
   - All others: non-test tasks
   ```

2. **Review unresolved TODOs.** Scan all source files under `app/src/` for TODO comments (`// TODO`, `/* TODO`, `# TODO`). For each TODO found:
   - Determine if it is **related to the current story** (references the story number, touches a feature area modified by this story, or was introduced/should have been resolved by this story).
   - Additionally, check whether the **precondition or blocker described in the TODO has been satisfied by this story's implementation** (e.g. a TODO that says "do X once table Y exists" becomes actionable if this story created table Y). A TODO whose precondition is now met should be classified as RESOLVE NOW regardless of which story number it references.
   - Classify each as:
      - **RESOLVE NOW** — directly related to this story, should have been implemented as part of this story, **or its stated precondition has been fulfilled by this story**. These block completion.
      - **ACKNOWLEDGED** — genuinely unrelated to this story and its precondition is not yet met. These are listed for awareness but do not block.

3. **If any TODOs are classified as RESOLVE NOW**, do the following:
   1. Present the RESOLVE NOW TODOs to the user for awareness.
   2. **Update the OpenSpec artefacts.** Modify the relevant delta specs, design document, and task list generated by OpenSpec to include the work required to resolve these TODOs.
   3. **Re-apply the changes.** Re-run the sub-agent orchestration from step 1 to implement the updated tasks (it will automatically skip already-completed sections).
   4. **Re-check TODOs.** Repeat from step 2 to verify that no RESOLVE NOW TODOs remain. Continue this loop until all TODOs are either resolved or classified as ACKNOWLEDGED.

4. **Run Compose UI tests on a connected device.** Once no RESOLVE NOW TODOs remain, apply the **device gate** (see above), then run the instrumented tests:
   ```
   ./gradlew connectedDebugAndroidTest
   ```
   **Run this autonomously — do not ask the user to run it.** Once the device is confirmed connected, execute the command yourself and report the results.
   If any tests fail:
   1. Present the failures to the user for awareness.
   2. Fix the failing tests or the production code as appropriate.
   3. Re-run the tests to confirm the fixes.
   4. Repeat until all tests pass.

5. **Run security review.** Once all tests pass, execute the `/security-review` command to review pending changes on the current branch for security issues. If the review reports any findings:
   1. Present the findings to the user for awareness.
   2. Fix all reported issues in the codebase.
   3. Re-run `/security-review` to confirm the fixes are effective.
   4. Repeat this cycle until the security review comes back clean.

6. **Update README.md if required.** Read `README.md` and check whether the changes delivered by this story affect any section of the file (e.g. feature list, architecture table, tech stack, build instructions). If any section is now outdated or incomplete, update it to reflect the current state of the project. If everything is already accurate, skip this step.

7. **Update AGENTS.md if required.** Read `AGENTS.md` and check whether the changes delivered by this story affect any section of the file (e.g. project overview, folder structure, workflow descriptions). If any section is now outdated or incomplete, update it to reflect the current state of the project. If everything is already accurate, skip this step.

8. **Add a report.** Once the security review is clean, append a section to the report for this user story following @docs/guidelines/guidelines-reports.md. The section should summarise: what was applied, how many iteration cycles were needed, any ACKNOWLEDGED TODOs that remain for future stories, security review result (pass/fail with number of fix cycles needed and summary of findings, if any), and whether README.md and AGENTS.md were updated.

9. **Display the summary.** Output the same summary on screen so the user can see what was done.

10. **Suggest a commit message.** Suggest a commit message following @docs/guidelines/guidelines-git.md.
