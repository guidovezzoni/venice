# Agent Prompt: Design and Implement Analytics Abstraction Layer (Language-Agnostic)

Use this prompt to instruct an agent to design a tracking plan and implement a provider-agnostic
analytics abstraction for any application, regardless of language, platform, or framework.

---

You are designing a tracking plan and implementing a provider-agnostic analytics abstraction layer.
The work has two sequential phases: **Plan** (define what to track and why) then **Build** (implement
the abstraction). Do not write any production code before the tracking plan is approved.

---

## CONTEXT YOU MUST READ FIRST

Before doing anything, read:
- The existing codebase to understand: the app's purpose, its main user flows, the language and
  framework in use, any existing analytics code, and the DI/wiring setup.
- Apply idiomatic patterns for the points below that say "use your language's equivalent".

---

## PHASE 1 — DESIGN THE TRACKING PLAN

### Step 1: Identify product questions

Before defining any event, identify 3–6 product questions the analytics must answer. Every event
must answer at least one of them; an event answering none does not belong in the plan.

Good product questions are specific and actionable:
- "Do users who complete step A go on to complete step B, and where do they drop out?"
- "Which features are actually used?"
- "How often does [key operation] fail in the wild?"
- "What shape do [core domain objects] have at the time of [key action]?"

### Step 2: Draft the event dictionary

For each question, identify the events that answer it. Apply these rules:

- **Names**: `snake_case`, `object_action` order, past tense — `item_added`, not `add_item`
- **12–20 events maximum** for the initial plan. Beyond that, nobody maintains it.
- Every event has: a name, its parameters, the user action or system outcome that triggers it,
  and which product question(s) it answers.
- **No identifiers** (UUIDs, PKs) — replace with aggregates (counts, bands)
- **No free text** — classify errors to a bounded vocabulary (`error_type: "network"`)
- **Band continuous values** — distances, durations, long-tail counts become string bands, not
  raw numbers. Small bounded counts (e.g. `item_count` capped by the app) may be sent raw.
- **Parameter types**: primitives only (`string`, `int`/`number`, `float`, `boolean`)
- **Emit explicit string values**, never raw enum/constant names (which are language-specific case)

Always include an `operation_failed` (or equivalent) reliability event covering tracked failure
paths, with `operation` and `error_type` as its only parameters.

Always include a `screen_viewed` (or equivalent) navigation event with `screen_name`.

### Step 3: Draft user properties

User properties are sticky attributes set independently of events; the backend attaches them to
every subsequent event for segmentation.

- Typically 2–5 properties for an initial plan (backends cap them; spend slots deliberately)
- Set as early as the value is known — they apply forward only, never retrospectively
- No name is shared between a user property and an event parameter — the two name sets must be
  fully disjoint. The same name in two scopes causes analytics tools to silently return wrong numbers.
- Band continuous values for the same reasons as event parameters

### Step 4: Document autocapture decisions

List every event the analytics SDK(s) would collect automatically. For each, make an explicit
decision: enable or disable, with a stated reason either way. There is no universal default —
the right call depends on the provider, the event, and the project's privacy posture.

Two situations always require a decision:
- **Double-counting**: if an automatic event duplicates a declared one, one of them must be the
  sole source. Either disable the automatic collection and map the declared event onto the
  backend's reserved name, or remove the declared event and rely on autocapture — pick one.
- **Privacy floor**: if an automatic event captures free text, URLs with query parameters,
  user-entered content, or coordinates, assess whether that violates the project's privacy posture.
  If it does, disable it. If it does not, record why.

Any event left enabled without a recorded reason is a gap, not a default.

### Step 5: Record the consent posture

State explicitly whether analytics fires before user consent is obtained, and why that position is
defensible (or what story will close it). "We haven't thought about it" is not a posture.

### Tracking plan output format

Produce a markdown file saved at [PATH_TO_TRACKING_PLAN] with these sections:

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

**Stop here and present the tracking plan for review before writing any code.**
Confirm the plan is approved before proceeding to Phase 2.

---

## PHASE 2 — IMPLEMENT THE ABSTRACTION

Use the approved tracking plan as the source of truth for all implementation decisions.

---

## ARCHITECTURE TO IMPLEMENT

Place all analytics code in a dedicated cross-cutting module or package if possible — separate from business
logic, data access, and UI layers. No business-logic or data-layer code may import or reference
analytics. Tracking calls belong in the presentation/controller layer only (ViewModel, Store,
Controller, Presenter, or equivalent).
