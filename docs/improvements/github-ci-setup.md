# GitHub Actions CI Setup — Venice ("Heading to the Alps!")

## Context

The project has no CI. The goal is a GitHub Actions workflow that validates builds and tests on every PR to `main` and on pushes to `main`, catching regressions before they land.

---

## Approach

Create a single workflow file `.github/workflows/ci.yml` with **one job** containing sequential steps. A single job avoids duplicate setup/compilation across runners — since this is a single-module project, `check` already compiles everything that `assemble` needs.

---

## Workflow Structure

```
Trigger: push to main, PR to main
Concurrency: cancel in-progress runs on same ref

Job: build (ubuntu-latest)
  1. Checkout
  2. Set up JDK 21 (temurin) with Gradle cache
  3. Set up Android SDK (android-actions/setup-android@v3)
  4. chmod +x gradlew
  5. ./gradlew check          — compile + unit tests + detekt + lint
  6. ./gradlew assembleDebug assembleRelease  — build both APKs
```

---

## Key Decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Jobs | Single job | Avoids recompilation; `check` failure stops the job before `assemble` |
| JDK | Temurin 21 | AGP 9.x requires JDK 21+; cross-compiles to Java 11 bytecode via `compileOptions` |
| Android SDK | `android-actions/setup-android@v3` | Ensures up-to-date cmdline-tools; AGP 9.x auto-downloads compileSdk 36 |
| Caching | `actions/setup-java` `cache: gradle` | Simple, caches `~/.gradle/caches` and wrapper |
| MAPS_API_KEY | Let it default to `""` | `local.properties` is gitignored; `build.gradle.kts` handles absence gracefully |
| Release signing | Skip | `keystore.properties` guard means unsigned release build succeeds |
| Concurrency | `cancel-in-progress: true` | Saves runner minutes on superseded commits |
| Gradle flags | `--console=plain --stacktrace` | Clean CI logs, useful stack traces on failure |

---

## What's NOT Included (By Design)

- **Instrumented/emulator tests** — slow and flaky on CI, to be added later if needed
- **APK artifact upload** — not needed at this stage
- **Release signing** — no keystore on CI; unsigned release builds verify the build still works
- **Matrix builds** — single-module personal project, one configuration is sufficient

---

## Files to Create

- `.github/workflows/ci.yml` — the only deliverable

---

## Reference Files

- `app/build.gradle.kts` — compileSdk 36, signing guards, MAPS_API_KEY default, detekt config
- `gradle/wrapper/gradle-wrapper.properties` — Gradle 9.5.0
- `gradle.properties` — JVM args (`-Xmx2048m`), no CI-breaking settings
- `config/detekt/detekt.yml` — detekt rules + baseline

---

## Verification

1. Push the branch to GitHub and open a PR to `main` — the workflow should trigger automatically
2. Confirm the CI run passes: check step (tests + detekt) and assemble step (both APKs)
3. If SDK 36 auto-download fails, add an explicit `sdkmanager "platforms;android-36"` step as fallback

---

## Potential Issues

| Issue | Likelihood | Mitigation |
|-------|-----------|------------|
| compileSdk 36 not available for auto-download | Low | Add explicit `sdkmanager "platforms;android-36"` step |
| Gradle 9.5.0 cache compatibility with `setup-java` | Low | Fall back to `gradle/actions/setup-gradle@v4` |
| Detekt baseline file mismatch | Very low | Baseline is committed; will work as-is |
