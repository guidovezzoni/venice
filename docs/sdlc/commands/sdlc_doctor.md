Please run the SDLC framework health check.

This command verifies that the SDLC tooling (OpenSpec, security plugin, Gradle wrapper) is properly installed and configured. For project-specific quality checks (Detekt, Kover, tests, CI/CD, Fastlane), use `/sdlc_project_doctor` instead.

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
     [PASS] <check description>
     [FAIL] <check description> — <one-liner explanation>

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

   ### Category: Gradle

   Checks to include in the sub-agent prompt:
   1. Run `./gradlew tasks --quiet` to verify the Gradle wrapper is working and the project configuration resolves without errors. If the command exits with a non-zero status, record FAIL with the first line of stderr as the explanation. Note: this is the only check that executes a build tool; `tasks --quiet` is lightweight and does not compile code.

2. **Collect results.** After all sub-agents complete, gather their output. Each sub-agent returns a list of `[PASS]`/`[FAIL]` lines.

3. **Display the results.** Output the collected results grouped by category. Use this format:

    ```
    ## SDLC Doctor Results

    ### OpenSpec
      [PASS] openspec/config.yaml exists
      [PASS] openspec CLI is installed
      [PASS] .claude/commands/opsx/explore.md exists
      **[FAIL] .claude/commands/opsx/apply.md exists — file not found**
      ...

    ### Security Review
      [PASS] security-guidance plugin is enabled in .claude/settings.json

    ### Gradle
      ...

    ---
    Summary: N/M checks passed, K failed.
    ```

    IMPORTANT: every `[FAIL]` line must be wrapped in bold markdown (`**...**`) so it stands out visually.

    If all checks pass, add a closing line: `All checks passed. The SDLC framework is properly configured.`

    If any checks failed, add: `**N check(s) failed.** Review the FAIL items above and fix them to ensure full SDLC workflow compatibility.`
