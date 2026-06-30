Please run the SDLC framework health check.

This command verifies that the SDLC tooling (OpenSpec, security review, SDLC commands) is properly installed and configured for the current AI coding tool. For project-specific quality checks (Detekt, Kover, tests, CI/CD, Fastlane, Gradle wrapper), use `/sdlc_project_doctor` instead.

This command uses sub-agent orchestration: each check category is delegated to a sub-agent using the **fast** model tier, running in parallel. All checks are read-only — sub-agents must not modify any files, run builds, or install anything.

Follow these steps:

1. **Self-identify.** Determine which AI coding tool you are running as (e.g. Claude Code, Codex CLI, OpenCode, or other). Report to the user: `"Checking SDLC installation for <tool name>..."`

2. **Spawn all check agents in parallel.** Launch one sub-agent per category using whatever parallel sub-agent mechanism is available in your environment. Use the **fast** model tier for all sub-agents. Each sub-agent receives its category-specific checks and returns its results as a structured list.

   ### Sub-agent prompt template

   Use this template to construct the prompt for each sub-agent. Replace placeholders with actual values.

   ```
   You are running read-only health checks for the "{CATEGORY}" category of an
   SDLC framework doctor command. Do NOT modify any files or run builds.

   ## Checks

   {PASTE THE CHECK LIST FOR THIS CATEGORY}

   ## Output Format — CRITICAL

   Return ONLY a structured list of results. Each line must follow this exact format:
     ✅ <check description>
     ❌ <check description> — <one-liner explanation>

   Do not include any other text, commentary, or markdown headings.
   ```

   ### Category: OpenSpec (all tools)

   Checks to include in the sub-agent prompt:
   1. Check that `openspec/config.yaml` exists.
   2. Run `command -v openspec` to confirm the OpenSpec CLI is installed and available in PATH.

   ### Category: Tool-Specific Setup

   Run only the checks relevant to the detected tool:

   #### If Claude Code:
   1. Check that each of these OpenSpec skill files exists:
      - `.claude/commands/opsx/explore.md`
      - `.claude/commands/opsx/propose.md`
      - `.claude/commands/opsx/apply.md`
      - `.claude/commands/opsx/verify.md`
      - `.claude/commands/opsx/sync.md`
      - `.claude/commands/opsx/archive.md`
   2. Check that each of these SDLC command files exists:
      - `.claude/commands/sdlc/sdlc_open_story.md`
      - `.claude/commands/sdlc/sdlc_propose_change.md`
      - `.claude/commands/sdlc/sdlc_implement_change.md`
      - `.claude/commands/sdlc/sdlc_verify_story.md`
      - `.claude/commands/sdlc/sdlc_doctor.md`
      - `.claude/commands/sdlc/sdlc_project_doctor.md`
   3. Check that `sdlc_security_review.md` is NOT linked in `.claude/commands/sdlc/` (Claude Code uses its native security review skill instead).
   4. Read `.claude/settings.json` and verify that `enabledPlugins` contains the key `security-guidance@claude-plugins-official` set to `true`.

   #### If Codex CLI:
   1. Check that each of these OpenSpec skill files exists:
      - `.codex/skills/openspec-explore/SKILL.md`
      - `.codex/skills/openspec-propose/SKILL.md`
      - `.codex/skills/openspec-apply-change/SKILL.md`
      - `.codex/skills/openspec-verify-change/SKILL.md`
      - `.codex/skills/openspec-sync-specs/SKILL.md`
      - `.codex/skills/openspec-archive-change/SKILL.md`
   2. Check that `.codex/skills/sdlc-*/SKILL.md` files exist for each SDLC command.
   3. Check that the `sdlc-security-review` skill is linked (`.codex/skills/sdlc-security-review/SKILL.md` exists).

   #### If OpenCode:
   1. Check that each of these OpenSpec skill files exists:
      - `.opencode/skills/openspec-explore/SKILL.md`
      - `.opencode/skills/openspec-propose/SKILL.md`
      - `.opencode/skills/openspec-apply-change/SKILL.md`
      - `.opencode/skills/openspec-verify-change/SKILL.md`
      - `.opencode/skills/openspec-sync-specs/SKILL.md`
      - `.opencode/skills/openspec-archive-change/SKILL.md`
   2. Check that `.opencode/command/sdlc-*.md` files exist for each SDLC command.
   3. Check that the `sdlc-security-review` command is linked (`.opencode/command/sdlc-security-review.md` exists).

3. **Collect results.** After all sub-agents complete, gather their output. Each sub-agent returns a list of `✅`/`❌` lines.

4. **Display the results.** Output the collected results grouped by category. Use this format:

    ```
    ## SDLC Doctor Results

    Detected tool: <tool name>

    ### OpenSpec
      ✅ openspec/config.yaml exists
      ✅ openspec CLI is installed
      ...

    ### Tool-Specific Setup (<tool name>)
      ✅ .claude/commands/opsx/explore.md exists
      ❌ .claude/commands/opsx/apply.md exists — file not found
      ...

    ---
    Summary: N/M checks passed, K failed.
    ```

    If all checks pass, add a closing line: `All checks passed. The SDLC framework is properly configured.`

    If any checks failed, add: `**N check(s) failed.** Review the FAIL items above and fix them to ensure full SDLC workflow compatibility.`
