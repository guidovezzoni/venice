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

## Artifact-First Change Discipline

When a change is requested during development — whether by the user, discovered during implementation, or surfaced by a review — it must flow through the artifact chain in order:

1. **User story first.** Check if the user story needs updating (acceptance criteria added/removed/modified, description clarified, DoD adjusted). Update if needed.
2. **OpenSpec artifacts second.** Check and update the relevant OpenSpec artifacts: proposal, design, delta specs, and task list. Ensure the tasks.md reflects the change.
3. **Code last — and only via tasks.** Code is never implemented directly. All implementation work corresponds to an unchecked task in `tasks.md`. If no task exists for the work, add one first.

This prevents drift between documentation and implementation. The artifacts are the source of truth — code is their output, not the other way around.

## Data Modelling Discipline
- When modelling values from external data sources (APIs, JSON snapshots, protocols), only include
  values that are **confirmed to exist** in the available data or documentation.
- Do not speculatively add values that "seem likely" or "make sense" — they may be wrong and mislead
  future developers.
- Always provide a safe fallback (e.g. `UNKNOWN`) for unrecognised values to ensure forward
  compatibility.
- When full documentation is not available, add a comment in the code noting the limitation so
  future maintainers know to revisit when documentation becomes accessible.

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
   - Exercise the feature under test using this **UIAutomator-first workflow**:
     1. **Discover elements** — run `adb shell uiautomator dump /sdcard/ui.xml && adb pull /sdcard/ui.xml /tmp/ui.xml` and parse the XML for element `text`, `content-desc`, `bounds`, and `clickable` attributes. Never estimate coordinates from screenshots — display scaling makes them unreliable.
     2. **Tap** — extract the `bounds="[left,top][right,bottom]"` of the target element, compute the centre `((left+right)/2, (top+bottom)/2)`, and run `adb shell input tap <x> <y>`.
     3. **Type text** — tap the target field first (step 2), then run `adb shell input text "<value>"`. For special characters use `adb shell input keyevent <code>`.
     4. **Verify** — after each interaction, either dump the UI again to assert the new state, or take a screenshot with `adb shell screencap -p /sdcard/screen.png && adb pull /sdcard/screen.png /tmp/screen.png` for visual confirmation.
     5. **Re-dump after navigation** — whenever the screen changes (dialog opens, screen navigates), dump the UI hierarchy again before the next tap. Stale bounds from a previous dump will miss.
3. **If no device is connected**, ask the user to connect one and retry, or ask them to perform the manual test and report back.
4. **If adb-based verification is not feasible** for a specific check (e.g. subjective UX judgement, complex multi-app interaction), ask the user to perform that specific check and wait for confirmation.

