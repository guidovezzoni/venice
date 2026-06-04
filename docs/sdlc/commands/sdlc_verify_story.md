Please verify the user story: $ARGUMENTS.

Follow these steps:

1. **Locate the user story.** Match `$ARGUMENTS` against the user story files by number or partial name. If no match is found, ask the user which user story to verify. Validate the **preconditions for Closing** as defined in @docs/guidelines/guidelines-userstories.md. If they are not met, inform the user and stop.

2. **Run OpenSpec verify.** Execute the OpenSpec verify command (`/opsx:verify`) to check that the implementation matches the change artefacts. If the verification reports any issues, stop here: present the issues clearly to the user and do **not** proceed to the next steps.

3. **Review unresolved TODOs.** Scan all source files under `app/src/` for TODO comments (`// TODO`, `/* TODO`, `# TODO`). For each TODO found:
   - Determine if it is **related to the current story** (references the story number, touches a feature area modified by this story, or was introduced/should have been resolved by this story).
   - Additionally, check whether the **precondition or blocker described in the TODO has been satisfied by this story's implementation** (e.g. a TODO that says "do X once table Y exists" becomes actionable if this story created table Y). A TODO whose precondition is now met should be classified as RESOLVE NOW regardless of which story number it references.
   - Classify each as:
      - **RESOLVE NOW** — directly related to this story, should have been implemented as part of this story, **or its stated precondition has been fulfilled by this story**. These block verification.
      - **ACKNOWLEDGED** — genuinely unrelated to this story and its precondition is not yet met. These are listed for awareness but do not block.
   - If any TODOs are classified as RESOLVE NOW, stop here: present them to the user and do **not** proceed to the next steps.
   - If all TODOs are ACKNOWLEDGED (or none exist), proceed.

4. **Run security review.** Execute the `/security-review` command to review pending changes on the current branch for security issues. If the review reports any critical or high-severity findings, stop here: present them to the user and do **not** proceed to the next steps.

5. **Run on-device tests.** Follow the on-device testing procedure in @docs/guidelines/guidelines-process.md:
   - Check for a connected device with `adb devices`.
   - If connected: run `./gradlew connectedDebugAndroidTest`, then install the app (`./gradlew installDebug`) and launch it with `adb shell am start`.
   - **Time-box adb UI exercise to 3 interactions.** If manual adb-based UI exercise (tap/input/screenshot) fails or requires complex multi-step setup (e.g. creating test data through multiple dialogs), stop immediately — do not loop on retries or attempt to fix adb input issues. Record the instrumented test results as the on-device verification and note that manual adb exercise was not feasible.
   - If no device is connected: ask the user to connect one or perform the manual verification themselves.
   - **Do not block remaining steps** on manual adb exercise. Instrumented Compose UI tests on a physical device are sufficient to proceed. Always continue to step 6 after the instrumented tests complete.

6. **Verify the Definition of Done.** Read the user story file and identify the "Acceptance Criteria" or "Definition of Done" section. For each item listed:
   - Check the codebase (source files, tests, configuration) to confirm the criterion is met.
   - Report each item as PASS or FAIL with a brief justification.
     If any item is marked FAIL, stop here: present a summary to the user and do **not** proceed to the rename step.

7. **Close the user story.** Once all verifications pass, perform the **Closing** operation as defined in @docs/guidelines/guidelines-userstories.md.

8. **Add a report.** Append a verification section to the report for this user story following @docs/guidelines/guidelines-reports.md. The section should summarise: date of verification, OpenSpec verify result (pass/fail summary), TODO scan result (list of ACKNOWLEDGED TODOs, or "none found"), security review result (pass/fail with summary of findings, if any), on-device test results (method used — agent via adb or user-confirmed — and outcomes), Definition of Done checklist with each item's PASS/FAIL status and justification, and final outcome (PASSED / FAILED) with the renamed filename.

9. **Display the summary.** Output the same summary on screen so the user can see what was verified.

10. **Suggest a commit message.** Suggest a commit message following @docs/guidelines/guidelines-git.md.
