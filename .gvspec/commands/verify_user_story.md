Please verify the user story: $ARGUMENTS.

Follow these steps:

1. **Locate the user story** in `@docs/userstories/`. Match `$ARGUMENTS` against the file names (by number or partial name). If no match is found, ask the user which user story to verify. If the file already has the `-DONE` suffix, inform the user and stop.

2. **Run OpenSpec verify.** Execute the OpenSpec verify command (`/opsx:verify`) to check that the implementation matches the change artefacts. If the verification reports any issues, stop here: present the issues clearly to the user and do **not** proceed to the next steps.

3. **Review unresolved TODOs.** Scan all source files under `app/src/` for TODO comments (`// TODO`, `/* TODO`, `# TODO`). For each TODO found:
   - Determine if it is **related to the current story** (references the story number, touches a feature area modified by this story, or was introduced/should have been resolved by this story).
   - Classify each as:
     - **RESOLVE NOW** — directly related to this story and should have been implemented. These block verification.
     - **ACKNOWLEDGED** — unrelated to this story or explicitly deferred to a future story. These are listed for awareness but do not block.
   - If any TODOs are classified as RESOLVE NOW, stop here: present them to the user and do **not** proceed to the next steps.
   - If all TODOs are ACKNOWLEDGED (or none exist), proceed.

4. **Verify the Definition of Done.** Read the user story file and identify the "Acceptance Criteria" or "Definition of Done" section. For each item listed:
   - Check the codebase (source files, tests, configuration) to confirm the criterion is met.
   - Report each item as PASS or FAIL with a brief justification.
   If any item is marked FAIL, stop here: present a summary to the user and do **not** proceed to the rename step.

5. **Rename the user story file.** Once all verifications pass:
   - If the filename contains `-WIP`, remove it.
   - Append `-DONE` before the `.md` extension.
   - For example: `1.4.2-Create-leg-WIP.md` → `1.4.2-Create-leg-DONE.md`, or `1.4.2-Create-leg.md` → `1.4.2-Create-leg-DONE.md`.
   - Update any references to the old filename in `@docs/userstories/index.md` if it exists.
   Use `git mv` so the rename is tracked in version control.

6. **Report the result.** Summarise what was verified and the new filename. Suggest a commit message with the prefix `[Suggested Commit Message]`.
