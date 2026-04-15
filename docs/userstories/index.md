# User Stories

## Epic 1: Trip Management

The core ability to create, organise, and manage roadtrips and their stops.

### Feature 1.1: Trip CRUD

| ID | User Story | Details |
|----|------------|---------|
| 1.1.1 | As a user, I want to create a new roadtrip | [1.1.1-create-roadtrip](1.1.1-create-roadtrip.md) |
| 1.1.2 | As a user, I want to see a list of my roadtrips | [1.1.2-list-roadtrips](1.1.2-list-roadtrips.md) |

### Feature 1.2: Stop Management

| ID | User Story | Details |
|----|------------|---------|
| 1.2.1 | As a user, I want to set a starting point for my roadtrip | [1.2.1-set-starting-point](1.2.1-set-starting-point.md) |
| 1.2.2 | As a user, I want to set a destination for my roadtrip | [1.2.2-set-destination](1.2.2-set-destination.md) |
| 1.2.3 | As a user, I want to add intermediate stops to my roadtrip | [1.2.3-add-intermediate-stops](1.2.3-add-intermediate-stops.md) |
| 1.2.4 | As a user, I want to reorder stops in my roadtrip | [1.2.4-reorder-stops](1.2.4-reorder-stops.md) |
| 1.2.5 | As a user, I want to edit a stop in my roadtrip | [1.2.5-edit-stop](1.2.5-edit-stop.md) |
| 1.2.6 | As a user, I want to remove a stop from my roadtrip | [1.2.6-remove-stop](1.2.6-remove-stop.md) |

### Feature 1.3: Stop Progress

| ID | User Story | Details |
|----|------------|---------|
| 1.3.1 | As a user, I want to mark a stop as visited/departed | [1.3.1-mark-stop-visited](1.3.1-mark-stop-visited.md) |
| 1.3.2 | As a user, I want to see which stops are upcoming vs completed | [1.3.2-see-stop-progress](1.3.2-see-stop-progress.md) |

---

## Epic 2: Place Search

Resolving human-readable place names to geographic coordinates when adding or editing stops.

### Feature 2.1: Geocoding

| ID | User Story | Details |
|----|------------|---------|
| 2.1.1 | As a user, I want to search for a place by name when adding a stop | [2.1.1-search-place-by-name](2.1.1-search-place-by-name.md) |
| 2.1.2 | As a user, I want the app to resolve place names to coordinates | [2.1.2-resolve-place-to-coordinates](2.1.2-resolve-place-to-coordinates.md) |

---

## Epic 3: Route Calculation

Using Google Directions API to compute distances, durations, and route geometry between stops.

### Feature 3.1: Leg Calculation

| ID | User Story | Details |
|----|------------|---------|
| 3.1.1 | As a user, I want the app to calculate the route between consecutive stops when I finalise my plan | [3.1.1-calculate-route-between-stops](3.1.1-calculate-route-between-stops.md) |
| 3.1.2 | As a user, I want to see the distance for each leg of the trip | [3.1.2-see-leg-distance](3.1.2-see-leg-distance.md) |
| 3.1.3 | As a user, I want to see the estimated duration for each leg of the trip | [3.1.3-see-leg-duration](3.1.3-see-leg-duration.md) |

### Feature 3.2: Trip Totals

| ID | User Story | Details |
|----|------------|---------|
| 3.2.1 | As a user, I want to see the total distance of the entire roadtrip | [3.2.1-see-total-distance](3.2.1-see-total-distance.md) |
| 3.2.2 | As a user, I want to see the total estimated duration of the entire roadtrip | [3.2.2-see-total-duration](3.2.2-see-total-duration.md) |

---

## Epic 4: Trip Overview

A summary screen showing the full trip with per-leg breakdowns.

### Feature 4.1: Trip Summary View

| ID | User Story | Details |
|----|------------|---------|
| 4.1.1 | As a user, I want to see a trip overview with all stops and per-leg breakdowns | [4.1.1-trip-overview](4.1.1-trip-overview.md) |

---

## Epic 5: Navigation Handoff

Delegating turn-by-turn navigation to Google Maps via Android Intent.

### Feature 5.1: Launch Navigation

| ID | User Story | Details |
|----|------------|---------|
| 5.1.1 | As a user, I want to tap a stop and open Google Maps navigation to it | [5.1.1-navigate-to-stop](5.1.1-navigate-to-stop.md) |
| 5.1.2 | As a user, I want to tap a leg and open Google Maps navigation for that segment | [5.1.2-navigate-leg-segment](5.1.2-navigate-leg-segment.md) |

---

## Epic 6: Live Position *(future — Level 2)*

Using GPS to provide real-time distance and ETA updates while driving.

### Feature 6.1: GPS-based ETA

| ID | User Story | Details |
|----|------------|---------|
| 6.1.1 | As a user, I want the app to use my GPS to update time/distance to the next stop in real time | [6.1.1-gps-live-distance-to-next-stop](6.1.1-gps-live-distance-to-next-stop.md) |
| 6.1.2 | As a user, I want ETAs to recalculate based on my current position | [6.1.2-recalculate-eta-from-position](6.1.2-recalculate-eta-from-position.md) |

---

## Epic 7: Android Auto *(future)*

A read-only companion view of the current trip on the car display.

### Feature 7.1: Auto Trip View

| ID | User Story | Details |
|----|------------|---------|
| 7.1.1 | As a user, I want to see my current roadtrip and upcoming stops on Android Auto | [7.1.1-android-auto-trip-view](7.1.1-android-auto-trip-view.md) |
| 7.1.2 | As a user, I want to tap a stop on Android Auto to start navigation | [7.1.2-android-auto-navigate-to-stop](7.1.2-android-auto-navigate-to-stop.md) |
