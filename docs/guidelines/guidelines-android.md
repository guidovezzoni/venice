# Android Guidelines

## Gradle & Dependencies
- All dependencies managed via `gradle/libs.versions.toml`
- Use version references (e.g., `version.ref = "kotlin"`)
- Group related dependencies logically

## Android Specific

### Architecture
- **MVVM Architecture**: Use Model-View-ViewModel pattern
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
│   ├── state/                  # UI State classes
│   │   ├── Feature01UiState.kt
│   │   └── Feature02UiState.kt
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
    └── database/
```
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
- **Ellipsis**: Always replace three dots with ellipsis

### Kotlin Best Practices
- **Immutability**: Always prefer `val` over `var` for immutable properties
- Never use the `!!` symbol as it could lead to unexpected crashes, cast or find a solution that guarantees non-nullability.
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
- **Packages**: lowercase, reverse domain notation

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
- cover all reasonable cases, but keeps the coverage over 80%
- Target 80%+ coverage


### Instrumentation Tests
- Located in `app/src/androidTest/`
- Use AndroidX Test and Espresso
- Test UI interactions and integration scenarios
