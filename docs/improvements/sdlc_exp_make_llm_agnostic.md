Convert the SDLC commands to be LLM-agnostic so they work across multiple AI coding agents (Claude Code, Codex CLI, OpenCode, and future tools).

Currently, the SDLC commands in `docs/sdlc/commands/` are symlinked into each tool's command/skill directory (e.g. `.claude/commands/sdlc/`, `.codex/skills/sdlc-*/SKILL.md`, `.opencode/command/`), meaning all tools read the **exact same markdown files**. These files must therefore use LLM-agnostic language — no tool-specific syntax, model names, or skill invocation formats.

## Steps

### 1. Create init scripts for Codex and OpenCode

Check whether the init scripts below already exist. If they do not, create them by adapting the Claude init scripts (`docs/sdlc/sdlc_init_claude_code.sh` and `docs/sdlc/sdlc_init_claude_code.ps1`). If they already exist, verify they follow the conventions below and fix any issues.

All init scripts must be idempotent and report created/updated/skipped counts, matching the Claude init scripts' structure.

#### Codex CLI (`docs/sdlc/sdlc_init_codex.sh` and `docs/sdlc/sdlc_init_codex.ps1`)

- **Destination directory**: `.codex/skills/` instead of `.claude/commands/sdlc/`
- **Skill structure**: Each command becomes a directory with a `SKILL.md` symlink inside (e.g. `.codex/skills/sdlc-open-story/SKILL.md → docs/sdlc/commands/sdlc_open_story.md`). The directory name uses kebab-case derived from the command filename (underscores → hyphens, no `.md` extension).
- Codex reads `AGENTS.md` directly — no `CLAUDE.md` symlink needed.

#### OpenCode (`docs/sdlc/sdlc_init_opencode.sh` and `docs/sdlc/sdlc_init_opencode.ps1`)

- **Destination directory**: `.opencode/command/` for command files (flat files, not subdirectories)
- **Naming**: kebab-case derived from the command filename (underscores → hyphens), e.g. `sdlc-open-story.md`
- OpenCode reads `AGENTS.md` directly — no `CLAUDE.md` symlink needed.

#### Exclusion rules

**Claude's init script only** must skip:
- `sdlc_security_review.md` — Claude Code uses its native `/security-review` skill instead; this file is only for tools without a built-in security review capability

Codex and OpenCode init scripts **must link** `sdlc_security_review.md` as their security review command.

### 2. Scan all SDLC command files for LLM-specific patterns

Read every `.md` file in `docs/sdlc/commands/`. For each file, identify all instances of the following pattern categories:

#### Category A: Tool-specific skill/command invocations

Slash-command-style references to skills that use a specific tool's naming convention. Current occurrences:
- `/opsx:explore` — in `sdlc_propose_change.md`
- `/opsx:propose` — in `sdlc_propose_change.md`
- `/opsx:verify` — in `sdlc_verify_story.md`
- `/opsx:sync` — in `sdlc_implement_change.md` (classification table example), `sdlc_verify_story.md`
- `/opsx:archive` — in `sdlc_verify_story.md`
- `/security-review` — in `sdlc_exp_vibe_a_story.md`, `sdlc_verify_story.md`

**Replace with**: A generic description that lets each LLM resolve to its own registered skill/command. Use the pattern: `"Execute the <Skill Description> skill/command available in your environment"`.

Examples:
- `/opsx:explore` → `"the OpenSpec explore skill/command available in your environment"`
- `/security-review` → `"the security review skill/command available in your environment"`

Do NOT replace references to CLI tools invoked via shell commands (e.g. `openspec status --json`, `./gradlew test`) — these are already LLM-agnostic.

#### Category B: Tool-specific sub-agent invocation syntax

Direct references to a specific tool's agent spawning API. This includes both code blocks and surrounding prose. Examples:
- Code blocks: `Agent(description: "...", model: "...", prompt: "...")`
- Prose: "Use the `Agent` tool with the assigned `model` parameter"

