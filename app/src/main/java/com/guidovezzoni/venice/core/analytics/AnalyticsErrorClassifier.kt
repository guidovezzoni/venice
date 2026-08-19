package com.guidovezzoni.venice.core.analytics

import android.database.sqlite.SQLiteException
import java.io.IOException
import java.net.SocketTimeoutException

/**
 * Classifies a [Throwable] into an [AnalyticsErrorType] by inspecting only the throwable's runtime
 * type.
 *
 * Note: [AnalyticsErrorType.NOT_FOUND], [AnalyticsErrorType.PERMISSION_DENIED], and
 * [AnalyticsErrorType.QUOTA_EXCEEDED] are unreachable from this classifier until finer-grained
 * classification (HTTP status or ApiException code parsing) is implemented in a follow-up story.
 *
 * Thread-safe: this is a pure function with no shared mutable state.
 */
fun classifyAnalyticsError(throwable: Throwable): AnalyticsErrorType = when {
    throwable is SocketTimeoutException -> AnalyticsErrorType.TIMEOUT
    throwable is IOException -> AnalyticsErrorType.NETWORK
    throwable is SQLiteException -> AnalyticsErrorType.PERSISTENCE
    else -> AnalyticsErrorType.UNKNOWN
}
