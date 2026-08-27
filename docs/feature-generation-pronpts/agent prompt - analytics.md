# Port: Provider-Agnostic Analytics Abstraction

A standalone prompt. Send it to an agent working in any project, in any language, on any platform.

---

## Goal

Design a **tracking plan** and implement a **provider-agnostic analytics abstraction layer** for this
project.

The problem being solved: analytics taxonomies rot. Event names get invented at call sites, error
messages leak into parameters, identifiers blow up cardinality, and a second backend cannot be added
without reshaping every event. Integrating an SDK is an afternoon's work; recovering from an
inconsistent taxonomy takes months, because historical data cannot be renamed.

The two things that prevent this, and which this port exists to reproduce:

1. **The taxonomy is designed and written down before any code is written**, and stays the single
   source of truth afterwards.
2. **Call sites depend on one typed client, never on an SDK.** Backends sit behind a provider
   interface, are registered by wiring rather than by ad-hoc calls, and know nothing about each other.

Everything below serves those two ends. Where a rule cannot be followed in this stack, the intent
above is what must survive.

**Before writing anything**, restate your understanding of this goal in your own words, then work
through *Context to gather*.

---

## Context to gather

Read this project first, then **ask the user about anything the code does not settle.** Do not guess,
and do not proceed to *Plan & confirm* with open questions.

### Discover from the codebase

- The app's purpose and its main user flows — you cannot design events without them.
- Language, framework, and platform.
- Any existing analytics or crash-reporting code, and every call site that would need migrating.
- The dependency-injection / wiring setup, and whether compile-time registration is available.
- Whether the build system has variants, environments, or build flags (dev vs production).
- The layering in use — where presentation logic ends and business/data logic begins.
- **The project's conventions**: naming style, file and module organisation, error-handling idiom,
  test framework and test layout, how existing types express bounded vocabularies.

### Ask the user

- Which analytics backend(s), if any, are to be wired now, and which might be added later.
- Where the tracking plan document should live (a docs directory, a wiki, alongside the code).
- The project's privacy and consent posture, and whether any regulatory regime applies.
- Whether a crash-reporting channel exists or is wanted alongside product analytics.
- Any existing taxonomy with live data — if events are already flowing somewhere, renaming them is a
  breaking change and the plan must account for it.
- Whether the initial scope is the abstraction only, or abstraction plus call sites plus a live
  backend.

---

## Plan & confirm

Two gates. **Do not write production code until both are approved.** They are separate because they
answer different questions: the first is a product decision, the second an engineering one.

### When this guidance and the project disagree

This prompt was written from one project's implementation. It **will** disagree with this project
somewhere — a naming style, an available library, a layering assumption, a build mechanism that does
not exist here.

**Non-trivial discrepancies are discussed with the user, not resolved unilaterally.** Neither side
automatically wins: this guidance is not authoritative over a project it has never seen, and a local
convention is not automatically right either — the point of raising it is that a human decides.

**Trivial — decide yourself, proceed, and record it in the *Summary*:**

- Renaming a type or file to match this project's vocabulary and casing.
- File placement within the analytics module, and how it is split across files.
- Which assertion library, test helper, or mocking approach to use, given what is already here.
- Formatting, import ordering, comment and documentation style.
- Substituting an exact language equivalent — a discriminated union for a sealed type, a protocol for
  an interface, a trait for either.

**Non-trivial — stop and discuss:**

- Adding a **new dependency**, or using a library other than one the project already relies on for
  that job.
- A **structural rule that cannot hold** here: no compile-time wiring, no build variants, no closed
  type system, no distinct presentation layer.
- A **layering conflict** — the project already places logic where this guidance forbids tracking
  calls, or has no layer matching the one nominated.
- An **existing analytics, telemetry, or logging abstraction** that overlaps with what this prompt
  asks you to build. Do not build a parallel one; ask whether to extend, replace, or wrap it.
- Anything requiring **changes outside the analytics module** — refactoring call sites' surrounding
  code, altering a shared base class, moving existing logic between layers.
- Anything touching **privacy, consent, data retention, or what leaves the device**. Never resolve
  these by inference from the code.
- Anything that would make you **deviate from the approved tracking plan**.

**The test:** would a maintainer be surprised to find this decided without them? If it changes
dependencies, architecture, a public surface, or the privacy posture, the answer is yes.

