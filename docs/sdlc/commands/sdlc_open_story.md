Please open the user story for development: $ARGUMENTS.

Follow these steps:

1. **Ensure main is up-to-date.** If the current branch is not main, check for active changes and inform the user about the current branch and changes, then ask what they want to do: DO NOT MAKE ASSUMPTIONS and DO NOT DELETE DATA. Once on main, fetch and pull the latest changes. If the pull fails or there are conflicts, inform the user and ask how to proceed: DO NOT MAKE ASSUMPTIONS.

2. **Resolve the user story argument.** If `$ARGUMENTS` is empty or not provided, identify the **Next User Story** as defined in @docs/guidelines/guidelines-userstories.md. Inform the user which story was auto-selected. If no actionable story exists, inform the user and stop.

3. **Locate the user story.** Match the argument against the user story files by number or partial name. If no match is found, ask the user which user story to open. Validate the **preconditions for Opening** as defined in @docs/guidelines/guidelines-userstories.md. If they are not met, inform the user and stop.

4. **Create a feature branch.** Follow the procedure in @docs/sdlc/references/create-branch-procedure.md using the resolved user story reference.

5. **Open the user story.** Perform the **Opening** operation as defined in @docs/guidelines/guidelines-userstories.md.

6. **Refine the user story.** Follow the procedure in @docs/sdlc/references/refine-user-story-procedure.md using the resolved user story reference.

7. **Add a report.** Create or update the report for this user story following @docs/guidelines/guidelines-reports.md. The section should summarise: the user story name, the branch created, and a brief summary of the story refinement.

8. **Display the summary.** Output the same summary on screen so the user can see what was done.

9. **Suggest a commit message.** Suggest a commit message following @docs/guidelines/guidelines-git.md.
