# Password Manager / Autofill Support for Authentication Screens

## Goal

Make every sign-in and sign-up form in the app compatible with platform
password managers (Bitwarden, 1Password, Google Autofill, Keychain, etc.) so
that:

1. The password manager detects fillable fields and offers stored credentials.
2. After successful authentication the OS prompts the user to save or update
   credentials.

Without explicit opt-in, most UI frameworks do **not** register form fields
with the platform autofill service, so password managers never see them.

## Context to gather

Before implementing, understand:

1. **Which framework/toolkit builds the auth screens?** (Flutter, SwiftUI,
   Jetpack Compose, React Native, web HTML, etc.) Each has its own autofill
   API surface.
2. **Where are the sign-in and sign-up forms?** Locate every screen or
   component that contains email/username and password text fields.
3. **Does the framework require an explicit autofill grouping widget?** Most
   do: Flutter's `AutofillGroup`, Android XML's `autofillHints`, HTML's
   `<form>` + `autocomplete`, SwiftUI's `.textContentType`, etc.
4. **Does the framework require per-field semantic hints?** e.g.
   `AutofillHints.email`, `autocomplete="email"`, `textContentType(.emailAddress)`.
5. **Is there a "finish autofill context" or equivalent call?** After
   successful auth, the app must signal the OS so the "Save password?" prompt
   fires. Without it, credentials are never offered for saving.

## Plan & confirm

Propose an implementation plan with these steps (adapt to the target stack):

### Step 1 — Group the fields

Wrap each form's input fields in the framework's autofill-group construct.
This registers an autofill client with the platform.

| Framework | Construct |
|-----------|-----------|
| Flutter | `AutofillGroup` widget wrapping the `Column`/`Form` |
| Android XML | fields inside a `<form>` or view with `importantForAutofill="yes"` |
| HTML | `<form>` element (browsers auto-detect) |
| SwiftUI | fields grouped in the same view hierarchy |
| React Native | platform-specific; may need a native module or library |

### Step 2 — Annotate each field with semantic hints

Each text input needs a hint telling the autofill service what it represents.
Use the most specific hint available:

| Field | Hint (conceptual) |
|-------|-------------------|
| Email / username (sign-in) | `email` or `username` |
| Password (sign-in) | `password` (existing credential) |
| Password (sign-up) | `newPassword` (signals "generate" to managers) |
| Confirm password (sign-up) | `newPassword` |

Reference choices from the source project (Flutter):
- `autofillHints: const [AutofillHints.email]`
- `autofillHints: const [AutofillHints.password]`
- `autofillHints: const [AutofillHints.newPassword]`

### Step 3 — Signal completion after successful auth

After the user successfully signs in, signs up, or completes email
verification, call the platform's "finish autofill context" API **before**
navigating away from the form. This triggers the OS save-credential prompt.

| Framework | Call |
|-----------|------|
| Flutter | `TextInput.finishAutofillContext()` |
| Android | `AutofillManager.commit()` |
| HTML | browser handles on `<form>` submit |
| SwiftUI | handled automatically on navigation if hints are set |

### Step 4 — Preserve existing field configuration

Do not remove or break existing field properties (keyboard type, input
actions, obscure-text flags, controllers, submit handlers). The autofill
additions are purely additive.

### Decisions to resolve per project

- **Username vs email:** if the app uses usernames instead of email, use
  `username` hint instead of `email`.
- **OAuth-only flows:** if auth is entirely via OAuth/SSO buttons (Google,
  Apple, etc.), there are no text fields to annotate — password manager
  support is not applicable to those flows.
- **Multi-step auth:** if email and password are on separate screens, each
  screen needs its own autofill group, and `finishAutofillContext` is called
  only after the final step succeeds.

## Implementation

Apply the three changes to every auth form in the project:

1. Add any required import for the autofill/services API.
2. Wrap each form's fields in the autofill-group construct.
3. Add semantic hints to every email/username and password field.
4. Call the finish-autofill-context API after each successful auth path
   (sign-in, sign-up, email verification, password reset if the form has a
   new-password field).

## Verification

- **Static analysis / lint:** confirm no new warnings.
- **Existing tests:** confirm they still pass (autofill additions should not
  affect widget test behaviour).
- **Manual on-device test (critical):**
  1. Open the sign-in screen. Confirm the password manager overlay or
     suggestion bar appears on the email field.
  2. Fill credentials and sign in. Confirm the OS "Save password?" prompt
     appears after successful auth.
  3. Open the sign-up screen. Confirm the password manager offers to generate
     a password (for managers that support it via the `newPassword` hint).
  4. Complete sign-up. Confirm the save prompt fires.
- **Emulator note:** autofill services may not be available on all emulators.
  Test on a physical device with the password manager installed and enabled in
  the OS autofill settings.

## Summary

This feature is purely a UI-layer concern — no backend, no new dependencies,
no state changes. The pattern is the same across all platforms: group fields,
hint fields, signal completion. The source implementation made Flutter-specific
choices (`AutofillGroup`, `AutofillHints.*`, `TextInput.finishAutofillContext`);
the target project should use whatever the equivalent is in its framework. The
risk of skipping this is that users cannot use their password manager with the
app, which is a significant UX friction on auth screens.
