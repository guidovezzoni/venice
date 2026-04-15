# 5.1.2 — Open Google Maps Navigation for a Leg Segment

**Epic:** Navigation Handoff
**Feature:** Launch Navigation

## User Story

As a user, I want to tap a leg and open Google Maps navigation for that segment so that I can navigate specifically from one stop to the next.

## Description

Each leg in the trip overview has a "Navigate" action. Tapping it opens Google Maps with the leg's origin as the starting point and the leg's destination as the endpoint. This is useful when the user wants to navigate a specific segment rather than just "to a stop."

## Acceptance Criteria

- Each leg between two stops has a "Navigate" action.
- Tapping it opens Google Maps with both origin and destination for the leg.
- If Google Maps is not installed, the app falls back to a generic geo intent.

## Technical Notes

- **Implementation:** Use a Google Maps directions intent:
  ```
  https://www.google.com/maps/dir/?api=1&origin={fromLat},{fromLng}&destination={toLat},{toLng}&travelmode=driving
  ```
  This opens Google Maps showing the route for the specific leg.
- Alternatively, the simpler `google.navigation:q={toLat},{toLng}` intent (same as 5.1.1) navigates from current GPS position to the destination, which may be more practical in a driving scenario. Consider offering both or defaulting to the simpler one.
