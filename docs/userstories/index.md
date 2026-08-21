# User Stories

## Epic 1: Trip Management

The core ability to create, organise, and manage roadtrips and their stops.

### Feature 1.1: Trip CRUD

| ID | User Story | Details |
|----|------------|---------|
| 1.1.1 | As a user, I want to create a new roadtrip | [1.1.1-create-roadtrip](1.1.1-create-roadtrip-DONE.md) |
| 1.1.2 | As a user, I want to see a list of my roadtrips | [1.1.2-list-roadtrips](1.1.2-list-roadtrips-DONE.md) |

### Feature 1.2: Stop Management

| ID | User Story | Details |
|----|------------|---------|
| 1.2.1 | As a user, I want to set a starting point for my roadtrip | [1.2.1-set-starting-point](1.2.1-set-starting-point-DONE.md) |
| 1.2.2 | As a user, I want to set a destination for my roadtrip | [1.2.2-set-destination](1.2.2-set-destination-DONE.md) |
| 1.2.3 | As a user, I want to add intermediate stops to my roadtrip | [1.2.3-add-intermediate-stops](1.2.3-add-intermediate-stops-DONE.md) |
| 1.2.3.1 | As a developer, I want the stop use cases consolidated into a single SetStopUseCase | [1.2.3.1-consolidate-stop-usecases](1.2.3.1-consolidate-stop-usecases-DONE.md) |
| 1.2.4 | As a user, I want to reorder stops in my roadtrip | [1.2.4-reorder-stops](1.2.4-reorder-stops-DONE.md) |
| 1.2.5 | As a user, I want to edit a stop in my roadtrip | [1.2.5-edit-stop](1.2.5-edit-stop-DONE.md) |
| 1.2.6 | As a user, I want to remove a stop from my roadtrip | [1.2.6-remove-stop](1.2.6-remove-stop-DONE.md) |

### Feature 1.3: Stop Progress

| ID | User Story | Details |
|----|------------|---------|
| 1.3.1 | As a user, I want to mark a stop as visited/departed | [1.3.1-mark-stop-visited](1.3.1-mark-stop-visited-DONE.md) |
| 1.3.2 | As a user, I want to see which stops are upcoming vs completed | [1.3.2-see-stop-progress](1.3.2-see-stop-progress-DONE.md) |

### Feature 1.4: UI Feedback

| ID | User Story | Details |
|----|------------|---------|
| 1.4.1 | As a user, I want dialogs to show that a save is in progress | [1.4.1-dialog-loading-feedback](1.4.1-dialog-loading-feedback-DONE.md) |

---

## Epic 2: Place Search

Resolving human-readable place names to geographic coordinates when adding or editing stops.

### Feature 2.1: Geocoding

| ID | User Story | Details |
|----|------------|---------|
| 2.1.1 | As a user, I want to search for a place by name when adding a stop | [2.1.1-search-place-by-name](2.1.1-search-place-by-name-DONE.md) |
| 2.1.2 | As a user, I want the app to resolve place names to coordinates | [2.1.2-resolve-place-to-coordinates](2.1.2-resolve-place-to-coordinates-DONE.md) |

---

## Epic 3: Route Calculation

Using Google Directions API to compute distances, durations, and route geometry between stops.

### Feature 3.1: Leg Calculation

| ID | User Story | Details |
|----|------------|---------|
| 3.1.1 | As a user, I want the app to calculate the route between consecutive stops when I finalise my plan | [3.1.1-calculate-route-between-stops](3.1.1-calculate-route-between-stops-DONE.md) |
| 3.1.2 | As a user, I want to see the distance for each leg of the trip | [3.1.2-see-leg-distance](3.1.2-see-leg-distance-DONE.md) |
| 3.1.3 | As a user, I want to see the estimated duration for each leg of the trip | [3.1.3-see-leg-duration](3.1.3-see-leg-duration-DONE.md) |

### Feature 3.2: Trip Totals

| ID | User Story | Details |
|----|------------|---------|
| 3.2.1 | As a user, I want to see the total distance of the entire roadtrip | [3.2.1-see-total-distance](3.2.1-see-total-distance-DONE.md) |
| 3.2.2 | As a user, I want to see the total estimated duration of the entire roadtrip | [3.2.2-see-total-duration](3.2.2-see-total-duration-DONE.md) |

---

## Epic 4: Trip Overview

A summary screen showing the full trip with per-leg breakdowns.

### Feature 4.1: Trip Summary View

| ID | User Story | Details |
|----|------------|---------|
| 4.1.1 | As a user, I want to see a trip overview with all stops and per-leg breakdowns | [4.1.1-trip-overview](4.1.1-trip-overview-DONE.md) ✅ |

---

## Epic 5: Navigation Handoff

