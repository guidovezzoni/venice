Please verify and archive the user story: $ARGUMENTS.

This command uses sub-agent orchestration: each verification gate is delegated to a separate sub-agent with a fresh context window, using cheaper models (Sonnet/Haiku) where appropriate. The orchestrator handles user interaction, device gates, and pass/fail decisions between gates.

Sub-agent orchestration is the default execution strategy for this command.

## Blocking gate protocol

Most steps in this command are **blocking gates**. After each gate sub-agent returns, the orchestrator reads the result:
- **PASS** → proceed to next gate
- **FAIL** → STOP immediately, present findings to user, do NOT proceed to subsequent gates
- **NOT_FEASIBLE** → handle per step-specific instructions (usually ask user)

## Device connectivity

### Early reminder (non-blocking)

At the very start of this command — before executing any step — inform the user that a connected Android device (physical or emulator) will be needed later for instrumented tests and on-device verification. **Do not block.** Proceed immediately with the steps.

### Device gate (blocking)

Whenever any step in this command requires a connected Android device (instrumented tests, on-device verification, manual UI checks, etc.):

1. Run `adb devices` to check for a connected device (physical or emulator).
2. If no device is listed:
   a. Ask the user to connect a device — either a physical device via USB with USB debugging enabled, or an emulator.
   b. **BLOCK here. Do NOT continue to subsequent steps or tasks.** Wait for the user to respond confirming the device is available.
   c. Re-run `adb devices` to verify the device appeared. If still not listed, repeat from sub-step a.
3. Only proceed once a device is confirmed connected.

This gate applies everywhere a device is needed — it is not limited to a specific step.

## Orchestrator pre-work

Before spawning any sub-agents, the orchestrator gathers context needed by multiple gates:

1. Run `git diff --name-only main...HEAD` to get the list of files modified by this story.
2. Read the user story file to extract story ID, title, and acceptance criteria.
3. Run `openspec status --json` to get the active change name.

Store this information for inclusion in sub-agent prompts.

## Steps

Follow these steps:

1. **Locate the user story.** Match `$ARGUMENTS` against the user story files by number or partial name. If no match is found, ask the user which user story to verify. Validate the **preconditions for Closing** as defined in @docs/guidelines/guidelines-userstories.md. If they are not met, inform the user and stop.

2. **Run OpenSpec verify (sub-agent).** — BLOCKING GATE

   ```
   Agent(
     description: "OpenSpec verify",
     model: "sonnet",
     prompt: "<constructed prompt>"
   )
   ```

   **Sub-agent prompt:**
   ```
   You are running an OpenSpec verification to check that implementation matches change artefacts.

   ## Change Name
   {CHANGE_NAME}

   ## Task

   Execute the OpenSpec verify command (`/opsx:verify`) to check that the implementation
   matches the change artefacts. Parse and report the results.

   ## Output Format — CRITICAL

   ### RESULT: PASS or FAIL

   PASS if no critical issues are reported.
   FAIL if the verification reports any critical issues.

   ### Summary
   - Total checks performed
   - Issues found (by severity)

   ### Issues (if any)
   - CRITICAL: [list with file references]
   - WARNING: [list]
   - SUGGESTION: [list]
   ```

   **Gate decision:** If RESULT is FAIL, STOP — present the issues clearly to the user and do NOT proceed.

   **Failure handling:** If the sub-agent itself fails (not the verification), retry once with Sonnet.

3. **Review unresolved TODOs (sub-agent).** — BLOCKING GATE

   ```
   Agent(
     description: "TODO scan and classification",
     model: "sonnet",
     prompt: "<constructed prompt>"
   )
   ```

   **Sub-agent prompt:**
   ```
   You are scanning the codebase for unresolved TODO comments and classifying them.

   ## User Story Context
   - Story ID: {STORY_ID}
   - Story title: {STORY_TITLE}
   - Files modified by this story: {MODIFIED_FILES_LIST}

   ## Task

   Scan all files under `app/src/` for TODO comments (// TODO, /* TODO, # TODO).
   For each TODO found, classify it as:

   - **RESOLVE NOW** — directly related to this story, should have been implemented as part
     of this story, OR its stated precondition has been fulfilled by this story's implementation.
     A TODO whose precondition is now met should be classified as RESOLVE NOW regardless of
     which story number it references.
   - **ACKNOWLEDGED** — genuinely unrelated to this story and its precondition is not yet met.

   ## Output Format — CRITICAL

   ### RESULT: PASS or FAIL

   PASS if all TODOs are ACKNOWLEDGED (or none exist).
   FAIL if any TODOs are classified as RESOLVE NOW.

   ### RESOLVE NOW (blocking)
   - File:line — TODO text — reason it must be resolved

   ### ACKNOWLEDGED (non-blocking)
   - File:line — TODO text — reason it's unrelated
   ```

   **Gate decision:** If RESULT is FAIL, STOP — present RESOLVE NOW TODOs to the user and do NOT proceed.

   **Failure handling:** If the sub-agent fails, retry once with Sonnet.

