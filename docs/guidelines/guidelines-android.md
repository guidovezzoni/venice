# Android Guidelines

## Gradle & Dependencies
- All dependencies managed via `gradle/libs.versions.toml`
- Use version references (e.g., `version.ref = "kotlin"`)
- Group related dependencies logically
- **Convention plugins**: When the project has multiple Gradle modules, use a `build-logic` composite build with convention plugins to centralise shared build configuration (compileSdk, minSdk, Java/Kotlin compile options, detekt, kover). Each module should apply a convention plugin rather than duplicating configuration inline. Define at minimum an Android library plugin and a pure Kotlin (JVM) library plugin.
  - **Version catalog in build-logic**: The `build-logic` composite build must share the root version catalog. Declare it in `build-logic/settings.gradle.kts`:
    ```kotlin
    dependencyResolutionManagement {
        versionCatalogs {
            create("libs") {
                from(files("../gradle/libs.versions.toml"))
            }
        }
    }
    ```
    Plugin artifacts declared as `dependencies` in `build-logic/build.gradle.kts` must be added as `[libraries]` entries in `libs.versions.toml` and referenced via `libs.*` — never as hardcoded strings with inline versions.

## Android Specific

### Architecture
- **MVI Architecture**: Use Model-View-Intent pattern. MVI enforces a strict **unidirectional data flow**:
  1. The **View** observes a single immutable `UiState` and renders it.
  2. User interactions are modelled as explicit `UiIntent` sealed classes and sent to the **ViewModel**.
  3. The **ViewModel** processes each intent, calls the domain layer, and emits a new `UiState`.
  4. Side-effects (navigation, toasts, etc.) are emitted as a separate `UiEffect` `Channel`/`SharedFlow` so they are consumed exactly once.

  This makes data flow predictable, state easy to reason about, and business logic straightforward to unit-test.

- **Clean Architecture**: Use a clean architecture approach, separating data, domain, and UI. Organise folder with this structure:
```
app/src/main/java/<package-name>/
├── di/                          # DI/Hilt modules
│   └── AppModule.kt
├── ui/
│   ├── MainActivity.kt
│   ├── MainScreen.kt
│   ├── viewmodel/              # ViewModels
│   │   ├── Feature01ViewModel.kt
│   │   └── Feature02ViewModel.kt
│   ├── state/                  # UI State classes (immutable data classes)
│   │   ├── Feature01UiState.kt
│   │   └── Feature02UiState.kt
│   ├── intent/                 # UI Intent sealed classes (user actions)
│   │   ├── Feature01UiIntent.kt
│   │   └── Feature02UiIntent.kt
│   ├── effect/                 # UI Effect sealed classes (one-shot side-effects)
│   │   ├── Feature01UiEffect.kt
│   │   └── Feature02UiEffect.kt
│   ├── screens/
│   │   ├── Feature01Screen.kt   
│   │   └── Feature02Screen.kt
│   └── theme/
│       ├── Theme.kt
│       ├── Color.kt
│       └── Type.kt       
├── domain/                     # Business logic
│   ├── usecase/
│   │   ├── Usecase001UseCase.kt
│   │   ├── Usecase002UseCase.kt
│   │   └── Usecase003UseCase.kt
│   └── repository/             # Repository interfaces
└── data/                       # Data layer
    ├── repository/             # Repository implementations
    │   ├── Repository001Impl.kt
    │   └── Repository002Impl.kt
    ├── mapper/                 # DTO → domain mappers
    │   └── EntityMapper.kt
    └── database/
```

- **Preserve meaningful domain concepts as value objects**: When an API response contains a nested object that represents a coherent domain concept (e.g. a rating with a score and count, an address with street and city), model it as a separate value object in the domain layer rather than flattening its fields into the parent entity. Flattening dissolves concept boundaries, making the model harder to reason about and extend. The DTO layer mirrors the API shape; the domain layer mirrors the business meaning.

