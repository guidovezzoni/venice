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
- Verification **cannot be marked as PASSED** until every check is confirmed — including on-device tests.
- Do not rename a user story to `-DONE` or write a "PASSED" outcome in a report until all checks — automated and on-device — are confirmed.
- "All automated tests pass" is necessary but not sufficient. The full checklist — unit tests, instrumented tests, and on-device verification — is the gate.

### On-Device Testing
When verification requires running the app on a physical device or emulator:

1. **Check connectivity first.** Run `adb devices` to see if a device is attached.
2. **If a device is connected**, attempt automated verification autonomously:
   - Run `./gradlew connectedDebugAndroidTest` to execute instrumented Compose UI tests.
   - Run `./gradlew installDebug` to install the app, then launch it with `adb shell am start -n com.guidovezzoni.venice/.ui.MainActivity`.
   - Use `adb` to exercise the feature under test (tap, input, screenshot) and verify behaviour.
3. **If no device is connected**, ask the user to connect one and retry, or ask them to perform the manual test and report back.
4. **If adb-based verification is not feasible** for a specific check (e.g. subjective UX judgement, complex multi-app interaction), ask the user to perform that specific check and wait for confirmation.