4. **Run security review (sub-agent).** — BLOCKING GATE

   ```
   Agent(
     description: "Security review",
     model: "sonnet",
     prompt: "<constructed prompt>"
   )
   ```

   **Sub-agent prompt:**
   ```
   You are running a security review on the pending changes on the current branch.

   ## Task

   Execute the `/security-review` skill to review all pending changes on the current branch
   for security issues. Use the Skill tool to invoke the "security-review" skill.

   ## Output Format — CRITICAL

   ### RESULT: PASS or FAIL

   PASS if no critical or high-severity findings.
   FAIL if any critical or high-severity findings exist.

   ### Findings (if any)
   - Severity: [CRITICAL/HIGH/MEDIUM/LOW]
   - Description: [what the issue is]
   - Location: [file:line]
   - Recommendation: [how to fix]
   ```

   **Gate decision:** If RESULT is FAIL (critical or high-severity findings), STOP — present findings to the user and do NOT proceed.

   **Failure handling:** If the sub-agent fails, retry once with Sonnet.

5. **Run clean build and static analysis (sub-agent).** — BLOCKING GATE

   ```
   Agent(
     description: "Clean build and static analysis",
     model: "haiku",
     prompt: "<constructed prompt>"
   )
   ```

   **Sub-agent prompt:**
   ```
   You are running a clean build and static analysis check.

   ## Task

   Run the following command and wait for it to complete:
   ```
   ./gradlew clean check
   ```

   Report the result.

   ## Output Format — CRITICAL

   ### RESULT: PASS or FAIL

   PASS if the build succeeds (exit code 0).
   FAIL if the build fails or lint reports errors.

   ### Details
   - Exit code: [0 or non-zero]
   - If FAIL: paste the relevant error output (compilation errors, lint errors)
   - If PASS: note any warnings (non-blocking)
   ```

   **Gate decision:** If RESULT is FAIL, STOP — present the build errors to the user and do NOT proceed.

   **Failure handling:** If the sub-agent fails, retry once. If Haiku, escalate to Sonnet on retry.

6. **Run unit tests (sub-agent).** — BLOCKING GATE

   ```
   Agent(
     description: "Unit tests",
     model: "haiku",
     prompt: "<constructed prompt>"
   )
   ```

   **Sub-agent prompt:**
   ```
   You are running the unit test suite.

   ## Task

   Run the following command and wait for it to complete:
   ```
   ./gradlew test
   ```

   Report the result.

   ## Output Format — CRITICAL

   ### RESULT: PASS or FAIL

   PASS if all tests pass (exit code 0).
   FAIL if any tests fail.

   ### Details
   - Exit code: [0 or non-zero]
   - Total tests run: [count if available from output]
   - If FAIL: list failing test classes and failure messages
   - If PASS: confirm all tests passed
   ```

   **Gate decision:** If RESULT is FAIL, STOP — present the failing tests to the user and do NOT proceed.

   **Failure handling:** If the sub-agent fails, retry once. If Haiku, escalate to Sonnet on retry.

