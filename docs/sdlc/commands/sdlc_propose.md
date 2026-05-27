Please design the change for the user story: $ARGUMENTS.

Follow these steps:

1. **Locate the user story.** Match `$ARGUMENTS` against the user story files by number or partial name. If no match is found, ask the user which user story to design a change for. The story should be in WIP status (opened for development). If it is not, inform the user and stop.

2. **Explore the user story.** Execute the OpenSpec explore command (`/opsx:explore`) with the user story content as input. Use this phase to think through the requirements, investigate the codebase, identify integration points, and surface any ambiguities or risks. DO NOT MAKE ASSUMPTIONS — if anything is unclear, ask the user for clarification before proceeding.

3. **Clarify all doubts.** Before moving to the proposal phase, ensure all questions and ambiguities have been resolved. Ask the user additional questions if required. No assumptions or unresolved doubts should be carried forward into the proposal. Once the user confirms that all is clear, proceed.

4. **Propose the change.** Execute the OpenSpec propose command (`/opsx:propose`) to create all SDD artefacts (proposal, design, delta specs, and tasks) based on the explored and clarified requirements.

5. **Add a report.** Append a section to the report for this user story following @docs/guidelines/guidelines-reports.md. The section should summarise: the exploration findings, questions asked and answers received, and the artefacts created by the proposal.

6. **Display the summary.** Output the same summary on screen so the user can see what was done.

7. **Suggest a commit message.** Suggest a commit message following @docs/guidelines/guidelines-git.md.
