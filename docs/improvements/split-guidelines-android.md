# Split `guidelines-android.md` by Agent Role

## Problem

`guidelines-android.md` is 610 lines and covers code patterns, testing, quality tooling, and CI/CD in
a single file. SDLC sub-agents load it as a monolithic block, but different agent roles need different
parts: a code-writing agent never needs ADB interaction commands, and a verification agent never needs
Kotlin naming conventions. This wastes context and makes the file harder to maintain.

## Proposed Split

Extract **testing** and **CI/CD** into their own files. Detekt and Kover stay in `guidelines-android.md`
because they are quality tooling tightly coupled to code-writing decisions.

### Result: three files

| File | Lines | Sections | Loaded by |
|------|-------|----------|-----------|
| `guidelines-android.md` | ~442 | Gradle, Architecture, Compose, Kotlin, i18n, naming, error handling, permissions, lifecycle, dev best practices, Detekt, Kover | Code-writing, refinement, proposal, quality-check agents |
| `guidelines-testing.md` (new) | ~72 | Unit Tests, Compose UI Tests, ADB Interaction with Compose UIs | Test-writing, verification, on-device agents |
| `guidelines-cicd.md` (new) | ~92 | Fastlane (setup, Fastfile, lanes) | Release/deploy agents |

### What moves where

**To `guidelines-testing.md`** (lines 276-347 of current file):
- `## Testing Guidelines` header becomes the new file's introduction
- `### Unit Tests` (20 lines) — JUnit 4, MockK, naming, coverage target, consolidate test setup
- `### Compose UI Tests` (13 lines) — createComposeRule, intent capture, naming, execution
- `### ADB Interaction with Compose UIs` (37 lines) — UIAutomator workflow, common pitfalls

**To `guidelines-cicd.md`** (lines 519-610 of current file):
- `## Fastlane` header becomes the new file's introduction
- `### Setup` (19 lines) — Gemfile, Appfile
- `### Fastfile` (56 lines) — lane definitions
- `### Lanes` (13 lines) — command reference table

**Stays in `guidelines-android.md`:**
- Gradle & Dependencies (17 lines)
- Architecture: MVI, Clean Architecture, core/ charter, layer boundaries, MVI Contract (133 lines)
- Jetpack Compose: structure, previews, UiState coverage, state hoisting, Material 3 (43 lines)
- Legacy Code (3 lines)
- Multi-language / i18n (8 lines)
- Kotlin Best Practices (10 lines)
- Import Organization (6 lines)
- Naming Conventions (13 lines)
- Error Handling (6 lines)
- Permission Handling (11 lines)
- Lifecycle-Aware Operations (15 lines)
- Development Best Practices (6 lines)
- Detekt (107 lines)
- Kover (64 lines)

## Files to Update

### AGENTS.md

Add lazy-load references after the existing `### Android Guidelines` block:

```markdown
### Testing Guidelines

For test writing patterns and on-device testing: @docs/guidelines/guidelines-testing.md

### CI/CD Guidelines

For Fastlane setup and build/release automation: @docs/guidelines/guidelines-cicd.md
```

### SDLC command files

| Command file | Change |
|---|---|
| `sdlc_open_story.md` (line 90) | No change needed |
| `sdlc_propose_change.md` (lines 44, 143) | Add `guidelines-testing.md` as context file |
| `sdlc_implement_change.md` (line 139) | Add `guidelines-testing.md`; update text to note testing patterns are in separate file |
| `sdlc_project_doctor.md` (line 90) | Add `guidelines-testing.md` for Unit Tests category check |
| `sdlc_doctor.md` (lines 79-92) | Add `guidelines-testing.md` and `guidelines-cicd.md` to existence checks; add to AGENTS.md wiring checks (four "normal work" guidelines become six) |
| `sdlc_exp_vibe_a_story.md` (lines 136, 312) | Add `guidelines-testing.md` as context file |

### Other cross-references

| File | Line | Change |
|------|------|--------|
| `docs/sdlc/README.md` | 286 | Update guideline inventory to list all three files |
| `README.md` | 139 | Remove the TODO "break down guidelines-android" |

No changes needed for:
- `guidelines-analytics.md` line 153 — references core/ charter, stays in `guidelines-android.md`
- `docs/improvements/detekt-diagnostics.md` line 210 — references Detekt section, stays in `guidelines-android.md`

## Eval Changes Required

The eval scaffolding has **four** areas that need updating. Getting these wrong will cause eval
scenarios to fail, so handle them together with the main split.

### 1. `eval_helpers.sh` — `create_guidelines_fixture()` (line 63)

The loop at line 67 creates placeholder files for six guideline names:

```bash
for guideline in android git process analytics userstories reports; do
```

Add `testing` and `cicd` to this list:

```bash
for guideline in android git process analytics userstories reports testing cicd; do
```

### 2. `eval_helpers.sh` — AGENTS.md fixture (lines 71-96)

The inline AGENTS.md heredoc only references four guidelines. Add the two new lazy-load entries:

```markdown
### Testing Guidelines

For test writing patterns and on-device testing: @docs/guidelines/guidelines-testing.md

### CI/CD Guidelines

For Fastlane setup and build/release automation: @docs/guidelines/guidelines-cicd.md
```

### 3. `doctor_all_pass.sh` (lines 46-53)

Add assertions for the two new guideline files:

```bash
assert_check_passes "guidelines-testing" "guidelines-testing.md passes"
assert_check_passes "guidelines-cicd" "guidelines-cicd.md passes"
```

### 4. `doctor_everything_broken.sh` (lines 44-52)

This scenario expects all guidelines to fail when files are missing. Two changes:

- Add failure assertions for the new files:
  ```bash
  assert_check_fails "guidelines-testing" "guidelines-testing.md fails"
  assert_check_fails "guidelines-cicd" "guidelines-cicd.md fails"
  ```
- Update `assert_min_fail_count` from 22 to **24** (line 52), since two more guideline files now
  need to exist and be wired. The comment on line 48 should be updated to say "8 guidelines files"
  instead of "6 guidelines files".

### 5. `check_guardrails.sh`

No changes needed unless new `assert_contains` checks are added for commands that should reference
the new guideline files. The existing assertions only check for `guidelines-reports.md`,
`guidelines-userstories.md`, and `guidelines-git.md` references in specific commands — none check
for `guidelines-android.md`, so the split does not break existing guardrails.

However, consider adding guardrails for the new references:
- `sdlc_implement_change.md` should reference `guidelines-testing.md`
- `sdlc_propose_change.md` should reference `guidelines-testing.md`

### 6. `evals/README.md` (line 208)

Update the fixture description from:
```
- Minimal guidelines files (userstories, git, reports, android, process)
```
to:
```
- Minimal guidelines files (userstories, git, reports, android, process, analytics, testing, cicd)
```
(Note: `analytics` is already created by the fixture but missing from this documentation line.)
