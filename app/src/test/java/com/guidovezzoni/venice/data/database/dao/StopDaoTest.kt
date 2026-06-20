package com.guidovezzoni.venice.data.database.dao

import com.guidovezzoni.venice.data.database.entity.StopEntity
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test

private const val TRIP_ID = "trip-1"
private const val STOP_ORDER_0 = 0
private const val STOP_ORDER_1 = 1
private const val STOP_ORDER_2 = 2
private const val STOP_ORDER_3 = 3
private const val LATITUDE = 43.7696
private const val LONGITUDE = 11.2558

class StopDaoTest {

    private lateinit var dao: StopDao

    @Before
    fun setUp() {
        dao = mockk()
        coEvery { dao.deleteAndReorder(any(), any()) } coAnswers { callOriginal() }
        coEvery { dao.shiftAndInsertStop(any(), any()) } coAnswers { callOriginal() }
        coEvery { dao.swapStopOrders(any(), any(), any()) } coAnswers { callOriginal() }
    }

    @Test
    fun `GIVEN a stop at order 2 WHEN deleteAndReorder is called THEN the stop is deleted and remaining orders are decremented above deleted order`() = runTest {
        val stopToDelete = createStopEntity("stop-2", STOP_ORDER_2)
        coEvery { dao.getStopById("stop-2") } returns stopToDelete
        coEvery { dao.deleteById("stop-2") } returns Unit
        coEvery { dao.decrementOrderAbove(TRIP_ID, STOP_ORDER_2) } returns Unit

        dao.deleteAndReorder(TRIP_ID, "stop-2")

        coVerifyOrder {
            dao.getStopById("stop-2")
            dao.deleteById("stop-2")
            dao.decrementOrderAbove(TRIP_ID, STOP_ORDER_2)
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

    @Test
    fun `GIVEN trip with destination at order 2 WHEN shiftAndInsertStop is called THEN orders are shifted and stop is inserted before destination`() = runTest {
        val destination = createStopEntity("stop-dest", STOP_ORDER_2)
        val newStop = createStopEntity("stop-new", STOP_ORDER_0)

        coEvery { dao.getDestination(TRIP_ID) } returns destination
        coEvery { dao.incrementOrderFrom(TRIP_ID, STOP_ORDER_2) } returns Unit
        val insertedStopSlot = slot<StopEntity>()
        coEvery { dao.insert(capture(insertedStopSlot)) } returns Unit

        val result = dao.shiftAndInsertStop(TRIP_ID, newStop)

        assertEquals(STOP_ORDER_2, result)
        coVerifyOrder {
            dao.getDestination(TRIP_ID)
            dao.incrementOrderFrom(TRIP_ID, STOP_ORDER_2)
            dao.insert(any())
        }
        assertEquals(STOP_ORDER_2, insertedStopSlot.captured.order)
    }

    @Test
    fun `GIVEN trip with no destination WHEN shiftAndInsertStop is called THEN stop is inserted at order 1`() = runTest {
        val newStop = createStopEntity("stop-new", STOP_ORDER_0)

        coEvery { dao.getDestination(TRIP_ID) } returns null
        val insertedStopSlot = slot<StopEntity>()
        coEvery { dao.insert(capture(insertedStopSlot)) } returns Unit

        val result = dao.shiftAndInsertStop(TRIP_ID, newStop)

        assertEquals(STOP_ORDER_1, result)
        coVerify(exactly = 0) { dao.incrementOrderFrom(any(), any()) }
        assertEquals(STOP_ORDER_1, insertedStopSlot.captured.order)
    }

    @Test
    fun `GIVEN two stops at orders 1 and 3 WHEN swapStopOrders is called THEN their orders are swapped`() = runTest {
        val fromStop = createStopEntity("stop-a", STOP_ORDER_1)
        val toStop = createStopEntity("stop-b", STOP_ORDER_3)

        coEvery { dao.getStopByTripIdAndOrder(TRIP_ID, STOP_ORDER_1) } returns fromStop
        coEvery { dao.getStopByTripIdAndOrder(TRIP_ID, STOP_ORDER_3) } returns toStop
        coEvery { dao.updateStopOrder(any(), any()) } returns Unit

        dao.swapStopOrders(TRIP_ID, STOP_ORDER_1, STOP_ORDER_3)

        coVerify { dao.updateStopOrder("stop-a", STOP_ORDER_3) }
        coVerify { dao.updateStopOrder("stop-b", STOP_ORDER_1) }
    }

    @Test
    fun `GIVEN no stop at fromOrder WHEN swapStopOrders is called THEN IllegalStateException is thrown`() = runTest {
        coEvery { dao.getStopByTripIdAndOrder(TRIP_ID, STOP_ORDER_1) } returns null

        assertThrows(IllegalStateException::class.java) {
            kotlinx.coroutines.test.runTest {
                dao.swapStopOrders(TRIP_ID, STOP_ORDER_1, STOP_ORDER_3)
            }
        }
    }

    @Test
    fun `GIVEN no stop at toOrder WHEN swapStopOrders is called THEN IllegalStateException is thrown`() = runTest {
        val fromStop = createStopEntity("stop-a", STOP_ORDER_1)
        coEvery { dao.getStopByTripIdAndOrder(TRIP_ID, STOP_ORDER_1) } returns fromStop
        coEvery { dao.getStopByTripIdAndOrder(TRIP_ID, STOP_ORDER_3) } returns null

        assertThrows(IllegalStateException::class.java) {
            kotlinx.coroutines.test.runTest {
                dao.swapStopOrders(TRIP_ID, STOP_ORDER_1, STOP_ORDER_3)
            }
        }
    }

    private fun createStopEntity(id: String, order: Int): StopEntity = StopEntity(
        id = id,
        tripId = TRIP_ID,
        placeName = "Place",
        latitude = LATITUDE,
        longitude = LONGITUDE,
        order = order,
        status = "PENDING",
    )
}