**Replace with**: Natural language instructions describing the intent. The replacement must:
1. Describe what to do (spawn a sub-agent with this description, model tier, and prompt)
2. Mention that the agent should use whatever sub-agent spawning mechanism is available in its environment
3. Optionally list examples of specific tools as a hint (e.g. "e.g. the Agent tool in Claude Code, or equivalent in Codex CLI or OpenCode") — these are helpful, not prescriptive

#### Category C: Provider-specific model names

References to specific model names from any provider, in any form: code parameters, prose, display templates, introductory paragraphs. Examples:
- Code parameters: `model: "sonnet"`, `model: "haiku"`, `model: "opus"`, `model: "<sonnet|haiku>"`
- Prose: "Sonnet for reasoning, Haiku for mechanical tasks", "one Haiku sub-agent", "upgrade to Sonnet", "retry once with Opus"
- Display templates: `Section 0: ... → Sonnet`, `Sections processed: M (X with Sonnet, Y with Haiku)`
- OpenAI: `gpt-5.4`, `gpt-5.3-codex-spark`, `o3`

**Replace with**: Abstract capability tiers:
- **standard** — for tasks requiring reasoning (BDD test+implementation pairs, complex logic, mixed/unclear tasks)
- **fast** — for mechanical tasks (wiring, previews, string resources, command execution, simple verification)
- **reasoning** — for tasks requiring deep analysis (product analysis, architecture exploration, design decisions)

Update all occurrences, including:
- Classification tables (e.g. "Haiku" column → "fast" column)
- Execution plan displays (e.g. `→ Sonnet` → `→ standard`)
- Retry escalation logic (e.g. "If Haiku, escalate to Sonnet" → "If fast, escalate to standard")
- Summary outputs (e.g. "X with Sonnet, Y with Haiku" → "X with standard, Y with fast")
- Introductory paragraphs (e.g. "using cheaper models (Sonnet/Haiku)" → "using cheaper model tiers (standard/fast)")

#### Category D: Tool-specific file references or configuration

References to tool-specific file paths or configuration that assume a single tool.

**General principle:** `CLAUDE.md` is just a symlink to `AGENTS.md`. All references to `CLAUDE.md` in SDLC commands must be replaced with `AGENTS.md` — it is the universal project instruction file that all tools read (directly or via symlink).

Current occurrences:
- `sdlc_verify_story.md`: symlink warnings `"Read AGENTS.md (NOT CLAUDE.md — it may be a symlink)"` and `"Do NOT write through symlinks. If CLAUDE.md is a symlink to AGENTS.md..."` — remove these entirely; simply say "Read and update AGENTS.md"
- `sdlc_doctor.md`: `.claude/settings.json` plugin check (checking `security-guidance@claude-plugins-official`) — this is tool-specific and belongs in the doctor's Claude-only check branch (see Step 5), not in generic commands

Note: `sdlc_implement_change.md` already uses `"Update AGENTS.md"` (not `CLAUDE.md`), so no change is needed there.

**Replace with**: LLM-agnostic equivalents:
- Replace every `CLAUDE.md` reference with `AGENTS.md`
- Remove symlink-related warnings and caveats entirely
- Plugin/settings checks that are tool-specific belong only in the tool-aware `sdlc_doctor` (see Step 5), not in individual commands

#### Category E: Tool-specific execution semantics

References to a specific tool's execution mechanisms that go beyond sub-agent syntax. Current occurrences:
- Parallel dispatch: `"all Agent tool calls in one response"` — in `sdlc_doctor.md`, `sdlc_project_doctor.md`
- Skill tool invocation: `"Use the Skill tool to invoke the 'security-review' skill"` — in `sdlc_verify_story.md`
- Tool-specific API references: `"Use the Agent tool with..."` — in `sdlc_implement_change.md`

**Replace with**: Natural language describing the intent:
- Parallel dispatch: "Spawn all check agents in parallel using whatever parallel sub-agent mechanism is available in your environment"
- Skill invocation: "Execute the security review skill/command available in your environment"
- Tool API references: "Spawn a sub-agent using whatever mechanism is available in your environment"

### 3. Apply replacements

For each pattern found in step 2, apply the replacement. After each file is modified:
- Re-read it to confirm no LLM-specific patterns remain
- Verify the instructions still make sense and are actionable

