## Warning

> **This is an experimental command.**
>
> It chains all four lifecycle phases (open, propose, implement, verify) without pausing for human review between them. Those review gates are the primary safeguard against tech debt, misaligned design, and scope creep being baked into the codebase.
>
> Additionally, running all four phases sequentially spawns significantly more sub-agents than any individual command. Token consumption is much higher, and a single run may bring you close to your rate limit.
>
> For more guardrails, use the individual commands (`/sdlc_open_story`, `/sdlc_propose_change`, `/sdlc_implement_change`, `/sdlc_verify_story`) instead.

---

Please vibe the user story end-to-end: $ARGUMENTS.

This command chains the four SDLC lifecycle commands (open, propose, implement, verify) into a single run with auto-commits between phases. Each phase executes its corresponding command file exactly as-is — all interactive steps are preserved. The only automation added is: commit after each phase completes, then launch the next phase.

## Model Assignment

| Phase | Command | Sub-agent model |
|-------|---------|----------------|
| 1. Open & Refine | `docs/sdlc/commands/sdlc_open_story.md` | opus |
| 2. Propose Change | `docs/sdlc/commands/sdlc_propose_change.md` | opus |
| 3. Implement Change | `docs/sdlc/commands/sdlc_implement_change.md` | sonnet |
| 4. Verify & Archive | `docs/sdlc/commands/sdlc_verify_story.md` | sonnet |

## Commit Policy

After each phase completes successfully, the orchestrator commits all changes before launching the next phase. Commits use the story reference from the branch name as prefix.

| Phase | Commit message |
|-------|---------------|
| 1 | `{REF} Open and refine user story` |
| 2 | `{REF} Propose change with BDD tasks` |
| 3 | `{REF} Implement change` |
| 4 | `{REF} Verify and archive story` |

Where `{REF}` is the story/ticket reference extracted from the branch name (e.g. `PL013`).

## Steps

### Step 1: Phase 1 — Open and Refine

Spawn a sub-agent to execute the open story command.

```
Agent(
  description: "Phase 1: Open and refine user story",
  model: "opus",
  prompt: "<constructed prompt>"
)
```

**Sub-agent prompt:**
```
You are executing the SDLC open story command for: {$ARGUMENTS}

Read and follow ALL steps in the command file at:
  docs/sdlc/commands/sdlc_open_story.md

Follow the command exactly as written, including all interactive steps.

IMPORTANT EXCEPTIONS:
- Do NOT suggest a commit message at the end (step 10) — the orchestrator handles commits.
- When the command says to "inform the user and stop" due to a blocker, you should
  still stop and report the issue — the orchestrator will relay it to the user.

When done, report:
1. The story file path (after rename to -WIP)
2. The branch name created
3. The story ID and title
4. A brief summary of the refinement
5. Any issues that require user attention
```

**After sub-agent returns:**
1. Verify the story file exists at its `-WIP` path.
2. Extract from the sub-agent output: `STORY_ID`, `STORY_TITLE`, `BRANCH_NAME`, `USER_STORY_FILE_PATH`.
3. Determine `{REF}` from the branch name (typically the ticket/story number prefix).
4. Commit:
   ```
   git add -A
   git commit -m "{REF} Open and refine user story

   Co-Authored-By: Claude <noreply@anthropic.com>"
   ```
5. If the sub-agent reported a blocker, relay it to the user and STOP.

---

### Step 2: Phase 2 — Propose Change

Spawn a sub-agent to execute the propose change command.

```
Agent(
  description: "Phase 2: Propose change with BDD tasks",
  model: "opus",
  prompt: "<constructed prompt>"
)
```

**Sub-agent prompt:**
```
You are executing the SDLC propose change command for the current user story.

Context from Phase 1:
- Story ID: {STORY_ID}
- Story title: {STORY_TITLE}
- Story file: {USER_STORY_FILE_PATH}
- Branch: {BRANCH_NAME}

Read and follow ALL steps in the command file at:
  docs/sdlc/commands/sdlc_propose_change.md

Follow the command exactly as written, including all interactive steps
(such as clarifying doubts with the user in step 3).

IMPORTANT EXCEPTIONS:
- Do NOT suggest a commit message at the end (step 7) — the orchestrator handles commits.
- When the command says to "inform the user and stop" due to a blocker, you should
  still stop and report the issue — the orchestrator will relay it to the user.

When done, report:
1. The OpenSpec change name
2. List of artifacts created (with file paths)
3. The tasks file path
4. Task count and structure summary
5. Any issues that require user attention
```

