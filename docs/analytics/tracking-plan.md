# Venice Tracking Plan

The single source of truth for which analytics events exist. Conventions governing how events are
designed live in `docs/guidelines/guidelines-analytics.md`.

**Status:** target state. Implemented by Epic 9 stories 9.1.2 (taxonomy) and 9.1.3 (screen views).
Until those land, the code emits the legacy events in the [migration table](#migration-from-the-legacy-taxonomy).

**Rule:** every event in the code has an entry here, and every entry here is implemented. Drift in
either direction is a defect.

## Product Questions

Every event exists to answer one of these four. An event answering none of them does not belong in the
plan.

| # | Question | Why it matters |
|---|----------|----------------|
| **Q1** | Do people who create a trip actually finish planning it, and where do they drop out? | Identifies the biggest leak in the core flow — the highest-leverage thing to fix |
| **Q2** | Which features actually get used? | Tells us what to invest in and what to cut |
| **Q3** | How often do Places lookups, route calculations, and persistence fail in the wild? | Failures invisible in development; the difference between "works" and "works for users" |
| **Q4** | What shape are real trips? | Informs performance work and future features (Android Auto, live position) |

## Event Dictionary

14 events. All names `snake_case`, `object_action`, past tense. No identifiers, no free text, no
coordinates, no place names — see the privacy floor in the guidelines.

### Funnel and activation

| Event | Parameters | Trigger | Answers |
|-------|-----------|---------|---------|
| `trip_created` | `trip_count_band` | A trip is successfully persisted | Q1, Q4 |
| `trip_opened` | `stop_count`, `route_state` | Trip detail screen is opened for an existing trip | Q1, Q4 |
| `stop_added` | `stop_type`, `stop_count` | A starting point, destination, or intermediate stop is persisted | Q1, Q2 |
| `route_calculated` | `stop_count`, `leg_count`, `distance_band`, `duration_band` | Route calculation succeeds | Q1, Q4 |
| `navigation_launched` | `stop_type`, `stop_position` | An external navigation app is launched for a stop | Q1, Q2 |

The activation funnel:

```
trip_created
  → stop_added (stop_type = starting_point)
  → stop_added (stop_type = destination)
  → route_calculated            ← activation
  → navigation_launched         ← value delivered
```

`route_calculated` is the activation moment: it is the first point at which the app has produced
something the user could not trivially get elsewhere.

### Feature adoption

| Event | Parameters | Trigger | Answers |
|-------|-----------|---------|---------|
| `stop_edited` | `stop_type` | A stop edit is persisted | Q2 |
| `stop_removed` | `stop_type`, `stop_count` | A stop removal is persisted | Q2 |
| `stop_reordered` | `direction`, `stop_count` | A stop move up/down is persisted | Q2 |
| `stop_departed` | `stop_position`, `stop_count` | A stop is marked departed | Q2 |
| `stop_departure_undone` | `stop_position` | A departure is undone | Q2 |
| `place_search_performed` | `suggestion_count` | A Places autocomplete query returns | Q2, Q3 |
| `place_suggestion_selected` | `suggestion_position` | A suggestion is chosen from the list | Q2 |

`place_search_performed` carries **`suggestion_count` only — never the query text**. The count alone
answers what we need: whether Places is returning usable results. `suggestion_position` shows whether
the top result is good enough, which is the same question from the other side.

### Reliability

| Event | Parameters | Trigger | Answers |
|-------|-----------|---------|---------|
| `operation_failed` | `operation`, `error_type` | Any tracked operation fails | Q3 |

Covers handled failures only, and answers **how often** something fails.

It is deliberately paired with a separate, non-analytics channel — `trackException(throwable, operation)`
— which answers **where and why** by carrying the real throwable and stack trace to crash reporting
only. Both fire on a failure path. The throwable never becomes an event parameter, which is what lets
the diagnostic payload be rich while `operation_failed` stays bounded to two enum values.

Nothing on the exception channel is part of this tracking plan: it never reaches product analytics, so it
has no event name, no parameters, and no custom definitions to register.

### Screens

| Event | Parameters | Trigger | Answers |
|-------|-----------|---------|---------|
| `screen_viewed` | `screen_name` | Navigation destination changes | Q1, Q2 |

Fired from navigation destination changes, **not** ViewModel `init` — so it correctly re-fires on
back-navigation and does not double-fire on process-death restore.

## Parameter Reference

| Parameter | Type | Allowed values | Notes |
|-----------|------|----------------|-------|
| `stop_type` | String | `starting_point`, `destination`, `intermediate` | From `StopTypeParam.value` — never enum `.name` |
| `stop_count` | Int | 0–n | Raw; naturally bounded by the app's stop limit |
| `stop_position` | Int | 0-based ordinal | Position in the stop list, not an identifier |
| `leg_count` | Int | 0–n | Raw; always `stop_count - 1` for a complete route |
| `route_state` | String | `none`, `stale`, `complete` | Mirrors the recalculation-prompt condition |
| `direction` | String | `up`, `down` | Reorder direction |
| `distance_band` | String | `under_50km`, `50_200km`, `200_500km`, `500_1000km`, `over_1000km` | Banded total route distance |
| `duration_band` | String | `under_1h`, `1_3h`, `3_6h`, `6_12h`, `over_12h` | Banded total route duration |
| `trip_count_band` | String | `0`, `1`, `2_5`, `6_plus` | Trips the user has after the action |
| `suggestion_count` | Int | 0–n | Places results returned |
| `suggestion_position` | Int | 0-based ordinal | Which suggestion was chosen |
| `operation` | String | `create_trip`, `set_stop`, `edit_stop`, `remove_stop`, `move_stop`, `mark_departed`, `undo_mark_departed`, `calculate_route`, `search_place`, `resolve_place` | From `AnalyticsOperation.value` |
| `error_type` | String | `network`, `timeout`, `not_found`, `permission_denied`, `quota_exceeded`, `persistence`, `unknown` | From `AnalyticsErrorType.value`, classified from the throwable |
| `screen_name` | String | `trip_list`, `trip_detail` | From `AnalyticsScreen.value` |

**14 distinct parameter names.** Each must be registered in the Firebase console as a **custom
dimension** (the `String` ones) or a **custom metric** (the `Int` ones) before it appears in reports.
Registered custom definitions are capped per property — confirm the current limit when 9.2.1 is
refined — but 14 leaves ample room for Epics 6–8 to add their own. No single event carries more than
four parameters, well inside the hard limit of 25 per event type.

`distance_band` and `duration_band` boundaries are a first guess at a road-trip planner's distribution.
Revisit once real data exists — banding is cheap to change *before* data accumulates and expensive
after, so err toward more bands rather than fewer.

## User Properties

Set via `AnalyticsClient.setUserProperty(...)`, not passed with events. The backend then attaches them
to every event logged **afterwards** — they are not applied retrospectively.

| Property | Type | Allowed values | Set when |
|----------|------|----------------|----------|
| `trip_count_band`| String | `0`, `1`, `2_5`, `6_plus` | Trip list loads, or a trip is created/removed |
| `distance_unit` | String | `metric`, `imperial` | App start — already derived by `DistanceFormatter` |

Both must be **registered in the Firebase console** under *Analytics → Custom Definitions* before they
appear in reports or as audience criteria. Unregistered properties are accepted silently by the SDK and
surface nowhere. Budget: 25 custom user properties per project, so these two spend 2 of 25 — ample room,
but names are case-sensitive and a casing slip spends an extra slot.

`distance_unit` must be set at app start, before the first event, or that session's early events cannot
be segmented by unit system.

**No user ID.** There is no authentication until Epic 8. When it lands, revisit deliberately — a user
ID is a significant privacy escalation and should not arrive as a side effect of adding auth.

> `trip_count_band` appears as both an event parameter on `trip_created` and a user property. This is
> the one intentional overlap, and the reason is that its value changes over time while the backend
> stores only the **current** value, with no history. As a user property it answers "how do people who
> are *now* power users behave?"; as a parameter on `trip_created` it freezes the value at the moment of
> creation, answering "what did their trip count look like *when* they created this trip?" Filtering a
> report on the property alone would include events emitted when the user was in a different band —
> a genuine analytical trap, not a technicality. Do not extend this dual treatment to other parameters
> without the same justification.

## Migration from the Legacy Taxonomy

The 10 events currently in `AnalyticsEvent.kt`. Nothing consumes them — the only provider is the
Logcat sink — so this migration is free: no historical data, no dashboards, no funnels to break.

| Legacy event | Legacy parameters | Fate | Target |
|--------------|-------------------|------|--------|
| `trip_created` | `trip_id` | Reparameterised | `trip_created { trip_count_band }` |
| `trip_opened` | `trip_id` | Reparameterised | `trip_opened { stop_count, route_state }` |
| `stop_set` | `trip_id`, `stop_type` | **Renamed** + reparameterised | `stop_added { stop_type, stop_count }` |
| `stop_edited` | `trip_id` | Reparameterised | `stop_edited { stop_type }` |
| `stop_removed` | `trip_id` | Reparameterised | `stop_removed { stop_type, stop_count }` |
| `stop_reordered` | `trip_id` | Reparameterised | `stop_reordered { direction, stop_count }` |
| `stop_departed` | `trip_id`, `stop_id` | Reparameterised | `stop_departed { stop_position, stop_count }` |
| `route_calculated` | `trip_id` | Reparameterised | `route_calculated { stop_count, leg_count, distance_band, duration_band }` |
| `screen_viewed` | `screen_name` | Unchanged name; **trigger moved** | Fired from navigation, not ViewModel `init` |
| `operation_failed` | `operation`, `error_message` | Reparameterised | `operation_failed { operation, error_type }` |

No legacy event is dropped. Two structural changes beyond parameters:

- **`stop_set` → `stop_added`.** `set` is not a past-tense action and reads ambiguously against
  `stop_edited`. `added` states what happened.
- **`error_message` → `error_type`.** The current code passes `error.message` straight through, which
  is both unbounded cardinality and a live PII path — a Places or Directions error message can contain
  a place name the user typed.

### New events

Five events with no legacy equivalent, closing gaps where shipped features emit nothing:

| Event | Gap it closes |
|-------|---------------|
| `navigation_launched` | Story 5.1.1 shipped with no analytics at all |
| `stop_departure_undone` | `undoMarkStopDeparted()` tracks nothing on success or failure |
| `place_search_performed` | Place search failures track nothing |
| `place_suggestion_selected` | No visibility into whether autocomplete results are usable |

Plus two new `operation` values — `search_place` and `resolve_place` — so place-search and
place-detail failures are represented in `operation_failed`. These close the untracked failure paths at
`TripDetailViewModel.kt:287` and `:323`, and resolve the deferred TODO at `TripDetailViewModel.kt:148`.

## Change Log

| Date | Change |
|------|--------|
| 2026-08-17 | Initial plan. 14 events, 14 parameters, 2 user properties. Supersedes the undocumented 10-event legacy taxonomy. |
