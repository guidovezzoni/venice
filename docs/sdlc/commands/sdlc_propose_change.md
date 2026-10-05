Please design the change for the user story: $ARGUMENTS.

This command uses sub-agent orchestration: the codebase exploration, artifact generation, and reporting steps are delegated to separate sub-agents with fresh context windows, using cheaper models (Sonnet/Haiku) where appropriate. Interactive steps (story location, user Q&A) remain with the orchestrator.

Sub-agent orchestration is the default execution strategy for this command.

## Execution Metrics

At the very start of this command, before any other work:

1. **Record start time.** Run `date +%s` and store the result as `{START_EPOCH}`.
2. **Initialise interaction counter.** Set `{INTERACTION_COUNT}` to 0. Each time you pause execution to ask the user a question or wait for user input (e.g. story clarification, answering doubts from exploration), increment `{INTERACTION_COUNT}` by 1 before resuming.

These values are used in the report step to produce the Command Metrics subsection.

## Steps

Follow these steps:

1. **Locate the user story.** Match `$ARGUMENTS` against the user story files by number or partial name. If no match is found, ask the user which user story to design a change for. The story should be in WIP status (opened for development). If it is not, inform the user and stop.

2. **Explore the user story (sub-agent).** Spawn a sub-agent to investigate the codebase and surface findings, questions, and risks.

   ```
   Agent(
     description: "Explore codebase for user story",
     model: "sonnet",
     prompt: "<constructed prompt>"
   )
   ```

   **Sub-agent prompt:**
   ```
   You are exploring a codebase to prepare for designing a change based on a user story.

   ## User Story
   {PASTE FULL USER STORY CONTENT}

   ## Your Mission

   Execute the OpenSpec explore command (`/opsx:explore`) with the user story content as input.
   Use this phase to think through the requirements, investigate the codebase, identify
   integration points, and surface any ambiguities or risks.

   Investigate the codebase to understand:
   1. Existing architecture relevant to this story
   2. Integration points where new code will connect
   3. Patterns already in use that should be followed
   4. Hidden complexity or risks
   5. Dependencies between components

   ## Context Files
   - Android guidelines: docs/guidelines/guidelines-android.md
   - Process guidelines: docs/guidelines/guidelines-process.md
   - AGENTS.md for project overview

   ## Output Format — CRITICAL

   Return a structured report with these exact sections:

   ### Architecture Findings
   - List of relevant modules, classes, patterns found

   ### Integration Points
   - Where new code will connect to existing code

   ### Questions for the User
   - List any ambiguities, unclear requirements, or decisions that need human input
   - Each question should explain WHY it matters for the design
   - DO NOT MAKE ASSUMPTIONS — surface everything that is unclear

   ### Risks and Concerns
   - Technical risks, complexity warnings, potential issues

   ### Recommendations
   - Suggested approach based on existing patterns

   ## Guardrails
   - Do NOT write any code or create any files (read-only exploration)
   - Do NOT make assumptions about unclear requirements — surface them as questions
   - DO explore the actual codebase — grep, find, read files
   - Be thorough but focused on what's relevant to this story
   ```

   **Verification:** After the sub-agent returns, confirm it produced a structured report with the required sections (Architecture Findings, Integration Points, Questions, Risks, Recommendations).

   **Failure handling:** If exploration returns incomplete (no findings), retry once with Sonnet. If still empty, the orchestrator performs exploration itself.

3. **Clarify all doubts.** Before moving to the proposal phase, present all questions surfaced by the exploration sub-agent to the user. Ensure all ambiguities have been resolved. Ask additional questions if required. No assumptions or unresolved doubts should be carried forward into the proposal. DO NOT MAKE ASSUMPTIONS.