**After sub-agent returns:**
1. Verify the change directory exists under `openspec/changes/`.
2. Extract from the sub-agent output: `CHANGE_NAME`, `TASKS_FILE_PATH`.
3. Commit:
   ```
   git add -A
   git commit -m "{REF} Propose change with BDD tasks

   Co-Authored-By: Claude <noreply@anthropic.com>"
   ```
4. If the sub-agent reported a blocker, relay it to the user and STOP.

---

### Step 3: Phase 3 — Implement Change

Spawn a sub-agent to execute the implement change command.

```
Agent(
  description: "Phase 3: Implement change",
  model: "sonnet",
  prompt: "<constructed prompt>"
)
```

**Sub-agent prompt:**
```
You are executing the SDLC implement change command.

Context from prior phases:
- Story ID: {STORY_ID}
- Story title: {STORY_TITLE}
- Story file: {USER_STORY_FILE_PATH}
- Branch: {BRANCH_NAME}
- Change name: {CHANGE_NAME}
- Tasks file: {TASKS_FILE_PATH}

Read and follow ALL steps in the command file at:
  docs/sdlc/commands/sdlc_implement_change.md

Follow the command exactly as written, including all interactive steps
(such as device gates).

IMPORTANT EXCEPTIONS:
- Do NOT suggest a commit message at the end (step 10) — the orchestrator handles commits.
- When the command says to "inform the user and stop" due to a blocker, you should
  still stop and report the issue — the orchestrator will relay it to the user.

When done, report:
1. Total tasks completed
2. Sections processed (with model assignments)
3. TODOs resolved vs acknowledged
4. Security review result
5. Any issues that require user attention
```

**After sub-agent returns:**
1. Read the tasks file and confirm all tasks are `[x]`.
2. Commit:
   ```
   git add -A
   git commit -m "{REF} Implement change

   Co-Authored-By: Claude <noreply@anthropic.com>"
   ```
3. If the sub-agent reported a blocker, relay it to the user and STOP.

---

### Step 4: Phase 4 — Verify and Archive

Spawn a sub-agent to execute the verify story command.

```
Agent(
  description: "Phase 4: Verify and archive story",
  model: "sonnet",
  prompt: "<constructed prompt>"
)
```

**Sub-agent prompt:**
```
You are executing the SDLC verify story command for: {STORY_ID}

Context from prior phases:
- Story ID: {STORY_ID}
- Story title: {STORY_TITLE}
- Story file: {USER_STORY_FILE_PATH}
- Branch: {BRANCH_NAME}
- Change name: {CHANGE_NAME}

Read and follow ALL steps in the command file at:
  docs/sdlc/commands/sdlc_verify_story.md

Follow the command exactly as written, including all interactive steps
(such as device gates and blocking on FAIL gates).

IMPORTANT EXCEPTIONS:
- Do NOT suggest a commit message at the end (step 18) — the orchestrator handles commits.
- When the command says to "inform the user and stop" due to a blocker, you should
  still stop and report the issue — the orchestrator will relay it to the user.

When done, report:
1. Each verification gate result (PASS/FAIL)
2. Coverage report table
3. Story closed (new filename)
4. Archive location
5. Any issues that require user attention
```

**After sub-agent returns:**
1. Confirm the story file has been renamed to `-DONE`.
2. Confirm the change has been archived.
3. Commit:
   ```
   git add -A
   git commit -m "{REF} Verify and archive story

   Co-Authored-By: Claude <noreply@anthropic.com>"
   ```
4. If the sub-agent reported a blocker, relay it to the user and STOP.

---

### Step 5: Final Summary

Display the summary:

```
## Vibe Complete

Story: {STORY_ID} — {STORY_TITLE}
Branch: {BRANCH_NAME}

| Phase | Status | Commit |
|-------|--------|--------|
| 1. Open & Refine | DONE | {REF} Open and refine user story |
| 2. Propose Change | DONE | {REF} Propose change with BDD tasks |
| 3. Implement | DONE | {REF} Implement change |
| 4. Verify & Archive | DONE | {REF} Verify and archive story |

Total commits: 4
Ready for: PR / merge to main
```
