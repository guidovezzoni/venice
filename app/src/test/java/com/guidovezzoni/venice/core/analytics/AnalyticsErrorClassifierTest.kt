package com.guidovezzoni.venice.core.analytics

import android.database.sqlite.SQLiteException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import org.junit.Assert.assertEquals
import org.junit.Test

class AnalyticsErrorClassifierTest {

    @Test
    fun `GIVEN a SocketTimeoutException WHEN classifyAnalyticsError is called THEN returns TIMEOUT`() {
        val expected = AnalyticsErrorType.TIMEOUT
        val actual = classifyAnalyticsError(SocketTimeoutException())
        assertEquals(expected, actual)
    }

    @Test
    fun `GIVEN a plain IOException WHEN classifyAnalyticsError is called THEN returns NETWORK`() {
        val expected = AnalyticsErrorType.NETWORK
        val actual = classifyAnalyticsError(IOException())
        assertEquals(expected, actual)
    }

    @Test
    fun `GIVEN an UnknownHostException WHEN classifyAnalyticsError is called THEN returns NETWORK`() {
        val expected = AnalyticsErrorType.NETWORK
        val actual = classifyAnalyticsError(UnknownHostException())
        assertEquals(expected, actual)
    }

    @Test
    fun `GIVEN a SQLiteException WHEN classifyAnalyticsError is called THEN returns PERSISTENCE`() {
        val expected = AnalyticsErrorType.PERSISTENCE
        val actual = classifyAnalyticsError(SQLiteException())
        assertEquals(expected, actual)
    }

    @Test
    fun `GIVEN an IllegalStateException WHEN classifyAnalyticsError is called THEN returns UNKNOWN`() {
        val expected = AnalyticsErrorType.UNKNOWN
        val actual = classifyAnalyticsError(IllegalStateException())
        assertEquals(expected, actual)
    }
}