**Timing.** Surface discrepancies at **Gate 2**, batched into one round rather than a stream of
questions. If one only becomes apparent during implementation, **stop and raise it then** — do not
press on and mention it in the *Summary*, and do not quietly pick whichever side is easier. When
raising one, state what the guidance is trying to achieve, what this project does instead, the options
you see, and your recommendation.

### Gate 1 — the tracking plan

#### Step 1: Identify product questions

Before defining any event, identify **3–6 product questions** the analytics must answer. Every event
must answer at least one; an event answering none does not belong in the plan.

Good questions are specific and actionable:

- "Do users who complete step A go on to complete step B, and where do they drop out?"
- "Which features are actually used?"
- "How often does [key operation] fail in the wild?"
- "What shape do [core domain objects] have at the time of [key action]?"

#### Step 2: Draft the event dictionary

- **Names**: `snake_case`, `object_action` order, past tense — `item_added`, not `add_item`.
  Object-first is what makes an alphabetically sorted list group itself.
- **12–20 events maximum** for an initial plan. Beyond that, nobody maintains it.
- Every event records: name, parameters, the action or outcome that triggers it, and which product
  question(s) it answers.
- **No identifiers** (UUIDs, primary keys). Replace with aggregates describing the shape of the thing
  — `{ item_count: 4 }`, not `{ order_id: "a3f9-…" }`. An ID answers no aggregate question, and
  high-cardinality parameters collapse into an `(other)` bucket in most backends.
- **No free text**, ever — no names, addresses, search queries, or error messages. Unbounded
  cardinality and the most common accidental-PII vector.
- **Band continuous values** — distances, durations, long-tail counts become string bands
  (`"under_50km"`, not `47382`). Small bounded counts may be sent raw.
- **Parameter types: primitives only** (`string`, `int`/`number`, `float`, `boolean`). This is the
  intersection of what backends accept, not a universal rule — some accept richer values. Holding to
  the strictest is what keeps events portable when a second destination is added.
- **Emit explicit string values**, never raw enum or constant names — those carry language-specific
  casing and would pollute an otherwise `snake_case` dataset.

Always include a reliability event — `operation_failed` or equivalent — covering tracked failure
paths, with `operation` and `error_type` as its only parameters.

Always include a navigation event — `screen_viewed` or equivalent — with `screen_name`.

#### Step 3: Draft user properties

Sticky attributes of the installation, set by their own call, attached by the backend to every event
logged *afterwards*. They segment the user base; they do not describe an occurrence.

- Typically **2–5** for an initial plan. Backends cap them tightly — spend slots deliberately.
- **They apply forward only.** Set each as early as its value is known, or a session's first events
  cannot be segmented.
- Most backends require registration in a console before a property appears in reports. Unregistered
  properties are accepted silently and surface nowhere — indistinguishable from a broken integration.
- **Only the current value is stored; there is no history.** Filtering on a property that changes over
  time selects by the user's *present* state, including events emitted when the value differed.
- **No name is shared between a user property and an event parameter.** The two name sets must be
  fully disjoint — no exceptions. Report builders list dimensions by name, so two same-named entries
  in different scopes are a mis-click that silently returns a plausible-looking wrong number. When you
  want both the moment and the current state, model them as two questions with two names; usually the
  event needs the narrower one ("is this their first?" rather than a full band).
- Band continuous values, at least as strictly as event parameters. A property rides on every event,
  so a high-cardinality one is a persistent pseudo-identifier.

#### Step 4: Document autocapture decisions

Enumerate every event and property each SDK would collect **automatically**, and make an explicit
decision for each — enable or disable — with a stated reason either way. There is no universal
default; the right call is per-provider and per-event. Two situations always require action:

- **Double-counting.** If an automatic event duplicates a declared one, resolve to a single source:
  either disable autocapture and map the declared event onto the backend's reserved name, or drop the
  declared event and rely on autocapture. Pick one.
- **Privacy floor.** Autocapture can put URLs with query parameters, element labels, user-entered
  content, or coordinates into the pipeline — bypassing code review entirely, because no event is
  declared anywhere. If that violates the project's posture, disable it. If not, record why.

Anything left at its SDK default without a recorded decision is a gap, not a default.

#### Step 5: Record the consent posture

