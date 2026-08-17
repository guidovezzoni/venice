# Analytics Guidelines

These are the standing decisions for product analytics. They are deliberately written to be
project-agnostic so they can be reapplied to other apps — anything Venice-specific is called out as
such.

The companion artefact is the **tracking plan** at `docs/analytics/tracking-plan.md`, which is the
single source of truth for *which* events exist. This file governs *how* events are designed, named,
and delivered.

## Core Principle

**The taxonomy is the product, not the SDK.** Integrating an analytics SDK is an afternoon's work;
recovering from an inconsistent event taxonomy takes months, because historical data cannot be
renamed. Design the events first, write them down, then wire the SDK.

A corollary: **an event that answers no question must not be added.** Every event exists to answer a
stated product question. If nobody can name the question, the event is noise that costs cardinality
budget and review attention.

## Naming Conventions

| Dimension | Rule | Example |
|-----------|------|---------|
| Casing | `snake_case` | `route_calculated` |
| Format | `object_action` — object first | `stop_added`, not `add_stop` |
| Tense | Past simple — the event records something that happened | `trip_created`, not `trip_create` |
| Vocabulary | One word per concept, always the same one | always `stop`, never `waypoint`/`location` |

Object-first is what makes an alphabetically sorted event list group itself: every `stop_*` event sits
together, every `trip_*` event sits together. Action-first scatters them.

**Parameter and user-property names** follow the same `snake_case` rule. Event-parameter names and
user-property names must be **mutually exclusive** — never reuse one name for both, or it becomes
impossible to tell "the user's current state" from "their state at the time of the event".

Keep the initial event set to **12–20 events**. Beyond that, nobody maintains the plan.

## Parameter Design

### Never send identifiers

Raw IDs (UUIDs, primary keys) must not be sent as event parameters. This is not only a privacy rule —
it is a *usefulness* rule:

- High-cardinality parameters collapse into an `(other)` bucket in GA4 and most analytics backends,
  making them unreportable.
- Each distinct parameter name consumes one of a limited number of registered custom definitions.
- An ID answers no aggregate question. "Which trip?" is not a product question; "how many stops did
  trips have when the route was calculated?" is.

Replace identifiers with **aggregates** describing the shape of the thing:

```
BAD   route_calculated { trip_id: "a3f9-8c21-…" }
GOOD  route_calculated { stop_count: 4, leg_count: 3 }
```

### Band continuous values

Continuous quantities (distances, durations, counts with a long tail) are sent as **bands**, not raw
numbers, unless the raw number is genuinely needed as a metric. Bands keep cardinality bounded and are
what you actually chart.

```
BAD   trip_created { trip_count: 47 }
GOOD  trip_created { trip_count_band: "6_plus" }
```

Small bounded counts (`stop_count`, `leg_count`) may be sent raw — they are naturally low-cardinality
and useful as numeric metrics.

### Never send free text

No user-entered strings, ever: no place names, no trip names, no search queries, no error messages.
Free text is unbounded cardinality *and* the most common accidental-PII vector.

Where a failure needs describing, classify it into a **bounded enum** rather than forwarding the
exception message:

```
BAD   operation_failed { error_message: "Unable to resolve host maps.googleapis.com" }
GOOD  operation_failed { error_type: "network" }
```

### Parameter values must be primitives

Values are restricted to `String`, `Int`, `Long`, `Double`, `Boolean`. This is a hard platform
constraint, not a style choice — Firebase carries parameters in a `Bundle`, which cannot hold
arbitrary objects. Anything else is silently dropped or throws at runtime.

The constraint is enforced structurally by building every event from a typed sealed-class constructor
rather than an ad-hoc map, so a non-primitive cannot reach a provider.

### User properties are not event parameters

A **user property** is a sticky attribute of the installation, set by its own call rather than passed
with an event, and automatically attached by the backend to every event logged *afterwards*. It exists
to segment the user base — "show me this funnel for imperial users versus metric users" — not to
describe a single occurrence.

Four consequences that follow from that and are easy to get wrong:

- **They apply forward only.** Events already logged do not gain the property retrospectively. Set
  properties as early as the value is known, or the session's first events cannot be segmented.
- **They must usually be registered with the backend** before they appear in reports or audiences
  (Firebase: *Analytics → Custom Definitions*). An unregistered property is accepted silently and shows
  up nowhere, which is indistinguishable from a broken integration.
- **The budget is much smaller than for events** — Firebase allows 25 custom user properties per
  project against 500 distinct event types. Names are case-sensitive, so a casing slip spends two.
