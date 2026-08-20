package com.guidovezzoni.venice.core.analytics

sealed interface AnalyticsUserProperty {
    /**
     * Test-only concrete subclass, accessible to unit tests via `internal` visibility.
     * Remove when story 9.1.2 introduces real subclasses (e.g. `TripCountBand`, `DistanceUnit`).
     */
    data object ForTesting : AnalyticsUserProperty
}