State explicitly whether analytics fires before consent is obtained, and why that position is
defensible — or what work will close it, with a named owner. "We haven't thought about it" is not a
posture. Whatever the model, a consent implementation has to cover: a gate over the whole provider
set (not per-provider checks); propagation to each SDK's own consent API, not just client-side
dropping; persisted state with a way to withdraw that takes effect without a restart; **no event
fired before the user has interacted with the consent surface**; and a decision on whether crash
reporting shares the analytics basis or gets its own.

#### Tracking plan output format

Write a markdown file at the path agreed in *Context to gather*:

```
# Tracking Plan

## Product Questions
Table: # | Question | Why it matters

## Event Dictionary
One subsection per category (funnel, feature adoption, reliability, screens).
Table per subsection: Event | Parameters | Trigger | Answers

## Parameter Reference
Table: Parameter | Type | Allowed values | Notes

## User Properties
Table: Property | Type | Allowed values | Set when

## Consent Posture
Current position and its rationale.

## Autocapture
Table: Event/Property | Status (Disabled / Enabled) | Reason
```

**Stop. Present the tracking plan and get explicit approval before continuing.**

### Gate 2 — the implementation plan

Once the tracking plan is approved, propose — and get explicit confirmation on — an implementation
plan covering:

- The **components** you will create, named in this project's vocabulary (see *Implementation*).
- The **files to be created or modified**, listed explicitly.
- The **libraries, SDKs, and language mechanisms** you intend to use, and why each suits this stack.
- The **call sites** to be added or migrated, one line per tracking-plan event.
- The **wiring mechanism** for provider registration, and the **build-time mechanism** gating the
  development-only provider.
- The **test strategy**, and what will be verified manually because it cannot be automated.
- **Every non-trivial discrepancy** between the guidance below and this project — per the rules above
  — each with the options you see and your recommendation.

**Wait for confirmation. Do not write production code before you have it.**

---

## Implementation

The approved tracking plan is the source of truth for every implementation decision.

> **Guidelines, not prescriptions.** What follows is expressed in modules, interfaces, and patterns —
> not syntax. The intent is what matters: separation of concerns, a typed taxonomy, provider fan-out,
> and development-only logging. The exact mechanism depends on what this language and framework make
> idiomatic and possible.
>
> If a guideline cannot be followed — the language has no interfaces, no compile-time DI, no
> discriminated unions, no build variants — **do not force it and do not silently drop it.** Raise it
> under *When this guidance and the project disagree*, above.

**Follow this project's existing conventions throughout** — naming style, file and module
organisation, error-handling idiom, test structure. Where a convention here conflicts with one below,
apply the triviality test in *When this guidance and the project disagree*: settle the small ones
yourself and record them in the *Summary*; **raise the rest with the user rather than deciding which
side wins.**

### Architecture

Place all analytics code in a **dedicated cross-cutting module or package**, separate from business
logic, data access, and UI. No business-logic or data-layer code may import or reference it — the
domain must not know it is being observed.

Keep contracts and implementations together. Analytics is one cohesive concern: the composite client
is not a repository and the providers map no DTOs, so splitting it across layers buys nothing.

> **The names below express roles, not required identifiers.** Adopt this project's vocabulary — if it
> says `Telemetry` or `Tracker`, use that. What must survive is the set of roles and the boundaries
> between them.

| Role | Purpose |
|------|---------|
| `AnalyticsTracking` | Shared interface/protocol/trait: `logEvent`, `setUserProperty`, `trackException` |
| `AnalyticsClient` | What call sites depend on — extends `AnalyticsTracking`, adds nothing |
| `AnalyticsProvider` | One implementation per backend — extends `AnalyticsTracking`, adds `shouldLog(event)` |
| `CompositeAnalyticsClient` | Fans out to every registered provider; silent no-op with zero providers |
| `AnalyticsEvent` | Discriminated union / sealed type — the full event taxonomy in one place |
| `AnalyticsUserProperty` | Discriminated union / sealed type — all user properties |
| `AnalyticsOperation` | Bounded type — the operation names used by `operation_failed` and `trackException` |
| One type per bounded parameter vocabulary | Each with an explicit `value` field in `snake_case` |
| `ConsoleAnalyticsProvider` | Development-only logging sink |
| Wiring | Providers registered at compile time or app startup, never by ad-hoc runtime calls |

