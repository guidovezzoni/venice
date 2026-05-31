package com.guidovezzoni.venice.data.database.dao

import com.guidovezzoni.venice.data.database.entity.StopEntity
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test

private const val TRIP_ID = "trip-1"

class StopDaoTest {

    private lateinit var dao: StopDao

    @Before
    fun setUp() {
        dao = mockk()
        coEvery { dao.deleteAndReorder(any(), any()) } coAnswers { callOriginal() }
    }

    @Test
    fun `GIVEN a stop at order 2 in a trip with stops 0,1,2,3 WHEN deleteAndReorder is called THEN the stop is deleted and remaining orders are decremented above deleted order`() = runTest {
        val stopToDelete = StopEntity(
            id = "stop-2",
            tripId = TRIP_ID,
            placeName = "Florence",
            latitude = 43.7696,
            longitude = 11.2558,
            order = 2,
            status = "PENDING",
        )
        coEvery { dao.getStopById("stop-2") } returns stopToDelete
        coEvery { dao.deleteById("stop-2") } returns Unit
        coEvery { dao.decrementOrderAbove(TRIP_ID, 2) } returns Unit

        dao.deleteAndReorder(TRIP_ID, "stop-2")

        coVerifyOrder {
            dao.getStopById("stop-2")
            dao.deleteById("stop-2")
            dao.decrementOrderAbove(TRIP_ID, 2)
        }
    }

    @Test
    fun `GIVEN stop does not exist WHEN deleteAndReorder is called THEN IllegalStateException is thrown`() = runTest {
        coEvery { dao.getStopById("nonexistent") } returns null

        assertThrows(IllegalStateException::class.java) {
            kotlinx.coroutines.test.runTest {
                dao.deleteAndReorder(TRIP_ID, "nonexistent")
            }
        }
    }
}
