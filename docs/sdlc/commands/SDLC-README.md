# SDLC

SDLC is a group of custom commands that wrap and extend the standard OpenSpec (SDD) workflow, tailored to this project's user-story-driven development process.

The standard OpenSpec flow generally follows this sequence: propose → apply → verify → archive
These commands replace the standard flow with a user-story-centric pipeline. Run them in order for a typical feature lifecycle.

Each of the below operations adds a summary of the actions/results into an HTML report in [reports](../../reports)

### 1. Open user story: `/sdlc_open_story <story>` 

[sdlc_open_story.md](sdlc_open_story.md) analyses the next story to open, creates a branch, sets the story open and refines it adding a full and detailed product analysis.

### 2. Propose change: `/sdlc_propose <story>`

[sdlc_propose.md](sdlc_propose.md) analyses the user story, asks for clarifications if something isn't clear, and finally generates the SDD artifacts: proposal, design, specs, and tasks. Tasks are defined with a BDD approach, based on GIVEN/WHEN/THEN acceptance criteria and test-first approach. Uses `/opsx:explore` and `/opsx:propose`.

### 3. Apply change: `/sdlc_apply_changes`

[sdlc_apply_changes](sdlc_apply_changes.md) implements the current OpenSpec change using BDD Red/Green cycle (test tasks verified RED before implementation, implementation tasks verified GREEN after). Then looks for outstanding TODOs, runs a security review, and updates the documentation. Uses `/opsx:apply` and `/security-review`.

### 4. Verify User Story: `/sdlc_verify_story <story>`

[sdlc_verify_story](sdlc_verify_story.md) is an end-to-end verification gate. Runs OpenSpec's verify, scans for unresolved TODOs, runs a security review on pending changes, checks every acceptance criterion in the story against the codebase, and finally closes the story. Uses `/opsx:verify` and `/security-review`.

### 5. Archive: `/sdlc_archive`

[sdlc_archive](sdlc_archive.md) runs OpenSpec's archive to finalise and archive the completed change, then verifies that the documentation is in sync with the codebase and specs. Uses `/opsx:archive`.

## Setup

Run once after cloning, or after adding a new command to `docs/sdlc/commands/`:

- **Linux / macOS**: `./docs/sdlc/sdlc_init.sh`
- **Windows** (PowerShell, Developer Mode or elevated): `.\docs\sdlc\sdlc_init.ps1`

These scripts create symlinks in `.claude/commands/sdlc/` and `.cursor/commands/` for every SDLC command.

## TODO

SDLC:
- add PR review - other  LLM provider???
- archive should also sync???
- archive should be merged with verification???
- multi-agent orchestration
- LLM agnostic
- self-improvement
- sort different phases in the command list -not sure how

Guidelines:
- async operations should be wrapped in a loading state with a spinner, and if required disabling the button that triggered the operation, to avoid re-trigger. The spinner should have a minimum duration of 0.5 seconds to avoid a flickering UI.
- create a guideline for readme

Not sure what's best yet:
- When both Domain and UI require the same data type, f.i. an enum, where should this be defined? In Domain? Should it be duplicated in UI? Should it be defined in another root package?