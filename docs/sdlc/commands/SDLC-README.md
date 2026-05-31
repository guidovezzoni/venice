# Custom Commands

Custom slash commands that wrap and extend the standard OpenSpec (SDD) workflow, tailored to this project's user-story-driven development process.

## Standard OpenSpec flow

```
propose -> apply -> verify -> archive
```

## Custom workflow

The commands below replace the standard flow with a user-story-centric pipeline. Run them in order for a typical feature lifecycle:

### 1. Open user story: `/sdlc_open_story <story>`

Opens a user story for development. Creates a feature branch from `main`, renames the story file with a `-WIP` suffix, enriches it with technical detail, and creates the initial report.

### 2. Propose change: `/sdlc_propose <story>`

Explores the user story via `/opsx:explore`, asks clarifying questions until all doubts are resolved, then runs `/opsx:propose` to generate all SDD artefacts with BDD-structured tasks (test-first pairs with GIVEN/WHEN/THEN descriptions).

### 3. Apply change: `/sdlc_apply_changes`

Runs `/opsx:apply` to implement the current OpenSpec change using BDD Red/Green cycle (test tasks verified RED before implementation, implementation tasks verified GREEN after), then scans `app/src/` for TODO comments. TODOs whose preconditions are now met are resolved iteratively until none remain; unrelated TODOs are acknowledged. Runs `/security-review` and iteratively fixes any findings until the review is clean. Updates `README.md` and `AGENTS.md` if affected by the delivered changes.

### 4. Verify User Story: `/sdlc_verify_story <story>`

End-to-end verification gate. Runs `/opsx:verify`, scans for unresolved TODOs, runs `/security-review` on pending changes, checks every acceptance criterion in the story against the codebase, renames the story file to `-DONE`, and appends a verification section to the report.

### 5. Archive: `/sdlc_archive`

Runs `/opsx:archive` to finalise and archive the completed change, then verifies that `README.md` and `AGENTS.md` are in sync with the codebase and specs. Appends an archive section to the report.

## Setup

Run once after cloning, or after adding a new command to `docs/sdlc/commands/`:

- **Linux / macOS**: `./docs/sdlc/sdlc_init.sh`
- **Windows** (PowerShell, Developer Mode or elevated): `.\docs\sdlc\sdlc_init.ps1`

These scripts create symlinks in `.claude/commands/sdlc/` and `.cursor/commands/` for every SDLC command.

## TODO

- add PR review
- use git mv during verification
- archive should also sync
