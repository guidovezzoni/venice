Please open the user story for development: $ARGUMENTS.

Follow these steps:

1. **Resolve the user story argument.** If `$ARGUMENTS` is empty or not provided, identify the **Next User Story** as defined in @docs/guidelines/guidelines-userstories.md. Inform the user which story was auto-selected. If no actionable story exists, inform the user and stop.

2. **Locate the user story.** Match the argument against the user story files by number or partial name. If no match is found, ask the user which user story to open. Validate the **preconditions for Opening** as defined in @docs/guidelines/guidelines-userstories.md. If they are not met, inform the user and stop.

3. **Create a feature branch.** Follow the procedure in @.gvspec/references/create-branch-procedure.md using the resolved user story reference.

4. **Open the user story.** Perform the **Opening** operation as defined in @docs/guidelines/guidelines-userstories.md.

5. **Refine the user story.** Follow the procedure in @.gvspec/references/refine-user-story-procedure.md using the resolved user story reference.

6. **Report the result.** Summarise what was done (branch created, file renamed, story refined) and the new filename. Suggest a commit message with the prefix `[Suggested Commit Message]`.