- **Clean Architecture layer boundaries**: The domain layer must not import or reference any data-layer types (DTOs, database entities, network models). Violations include domain repository interfaces returning DTOs, or mappers living in `domain/mapper/`.
  - **Repository interfaces** (in `domain/repository/`) must declare return types using only domain models.
  - **Mappers** (DTO → domain) belong in `data/mapper/` — they operate on data-layer inputs and produce domain outputs.
  - **Repository implementations** (in `data/repository/`) own the mapping step: parse/fetch the data-layer representation, apply the mapper, and return a domain model.
  - **Use cases** receive domain models directly from the repository and must not perform DTO-to-domain mapping.
  
- **MVI Contract**: Each feature exposes a clear contract between View and ViewModel:
  - `UiState`: a single immutable `data class` representing the full UI state. Default to a sensible initial state.
  - `UiIntent`: a `sealed class` listing every user action the screen can trigger (e.g. `OnButtonClicked`, `OnTextChanged`).
  - `UiEffect`: a `sealed class` for one-shot effects that should not survive recomposition (e.g. `NavigateTo`, `ShowSnackbar`).
  - The ViewModel exposes `uiState: StateFlow<UiState>` and `uiEffect: SharedFlow<UiEffect>`, and receives intents via a `fun onIntent(intent: UiIntent)` function.

  Example skeleton:
```kotlin
// UiState
data class Feature01UiState(
    val isLoading: Boolean = false,
    val items: List<Item> = emptyList(),
    val error: String? = null,
)

// UiIntent
sealed class Feature01UiIntent {
    data object LoadItems : Feature01UiIntent()
    data class DeleteItem(val id: String) : Feature01UiIntent()
}

// UiEffect
sealed class Feature01UiEffect {
    data class ShowError(val message: String) : Feature01UiEffect()
    data object NavigateBack : Feature01UiEffect()
}

// ViewModel
class Feature01ViewModel(...) : ViewModel() {
    private val _uiState = MutableStateFlow(Feature01UiState())
    val uiState: StateFlow<Feature01UiState> = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<Feature01UiEffect>()
    val uiEffect: SharedFlow<Feature01UiEffect> = _uiEffect.asSharedFlow()

    fun onIntent(intent: Feature01UiIntent) {
        when (intent) {
            is Feature01UiIntent.LoadItems -> loadItems()
            is Feature01UiIntent.DeleteItem -> deleteItem(intent.id)
        }
    }
}
```

- **Composables are purely presentational**: Composables must only render pre-computed data received via parameters (typically from `UiState`). They must not contain logic such as locale detection, API-level gating, formatting computations, or conditional business branching. All such logic belongs in the ViewModel, which computes display-ready values and exposes them through `UiState` fields.

- **Utility functions in their own file**: Testable helper functions (formatters, validators, converters) must live in their own dedicated file under `ui/util/` (or the appropriate layer), never co-located inside a composable file. Even if there is only a single call site, a function marked `internal` for testability belongs in a separate utility file.

- **Dependency Injection**: Ask the user if Hilt should be used or manual di. Typically manual di should be replaced by Hilt when the project is growing.
- **Async Operations**: Use Coroutines and Flow for asynchronous operations

### Jetpack Compose
- **Composable Structure:**
    - Parameters usually usually include a `Modifier` as a optional argument: `modifier: Modifier = Modifier`. When present it should always be the first parameter.
    - Avoid hardcoded dimensions; use `dimens.xml` or constant values.
    - Parameters containing lambdas to handle user UI interactions should usually have a default value, so we don't have to specify that at each call.
    - Boolean parameters that control if a part of the UI should show should have a default value, so we don't have to specify that at each call.
    - Example:
```kotlin
@Composable
fun ComponentName(
    modifier: Modifier = Modifier,
    // other parameters
) {
    // implementation
}
```
- **Composable Previews**: Always create a preview for the stateless composable, considering all the reasonable states that the composable could have.
    - The preview should be named `Preview[ComponentName]`.
    - It should be `private`.
    - It must be wrapped in the application's main theme (e.g., `MyAppTheme { ... }`).
    - Set `showBackground = true` in the `@Preview` annotation.
    - When calling a composable, parameters with a default value matching should not be specified
    - Example:
