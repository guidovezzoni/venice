package com.guidovezzoni.venice.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import com.guidovezzoni.venice.domain.model.Stop
import com.guidovezzoni.venice.domain.model.StopStatus
import com.guidovezzoni.venice.domain.usecase.ObserveStopsUseCase
import com.guidovezzoni.venice.domain.usecase.SetStartingPointUseCase
import com.guidovezzoni.venice.ui.effect.TripDetailUiEffect
import com.guidovezzoni.venice.ui.intent.TripDetailUiIntent
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

private const val TRIP_ID = "trip-1"
private const val PLACE_NAME = "Rome"
private const val LATITUDE = 41.9028
private const val LONGITUDE = 12.4964

class TripDetailViewModelTest {

    @OptIn(ExperimentalCoroutinesApi::class)
    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var setStartingPointUseCase: SetStartingPointUseCase
    private lateinit var observeStopsUseCase: ObserveStopsUseCase
    private lateinit var savedStateHandle: SavedStateHandle

    @OptIn(ExperimentalCoroutinesApi::class)
    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        setStartingPointUseCase = mockk()
        observeStopsUseCase = mockk()
        savedStateHandle = SavedStateHandle(mapOf("tripId" to TRIP_ID))
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): TripDetailViewModel {
        every { observeStopsUseCase(TRIP_ID) } returns flowOf(emptyList())
        return TripDetailViewModel(setStartingPointUseCase, observeStopsUseCase, savedStateHandle)
    }

    @Test
    fun `GIVEN initial state WHEN OnSetStartingPointClicked is dispatched THEN isSetStartingPointDialogVisible becomes true`() = runTest {
        val viewModel = createViewModel()

        viewModel.onIntent(TripDetailUiIntent.OnSetStartingPointClicked)

        assertTrue(viewModel.uiState.value.isSetStartingPointDialogVisible)
    }

    @Test
    fun `GIVEN dialog is visible WHEN OnDismissStartingPointDialog is dispatched THEN isSetStartingPointDialogVisible becomes false`() = runTest {
        val viewModel = createViewModel()

        viewModel.onIntent(TripDetailUiIntent.OnSetStartingPointClicked)
        viewModel.onIntent(TripDetailUiIntent.OnDismissStartingPointDialog)

        assertFalse(viewModel.uiState.value.isSetStartingPointDialogVisible)
    }

    @Test
    fun `GIVEN valid input WHEN OnStartingPointConfirmed is dispatched and use case succeeds THEN startingPoint is updated and dialog is dismissed`() = runTest {
        val stop = Stop(
            id = "stop-1",
            tripId = TRIP_ID,
            placeName = PLACE_NAME,
            latitude = LATITUDE,
            longitude = LONGITUDE,
            order = 0,
            status = StopStatus.PENDING,
        )
        every { observeStopsUseCase(TRIP_ID) } returns flowOf(listOf(stop))
        coEvery { setStartingPointUseCase(TRIP_ID, PLACE_NAME, LATITUDE, LONGITUDE) } returns Result.success(stop)

        val viewModel = TripDetailViewModel(setStartingPointUseCase, observeStopsUseCase, savedStateHandle)
        viewModel.onIntent(TripDetailUiIntent.OnSetStartingPointClicked)
        viewModel.onIntent(TripDetailUiIntent.OnStartingPointConfirmed(PLACE_NAME, LATITUDE, LONGITUDE))

        assertFalse(viewModel.uiState.value.isSetStartingPointDialogVisible)
    }

    @Test
    fun `GIVEN valid input WHEN OnStartingPointConfirmed is dispatched and use case fails THEN ShowError effect is emitted`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        coEvery { setStartingPointUseCase(TRIP_ID, PLACE_NAME, LATITUDE, LONGITUDE) } returns Result.failure(RuntimeException("error"))

        val effects = mutableListOf<TripDetailUiEffect>()
        val collectJob = launch {
            viewModel.uiEffect.collect { effects.add(it) }
        }

        viewModel.onIntent(TripDetailUiIntent.OnStartingPointConfirmed(PLACE_NAME, LATITUDE, LONGITUDE))

        assertTrue(effects.any { it is TripDetailUiEffect.ShowError })
        collectJob.cancel()
    }

    @Test
    fun `GIVEN a stop with order=0 in the stream WHEN ViewModel initialises THEN startingPoint reflects that stop`() = runTest {
        val expectedStop = Stop(
            id = "stop-1",
            tripId = TRIP_ID,
            placeName = PLACE_NAME,
            latitude = LATITUDE,
            longitude = LONGITUDE,
            order = 0,
            status = StopStatus.PENDING,
        )
        every { observeStopsUseCase(TRIP_ID) } returns flowOf(listOf(expectedStop))

        val viewModel = TripDetailViewModel(setStartingPointUseCase, observeStopsUseCase, savedStateHandle)

        assertEquals(expectedStop, viewModel.uiState.value.startingPoint)
    }

    @Test
    fun `GIVEN no stops in the stream WHEN ViewModel initialises THEN startingPoint is null`() = runTest {
        val viewModel = createViewModel()

        assertNull(viewModel.uiState.value.startingPoint)
    }
}
