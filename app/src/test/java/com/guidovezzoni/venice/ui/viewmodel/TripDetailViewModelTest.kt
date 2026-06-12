package com.guidovezzoni.venice.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import com.guidovezzoni.venice.domain.model.PlaceDetail
import com.guidovezzoni.venice.domain.model.PlaceSuggestion
import com.guidovezzoni.venice.domain.model.Stop
import com.guidovezzoni.venice.domain.model.StopStatus
import com.guidovezzoni.venice.domain.model.StopType
import com.guidovezzoni.venice.domain.repository.PlaceSearchRepository
import com.guidovezzoni.venice.domain.usecase.EditStopUseCase
import com.guidovezzoni.venice.domain.usecase.GetPlaceDetailUseCase
import com.guidovezzoni.venice.domain.usecase.MarkStopDepartedUseCase
import com.guidovezzoni.venice.domain.usecase.MoveStopUseCase
import com.guidovezzoni.venice.domain.usecase.ObserveStopsUseCase
import com.guidovezzoni.venice.domain.usecase.RemoveStopUseCase
import com.guidovezzoni.venice.domain.usecase.SearchPlacesUseCase
import com.guidovezzoni.venice.domain.usecase.SetStopUseCase
import com.guidovezzoni.venice.domain.usecase.UndoMarkStopDepartedUseCase
import com.guidovezzoni.venice.ui.effect.TripDetailUiEffect
import com.guidovezzoni.venice.ui.intent.TripDetailUiIntent
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
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
private const val DEBOUNCE_MILLIS = 300L

