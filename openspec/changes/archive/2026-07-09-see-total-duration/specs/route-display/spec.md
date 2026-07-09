## MODIFIED Requirements

### Requirement: Route calculation string resources
The app SHALL define the following string resources:
- `trip_detail_calculate_route`: "Calculate route" — button label
- `trip_detail_calculating_route`: "Calculating route…" — loading description
- `trip_detail_route_error`: error message shown when route calculation fails
- `trip_detail_leg_distance_metres`: "%1$d m" — distance in metres
- `trip_detail_leg_distance_kilometres`: "%.1f km" — distance in kilometres
- `trip_detail_leg_distance_miles`: "%.1f mi" — distance in miles, for imperial-unit locales
- `trip_detail_leg_duration_minutes`: "%1$d min" — duration in minutes only
- `trip_detail_leg_duration_hours_minutes`: "%1$dh %2$dmin" — duration in hours and minutes
- `trip_detail_total_distance_label`: "Total distance" — label displayed alongside the pre-formatted total distance value on the trip detail screen
- `trip_detail_total_distance_unavailable`: "Total distance unavailable" — displayed in place of the distance label/value pair when only the total distance is absent
- `trip_detail_total_duration_label`: "Est. driving time" — label displayed alongside the pre-formatted total driving duration value on the trip detail screen; explicitly scoped as driving time only, not total trip time including stops
- `trip_detail_total_duration_unavailable`: "Est. driving time unavailable" — displayed in place of the duration label/value pair when only the total duration is absent
- `trip_detail_totals_unavailable`: "Trip totals unavailable" — combined message displayed instead of the label/value pairs when both the total distance and the total duration are absent

#### Scenario: String resources defined
- **WHEN** the app is built
- **THEN** all route calculation string resources, including `trip_detail_leg_distance_miles`, `trip_detail_total_distance_label`, `trip_detail_total_distance_unavailable`, `trip_detail_total_duration_label`, `trip_detail_total_duration_unavailable`, and `trip_detail_totals_unavailable`, resolve without errors in all supported locales (English, Italian, Spanish)
