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
| `docs/sdlc/commands/` | SDLC custom command definitions (see `SDLC-README.md`) |
| `docs/reports/` | Verification and archive reports |
| `docs/improvements/` | Improvement proposals |
| `openspec/specs/` | Living specifications maintained via OpenSpec |
| `openspec/changes/` | Active and archived OpenSpec changes |

### Development Process

Development follows a Specification-Driven Development (SDD) workflow powered by OpenSpec, with BDD test-first task ordering. Custom commands (`/sdlc_open_story`, `/sdlc_apply_changes`, `/sdlc_verify_story`, `/sdlc_archive`) drive the user-story lifecycle. See @docs/sdlc/commands/SDLC-README.md for details.

### Current Status

Completed stories (see `docs/userstories/index.md` for full backlog):
- Epic 1 / Feature 1.1: Trip CRUD (create, list)
- Epic 1 / Feature 1.2: Stop management (set starting point, set destination, add intermediate stops, consolidate use cases, reorder stops, edit stop)

Next up: remove stop (1.2.6), then UI feedback (1.4.x) and stop progress tracking (1.3.x).
