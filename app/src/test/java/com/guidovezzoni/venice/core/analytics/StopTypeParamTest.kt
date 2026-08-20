package com.guidovezzoni.venice.core.analytics

import com.guidovezzoni.venice.domain.model.StopType
import org.junit.Assert.assertEquals
import org.junit.Test

class StopTypeParamTest {

    @Test
    fun `GIVEN STARTING_POINT stop type WHEN toStopTypeParam is called THEN returns STARTING_POINT with correct value`() {
        val expected = StopTypeParam.STARTING_POINT
        val actual = StopType.STARTING_POINT.toStopTypeParam()
        assertEquals(expected, actual)
    }

    @Test
    fun `GIVEN DESTINATION stop type WHEN toStopTypeParam is called THEN returns DESTINATION with correct value`() {
        val expected = StopTypeParam.DESTINATION
        val actual = StopType.DESTINATION.toStopTypeParam()
        assertEquals(expected, actual)
    }

    @Test
    fun `GIVEN INTERMEDIATE stop type WHEN toStopTypeParam is called THEN returns INTERMEDIATE with correct value`() {
        val expected = StopTypeParam.INTERMEDIATE
        val actual = StopType.INTERMEDIATE.toStopTypeParam()
        assertEquals(expected, actual)
    }

    @Test
    fun `GIVEN STARTING_POINT stop type WHEN toStopTypeParam value is read THEN returns snake_case string`() {
        val expected = "starting_point"
        val actual = StopType.STARTING_POINT.toStopTypeParam().value
        assertEquals(expected, actual)
    }

    @Test
    fun `GIVEN DESTINATION stop type WHEN toStopTypeParam value is read THEN returns snake_case string`() {
        val expected = "destination"
        val actual = StopType.DESTINATION.toStopTypeParam().value
        assertEquals(expected, actual)
    }

    @Test
    fun `GIVEN INTERMEDIATE stop type WHEN toStopTypeParam value is read THEN returns snake_case string`() {
        val expected = "intermediate"
        val actual = StopType.INTERMEDIATE.toStopTypeParam().value
        assertEquals(expected, actual)
    }

    @Test
    fun `GIVEN all StopType values WHEN each is mapped THEN every domain value has a corresponding param`() {
        val mappedCount = StopType.entries.map { it.toStopTypeParam() }.toSet().size
        assertEquals(StopType.entries.size, mappedCount)
    }
}