```
interface AnalyticsTracking {
    logEvent(event: AnalyticsEvent): void
    setUserProperty(property: AnalyticsUserProperty): void
    trackException(error: Error, operation: AnalyticsOperation): void
}

interface AnalyticsClient extends AnalyticsTracking { }

interface AnalyticsProvider extends AnalyticsTracking {
    shouldLog(event: AnalyticsEvent): boolean
}
```

**Declare the tracking surface once**, in the shared supertype both sides implement. Declaring the
same operations separately on client and provider lets the two drift, and every operation added later
has to be remembered twice.

`AnalyticsClient` adds nothing of its own. It exists to name what call sites depend on, and to stop
them reaching for `AnalyticsProvider` and its filter.

`shouldLog` gates events only. A provider that wants to ignore user properties or exceptions no-ops
its own implementation — that is what lets a crash-reporting provider subscribe to failure events
only while a product-analytics provider takes everything, with neither knowing the other exists.

**Providers must be safe to call from any thread or async context.** Document this on every
implementation.

**Register providers by wiring, not by a runtime `register()` call.** Runtime registration depends on
somebody remembering to call it during startup, and fails silently when they don't.

Each provider owns its own backend's quirks — name-length limits, reserved prefixes, parameter
mapping, event-name translation. No other layer knows which backends exist.

### Event taxonomy rules

Model every event from the tracking plan as a **typed entry in one place**, with explicit named
parameters. No ad-hoc dictionaries, no string constants scattered across controllers — scattered
constants cannot be cross-checked against the plan and are how taxonomies rot. A closed type makes
the full event set readable in one file and makes an untracked event a compile error rather than a
typo.

All Step 2 rules apply to the code as written: `snake_case` `object_action` past-tense names,
primitives only, no identifiers, no free text, banded continuous values.

**Emit the explicit string value, never the raw enum or constant name.** This is a structural fix
rather than a convention to remember — it makes the correct thing the easy thing.

Give each bounded parameter vocabulary its **own dedicated type in its own file**, with no exception
for small ones. A three-line file holding a single enum is normal.

Where such a type **maps or classifies** — banding a continuous value, resolving a route, translating
a domain type — it is behaviour rather than a label list, and **requires unit tests covering zero,
negative, and band-edge inputs**. A flat vocabulary with no logic needs none. This distinction governs
test requirements only, never file layout.

Values shared between an event parameter and a user property must be **top-level types**, not nested
inside the event type — nesting couples a shared vocabulary to one of its two consumers.

### User properties

Set via the client's `setUserProperty`, never passed with an event. Apply the Step 3 rules as written
— especially the disjoint-names rule, which nothing in the code will enforce for you.

### Exceptions vs events

On every failure path, fire **both**, through separate channels:

| Channel | Goes to | Carries | Answers |
|---------|---------|---------|---------|
| `logEvent(OperationFailed(operation, errorType))` | product analytics | bounded types only | *how often* it fails, for how many users |
| `trackException(error, operation)` | crash reporting only | the real error and stack trace | *where and why* it fails |

`trackException` exists because crash reporting needs a stack trace the event taxonomy cannot carry. A
failure event is bounded to enum values by design — that is what keeps free text and PII out of
product analytics — so it has nowhere to put an error object. Routing exceptions through the taxonomy
would either strip the stack trace, making the crash report almost worthless, or smuggle an error
message into a parameter, breaking the privacy floor.

**The error object, its message, and its stack trace must never become event parameters.**
Product-analytics providers ignore `trackException` entirely; crash-reporting providers filter
`logEvent` via `shouldLog`.

Emit failure events on the failure path of existing result handling, next to the user-facing error, so
the two cannot drift apart.

### Where tracking calls live

| Layer | Allowed |
|-------|---------|
| Presentation (ViewModel / Store / Controller / Presenter) | **Yes** — alongside the action and result handling that causes the event |
| View / component (components, templates, composables) | **No** — fires on re-render; not testable in isolation |
| Business logic (use cases, services, domain) | **No** — the domain must not know it is being observed |
| Data (repositories, API clients, database) | **No** |

**Screen-view events fire from navigation or routing changes**, not from component mount or controller
construction. Construction fires once per instance: it misses re-entry via back navigation and can
double-fire on state restore. Keep the route-to-screen mapping in a testable non-component function.

### Autocapture

Implement the Step 4 decisions in code, for every backend being wired. **Disable autocapture
explicitly at initialisation** rather than relying on a default — SDKs collect events you did not ask
for, and both failure modes (double-counting, privacy-floor bypass) escape code review because no
event is declared anywhere.

