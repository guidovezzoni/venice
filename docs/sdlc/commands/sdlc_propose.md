Please design the change for the user story: $ARGUMENTS.

Follow these steps:

1. **Locate the user story.** Match `$ARGUMENTS` against the user story files by number or partial name. If no match is found, ask the user which user story to design a change for. The story should be in WIP status (opened for development). If it is not, inform the user and stop.

2. **Explore the user story.** Execute the OpenSpec explore command (`/opsx:explore`) with the user story content as input. Use this phase to think through the requirements, investigate the codebase, identify integration points, and surface any ambiguities or risks. DO NOT MAKE ASSUMPTIONS — if anything is unclear, ask the user for clarification before proceeding.

3. **Clarify all doubts.** Before moving to the proposal phase, ensure all questions and ambiguities have been resolved. Ask the user additional questions if required. No assumptions or unresolved doubts should be carried forward into the proposal.

4. **Propose the change with BDD task structure.** Execute the OpenSpec propose command (`/opsx:propose`) to create all SDD artefacts (proposal, design, delta specs, and tasks) based on the explored and clarified requirements.

   **IMPORTANT — BDD task-structuring rules.** When generating the tasks artefact, apply these rules in addition to any rules returned by the openspec CLI:

   Structure tasks using BDD (test-first) ordering. For each testable unit of work,
   create a paired group:

   1. A test task (write the test first)
   2. The implementation task(s) that make the test pass

   Group these pairs under a section heading that describes the feature being built.
   Use the pattern:

     ## N. \<Feature Name\> (BDD)
     - [ ] N.1 Write test: \<GIVEN/WHEN/THEN description\> in \<TestClass\>
     - [ ] N.2 Implement: \<what to build to make the test pass\>

   Tasks that are NOT testable (string resources, DI wiring, project setup,
   navigation wiring, verification) should be grouped in their own
   sections WITHOUT test pairs — either as prerequisites at the beginning or as
   integration tasks at the end.

   Ordering rules:
   - Prerequisites (setup, dependencies, model/data classes that tests import) come first
   - BDD pairs come in the middle, ordered by dependency
   - Integration tasks (strings, DI, navigation wiring) come after BDD pairs
   - Final verification section at the end

   Each test task description MUST include the GIVEN/WHEN/THEN name
   and the target test class name.

5. **Add a report.** Append a section to the report for this user story following @docs/guidelines/guidelines-reports.md. The section should summarise: the exploration findings, questions asked and answers received, and the artefacts created by the proposal.

6. **Display the summary.** Output the same summary on screen so the user can see what was done.

7. **Suggest a commit message.** Suggest a commit message following @docs/guidelines/guidelines-git.md.