Delegating turn-by-turn navigation to Google Maps via Android Intent.

### Feature 5.1: Launch Navigation

| ID | User Story | Details |
|----|------------|---------|
| 5.1.1 | As a user, I want to tap a stop and open Google Maps navigation to it | [5.1.1-navigate-to-stop](5.1.1-navigate-to-stop-DONE.md) |
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

---

## Epic 8: Cloud Sync & Multi-User

Cloud storage via Supabase, user authentication, and trip sharing between users.

### Feature 8.1: Backend Foundation & Auth

| ID | User Story | Details |
|----|------------|---------|
| 8.1.1 | As a developer, I want to analyse offline+online vs online-only architectures | [8.1.1-spike-offline-vs-online](8.1.1-spike-offline-vs-online.md) |
| 8.1.2 | As a developer, I want the Supabase project configured with remote schema and Android SDK | [8.1.2-supabase-project-setup](8.1.2-supabase-project-setup.md) |
| 8.1.3 | As a user, I want to create an account | [8.1.3-sign-up](8.1.3-sign-up.md) |
| 8.1.4 | As a user, I want to log in to my account | [8.1.4-log-in](8.1.4-log-in.md) |
| 8.1.5 | As a user, I want to log out of my account | [8.1.5-log-out](8.1.5-log-out.md) |
| 8.1.6 | As a user, I want the app to remember my login | [8.1.6-auth-state-management](8.1.6-auth-state-management.md) |

### Feature 8.2: Trip Sync

| ID | User Story | Details |
|----|------------|---------|
| 8.2.1 | As a user, I want my trips saved to the cloud | [8.2.1-sync-trips-to-cloud](8.2.1-sync-trips-to-cloud.md) |
| 8.2.2 | As a user, I want my stops saved to the cloud | [8.2.2-sync-stops-to-cloud](8.2.2-sync-stops-to-cloud.md) |
| 8.2.3 | As a user, I want to see my trips when I log in on a new device | [8.2.3-pull-trips-on-login](8.2.3-pull-trips-on-login.md) |
| 8.2.4 | As a user, I want to view and edit trips without an internet connection | [8.2.4-offline-support](8.2.4-offline-support.md) |

### Feature 8.3: Trip Sharing *(future)*

| ID | User Story | Details |
|----|------------|---------|
| 8.3.1 | As a developer, I want to design the trip sharing model | [8.3.1-spike-sharing-model](8.3.1-spike-sharing-model.md) |
| 8.3.2 | As a user, I want to share a trip with another person | [8.3.2-share-trip-with-user](8.3.2-share-trip-with-user.md) |
| 8.3.3 | As a user, I want to see trips that others have shared with me | [8.3.3-view-shared-trips](8.3.3-view-shared-trips.md) |

---

## Epic 9: Analytics

Understanding how the app is used: a consistent event taxonomy, real analytics and crash-reporting
destinations, and user control over collection.

Conventions live in [guidelines-analytics.md](../guidelines/guidelines-analytics.md); the event
dictionary is the [tracking plan](../analytics/tracking-plan.md).

### Feature 9.1: Analytics Foundation

| ID | User Story | Details |
|----|------------|---------|
| 9.1.1 | As a developer, I want the analytics abstraction clearly named, documented, and safe by construction | [9.1.1-analytics-abstraction-upgrade](9.1.1-analytics-abstraction-upgrade-DONE.md) |
| 9.1.2 | As a product owner, I want a consistent, analysable event taxonomy covering every user action | [9.1.2-analytics-taxonomy-and-coverage](9.1.2-analytics-taxonomy-and-coverage-DONE.md) |
| 9.1.3 | As a product owner, I want screen view events to reflect what the user actually saw | [9.1.3-screen-view-tracking](9.1.3-screen-view-tracking.md) |
| 9.1.4 | As a developer, I want Hilt DI working in instrumented tests | [9.1.4-hilt-instrumented-test-infrastructure](9.1.4-hilt-instrumented-test-infrastructure.md) |

### Feature 9.2: Analytics Destinations

| ID | User Story | Details |
|----|------------|---------|
| 9.2.1 | As a product owner, I want events delivered to Firebase Analytics | [9.2.1-firebase-analytics-provider](9.2.1-firebase-analytics-provider-WIP.md) |
| 9.2.2 | As a developer, I want handled failures and crashes reported to Crashlytics | [9.2.2-crashlytics-provider](9.2.2-crashlytics-provider.md) |

### Feature 9.3: Analytics Privacy

| ID | User Story | Details |
|----|------------|---------|
| 9.3.1 | As a user, I want to decide whether the app collects analytics about my usage | [9.3.1-analytics-consent](9.3.1-analytics-consent.md) |