### 4. Create `sdlc_security_review.md`

Create `docs/sdlc/commands/sdlc_security_review.md` as a portable security review command for tools that lack a built-in security review capability (Codex CLI, OpenCode, and future tools).

The file should:
- Describe the intent: "Run a security review of pending changes on the current branch"
- Contain a placeholder implementation with a TODO explaining it needs to be fully fleshed out
- Outline the basic scope: scan for common vulnerability patterns (OWASP top 10), check for secrets/credentials in committed files, review input validation and output encoding
- Note that Claude Code users use the native `/security-review` skill instead of this command

Init script handling (already covered in Step 1):
- Claude's init script excludes this file
- Codex and OpenCode init scripts link it as a regular command

### 5. Restructure `sdlc_doctor.md` for multi-tool support

The doctor command is structurally different from other commands — its job IS to check tool-specific setup, so it cannot simply replace tool references with agnostic language.

**Approach:**
1. **Self-identify**: The agent identifies which AI coding tool it is running as
2. **Report**: Tell the user which tool was detected (e.g. "Checking SDLC installation for Claude Code...")
3. **Check tool-specific paths**: Run only the checks relevant to that tool

**Implementation:** Replace the current hardcoded Claude-only check lists with a decision tree:

- **Shared checks (all tools):**
    - `openspec/config.yaml` exists
    - OpenSpec CLI is installed (`command -v openspec`)

- **If Claude Code:**
    - Check `.claude/commands/opsx/` skill files exist (explore, propose, apply, verify, sync, archive, onboard)
    - Check `.claude/commands/sdlc/` command files exist (all SDLC commands)
    - Check `.claude/settings.json` has `security-guidance@claude-plugins-official` enabled
    - `sdlc_security_review.md` should NOT be linked (Claude uses native skill)

- **If Codex CLI:**
    - Check `.codex/skills/sdlc-*/SKILL.md` files exist
    - Check `.codex/skills/openspec-*/SKILL.md` files exist
    - Check `sdlc_security_review` skill is linked

- **If OpenCode:**
    - Check `.opencode/command/sdlc-*.md` files exist
    - Check `.opencode/skills/openspec-*/SKILL.md` files exist
    - Check `sdlc_security_review` command is linked

### 6. Create placeholder skills for Codex and OpenCode

Check whether the following skills exist for Codex and OpenCode. For any that are missing, create a minimal placeholder with a TODO note explaining that the content needs to be defined.

**Codex (`security-review`):** `.codex/skills/security-review/SKILL.md` — symlink to `docs/sdlc/commands/sdlc_security_review.md`

**OpenCode (`security-review`):** `.opencode/skills/security-review/SKILL.md` — symlink to `docs/sdlc/commands/sdlc_security_review.md`

Repeat for any other skill referenced by the SDLC commands that exists in Claude Code but not in Codex or OpenCode.

### 7. Update README.md

Read `docs/sdlc/README.md` and update it to reflect multi-tool support:

- Replace provider-specific model recommendations (e.g. "Recommended agent: Opus") with tier-based recommendations (e.g. "Recommended model tier: reasoning")
- Replace model names in command description prose (e.g. "delegates refinement to Opus sub-agent" → "delegates refinement to a reasoning-tier sub-agent", "Haiku sub-agents run the checks" → "fast-tier sub-agents run the checks")
- Add Codex CLI and OpenCode setup instructions alongside Claude Code in the Quick Start section
- Update the "currently only Claude is supported" statement (Quick Start section) to reflect multi-tool support
- Update the TODOs section: remove "Add support for Cursor, OpenCOde" (Codex/OpenCode now supported; Cursor can be a separate improvement)
- Generalise feature descriptions that reference a specific tool's capabilities (e.g. "Claude Code's security scanners" → "security scanners")
- Update the "Additional customisations" section: replace all `CLAUDE.md` references with `AGENTS.md`; explain that `AGENTS.md` is the universal project instruction file and each tool reads it (Claude Code via a `CLAUDE.md` symlink, others directly)
- Update the "Repeating errors to fix" section: the `CLAUDE.md` symlink error is Claude-specific — reframe or move it to a Claude-specific notes section
- Update the features table: "Sub-agent orchestration" row's description uses "Sonnet for reasoning, Haiku for mechanical tasks" — replace with tier names ("standard for reasoning, fast for mechanical tasks")
- Add a **Tier Mapping** reference table showing how abstract tiers map to each provider's models:

  | Tier | Intent | Claude Code | Codex CLI | OpenCode |
      |------|--------|-------------|-----------|----------|
  | fast | Mechanical tasks | haiku | (tool's cheapest) | (tool's cheapest) |
  | standard | Reasoning tasks | sonnet | (tool's default) | (tool's default) |
  | reasoning | Deep analysis | opus | (tool's most capable) | (tool's most capable) |

  The command files just say "fast", "standard", or "reasoning" — each tool resolves to its own model.