```kotlin
@Preview(showBackground = true)
@Composable
private fun PreviewComponentName() {
    WhatsappFilesFixerTheme {
        ComponentName()
    }
}
```
- **UiState Preview Coverage**: Every field of the screen's `UiState` must appear in a non-default value in at least one preview. Two fields may share a preview when it makes sense (e.g. `editingStop` + `isEditStopDialogVisible`), but no field should be left without visual coverage.
- **State Hoisting**: Keep composables stateless, hoist state to caller or ViewModel
- Use `mutableStateOf` only for state that needs to trigger recomposition
- Use `rememberSaveable` for state that should survive configuration changes
- **Material 3:** Always use Material 3 components unless specified otherwise.

#### Material 3 Usage
- Always use Material 3 components (`androidx.compose.material3.*`)
- **Scaffold Usage:** When creating screens, start with a `Scaffold` to handle top bars, floating action buttons, and snackbars correctly.
- Follow Material 3 design guidelines for colors and typography

### Legacy Code
- Do not use `findViewById`; strictly use Jetpack Compose or ViewBinding (if working with legacy views).

### Multi-language (i18n)
- Base language should always be UK English
- When adding a new user facing string, always extract it in the strings.xml file for easy translation
- Common strings, like button names, should be listed at the top of strings.xml and have this prefix: "global_"
- When a new string is added or modified to strings.xml, you should always update the same string in other languages that might be present, translating it accordingly.
- **Locale directories**: Before creating a new `values-xx` directory for a language, always run `ls app/src/main/res/` to check whether a region-qualified variant (e.g. `values-es-rES`) already exists. If it does, add strings there — never create a plain `values-xx` alongside it. Creating both causes Android to serve different string sets depending on device region, producing inconsistent translations.
- **Ellipsis**: Always replace three dots with ellipsis

### Kotlin Best Practices
- **One class per file**: Each Kotlin model (data class, sealed class, enum) must live in its own file. Do not group multiple models into a single file.
- **Immutability**: Always prefer `val` over `var` for immutable properties
- **Never use `!!`**: The non-null assertion operator can lead to unexpected crashes. Instead, use one of these alternatives:
  - `?: throw IllegalStateException("descriptive message")` — when null means a programming error or broken contract.
  - `?: return` / `?: return defaultValue` — when null is a valid signal to stop or fall back.
  - `?.let { ... }` — when the operation should simply be skipped if the value is absent.
  - Safe-cast with `as?` — when the type is uncertain.
- Use `Result` type for operations that can fail

### Import Organization
- Android framework imports first
- AndroidX imports next
- Project imports last
- Use alphabetical ordering within each group

### Naming Conventions
- **Composables**: PascalCase (e.g., `WhatsappScreen`, `MainScreen`)
- **Functions**: camelCase (e.g., `findWhatsappFolder`, `checkPermission`)
- **Variables**: camelCase, prefer descriptive names
- **Constants**: UPPER_SNAKE_CASE for top-level constants
- **No abbreviations or acronyms**: Use full, descriptive names for all variables, parameters, functions, and classes. Avoid shortened forms that require the reader to guess the meaning (e.g. use `destination` not `dest`, `startingPoint` not `sp`, `displayState` not `ds`).
- **Packages**: lowercase, reverse domain notation
- **MVI artefacts**:
  - State classes: `<Feature>UiState` (e.g., `HomeUiState`)
  - Intent classes: `<Feature>UiIntent` (e.g., `HomeUiIntent`)
  - Effect classes: `<Feature>UiEffect` (e.g., `HomeUiEffect`)
  - Intent entries: verb + noun in PascalCase (e.g., `LoadItems`, `DeleteItem`, `OnSearchQueryChanged`)

