Convert the SDLC commands to be LLM-agnostic so they work across multiple AI coding agents (Claude Code, Codex CLI, and future tools).

Currently, the SDLC commands in `docs/sdlc/commands/` are symlinked into both `.claude/commands/sdlc/` and `.codex/skills/sdlc-*/SKILL.md`, meaning both tools read the **exact same markdown files**. These files must therefore use LLM-agnostic language — no tool-specific syntax, model names, or skill invocation formats.

## Steps

### 1. Create Codex init scripts

Check whether `docs/sdlc/sdlc_init_codex.sh` and `docs/sdlc/sdlc_init_codex.ps1` already exist. If they do not exist, create them by adapting the Claude init scripts (`docs/sdlc/sdlc_init_claude_code.sh` and `docs/sdlc/sdlc_init_claude_code.ps1`) with these differences:

- **Destination directory**: `.codex/skills/` instead of `.claude/commands/sdlc/`
- **Skill structure**: Each command becomes a directory with a `SKILL.md` symlink inside (e.g. `.codex/skills/sdlc-open-story/SKILL.md → docs/sdlc/commands/sdlc_open_story.md`). The directory name uses kebab-case derived from the command filename (underscores → hyphens, no `.md` extension).
- **No CLAUDE.md symlink**: Codex reads `AGENTS.md` directly, so do not create a `CLAUDE.md → AGENTS.md` symlink.
- The scripts should follow the same structure and conventions as the Claude init scripts (idempotent, reports created/updated/skipped counts).

If the scripts already exist, verify they follow these conventions and fix any issues.

### 2. Scan all SDLC command files for LLM-specific patterns

Read every `.md` file in `docs/sdlc/commands/`. For each file, identify all instances of the following pattern categories:

#### Category A: Tool-specific skill/command invocations

Slash-command-style references to skills that use a specific tool's naming convention. Examples:
- Claude Code: `/opsx:explore`, `/opsx:propose`, `/opsx:verify`, `/opsx:sync`, `/opsx:archive`
- Claude Code: `/security-review`, `/code-review`

**Replace with**: A generic description that lets each LLM resolve to its own registered skill. Use the pattern: `"Execute the <Skill Description> skill available in your environment"`.

Examples:
- `/opsx:explore` → `"the OpenSpec explore skill available in your environment"`
- `/security-review` → `"the security-review skill available in your environment"`

Do NOT replace references to CLI tools invoked via shell commands (e.g. `openspec status --json`, `./gradlew test`) — these are already LLM-agnostic.

#### Category B: Tool-specific sub-agent invocation syntax

Direct references to a specific tool's agent spawning API. Examples:
- Claude Code: `Agent(description: "...", model: "...", prompt: "...")`
- Codex CLI: `spawn_agent(...)`

**Replace with**: Natural language instructions describing the intent. The replacement must:
1. Describe what to do (spawn a sub-agent with this description, model tier, and prompt)
2. Mention that the agent should use whatever sub-agent spawning mechanism is available in its environment
3. Optionally list examples of specific tools as a hint (e.g. "e.g. the Agent tool in Claude Code, or the spawn_agent tool in Codex CLI") — these are helpful, not prescriptive

#### Category C: Provider-specific model names

References to specific model names from any provider. Examples:
- Anthropic: `Sonnet`, `Haiku`, `Opus`, `sonnet`, `haiku`, `opus`
- OpenAI: `gpt-5.4`, `gpt-5.3-codex-spark`, `o3`

**Replace with**: Abstract capability tiers:
- **standard** — for tasks requiring reasoning (BDD test+implementation pairs, complex logic, mixed/unclear tasks)
- **fast** — for mechanical tasks (wiring, previews, string resources, command execution, simple verification)
- **reasoning** — for tasks requiring deep analysis (product analysis, architecture exploration, design decisions)

Update any classification tables, execution plan displays, and retry escalation logic to use these tier names (e.g. "fast → standard" instead of "Haiku → Sonnet").

#### Category D: Tool-specific file references or configuration

References to tool-specific file paths or configuration that assume a single tool. Examples:
- `"Update CLAUDE.md"` (should be `"Update AGENTS.md"` — both tools read it)
- References to `.claude/settings.local.json` as the only permission model

**Replace with**: LLM-agnostic equivalents. `AGENTS.md` is the universal project instruction file; reference it instead of tool-specific variants.

### 3. Apply replacements

For each pattern found in step 2, apply the replacement. After each file is modified:
- Re-read it to confirm no LLM-specific patterns remain
- Verify the instructions still make sense and are actionable

### 4. Create placeholder skills for Codex

Check whether a `security-review` skill exists for Codex at `.codex/skills/security-review/SKILL.md`. If not, create a minimal placeholder with a TODO note explaining that the content needs to be defined. This ensures the "execute the security-review skill available in your environment" instruction can resolve in Codex.

Repeat for any other skill referenced by the SDLC commands that exists in Claude Code but not in Codex.

### 5. Update README.md

Read `docs/sdlc/README.md` and update it to reflect multi-tool support:
- Replace provider-specific model recommendations (e.g. "Recommended agent: Opus") with tier-based recommendations (e.g. "Recommended model tier: Use a reasoning-capable model")
- Add Codex CLI setup instructions alongside Claude Code in the Quick Start section
- Update any "currently only X is supported" statements
- Generalise any feature descriptions that reference a specific tool's capabilities (e.g. "Claude Code's security scanners" → "security scanners")

### 6. Verify

Run a final scan across all files in `docs/sdlc/commands/` and `docs/sdlc/README.md`:

```bash
grep -rn -i -E '/opsx:|/security-review|Agent\(|"sonnet"|"haiku"|"opus"|`sonnet`|`haiku`|`opus`|Sonnet|Haiku|Opus' docs/sdlc/commands/ docs/sdlc/README.md
```

The only acceptable matches are:
- References inside example tool hints (e.g. "e.g. the Agent tool in Claude Code")
- References in the Quick Start section where tool-specific setup is expected (init script names, `.claude/` paths, `.codex/` paths)

If any other matches are found, fix them.

### 7. Report

Display a summary of all changes made:
- Number of files modified
- Number of patterns replaced per category (A/B/C/D)
- Any placeholder skills created
- Any issues found that could not be automatically resolved
