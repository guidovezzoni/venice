## MODIFIED Requirements

### Requirement: Create trip dialog UI
The UI SHALL present a modal dialog (bottom sheet or `AlertDialog`) with:
- A labelled text input field ("Trip name").
- A Confirm button (disabled while the name is blank or whitespace-only, or while `isLoading` is `true`).
- A Cancel button or dismiss-by-tapping-outside behaviour.

The dialog composable SHALL be stateless, receiving `tripName`, `onNameChange`, `onConfirm`, `onDismiss`, and `isLoading` as parameters.

When `isLoading` is `true`:
- The confirm button SHALL be disabled regardless of whether the trip name is valid
- A `CircularProgressIndicator` SHALL be displayed inline in the confirm button slot
- The dismiss button SHALL remain enabled so the user can still close the dialog

#### Scenario: Dialog opens with empty input
- **WHEN** the user taps the FAB to create a trip
- **THEN** the dialog appears with an empty text field and the Confirm button disabled

#### Scenario: Confirm enabled after valid input
- **WHEN** the user types a non-blank trip name and `isLoading` is `false`
- **THEN** the Confirm button becomes enabled

#### Scenario: Dialog dismissed via Cancel
- **WHEN** the user taps Cancel or taps outside the dialog
- **THEN** the dialog closes without creating a trip

#### Scenario: Confirm button disabled while loading
- **WHEN** `isLoading` is `true`
- **THEN** the Confirm button is disabled regardless of the trip name value

#### Scenario: Spinner visible while loading
- **WHEN** `isLoading` is `true`
- **THEN** a `CircularProgressIndicator` is displayed in the confirm button slot

#### Scenario: Dismiss button enabled while loading
- **WHEN** `isLoading` is `true`
- **THEN** the Cancel/dismiss button remains enabled