- **Only the current value is stored; there is no history.** Filtering a report on a property that
  changes over time selects events by the user's *present* state, including events emitted when the
  value was different. That is a different question from "how did users in that state behave", and it
  is a common way to mislead yourself.

Where the distinction matters for a value that changes, send it **both** ways: as an event parameter it
freezes the value at that moment, as a user property it tracks the current state. Do this deliberately
and document it, not by default.

Because a user property rides on every event, a high-cardinality one is effectively a persistent
pseudo-identifier. Bound the values as strictly as event parameters — more strictly, if anything.

### Privacy floor

Events carry **no personal data**: no identifiers, no free text, no coordinates, no place names, no
account details. This floor was set by stories 3.2.1, 3.2.2 and 4.1.1 and applies to every event
added since.

Where a story's data is inherently sensitive (locations, in Venice's case), the aggregate-parameter
rule and the privacy floor point the same way — which is the sign that the rule is right.

## Architecture

### Layering

**Analytics lives in `core/`, not `domain/`.** It carries no business rules, and no use case or
repository should ever reference it — the domain layer must not know it is being observed. Putting the
taxonomy in `domain/` would weaken what that layer communicates and set a precedent for the next
cross-cutting concern. See the `core/` charter in `guidelines-android.md`.

Contracts and implementations stay together, because analytics is one cohesive concern — the composite
client is not a repository and the providers map no DTOs, so a `domain`/`data` split buys nothing.

```
core/analytics/   AnalyticsClient            interface — what call sites depend on
                  AnalyticsProvider          interface — one implementation per backend
                  AnalyticsEvent             sealed class — the event taxonomy, in code
                  AnalyticsUserProperty      sealed class — the user properties, in code
                  <enums>                    bounded parameter vocabularies, one per file
                  CompositeAnalyticsClient   fans out to every registered provider
                  DebugAnalyticsProvider     Logcat sink, debug builds only
                  <Backend>AnalyticsProvider Firebase, Crashlytics, …
di/               AnalyticsModule            @Binds @IntoSet per provider
```

The backend providers are the only part carrying a heavy third-party dependency. In a single-module
app that is invisible; if the project is ever modularised they are the natural extraction into their
own module, so a `:core` module stays dependency-light.

`AnalyticsClient` exposes two operations — `logEvent(event)` and `setUserProperty(property)` — and both
fan out to every registered provider. `shouldLog` filters events only; a provider that wants to ignore a
user property no-ops its own implementation.

**The taxonomy lives in one sealed class**, not as string constants scattered across call sites.
Scattered constants cannot be cross-checked against the tracking plan and are how taxonomies rot.
A sealed class makes the full event set readable in one file and makes an untracked event a compile
error rather than a typo.

**Enumerated parameter values are enums carrying an explicit `value: String`** in `snake_case`. Always
emit `.value`, **never** `.name` — Kotlin enum names are `SCREAMING_SNAKE_CASE` and would leak into an
otherwise snake_case dataset. This is a structural fix rather than a convention to remember: it makes
the correct thing the easy thing.

**Each enum lives in its own file**, per the project's one-class-per-file rule — no exception for small
ones, consistent with `StopStatus` and `StopType`, which are three-line files.

Where such an enum **maps or classifies** — banding a continuous value, resolving a navigation route,
translating a domain type — it is behaviour rather than a label list, and **requires boundary tests**
covering zero, negative, and band-edge inputs. A flat vocabulary with no logic needs no such tests.
This distinction governs test requirements only, never file layout.

Values shared between an event parameter and a user property must be **top-level types**, not nested
inside `AnalyticsEvent`. Nesting couples a shared vocabulary to one of its two consumers.

### Providers

Providers are registered by **compile-time DI multibinding** (`@Binds @IntoSet`), not by a runtime
`register()` call. Runtime registration depends on somebody remembering to call it from
`Application.onCreate` and fails silently when they don't.

Each provider owns its own backend's quirks — name-length limits, reserved prefixes, parameter
mapping, event-name translation. No other layer knows which backends exist.

`AnalyticsProvider.shouldLog(event)` is the per-provider filter. It is what lets a crash-reporting
provider subscribe only to failure events while a product-analytics provider takes everything, without
either one knowing about the other.

Providers must be **thread-safe** — `logEvent` may be called from any thread or coroutine context.
Document this on every implementation.

### Debug provider is debug-only

