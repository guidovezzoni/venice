Please open the user story for development: $ARGUMENTS.

This command uses sub-agent orchestration: self-contained steps are delegated to separate sub-agents with fresh context windows, using cheaper models (Sonnet/Haiku) where appropriate. Interactive steps (git operations, user questions) remain with the orchestrator.

Sub-agent orchestration is the default execution strategy for this command.

## Steps

Follow these steps:

1. **Ensure main is up-to-date.** If the current branch is not main, check for active changes and inform the user about the current branch and changes, then ask what they want to do: DO NOT MAKE ASSUMPTIONS and DO NOT DELETE DATA. Once on main, fetch and pull the latest changes. If the pull fails or there are conflicts, inform the user and ask how to proceed: DO NOT MAKE ASSUMPTIONS.

2. **Resolve the user story argument.** If `$ARGUMENTS` is empty or not provided, identify the **Next User Story** as defined in @docs/guidelines/guidelines-userstories.md. Inform the user which story was auto-selected. If no actionable story exists, inform the user and stop.

3. **Locate the user story.** Match the argument against the user story files by number or partial name. If no match is found, ask the user which user story to open. Validate the **preconditions for Opening** as defined in @docs/guidelines/guidelines-userstories.md. If they are not met, inform the user and stop.

4. **Create a feature branch.** Using the resolved user story reference:
   1. The new branch should live under the "feature" folder.
   2. The new branch should start with the ticket number or reference of the user story.

5. **Open the user story (sub-agent).** Spawn a sub-agent to perform the **Opening** operation.

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
   - Use `git mv` (not plain mv) so git tracks any renames
   - Follow the exact Opening procedure from the guidelines
   - Update the index link to match the new filename

   ## When Done
   Report:
   1. What operation was performed (rename, status change, etc.)
   2. Confirm the index was updated
   3. Any issues encountered
   ```

   **Verification:** After the sub-agent returns, confirm the user story file is in the expected state and the index is updated.

   **Failure handling:** If the sub-agent fails, retry once with Sonnet. If still failing, perform the operation inline.

6. **Refine the user story (sub-agent).** Spawn a sub-agent to analyse and refine the user story.

   ```
   Agent(
     description: "Refine user story",
     model: "opus",
     prompt: "<constructed prompt>"
   )
   ```

   **Sub-agent prompt:**
   ```
   You are an expert Product Manager, Business Analyst, with a strong engineering background,
   and a special expertise in GDPR, sensitive information, and security.

   ## Task

   Analyse and refine the user story at: {USER_STORY_FILE_PATH}

   ## Context Files
   - User story: {USER_STORY_FILE_PATH}
   - Android guidelines: docs/guidelines/guidelines-android.md
   - Process guidelines: docs/guidelines/guidelines-process.md

   ## Refinement Requirements

   Analyse the user story and enrich it so it is fully defined. The refined story must include:
   1. A full description of the functionality
   2. A comprehensive list of fields to be updated
   3. The structure and URLs of the necessary endpoints
   4. The files to be modified according to the architecture and best practices
   5. How to create Unit Tests
   6. How to update any relevant documentation
   7. Security potential issues and mitigations
   8. Performance potential issues and mitigations
   9. GDPR and sensitive information potential issues and mitigations
   10. Other non-functional requirement concerns and mitigations
   11. The steps required for the task to be considered complete

   ## Critical Constraint
   Stay at the "what" level, not the "how." Focus on requirements, acceptance criteria,
   data models, and external-facing behaviour. Do NOT prescribe architecture decisions
   (e.g. which HTTP library to use, where invalidation logic should live, specific design
   patterns) — those belong in the propose_change phase where they can be explored against
   the real codebase. Acceptance criteria should describe observable outcomes without implying
   hidden complexity that hasn't been validated against the actual cost. When listing files to
   modify, describe the intent (e.g. "persist legs locally") not the solution.

   ## Output Format
   Update the user story file:
   - Add the refined content at the TOP of the file
   - Leave the original story at the bottom, prepended with "## Original user story"
   - Downgrade original headings by one level (## becomes ###, etc.)
   - Apply proper formatting (lists, code snippets, etc.)

   ## When Done
   Report:
   1. Summary of refinements made (bullet points)
   2. Key concerns identified (security, performance, GDPR)
   3. Confirm the file was updated with new content at top and original at bottom
   4. Any assumptions made (these will be reviewed by the orchestrator)
   ```

   **Verification:** After the sub-agent returns, read the user story file and confirm it contains refined content at the top and the original story at the bottom under "## Original user story".

   **Failure handling:** If the sub-agent fails or produces incomplete output, retry once with Opus. If still failing, the orchestrator performs the refinement itself.

7. **Add a report (sub-agent).** Spawn a sub-agent to create or update the report.

   ```
   Agent(
     description: "Add opening report",
     model: "haiku",
     prompt: "<constructed prompt>"
   )
   ```

   **Sub-agent prompt:**
   ```
   You are generating a report section for a user story lifecycle event.

   ## Task

   Create or update the report for this user story following the report guidelines.

   ## Report Data
   - Story ID: {STORY_ID}
   - Story title: {STORY_TITLE}
   - Branch: {BRANCH_NAME}
   - Date: {ISO_DATE}
   - Refinement summary: {REFINEMENT_SUMMARY_FROM_STEP_6}

   ## Guidelines
   Read the report guidelines at: docs/guidelines/guidelines-reports.md

   ## Instructions
   The section should summarise: the user story name, the branch created, and a brief
   summary of the story refinement.

   ## When Done
   Report:
   1. Whether the file was created or updated
   2. Confirm the section was appended correctly
   3. Any issues encountered
   ```

   **Verification:** After the sub-agent returns, confirm the report file exists and contains the new section.

   **Failure handling:** If the sub-agent fails, retry once. If still failing, the orchestrator generates the report inline.

8. **Display the summary.** Output the summary on screen so the user can see what was done: the user story opened, branch created, and key points from the refinement.

9. **Suggest a commit message.** Suggest a commit message following @docs/guidelines/guidelines-git.md.
