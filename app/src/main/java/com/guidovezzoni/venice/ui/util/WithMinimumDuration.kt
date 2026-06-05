package com.guidovezzoni.venice.ui.util

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay

private const val MINIMUM_LOADING_DURATION_MILLIS = 500L

suspend fun <T> withMinimumDuration(
    minimumMillis: Long = MINIMUM_LOADING_DURATION_MILLIS,
    block: suspend () -> T,
): T = coroutineScope {
    val deferredDelay = async { delay(minimumMillis) }
    val result = block()
    deferredDelay.await()
    result
}
