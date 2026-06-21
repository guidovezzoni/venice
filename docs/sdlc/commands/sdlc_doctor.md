Please run the SDLC project health check.

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
   SDLC project doctor command. Do NOT modify any files or run builds.

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

   ### Category: Detekt

   Checks to include in the sub-agent prompt (for compliance with @docs/guidelines/guidelines-android.md):
   1. Verify `gradle/libs.versions.toml` declares a plugin with id `io.gitlab.arturbosch.detekt` in the `[plugins]` section.
   2. Verify `app/build.gradle.kts` applies the detekt plugin (look for `alias(libs.plugins.detekt)` or equivalent).
   3. Verify the config file `config/detekt/detekt.yml` exists.
   4. Read `config/detekt/detekt.yml` and verify `maxIssues` is set to `0` under the `build:` key.
   5. Verify `gradle/libs.versions.toml` declares a library referencing `io.nlopez.compose.rules` (Compose detekt rules).

   ### Category: Kover

   Checks to include in the sub-agent prompt (for compliance with @docs/guidelines/guidelines-android.md):
   1. Verify `gradle/libs.versions.toml` declares a plugin with id `org.jetbrains.kotlinx.kover` in the `[plugins]` section.
   2. Verify `app/build.gradle.kts` applies the kover plugin (look for `alias(libs.plugins.kover)` or equivalent).
   3. Read `app/build.gradle.kts` and verify a kover verify rule enforces a minimum bound of 95 (look for `minBound(95)` or equivalent in the `kover` configuration block).

   ### Category: Unit Tests

   Checks to include in the sub-agent prompt (for compliance with @docs/guidelines/guidelines-android.md):
   1. Verify `gradle/libs.versions.toml` declares a JUnit 4 library (`junit`).
   2. Verify `gradle/libs.versions.toml` declares a MockK library (`io.mockk`).
   3. Verify `gradle/libs.versions.toml` declares a kotlinx-coroutines-test library (`kotlinx-coroutines-test`).
   4. Verify the directory `app/src/test/` exists and contains at least one `.kt` file (search recursively).

   ### Category: Fastlane

   Checks to include in the sub-agent prompt:
   1. Verify `fastlane/Fastfile` exists.
   2. Verify `Gemfile` exists.
   3. Run `command -v fastlane` to check that the `fastlane` command is available in PATH.
   4. Run `command -v bundle` to check that `bundler` is available in PATH.

   ### Category: CI/CD

   Checks to include in the sub-agent prompt:
   1. Verify `.github/workflows/ci.yml` exists.
   2. Read `.github/workflows/ci.yml` and verify it contains a step that runs `./gradlew check` (look for the string `gradlew check` in a `run:` value).
   3. Read `.github/workflows/ci.yml` and verify it contains a step that runs `./gradlew koverVerify` (look for the string `gradlew koverVerify` in a `run:` value).

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
      [FAIL] .claude/commands/opsx/apply.md exists — file not found
      ...

    ### Security Review
      [PASS] security-guidance plugin is enabled in .claude/settings.json

    ### Detekt
      ...

    (remaining categories)

    ---
    Summary: N/M checks passed, K failed.
    ```

    If all checks pass, add a closing line: `All checks passed. The project is properly configured for the SDLC workflow.`

    If any checks failed, add: `N check(s) failed. Review the FAIL items above and fix them to ensure full SDLC workflow compatibility.`
