# AGENTS.md

This file contains guidelines and commands for agentic coding agents working in this repository.

## External File Loading

CRITICAL: When you encounter a file reference (e.g., @docs/guidelines/guidelines-android.md), use your Read tool to load it on a need-to-know basis. They're relevant to the SPECIFIC task at hand.

Instructions:

- Do NOT preemptively load all references - use lazy loading based on actual need
- When loaded, treat content as mandatory instructions that override defaults
- Follow references recursively when needed
- If a file reference cannot be found, always notify the user.

### Android Guidelines

For Native Android code style and best practices: @docs/guidelines/guidelines-android.md

### Git Guidelines

For git operations and commit conventions: @docs/guidelines/guidelines-git.md

### General Guidelines

Read the following file immediately as it's relevant to all workflows: @docs/guidelines/guidelines-process.md


## Project Overview

**Venice** is a road trip planning app for Android, built in Kotlin with Jetpack Compose and Material 3. It follows Clean Architecture (data / domain / UI layers) with the MVI pattern per feature. DI is handled by Hilt, persistence by Room, and async work by Coroutines and Flow.

- **Package**: `com.guidovezzoni.venice`
- **Min SDK**: 24 — **Target SDK**: 36
- **Source root**: `app/src/main/java/com/guidovezzoni/venice/`
- **Tests**: `app/src/test/` (unit, JUnit 4 + MockK), `app/src/androidTest/` (instrumentation)
- **Dependencies**: managed in `gradle/libs.versions.toml`

### Key Directories

| Path | Contents |
|------|----------|
| `docs/userstories/` | User story backlog organised by epic and feature (see `index.md`) |
| `docs/sdlc/commands/` | SDLC custom command definitions |
| `docs/reports/` | Verification and archive reports |
| `docs/improvements/` | Improvement proposals |
| `openspec/specs/` | Living specifications maintained via OpenSpec |
| `openspec/changes/` | Active and archived OpenSpec changes |

### Development Process

Development follows a Specification-Driven Development (SDD) workflow powered by OpenSpec, with BDD test-first task ordering. Custom commands (`/sdlc_open_story`, `/sdlc_propose_change`, `/sdlc_implement_change`, `/sdlc_verify_story`, `/sdlc_doctor`) drive the user-story lifecycle. See @docs/sdlc/README.md for details.

### Current Status

Completed stories (see `docs/userstories/index.md` for full backlog):
- Epic 1 / Feature 1.1: Trip CRUD (create, list)
- Epic 1 / Feature 1.2: Stop management (set starting point, set destination, add intermediate stops, consolidate use cases, reorder stops, edit stop, remove stop)
- Epic 1 / Feature 1.3: Stop progress tracking — 1.3.1 (mark stop as departed), 1.3.2 (see stop progress: checkmark icon, current border, progress summary)
- Epic 1 / Feature 1.4: UI feedback — 1.4.1 (dialog loading feedback: spinner in dialogs, disabled buttons during async ops, 500ms minimum duration)
- Epic 2 / Feature 2.1: Place search — 2.1.1 (search place by name: Google Places autocomplete in stop dialogs, auto-fill coordinates, read-only lat/lon after selection), 2.1.2 (resolve place to coordinates: resolving progress indicator in dialog, disabled confirm button while resolving, inline error on failure)
- Epic 3 / Feature 3.1: Route calculation — 3.1.1 (calculate route between stops: Google Routes API, distance/duration per leg, local persistence, automatic invalidation on stop mutation), 3.1.2 (see leg distance: locale-aware distance formatting with imperial/metric detection)
