# Process and Workflow Guidelines


## Development Workflow
1. **Before Coding**: Run `./gradlew clean` to ensure clean state
2. **During Development**: Use `./gradlew assembleDebug` for quick builds
3. **After applying changes**: Run `./gradlew check` to ensure code quality
4. **Testing**: Run `./gradlew test` for unit tests, `./gradlew connectedAndroidTest` for UI tests
5. **Final Build**: Use `./gradlew build` for complete verification

## BDD Implementation Discipline
- After reaching GREEN in a BDD cycle, **re-read the task description and spec** for requirements that mocked unit tests cannot verify — e.g. `@Transaction` annotations, threading constraints, runtime guarantees, database constraints, concurrency behaviour.
- "Tests pass" is necessary but not sufficient. The spec is the source of truth, not the test suite.

## Verification Discipline
- Verification **cannot be marked as PASSED** until every check is confirmed — including manual tests that require the user to run the app on a device or emulator.
- If a checklist item cannot be automated (e.g. "manual verification on emulator"), the agent must **stop and ask the user to perform the test**, then wait for explicit confirmation before proceeding.
- Do not rename a user story to `-DONE` or write a "PASSED" outcome in a report until the user has confirmed all manual checks.
- "All automated tests pass" is necessary but not sufficient. The full checklist — automated and manual — is the gate.