### 8. Verify

Run a final scan across all files in `docs/sdlc/commands/` and `docs/sdlc/README.md`:

```bash
grep -rn -i -E '/opsx:|/security-review|Agent\(|"sonnet"|"haiku"|"opus"|`sonnet`|`haiku`|`opus`|Sonnet|Haiku|Opus|model:\s*"(sonnet|haiku|opus)"|\.claude/|CLAUDE\.md|Skill tool|\.codex/|\.opencode/' docs/sdlc/commands/ docs/sdlc/README.md
```

The only acceptable matches are:
- References inside example tool hints (e.g. "e.g. the Agent tool in Claude Code")
- References in the Quick Start section where tool-specific setup is expected (init script names, `.claude/` paths, `.codex/` paths, `.opencode/` paths)
- References in the Tier Mapping table (provider-specific model names as column values)
- The `sdlc_security_review.md` file itself (it's allowed to reference Claude Code's native skill by name for context)
- The `sdlc_doctor.md` file's tool-specific check lists (these are inherently tool-aware by design)

If any other matches are found, fix them.

### 9. Report

Display a summary of all changes made:
- Number of files modified
- Number of patterns replaced per category (A/B/C/D/E)
- Init scripts created or updated
- `sdlc_security_review.md` created
- `sdlc_doctor.md` restructured
- Placeholder skills created
- Any issues found that could not be automatically resolved

### 10. OpenCode Open points not covered above

The following gaps were identified during the initial analysis and are not addressed by steps 1–9. They should be resolved before or alongside the main migration.

#### 10.1 OpenCode permission configuration

OpenCode requires explicit permission rules in `opencode.json` for the agent to execute shell commands and skills. The Claude Code equivalent (`.claude/settings.local.json`) is replaced by a `permission` block. The init scripts must either:
- Create or update `opencode.json` with the required permissions, or
- Document that the user must add them manually.

Minimum required permissions for SDLC commands:
```json
{
  "permission": {
    "bash": {
      "openspec *": "allow",
      "./gradlew *": "allow",
      "git *": "allow",
      "adb *": "allow",
      "command -v *": "allow"
    },
    "skill": {
      "*": "allow"
    }
  }
}
```

This applies to both the global (`~/.config/opencode/opencode.json`) and project-level config files.

#### 10.2 Parallel dispatch feasibility

Category E instructs agents to "spawn all check agents in parallel using whatever parallel sub-agent mechanism is available." However, OpenCode's `Task` tool does not support spawning multiple sub-agents in a single model response — sub-agents execute sequentially. This affects `sdlc_doctor.md` and `sdlc_project_doctor.md`, which dispatch 3–7 parallel checks.

