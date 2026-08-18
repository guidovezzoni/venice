package com.guidovezzoni.venice.core.analytics

sealed class AnalyticsUserProperty {
    /**
     * Test-only concrete subclass, accessible to unit tests via `internal` visibility.
     * Remove when story 9.1.2 introduces real subclasses (e.g. `TripCountBand`, `DistanceUnit`).
     */
    internal object ForTesting : AnalyticsUserProperty()
}
