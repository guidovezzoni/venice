Please archive the completed change: $ARGUMENTS.

Follow these steps:

1. **Archive the OpenSpec change.** Execute the OpenSpec archive command (`/opsx:archive`) to finalise and archive the completed change artefacts.

2. **Update README.md if required.** Read `README.md` and check whether the changes delivered by the archived story affect any section of the file (e.g. feature list, architecture table, tech stack, build instructions). If any section is now outdated or incomplete, update it to reflect the current state of the project. If everything is already accurate, skip this step.

3. **Update AGENTS.md if required.** Read `AGENTS.md` and check whether the changes delivered by the archived story affect any section of the file (e.g. project overview, folder structure, workflow descriptions). If any section is now outdated or incomplete, update it to reflect the current state of the project. If everything is already accurate, skip this step.

4. **Add a report.** Append a section to the report for this user story following @docs/guidelines/guidelines-reports.md. The section should summarise: the change name, the archive location, the spec sync status (synced / skipped / no delta specs), whether README.md and AGENTS.md were updated, and any warnings raised during the archive (incomplete artefacts or tasks).

5. **Display the summary.** Output the same summary on screen so the user can see what was done.

6. **Suggest a commit message.** Suggest a commit message following @docs/guidelines/guidelines-git.md.