**Options:**
a) Document that OpenCode runs checks sequentially (slower but correct).
b) Implement a batching workaround (one sub-agent runs all checks within a category rather than one sub-agent per category).
c) Check if OpenCode plans to add parallel sub-agent dispatch (issue [#20059](https://github.com/anomalyco/opencode/issues/20059) tracks custom subagent support but not parallel dispatch).

Recommendation: option (b) — restructure doctor commands so each sub-agent runs all checks for a category instead of spawning one sub-agent per check. This eliminates the need for parallelism entirely.

#### 10.3 `@file` auto-attach behaviour

Claude Code auto-attaches `@referenced-file.md` to the prompt before it reaches the LLM. OpenCode V2 treats `@path` as plain text — the file is not automatically included.

This is a **minor concern** because the SDLC commands already include explicit reading instructions alongside `@` references (e.g. "Read the guidelines at: docs/guidelines/guidelines-userstories.md"). The LLM will use its Read tool regardless. No action required beyond noting the difference in a comment or README.

#### 10.4 Tier-to-model mapping for OpenCode Go

The Tier Mapping table in Step 7 uses placeholders (`(tool's cheapest)`, `(tool's default)`, `(tool's most capable)`). Since the Go subscription has been chosen as the target, these can be pre-filled:

| Tier | Intent | Claude Code | OpenCode Go |
|------|--------|-------------|-------------|
| fast | Mechanical tasks | haiku | DeepSeek V4 Flash or MiMo-V2.5 |
| standard | Reasoning tasks | sonnet | DeepSeek V4 Pro or Qwen3.7 Plus |
| reasoning | Deep analysis | opus | GLM-5.2 or Kimi K3 |

This gives users concrete guidance without requiring them to research model tiers.


# 11. Phase 2 – Provider-Agnostic Architecture Improvements

The following architectural improvements are **not required** for the initial LLM-agnostic migration, but would significantly improve the framework's long-term maintainability, portability and extensibility.

The goal is to evolve the SDLC from a framework that supports multiple AI coding agents into a provider-agnostic software development framework with thin adapters for each supported tool.

## 11.1 Introduce Capability Abstractions

Abstract capabilities (parallel analysis, shell execution, patch application, repository search, browser/MCP access, etc.) rather than assuming specific tool features. Provider adapters should map these capabilities to native implementations.

## 11.2 Add Feature Negotiation

Allow each provider to advertise supported capabilities (sub-agents, shell, git, browser, MCP, parallel execution, etc.) so workflows adapt automatically instead of containing provider-specific conditionals.

## 11.3 Introduce Provider Adapters

Centralise provider-specific behaviour into adapter modules (e.g. `providers/claude`, `providers/codex`, `providers/opencode`) so SDLC commands never need to know which provider is executing them.

## 11.4 Optimise for Context Budget

Review every command with the question:

> What is the minimum context required to complete this task?

Prefer minimal context over loading documentation "just in case".

## 11.5 Reduce Prompt Size

Perform a dedicated optimisation pass to:
- remove duplicated instructions
- centralise shared guidance
- replace repeated prose with conventions where appropriate

This should reduce token usage while preserving behaviour.

## 11.6 Standardise Output Contracts

Where practical, define explicit output structures (Summary, Implementation, Verification, Risks, Next Steps, etc.) instead of relying on free-form responses. Consistent outputs improve automation and portability.

## 11.7 Prefer Workflow Contracts over Prompt Contracts

Commands should primarily define:
- Inputs
- Outputs
- Constraints
- Success Criteria

rather than prescribing how the model should reason internally.

## 11.8 Add Cross-Provider Behavioural Testing

Complement static validation with behavioural testing by executing representative SDLC workflows across multiple providers and comparing:
- generated plans
- implementations
- verification reports
- output structure

## 11.9 Separate Process from AI Behaviour

Keep software development process independent from provider execution details.

For example, specify:

> Analyse the solution from three independent perspectives.

instead of:

> Spawn three reasoning agents.

The provider adapter can determine how best to execute the workflow.

## 11.10 Establish a Long-Term Provider-Agnostic Architecture

Treat the SDLC as an AI-independent specification.

Provider-specific responsibilities should be limited to:
- model selection
- capability mapping
- tool invocation
- environment configuration

Adding support for a new provider should primarily involve implementing a new adapter rather than modifying SDLC commands.

## Overall Vision

The initial migration transforms the SDLC from a Claude-specific framework into a multi-provider framework.

These architectural improvements represent a second phase that transforms the SDLC into a provider-agnostic software development specification.

Desired layering:

```
SDLC
    ↓
Workflow Contracts
    ↓
Capability Abstractions
    ↓
Provider Adapter
    ↓
AI Coding Tool
```

With this architecture, the SDLC remains stable while providers evolve independently, reducing future maintenance effort and making support for additional AI coding agents substantially easier.
