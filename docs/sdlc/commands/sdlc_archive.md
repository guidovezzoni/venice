Please archive the completed change: $ARGUMENTS.

Follow these steps:

1. **Archive the OpenSpec change.** Execute the OpenSpec archive command (`/opsx:archive`) to finalise and archive the completed change artefacts. Ensure that all file moves use `git mv` so that Git tracks the renames.

2. **Verify README.md and AGENTS.md are in sync.** Read `README.md` and `AGENTS.md` and verify that they accurately reflect the current state of the codebase and specs after the archived change. If any section is outdated or incomplete, flag it to the user and update it. If everything is already accurate, note that the check passed.

3. **Add a report.** Append a section to the report for this user story following @docs/guidelines/guidelines-reports.md. The section should summarise: the change name, the archive location, the spec sync status (synced / skipped / no delta specs), README.md and AGENTS.md sync check result (in sync / updated), and any warnings raised during the archive (incomplete artefacts or tasks).

4. **Display the summary.** Output the same summary on screen so the user can see what was done.

5. **Suggest a commit message.** Suggest a commit message following @docs/guidelines/guidelines-git.md.