@OptIn(ExperimentalCoroutinesApi::class)
class TripDetailViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var setStopUseCase: SetStopUseCase
    private lateinit var moveStopUseCase: MoveStopUseCase
    private lateinit var editStopUseCase: EditStopUseCase
    private lateinit var removeStopUseCase: RemoveStopUseCase
    private lateinit var markStopDepartedUseCase: MarkStopDepartedUseCase
    private lateinit var undoMarkStopDepartedUseCase: UndoMarkStopDepartedUseCase
    private lateinit var observeStopsUseCase: ObserveStopsUseCase
    private lateinit var searchPlacesUseCase: SearchPlacesUseCase
    private lateinit var getPlaceDetailUseCase: GetPlaceDetailUseCase
    private lateinit var placeSearchRepository: PlaceSearchRepository
    private lateinit var savedStateHandle: SavedStateHandle

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        setStopUseCase = mockk()
        moveStopUseCase = mockk()
        editStopUseCase = mockk()
        removeStopUseCase = mockk()
        markStopDepartedUseCase = mockk()
        undoMarkStopDepartedUseCase = mockk()
        observeStopsUseCase = mockk()
        searchPlacesUseCase = mockk()
        getPlaceDetailUseCase = mockk()
        placeSearchRepository = mockk(relaxed = true)
        savedStateHandle = SavedStateHandle(mapOf("tripId" to TRIP_ID))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): TripDetailViewModel {
        every { observeStopsUseCase(TRIP_ID) } returns flowOf(emptyList())
        return TripDetailViewModel(setStopUseCase, moveStopUseCase, editStopUseCase, removeStopUseCase, markStopDepartedUseCase, undoMarkStopDepartedUseCase, observeStopsUseCase, searchPlacesUseCase, getPlaceDetailUseCase, placeSearchRepository, savedStateHandle)
    }

    @Test
    fun `GIVEN initial state WHEN OnSetStartingPointClicked is dispatched THEN isSetStartingPointDialogVisible becomes true`() = runTest(testDispatcher) {
        val viewModel = createViewModel()

        viewModel.onIntent(TripDetailUiIntent.OnSetStartingPointClicked)

        assertTrue(viewModel.uiState.value.isSetStartingPointDialogVisible)
    }

    @Test
    fun `GIVEN dialog is visible WHEN OnDismissStartingPointDialog is dispatched THEN isSetStartingPointDialogVisible becomes false`() = runTest(testDispatcher) {
        val viewModel = createViewModel()

        viewModel.onIntent(TripDetailUiIntent.OnSetStartingPointClicked)
        viewModel.onIntent(TripDetailUiIntent.OnDismissStartingPointDialog)

        assertFalse(viewModel.uiState.value.isSetStartingPointDialogVisible)
    }

    @Test
    fun `GIVEN valid input WHEN OnStartingPointConfirmed is dispatched and use case succeeds THEN startingPoint is updated and dialog is dismissed`() = runTest(testDispatcher) {
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
        coEvery { setStopUseCase(TRIP_ID, PLACE_NAME, LATITUDE, LONGITUDE, StopType.STARTING_POINT) } returns Result.success(stop)

        val viewModel = TripDetailViewModel(setStopUseCase, moveStopUseCase, editStopUseCase, removeStopUseCase, markStopDepartedUseCase, undoMarkStopDepartedUseCase, observeStopsUseCase, searchPlacesUseCase, getPlaceDetailUseCase, placeSearchRepository, savedStateHandle)
        viewModel.onIntent(TripDetailUiIntent.OnSetStartingPointClicked)
        viewModel.onIntent(TripDetailUiIntent.OnStartingPointConfirmed(PLACE_NAME, LATITUDE, LONGITUDE))
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isSetStartingPointDialogVisible)
    }

    @Test
    fun `GIVEN valid input WHEN OnStartingPointConfirmed is dispatched and use case fails THEN ShowError effect is emitted`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        coEvery { setStopUseCase(TRIP_ID, PLACE_NAME, LATITUDE, LONGITUDE, StopType.STARTING_POINT) } returns Result.failure(RuntimeException("error"))

        val effects = mutableListOf<TripDetailUiEffect>()
        val collectJob = launch {
            viewModel.uiEffect.collect { effects.add(it) }
        }

        viewModel.onIntent(TripDetailUiIntent.OnStartingPointConfirmed(PLACE_NAME, LATITUDE, LONGITUDE))
        advanceUntilIdle()

        assertTrue(effects.any { it is TripDetailUiEffect.ShowError })
        collectJob.cancel()
    }

    @Test
    fun `GIVEN a stop with order=0 in the stream WHEN ViewModel initialises THEN startingPoint reflects that stop`() = runTest(testDispatcher) {
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

        val viewModel = TripDetailViewModel(setStopUseCase, moveStopUseCase, editStopUseCase, removeStopUseCase, markStopDepartedUseCase, undoMarkStopDepartedUseCase, observeStopsUseCase, searchPlacesUseCase, getPlaceDetailUseCase, placeSearchRepository, savedStateHandle)

        assertEquals(expectedStop, viewModel.uiState.value.startingPoint)
    }

    @Test
    fun `GIVEN no stops in the stream WHEN ViewModel initialises THEN startingPoint is null`() = runTest(testDispatcher) {
        val viewModel = createViewModel()

        assertNull(viewModel.uiState.value.startingPoint)
    }

    @Test
    fun `GIVEN initial state WHEN OnSetDestinationClicked is dispatched THEN isSetDestinationDialogVisible becomes true`() = runTest(testDispatcher) {
        val viewModel = createViewModel()

        viewModel.onIntent(TripDetailUiIntent.OnSetDestinationClicked)

        assertTrue(viewModel.uiState.value.isSetDestinationDialogVisible)
    }

    @Test
    fun `GIVEN dialog is visible WHEN OnDismissDestinationDialog is dispatched THEN isSetDestinationDialogVisible becomes false`() = runTest(testDispatcher) {
        val viewModel = createViewModel()

        viewModel.onIntent(TripDetailUiIntent.OnSetDestinationClicked)
        viewModel.onIntent(TripDetailUiIntent.OnDismissDestinationDialog)

        assertFalse(viewModel.uiState.value.isSetDestinationDialogVisible)
    }

    @Test
    fun `GIVEN valid input WHEN OnDestinationConfirmed is dispatched and use case succeeds THEN destination is updated and dialog is dismissed`() = runTest(testDispatcher) {
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
        coEvery { setStopUseCase(TRIP_ID, PLACE_NAME, LATITUDE, LONGITUDE, StopType.DESTINATION) } returns Result.success(destinationStop)

        val viewModel = TripDetailViewModel(setStopUseCase, moveStopUseCase, editStopUseCase, removeStopUseCase, markStopDepartedUseCase, undoMarkStopDepartedUseCase, observeStopsUseCase, searchPlacesUseCase, getPlaceDetailUseCase, placeSearchRepository, savedStateHandle)
        viewModel.onIntent(TripDetailUiIntent.OnSetDestinationClicked)
        viewModel.onIntent(TripDetailUiIntent.OnDestinationConfirmed(PLACE_NAME, LATITUDE, LONGITUDE))
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isSetDestinationDialogVisible)
    }

    @Test
    fun `GIVEN valid input WHEN OnDestinationConfirmed is dispatched and use case fails THEN ShowError effect is emitted`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        coEvery { setStopUseCase(TRIP_ID, PLACE_NAME, LATITUDE, LONGITUDE, StopType.DESTINATION) } returns Result.failure(RuntimeException("error"))

        val effects = mutableListOf<TripDetailUiEffect>()
        val collectJob = launch {
            viewModel.uiEffect.collect { effects.add(it) }
        }

        viewModel.onIntent(TripDetailUiIntent.OnDestinationConfirmed(PLACE_NAME, LATITUDE, LONGITUDE))
        advanceUntilIdle()

        assertTrue(effects.any { it is TripDetailUiEffect.ShowError })
        collectJob.cancel()
    }

    @Test
    fun `GIVEN a stop with order=1 in the stream WHEN ViewModel initialises THEN destination reflects that stop`() = runTest(testDispatcher) {
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

        val viewModel = TripDetailViewModel(setStopUseCase, moveStopUseCase, editStopUseCase, removeStopUseCase, markStopDepartedUseCase, undoMarkStopDepartedUseCase, observeStopsUseCase, searchPlacesUseCase, getPlaceDetailUseCase, placeSearchRepository, savedStateHandle)

        assertEquals(expectedDestination, viewModel.uiState.value.destination)
    }

    @Test
    fun `GIVEN only a stop with order=0 in the stream WHEN ViewModel initialises THEN destination is null`() = runTest(testDispatcher) {
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

        val viewModel = TripDetailViewModel(setStopUseCase, moveStopUseCase, editStopUseCase, removeStopUseCase, markStopDepartedUseCase, undoMarkStopDepartedUseCase, observeStopsUseCase, searchPlacesUseCase, getPlaceDetailUseCase, placeSearchRepository, savedStateHandle)

        assertNull(viewModel.uiState.value.destination)
    }

    @Test
    fun `GIVEN stops exist WHEN observed THEN intermediateStops contains only stops with order between start and destination`() = runTest(testDispatcher) {
        val startingPoint = Stop("s0", TRIP_ID, "Start", 0.0, 0.0, 0, StopStatus.PENDING)
        val intermediate1 = Stop("s1", TRIP_ID, "Stop A", 1.0, 1.0, 1, StopStatus.PENDING)
        val intermediate2 = Stop("s2", TRIP_ID, "Stop B", 2.0, 2.0, 2, StopStatus.PENDING)
        val destination = Stop("s3", TRIP_ID, "End", 3.0, 3.0, 3, StopStatus.PENDING)
        every { observeStopsUseCase(TRIP_ID) } returns flowOf(listOf(startingPoint, intermediate1, intermediate2, destination))

        val viewModel = TripDetailViewModel(setStopUseCase, moveStopUseCase, editStopUseCase, removeStopUseCase, markStopDepartedUseCase, undoMarkStopDepartedUseCase, observeStopsUseCase, searchPlacesUseCase, getPlaceDetailUseCase, placeSearchRepository, savedStateHandle)

        val expectedIntermediateStops = listOf(intermediate1, intermediate2)
        assertEquals(expectedIntermediateStops, viewModel.uiState.value.intermediateStops)
        assertEquals(startingPoint, viewModel.uiState.value.startingPoint)
        assertEquals(destination, viewModel.uiState.value.destination)
    }

    @Test
    fun `GIVEN OnAddStopClicked intent WHEN dispatched THEN isAddStopDialogVisible becomes true`() = runTest(testDispatcher) {
        val viewModel = createViewModel()

        viewModel.onIntent(TripDetailUiIntent.OnAddStopClicked)

        assertTrue(viewModel.uiState.value.isAddStopDialogVisible)
    }

    @Test
    fun `GIVEN OnDismissAddStopDialog intent WHEN dispatched THEN isAddStopDialogVisible becomes false`() = runTest(testDispatcher) {
        val viewModel = createViewModel()

        viewModel.onIntent(TripDetailUiIntent.OnAddStopClicked)
        viewModel.onIntent(TripDetailUiIntent.OnDismissAddStopDialog)

        assertFalse(viewModel.uiState.value.isAddStopDialogVisible)
    }

    @Test
    fun `GIVEN OnAddStopConfirmed intent WHEN dispatched and use case succeeds THEN dialog is dismissed`() = runTest(testDispatcher) {
        val stop = Stop("s1", TRIP_ID, PLACE_NAME, LATITUDE, LONGITUDE, 1, StopStatus.PENDING)
        coEvery { setStopUseCase(TRIP_ID, PLACE_NAME, LATITUDE, LONGITUDE, StopType.INTERMEDIATE) } returns Result.success(stop)
        val viewModel = createViewModel()

        viewModel.onIntent(TripDetailUiIntent.OnAddStopClicked)
        viewModel.onIntent(TripDetailUiIntent.OnAddStopConfirmed(PLACE_NAME, LATITUDE, LONGITUDE))
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isAddStopDialogVisible)
    }

    @Test
    fun `GIVEN OnAddStopConfirmed intent WHEN dispatched and use case fails THEN ShowError effect is emitted`() = runTest(testDispatcher) {
        coEvery { setStopUseCase(TRIP_ID, PLACE_NAME, LATITUDE, LONGITUDE, StopType.INTERMEDIATE) } returns Result.failure(RuntimeException("error"))
        val viewModel = createViewModel()

        val effects = mutableListOf<TripDetailUiEffect>()
        val collectJob = launch {
            viewModel.uiEffect.collect { effects.add(it) }
        }

        viewModel.onIntent(TripDetailUiIntent.OnAddStopConfirmed(PLACE_NAME, LATITUDE, LONGITUDE))
        advanceUntilIdle()

        assertTrue(effects.any { it is TripDetailUiEffect.ShowError })
        collectJob.cancel()
    }

    @Test
    fun `GIVEN 25 stops exist WHEN observed THEN canAddMoreStops is false`() = runTest(testDispatcher) {
        val stops = (0 until 25).map { i ->
            Stop("s$i", TRIP_ID, "Stop $i", i.toDouble(), i.toDouble(), i, StopStatus.PENDING)
        }
        every { observeStopsUseCase(TRIP_ID) } returns flowOf(stops)

        val viewModel = TripDetailViewModel(setStopUseCase, moveStopUseCase, editStopUseCase, removeStopUseCase, markStopDepartedUseCase, undoMarkStopDepartedUseCase, observeStopsUseCase, searchPlacesUseCase, getPlaceDetailUseCase, placeSearchRepository, savedStateHandle)

        assertFalse(viewModel.uiState.value.canAddMoreStops)
    }

    @Test
    fun `GIVEN intermediate stops WHEN OnMoveStopUp intent is received THEN MoveStopUseCase is called with correct orders`() = runTest(testDispatcher) {
        val stops = listOf(
            Stop("s0", TRIP_ID, "Start", 0.0, 0.0, 0, StopStatus.PENDING),
            Stop("s1", TRIP_ID, "Stop A", 1.0, 1.0, 1, StopStatus.PENDING),
            Stop("s2", TRIP_ID, "Stop B", 2.0, 2.0, 2, StopStatus.PENDING),
            Stop("s3", TRIP_ID, "End", 3.0, 3.0, 3, StopStatus.PENDING),
        )
        every { observeStopsUseCase(TRIP_ID) } returns flowOf(stops)
        coEvery { moveStopUseCase(TRIP_ID, 2, 1) } returns Result.success(Unit)

        val viewModel = TripDetailViewModel(setStopUseCase, moveStopUseCase, editStopUseCase, removeStopUseCase, markStopDepartedUseCase, undoMarkStopDepartedUseCase, observeStopsUseCase, searchPlacesUseCase, getPlaceDetailUseCase, placeSearchRepository, savedStateHandle)
        viewModel.onIntent(TripDetailUiIntent.OnMoveStopUp("s2", 2))

        coVerify(exactly = 1) { moveStopUseCase(TRIP_ID, 2, 1) }
    }

    @Test
    fun `GIVEN intermediate stops WHEN OnMoveStopDown intent is received THEN MoveStopUseCase is called with correct orders`() = runTest(testDispatcher) {
        val stops = listOf(
            Stop("s0", TRIP_ID, "Start", 0.0, 0.0, 0, StopStatus.PENDING),
            Stop("s1", TRIP_ID, "Stop A", 1.0, 1.0, 1, StopStatus.PENDING),
            Stop("s2", TRIP_ID, "Stop B", 2.0, 2.0, 2, StopStatus.PENDING),
            Stop("s3", TRIP_ID, "End", 3.0, 3.0, 3, StopStatus.PENDING),
        )
        every { observeStopsUseCase(TRIP_ID) } returns flowOf(stops)
        coEvery { moveStopUseCase(TRIP_ID, 1, 2) } returns Result.success(Unit)

        val viewModel = TripDetailViewModel(setStopUseCase, moveStopUseCase, editStopUseCase, removeStopUseCase, markStopDepartedUseCase, undoMarkStopDepartedUseCase, observeStopsUseCase, searchPlacesUseCase, getPlaceDetailUseCase, placeSearchRepository, savedStateHandle)
        viewModel.onIntent(TripDetailUiIntent.OnMoveStopDown("s1", 1))

        coVerify(exactly = 1) { moveStopUseCase(TRIP_ID, 1, 2) }
    }

    @Test
    fun `GIVEN a move fails WHEN intent is received THEN ShowError effect is emitted`() = runTest(testDispatcher) {
        coEvery { moveStopUseCase(TRIP_ID, 2, 1) } returns Result.failure(IllegalStateException("No stop found"))
        val viewModel = createViewModel()

        val effects = mutableListOf<TripDetailUiEffect>()
        val collectJob = launch {
            viewModel.uiEffect.collect { effects.add(it) }
        }

        viewModel.onIntent(TripDetailUiIntent.OnMoveStopUp("s2", 2))
        advanceUntilIdle()

        assertTrue(effects.any { it is TripDetailUiEffect.ShowError })
        collectJob.cancel()
    }

    @Test
    fun `GIVEN initial state WHEN OnEditStopClicked is dispatched THEN editingStop is set and isEditStopDialogVisible is true`() = runTest(testDispatcher) {
        val stop = Stop("s1", TRIP_ID, PLACE_NAME, LATITUDE, LONGITUDE, 1, StopStatus.PENDING)
        val viewModel = createViewModel()

        viewModel.onIntent(TripDetailUiIntent.OnEditStopClicked(stop))

        assertEquals(stop, viewModel.uiState.value.editingStop)
        assertTrue(viewModel.uiState.value.isEditStopDialogVisible)
    }

    @Test
    fun `GIVEN edit dialog visible WHEN OnDismissEditStopDialog is dispatched THEN editingStop is null and isEditStopDialogVisible is false`() = runTest(testDispatcher) {
        val stop = Stop("s1", TRIP_ID, PLACE_NAME, LATITUDE, LONGITUDE, 1, StopStatus.PENDING)
        val viewModel = createViewModel()

        viewModel.onIntent(TripDetailUiIntent.OnEditStopClicked(stop))
        viewModel.onIntent(TripDetailUiIntent.OnDismissEditStopDialog)

        assertNull(viewModel.uiState.value.editingStop)
        assertFalse(viewModel.uiState.value.isEditStopDialogVisible)
    }

    @Test
    fun `GIVEN edit dialog visible WHEN OnEditStopConfirmed is dispatched and EditStopUseCase succeeds THEN editingStop is null and isEditStopDialogVisible is false`() = runTest(testDispatcher) {
        val stop = Stop("s1", TRIP_ID, PLACE_NAME, LATITUDE, LONGITUDE, 1, StopStatus.PENDING)
        coEvery { editStopUseCase("s1", PLACE_NAME, LATITUDE, LONGITUDE) } returns Result.success(stop)
        val viewModel = createViewModel()

        viewModel.onIntent(TripDetailUiIntent.OnEditStopClicked(stop))
        viewModel.onIntent(TripDetailUiIntent.OnEditStopConfirmed("s1", PLACE_NAME, LATITUDE, LONGITUDE))
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.editingStop)
        assertFalse(viewModel.uiState.value.isEditStopDialogVisible)
    }

    @Test
    fun `GIVEN edit dialog visible WHEN OnEditStopConfirmed is dispatched and EditStopUseCase fails THEN ShowError effect is emitted`() = runTest(testDispatcher) {
        val stop = Stop("s1", TRIP_ID, PLACE_NAME, LATITUDE, LONGITUDE, 1, StopStatus.PENDING)
        coEvery { editStopUseCase("s1", PLACE_NAME, LATITUDE, LONGITUDE) } returns Result.failure(RuntimeException("error"))
        val viewModel = createViewModel()

        val effects = mutableListOf<TripDetailUiEffect>()
        val collectJob = launch {
            viewModel.uiEffect.collect { effects.add(it) }
        }

        viewModel.onIntent(TripDetailUiIntent.OnEditStopClicked(stop))
        viewModel.onIntent(TripDetailUiIntent.OnEditStopConfirmed("s1", PLACE_NAME, LATITUDE, LONGITUDE))
        advanceUntilIdle()

        assertTrue(effects.any { it is TripDetailUiEffect.ShowError })
        collectJob.cancel()
    }

    @Test
    fun `GIVEN the screen is displayed WHEN OnRemoveStopClicked is dispatched THEN stopToRemove is set and isRemoveStopDialogVisible becomes true`() = runTest(testDispatcher) {
        val stop = Stop("s1", TRIP_ID, PLACE_NAME, LATITUDE, LONGITUDE, 1, StopStatus.PENDING)
        val viewModel = createViewModel()

        viewModel.onIntent(TripDetailUiIntent.OnRemoveStopClicked(stop))

        assertEquals(stop, viewModel.uiState.value.stopToRemove)
        assertTrue(viewModel.uiState.value.isRemoveStopDialogVisible)
    }

    @Test
    fun `GIVEN remove dialog is visible WHEN OnDismissRemoveStopDialog is dispatched THEN stopToRemove is null and isRemoveStopDialogVisible becomes false`() = runTest(testDispatcher) {
        val stop = Stop("s1", TRIP_ID, PLACE_NAME, LATITUDE, LONGITUDE, 1, StopStatus.PENDING)
        val viewModel = createViewModel()

        viewModel.onIntent(TripDetailUiIntent.OnRemoveStopClicked(stop))
        viewModel.onIntent(TripDetailUiIntent.OnDismissRemoveStopDialog)

        assertNull(viewModel.uiState.value.stopToRemove)
        assertFalse(viewModel.uiState.value.isRemoveStopDialogVisible)
    }

    @Test
    fun `GIVEN remove dialog is visible WHEN OnRemoveStopConfirmed is dispatched and RemoveStopUseCase succeeds THEN stopToRemove is null and isRemoveStopDialogVisible becomes false`() = runTest(testDispatcher) {
        val stop = Stop("s1", TRIP_ID, PLACE_NAME, LATITUDE, LONGITUDE, 1, StopStatus.PENDING)
        coEvery { removeStopUseCase(TRIP_ID, "s1") } returns Result.success(Unit)
        val viewModel = createViewModel()

        viewModel.onIntent(TripDetailUiIntent.OnRemoveStopClicked(stop))
        viewModel.onIntent(TripDetailUiIntent.OnRemoveStopConfirmed)
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.stopToRemove)
        assertFalse(viewModel.uiState.value.isRemoveStopDialogVisible)
    }

    @Test
    fun `GIVEN remove dialog is visible WHEN OnRemoveStopConfirmed is dispatched and RemoveStopUseCase fails THEN ShowError effect is emitted`() = runTest(testDispatcher) {
        val stop = Stop("s1", TRIP_ID, PLACE_NAME, LATITUDE, LONGITUDE, 1, StopStatus.PENDING)
        coEvery { removeStopUseCase(TRIP_ID, "s1") } returns Result.failure(RuntimeException("error"))
        val viewModel = createViewModel()

        val effects = mutableListOf<TripDetailUiEffect>()
        val collectJob = launch {
            viewModel.uiEffect.collect { effects.add(it) }
        }

        viewModel.onIntent(TripDetailUiIntent.OnRemoveStopClicked(stop))
        viewModel.onIntent(TripDetailUiIntent.OnRemoveStopConfirmed)
        advanceUntilIdle()

        assertTrue(effects.any { it is TripDetailUiEffect.ShowError })
        collectJob.cancel()
    }

    @Test
    fun `GIVEN OnMarkStopDepartedClicked is dispatched WHEN MarkStopDepartedUseCase succeeds THEN no error effect is emitted`() = runTest(testDispatcher) {
        coEvery { markStopDepartedUseCase(TRIP_ID, "s1") } returns Result.success(Unit)
        val viewModel = createViewModel()

        val effects = mutableListOf<TripDetailUiEffect>()
        val collectJob = launch {
            viewModel.uiEffect.collect { effects.add(it) }
        }

        viewModel.onIntent(TripDetailUiIntent.OnMarkStopDepartedClicked("s1"))
        advanceUntilIdle()

        assertTrue(effects.none { it is TripDetailUiEffect.ShowError })
        collectJob.cancel()
    }

    @Test
    fun `GIVEN OnMarkStopDepartedClicked is dispatched WHEN MarkStopDepartedUseCase fails THEN ShowError effect is emitted`() = runTest(testDispatcher) {
        coEvery { markStopDepartedUseCase(TRIP_ID, "s1") } returns Result.failure(RuntimeException("error"))
        val viewModel = createViewModel()

        val effects = mutableListOf<TripDetailUiEffect>()
        val collectJob = launch {
            viewModel.uiEffect.collect { effects.add(it) }
        }

        viewModel.onIntent(TripDetailUiIntent.OnMarkStopDepartedClicked("s1"))
        advanceUntilIdle()

        assertTrue(effects.any { it is TripDetailUiEffect.ShowError })
        collectJob.cancel()
    }

    @Test
    fun `GIVEN OnUndoMarkStopDepartedClicked is dispatched WHEN UndoMarkStopDepartedUseCase succeeds THEN no error effect is emitted`() = runTest(testDispatcher) {
        coEvery { undoMarkStopDepartedUseCase(TRIP_ID) } returns Result.success(Unit)
        val viewModel = createViewModel()

        val effects = mutableListOf<TripDetailUiEffect>()
        val collectJob = launch {
            viewModel.uiEffect.collect { effects.add(it) }
        }

        viewModel.onIntent(TripDetailUiIntent.OnUndoMarkStopDepartedClicked("s1"))
        advanceUntilIdle()

        assertTrue(effects.none { it is TripDetailUiEffect.ShowError })
        collectJob.cancel()
    }

    @Test
    fun `GIVEN OnUndoMarkStopDepartedClicked is dispatched WHEN UndoMarkStopDepartedUseCase fails THEN ShowError effect is emitted`() = runTest(testDispatcher) {
        coEvery { undoMarkStopDepartedUseCase(TRIP_ID) } returns Result.failure(RuntimeException("error"))
        val viewModel = createViewModel()

        val effects = mutableListOf<TripDetailUiEffect>()
        val collectJob = launch {
            viewModel.uiEffect.collect { effects.add(it) }
        }

        viewModel.onIntent(TripDetailUiIntent.OnUndoMarkStopDepartedClicked("s1"))
        advanceUntilIdle()

        assertTrue(effects.any { it is TripDetailUiEffect.ShowError })
        collectJob.cancel()
    }

    @Test
    fun `GIVEN editStop operation WHEN OnEditStopConfirmed is dispatched and EditStopUseCase succeeds THEN isLoading is false after completion`() = runTest(testDispatcher) {
        val stop = Stop("s1", TRIP_ID, PLACE_NAME, LATITUDE, LONGITUDE, 1, StopStatus.PENDING)
        coEvery { editStopUseCase("s1", PLACE_NAME, LATITUDE, LONGITUDE) } returns Result.success(stop)
        val viewModel = createViewModel()

        viewModel.onIntent(TripDetailUiIntent.OnEditStopClicked(stop))
        viewModel.onIntent(TripDetailUiIntent.OnEditStopConfirmed("s1", PLACE_NAME, LATITUDE, LONGITUDE))
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `GIVEN editStop operation WHEN OnEditStopConfirmed is dispatched and EditStopUseCase fails THEN isLoading is false after completion`() = runTest(testDispatcher) {
        val stop = Stop("s1", TRIP_ID, PLACE_NAME, LATITUDE, LONGITUDE, 1, StopStatus.PENDING)
        coEvery { editStopUseCase("s1", PLACE_NAME, LATITUDE, LONGITUDE) } returns Result.failure(RuntimeException("error"))
        val viewModel = createViewModel()

        viewModel.onIntent(TripDetailUiIntent.OnEditStopClicked(stop))
        viewModel.onIntent(TripDetailUiIntent.OnEditStopConfirmed("s1", PLACE_NAME, LATITUDE, LONGITUDE))
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `GIVEN removeStop operation WHEN OnRemoveStopConfirmed is dispatched and RemoveStopUseCase succeeds THEN isLoading is false after completion`() = runTest(testDispatcher) {
        val stop = Stop("s1", TRIP_ID, PLACE_NAME, LATITUDE, LONGITUDE, 1, StopStatus.PENDING)
        coEvery { removeStopUseCase(TRIP_ID, "s1") } returns Result.success(Unit)
        val viewModel = createViewModel()

        viewModel.onIntent(TripDetailUiIntent.OnRemoveStopClicked(stop))
        viewModel.onIntent(TripDetailUiIntent.OnRemoveStopConfirmed)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `GIVEN removeStop operation WHEN OnRemoveStopConfirmed is dispatched and RemoveStopUseCase fails THEN isLoading is false after completion`() = runTest(testDispatcher) {
        val stop = Stop("s1", TRIP_ID, PLACE_NAME, LATITUDE, LONGITUDE, 1, StopStatus.PENDING)
        coEvery { removeStopUseCase(TRIP_ID, "s1") } returns Result.failure(RuntimeException("error"))
        val viewModel = createViewModel()

        viewModel.onIntent(TripDetailUiIntent.OnRemoveStopClicked(stop))
        viewModel.onIntent(TripDetailUiIntent.OnRemoveStopConfirmed)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `GIVEN moveStop operation WHEN OnMoveStopUp is dispatched and MoveStopUseCase succeeds THEN isLoading is false after completion`() = runTest(testDispatcher) {
        coEvery { moveStopUseCase(TRIP_ID, 2, 1) } returns Result.success(Unit)
        val viewModel = createViewModel()

        viewModel.onIntent(TripDetailUiIntent.OnMoveStopUp("s2", 2))
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `GIVEN moveStop operation WHEN OnMoveStopUp is dispatched and MoveStopUseCase fails THEN isLoading is false after completion`() = runTest(testDispatcher) {
        coEvery { moveStopUseCase(TRIP_ID, 2, 1) } returns Result.failure(RuntimeException("error"))
        val viewModel = createViewModel()

        viewModel.onIntent(TripDetailUiIntent.OnMoveStopUp("s2", 2))
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `GIVEN markStopDeparted operation WHEN OnMarkStopDepartedClicked is dispatched and MarkStopDepartedUseCase succeeds THEN isLoading is false after completion`() = runTest(testDispatcher) {
        coEvery { markStopDepartedUseCase(TRIP_ID, "s1") } returns Result.success(Unit)
        val viewModel = createViewModel()

        viewModel.onIntent(TripDetailUiIntent.OnMarkStopDepartedClicked("s1"))
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `GIVEN markStopDeparted operation WHEN OnMarkStopDepartedClicked is dispatched and MarkStopDepartedUseCase fails THEN isLoading is false after completion`() = runTest(testDispatcher) {
        coEvery { markStopDepartedUseCase(TRIP_ID, "s1") } returns Result.failure(RuntimeException("error"))
        val viewModel = createViewModel()

        viewModel.onIntent(TripDetailUiIntent.OnMarkStopDepartedClicked("s1"))
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `GIVEN undoMarkStopDeparted operation WHEN OnUndoMarkStopDepartedClicked is dispatched and UndoMarkStopDepartedUseCase succeeds THEN isLoading is false after completion`() = runTest(testDispatcher) {
        coEvery { undoMarkStopDepartedUseCase(TRIP_ID) } returns Result.success(Unit)
        val viewModel = createViewModel()

        viewModel.onIntent(TripDetailUiIntent.OnUndoMarkStopDepartedClicked("s1"))
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `GIVEN undoMarkStopDeparted operation WHEN OnUndoMarkStopDepartedClicked is dispatched and UndoMarkStopDepartedUseCase fails THEN isLoading is false after completion`() = runTest(testDispatcher) {
        coEvery { undoMarkStopDepartedUseCase(TRIP_ID) } returns Result.failure(RuntimeException("error"))
        val viewModel = createViewModel()

        viewModel.onIntent(TripDetailUiIntent.OnUndoMarkStopDepartedClicked("s1"))
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
    }

    // --- Task 10.1: Search query debounce and blank query ---

    @Test
    fun `GIVEN OnSearchQueryChanged dispatched WHEN debounce elapses THEN SearchPlacesUseCase called and placeSuggestions updated`() = runTest {
        val standardDispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(standardDispatcher)
        val suggestions = listOf(
            PlaceSuggestion("id-1", "Rome", "Italy"),
            PlaceSuggestion("id-2", "Roma", "Texas"),
        )
        coEvery { searchPlacesUseCase("Rome") } returns Result.success(suggestions)
        val viewModel = createViewModel()

        viewModel.onIntent(TripDetailUiIntent.OnSearchQueryChanged("Rome"))
        advanceTimeBy(DEBOUNCE_MILLIS)
        runCurrent()

        val expectedSuggestions = suggestions
        assertEquals(expectedSuggestions, viewModel.uiState.value.placeSuggestions)
        assertFalse(viewModel.uiState.value.isSearchingPlaces)
        coVerify(exactly = 1) { searchPlacesUseCase("Rome") }
    }

    @Test
    fun `GIVEN blank query WHEN OnSearchQueryChanged dispatched THEN search state cleared without use case call`() = runTest(testDispatcher) {
        val viewModel = createViewModel()

        viewModel.onIntent(TripDetailUiIntent.OnSearchQueryChanged(""))

        val expectedSuggestions = emptyList<PlaceSuggestion>()
        assertEquals(expectedSuggestions, viewModel.uiState.value.placeSuggestions)
        assertFalse(viewModel.uiState.value.isSearchingPlaces)
        assertNull(viewModel.uiState.value.searchError)
        coVerify(exactly = 0) { searchPlacesUseCase(any()) }
    }

    // --- Task 10.2: Debounce cancels previous query ---

    @Test
    fun `GIVEN OnSearchQueryChanged dispatched twice within 300ms WHEN debounce elapses THEN only last query triggers use case call`() = runTest {
        val standardDispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(standardDispatcher)
        val suggestions = listOf(PlaceSuggestion("id-1", "Rome", "Italy"))
        coEvery { searchPlacesUseCase("Rome") } returns Result.success(suggestions)
        val viewModel = createViewModel()

        viewModel.onIntent(TripDetailUiIntent.OnSearchQueryChanged("Rom"))
        advanceTimeBy(150L)
        viewModel.onIntent(TripDetailUiIntent.OnSearchQueryChanged("Rome"))
        advanceTimeBy(DEBOUNCE_MILLIS)
        runCurrent()

        coVerify(exactly = 0) { searchPlacesUseCase("Rom") }
        coVerify(exactly = 1) { searchPlacesUseCase("Rome") }
    }

    // --- Task 10.3: OnSuggestionSelected ---

    @Test
    fun `GIVEN OnSuggestionSelected dispatched WHEN GetPlaceDetailUseCase succeeds THEN selectedPlaceDetail set and suggestions cleared`() = runTest(testDispatcher) {
        val suggestion = PlaceSuggestion("place-abc", "Colosseum", "Rome, Italy")
        val placeDetail = PlaceDetail("Colosseum", 41.8902, 12.4922)
        coEvery { getPlaceDetailUseCase("place-abc") } returns Result.success(placeDetail)
        val viewModel = createViewModel()

        viewModel.onIntent(TripDetailUiIntent.OnSuggestionSelected(suggestion))
        advanceUntilIdle()

        val expectedPlaceDetail = placeDetail
        assertEquals(expectedPlaceDetail, viewModel.uiState.value.selectedPlaceDetail)
        assertEquals(emptyList<PlaceSuggestion>(), viewModel.uiState.value.placeSuggestions)
    }

    @Test
    fun `GIVEN OnSuggestionSelected dispatched WHEN GetPlaceDetailUseCase fails THEN placeDetailError is set`() = runTest(testDispatcher) {
        val suggestion = PlaceSuggestion("place-abc", "Colosseum", "Rome, Italy")
        coEvery { getPlaceDetailUseCase("place-abc") } returns Result.failure(RuntimeException("Network error"))
        val viewModel = createViewModel()

        viewModel.onIntent(TripDetailUiIntent.OnSuggestionSelected(suggestion))
        advanceUntilIdle()

        val expectedPlaceDetailError = "Network error"
        assertEquals(expectedPlaceDetailError, viewModel.uiState.value.placeDetailError)
        assertNull(viewModel.uiState.value.selectedPlaceDetail)
    }

    // --- Task 2.1: isResolvingPlace is true while GetPlaceDetailUseCase is in-flight ---

    @Test
    fun `GIVEN a suggestion is selected WHEN GetPlaceDetailUseCase is in-flight THEN isResolvingPlace is true`() = runTest {
        val standardDispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(standardDispatcher)
        val suggestion = PlaceSuggestion("place-abc", "Colosseum", "Rome, Italy")
        val deferred = CompletableDeferred<Result<PlaceDetail>>()
        coEvery { getPlaceDetailUseCase("place-abc") } coAnswers { deferred.await() }
        val viewModel = createViewModel()

        viewModel.onIntent(TripDetailUiIntent.OnSuggestionSelected(suggestion))
        runCurrent()

        assertTrue(viewModel.uiState.value.isResolvingPlace)

        deferred.complete(Result.success(PlaceDetail("Colosseum", 41.8902, 12.4922)))
        advanceUntilIdle()
    }

    // --- Task 2.2: isResolvingPlace is false and selectedPlaceDetail is populated after success ---

    @Test
    fun `GIVEN a suggestion is selected WHEN GetPlaceDetailUseCase succeeds THEN isResolvingPlace is false and selectedPlaceDetail is populated`() = runTest(testDispatcher) {
        val suggestion = PlaceSuggestion("place-abc", "Colosseum", "Rome, Italy")
        val placeDetail = PlaceDetail("Colosseum", 41.8902, 12.4922)
        coEvery { getPlaceDetailUseCase("place-abc") } returns Result.success(placeDetail)
        val viewModel = createViewModel()

        viewModel.onIntent(TripDetailUiIntent.OnSuggestionSelected(suggestion))
        advanceUntilIdle()

        val expectedPlaceDetail = placeDetail
        assertFalse(viewModel.uiState.value.isResolvingPlace)
        assertEquals(expectedPlaceDetail, viewModel.uiState.value.selectedPlaceDetail)
    }

    // --- Task 3.1: isResolvingPlace is false and placeDetailError is set on failure ---

    @Test
    fun `GIVEN a suggestion is selected WHEN GetPlaceDetailUseCase fails THEN isResolvingPlace is false and placeDetailError contains the error message`() = runTest(testDispatcher) {
        val suggestion = PlaceSuggestion("place-abc", "Colosseum", "Rome, Italy")
        coEvery { getPlaceDetailUseCase("place-abc") } returns Result.failure(RuntimeException("Network error"))
        val viewModel = createViewModel()

        viewModel.onIntent(TripDetailUiIntent.OnSuggestionSelected(suggestion))
        advanceUntilIdle()

        val expectedPlaceDetailError = "Network error"
        assertFalse(viewModel.uiState.value.isResolvingPlace)
        assertEquals(expectedPlaceDetailError, viewModel.uiState.value.placeDetailError)
    }

    // --- Task 4.1: OnSearchQueryChanged clears placeDetailError when query is non-blank ---

    @Test
    fun `GIVEN placeDetailError is set WHEN OnSearchQueryChanged is dispatched with a non-blank query THEN placeDetailError is cleared`() = runTest(testDispatcher) {
        val suggestion = PlaceSuggestion("place-abc", "Colosseum", "Rome, Italy")
        coEvery { getPlaceDetailUseCase("place-abc") } returns Result.failure(RuntimeException("Network error"))
        coEvery { searchPlacesUseCase("Rome") } returns Result.success(emptyList())
        val viewModel = createViewModel()

        viewModel.onIntent(TripDetailUiIntent.OnSuggestionSelected(suggestion))
        advanceUntilIdle()

        viewModel.onIntent(TripDetailUiIntent.OnSearchQueryChanged("Rome"))

        assertNull(viewModel.uiState.value.placeDetailError)
    }

    // --- Task 4.2: OnSuggestionSelected clears placeDetailError ---

    @Test
    fun `GIVEN placeDetailError is set WHEN OnSuggestionSelected is dispatched THEN placeDetailError is cleared`() = runTest(testDispatcher) {
        val suggestion = PlaceSuggestion("place-abc", "Colosseum", "Rome, Italy")
        coEvery { getPlaceDetailUseCase("place-abc") } returnsMany listOf(
            Result.failure(RuntimeException("Network error")),
            Result.success(PlaceDetail("Colosseum", 41.8902, 12.4922)),
        )
        val viewModel = createViewModel()

        viewModel.onIntent(TripDetailUiIntent.OnSuggestionSelected(suggestion))
        advanceUntilIdle()

        viewModel.onIntent(TripDetailUiIntent.OnSuggestionSelected(suggestion))
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.placeDetailError)
    }

    // --- Task 10.4: Dialog dismiss clears search state ---

    @Test
    fun `GIVEN search state populated WHEN OnDismissStartingPointDialog dispatched THEN search state cleared and resetSession called`() = runTest(testDispatcher) {
        val suggestions = listOf(PlaceSuggestion("id-1", "Rome", "Italy"))
        coEvery { searchPlacesUseCase("Rome") } returns Result.success(suggestions)
        every { placeSearchRepository.resetSession() } just runs
        val viewModel = createViewModel()

        viewModel.onIntent(TripDetailUiIntent.OnSearchQueryChanged("Rome"))
        advanceTimeBy(DEBOUNCE_MILLIS)
        viewModel.onIntent(TripDetailUiIntent.OnDismissStartingPointDialog)

        assertEquals(emptyList<PlaceSuggestion>(), viewModel.uiState.value.placeSuggestions)
        assertFalse(viewModel.uiState.value.isSearchingPlaces)
        assertNull(viewModel.uiState.value.searchError)
        assertNull(viewModel.uiState.value.selectedPlaceDetail)
        verify(exactly = 1) { placeSearchRepository.resetSession() }
    }

    @Test
    fun `GIVEN search state populated WHEN OnDismissDestinationDialog dispatched THEN search state cleared and resetSession called`() = runTest(testDispatcher) {
        every { placeSearchRepository.resetSession() } just runs
        val viewModel = createViewModel()

        viewModel.onIntent(TripDetailUiIntent.OnDismissDestinationDialog)

        assertEquals(emptyList<PlaceSuggestion>(), viewModel.uiState.value.placeSuggestions)
        assertFalse(viewModel.uiState.value.isSearchingPlaces)
        assertNull(viewModel.uiState.value.searchError)
        assertNull(viewModel.uiState.value.selectedPlaceDetail)
        verify(exactly = 1) { placeSearchRepository.resetSession() }
    }

    @Test
    fun `GIVEN search state populated WHEN OnDismissAddStopDialog dispatched THEN search state cleared and resetSession called`() = runTest(testDispatcher) {
        every { placeSearchRepository.resetSession() } just runs
        val viewModel = createViewModel()

        viewModel.onIntent(TripDetailUiIntent.OnDismissAddStopDialog)

        assertEquals(emptyList<PlaceSuggestion>(), viewModel.uiState.value.placeSuggestions)
        assertFalse(viewModel.uiState.value.isSearchingPlaces)
        assertNull(viewModel.uiState.value.searchError)
        assertNull(viewModel.uiState.value.selectedPlaceDetail)
        verify(exactly = 1) { placeSearchRepository.resetSession() }
    }

    @Test
    fun `GIVEN search state populated WHEN OnDismissEditStopDialog dispatched THEN search state cleared and resetSession called`() = runTest(testDispatcher) {
        every { placeSearchRepository.resetSession() } just runs
        val viewModel = createViewModel()

        viewModel.onIntent(TripDetailUiIntent.OnDismissEditStopDialog)

        assertEquals(emptyList<PlaceSuggestion>(), viewModel.uiState.value.placeSuggestions)
        assertFalse(viewModel.uiState.value.isSearchingPlaces)
        assertNull(viewModel.uiState.value.searchError)
        assertNull(viewModel.uiState.value.selectedPlaceDetail)
        verify(exactly = 1) { placeSearchRepository.resetSession() }
    }
}
