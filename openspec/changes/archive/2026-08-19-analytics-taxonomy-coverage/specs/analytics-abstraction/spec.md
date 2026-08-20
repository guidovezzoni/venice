## MODIFIED Requirements

### Requirement: Operation identifiers are a typed, bounded enum
`AnalyticsOperation` SHALL be a top-level `enum class` in `core/analytics/`, each constant carrying an
explicit `value: String` in `snake_case`. It SHALL contain exactly the 10 operation values in use as
of this change: `create_trip`, `set_stop`, `edit_stop`, `remove_stop`, `move_stop`, `mark_departed`,
`undo_mark_departed`, `calculate_route`, `search_place`, `resolve_place`. The `trackException` member
of `AnalyticsTracking` SHALL take an `AnalyticsOperation` as its `operation` parameter.

#### Scenario: AnalyticsOperation carries exactly the ten documented values
- **WHEN** `AnalyticsOperation`'s constants are enumerated
- **THEN** they are exactly `CREATE_TRIP`, `SET_STOP`, `EDIT_STOP`, `REMOVE_STOP`, `MOVE_STOP`,
  `MARK_DEPARTED`, `UNDO_MARK_DEPARTED`, `CALCULATE_ROUTE`, `SEARCH_PLACE`, and `RESOLVE_PLACE`, each
  with a `value` matching its documented snake_case string

## REMOVED Requirements

### Requirement: The analytics taxonomy is unchanged by this structural upgrade
**Reason**: This requirement pinned the taxonomy to its pre-9.1.2 legacy shape (10 subclasses,
several carrying raw identifiers) as a guarantee that the 9.1.1 structural relocation made no
behavioural change. Story 9.1.2 intentionally and completely replaces that taxonomy with the 14 events
defined in `docs/analytics/tracking-plan.md`, so the guarantee this requirement made no longer holds
and the requirement is retired rather than modified.

**Migration**: The taxonomy's shape is now governed by the `analytics-taxonomy` capability
(`specs/analytics-taxonomy/spec.md`), which defines the 14 current events and their parameters. No
code migration is needed beyond what the `analytics-taxonomy` and `analytics-instrumentation`
capabilities already require — there is no other consumer of the retired requirement's guarantee, per
the tracking plan's migration table (the only prior provider was the debug Logcat sink).
