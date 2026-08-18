# Detekt on Modern Android — Diagnostics and Version Traps

## Context

Investigated 2026-08-17/18, starting from a trivial symptom: `./gradlew detektDebug` did not exist.

It turned out the project had been running detekt with **no type resolution and no Compose rules at
all** — for months, across every CI run, with a green build the whole time. Neither failure produces a
warning, an error, or a log line.

This document is the reference for diagnosing the same class of problem elsewhere. It is deliberately
project-agnostic; the current state of this repository is quarantined in the last section.

The general lesson is worth stating once: **detekt fails silently when versions do not line up.** A
missing task, a mismatched ruleset, and a key from the wrong major all present identically — as a build
that passes having analysed less than you think.

---

## Symptom index

| Symptom | Likely cause | Section |
|---------|--------------|---------|
| `detektDebug` / `detektRelease` task not found | variant tasks never registered | Finding 1 |
| Only `detekt`, `detektBaseline`, `detektGenerateConfig` exist | same | Finding 1 |
| `Compose:` block accepted but no Compose rules ever fire | ruleset/detekt major mismatch | Finding 2 |
| Baseline contains only built-in rule IDs | same | Finding 2 |
| Unused imports / unused private members never reported | no type resolution | Finding 1 |
| Config key silently ignored | validation disabled | Finding 3 |
| Config key rejected after an upgrade | rule renamed between majors | Finding 4 |

---

## Finding 1 — Variant tasks never register on detekt 1.x with a current AGP

**Symptom.** `./gradlew detektDebug` fails with "task not found". `:app:tasks --all` lists only
`detekt`, `detektBaseline`, `detektGenerateConfig`.

**Why it matters.** Only the variant tasks run with **type resolution** (they compile first and analyse
with the full classpath). Without them, every rule needing a classpath — unused imports, unused private
members — reports nothing while appearing to run. The plain `detekt` task completes in seconds, which is
the tell.

**Mechanism.** Two independent blockers, both confirmed by inspecting `detekt-gradle-plugin-1.23.8.jar`:

1. detekt 1.x has three registration paths — `registerDetektJvmTasks` (needs `kotlin-jvm`),
   `registerDetektMultiplatformTasks` (needs `kotlin-multiplatform`), and an Android path that waits for
   the **`kotlin-android`** plugin. When AGP supplies Kotlin support itself, `kotlin-android` is never
   applied, so none of the three fire and only the base task is created.
2. The Android path references `com.android.build.gradle.api.BaseVariant` — the legacy Variant API,
   deprecated in AGP 7 and **removed in AGP 8**.

**Fix.** Upgrade to detekt 2.x, whose Gradle plugin is a rewrite against the modern variant API.

**Not a fix.** Applying `kotlin-android` to trigger registration. Blocker 2 defeats it regardless, and
you will have added a redundant plugin for nothing.

**Alternative if you cannot upgrade.** Detekt documents registering a `Detekt` task manually, specifying
`classpath` and `jvmTarget` yourself. More build code, but it restores type resolution on 1.x.

---

## Finding 2 — A custom ruleset silently contributes zero rules across a major mismatch

**Symptom.** `Compose: active: true` (or any custom ruleset block) is accepted, the build passes, and no
rule from that set has ever fired.

**Mechanism.** Rulesets are discovered by `ServiceLoader`, and the service file name changed between
majors:

```
detekt 1.x looks for:  META-INF/services/io.gitlab.arturbosch.detekt.api.RuleSetProvider
detekt 2.x looks for:  META-INF/services/dev.detekt.api.RuleSetProvider
```

A ruleset built for the other major registers under a name the running detekt never reads. There is no
`ClassNotFoundException`, no warning — the loader simply finds nothing.

The artifact coordinates changed too: `io.gitlab.arturbosch.detekt:*` for 1.x,
`dev.detekt:*` for 2.x. Checking a ruleset's POM for which one it depends on is the fastest way to tell
which major it targets.

**How to confirm in minutes**: see the Diagnostic recipes below — check the ruleset jar's service file,
its POM dependency, and whether the baseline contains any rule ID from that set.

---

## Finding 3 — Config validation is stronger than documented, but has a blind spot

`config: validation: true` makes detekt check its own configuration.

- **It fails the build**, it does not warn. The docs say invalid items are "reported as warnings by
  default"; verified behaviour on 1.23.8 is a hard failure:
  `Run failed with 1 invalid config property.` Treat enabling it as a change that can break the build.
- **It does not cover custom rulesets.** Custom rule sets are excluded from validation by default, so a
  `Compose:` block validates fine whether or not the ruleset loaded. This is precisely why Finding 2 is
  invisible — the one mechanism that would have caught it is scoped out.

Enable it in its own change, run detekt once, fix what it names.

---

## Finding 4 — What changed between detekt 1.x and 2.x

