# SDLC

SDLC is a group of custom commands that wrap and extend the standard OpenSpec (SDD) workflow.
The standard OpenSpec flow generally follows this sequence: propose → apply → verify → archive
SDLC commands replace the standard flow with a user-story-centric pipeline. Run them in order for a typical feature lifecycle.

Here is a short description of each of the steps. 

1. Open user story: `/sdlc_open_story <story>` 

[sdlc_open_story.md](sdlc_open_story.md) analyses the next story to open, creates a branch, sets the story open and refines it adding a full and detailed product analysis.

2. Propose change: `/sdlc_propose_change <story>`

[sdlc_propose_change.md](sdlc_propose_change.md) analyses the user story, asks for clarifications if something isn't clear, and finally generates the SDD artifacts: proposal, design, specs, and tasks. Tasks are defined with a BDD approach, based on GIVEN/WHEN/THEN acceptance criteria and test-first approach. Uses `/opsx:explore` and `/opsx:propose`.

3. Implement change: `/sdlc_implement_change`

[sdlc_implement_change](sdlc_implement_change.md) implements the current OpenSpec change using sub-agent orchestration with BDD Red/Green cycle (test tasks verified RED before implementation, implementation tasks verified GREEN after). Each task section is delegated to a separate sub-agent with a fresh context window, preventing task checkboxes from being forgotten in long sessions and reducing cost by using cheaper models: Sonnet for BDD sections (test + implement cycles) and Haiku for mechanical tasks (wiring, previews, commands). The parent agent orchestrates, verifies checkbox completion after each section, and handles failures. Then looks for outstanding TODOs, runs a security review, and updates the documentation. Uses `/opsx:apply` and `/security-review`.

4. Verify and Archive: `/sdlc_verify_story <story>`

[sdlc_verify_story](sdlc_verify_story.md) is an end-to-end verification and archive gate. Runs OpenSpec's verify, scans for unresolved TODOs, runs a security review on pending changes, checks every acceptance criterion in the story against the codebase, closes the story, then archives the OpenSpec change and verifies documentation is in sync. Uses `/opsx:verify`, `/opsx:sync`, `/security-review`, and `/opsx:archive`.

Each of the below operations adds a summary of the actions/results into an HTML report in [reports](../../reports)

## Quick start

1. Download the `docs` folder into your project
2. Install OpenSpec from https://github.com/Fission-AI/OpenSpec/ and ensure these commands are available: explore, propose, apply, verify, sync, archive
3. Run the script in `./docs/sdlc`:
- **Linux / macOS**: `./docs/sdlc/sdlc_init.sh`
- **Windows** (PowerShell, Developer Mode or elevated): `.\docs\sdlc\sdlc_init.ps1`

**Please note** : currently only Claude is supported.

## Customisation

The folder structure is:

```
base project folder
├── openspec/                            # OpenSpec standard folder, managed by OpenSpec commands
├── docs/                                # Documentation & SDLC folders
│   ├── guidelines/                      # Development guidelines
│   │   ├── guidelines-android.md
│   │   ├── guidelines-git.md
│   │   ├── guidelines-process.md
│   │   ├── guidelines-reports.md
│   │   └── guidelines-userstories.md
│   ├── reports/                         # Verification and archive reports (HTML/MD)
│   ├── sdlc/                            # SDLC framework
│   │   ├── commands/                    # Command definitions (source of truth)
│   │   │   ├── SDLC-README.md
│   │   │   ├── <sdlc_command>.md
│   │   ├── sdlc_init.sh                 # Symlink setup script (Linux/macOS)
│   │   └── sdlc_init.ps1                # Symlink setup script (Windows)
│   └── userstories/                     # User story backlog organised by epic/feature
│       ├── index.md
│       ├── <id>-<name>-DONE.md          # Completed stories
│       ├── <id>-<name>-WIP.md           # In-progress stories
│       └── <id>-<name>.md               # Backlog stories
├── AGENTS.md                            # Agent instructions (CLAUDE.md symlinks here)
└── README.md
```



## TODO

SDLC:
- multi-agent orchestration
- LLM agnostic
- self-improvement
- sort different phases in the command list -not sure how

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
