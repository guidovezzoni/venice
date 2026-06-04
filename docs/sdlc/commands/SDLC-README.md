# SDLC

SDLC is a group of custom commands that wrap and extend the standard OpenSpec (SDD) workflow, tailored to this project's user-story-driven development process.

The standard OpenSpec flow generally follows this sequence: propose → apply → verify → archive
These commands replace the standard flow with a user-story-centric pipeline. Run them in order for a typical feature lifecycle:

### 1. Open user story: `/sdlc_open_story <story>` 

[sdlc_open_story.md](sdlc_open_story.md) analyses the next story to open, creates a branch, sets the story open and refines it adding a full and detailed product analysis.

### 2. Propose change: `/sdlc_propose <story>`

[sdlc_propose.md](sdlc_propose.md) analyses the user story, asks for clarifications if something isn't clear, and finally generates the SDD artifacts: proposal, design, specs, and tasks. Tasks are defined with a BDD approach, based on GIVEN/WHEN/THEN acceptance criteria and test-first approach. Uses `/opsx:explore` and `/opsx:propose`.

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

SDLC:
- add PR review - other  LLM provider???
- use git mv during verification
- archive should also sync???
- archive should be merged with verification???
- multi-agent orchestration
- LLM agnostic
- self-improvement
- sort different phases in the command list -not sure how

Not sure what's best yet:
- When both Domain and UI require the same data type, f.i. an enum, where should this be defined? In Domain? Should it be duplicated in UI? Should it be defined in another root package?