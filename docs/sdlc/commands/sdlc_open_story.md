Please open the user story for development: $ARGUMENTS.

Follow these steps:

1. **Ensure main is up-to-date.** If the current branch is not main, check for active changes and inform the user about the current branch and changes, then ask what they want to do: DO NOT MAKE ASSUMPTIONS and DO NOT DELETE DATA. Once on main, fetch and pull the latest changes. If the pull fails or there are conflicts, inform the user and ask how to proceed: DO NOT MAKE ASSUMPTIONS.

2. **Resolve the user story argument.** If `$ARGUMENTS` is empty or not provided, identify the **Next User Story** as defined in @docs/guidelines/guidelines-userstories.md. Inform the user which story was auto-selected. If no actionable story exists, inform the user and stop.

3. **Locate the user story.** Match the argument against the user story files by number or partial name. If no match is found, ask the user which user story to open. Validate the **preconditions for Opening** as defined in @docs/guidelines/guidelines-userstories.md. If they are not met, inform the user and stop.

4. **Create a feature branch.** Using the resolved user story reference:
   1. The new branch should live under the "feature" folder.
   2. The new branch should start with the ticket number or reference of the user story.

5. **Open the user story.** Perform the **Opening** operation as defined in @docs/guidelines/guidelines-userstories.md.

6. **Refine the user story.** Analyse and refine the resolved user story following these steps:
   1. You are now an expert Product Manager, Business Analyst, with a strong engineering background, and a special expertise in GDPR, sensitive information, and security.
   2. Read the user story from @docs/userstories. If you cannot find it ask the user what user story should be used.
   3. Understand the problem described in the ticket.
   4. Analyse the User Story and decide whether it's a well-defined User Story. It is so when it's fully defined according to product's best practices, it should include:
      1. A full description of the functionality
      2. A comprehensive list of fields to be updated
      3. The structure and URLs of the necessary endpoints
      4. The files to be modified according to the architecture and best practices
      5. How to create Unit Tests
      6. How to update any relevant documentation
      7. Highlight any security potential issue and how the suggested solution addresses them
      8. Highlight any performance potential issue and how the suggested solution addresses them
      9. Highlight any GDPR and sensitive information potential issue and how the suggested solution addresses them
      10. Highlight any other concern related to non-functional requirement and how the suggested solution addresses them
      11. The steps required for the task to be considered complete
   5. If the current user story lacks the technical and specific detail required to allow the developer to be fully autonomous when completing it, provide an improved story that is clearer, more specific, and more in line with product best practices described in step 4. Use the technical context you will find in @docs/guidelines. Return it in markdown format.
   6. Update the user story file, adding the new content at the top of the file, and leaving the old user story at the end. Prepend the old user story with an h2 tag "Original user story" and update the old story's titles accordingly - if there was a "## Title" it would become "### Title" and so on. Apply proper formatting to make it readable and visually clear, using appropriate text types (lists, code snippets...).

7. **Add a report.** Create or update the report for this user story following @docs/guidelines/guidelines-reports.md. The section should summarise: the user story name, the branch created, and a brief summary of the story refinement.

8. **Display the summary.** Output the same summary on screen so the user can see what was done.

9. **Suggest a commit message.** Suggest a commit message following @docs/guidelines/guidelines-git.md.
