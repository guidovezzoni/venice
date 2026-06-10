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

### 3b. Apply change (sub-agents): `/sdlc_apply_changes_with_sub_agents`

[sdlc_apply_changes_with_sub_agents](sdlc_apply_changes_with_sub_agents.md) is an alternative to `/sdlc_apply_changes` that uses sub-agent orchestration. Instead of running all tasks in a single session, it delegates each task section to a separate sub-agent with a fresh context window. This prevents task checkboxes from being forgotten in long sessions. It also reduces cost by using cheaper models: Sonnet for BDD sections (test + implement cycles) and Haiku for mechanical tasks (wiring, previews, commands). The parent agent orchestrates, verifies checkbox completion after each section, and handles failures. Steps 2-10 (TODO scan, security review, etc.) are identical to the standard apply command.

### 4. Verify and Archive: `/sdlc_verify_story <story>`

[sdlc_verify_story](sdlc_verify_story.md) is an end-to-end verification and archive gate. Runs OpenSpec's verify, scans for unresolved TODOs, runs a security review on pending changes, checks every acceptance criterion in the story against the codebase, closes the story, then archives the OpenSpec change and verifies documentation is in sync. Uses `/opsx:verify`, `/security-review`, and `/opsx:archive`.

## Setup

Run once after cloning, or after adding a new command to `docs/sdlc/commands/`:

- **Linux / macOS**: `./docs/sdlc/sdlc_init.sh`
- **Windows** (PowerShell, Developer Mode or elevated): `.\docs\sdlc\sdlc_init.ps1`

These scripts create symlinks in `.claude/commands/sdlc/` and `.cursor/commands/` for every SDLC command.

## TODO

SDLC:
- multi-agent orchestration
- LLM agnostic
- self-improvement
- sort different phases in the command list -not sure how
- verification: remind the user to connect a phone but then start the verification anyway. When the on-device test need to be run, then ask the user to connect the device.

Guidelines:
- async operations should be wrapped in a loading state with a spinner, and if required disabling the button that triggered the operation, to avoid re-trigger. The spinner should have a minimum duration of 0.5 seconds to avoid a flickering UI.
- create a guideline for readme
- step for static checks:
  - unused import directive / deprecation
- Test: when writing a test:
  - use SUT to clarify what class is being tested
  - do not use for the expected value, the same internal function being tested -> this however conflicts with BDD's black box behaviour
  - insert a comment with the AAA?
- There are several decisions that have been taken just "because it's a small project": that should not happen: all the projects I start are small and they will likely  become bigger, so they should use the expected architecture and structures.
- Check coverage - 100% ???

Not sure what's best yet:
- When both Domain and UI require the same data type, f.i. an enum, where should this be defined? In Domain? Should it be duplicated in UI? Should it be defined in another root package?
- add PR review - other  LLM provider???
