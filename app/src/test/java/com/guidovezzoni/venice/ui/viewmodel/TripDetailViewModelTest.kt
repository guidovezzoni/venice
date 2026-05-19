package com.guidovezzoni.venice.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import com.guidovezzoni.venice.domain.model.Stop
import com.guidovezzoni.venice.domain.model.StopStatus
import com.guidovezzoni.venice.domain.usecase.AddIntermediateStopUseCase
import com.guidovezzoni.venice.domain.usecase.ObserveStopsUseCase
import com.guidovezzoni.venice.domain.usecase.SetDestinationUseCase
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
    private lateinit var setDestinationUseCase: SetDestinationUseCase
    private lateinit var addIntermediateStopUseCase: AddIntermediateStopUseCase
    private lateinit var observeStopsUseCase: ObserveStopsUseCase
    private lateinit var savedStateHandle: SavedStateHandle

    @OptIn(ExperimentalCoroutinesApi::class)
    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        setStartingPointUseCase = mockk()
        setDestinationUseCase = mockk()
        addIntermediateStopUseCase = mockk()
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
        return TripDetailViewModel(setStartingPointUseCase, setDestinationUseCase, addIntermediateStopUseCase, observeStopsUseCase, savedStateHandle)
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

        val viewModel = TripDetailViewModel(setStartingPointUseCase, setDestinationUseCase, addIntermediateStopUseCase, observeStopsUseCase, savedStateHandle)
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

        val viewModel = TripDetailViewModel(setStartingPointUseCase, setDestinationUseCase, addIntermediateStopUseCase, observeStopsUseCase, savedStateHandle)

        assertEquals(expectedStop, viewModel.uiState.value.startingPoint)
    }

    @Test
    fun `GIVEN no stops in the stream WHEN ViewModel initialises THEN startingPoint is null`() = runTest {
        val viewModel = createViewModel()

        assertNull(viewModel.uiState.value.startingPoint)
    }

    @Test
    fun `GIVEN initial state WHEN OnSetDestinationClicked is dispatched THEN isSetDestinationDialogVisible becomes true`() = runTest {
        val viewModel = createViewModel()

        viewModel.onIntent(TripDetailUiIntent.OnSetDestinationClicked)

        assertTrue(viewModel.uiState.value.isSetDestinationDialogVisible)
    }

    @Test
    fun `GIVEN dialog is visible WHEN OnDismissDestinationDialog is dispatched THEN isSetDestinationDialogVisible becomes false`() = runTest {
        val viewModel = createViewModel()

        viewModel.onIntent(TripDetailUiIntent.OnSetDestinationClicked)
        viewModel.onIntent(TripDetailUiIntent.OnDismissDestinationDialog)

        assertFalse(viewModel.uiState.value.isSetDestinationDialogVisible)
    }

    @Test
    fun `GIVEN valid input WHEN OnDestinationConfirmed is dispatched and use case succeeds THEN destination is updated and dialog is dismissed`() = runTest {
        val destinationStop = Stop(
            id = "stop-dest",
            tripId = TRIP_ID,
            placeName = PLACE_NAME,
            latitude = LATITUDE,
            longitude = LONGITUDE,
            order = 1,
            status = StopStatus.PENDING,
        )
        every { observeStopsUseCase(TRIP_ID) } returns flowOf(listOf(destinationStop))
        coEvery { setDestinationUseCase(TRIP_ID, PLACE_NAME, LATITUDE, LONGITUDE) } returns Result.success(destinationStop)

        val viewModel = TripDetailViewModel(setStartingPointUseCase, setDestinationUseCase, addIntermediateStopUseCase, observeStopsUseCase, savedStateHandle)
        viewModel.onIntent(TripDetailUiIntent.OnSetDestinationClicked)
        viewModel.onIntent(TripDetailUiIntent.OnDestinationConfirmed(PLACE_NAME, LATITUDE, LONGITUDE))

        assertFalse(viewModel.uiState.value.isSetDestinationDialogVisible)
    }

    @Test
    fun `GIVEN valid input WHEN OnDestinationConfirmed is dispatched and use case fails THEN ShowError effect is emitted`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        coEvery { setDestinationUseCase(TRIP_ID, PLACE_NAME, LATITUDE, LONGITUDE) } returns Result.failure(RuntimeException("error"))

        val effects = mutableListOf<TripDetailUiEffect>()
        val collectJob = launch {
            viewModel.uiEffect.collect { effects.add(it) }
        }

        viewModel.onIntent(TripDetailUiIntent.OnDestinationConfirmed(PLACE_NAME, LATITUDE, LONGITUDE))

        assertTrue(effects.any { it is TripDetailUiEffect.ShowError })
        collectJob.cancel()
    }

    @Test
    fun `GIVEN a stop with order=1 in the stream WHEN ViewModel initialises THEN destination reflects that stop`() = runTest {
        val expectedDestination = Stop(
            id = "stop-dest",
            tripId = TRIP_ID,
            placeName = PLACE_NAME,
            latitude = LATITUDE,
            longitude = LONGITUDE,
            order = 1,
            status = StopStatus.PENDING,
        )
        every { observeStopsUseCase(TRIP_ID) } returns flowOf(listOf(expectedDestination))

        val viewModel = TripDetailViewModel(setStartingPointUseCase, setDestinationUseCase, addIntermediateStopUseCase, observeStopsUseCase, savedStateHandle)

        assertEquals(expectedDestination, viewModel.uiState.value.destination)
    }

    @Test
    fun `GIVEN only a stop with order=0 in the stream WHEN ViewModel initialises THEN destination is null`() = runTest {
        val startingPoint = Stop(
            id = "stop-start",
            tripId = TRIP_ID,
            placeName = PLACE_NAME,
            latitude = LATITUDE,
            longitude = LONGITUDE,
            order = 0,
            status = StopStatus.PENDING,
        )
        every { observeStopsUseCase(TRIP_ID) } returns flowOf(listOf(startingPoint))

        val viewModel = TripDetailViewModel(setStartingPointUseCase, setDestinationUseCase, addIntermediateStopUseCase, observeStopsUseCase, savedStateHandle)

        assertNull(viewModel.uiState.value.destination)
    }

    @Test
    fun `GIVEN stops exist WHEN observed THEN intermediateStops contains only stops with order between start and destination`() = runTest {
        val startingPoint = Stop("s0", TRIP_ID, "Start", 0.0, 0.0, 0, StopStatus.PENDING)
        val intermediate1 = Stop("s1", TRIP_ID, "Stop A", 1.0, 1.0, 1, StopStatus.PENDING)
        val intermediate2 = Stop("s2", TRIP_ID, "Stop B", 2.0, 2.0, 2, StopStatus.PENDING)
        val destination = Stop("s3", TRIP_ID, "End", 3.0, 3.0, 3, StopStatus.PENDING)
        every { observeStopsUseCase(TRIP_ID) } returns flowOf(listOf(startingPoint, intermediate1, intermediate2, destination))

        val viewModel = TripDetailViewModel(setStartingPointUseCase, setDestinationUseCase, addIntermediateStopUseCase, observeStopsUseCase, savedStateHandle)

        val expectedIntermediateStops = listOf(intermediate1, intermediate2)
        assertEquals(expectedIntermediateStops, viewModel.uiState.value.intermediateStops)
        assertEquals(startingPoint, viewModel.uiState.value.startingPoint)
        assertEquals(destination, viewModel.uiState.value.destination)
    }

    @Test
    fun `GIVEN OnAddStopClicked intent WHEN dispatched THEN isAddStopDialogVisible becomes true`() = runTest {
        val viewModel = createViewModel()

        viewModel.onIntent(TripDetailUiIntent.OnAddStopClicked)

        assertTrue(viewModel.uiState.value.isAddStopDialogVisible)
    }

    @Test
    fun `GIVEN OnDismissAddStopDialog intent WHEN dispatched THEN isAddStopDialogVisible becomes false`() = runTest {
        val viewModel = createViewModel()

        viewModel.onIntent(TripDetailUiIntent.OnAddStopClicked)
        viewModel.onIntent(TripDetailUiIntent.OnDismissAddStopDialog)

        assertFalse(viewModel.uiState.value.isAddStopDialogVisible)
    }

    @Test
    fun `GIVEN OnAddStopConfirmed intent WHEN dispatched and use case succeeds THEN dialog is dismissed`() = runTest {
        val stop = Stop("s1", TRIP_ID, PLACE_NAME, LATITUDE, LONGITUDE, 1, StopStatus.PENDING)
        coEvery { addIntermediateStopUseCase(TRIP_ID, PLACE_NAME, LATITUDE, LONGITUDE) } returns Result.success(stop)
        val viewModel = createViewModel()

        viewModel.onIntent(TripDetailUiIntent.OnAddStopClicked)
        viewModel.onIntent(TripDetailUiIntent.OnAddStopConfirmed(PLACE_NAME, LATITUDE, LONGITUDE))

        assertFalse(viewModel.uiState.value.isAddStopDialogVisible)
    }

    @Test
    fun `GIVEN OnAddStopConfirmed intent WHEN dispatched and use case fails THEN ShowError effect is emitted`() = runTest(testDispatcher) {
        coEvery { addIntermediateStopUseCase(TRIP_ID, PLACE_NAME, LATITUDE, LONGITUDE) } returns Result.failure(RuntimeException("error"))
        val viewModel = createViewModel()

        val effects = mutableListOf<TripDetailUiEffect>()
        val collectJob = launch {
            viewModel.uiEffect.collect { effects.add(it) }
        }

        viewModel.onIntent(TripDetailUiIntent.OnAddStopConfirmed(PLACE_NAME, LATITUDE, LONGITUDE))

        assertTrue(effects.any { it is TripDetailUiEffect.ShowError })
        collectJob.cancel()
    }

    @Test
    fun `GIVEN 25 stops exist WHEN observed THEN canAddMoreStops is false`() = runTest {
        val stops = (0 until 25).map { i ->
            Stop("s$i", TRIP_ID, "Stop $i", i.toDouble(), i.toDouble(), i, StopStatus.PENDING)
        }
        every { observeStopsUseCase(TRIP_ID) } returns flowOf(stops)

        val viewModel = TripDetailViewModel(setStartingPointUseCase, setDestinationUseCase, addIntermediateStopUseCase, observeStopsUseCase, savedStateHandle)

        assertFalse(viewModel.uiState.value.canAddMoreStops)
    }
}
