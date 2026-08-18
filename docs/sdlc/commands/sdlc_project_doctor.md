Please run the project configuration health check.

This command verifies that the project's quality tooling (static analysis, coverage, tests, analytics tracking plan, CI/CD, deployment, Gradle wrapper) is properly configured. For SDLC framework checks (OpenSpec, security plugin, SDLC commands, guidelines), use `/sdlc_doctor` instead.

This command uses sub-agent orchestration: each check category is delegated to a Haiku sub-agent running in parallel. All checks are read-only — sub-agents must not modify any files, run builds, or install anything.

Follow these steps:

1. **Spawn all check agents in parallel.** Launch one Haiku sub-agent per category in a single message (all `Agent` tool calls in one response). Each sub-agent receives its category-specific checks and returns its results as a structured list.

   Use this `Agent` call pattern for every category:
   ```
   Agent(
     description: "Project Doctor: <Category>",
     model: "haiku",
     prompt: "<constructed prompt from the category section below>"
   )
   ```

   ### Sub-agent prompt template

   Use this template to construct the prompt for each sub-agent. Replace placeholders with actual values.

   ```
   You are running read-only health checks for the "{CATEGORY}" category of a
   project configuration doctor command. Do NOT modify any files or run builds.

   ## Checks

   {PASTE THE CHECK LIST FOR THIS CATEGORY}

   ## Output Format — CRITICAL

   Return ONLY a structured list of results. Each line must follow this exact format:
     ✅ <check description>
     ❌ <check description> — <one-liner explanation>

   Do not include any other text, commentary, or markdown headings.
   ```

   ### Category: Detekt

   Checks to include in the sub-agent prompt (for compliance with @docs/guidelines/guidelines-android.md):
   1. Verify `gradle/libs.versions.toml` declares a detekt plugin in the `[plugins]` section.
   2. Verify `app/build.gradle.kts` applies the detekt plugin (look for `alias(libs.plugins.detekt)` or equivalent).
   3. Verify the config file `config/detekt/detekt.yml` exists.
   4. **Require detekt 2.x or later.** Read the detekt version string from `gradle/libs.versions.toml`
      (the `[versions]` entry referenced by the detekt plugin's `version.ref`) and take the leading
      integer as the major — e.g. `1.23.8` → major 1, `2.0.0-alpha.5` → major 2.
      - Major **2 or higher**: PASS, naming the detected version.
      - Major **1**: **FAIL** — "detekt 1.x runs no type resolution and loads no custom rulesets on a
        current AGP; upgrade to 2.x." Include this detail in the explanation, because both failures are
        silent and the build stays green while nothing runs:
        * detekt 1.x registers its Android variant tasks (`detektDebug`, `detektRelease`) only when the
          `kotlin-android` plugin is applied, which does not happen when AGP supplies Kotlin itself, and
          its variant code targets `com.android.build.gradle.api.BaseVariant`, removed in AGP 8. Only the
          plain `detekt` task exists, so every rule needing a full classpath is inactive.
        * Custom rulesets published for 2.x register `META-INF/services/dev.detekt.api.RuleSetProvider`,
          while 1.x looks for `io.gitlab.arturbosch.detekt.api.RuleSetProvider`. The ruleset contributes
          zero rules with no error, and `validation` does not cover custom rulesets so nothing reports it.
        * Applying `kotlin-android` is **not** a workaround.
      - No version determinable: FAIL.
      **If this check fails, skip checks 5 and 6** and say so, so a project that is simply behind gets one
      actionable failure rather than a cascade of secondary ones.
   5. **Verify zero tolerance.** `config/detekt/detekt.yml` must set `warningsAsErrors: true` under the
      `config:` key, so warnings reach `error` severity and fail the build. Additionally verify that
      `maxIssues` does **not** appear anywhere in the file — the key was removed in detekt 2.x, so a
      leftover enforces nothing while looking like it does. Report FAIL for either condition.
   6. Read `config/detekt/detekt.yml` and verify `validation: true` is set under the `config:` key. Detekt
      then fails on any configuration key that does not exist in the running version, instead of ignoring
      it. Note in the check description that this does not cover custom rulesets.
   7. Verify `gradle/libs.versions.toml` declares a library referencing `io.nlopez.compose.rules` (Compose detekt rules).
   8. **Verify the Compose rules build matches the detekt major.** Read the `detekt-compose-rules` version
      from `gradle/libs.versions.toml`. When detekt is 2.x, that version must be **0.6.0 or later** — the
      0.6.x line is published against detekt 2.x, and earlier lines against 1.x. A mismatch in either
      direction means the ruleset silently registers zero rules, so `Compose: active: true` is honoured in
      config while nothing runs. Report FAIL with the detected pair if they do not match.
      > Maintenance note: this floor is a hardcoded mapping and needs revisiting when either project
      > releases a new major. It is kept because the failure it catches is otherwise invisible.

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

   ### Category: Analytics

   Checks to include in the sub-agent prompt (for compliance with @docs/guidelines/guidelines-analytics.md):
   1. Verify the tracking plan `docs/analytics/tracking-plan.md` exists. The analytics guidelines make it
      the single source of truth for which events exist; without it, event names get invented at each
      call site and the taxonomy drifts.
   2. Read `docs/analytics/tracking-plan.md` and verify it contains all three of these headings:
      `## Event Dictionary`, `## Parameter Reference`, `## User Properties`.
   3. Read `AGENTS.md` and verify it references `docs/analytics/tracking-plan.md`, so agents load it.
   4. Read `docs/sdlc/commands/sdlc_open_story.md` and verify **both**: that it lists
      `docs/guidelines/guidelines-analytics.md` among its Context Files, and that its refinement
      requirements include an analytics requirement (search for the string `## Analytics` within the
      refinement requirement list). This is the hook that makes every feature story define its own
      events; it can be silently removed by an SDLC sync, after which stories quietly stop declaring
      analytics with nothing failing.

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

2. **Collect results.** After all sub-agents complete, gather their output. Each sub-agent returns a list of `✅`/`❌` lines.

3. **Display the results.** Output the collected results grouped by category. Use this format:

    ```
    ## Project Doctor Results

    ### Detekt
      ✅ Detekt plugin declared in gradle/libs.versions.toml
      ❌ Detekt config file exists — config/detekt/detekt.yml not found
      ...

    ### Kover
      ✅ Kover plugin declared in gradle/libs.versions.toml
      ...

    ### Analytics
      ✅ docs/analytics/tracking-plan.md exists
      ❌ sdlc_open_story.md includes an analytics refinement requirement — not found
      ...

    ### Gradle
      ✅ Gradle wrapper resolves without errors

    (remaining categories)

    ---
    Summary: N/M checks passed, K failed.
    ```

    If all checks pass, add a closing line: `All checks passed. The project is properly configured.`

    If any checks failed, add: `**N check(s) failed.** Review the FAIL items above and fix them to ensure full project quality compliance.`
