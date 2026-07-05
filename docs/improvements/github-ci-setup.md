# GitHub Actions CI/CD + Fastlane Setup — Venice ("Heading to Venice!")

## Context

The project has no CI/CD. The goal is a GitHub Actions pipeline that:
1. Validates builds and tests on every PR/push to `main` (CI)
2. Deploys signed release builds to Google Play Store via Fastlane (CD)

---

## Architecture

Two workflows + Fastlane:

| Workflow | Trigger | Secrets | Purpose |
|----------|---------|---------|---------|
| `ci.yml` | push/PR to `main` | None | Compile, unit tests, detekt, lint, assemble both APKs |
| `deploy.yml` | Manual dispatch or `v*` tag | All 6 | Build signed AAB, upload to Play Store via Fastlane |

---

## CI Workflow (`.github/workflows/ci.yml`)

```
Trigger: push to main, PR to main
Concurrency: cancel in-progress runs on same ref

Job: build (ubuntu-latest)
  1. Checkout
  2. Set up JDK 21 (temurin) with Gradle cache
  3. Set up Android SDK (android-actions/setup-android@v3)
  4. chmod +x gradlew
  5. ./gradlew check          — compile + unit tests + detekt + lint
  6. ./gradlew assembleDebug assembleRelease  — build both APKs (release unsigned)
```

No secrets required — `build.gradle.kts` signing guard produces unsigned release APK when `keystore.properties` is absent; `MAPS_API_KEY` defaults to `""`.

---

## Deploy Workflow (`.github/workflows/deploy.yml`)

```
Trigger: workflow_dispatch (lane: beta/deploy) OR tag push (v*)
Concurrency: cancel-in-progress: false (never cancel a deploy)

Job: deploy (ubuntu-latest)
  1. Checkout
  2. Set up JDK 21 (temurin) with Gradle cache
  3. Set up Android SDK
  4. Set up Ruby 3.3 with bundler cache
  5. chmod +x gradlew
  6. Decode keystore (base64 → .jks) + write keystore.properties
  7. Write MAPS_API_KEY to local.properties
  8. Write Play Store JSON key
  9. bundle exec fastlane <lane>
```

---

## Fastlane Setup

| File | Purpose |
|------|---------|
| `Gemfile` | Pin Fastlane gem version |
| `fastlane/Appfile` | Package name + Play Store JSON key path |
| `fastlane/Fastfile` | Automation lanes |
| `fastlane/.gitignore` | Exclude generated reports |

### Lanes

| Lane | Action |
|------|--------|
| `test` | `./gradlew check` |
| `build` | `./gradlew assembleDebug` |
| `beta` | `bundleRelease` → upload to Play Store internal track |
| `deploy` | `bundleRelease` → upload to Play Store production track |

---

## GitHub Secrets Required

| Secret | Source |
|--------|--------|
| `KEYSTORE_BASE64` | `base64 -w 0 Venice.jks` |
| `KEYSTORE_PASSWORD` | From `keystore.properties` |
| `KEY_ALIAS` | From `keystore.properties` |
| `KEY_PASSWORD` | From `keystore.properties` |
| `PLAY_STORE_JSON_KEY` | Google Cloud Console service account JSON |
| `MAPS_API_KEY` | From `local.properties` |

---

## Key Decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Workflows | Two (CI + Deploy) | CI needs zero secrets; deploy needs all 6 — separation keeps CI reliable |
| Jobs | Single job per workflow | Avoids recompilation; single-module project |
| JDK | Temurin 21 | AGP 9.x requires JDK 21+ |
| Android SDK | `android-actions/setup-android@v3` | Auto-downloads compileSdk 36 |
| Caching | `actions/setup-java` `cache: gradle` | Caches `~/.gradle/caches` and wrapper |
| Build format | AAB (not APK) | Required by Play Store for new apps |
| Deploy track | Internal (beta lane) | Most restrictive; promote via Play Console or `deploy` lane |
| Fastlane | Over raw Gradle | Provides `supply` for Play Store uploads, metadata, track promotion |
| Deploy concurrency | `cancel-in-progress: false` | Never cancel a deploy mid-upload |
| Secret strategy | Base64-encoded keystore | Avoids committing JKS; decoded at build time into `keystore.properties` |

---

## Manual Prerequisites

1. **Play Store**: Create app listing for `com.guidovezzoni.venice`, upload first AAB manually
2. **Service account**: Create in Google Cloud Console → grant "Release manager" in Play Console → download JSON → wait 24–48h
3. **GitHub Secrets**: Add all 6 in repo Settings → Secrets → Actions
4. **Local Ruby**: Install Ruby 3.x + bundler for local Fastlane usage

---

## What's NOT Included (By Design)

- **Instrumented/emulator tests on CI** — slow, flaky, expensive on hosted runners
- **APK/AAB artifact upload** — deploy workflow handles distribution
- **Automatic version bumping** — manual for now, can automate later
- **Firebase App Distribution** — no Firebase in the project
- **Code coverage reporting** — would need JaCoCo, separate improvement

---

## Verification

1. Push branch → open PR to `main` → CI passes (no secrets needed)
2. Configure secrets → trigger deploy manually with `beta` → verify AAB uploads to internal track
3. Tag `v0.1.0` → deploy auto-triggers → verify upload

---

## Potential Issues

| Issue | Likelihood | Mitigation |
|-------|-----------|------------|
| compileSdk 36 auto-download fails | Low | Add explicit `sdkmanager "platforms;android-36"` step |
| Gradle 9.5.0 cache incompatibility | Low | Fall back to `gradle/actions/setup-gradle@v4` |
| First `supply` run without app listing | Certain | Create listing manually first; `skip_upload_metadata` flags prevent metadata failures |
| Keystore decode produces wrong path | Low | Use absolute `$GITHUB_WORKSPACE` path in generated `keystore.properties` |

---

## Files

| File | Action |
|------|--------|
| `.github/workflows/ci.yml` | Create |
| `.github/workflows/deploy.yml` | Create |
| `Gemfile` | Create |
| `fastlane/Appfile` | Create |
| `fastlane/Fastfile` | Create |
| `fastlane/.gitignore` | Create |
| `.gitignore` | Modify (append Fastlane/Bundler/Play Store ignores) |
| `docs/improvements/github-ci-setup.md` | Replace (this file) |

---

## Reference Files

- `app/build.gradle.kts` — signing guards (lines 51–58, 74–76), MAPS_API_KEY default (line 47), compileSdk 36
- `keystore.properties.template` — expected signing properties format
- `gradle/wrapper/gradle-wrapper.properties` — Gradle 9.5.0
- `gradle.properties` — JVM args (`-Xmx2048m`)
- `config/detekt/detekt.yml` — detekt rules + baseline