### Development-only provider

The console provider must be active in **development builds only**. Its output is readable by anyone
with access to the device or host, so shipping it in production leaks the event stream.

**Gate it with a build-time mechanism** — an environment variable, a build flag, a wiring binding
placed in a development-only source set — **not a runtime conditional inside the provider.** A runtime
flag is a second knob that can be set wrong; build-time exclusion is the only reliable gate.

Keep the provider class itself in the shared source set and move only its registration, so its unit
test and coverage reporting need no special case.

**Verify the project compiles and runs with zero providers registered.** Production reaches this state
genuinely — a release build has no providers at all until a real backend is wired. With none present,
every operation is a silent no-op, not an error and not a crash. Document that on the composite
client. Some wiring frameworks need an explicit empty-set declaration for this to compile.

---

## Verification

Run everything that can be run, and **report the actual results** — command output, pass/fail counts,
findings. Do not report a step as passing without having run it.

**Automated**

1. The project **builds** cleanly, in both development and production configurations.
2. **Lint and static analysis** pass, with no new findings introduced.
3. **Unit tests** for every presentation-layer change: dispatch the action, assert the resulting state
   or side-effect, and assert the expected tracking call.
4. **Unit tests** for every type containing mapping or banding logic — zero, negative, and band-edge
   inputs.
5. **Unit tests** for the composite client, including the zero-provider no-op case.
6. A build with **no providers registered compiles and runs** without throwing.

**Manual checklist** — the parts no test covers

7. Exercise each feature in a development build and confirm via the console provider that every event
   fires **exactly once**, with the expected name and parameters. Double-fires and missed
   back-navigation are the two failures unit tests will not catch.
8. Inspect every logged payload and confirm **no identifier, free text, error message, coordinate, or
   other value excluded by the privacy posture** appears in any of them.
9. Confirm a **production build emits no development logs**.
10. If consent is gated: observe traffic from a **cold start on a fresh install** and confirm no event
    fires before the consent surface has been answered. This is the requirement that fails silently
    and the one regulators test for — verify it by observation, not by reading code.
11. **Cross-check the plan against the code in both directions.** Every event in the code has a
    tracking-plan entry; every plan entry is implemented. Drift either way is a defect. Do not add
    events absent from the plan, and do not omit events present in it — if the code needs to change,
    update the plan first.
12. If a backend was wired: confirm every custom parameter and user property is **registered in that
    backend's console**, or it will surface nowhere despite being accepted silently.

---

## Summary

Finish by reporting, in prose:

**Ported as-is** — which parts of the design transferred unchanged.

**Adapted** — what changed for this stack and why: the language mechanism substituted for a sealed
type or interface, the wiring mechanism used, and how the development-only gate was achieved. Split
this into **decided by you** (the trivial discrepancies you settled alone, listed so the user can
object to any of them now) and **agreed with the user** (what was raised and how it was resolved).

**Unresolved decisions** — flag every one of these that this project has not settled, rather than
letting it pass as decided:

- **Consent.** Is analytics gated? If deferred, that is legitimate only as a recorded, temporary
  position with a named owner and a scheduled piece of work to close it — never an assumption living
  in someone's head.
- **Crash-reporting basis.** Does it share the analytics consent basis or get its own? It commonly
  gets its own; what is not acceptable is leaving it ungated because nobody considered it.
- **Band boundaries.** These are a first guess at a distribution nobody has measured yet. Cheap to
  change *before* data accumulates, expensive after — err toward more bands rather than fewer, and
  say when they should be revisited.
- **User identity.** If the app has no authentication yet, record that a user ID is a deliberate
  privacy escalation to revisit when auth lands, not something that should arrive as a side effect of
  adding it.
- **The primitive-only parameter rule.** A constraint of the strictest backend, held deliberately for
  portability. Note it, so a future maintainer knows it is a choice rather than a platform limit.
- **The 12–20 event ceiling.** A maintainability heuristic, not a hard limit. Note where the plan sits
  against it and what headroom remains.
- **Anything the codebase left ambiguous** that a reader would otherwise assume was decided.

**The window closes.** Before a real backend ships, the taxonomy can be rewritten freely — there are
no consumers. Afterwards, renaming or removing a live event breaks every dashboard and funnel built on
it, and historical data cannot be renamed. Say plainly whether that window is still open.
