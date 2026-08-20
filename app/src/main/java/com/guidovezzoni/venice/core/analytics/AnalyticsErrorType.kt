package com.guidovezzoni.venice.core.analytics

enum class AnalyticsErrorType(val value: String) {
    NETWORK("network"),
    TIMEOUT("timeout"),
    // NOT_FOUND, PERMISSION_DENIED, and QUOTA_EXCEEDED are reserved vocabulary declared for forward
    // compatibility with the tracking plan's parameter reference. They are unreachable from
    // AnalyticsErrorClassifier until finer-grained classification (HTTP status or ApiException code
    // parsing) is implemented in a follow-up story.
    NOT_FOUND("not_found"),
    PERMISSION_DENIED("permission_denied"),
    QUOTA_EXCEEDED("quota_exceeded"),
    PERSISTENCE("persistence"),
    UNKNOWN("unknown"),
}
