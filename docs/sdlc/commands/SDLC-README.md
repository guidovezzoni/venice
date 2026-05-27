# Custom Commands

Custom slash commands that wrap and extend the standard OpenSpec (SDD) workflow, tailored to this project's user-story-driven development process.

## Standard OpenSpec flow

```
propose -> apply -> verify -> archive
```

## Custom workflow

The commands below replace the standard flow with a user-story-centric pipeline. Run them in order for a typical feature lifecycle:

### 1. Open user story: `/sdlc_open_story <story>`

Opens a user story for development. Creates a feature branch from `main` (via `/create_branch`), renames the story file with a `-WIP` suffix, enriches it with technical detail (via `/refine_user_story`), and creates the initial report.

### 2. Propose change: `/sdlc_propose <story>`

Explores the user story via `/opsx:explore`, asks clarifying questions until all doubts are resolved, then runs `/opsx:propose` to generate all SDD artefacts (proposal, design, delta specs, and tasks) needed before implementation can begin.

### 3. Apply change: `/sdlc_apply_changes`

Runs `/opsx:apply` to implement the current OpenSpec change, then scans `app/src/` for TODO comments. TODOs whose preconditions are now met are resolved iteratively until none remain; unrelated TODOs are acknowledged.

### 4. Verify User Story: `/sdlc_verify_story <story>`

End-to-end verification gate. Runs `/opsx:verify`, scans for unresolved TODOs, checks every acceptance criterion in the story against the codebase, renames the story file to `-DONE`, and appends a verification section to the report.

### 5. Archive: `/sdlc_archive`

Runs `/opsx:archive` to finalise and archive the completed change, then checks whether `README.md` and `AGENTS.md` need updating to reflect the delivered work. Appends an archive section to the report.

## Setup

Run once after cloning, or after adding a new command to `docs/sdlc/commands/`:

- **Linux / macOS**: `./docs/sdlc/sdlc_init.sh`
- **Windows** (PowerShell, Developer Mode or elevated): `.\docs\sdlc\sdlc_init.ps1`

These scripts create symlinks in `.claude/commands/sdlc/` and `.cursor/commands/` for every SDLC command.

## Helper commands

These are called internally by the workflow commands above but can also be run standalone:

- **`/create_branch <story>`** — Creates a feature branch from an up-to-date `main`, named after the story's ticket number. Handles dirty-state and conflict scenarios interactively.
- **`/refine_user_story <story>`** — Analyses a user story as a Product Manager / Business Analyst would, enriching it with endpoint details, field lists, file mappings, test strategy, and non-functional concerns (security, GDPR, performance). Updates the story file in place, preserving the original text.

## Shared references

Reusable procedure definitions live in `docs/sdlc/references/` and are loaded via `@` file references. This avoids skill invocations (which create turn boundaries) and keeps logic in a single place:

- **`create-branch-procedure.md`** — Branch creation criteria (used by `/open_user_story` and `/create_branch`)
- **`refine-user-story-procedure.md`** — User story refinement steps (used by `/open_user_story` and `/refine_user_story`)

## TODO

- BDD should be taken out of opsx into sdlc