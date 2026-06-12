Please verify and archive the user story: $ARGUMENTS.

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

## Steps

Follow these steps:

1. **Locate the user story.** Match `$ARGUMENTS` against the user story files by number or partial name. If no match is found, ask the user which user story to verify. Validate the **preconditions for Closing** as defined in @docs/guidelines/guidelines-userstories.md. If they are not met, inform the user and stop.

2. **Run OpenSpec verify.** Execute the OpenSpec verify command (`/opsx:verify`) to check that the implementation matches the change artefacts. If the verification reports any issues, stop here: present the issues clearly to the user and do **not** proceed to the next steps. If no issues are reported, proceed immediately to step 3.

3. **Review unresolved TODOs.** Scan all source files under `app/src/` for TODO comments (`// TODO`, `/* TODO`, `# TODO`). For each TODO found:
   - Determine if it is **related to the current story** (references the story number, touches a feature area modified by this story, or was introduced/should have been resolved by this story).
   - Additionally, check whether the **precondition or blocker described in the TODO has been satisfied by this story's implementation** (e.g. a TODO that says "do X once table Y exists" becomes actionable if this story created table Y). A TODO whose precondition is now met should be classified as RESOLVE NOW regardless of which story number it references.
   - Classify each as:
      - **RESOLVE NOW** — directly related to this story, should have been implemented as part of this story, **or its stated precondition has been fulfilled by this story**. These block verification.
      - **ACKNOWLEDGED** — genuinely unrelated to this story and its precondition is not yet met. These are listed for awareness but do not block.
   - If any TODOs are classified as RESOLVE NOW, stop here: present them to the user and do **not** proceed to the next steps.
   - If all TODOs are ACKNOWLEDGED (or none exist), proceed immediately to step 4.

4. **Run security review.** Execute the `/security-review` command to review pending changes on the current branch for security issues. If the review reports any critical or high-severity findings, stop here: present them to the user and do **not** proceed to the next steps. If no critical or high-severity findings are reported, proceed immediately to step 5.

5. **Run clean build and static analysis.** Run `./gradlew clean check`. This catches compilation errors, lint warnings, unused imports, and deprecations. If the build fails or lint reports errors, stop here: present the issues to the user and do **not** proceed to the next steps. If the build succeeds, proceed immediately to step 6.

6. **Run unit tests.** Run `./gradlew test`. If any tests fail, stop here: present the failures to the user and do **not** proceed to the next steps. If all tests pass, proceed immediately to step 7.

7. **Verify test file coverage.** For each new source class introduced or modified by this story (use cases, ViewModels, repositories), check that a corresponding unit test file exists in `app/src/test/`. For each screen composable introduced or modified by this story, check that a corresponding Compose UI test file exists in `app/src/androidTest/`. List any missing test files. If any are missing, stop here: present the list to the user and do **not** proceed to the next steps. If all test files exist, proceed immediately to step 8.

8. **Verify Compose preview coverage.** For each screen composable modified or introduced by this story, check that:
   - A `@Preview` function exists for the stateless composable.
   - Every field of the screen's `UiState` appears with a non-default value in at least one preview.
   - List any composables missing previews and any `UiState` fields without preview coverage. If any are missing, stop here: present the list to the user and do **not** proceed to the next steps. If all previews and fields are covered, proceed immediately to step 9.

9. **Run on-device tests.** Apply the **device gate** (see above), then:
   - Run `./gradlew connectedDebugAndroidTest`. If any tests fail, stop here and present the failures.
   - Install the app (`./gradlew installDebug`) and launch it with `adb shell am start`.
   - **Time-box adb UI exercise to 3 interactions.** If manual adb-based UI exercise (tap/input/screenshot) fails or requires complex multi-step setup (e.g. creating test data through multiple dialogs), stop immediately — do not loop on retries or attempt to fix adb input issues.
   - **If adb exercise is not feasible**, ask the user to perform the manual verification and describe what to check. **BLOCK here** — wait for the user to confirm the result before proceeding. If the user reports a failure, stop and present it.
   - Only proceed to step 10 once both instrumented tests and manual verification (agent or user) have passed.

10. **Verify the Definition of Done.** Read the user story file and identify the "Acceptance Criteria" or "Definition of Done" section. For each item listed:
   - Check the codebase (source files, tests, configuration) to confirm the criterion is met.
   - Report each item as PASS or FAIL with a brief justification.
   - If any item is marked FAIL, stop here: present a summary to the user and do **not** proceed to the next steps.
   - If all items pass, proceed immediately to step 11.

11. **Close the user story.** Once all verifications pass, perform the **Closing** operation as defined in @docs/guidelines/guidelines-userstories.md.

12. **Sync delta specs.** Execute the OpenSpec sync command (`/opsx:sync`) to merge any delta specs from this change into the main specs. Ensure that all file moves use `git mv` so that Git tracks the renames.

13. **Archive the OpenSpec change.** Execute the OpenSpec archive command (`/opsx:archive`) to finalise and archive the completed change artefacts. Ensure that all file moves use `git mv` so that Git tracks the renames.

14. **Verify README.md and AGENTS.md are in sync.** Read `README.md` and `AGENTS.md` and verify that they accurately reflect the current state of the codebase and specs after the archived change. If any section is outdated or incomplete, flag it to the user and update it. If everything is already accurate, note that the check passed.

15. **Add a report.** Append a verification and archive section to the report for this user story following @docs/guidelines/guidelines-reports.md. The section should summarise: date of verification, OpenSpec verify result (pass/fail summary), TODO scan result (list of ACKNOWLEDGED TODOs, or "none found"), security review result (pass/fail with summary of findings, if any), clean build and static analysis result (pass/fail with warning count), unit test result (pass/fail with test count), test file coverage result (pass / list of missing files), Compose preview coverage result (pass / list of uncovered fields), on-device test results (method used — agent via adb or user-confirmed — and outcomes), Definition of Done checklist with each item's PASS/FAIL status and justification, archive location, spec sync status (synced / skipped / no delta specs), README.md and AGENTS.md sync check result (in sync / updated), and final outcome (PASSED / FAILED) with the renamed filename.

16. **Display the summary.** Output the same summary on screen so the user can see what was verified and archived.

17. **Suggest a commit message.** Suggest a commit message following @docs/guidelines/guidelines-git.md.