7. **Generate coverage report (sub-agent).** — NON-BLOCKING (informational)

   ```
   Agent(
     description: "Coverage report",
     model: "sonnet",
     prompt: "<constructed prompt>"
   )
   ```

   **Sub-agent prompt:**
   ```
   You are generating a code coverage report for the current state of the branch
   (HEAD), broken down by Kover coverage category.

   ## Task

   1. Generate the coverage report:
      ```
      ./gradlew koverXmlReport
      ```

   2. Parse `app/build/reports/kover/report.xml` and, for each `<counter type="...">`
      element (INSTRUCTION, BRANCH, LINE, METHOD, CLASS), compute:
      `percentage = covered / (covered + missed) * 100`

   ## Output Format — CRITICAL

   ### RESULT: PASS or FAIL

   PASS once the coverage table is produced. FAIL only if the Gradle command itself
   errors out (not if coverage is below target — this step never blocks on coverage,
   it only reports it; the 95% minimum is already enforced separately by
   `koverVerify` as part of `./gradlew check` in step 5).

   ### Coverage Report
   | Category | Coverage |
   |---|---|
   | Instructions | XX.XX% |
   | Branches | XX.XX% |
   | Lines | XX.XX% |
   | Methods | XX.XX% |
   | Classes | XX.XX% |
   ```

   **Gate decision:** Informational only — never blocks subsequent steps.

   **Failure handling:** If the sub-agent fails, retry once with Sonnet. If it still
   fails, proceed without the report and note its absence in the report.

8. **Verify test file coverage (sub-agent).** — BLOCKING GATE

   ```
   Agent(
     description: "Test file coverage check",
     model: "sonnet",
     prompt: "<constructed prompt>"
   )
   ```

   **Sub-agent prompt:**
   ```
   You are verifying that all new/modified source classes have corresponding test files.

   ## Context
   - Story ID: {STORY_ID}
   - Files modified/introduced by this story:
   {MODIFIED_FILES_LIST}

   ## Task

   1. For each new or modified use case, ViewModel, or repository class under `app/src/main/`:
      - Check that a corresponding unit test file exists in `app/src/test/`
      - Naming convention: `FooClass.kt` → `FooClassTest.kt`

   2. For each new or modified screen composable under `app/src/main/`:
      - Check that a corresponding Compose UI test file exists in `app/src/androidTest/`

   ## Output Format — CRITICAL

   ### RESULT: PASS or FAIL

   PASS if all classes have corresponding test files.
   FAIL if any are missing.

   ### Coverage Check
   | Source class | Test file | Status |
   |---|---|---|
   | path/to/Foo.kt | path/to/FooTest.kt | FOUND/MISSING |

   ### Missing Test Files (if any)
   - [list of source files without corresponding tests]
   ```

   **Gate decision:** If RESULT is FAIL, STOP — present the list of missing test files to the user and do NOT proceed.

   **Failure handling:** If the sub-agent fails, retry once with Sonnet.

9. **Verify Compose preview coverage (sub-agent).** — BLOCKING GATE

   ```
   Agent(
     description: "Compose preview coverage check",
     model: "sonnet",
     prompt: "<constructed prompt>"
   )
   ```

   **Sub-agent prompt:**
   ```
   You are verifying that all screen composables have adequate preview coverage.

   ## Context
   - Files modified/introduced by this story:
   {MODIFIED_FILES_LIST}

   ## Task

   For each screen composable modified or introduced by this story:
   1. Check that a `@Preview` function exists for the stateless composable
   2. Parse the screen's `UiState` data class to identify all fields
   3. Verify that every field appears with a non-default value in at least one preview

   ## Output Format — CRITICAL

   ### RESULT: PASS or FAIL

   PASS if all composables have previews and all UiState fields are covered.
   FAIL if any are missing.

   ### Preview Coverage
   | Composable | @Preview exists | UiState fields covered |
   |---|---|---|
   | FooScreen | YES/NO | 5/5 or 3/5 (missing: field1, field2) |

   ### Missing (if any)
   - Composables without previews: [list]
   - UiState fields without preview coverage: [list]
   ```

   **Gate decision:** If RESULT is FAIL, STOP — present the list of missing previews/fields to the user and do NOT proceed.

   **Failure handling:** If the sub-agent fails, retry once with Sonnet.

