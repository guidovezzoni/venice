Please run the SDLC framework health check.

This command verifies that the SDLC tooling (OpenSpec, security plugin, SDLC commands) is properly installed and configured. For project-specific quality checks (Detekt, Kover, tests, CI/CD, Fastlane, Gradle wrapper), use `/sdlc_project_doctor` instead.

This command uses sub-agent orchestration: each check category is delegated to a Haiku sub-agent running in parallel. All checks are read-only — sub-agents must not modify any files, run builds, or install anything.

Follow these steps:

1. **Spawn all check agents in parallel.** Launch one Haiku sub-agent per category in a single message (all `Agent` tool calls in one response). Each sub-agent receives its category-specific checks and returns its results as a structured list.

   Use this `Agent` call pattern for every category:
   ```
   Agent(
     description: "Doctor: <Category>",
     model: "haiku",
     prompt: "<constructed prompt from the category section below>"
   )
   ```

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

   ### Category: OpenSpec

   Checks to include in the sub-agent prompt:
   1. Check that `openspec/config.yaml` exists.
   2. Run `command -v openspec` to confirm the OpenSpec CLI is installed and available in PATH.
   3. Check that each of these command files exists:
      - `.claude/commands/opsx/explore.md`
      - `.claude/commands/opsx/propose.md`
      - `.claude/commands/opsx/apply.md`
      - `.claude/commands/opsx/verify.md`
      - `.claude/commands/opsx/sync.md`
      - `.claude/commands/opsx/archive.md`

   ### Category: Security Review

   Checks to include in the sub-agent prompt:
   1. Read `.claude/settings.json` and verify that `enabledPlugins` contains the key `security-guidance@claude-plugins-official` set to `true`.

   ### Category: SDLC Commands

   Checks to include in the sub-agent prompt:
   1. Check that each of these command files exists:
      - `.claude/commands/sdlc/sdlc_open_story.md`
      - `.claude/commands/sdlc/sdlc_propose_change.md`
      - `.claude/commands/sdlc/sdlc_implement_change.md`
      - `.claude/commands/sdlc/sdlc_verify_story.md`
      - `.claude/commands/sdlc/sdlc_doctor.md`
      - `.claude/commands/sdlc/sdlc_project_doctor.md`

2. **Collect results.** After all sub-agents complete, gather their output. Each sub-agent returns a list of `✅`/`❌` lines.

3. **Display the results.** Output the collected results grouped by category. Use this format:

    ```
    ## SDLC Doctor Results

    ### OpenSpec
      ✅ openspec/config.yaml exists
      ✅ openspec CLI is installed
      ✅ .claude/commands/opsx/explore.md exists
      ❌ .claude/commands/opsx/apply.md exists — file not found
      ...

    ### Security Review
      ✅ security-guidance plugin is enabled in .claude/settings.json

    ### SDLC Commands
      ✅ .claude/commands/sdlc/sdlc_open_story.md exists
      ❌ .claude/commands/sdlc/sdlc_doctor.md exists — file not found
      ...

    ---
    Summary: N/M checks passed, K failed.
    ```

    If all checks pass, add a closing line: `All checks passed. The SDLC framework is properly configured.`

    If any checks failed, add: `**N check(s) failed.** Review the FAIL items above and fix them to ensure full SDLC workflow compatibility.`