The Logcat provider must be registered in **debug build variants only**. Logcat is readable over ADB
on any connected device, so shipping it in release leaks the event stream to anyone with physical
access.

The gate is the **DI binding placed in the `debug` source set**, not a runtime flag:

```
app/src/main/java/…/core/analytics/DebugAnalyticsProvider.kt    class
app/src/debug/java/…/di/DebugAnalyticsModule.kt                 @Binds @IntoSet — debug only
```

Keep the class in `main` and move only its binding, so its unit test stays in the ordinary `src/test`
source set and coverage tooling needs no special case. A build-type `BuildConfig` flag checked inside
`shouldLog()` is *not* the mechanism — a second knob only adds a way to get it wrong.

When no provider is bound for a variant, the module needs an explicit
`@Multibinds` declaration for the provider set, or the variant will not compile.

### Where tracking calls live

Tracking calls belong in the **ViewModel**, alongside the intent handling that causes them. Not in
composables (they are presentational), not in use cases or repositories (analytics is a presentation
concern, and domain code should not know it is being observed).

Screen-view events are driven from **navigation**, not from ViewModel `init`. An `init` block fires
once per ViewModel instance, which means it misses re-entry via back navigation and double-fires on
process-death restore. Observe the navigation controller's destination changes instead, and keep the
route-to-screen mapping in a testable non-composable function.

Failure events are emitted on the failure path of the existing result handling, next to the user-facing
error, so the two cannot drift apart.

## Process

### Every feature story defines its own analytics

**A feature story is not refined until it has an `## Analytics` section.** That section names the
events the feature emits, their parameters, and the product question each answers — following the
conventions above.

Analytics is a product requirement of the feature that generates the data, not a follow-up task and
not a separate story. Deferring it produces features that ship blind and a taxonomy assembled
retroactively by whoever is left holding it.

The mechanism is the refinement prompt in `docs/sdlc/commands/sdlc_open_story.md`, which lists this
file as a context file and analytics as a refinement requirement.

### Changing the taxonomy

1. Update `docs/analytics/tracking-plan.md` first — the plan is the source of truth.
2. Then change the code to match.
3. Renaming or removing a **live** event is a breaking change: historical data cannot be renamed, so
   dashboards and funnels built on it break. Prefer adding a new event and deprecating the old one in
   the plan.

Before any backend is wired, none of this applies — with no consumers, the taxonomy can be rewritten
freely. **That is the moment to get it right**, and the window closes permanently when the first real
provider ships.

### Verification

- Every event in the code has a tracking-plan entry, and every plan entry is implemented. Drift in
  either direction is a defect.
- On-device: exercise the feature and confirm via `adb logcat -s Analytics` that each event fires
  **once**, with the expected name and parameters, and that no identifier, free text, place name, or
  coordinate appears in any payload.
- Confirm a release build does not emit debug logs.

## Consent

**Current status (Venice):** consent is **assumed granted**. Analytics ships without a consent gate
until story 9.3.1 lands. This is a deliberate, temporary position, recorded here rather than left
implicit.

It is defensible only because of the privacy floor above — events carry no personal data — and because
`docs/publishing/Privacy Policy.md` already discloses analytics SDK use. It is **not** a general
recommendation: an app collecting identifiers, location, or account data needs consent before the
first event fires.

When 9.3.1 is implemented it must cover:

- A consent decorator over the provider set, so no provider is reached before consent is resolved.
- `FirebaseAnalytics.setConsent(ANALYTICS_STORAGE, …)` and `setCrashlyticsCollectionEnabled`.
- Persisted consent state and a user-accessible way to withdraw it.
- No event fired before the user has interacted with the consent surface. Regulators test for exactly
  this: SDKs firing during app start, before the consent dialog is answered.

Venice has no settings screen, which is why this is blocked rather than merely pending.

## Anti-Patterns

| Anti-pattern | Why it fails |
|---|---|
| Event names as `const val` in each ViewModel | Cannot be cross-checked against the plan; guarantees drift |
| Sending `error.message` | Unbounded cardinality and an accidental-PII vector |
| Emitting `.name` from a Kotlin enum | Leaks `SCREAMING_SNAKE_CASE` into an otherwise snake_case dataset |
| Adding events "because we might want them later" | Costs cardinality budget now, answers nothing |
| Tracking in composables | Fires on recomposition; untestable |
| Screen views in ViewModel `init` | Misses back-navigation, double-fires on process restore |
| Logcat provider in release builds | Leaks the event stream over ADB |
| Wiring the SDK before writing the plan | Locks in names you cannot change later |