10. **Run on-device tests (orchestrator + sub-agent).** — BLOCKING GATE

   Apply the **device gate** (see above) BEFORE spawning the sub-agent. Only proceed once a device is confirmed connected.

   ```
   Agent(
     description: "On-device tests",
     model: "haiku",
     prompt: "<constructed prompt>"
   )
   ```

   **Sub-agent prompt:**
   ```
   You are running on-device tests and exercising the app via adb.

   ## Task

   1. Run instrumented tests:
      ```
      ./gradlew connectedDebugAndroidTest
      ```

   2. If tests pass, install the app:
      ```
      ./gradlew installDebug
      ```

   3. Launch the app:
      ```
      adb shell am start -n com.guidovezzoni.venice/.ui.MainActivity
      ```

   4. Exercise the feature under test via adb:
      - Dump UI hierarchy and identify targets
      - Perform up to 3 interactions to verify the feature
      - TIME-BOX: Maximum 3 interactions. If exercise fails or requires complex
        multi-step setup, STOP immediately.

   ## Feature to Verify
   {DESCRIPTION_OF_WHAT_TO_CHECK — derived from user story acceptance criteria}

   ## Output Format — CRITICAL

   ### RESULT: PASS or FAIL or NOT_FEASIBLE

   PASS if instrumented tests pass AND app exercise confirmed the feature works.
   FAIL if instrumented tests fail OR app exercise shows a defect.
   NOT_FEASIBLE if adb-based exercise cannot verify the feature (too complex, requires
   multi-step setup that exceeds 3 interactions).

   ### Instrumented Tests
   - Result: PASS/FAIL
   - Test count: [N]
   - Failures (if any): [list]

   ### App Exercise
   - Method: adb (or NOT_FEASIBLE)
   - Interactions performed: [list]
   - Verification: [what was confirmed or why it's not feasible]
   ```

   **Gate decision:**
   - If RESULT is PASS: proceed to next step.
   - If RESULT is FAIL: STOP — present failures to user and do NOT proceed.
   - If RESULT is NOT_FEASIBLE: ask the user to perform the manual verification. Describe what to check. **BLOCK** — wait for the user to confirm the result. If the user reports a failure, STOP.

   **Failure handling:** If the sub-agent fails, retry once. If Haiku, escalate to Sonnet on retry.

11. **Verify the Definition of Done (sub-agent).** — BLOCKING GATE

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
    {PASTE ACCEPTANCE CRITERIA / DEFINITION OF DONE SECTION FROM USER STORY}

    ## Files Modified by This Story
    {MODIFIED_FILES_LIST}

    ## Task

    For each item in the "Acceptance Criteria" or "Definition of Done" section:
    1. Search the codebase (source files, tests, configuration) for evidence that the
       criterion is met
    2. Report each item as PASS or FAIL with a brief justification referencing specific
       files or tests

    ## Output Format — CRITICAL

    ### RESULT: PASS or FAIL

    PASS if all items are confirmed met.
    FAIL if any item cannot be confirmed.

    ### Definition of Done Checklist
    | # | Criterion | Status | Justification |
    |---|---|---|---|
    | 1 | [criterion text] | PASS/FAIL | [evidence: file path, test name, etc.] |
    | 2 | ... | ... | ... |
    ```

    **Gate decision:** If RESULT is FAIL, STOP — present the failing criteria to the user and do NOT proceed.

    **Failure handling:** If the sub-agent fails, retry once with Sonnet.

12. **Close the user story (sub-agent).**

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
    - Use `git mv` (not plain mv) so git tracks any renames
    - Follow the exact Closing procedure from the guidelines
    - Update the index link to match the new filename

    ## When Done
    Report:
    1. What operation was performed
    2. New file path
    3. Confirm the index was updated
    ```

    **Verification:** Confirm the user story file is at its new path and the index is updated.

    **Failure handling:** Retry once. If Haiku, escalate to Sonnet on retry.

13. **Sync delta specs (sub-agent).**

    ```
    Agent(
      description: "Sync delta specs",
      model: "sonnet",
      prompt: "<constructed prompt>"
    )
    ```

    **Sub-agent prompt:**
    ```
    You are syncing delta specs from an OpenSpec change to main specs.

    ## Change Name
    {CHANGE_NAME}

    ## Task

    Execute the OpenSpec sync command (`/opsx:sync`) to merge any delta specs from this
    change into the main specs. Ensure that all file moves use `git mv` so that Git
    tracks the renames.

    ## When Done
    Report:
    1. Which capabilities were synced (or "no delta specs to sync")
    2. What changes were made (added/modified/removed)
    3. Any issues encountered
    ```

    **Verification:** Confirm the sync completed without errors.

    **Failure handling:** Retry once with Sonnet.