| | 1.x | 2.x |
|---|---|---|
| Artifact group | `io.gitlab.arturbosch.detekt` | `dev.detekt` |
| Zero-tolerance gate | `build: maxIssues: 0` | fails on any `error` severity; `failOnSeverity` sets threshold (default `Error`) |
| `maxIssues` | required | **removed** — a leftover key enforces nothing |
| `warningsAsErrors` | available | available; needed so warnings reach `error` severity |
| Severity model | weights | three-value enum (Info, Warning, Error), configurable per rule |

Rule renames that bite on migration (not exhaustive — turn on `validation: true` and let detekt
enumerate the rest):

- `UnusedImports` → `UnusedImport`
- `MayBeConst` → `MayBeConstant`
- Threshold-style keys renamed to `allowed*` prefixes
- YAML values that accepted comma-separated strings now require real lists
- Some rules removed outright (e.g. `OptionalWhenBraces`, `PreferToOverPairSyntax`)

Not every rename is a 2.x change — `UnusedPrivateMember` was split into `UnusedPrivateFunction` /
`UnusedPrivateProperty` back in detekt 1.23, where the old name survives as a deprecated alias. A config
can therefore use the old name while the baseline records the new one, which is confusing but not a
version mismatch. Check the version a rename landed in before assuming it is a migration blocker.

A baseline generated under one major may reference rules that do not exist in the other, so plan to
regenerate it.

---

## Diagnostic recipes

```bash
# 1. Do variant tasks exist? (only these three = no type resolution)
./gradlew :app:tasks --all | grep -i "^detekt"

# 2. Which detekt version, and does the plugin jar still use the legacy AGP API?
grep -E '^detekt' gradle/libs.versions.toml
jar=$(find ~/.gradle/caches -name "detekt-gradle-plugin-*.jar" | head -1)
mkdir -p /tmp/dtk && unzip -oq "$jar" -d /tmp/dtk
s=$(strings -a $(find /tmp/dtk -name "DetektPlugin.class" -o -name "DetektAndroid*.class"))
grep -qF 'kotlin-android' <<<"$s" && echo "Android tasks gated on the kotlin-android plugin"
grep -qF 'com/android/build/gradle/api/BaseVariant' <<<"$s" && echo "uses BaseVariant — removed in AGP 8" 

# 3. Which major is a custom ruleset built for?
jar=$(find ~/.gradle/caches -name "detekt-*.jar" -path "*compose*" | head -1)
unzip -l "$jar" | grep "META-INF/services"     # dev.detekt.* = 2.x, io.gitlab.* = 1.x

# 4. Same question, from the POM
curl -sS https://repo1.maven.org/maven2/io/nlopez/compose/rules/detekt/<version>/detekt-<version>.pom \
  | grep -A2 detekt-core

# 5. Has the custom ruleset ever fired? (expect its rule IDs here)
grep -oP '<ID>\K[^:]+' app/detekt-baseline.xml | sort -u

# 6. Prove validation is active — inject a key from the other major, expect a build failure
#    e.g. add `UnusedImport:` under `style:` on 1.x
./gradlew :app:detekt
```

Recipe 5 is the quickest smoke test: if a baseline generated on a Compose-heavy codebase contains no
Compose rule IDs, the ruleset is not loading.

---

## Guarding against recurrence

`/sdlc_project_doctor` has a Detekt category covering all of the above: it requires detekt 2.x, verifies
the zero-tolerance mechanism, requires `validation: true`, and checks that the Compose rules version
matches the detekt major. Each failure message carries the mechanism, because the underlying faults are
silent and the person reading the failure is unlikely to have this context.

The compose-rules version floor is a hardcoded mapping and needs revisiting when either project releases
a new major.

---

## Status of this repository (2026-08-18)

| | |
|---|---|
| detekt | 1.23.8 |
| AGP | 9.2.1 (built-in Kotlin support, no `kotlin-android` plugin) |
| detekt-compose-rules | 0.6.2 — built against `dev.detekt:detekt-core:2.0.0-alpha.5`, so **2.x only** |
| Variant tasks | absent — no type resolution, confirmed |
| Compose rules | never loaded — confirmed via service-file mismatch and a baseline with zero Compose rule IDs |
| `validation: true` | added 2026-08-18; reports no invalid keys |
| Doctor result | fails checks 4 and 8 of the Detekt category |

Both failures resolve with the same action: upgrade detekt to 2.x. The Compose rules dependency is
already ahead of the tool and needs no change.

Costs to plan for, in rough order of size:

1. Type resolution and ~30 Compose rules switch on together, on a codebase that has had neither. Expect
   a substantial first-run finding list and a regenerated `app/detekt-baseline.xml`.
2. Config migration per Finding 4 — remove the `build` block, rename rules.
3. detekt 2.x is pre-release (`2.0.0-alpha.5` at time of writing). Requiring it trades stability for
   analysis that actually runs.

---

## References

- Detekt type resolution: https://detekt.dev/docs/gettingstarted/type-resolution
- Detekt 2.0 migration guide: https://detekt.dev/docs/introduction/migration
- Detekt configuration: https://detekt.dev/docs/introduction/configurations
- Compose rules: https://github.com/mrmans0n/compose-rules
- Conventions: `docs/guidelines/guidelines-android.md` (Detekt section)
