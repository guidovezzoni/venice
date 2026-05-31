# Venice

A road trip planning app for Android, built with a modern architecture stack and developed through a specification-driven process (SDD).

The app is built entirely in Kotlin with Jetpack Compose and Material 3, following Clean Architecture to separate domain logic, data access, and UI into independent layers. Each feature uses the MVI (Model-View-Intent) pattern, enforcing unidirectional data flow through immutable state, explicit user intents, and one-shot effects. Dependency injection is handled by Hilt, persistence by Room, and all asynchronous work runs on Kotlin Coroutines and Flow.

Development is driven by a Specification-Driven Development (SDD) workflow powered by OpenSpec, where every change moves through a structured lifecycle: explore, propose, apply, verify, sync, and archive. Tasks within each change follow a BDD (Behaviour-Driven Development) structure with test-first ordering, so the test is always written before the production code that makes it pass. Custom commands (`/sdlc_open_story`, `/sdlc_apply_changes`, `/sdlc_verify_story`, `/sdlc_archive`, `/opsx:*`) automate the repetitive steps, from opening and refining user stories to implementing changes, verifying acceptance criteria, and driving the full OpenSpec lifecycle.

See [`docs/sdlc/commands/SDLC-README.md`](docs/sdlc/commands/SDLC-README.md) for full details on each command.

## The App

Venice helps users plan multi-stop road trips. The current version supports creating trips, viewing them in a list, navigating to trip details, and managing stops: setting a starting point and destination, adding up to 25 intermediate stops, reordering stops, editing any stop's location, and removing stops. The roadmap includes place search via geocoding, route calculation with Google Directions API, live GPS-based ETA, and Android Auto integration.

### Architecture & Tech Stack

| Layer | Technology |
|-------|------------|
| **UI** | Jetpack Compose, Material 3, Navigation Compose |
| **Architecture** | MVI (Model-View-Intent), Clean Architecture |
| **Dependency Injection** | Hilt |
| **Database** | Room |
| **Async** | Kotlin Coroutines, Flow |
| **Testing** | JUnit 4, MockK, Compose UI Test |

The codebase follows Clean Architecture with three layers:

- **Domain** — models, repository interfaces, and use cases
- **Data** — Room database, DAOs, entities, mappers, and repository implementations
- **UI** — Compose screens, ViewModels, and MVI contracts (UiState / UiIntent / UiEffect)

Each feature exposes a strict MVI contract: the View observes an immutable `UiState`, sends user actions as `UiIntent` values to the ViewModel, and consumes one-shot side effects via `UiEffect`. This enforces unidirectional data flow and makes state easy to test.

```
app/src/main/java/com/guidovezzoni/venice/
├── di/                  # Hilt modules
├── domain/              # Models, repository interfaces, use cases
├── data/                # Room DB, DAOs, entities, repository impls
└── ui/
    ├── screens/         # Compose screens per feature
    ├── viewmodel/       # MVI ViewModels
    ├── state/           # UiState data classes
    ├── intent/          # UiIntent sealed classes
    ├── effect/          # UiEffect sealed classes
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

| Command | Purpose |
|---------|---------|
| `/sdlc_open_story` | Open a story for development: creates a feature branch, renames the file to `-WIP`, and refines it with technical detail |
| `/sdlc_apply_changes` | Implement the current OpenSpec change and iteratively resolve any TODOs whose preconditions are now met |
| `/sdlc_verify_story` | End-to-end verification: runs OpenSpec verify, checks TODOs, validates acceptance criteria, renames the file to `-DONE`, and writes a report |
| `/sdlc_archive` | Archive the completed change, update project docs (README, AGENTS) if needed, and append an archive section to the report |
| `/opsx:*` | OpenSpec lifecycle commands (propose, apply, verify, sync, archive, explore, onboard) |

Helper commands (`/refine_user_story`, `/create_branch`) are called internally by `/sdlc_open_story` but can also be run standalone. See [`docs/sdlc/commands/SDLC-README.md`](docs/sdlc/commands/SDLC-README.md) for full details on each command.

### BDD Task Structure

Tasks are structured with test-first ordering, configured via `openspec/config.yaml`:

```
## 1. Feature Name (BDD)
- [ ] 1.1 Write test: GIVEN/WHEN/THEN description in TestClass
- [ ] 1.2 Implement: what to build to make the test pass
```

Prerequisites (setup, models) come first, BDD pairs in the middle ordered by dependency, and integration tasks (DI wiring, navigation, composables) at the end.

## Build & Run

```bash
./gradlew clean              # Clean state
./gradlew assembleDebug      # Debug build
./gradlew test               # Unit tests
./gradlew check              # Full checks
./gradlew build              # Complete verification
```