### Error Handling
- Use Kotlin's `Result` type for operations that can fail
- Provide user-friendly error messages in UI
- Log errors with appropriate context (don't log sensitive data)
- Handle permissions gracefully with clear user guidance

### Permission Handling
```kotlin
fun checkPermission(): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        Environment.isExternalStorageManager()
    } else {
        true // For older versions
    }
}
```

### Lifecycle-Aware Operations
```kotlin
DisposableEffect(lifecycleOwner) {
    val observer = LifecycleEventObserver { _, event ->
        if (event == Lifecycle.Event.ON_RESUME) {
            // Re-check permissions or refresh data
        }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose {
        lifecycleOwner.lifecycle.removeObserver(observer)
    }
}
```

## Development Best Practices
- **Literals extraction**: When adding new code or modifying existing code, you should always extract literals in private constants to be held in the same class/file in which they are used. By literals I mean:
    - strings and numeric
    - non UI/user facing, only development related
- **EOF Empty Line**: Always leave an empty line at the end of the source file.

## Testing Guidelines

### Unit Tests
- **Test file required**: Every new source class (use cases, ViewModels, repositories) must have a corresponding unit test file created at the same time.

Unit tests should follow these criteria:
- Located in `app/src/test/`
- Use JUnit 4
- Test composables with `compose-ui-test` library
- The test name should use the GIVEN / WHEN / THEN pattern, f.i. fun `GIVEN a context WHEN an action happens THEN the expected outcome is reached`
- Mock dependencies appropriately using MockK
- Create a setup method if required
- use runTest if required
- use UnconfinedTestDispatcher if required
- use @MockK notation if required
- extract in a variable the expected value before asserting
- cover all reasonable cases, but keeps the coverage over 95%
- Target 95%+ coverage
- **MVI ViewModel tests**: Test by dispatching `UiIntent` values via `onIntent()` and asserting the resulting `uiState` and any emitted `uiEffect`. Never test internal ViewModel methods directly.
- **Consolidate test setup**: When a ViewModel (or any SUT) is instantiated in multiple tests, use a single `createViewModel(...)` factory method with default parameters for varied inputs (e.g. `stops: List<Stop> = emptyList()`, `legs: List<Leg> = emptyList()`). Tests should never inline the full constructor call — extend the factory helper instead.


### Compose UI Tests
- **Test file required**: Every screen composable must have a corresponding Compose UI test file.
- Located in `app/src/androidTest/`
- Use `createComposeRule()` from `compose-ui-test-junit4`
- **Test the composable in isolation**: pass `uiState` directly and capture intents via the `onIntent` lambda — no ViewModel mocking needed.
- Assert UI elements are displayed using `onNodeWithText`, `onNodeWithContentDescription`, etc.
- Assert user interactions fire the correct `UiIntent` by collecting intents in a `mutableListOf` passed to `onIntent`.
- When a text string appears in multiple nodes (e.g. a button label and a dialog title), use `onAllNodesWithText(...)[index]` instead of `onNodeWithText`.
- Wrap the composable in the app theme for accurate rendering.
- Cover at minimum: empty/default state, populated state, visibility toggles for dialogs, button click intents, and dismiss intents.
- **Test naming**: Backtick-quoted function names are not supported by the Android test runner. Use camelCase with underscores as GIVEN/WHEN/THEN separators instead, e.g. `givenProductList_whenScreenIsDisplayed_thenOneCardPerProduct`.
- **Execution**: tests run on a connected device (physical or emulator) via `./gradlew connectedDebugAndroidTest`.

### ADB Interaction with Compose UIs

When verifying features on-device via adb and UIAutomator, Jetpack Compose requires specific handling because Compose nodes differ from traditional Android Views.

#### Workflow

1. **Dump the UI hierarchy** to discover element bounds:
   ```bash
   adb shell uiautomator dump /sdcard/ui.xml && adb pull /sdcard/ui.xml /tmp/ui.xml
   ```
   Parse the XML for `text`, `content-desc`, `bounds`, `clickable`, and `focused` attributes.

2. **Tap an element** by computing the centre of its `bounds="[left,top][right,bottom]"`:
   ```bash
   adb shell input tap $((($left+$right)/2)) $((($top+$bottom)/2))
   ```

3. **Find clickable parents**: In Compose, text nodes (`TextView`) are typically not clickable. The clickable target is an ancestor `View` node. Walk up the hierarchy to find the nearest `clickable="true"` parent and use its bounds.

4. **Enter text in a Compose EditText**:
   - Tap the `EditText` to focus it (verify `focused="true"` in a fresh dump)
   - Use `adb shell input text "<value>"` to type
   - Special characters: use `adb shell input keyevent <code>` instead

5. **Dismiss the keyboard** before tapping other elements:
   ```bash
   adb shell input keyevent 4   # BACK key
   ```

6. **Re-dump after every state change**: Whenever the UI changes (keyboard appears/dismisses, dialog opens/closes, screen navigates), the element bounds shift. Always dump the hierarchy again before the next interaction. Stale bounds from a previous dump will miss.

#### Common Pitfalls

- **Never estimate coordinates from screenshots** — display scaling makes pixel mapping unreliable. Always use bounds from `uiautomator dump`.
- **Keyboard shifts dialog bounds**: When the keyboard appears, dialog elements move up. Dismiss the keyboard or re-dump before tapping dialog buttons.
- **Confirm/Cancel buttons in Compose dialogs**: The `TextView` with "Confirm"/"Cancel" text is not clickable. Find the clickable `View` parent encompassing that text area.

## Detekt

Static analysis via [Detekt](https://detekt.dev/) with the [Compose rules plugin](https://github.com/mrmans0n/compose-rules).

### Running detekt

Always use the variant-specific tasks instead of the plain `detekt` task:

```bash
./gradlew detektDebug    # or detektRelease
```

The variant tasks (`detektDebug`, `detektRelease`) run with **type resolution** — they compile the code first and analyse it with the full classpath, enabling deeper rules such as `UnusedImport` and `UnusedPrivateFunction`. The plain `detekt` task runs without type resolution and misses these issues.

### Gradle setup

Apply the plugin in the root `build.gradle.kts` (without `apply`), then apply it in the module and add the Compose rules plugin as a `detektPlugins` dependency:

```kotlin
// root build.gradle.kts
plugins {
    alias(libs.plugins.detekt) apply false
}

// app/build.gradle.kts
plugins {
    alias(libs.plugins.detekt)
}

dependencies {
    detektPlugins(libs.detekt.compose.rules)
}

detekt {
    buildUponDefaultConfig = true
    config.setFrom(files("$rootDir/config/detekt/detekt.yml"))
    baseline = file("detekt-baseline.xml")
}
```

### Configuration file

Place the configuration at `config/detekt/detekt.yml`. Only settings that are stricter than Detekt's defaults are listed — everything else is left to the built-in defaults (enabled via `buildUponDefaultConfig = true`):

```yaml
config:
  warningsAsErrors: true

naming:
  FunctionNaming:
    ignoreAnnotated:
      - 'Composable'

style:
  UnusedPrivateFunction:
    ignoreAnnotated:
      - 'Preview'
  MagicNumber:
    ignorePropertyDeclaration: true
  UnusedImport:
    active: true
  WildcardImport:
    active: true

Compose:
  active: true
```

Key decisions:
- `warningsAsErrors: true` — zero tolerance; any violation fails the build. (Detekt 2.x replaced the 1.x `build: maxIssues: 0` key with this setting.)
- `FunctionNaming: ignoreAnnotated: ['Composable']` — Composable functions use PascalCase by convention, which would otherwise violate the default `[a-z][a-zA-Z0-9]*` naming pattern.
- `UnusedPrivateFunction: ignoreAnnotated: ['Preview']` — `@Preview` composables are private by convention but only invoked by the IDE tooling, not by runtime code. Without this exclusion, type-resolution tasks (`detektDebug`) flag them as unused.
- `MagicNumber: ignorePropertyDeclaration: true` — allows numeric literals in property declarations (e.g. `val Purple80 = Color(0xFFD0BCFF)`) without flagging them as magic numbers.
- `UnusedImport` and `WildcardImport` are inactive in Detekt's defaults; explicitly enabled here.
- `Compose: active: true` enables all rules from the `detekt-compose-rules` plugin, which are off by default.
- A `detekt-baseline.xml` can be generated to suppress pre-existing issues when adopting Detekt on a legacy codebase.

## Kover

Code coverage via [Kover](https://kotlin.github.io/kotlinx-kover/).

### Gradle setup

Apply the plugin in the root `build.gradle.kts` (without `apply`), then apply it in the module and configure the `kover` block:

```kotlin
// root build.gradle.kts
plugins {
    alias(libs.plugins.kover) apply false
}

// app/build.gradle.kts
plugins {
    alias(libs.plugins.kover)
}

kover {
    reports {
        filters {
            excludes {
                classes(
                    "*.BuildConfig",
                    "*.ComposableSingletons*",
                    "*_Factory*",
                    "*_HiltModules*",
                    "*_Impl",
                    "*_MembersInjector",
                    "hilt_aggregated_deps.*",
                    "dagger.hilt.*",
                    "*.Hilt_*",
                    "*.di.*",
                    "*.database.*Dao_Impl*",
                    "*.database.AppDatabase*",
                    "*.ui.theme.*",
                    "*.ui.screens.*",       // composable screens — tested via Compose UI tests
                    "*.YourApplication",    // replace with your Application class
                    "*.MainActivity",
                )
                annotatedBy(
                    "androidx.compose.ui.tooling.preview.Preview",
                    "androidx.compose.runtime.Composable",
                    "dagger.hilt.android.lifecycle.HiltViewModel",
                )
            }
        }
        verify {
            rule {
                minBound(95)
            }
        }
    }
}
```

Key decisions:
- Generated and DI glue classes (Hilt, Room DAO implementations, `_Factory`, `_Impl`) are excluded — they are not unit-testable and skew coverage metrics.
- `@Composable` and `@Preview` annotated code is excluded; composables are covered by separate Compose UI tests.
- `@HiltViewModel` annotated classes are excluded from the annotation filter because their constructor is generated; the ViewModel logic itself is tested via `UiIntent`/`UiState` assertions.
- The minimum bound is **95%**; lower it only with a documented rationale.
- Update `*.YourApplication` and `*.MainActivity` to match your actual package and class names.

## Fastlane

Automated build and release via [Fastlane](https://fastlane.tools/).

### Setup

`Gemfile` at the project root:

```ruby
source "https://rubygems.org"

gem "fastlane"
```

`fastlane/Appfile`:

```ruby
json_key_file(ENV["PLAY_STORE_JSON_KEY_FILE"] || "play-store-key.json")
package_name("com.your.package")   # replace with your application ID
```

- The Play Store JSON key path is read from the environment variable `PLAY_STORE_JSON_KEY_FILE`, falling back to `play-store-key.json` in the project root. Never commit the key file; add it to `.gitignore`.

### Fastfile

```ruby
default_platform(:android)

platform :android do
  desc "Run unit tests, detekt, and lint"
  lane :test do
    gradle(
      task: "check",
      flags: "--console=plain --stacktrace"
    )
  end

  desc "Build debug APK"
  lane :build do
    gradle(
      task: "assembleDebug",
      flags: "--console=plain --stacktrace"
    )
  end

  desc "Build release AAB and upload to Play Store internal track"
  lane :beta do
    gradle(
      task: "bundleRelease",
      flags: "--console=plain --stacktrace"
    )
    upload_to_play_store(
      track: "internal",
      aab: "app/build/outputs/bundle/release/app-release.aab",
      skip_upload_metadata: true,
      skip_upload_images: true,
      skip_upload_screenshots: true,
      skip_upload_apk: true
    )
  end

  desc "Build release AAB and upload to Play Store production track"
  lane :deploy do
    gradle(
      task: "bundleRelease",
      flags: "--console=plain --stacktrace"
    )
    upload_to_play_store(
      track: "production",
      aab: "app/build/outputs/bundle/release/app-release.aab",
      skip_upload_metadata: true,
      skip_upload_images: true,
      skip_upload_screenshots: true,
      skip_upload_apk: true
    )
  end
end
```

### Lanes

| Lane | Command | Purpose |
|------|---------|---------|
| `test` | `bundle exec fastlane test` | Runs `./gradlew check` (unit tests + detekt + lint) |
| `build` | `bundle exec fastlane build` | Builds a debug APK |
| `beta` | `bundle exec fastlane beta` | Builds release AAB and uploads to Play Store **internal** track |
| `deploy` | `bundle exec fastlane deploy` | Builds release AAB and uploads to Play Store **production** track |

Key decisions:
- Metadata, images, and screenshots are skipped on upload — manage store listing assets separately.
- `--console=plain` keeps Gradle output readable in CI logs; `--stacktrace` aids debugging.
- The `beta` / `deploy` lanes upload only an AAB (`skip_upload_apk: true`), which is the required format for Play Store submissions.