14. **Archive the OpenSpec change (sub-agent).**

    ```
    Agent(
      description: "Archive change",
      model: "haiku",
      prompt: "<constructed prompt>"
    )
    ```

    **Sub-agent prompt:**
    ```
    You are archiving a completed OpenSpec change.

    ## Change Name
    {CHANGE_NAME}

    ## Task

    Execute the OpenSpec archive command (`/opsx:archive`) to finalise and archive the
    completed change artefacts. Ensure that all file moves use `git mv` so that Git
    tracks the renames.

    ## When Done
    Report:
    1. Archive location
    2. Confirm the move was successful
    3. Any issues encountered
    ```

    **Verification:** Confirm the change directory has been moved to the archive.

    **Failure handling:** Retry once. If Haiku, escalate to Sonnet on retry.

15. **Verify README.md and AGENTS.md are in sync (sub-agent).**

    ```
    Agent(
      description: "Verify docs sync",
      model: "sonnet",
      prompt: "<constructed prompt>"
    )
    ```

    **Sub-agent prompt:**
    ```
    You are checking whether README.md and AGENTS.md accurately reflect the current
    codebase state after a completed user story.

    ## Context
    - Change name: {CHANGE_NAME}
    - Story title: {STORY_TITLE}
    - Summary of changes: {BRIEF_SUMMARY_OF_WHAT_THE_STORY_DELIVERED}

    ## Task

    1. Read README.md
    2. Read AGENTS.md (NOT CLAUDE.md — it may be a symlink)
    3. Check whether the changes delivered by this story affect any section:
       - Feature list, architecture table, tech stack, build instructions
       - Project overview, folder structure, workflow descriptions
       - Current status section
    4. If any section is outdated or incomplete, update it
    5. If everything is accurate, note that the check passed

    ## Important
    - Do NOT write through symlinks. If CLAUDE.md is a symlink to AGENTS.md,
      edit AGENTS.md directly.
    - Only update sections that are actually affected by this story's changes

    ## When Done
    Report:
    1. README.md: in sync / updated (what changed)
    2. AGENTS.md: in sync / updated (what changed)
    ```

    **Verification:** Confirm README.md and AGENTS.md are up to date.

    **Failure handling:** Retry once with Sonnet.

16. **Add a report (sub-agent).**

    ```
    Agent(
      description: "Add verification report",
      model: "haiku",
      prompt: "<constructed prompt>"
    )
    ```

    **Sub-agent prompt:**
    ```
    You are generating the final verification and archive report section.

    ## Task

    Append a verification and archive section to the report for this user story.

    ## Report Data
    - Date: {ISO_DATE}
    - OpenSpec verify: {RESULT_FROM_STEP_2}
    - TODO scan: {RESULT_FROM_STEP_3}
    - Security review: {RESULT_FROM_STEP_4}
    - Clean build: {RESULT_FROM_STEP_5}
    - Unit tests: {RESULT_FROM_STEP_6}
    - Coverage report table: {COVERAGE_TABLE_FROM_STEP_7}
    - Test file coverage: {RESULT_FROM_STEP_8}
    - Compose preview coverage: {RESULT_FROM_STEP_9}
    - On-device tests: {RESULT_FROM_STEP_10}
    - Definition of Done: {RESULT_FROM_STEP_11}
    - Archive location: {RESULT_FROM_STEP_14}
    - Spec sync status: {RESULT_FROM_STEP_13}
    - README/AGENTS sync: {RESULT_FROM_STEP_15}
    - Final outcome: PASSED
    - Renamed filename: {NEW_STORY_FILENAME}

    ## Guidelines
    Read the report guidelines at: docs/guidelines/guidelines-reports.md

    ## Instructions
    The section should summarise all the data above in a structured format following
    the report guidelines. Include each gate's PASS/FAIL status with details.

    Render the Coverage Report table from step 7 as an HTML `<table>` inside this
    section, using the existing `table`/`th`/`td` styles from the report skeleton —
    columns: Category, Coverage. Do this even if some gates failed, as long as the
    coverage data is available.

    ## When Done
    Report:
    1. Confirm the section was appended
    2. Any issues encountered
    ```

    **Verification:** Confirm the report file contains the new verification section.

    **Failure handling:** Retry once. If still failing, the orchestrator generates the report inline.

17. **Display the summary.** Output the summary on screen so the user can see what was verified and archived, including the Coverage Report table from step 7 (Category | Coverage), rendered as a markdown table.

18. **Suggest a commit message.** Suggest a commit message following @docs/guidelines/guidelines-git.md.
