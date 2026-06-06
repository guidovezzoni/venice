package com.guidovezzoni.venice.ui.util

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private const val MINIMUM_MILLIS = 500L

@OptIn(ExperimentalCoroutinesApi::class)
class WithMinimumDurationTest {

    @Test
    fun `GIVEN withMinimumDuration called with 500ms WHEN the block completes in under 500ms THEN the function does not return until 500ms have elapsed`() =
        runTest(StandardTestDispatcher()) {
            var completed = false

            launch {
                withMinimumDuration(MINIMUM_MILLIS) { }
                completed = true
            }

            advanceTimeBy(499L)
            assertFalse(completed)

            advanceTimeBy(2L)
            assertTrue(completed)
        }

    @Test
    fun `GIVEN withMinimumDuration called with 500ms WHEN the block takes longer than 500ms THEN the function returns immediately after the block completes`() =
        runTest(StandardTestDispatcher()) {
            var completed = false

            launch {
                withMinimumDuration(MINIMUM_MILLIS) {
                    delay(800L)
                }
                completed = true
            }

            advanceTimeBy(799L)
            assertFalse(completed)

            advanceTimeBy(2L)
            assertTrue(completed)
        }
}