4. **Propose the change with BDD task structure (sub-agent).** Spawn a sub-agent to create all SDD artefacts.

   ```
   Agent(
     description: "Propose change with BDD tasks",
     model: "sonnet",
     prompt: "<constructed prompt>"
   )
   ```

   **Sub-agent prompt:**
   ```
   You are creating an OpenSpec change proposal with all SDD artefacts for a user story.

   ## User Story
   {PASTE FULL USER STORY CONTENT}

   ## Exploration Findings
   {PASTE STRUCTURED OUTPUT FROM STEP 2 SUB-AGENT}

   ## Clarifications from User
   {PASTE Q&A FROM STEP 3}

   ## Task

   Execute the OpenSpec propose command (`/opsx:propose`) to create all SDD artefacts
   (proposal, design, delta specs, and tasks) based on the explored and clarified requirements.

   ## BDD Task-Structuring Rules — CRITICAL

   When generating the tasks artefact, apply these rules in addition to any rules returned
   by the openspec CLI:

   Structure tasks using BDD (test-first) ordering. For each testable unit of work,
   create a paired group:

   1. A test task (write the test first)
   2. The implementation task(s) that make the test pass

   Group these pairs under a section heading that describes the feature being built.
   Use the pattern:

     ## N. <Feature Name> (BDD)
     - [ ] N.1 Write test: <GIVEN/WHEN/THEN description> in <TestClass>
     - [ ] N.2 Implement: <what to build to make the test pass>

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

   ## Context Files
   - Android guidelines: docs/guidelines/guidelines-android.md
   - Process guidelines: docs/guidelines/guidelines-process.md

   ## Guardrails
   - Create ALL artifacts needed for implementation
   - Follow the BDD task-structuring rules strictly
   - Do NOT ask clarifying questions — all ambiguities have been resolved
   - Verify each artifact file exists after writing

   ## When Done
   Report:
   1. Change name and location
   2. List of artifacts created (with file paths)
   3. Task count and structure summary (how many BDD pairs, how many mechanical sections)
   4. Any issues encountered
   ```

   **Verification:** After the sub-agent returns, verify that the change directory exists under `openspec/changes/` and contains the expected artifacts (proposal, design, specs, tasks).

   **Failure handling:** If artifact generation fails (missing artifacts, CLI errors), retry once with Sonnet. If still failing, the orchestrator performs the proposal generation itself (escalation to Opus).

5. **Add a report (sub-agent).** Run `date +%s` and store the result as `{END_EPOCH}`. Then spawn a sub-agent to append the report section.

   ```
   Agent(
     description: "Add proposal report",
     model: "haiku",
     prompt: "<constructed prompt>"
   )
   ```

   **Sub-agent prompt:**
   ```
   You are generating a report section for the propose_change phase of a user story.

   ## Task

   Create or update the report for this user story following the report guidelines.

   ## Report Data
   - Story ID: {STORY_ID}
   - Story title: {STORY_TITLE}
   - Branch: {BRANCH_NAME}
   - Date: {ISO_DATE}
   - Exploration summary: {EXPLORATION_FINDINGS_SUMMARY}
   - Questions asked: {LIST_OF_QUESTIONS}
   - Answers received: {LIST_OF_ANSWERS}
   - Artifacts created: {LIST_OF_ARTIFACTS_FROM_STEP_4}
   - Command start time (epoch): {START_EPOCH}
   - Command end time (epoch): {END_EPOCH}
   - User interaction count: {INTERACTION_COUNT}

   ## Guidelines
   Read the report guidelines at: docs/guidelines/guidelines-reports.md

   ## Instructions
   The section should summarise: the exploration findings, questions asked and answers
   received, and the artefacts created by the proposal.

   Additionally, include a **Command Metrics** subsection at the end of the section with
   this HTML structure:

   ```html
   <h3>Command Metrics</h3>
   <table>
     <tr><th>Metric</th><th>Value</th></tr>
     <tr><td>Start time</td><td>{START_EPOCH converted to human-readable local time, formatted as YYYY-MM-DD HH:MM:SS}</td></tr>
     <tr><td>End time</td><td>{END_EPOCH converted to human-readable local time, formatted as YYYY-MM-DD HH:MM:SS}</td></tr>
     <tr><td>Duration</td><td>{computed from END_EPOCH - START_EPOCH, formatted as Xm Ys}</td></tr>
     <tr><td>User interactions</td><td>{INTERACTION_COUNT}</td></tr>
   </table>
   <p><em>Metrics are tracked by the LLM during execution. Duration includes time
   waiting for user responses. Interaction count relies on in-context tracking and
   may be approximate.</em></p>
   ```

   ## When Done
   Report:
   1. Whether the file was created or updated
   2. Confirm the section was appended correctly
   3. Any issues encountered
   ```

   **Verification:** After the sub-agent returns, confirm the report file exists and contains the new section.

   **Failure handling:** If the sub-agent fails, retry once. If still failing, the orchestrator generates the report inline.

6. **Display the summary.** Output the same summary on screen so the user can see what was done.

7. **Suggest a commit message.** Suggest a commit message following @docs/guidelines/guidelines-git.md.
