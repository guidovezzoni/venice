# Custom Commands

Custom slash commands that wrap and extend the standard OpenSpec (SDD) workflow, tailored to this project's user-story-driven development process.

## Standard OpenSpec flow

```
propose -> apply -> verify -> archive
```

## Custom workflow

The commands below replace the standard flow with a user-story-centric pipeline. Run them in order for a typical feature lifecycle:

### 1. `/open_user_story <story>`

Opens a user story for development. Creates a feature branch from `main` (via `/create_branch`), renames the story file with a `-WIP` suffix, and enriches it with technical detail (via `/refine_user_story`).

### 2. `/opsx:propose`

Proposes the change based on the refined user story. Generates all OpenSpec artifacts (design, delta specs, and tasks) needed before implementation can begin.

### 3. `/apply_changes`

Runs `/opsx:apply` to implement the current OpenSpec change, then scans `app/src/` for TODO comments. TODOs whose preconditions are now met are resolved iteratively until none remain; unrelated TODOs are acknowledged.

### 4. `/verify_user_story <story>`

End-to-end verification gate. Runs `/opsx:verify`, scans for unresolved TODOs, checks every acceptance criterion in the story against the codebase, renames the story file to `-DONE`, and writes a verification report to `docs/reports/`.

### 5. Archive

Use the standard OpenSpec archive command (`/opsx:archive`) to finalise and archive the completed change.

## Helper commands

These are called internally by the workflow commands above but can also be run standalone:

- **`/create_branch <story>`** — Creates a feature branch from an up-to-date `main`, named after the story's ticket number. Handles dirty-state and conflict scenarios interactively.
- **`/refine_user_story <story>`** — Analyses a user story as a Product Manager / Business Analyst would, enriching it with endpoint details, field lists, file mappings, test strategy, and non-functional concerns (security, GDPR, performance). Updates the story file in place, preserving the original text.

## Shared references

Reusable procedure definitions live in `.gvspec/references/` and are loaded via `@` file references. This avoids skill invocations (which create turn boundaries) and keeps logic in a single place:

- **`create-branch-procedure.md`** — Branch creation criteria (used by `/open_user_story` and `/create_branch`)
- **`refine-user-story-procedure.md`** — User story refinement steps (used by `/open_user_story` and `/refine_user_story`)

## TODO

- add a prefix
- open_user_story should switch to main and fetch update as a first task
