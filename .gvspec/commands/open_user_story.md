Please open the user story for development: $ARGUMENTS.

Follow these steps:

1. **Resolve the user story argument.** If `$ARGUMENTS` is empty or not provided:
   a. Check `@docs/userstories/index.md` (or scan the `@docs/userstories/` directory if no index exists).
   b. Find the first user story (in index order) whose filename does **not** end with `-DONE` or `-WIP` (i.e. it is ready to be started).
   c. If exactly one or more such stories exist, select the **first** one and use it as the argument going forward. Inform the user which story was auto-selected.
   d. If **all** stories are either DONE or WIP, inform the user that there are no actionable stories and stop.

2. **Locate the user story** in `@docs/userstories/`. Match the argument against the file names (by number or partial name). If no match is found, ask the user which user story to open. If the file already has the `-WIP` suffix, inform the user the story is already in progress and stop. If the file already has the `-DONE` suffix, inform the user the story is already completed and stop.

3. **Create a feature branch.** Execute the `/create_branch` command passing `$ARGUMENTS` to create a properly named feature branch for this story.

4. **Rename the user story file.** Append `-WIP` before the `.md` extension (e.g. `1.4.2-Create-leg.md` → `1.4.2-Create-leg-WIP.md`). Use `git mv` so the rename is tracked in version control. Update any references to the old filename in `@docs/userstories/index.md` if it exists.

5. **Refine the user story.** Execute the `/refine_user_story` command passing `$ARGUMENTS` to analyse and enhance the story with full technical detail.

6. **Report the result.** Summarise what was done (branch created, file renamed, story refined) and the new filename. Suggest a commit message with the prefix `[Suggested Commit Message]`.
