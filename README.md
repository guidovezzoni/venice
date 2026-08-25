# Intro on SDLC — Agentic AI Software Development Lifecycle

SDLC is a set of commands that let an AI coding agent autonomously drive the full lifecycle of a user story — from opening to verified delivery — while keeping the human developer in control of key decisions.

Built on top of [OpenSpec](https://github.com/Fission-AI/OpenSpec/) (Spec-Driven Development), SDLC replaces manual task management with agentic orchestration: the AI reads specifications, reasons about architecture, writes and verifies code, and produces auditable reports at every stage.

## Features


| Capability                          | How SDLC uses it                                                                                                                                           |
| ----------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **Autonomous multi-step reasoning** | Each command chains 15+ sequential steps, making decisions at each gate without human intervention                                                         |
| **Sub-agent orchestration**         | All four commands delegate self-contained steps to cheaper sub-agents (Sonnet for reasoning, Haiku for mechanical tasks), each with a fresh context window |
| **Tool use**                        | The agent drives git, Gradle, adb, static analysis, and Claude Code's security scanners directly                                                           |
| **Self-healing loops**              | Failed tests or security findings trigger automatic fix-and-retry cycles                                                                                   |
| **On-device verification**          | The agent installs the app on a physical device, interacts with it via UIAutomator, and verifies behaviour autonomously                                    |
| **Specification grounding**         | All code generation is anchored to living specs and acceptance criteria — not just free-form prompts                                                       |


**Please note**: full description and repo available at [https://github.com/guidovezzoni/SDLC](https://github.com/guidovezzoni/SDLC)

# The App - Venice

A road trip planning app for Android, built with a modern architecture stack and developed through a specification-driven process (SDD).

The app is built entirely in Kotlin with Jetpack Compose and Material 3, following Clean Architecture to separate domain logic, data access, and UI into independent layers. Each feature uses the MVI (Model-View-Intent) pattern, enforcing unidirectional data flow through immutable state, explicit user intents, and one-shot effects. Dependency injection is handled by Hilt, persistence by Room, and all asynchronous work runs on Kotlin Coroutines and Flow.

Development follows a Specification-Driven Development (SDD) workflow powered by OpenSpec, where every change moves through a structured lifecycle: explore, propose, apply, verify, sync, and archive. Tasks within each change follow a BDD (Behaviour-Driven Development) structure with test-first ordering, so the test is always written before the production code that makes it pass. Custom commands (`/sdlc_open_story`, `/sdlc_propose_change`, `/sdlc_implement_change`, `/sdlc_verify_story`, `/sdlc_archive`, `/opsx:*`) automate the repetitive steps, from opening and refining user stories to implementing changes, verifying acceptance criteria, and driving the full OpenSpec lifecycle.

See [`docs/sdlc/README.md`](docs/sdlc/README.md) for full details on each command.

## Architecture &amp; Tech Stack


| Layer                    | Technology                                      |
| ------------------------ | ----------------------------------------------- |
| **UI**                   | Jetpack Compose, Material 3, Navigation Compose |
| **Architecture**         | MVI (Model-View-Intent), Clean Architecture     |
| **Dependency Injection** | Hilt                                            |
| **Database**             | Room                                            |
| **Async**                | Kotlin Coroutines, Flow                         |
| **Places**               | Google Places SDK (autocomplete)                |
| **Routes**               | Google Routes API (OkHttp)                      |
| **Analytics**            | Firebase Analytics (GA4) + Firebase Crashlytics, provider-agnostic abstraction in `core/analytics/` |
| **Testing**              | JUnit 4, MockK, Compose UI Test                 |


The codebase follows Clean Architecture with four layers:

- **Domain** — models, repository interfaces, and use cases
- **Data** — Room database, DAOs, entities, mappers, and repository implementations
- **UI** — Compose screens, ViewModels, and MVI contracts (UiState / UiIntent / UiEffect)
- **Core** — cross-cutting concerns consumed by more than one layer (analytics, logging, generic utilities)

Each feature exposes a strict MVI contract: the View observes an immutable `UiState`, sends user actions as `UiIntent` values to the ViewModel, and consumes one-shot side effects via `UiEffect`. This enforces unidirectional data flow and makes state easy to test.

```
app/src/main/java/com/guidovezzoni/venice/
├── di/                  # Hilt modules
├── core/                # Cross-cutting concerns (analytics, etc.)
│   └── analytics/       # AnalyticsClient, providers, event taxonomy
├── domain/              # Models, repository interfaces, use cases
├── data/                # Room DB, DAOs, entities, repository impls
└── ui/
    ├── screens/         # Compose screens per feature
    ├── viewmodel/       # MVI ViewModels
    ├── state/           # UiState data classes
    ├── intent/          # UiIntent sealed classes
    ├── effect/          # UiEffect sealed classes
    ├── util/            # Formatting and other testable UI utilities
    └── theme/           # Material 3 theme
```

## The Process

Development follows a **Specification-Driven Development (SDD)** workflow powered by [OpenSpec](https://github.com/OpenSpec-ai/openspec), enhanced with custom commands for Claude / Cursor.

### Workflow

The process starts from user stories organised into epics and features (see `docs/userstories/`). Each piece of work then moves through the OpenSpec lifecycle:

1. **Explore** — think through the problem, investigate constraints, and clarify requirements before committing to a solution
2. **Propose** — generate a complete change proposal: design document, delta specs, and a BDD-structured task list
3. **Apply** — implement the tasks, following test-first ordering (write the test, then the code that makes it pass)
4. **Verify** — validate that the implementation matches the change artifacts
5. **Sync** — merge delta specs into the main specification set
6. **Archive** — finalise and archive the completed change

Specifications live in `openspec/specs/` and evolve incrementally through delta specs attached to each change. Completed changes are archived in `openspec/changes/archive/` with their full proposal, design, tasks, and delta specs preserved.

### Custom Commands

The workflow is extended with custom commands that wrap the OpenSpec lifecycle into a user-story-driven pipeline:


| Command                  | Purpose                                                                                                                                      |
| ------------------------ | -------------------------------------------------------------------------------------------------------------------------------------------- |
| `/sdlc_open_story`       | Open a story for development: creates a feature branch, renames the file to `-WIP`, and refines it with technical detail                     |
| `/sdlc_implement_change` | Implement the current OpenSpec change and iteratively resolve any TODOs whose preconditions are now met                                      |
| `/sdlc_verify_story`     | End-to-end verification: runs OpenSpec verify, checks TODOs, validates acceptance criteria, renames the file to `-DONE`, and writes a report |
| `/sdlc_archive`          | Archive the completed change, update project docs (README, AGENTS) if needed, and append an archive section to the report                    |
| `/opsx:*`                | OpenSpec lifecycle commands (propose, apply, verify, sync, archive, explore, onboard)                                                        |


Helper commands (`/refine_user_story`, `/create_branch`) are called internally by `/sdlc_open_story` but can also be run standalone. See [`docs/sdlc/README.md`](docs/sdlc/README.md) for full details on each command.

### BDD Task Structure

Tasks are structured with test-first ordering, configured via `openspec/config.yaml`:

```
## 1. Feature Name (BDD)
- [ ] 1.1 Write test: GIVEN/WHEN/THEN description in TestClass
- [ ] 1.2 Implement: what to build to make the test pass
```

Prerequisites (setup, models) come first, BDD pairs in the middle ordered by dependency, and integration tasks (DI wiring, navigation, composables) at the end.

## Analytics

Product analytics is delivered through a provider-agnostic abstraction in `core/analytics/`. The design separates the event taxonomy from the backends that consume it, so providers can be added or replaced without touching call sites.

### Product Questions

Every event exists to answer one of these four.

| # | Question | Why it matters |
|---|----------|----------------|
| **Q1** | Do people who create a trip actually finish planning it, and where do they drop out? | Identifies the biggest leak in the core flow — the highest-leverage thing to fix |
| **Q2** | Which features actually get used? | Tells us what to invest in and what to cut |
| **Q3** | How often do Places lookups, route calculations, and persistence fail in the wild? | Failures invisible in development; the difference between "works" and "works for users" |
| **Q4** | What shape are real trips? | Informs performance work and future features (Android Auto, live position) |

### Implementation

**Architecture:**

- `AnalyticsTracking` defines the shared surface: `logEvent`, `setUserProperty`, and `trackException`. Both `AnalyticsClient` (consumed by call sites) and `AnalyticsProvider` (implemented by each backend) extend it.
- `CompositeAnalyticsClient` fans out every operation to all registered providers via Hilt `@IntoSet` multibinding. When no providers are bound, every operation is a silent no-op.
- The full event taxonomy is a single `AnalyticsEvent` sealed class (14 events), with typed constructors enforcing parameter shapes at compile time. Parameter values use enums with explicit `snake_case` `value` properties — never `.name`.

**Providers:**

| Provider | Build variant | Purpose |
|----------|---------------|---------|
| `FirebaseAnalyticsProvider` | Release + Debug | Forwards events and user properties to Firebase Analytics (GA4). Validates platform limits (name length, parameter count, reserved prefixes), maps `ScreenViewed` to Firebase's `SCREEN_VIEW`, and converts `Boolean` parameters to `String` for GA4 compatibility. |
| `CrashlyticsAnalyticsProvider` | Release + Debug | Forwards exceptions to Firebase Crashlytics with operation context as custom keys. Receives `OperationFailed` error type as a "last known error" breadcrumb for fatal crashes. |
| `DebugAnalyticsProvider` | Debug only | Logs all events, user properties, and exceptions to Logcat. Bound via a debug-source-set DI module to prevent analytics stream leakage in release builds. |

**Privacy floor:** no identifiers, no free text, no coordinates, no place names are sent. Continuous values are banded (distance, duration); failures are classified into bounded enums rather than forwarding exception messages.

**Dual-channel failure reporting:** every failure path emits both an `OperationFailed` event (bounded enums to product analytics, answering "how often") and a `trackException` call (full throwable to crash reporting, answering "where and why"). The two channels are kept strictly separate — the throwable never becomes an event parameter.

**Tracking calls** live in ViewModels alongside intent handling. Screen views are fired from navigation destination changes, not ViewModel `init`, to correctly handle back-navigation and process-death restore. Firebase `screen_view` autocapture is disabled in the manifest; the app's `screen_viewed` event is mapped to Firebase's reserved `SCREEN_VIEW` name at the provider level.

The tracking plan at `docs/analytics/tracking-plan.md` is the single source of truth for which events exist, their parameters, and the product questions they answer.

## Deployment

The app uses **Fastlane** for build automation and **GitHub Actions** for CI/CD.

### Automated Releases

Releases are triggered by pushing a version tag (e.g., `v1.2.3`) to a `release/*` branch. The CI/CD pipeline automatically:
1.  **Validates the branch**: Ensures production tags are only processed from dedicated release branches.
2.  **Selects the Track**:
    *   `vX.Y.Z` -> **Google Play Production** (`deploy` lane).
    *   `vX.Y.Z-rcN` -> **Google Play Open Testing** (`beta` lane).
    *   `vX.Y.Z-alphaN` -> **Google Play Closed Testing** (`alpha` lane).
3.  **Generates GitHub Release**: Creates a new release on GitHub, attaches the `.aab` and `.apk` artifacts, and generates release notes. Tags with `-rc` or `-alpha` are marked as **Pre-release** - [GitHub Release](https://github.com/guidovezzoni/venice/releases)
4.  **Uploads to Google Play**: Submits the App Bundle to the mapped track in the Google Play Console: [Google Play](https://play.google.com/store/apps/details?id=com.guidovezzoni.venice)

### Fastlane Lanes

| Lane | Command | Purpose |
| :--- | :--- | :--- |
| `test` | `fastlane test` | Runs unit tests, Detekt, and Lint. |
| `beta` | `fastlane beta` | Builds and uploads to Google Play **Internal** track. |
| `alpha` | `fastlane alpha` | Builds and uploads to Google Play **Alpha** track. |
| `deploy` | `fastlane deploy` | Builds and uploads to Google Play **Production** track. |

## Build &amp; Run

```bash
./gradlew clean                        # Clean state
./gradlew assembleDebug                # Debug build
./gradlew test                         # Unit tests
./gradlew connectedDebugAndroidTest    # UI tests (requires connected device or emulator)
./gradlew check                        # Full checks
./gradlew build                        # Complete verification
```
