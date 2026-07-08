package com.guidovezzoni.venice.ui.viewmodel

import android.app.Application
import android.content.res.Resources
import androidx.lifecycle.SavedStateHandle
import com.guidovezzoni.venice.R
import com.guidovezzoni.venice.domain.analytics.AnalyticsEvent
import com.guidovezzoni.venice.domain.analytics.AnalyticsTracker
import com.guidovezzoni.venice.domain.model.Leg
import com.guidovezzoni.venice.domain.model.PlaceDetail
import com.guidovezzoni.venice.domain.model.PlaceSuggestion
import com.guidovezzoni.venice.domain.model.Stop
import com.guidovezzoni.venice.domain.model.StopStatus
import com.guidovezzoni.venice.domain.model.StopType
import com.guidovezzoni.venice.domain.repository.PlaceSearchRepository
import com.guidovezzoni.venice.domain.usecase.CalculateRouteUseCase
import com.guidovezzoni.venice.domain.usecase.EditStopUseCase
import com.guidovezzoni.venice.domain.usecase.GetPlaceDetailUseCase
import com.guidovezzoni.venice.domain.usecase.MarkStopDepartedUseCase
import com.guidovezzoni.venice.domain.usecase.MoveStopUseCase
import com.guidovezzoni.venice.domain.usecase.ObserveLegsUseCase
import com.guidovezzoni.venice.domain.usecase.ObserveStopsUseCase
import com.guidovezzoni.venice.domain.usecase.RemoveStopUseCase
import com.guidovezzoni.venice.domain.usecase.SearchPlacesUseCase
import com.guidovezzoni.venice.domain.usecase.SetStopUseCase
import com.guidovezzoni.venice.domain.usecase.UndoMarkStopDepartedUseCase
import com.guidovezzoni.venice.ui.effect.TripDetailUiEffect
import com.guidovezzoni.venice.ui.intent.TripDetailUiIntent
import com.guidovezzoni.venice.ui.state.DialogState
import com.guidovezzoni.venice.ui.state.RouteCalculationState
import com.guidovezzoni.venice.ui.util.formatCoordinates
import com.guidovezzoni.venice.ui.util.formatDistance
import com.guidovezzoni.venice.ui.util.formatDuration
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
import kotlinx.coroutines.flow.MutableSharedFlow
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
import java.util.Locale
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
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

    private lateinit var application: Application
    private lateinit var setStopUseCase: SetStopUseCase
    private lateinit var moveStopUseCase: MoveStopUseCase
    private lateinit var editStopUseCase: EditStopUseCase
    private lateinit var removeStopUseCase: RemoveStopUseCase
    private lateinit var markStopDepartedUseCase: MarkStopDepartedUseCase
    private lateinit var undoMarkStopDepartedUseCase: UndoMarkStopDepartedUseCase
    private lateinit var calculateRouteUseCase: CalculateRouteUseCase
    private lateinit var observeLegsUseCase: ObserveLegsUseCase
    private lateinit var observeStopsUseCase: ObserveStopsUseCase
    private lateinit var searchPlacesUseCase: SearchPlacesUseCase
    private lateinit var getPlaceDetailUseCase: GetPlaceDetailUseCase
    private lateinit var placeSearchRepository: PlaceSearchRepository
    private lateinit var analyticsTracker: AnalyticsTracker
    private lateinit var savedStateHandle: SavedStateHandle

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        val mockResources = mockk<Resources>()
        every { mockResources.getString(R.string.trip_detail_leg_distance_metres, any()) } returns "0 m"
        every { mockResources.getString(R.string.trip_detail_leg_distance_kilometres, any()) } returns "0.0 km"
        every { mockResources.getString(R.string.trip_detail_leg_distance_miles, any()) } returns "0.0 mi"
        every { mockResources.getString(R.string.trip_detail_leg_duration_minutes, any()) } returns "0 min"
        every { mockResources.getString(R.string.trip_detail_leg_duration_hours_minutes, any(), any()) } returns "0h 0min"
        application = mockk()
        every { application.resources } returns mockResources
        setStopUseCase = mockk()
        moveStopUseCase = mockk()
        editStopUseCase = mockk()
        removeStopUseCase = mockk()
        markStopDepartedUseCase = mockk()
        undoMarkStopDepartedUseCase = mockk()
        calculateRouteUseCase = mockk()
        observeLegsUseCase = mockk()
        observeStopsUseCase = mockk()
        searchPlacesUseCase = mockk()
        getPlaceDetailUseCase = mockk()
        placeSearchRepository = mockk(relaxed = true)
        analyticsTracker = mockk(relaxed = true)
        savedStateHandle = SavedStateHandle(mapOf("tripId" to TRIP_ID))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(
        stops: List<Stop> = emptyList(),
        legs: List<Leg> = emptyList(),
    ): TripDetailViewModel {
        every { observeStopsUseCase(TRIP_ID) } returns flowOf(stops)
        every { observeLegsUseCase(TRIP_ID) } returns flowOf(legs)
        return TripDetailViewModel(application, setStopUseCase, moveStopUseCase, editStopUseCase, removeStopUseCase, markStopDepartedUseCase, undoMarkStopDepartedUseCase, calculateRouteUseCase, observeStopsUseCase, observeLegsUseCase, searchPlacesUseCase, getPlaceDetailUseCase, placeSearchRepository, analyticsTracker, savedStateHandle)
    }

    @Test
    fun `GIVEN initial state WHEN OnSetStartingPointClicked is dispatched THEN dialogState becomes SetStartingPoint`() = runTest(testDispatcher) {
        val viewModel = createViewModel()

        viewModel.onIntent(TripDetailUiIntent.OnSetStartingPointClicked)

        assertEquals(DialogState.SetStartingPoint, viewModel.uiState.value.dialogState)
    }

    @Test
    fun `GIVEN dialog is visible WHEN OnDismissStartingPointDialog is dispatched THEN dialogState becomes None`() = runTest(testDispatcher) {
        val viewModel = createViewModel()

        viewModel.onIntent(TripDetailUiIntent.OnSetStartingPointClicked)
        viewModel.onIntent(TripDetailUiIntent.OnDismissStartingPointDialog)

        assertEquals(DialogState.None, viewModel.uiState.value.dialogState)
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
        coEvery { setStopUseCase(TRIP_ID, PLACE_NAME, LATITUDE, LONGITUDE, StopType.STARTING_POINT) } returns Result.success(stop)

        val viewModel = createViewModel(stops = listOf(stop))
        viewModel.onIntent(TripDetailUiIntent.OnSetStartingPointClicked)
        viewModel.onIntent(TripDetailUiIntent.OnStartingPointConfirmed(PLACE_NAME, LATITUDE, LONGITUDE))
        advanceUntilIdle()

        assertEquals(DialogState.None, viewModel.uiState.value.dialogState)
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
        val viewModel = createViewModel(stops = listOf(expectedStop))

        assertEquals(expectedStop, viewModel.uiState.value.startingPoint)
    }

    @Test
    fun `GIVEN no stops in the stream WHEN ViewModel initialises THEN startingPoint is null`() = runTest(testDispatcher) {
        val viewModel = createViewModel()

        assertNull(viewModel.uiState.value.startingPoint)
    }

    @Test
    fun `GIVEN initial state WHEN OnSetDestinationClicked is dispatched THEN dialogState becomes SetDestination`() = runTest(testDispatcher) {
        val viewModel = createViewModel()

        viewModel.onIntent(TripDetailUiIntent.OnSetDestinationClicked)

        assertEquals(DialogState.SetDestination, viewModel.uiState.value.dialogState)
    }

    @Test
    fun `GIVEN dialog is visible WHEN OnDismissDestinationDialog is dispatched THEN dialogState becomes None`() = runTest(testDispatcher) {
        val viewModel = createViewModel()

        viewModel.onIntent(TripDetailUiIntent.OnSetDestinationClicked)
        viewModel.onIntent(TripDetailUiIntent.OnDismissDestinationDialog)

        assertEquals(DialogState.None, viewModel.uiState.value.dialogState)
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
        coEvery { setStopUseCase(TRIP_ID, PLACE_NAME, LATITUDE, LONGITUDE, StopType.DESTINATION) } returns Result.success(destinationStop)

        val viewModel = createViewModel(stops = listOf(destinationStop))
        viewModel.onIntent(TripDetailUiIntent.OnSetDestinationClicked)
        viewModel.onIntent(TripDetailUiIntent.OnDestinationConfirmed(PLACE_NAME, LATITUDE, LONGITUDE))
        advanceUntilIdle()

        assertEquals(DialogState.None, viewModel.uiState.value.dialogState)
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
        val viewModel = createViewModel(stops = listOf(expectedDestination))

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
        val viewModel = createViewModel(stops = listOf(startingPoint))

        assertNull(viewModel.uiState.value.destination)
    }

    @Test
    fun `GIVEN stops exist WHEN observed THEN intermediateStops contains only stops with order between start and destination`() = runTest(testDispatcher) {
        val startingPoint = Stop("s0", TRIP_ID, "Start", 0.0, 0.0, 0, StopStatus.PENDING)
        val intermediate1 = Stop("s1", TRIP_ID, "Stop A", 1.0, 1.0, 1, StopStatus.PENDING)
        val intermediate2 = Stop("s2", TRIP_ID, "Stop B", 2.0, 2.0, 2, StopStatus.PENDING)
        val destination = Stop("s3", TRIP_ID, "End", 3.0, 3.0, 3, StopStatus.PENDING)
        val viewModel = createViewModel(stops = listOf(startingPoint, intermediate1, intermediate2, destination))

        val expectedIntermediateStops = listOf(intermediate1, intermediate2)
        assertEquals(expectedIntermediateStops, viewModel.uiState.value.intermediateStops)
        assertEquals(startingPoint, viewModel.uiState.value.startingPoint)
        assertEquals(destination, viewModel.uiState.value.destination)
    }

    @Test
    fun `GIVEN OnAddStopClicked intent WHEN dispatched THEN dialogState becomes AddStop`() = runTest(testDispatcher) {
        val viewModel = createViewModel()

        viewModel.onIntent(TripDetailUiIntent.OnAddStopClicked)

        assertEquals(DialogState.AddStop, viewModel.uiState.value.dialogState)
    }

    @Test
    fun `GIVEN OnDismissAddStopDialog intent WHEN dispatched THEN dialogState becomes None`() = runTest(testDispatcher) {
        val viewModel = createViewModel()

        viewModel.onIntent(TripDetailUiIntent.OnAddStopClicked)
        viewModel.onIntent(TripDetailUiIntent.OnDismissAddStopDialog)

        assertEquals(DialogState.None, viewModel.uiState.value.dialogState)
    }

    @Test
    fun `GIVEN OnAddStopConfirmed intent WHEN dispatched and use case succeeds THEN dialog is dismissed`() = runTest(testDispatcher) {
        val stop = Stop("s1", TRIP_ID, PLACE_NAME, LATITUDE, LONGITUDE, 1, StopStatus.PENDING)
        coEvery { setStopUseCase(TRIP_ID, PLACE_NAME, LATITUDE, LONGITUDE, StopType.INTERMEDIATE) } returns Result.success(stop)
        val viewModel = createViewModel()

        viewModel.onIntent(TripDetailUiIntent.OnAddStopClicked)
        viewModel.onIntent(TripDetailUiIntent.OnAddStopConfirmed(PLACE_NAME, LATITUDE, LONGITUDE))
        advanceUntilIdle()

        assertEquals(DialogState.None, viewModel.uiState.value.dialogState)
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
        val viewModel = createViewModel(stops = stops)

        assertFalse(viewModel.uiState.value.canAddMoreStops)
    }

    @Test
    fun `GIVEN stops with coordinates WHEN observed THEN formattedStopCoordinates maps each stop id to locale-formatted string`() = runTest(testDispatcher) {
        val stops = listOf(
            Stop("s0", TRIP_ID, "Rome", 41.9028, 12.4964, 0, StopStatus.PENDING),
            Stop("s1", TRIP_ID, "Florence", 43.7696, 11.2558, 1, StopStatus.PENDING),
        )
        val viewModel = createViewModel(stops = stops)

        val expected = mapOf(
            "s0" to formatCoordinates(41.9028, 12.4964),
            "s1" to formatCoordinates(43.7696, 11.2558),
        )
        assertEquals(expected, viewModel.uiState.value.formattedStopCoordinates)
    }

    @Test
    fun `GIVEN intermediate stops WHEN OnMoveStopUp intent is received THEN MoveStopUseCase is called with correct orders`() = runTest(testDispatcher) {
        val stops = listOf(
            Stop("s0", TRIP_ID, "Start", 0.0, 0.0, 0, StopStatus.PENDING),
            Stop("s1", TRIP_ID, "Stop A", 1.0, 1.0, 1, StopStatus.PENDING),
            Stop("s2", TRIP_ID, "Stop B", 2.0, 2.0, 2, StopStatus.PENDING),
            Stop("s3", TRIP_ID, "End", 3.0, 3.0, 3, StopStatus.PENDING),
        )
        coEvery { moveStopUseCase(TRIP_ID, 2, 1) } returns Result.success(Unit)

        val viewModel = createViewModel(stops = stops)
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
        coEvery { moveStopUseCase(TRIP_ID, 1, 2) } returns Result.success(Unit)

        val viewModel = createViewModel(stops = stops)
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
    fun `GIVEN initial state WHEN OnEditStopClicked is dispatched THEN dialogState becomes EditStop with the stop`() = runTest(testDispatcher) {
        val stop = Stop("s1", TRIP_ID, PLACE_NAME, LATITUDE, LONGITUDE, 1, StopStatus.PENDING)
        val viewModel = createViewModel()

        viewModel.onIntent(TripDetailUiIntent.OnEditStopClicked(stop))

        assertEquals(DialogState.EditStop(stop), viewModel.uiState.value.dialogState)
    }

    @Test
    fun `GIVEN edit dialog visible WHEN OnDismissEditStopDialog is dispatched THEN dialogState becomes None`() = runTest(testDispatcher) {
        val stop = Stop("s1", TRIP_ID, PLACE_NAME, LATITUDE, LONGITUDE, 1, StopStatus.PENDING)
        val viewModel = createViewModel()

        viewModel.onIntent(TripDetailUiIntent.OnEditStopClicked(stop))
        viewModel.onIntent(TripDetailUiIntent.OnDismissEditStopDialog)

        assertEquals(DialogState.None, viewModel.uiState.value.dialogState)
    }

    @Test
    fun `GIVEN edit dialog visible WHEN OnEditStopConfirmed is dispatched and EditStopUseCase succeeds THEN dialogState becomes None`() = runTest(testDispatcher) {
        val stop = Stop("s1", TRIP_ID, PLACE_NAME, LATITUDE, LONGITUDE, 1, StopStatus.PENDING)
        coEvery { editStopUseCase("s1", PLACE_NAME, LATITUDE, LONGITUDE) } returns Result.success(stop)
        val viewModel = createViewModel()

        viewModel.onIntent(TripDetailUiIntent.OnEditStopClicked(stop))
        viewModel.onIntent(TripDetailUiIntent.OnEditStopConfirmed("s1", PLACE_NAME, LATITUDE, LONGITUDE))
        advanceUntilIdle()

        assertEquals(DialogState.None, viewModel.uiState.value.dialogState)
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
    fun `GIVEN the screen is displayed WHEN OnRemoveStopClicked is dispatched THEN dialogState becomes RemoveStop with the stop`() = runTest(testDispatcher) {
        val stop = Stop("s1", TRIP_ID, PLACE_NAME, LATITUDE, LONGITUDE, 1, StopStatus.PENDING)
        val viewModel = createViewModel()

        viewModel.onIntent(TripDetailUiIntent.OnRemoveStopClicked(stop))

        assertEquals(DialogState.RemoveStop(stop), viewModel.uiState.value.dialogState)
    }

    @Test
    fun `GIVEN remove dialog is visible WHEN OnDismissRemoveStopDialog is dispatched THEN dialogState becomes None`() = runTest(testDispatcher) {
        val stop = Stop("s1", TRIP_ID, PLACE_NAME, LATITUDE, LONGITUDE, 1, StopStatus.PENDING)
        val viewModel = createViewModel()

        viewModel.onIntent(TripDetailUiIntent.OnRemoveStopClicked(stop))
        viewModel.onIntent(TripDetailUiIntent.OnDismissRemoveStopDialog)

        assertEquals(DialogState.None, viewModel.uiState.value.dialogState)
    }

    @Test
    fun `GIVEN remove dialog is visible WHEN OnRemoveStopConfirmed is dispatched and RemoveStopUseCase succeeds THEN dialogState becomes None`() = runTest(testDispatcher) {
        val stop = Stop("s1", TRIP_ID, PLACE_NAME, LATITUDE, LONGITUDE, 1, StopStatus.PENDING)
        coEvery { removeStopUseCase(TRIP_ID, "s1") } returns Result.success(Unit)
        val viewModel = createViewModel()

        viewModel.onIntent(TripDetailUiIntent.OnRemoveStopClicked(stop))
        viewModel.onIntent(TripDetailUiIntent.OnRemoveStopConfirmed)
        advanceUntilIdle()

        assertEquals(DialogState.None, viewModel.uiState.value.dialogState)
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
        assertEquals(expectedSuggestions, viewModel.uiState.value.placeSearchState.suggestions)
        assertFalse(viewModel.uiState.value.placeSearchState.isSearching)
        coVerify(exactly = 1) { searchPlacesUseCase("Rome") }
    }

    @Test
    fun `GIVEN blank query WHEN OnSearchQueryChanged dispatched THEN search state cleared without use case call`() = runTest(testDispatcher) {
        val viewModel = createViewModel()

        viewModel.onIntent(TripDetailUiIntent.OnSearchQueryChanged(""))

        val expectedSuggestions = emptyList<PlaceSuggestion>()
        assertEquals(expectedSuggestions, viewModel.uiState.value.placeSearchState.suggestions)
        assertFalse(viewModel.uiState.value.placeSearchState.isSearching)
        assertNull(viewModel.uiState.value.placeSearchState.searchError)
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
        assertEquals(expectedPlaceDetail, viewModel.uiState.value.placeSearchState.selectedPlaceDetail)
        assertEquals(emptyList<PlaceSuggestion>(), viewModel.uiState.value.placeSearchState.suggestions)
    }

    @Test
    fun `GIVEN OnSuggestionSelected dispatched WHEN GetPlaceDetailUseCase fails THEN placeDetailError is set`() = runTest(testDispatcher) {
        val suggestion = PlaceSuggestion("place-abc", "Colosseum", "Rome, Italy")
        coEvery { getPlaceDetailUseCase("place-abc") } returns Result.failure(RuntimeException("Network error"))
        val viewModel = createViewModel()

        viewModel.onIntent(TripDetailUiIntent.OnSuggestionSelected(suggestion))
        advanceUntilIdle()

        val expectedPlaceDetailError = "Network error"
        assertEquals(expectedPlaceDetailError, viewModel.uiState.value.placeSearchState.placeDetailError)
        assertNull(viewModel.uiState.value.placeSearchState.selectedPlaceDetail)
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

        assertTrue(viewModel.uiState.value.placeSearchState.isResolvingPlace)

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
        assertFalse(viewModel.uiState.value.placeSearchState.isResolvingPlace)
        assertEquals(expectedPlaceDetail, viewModel.uiState.value.placeSearchState.selectedPlaceDetail)
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
        assertFalse(viewModel.uiState.value.placeSearchState.isResolvingPlace)
        assertEquals(expectedPlaceDetailError, viewModel.uiState.value.placeSearchState.placeDetailError)
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

        assertNull(viewModel.uiState.value.placeSearchState.placeDetailError)
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

        assertNull(viewModel.uiState.value.placeSearchState.placeDetailError)
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

        assertEquals(emptyList<PlaceSuggestion>(), viewModel.uiState.value.placeSearchState.suggestions)
        assertFalse(viewModel.uiState.value.placeSearchState.isSearching)
        assertNull(viewModel.uiState.value.placeSearchState.searchError)
        assertNull(viewModel.uiState.value.placeSearchState.selectedPlaceDetail)
        verify(exactly = 1) { placeSearchRepository.resetSession() }
    }

    @Test
    fun `GIVEN search state populated WHEN OnDismissDestinationDialog dispatched THEN search state cleared and resetSession called`() = runTest(testDispatcher) {
        every { placeSearchRepository.resetSession() } just runs
        val viewModel = createViewModel()

        viewModel.onIntent(TripDetailUiIntent.OnDismissDestinationDialog)

        assertEquals(emptyList<PlaceSuggestion>(), viewModel.uiState.value.placeSearchState.suggestions)
        assertFalse(viewModel.uiState.value.placeSearchState.isSearching)
        assertNull(viewModel.uiState.value.placeSearchState.searchError)
        assertNull(viewModel.uiState.value.placeSearchState.selectedPlaceDetail)
        verify(exactly = 1) { placeSearchRepository.resetSession() }
    }

    @Test
    fun `GIVEN search state populated WHEN OnDismissAddStopDialog dispatched THEN search state cleared and resetSession called`() = runTest(testDispatcher) {
        every { placeSearchRepository.resetSession() } just runs
        val viewModel = createViewModel()

        viewModel.onIntent(TripDetailUiIntent.OnDismissAddStopDialog)

        assertEquals(emptyList<PlaceSuggestion>(), viewModel.uiState.value.placeSearchState.suggestions)
        assertFalse(viewModel.uiState.value.placeSearchState.isSearching)
        assertNull(viewModel.uiState.value.placeSearchState.searchError)
        assertNull(viewModel.uiState.value.placeSearchState.selectedPlaceDetail)
        verify(exactly = 1) { placeSearchRepository.resetSession() }
    }

    @Test
    fun `GIVEN search state populated WHEN OnDismissEditStopDialog dispatched THEN search state cleared and resetSession called`() = runTest(testDispatcher) {
        every { placeSearchRepository.resetSession() } just runs
        val viewModel = createViewModel()

        viewModel.onIntent(TripDetailUiIntent.OnDismissEditStopDialog)

        assertEquals(emptyList<PlaceSuggestion>(), viewModel.uiState.value.placeSearchState.suggestions)
        assertFalse(viewModel.uiState.value.placeSearchState.isSearching)
        assertNull(viewModel.uiState.value.placeSearchState.searchError)
        assertNull(viewModel.uiState.value.placeSearchState.selectedPlaceDetail)
        verify(exactly = 1) { placeSearchRepository.resetSession() }
    }

    // --- Section 10: Route Handling ---

    @Test
    fun `GIVEN legs exist in database WHEN ViewModel initialises THEN uiState legs contains the persisted legs`() = runTest(testDispatcher) {
        val legs = listOf(Leg("leg-1", TRIP_ID, "stop-a", "stop-b", 1000, 60, ""))

        val viewModel = createViewModel(legs = legs)

        val expectedLegs = legs
        assertEquals(expectedLegs, viewModel.uiState.value.legs)
    }

    @Test
    fun `GIVEN OnCalculateRouteClicked dispatched WHEN CalculateRouteUseCase succeeds THEN routeCalculationState becomes Idle`() = runTest(testDispatcher) {
        coEvery { calculateRouteUseCase(TRIP_ID, any()) } returns Result.success(Unit)
        val viewModel = createViewModel()

        viewModel.onIntent(TripDetailUiIntent.OnCalculateRouteClicked)
        advanceUntilIdle()

        assertEquals(RouteCalculationState.Idle, viewModel.uiState.value.routeCalculationState)
    }

    @Test
    fun `GIVEN OnCalculateRouteClicked dispatched WHEN CalculateRouteUseCase fails THEN routeCalculationState becomes Error with message`() = runTest(testDispatcher) {
        val errorMessage = "Network error"
        coEvery { calculateRouteUseCase(TRIP_ID, any()) } returns Result.failure(RuntimeException(errorMessage))
        val viewModel = createViewModel()

        viewModel.onIntent(TripDetailUiIntent.OnCalculateRouteClicked)
        advanceUntilIdle()

        val expectedRouteCalculationState = RouteCalculationState.Error(errorMessage)
        assertEquals(expectedRouteCalculationState, viewModel.uiState.value.routeCalculationState)
    }

    @Test
    fun `GIVEN OnCalculateRouteClicked dispatched WHEN API call is in-flight THEN routeCalculationState is Calculating`() = runTest(testDispatcher) {
        val deferred = CompletableDeferred<Result<Unit>>()
        coEvery { calculateRouteUseCase(TRIP_ID, any()) } coAnswers { deferred.await() }
        val viewModel = createViewModel()

        val job = launch { viewModel.onIntent(TripDetailUiIntent.OnCalculateRouteClicked) }
        runCurrent()

        assertEquals(RouteCalculationState.Calculating, viewModel.uiState.value.routeCalculationState)

        deferred.complete(Result.success(Unit))
        advanceUntilIdle()
        job.join()
    }

    // --- Section 11: Analytics Tracking ---

    @Test
    fun `GIVEN ViewModel initialised WHEN init completes THEN ScreenViewed is tracked`() = runTest(testDispatcher) {
        createViewModel()

        verify(exactly = 1) {
            analyticsTracker.track(match { it is AnalyticsEvent.ScreenViewed && it.screenName == AnalyticsEvent.SCREEN_TRIP_DETAIL })
        }
    }

    @Test
    fun `GIVEN valid input WHEN OnStartingPointConfirmed succeeds THEN StopSet is tracked with STARTING_POINT`() = runTest(testDispatcher) {
        coEvery { setStopUseCase(TRIP_ID, PLACE_NAME, LATITUDE, LONGITUDE, StopType.STARTING_POINT) } returns Result.success(
            Stop("s1", TRIP_ID, PLACE_NAME, LATITUDE, LONGITUDE, 0, StopStatus.PENDING)
        )
        val viewModel = createViewModel()

        viewModel.onIntent(TripDetailUiIntent.OnStartingPointConfirmed(PLACE_NAME, LATITUDE, LONGITUDE))
        advanceUntilIdle()

        verify(exactly = 1) {
            analyticsTracker.track(match { it is AnalyticsEvent.StopSet && it.stopType == "STARTING_POINT" })
        }
    }

    @Test
    fun `GIVEN valid input WHEN OnDestinationConfirmed succeeds THEN StopSet is tracked with DESTINATION`() = runTest(testDispatcher) {
        coEvery { setStopUseCase(TRIP_ID, PLACE_NAME, LATITUDE, LONGITUDE, StopType.DESTINATION) } returns Result.success(
            Stop("s2", TRIP_ID, PLACE_NAME, LATITUDE, LONGITUDE, 1, StopStatus.PENDING)
        )
        val viewModel = createViewModel()

        viewModel.onIntent(TripDetailUiIntent.OnDestinationConfirmed(PLACE_NAME, LATITUDE, LONGITUDE))
        advanceUntilIdle()

        verify(exactly = 1) {
            analyticsTracker.track(match { it is AnalyticsEvent.StopSet && it.stopType == "DESTINATION" })
        }
    }

    @Test
    fun `GIVEN valid input WHEN OnAddStopConfirmed succeeds THEN StopSet is tracked with INTERMEDIATE`() = runTest(testDispatcher) {
        coEvery { setStopUseCase(TRIP_ID, PLACE_NAME, LATITUDE, LONGITUDE, StopType.INTERMEDIATE) } returns Result.success(
            Stop("s3", TRIP_ID, PLACE_NAME, LATITUDE, LONGITUDE, 1, StopStatus.PENDING)
        )
        val viewModel = createViewModel()

        viewModel.onIntent(TripDetailUiIntent.OnAddStopConfirmed(PLACE_NAME, LATITUDE, LONGITUDE))
        advanceUntilIdle()

        verify(exactly = 1) {
            analyticsTracker.track(match { it is AnalyticsEvent.StopSet && it.stopType == "INTERMEDIATE" })
        }
    }

    @Test
    fun `GIVEN valid input WHEN OnEditStopConfirmed succeeds THEN StopEdited is tracked`() = runTest(testDispatcher) {
        val stop = Stop("s1", TRIP_ID, PLACE_NAME, LATITUDE, LONGITUDE, 1, StopStatus.PENDING)
        coEvery { editStopUseCase("s1", PLACE_NAME, LATITUDE, LONGITUDE) } returns Result.success(stop)
        val viewModel = createViewModel()

        viewModel.onIntent(TripDetailUiIntent.OnEditStopConfirmed("s1", PLACE_NAME, LATITUDE, LONGITUDE))
        advanceUntilIdle()

        verify(exactly = 1) {
            analyticsTracker.track(match { it is AnalyticsEvent.StopEdited && it.tripId == TRIP_ID })
        }
    }

    @Test
    fun `GIVEN remove dialog visible WHEN OnRemoveStopConfirmed succeeds THEN StopRemoved is tracked`() = runTest(testDispatcher) {
        val stop = Stop("s1", TRIP_ID, PLACE_NAME, LATITUDE, LONGITUDE, 1, StopStatus.PENDING)
        coEvery { removeStopUseCase(TRIP_ID, "s1") } returns Result.success(Unit)
        val viewModel = createViewModel()

        viewModel.onIntent(TripDetailUiIntent.OnRemoveStopClicked(stop))
        viewModel.onIntent(TripDetailUiIntent.OnRemoveStopConfirmed)
        advanceUntilIdle()

        verify(exactly = 1) {
            analyticsTracker.track(match { it is AnalyticsEvent.StopRemoved && it.tripId == TRIP_ID })
        }
    }

    @Test
    fun `GIVEN intermediate stops WHEN OnMoveStopUp succeeds THEN StopReordered is tracked`() = runTest(testDispatcher) {
        coEvery { moveStopUseCase(TRIP_ID, 2, 1) } returns Result.success(Unit)
        val viewModel = createViewModel()

        viewModel.onIntent(TripDetailUiIntent.OnMoveStopUp("s2", 2))
        advanceUntilIdle()

        verify(exactly = 1) {
            analyticsTracker.track(match { it is AnalyticsEvent.StopReordered && it.tripId == TRIP_ID })
        }
    }

    @Test
    fun `GIVEN a stop WHEN OnMarkStopDepartedClicked succeeds THEN StopDeparted is tracked`() = runTest(testDispatcher) {
        coEvery { markStopDepartedUseCase(TRIP_ID, "s1") } returns Result.success(Unit)
        val viewModel = createViewModel()

        viewModel.onIntent(TripDetailUiIntent.OnMarkStopDepartedClicked("s1"))
        advanceUntilIdle()

        verify(exactly = 1) {
            analyticsTracker.track(match { it is AnalyticsEvent.StopDeparted && it.tripId == TRIP_ID && it.stopId == "s1" })
        }
    }

    @Test
    fun `GIVEN stops WHEN OnCalculateRouteClicked succeeds THEN RouteCalculated is tracked`() = runTest(testDispatcher) {
        coEvery { calculateRouteUseCase(TRIP_ID, any()) } returns Result.success(Unit)
        val viewModel = createViewModel()

        viewModel.onIntent(TripDetailUiIntent.OnCalculateRouteClicked)
        advanceUntilIdle()

        verify(exactly = 1) {
            analyticsTracker.track(match { it is AnalyticsEvent.RouteCalculated && it.tripId == TRIP_ID })
        }
    }

    @Test
    fun `GIVEN setStop fails WHEN OnStartingPointConfirmed is dispatched THEN OperationFailed is tracked`() = runTest(testDispatcher) {
        coEvery { setStopUseCase(TRIP_ID, PLACE_NAME, LATITUDE, LONGITUDE, StopType.STARTING_POINT) } returns Result.failure(RuntimeException("error"))
        val viewModel = createViewModel()

        viewModel.onIntent(TripDetailUiIntent.OnStartingPointConfirmed(PLACE_NAME, LATITUDE, LONGITUDE))
        advanceUntilIdle()

        verify(exactly = 1) {
            analyticsTracker.track(match { it is AnalyticsEvent.OperationFailed && it.operation == AnalyticsEvent.OPERATION_SET_STOP })
        }
    }

    @Test
    fun `GIVEN editStop fails WHEN OnEditStopConfirmed is dispatched THEN OperationFailed is tracked`() = runTest(testDispatcher) {
        coEvery { editStopUseCase("s1", PLACE_NAME, LATITUDE, LONGITUDE) } returns Result.failure(RuntimeException("error"))
        val viewModel = createViewModel()

        viewModel.onIntent(TripDetailUiIntent.OnEditStopConfirmed("s1", PLACE_NAME, LATITUDE, LONGITUDE))
        advanceUntilIdle()

        verify(exactly = 1) {
            analyticsTracker.track(match { it is AnalyticsEvent.OperationFailed && it.operation == AnalyticsEvent.OPERATION_EDIT_STOP })
        }
    }

    @Test
    fun `GIVEN removeStop fails WHEN OnRemoveStopConfirmed is dispatched THEN OperationFailed is tracked`() = runTest(testDispatcher) {
        val stop = Stop("s1", TRIP_ID, PLACE_NAME, LATITUDE, LONGITUDE, 1, StopStatus.PENDING)
        coEvery { removeStopUseCase(TRIP_ID, "s1") } returns Result.failure(RuntimeException("error"))
        val viewModel = createViewModel()

        viewModel.onIntent(TripDetailUiIntent.OnRemoveStopClicked(stop))
        viewModel.onIntent(TripDetailUiIntent.OnRemoveStopConfirmed)
        advanceUntilIdle()

        verify(exactly = 1) {
            analyticsTracker.track(match { it is AnalyticsEvent.OperationFailed && it.operation == AnalyticsEvent.OPERATION_REMOVE_STOP })
        }
    }

    @Test
    fun `GIVEN moveStop fails WHEN OnMoveStopUp is dispatched THEN OperationFailed is tracked`() = runTest(testDispatcher) {
        coEvery { moveStopUseCase(TRIP_ID, 2, 1) } returns Result.failure(RuntimeException("error"))
        val viewModel = createViewModel()

        viewModel.onIntent(TripDetailUiIntent.OnMoveStopUp("s2", 2))
        advanceUntilIdle()

        verify(exactly = 1) {
            analyticsTracker.track(match { it is AnalyticsEvent.OperationFailed && it.operation == AnalyticsEvent.OPERATION_MOVE_STOP })
        }
    }

    @Test
    fun `GIVEN markStopDeparted fails WHEN OnMarkStopDepartedClicked is dispatched THEN OperationFailed is tracked`() = runTest(testDispatcher) {
        coEvery { markStopDepartedUseCase(TRIP_ID, "s1") } returns Result.failure(RuntimeException("error"))
        val viewModel = createViewModel()

        viewModel.onIntent(TripDetailUiIntent.OnMarkStopDepartedClicked("s1"))
        advanceUntilIdle()

        verify(exactly = 1) {
            analyticsTracker.track(match { it is AnalyticsEvent.OperationFailed && it.operation == AnalyticsEvent.OPERATION_MARK_DEPARTED })
        }
    }

    @Test
    fun `GIVEN calculateRoute fails WHEN OnCalculateRouteClicked is dispatched THEN OperationFailed is tracked`() = runTest(testDispatcher) {
        coEvery { calculateRouteUseCase(TRIP_ID, any()) } returns Result.failure(RuntimeException("error"))
        val viewModel = createViewModel()

        viewModel.onIntent(TripDetailUiIntent.OnCalculateRouteClicked)
        advanceUntilIdle()

        verify(exactly = 1) {
            analyticsTracker.track(match { it is AnalyticsEvent.OperationFailed && it.operation == AnalyticsEvent.OPERATION_CALCULATE_ROUTE })
        }
    }

    // --- Section 2: Total Distance ---

    @Test
    fun `GIVEN complete route with at least 2 stops WHEN both use cases emit THEN formattedTotalDistance equals formatDistance of summed leg distanceMetres`() = runTest(testDispatcher) {
        val stops = listOf(
            Stop("s0", TRIP_ID, "Start", 0.0, 0.0, 0, StopStatus.PENDING),
            Stop("s1", TRIP_ID, "End", 1.0, 1.0, 1, StopStatus.PENDING),
        )
        val legs = listOf(
            Leg("leg-1", TRIP_ID, "s0", "s1", 5000, 120, ""),
        )
        val viewModel = createViewModel(stops = stops, legs = legs)

        val expectedFormattedTotalDistance = formatDistance(
            legs.sumOf { it.distanceMetres },
            Locale.getDefault(),
            application.resources,
        )
        assertEquals(expectedFormattedTotalDistance, viewModel.uiState.value.formattedTotalDistance)
    }

    @Test
    fun `GIVEN two legs with distanceMetres 10000 and 2500 for 3 stops WHEN observed THEN formattedTotalDistance equals formatted 12500 metres in active locale`() = runTest(testDispatcher) {
        val stops = listOf(
            Stop("s0", TRIP_ID, "Start", 0.0, 0.0, 0, StopStatus.PENDING),
            Stop("s1", TRIP_ID, "Mid", 1.0, 1.0, 1, StopStatus.PENDING),
            Stop("s2", TRIP_ID, "End", 2.0, 2.0, 2, StopStatus.PENDING),
        )
        val legs = listOf(
            Leg("leg-1", TRIP_ID, "s0", "s1", 10000, 120, ""),
            Leg("leg-2", TRIP_ID, "s1", "s2", 2500, 60, ""),
        )
        val viewModel = createViewModel(stops = stops, legs = legs)

        val expectedFormattedTotalDistance = formatDistance(12500, Locale.getDefault(), application.resources)
        assertEquals(expectedFormattedTotalDistance, viewModel.uiState.value.formattedTotalDistance)
    }

    @Test
    fun `GIVEN at least 2 stops and no legs exist for the trip WHEN observed THEN uiState formattedTotalDistance is null`() = runTest(testDispatcher) {
        val stops = listOf(
            Stop("s0", TRIP_ID, "Start", 0.0, 0.0, 0, StopStatus.PENDING),
            Stop("s1", TRIP_ID, "End", 1.0, 1.0, 1, StopStatus.PENDING),
        )
        val viewModel = createViewModel(stops = stops, legs = emptyList())

        assertNull(viewModel.uiState.value.formattedTotalDistance)
    }

    @Test
    fun `GIVEN a leg count that is neither 0 nor stops-size minus 1 WHEN observed THEN uiState formattedTotalDistance is null`() = runTest(testDispatcher) {
        val stops = listOf(
            Stop("s0", TRIP_ID, "Start", 0.0, 0.0, 0, StopStatus.PENDING),
            Stop("s1", TRIP_ID, "Mid", 1.0, 1.0, 1, StopStatus.PENDING),
            Stop("s2", TRIP_ID, "End", 2.0, 2.0, 2, StopStatus.PENDING),
        )
        val legs = listOf(
            Leg("leg-1", TRIP_ID, "s0", "s1", 5000, 120, ""),
        )
        val viewModel = createViewModel(stops = stops, legs = legs)

        assertNull(viewModel.uiState.value.formattedTotalDistance)
    }

    @Test
    fun `GIVEN fewer than two placed stops WHEN observed THEN uiState formattedTotalDistance is null regardless of any legs present`() = runTest(testDispatcher) {
        val stops = listOf(
            Stop("s0", TRIP_ID, "Start", 0.0, 0.0, 0, StopStatus.PENDING),
        )
        val legs = listOf(
            Leg("leg-1", TRIP_ID, "s0", "s1", 5000, 120, ""),
        )
        val viewModel = createViewModel(stops = stops, legs = legs)

        assertNull(viewModel.uiState.value.formattedTotalDistance)
    }

    @Test
    fun `GIVEN a viewmodel with a complete route WHEN a stop mutation invalidates all legs THEN uiState formattedTotalDistance transitions to null`() = runTest(testDispatcher) {
        val stops = listOf(
            Stop("s0", TRIP_ID, "Start", 0.0, 0.0, 0, StopStatus.PENDING),
            Stop("s1", TRIP_ID, "End", 1.0, 1.0, 1, StopStatus.PENDING),
        )
        val completeLeg = Leg("leg-1", TRIP_ID, "s0", "s1", 5000, 120, "")
        val legsFlow = MutableSharedFlow<List<Leg>>(replay = 1)

        every { observeStopsUseCase(TRIP_ID) } returns flowOf(stops)
        every { observeLegsUseCase(TRIP_ID) } returns legsFlow

        legsFlow.emit(listOf(completeLeg))

        val viewModel = TripDetailViewModel(
            application, setStopUseCase, moveStopUseCase, editStopUseCase, removeStopUseCase,
            markStopDepartedUseCase, undoMarkStopDepartedUseCase, calculateRouteUseCase,
            observeStopsUseCase, observeLegsUseCase, searchPlacesUseCase, getPlaceDetailUseCase,
            placeSearchRepository, analyticsTracker, savedStateHandle,
        )

        assertNotNull(viewModel.uiState.value.formattedTotalDistance)

        legsFlow.emit(emptyList())

        assertNull(viewModel.uiState.value.formattedTotalDistance)
    }

    @Test
    fun `GIVEN a complete route under an imperial-unit locale WHEN observed THEN uiState formattedTotalDistance is expressed in miles consistent with formattedLegDistances for the same legs`() = runTest(testDispatcher) {
        val imperialLocale = Locale("en", "US")
        val savedLocale = Locale.getDefault()
        try {
            Locale.setDefault(imperialLocale)
            val stops = listOf(
                Stop("s0", TRIP_ID, "Start", 0.0, 0.0, 0, StopStatus.PENDING),
                Stop("s1", TRIP_ID, "End", 1.0, 1.0, 1, StopStatus.PENDING),
            )
            val legs = listOf(
                Leg("leg-1", TRIP_ID, "s0", "s1", 5000, 120, ""),
            )
            val viewModel = createViewModel(stops = stops, legs = legs)

            val expectedFormattedTotalDistance = "0.0 mi"
            val expectedFormattedLegDistance = "0.0 mi"
            assertEquals(expectedFormattedTotalDistance, viewModel.uiState.value.formattedTotalDistance)
            assertEquals(mapOf("s0" to expectedFormattedLegDistance), viewModel.uiState.value.formattedLegDistances)
        } finally {
            Locale.setDefault(savedLocale)
        }
    }

    // --- Section 2 (continued): Total Duration ---

    @Test
    fun `GIVEN complete route with at least 2 stops WHEN both use cases emit THEN formattedTotalDuration equals formatDuration of summed leg durationSeconds`() = runTest(testDispatcher) {
        val stops = listOf(
            Stop("s0", TRIP_ID, "Start", 0.0, 0.0, 0, StopStatus.PENDING),
            Stop("s1", TRIP_ID, "End", 1.0, 1.0, 1, StopStatus.PENDING),
        )
        val legs = listOf(
            Leg("leg-1", TRIP_ID, "s0", "s1", 5000, 120, ""),
        )
        val viewModel = createViewModel(stops = stops, legs = legs)

        val expectedFormattedTotalDuration = formatDuration(
            legs.sumOf { it.durationSeconds },
            application.resources,
        )
        assertEquals(expectedFormattedTotalDuration, viewModel.uiState.value.formattedTotalDuration)
    }

    @Test
    fun `GIVEN two legs with durationSeconds 600 and 900 for 3 stops WHEN observed THEN formattedTotalDuration equals formatDuration of 1500 seconds`() = runTest(testDispatcher) {
        val stops = listOf(
            Stop("s0", TRIP_ID, "Start", 0.0, 0.0, 0, StopStatus.PENDING),
            Stop("s1", TRIP_ID, "Mid", 1.0, 1.0, 1, StopStatus.PENDING),
            Stop("s2", TRIP_ID, "End", 2.0, 2.0, 2, StopStatus.PENDING),
        )
        val legs = listOf(
            Leg("leg-1", TRIP_ID, "s0", "s1", 1000, 600, ""),
            Leg("leg-2", TRIP_ID, "s1", "s2", 2000, 900, ""),
        )
        val viewModel = createViewModel(stops = stops, legs = legs)

        val expectedFormattedTotalDuration = formatDuration(1500, application.resources)
        assertEquals(expectedFormattedTotalDuration, viewModel.uiState.value.formattedTotalDuration)
    }

    @Test
    fun `GIVEN at least 2 stops and no legs exist for the trip WHEN observed THEN uiState formattedTotalDuration is null`() = runTest(testDispatcher) {
        val stops = listOf(
            Stop("s0", TRIP_ID, "Start", 0.0, 0.0, 0, StopStatus.PENDING),
            Stop("s1", TRIP_ID, "End", 1.0, 1.0, 1, StopStatus.PENDING),
        )
        val viewModel = createViewModel(stops = stops, legs = emptyList())

        assertNull(viewModel.uiState.value.formattedTotalDuration)
    }

    @Test
    fun `GIVEN a leg count that is neither 0 nor stops-size minus 1 WHEN observed THEN uiState formattedTotalDuration is null`() = runTest(testDispatcher) {
        val stops = listOf(
            Stop("s0", TRIP_ID, "Start", 0.0, 0.0, 0, StopStatus.PENDING),
            Stop("s1", TRIP_ID, "Mid", 1.0, 1.0, 1, StopStatus.PENDING),
            Stop("s2", TRIP_ID, "End", 2.0, 2.0, 2, StopStatus.PENDING),
        )
        val legs = listOf(
            Leg("leg-1", TRIP_ID, "s0", "s1", 5000, 120, ""),
        )
        val viewModel = createViewModel(stops = stops, legs = legs)

        assertNull(viewModel.uiState.value.formattedTotalDuration)
    }

    @Test
    fun `GIVEN fewer than two placed stops WHEN observed THEN uiState formattedTotalDuration is null regardless of any legs present`() = runTest(testDispatcher) {
        val stops = listOf(
            Stop("s0", TRIP_ID, "Start", 0.0, 0.0, 0, StopStatus.PENDING),
        )
        val legs = listOf(
            Leg("leg-1", TRIP_ID, "s0", "s1", 5000, 120, ""),
        )
        val viewModel = createViewModel(stops = stops, legs = legs)

        assertNull(viewModel.uiState.value.formattedTotalDuration)
    }

    @Test
    fun `GIVEN a viewmodel with a complete route WHEN a stop mutation invalidates all legs THEN uiState formattedTotalDuration transitions to null at the same time as formattedTotalDistance`() = runTest(testDispatcher) {
        val stops = listOf(
            Stop("s0", TRIP_ID, "Start", 0.0, 0.0, 0, StopStatus.PENDING),
            Stop("s1", TRIP_ID, "End", 1.0, 1.0, 1, StopStatus.PENDING),
        )
        val completeLeg = Leg("leg-1", TRIP_ID, "s0", "s1", 5000, 120, "")
        val legsFlow = MutableSharedFlow<List<Leg>>(replay = 1)

        every { observeStopsUseCase(TRIP_ID) } returns flowOf(stops)
        every { observeLegsUseCase(TRIP_ID) } returns legsFlow

        legsFlow.emit(listOf(completeLeg))

        val viewModel = TripDetailViewModel(
            application, setStopUseCase, moveStopUseCase, editStopUseCase, removeStopUseCase,
            markStopDepartedUseCase, undoMarkStopDepartedUseCase, calculateRouteUseCase,
            observeStopsUseCase, observeLegsUseCase, searchPlacesUseCase, getPlaceDetailUseCase,
            placeSearchRepository, analyticsTracker, savedStateHandle,
        )

        assertNotNull(viewModel.uiState.value.formattedTotalDuration)

        legsFlow.emit(emptyList())

        assertNull(viewModel.uiState.value.formattedTotalDuration)
        assertNull(viewModel.uiState.value.formattedTotalDistance)
    }

    @Test
    fun `GIVEN a complete route WHEN formattedTotalDistance is non-null THEN formattedTotalDuration is simultaneously non-null in the same uiState emission and vice versa when both are null`() = runTest(testDispatcher) {
        val stops = listOf(
            Stop("s0", TRIP_ID, "Start", 0.0, 0.0, 0, StopStatus.PENDING),
            Stop("s1", TRIP_ID, "End", 1.0, 1.0, 1, StopStatus.PENDING),
        )
        val legs = listOf(
            Leg("leg-1", TRIP_ID, "s0", "s1", 5000, 120, ""),
        )
        val completeViewModel = createViewModel(stops = stops, legs = legs)

        assertNotNull(completeViewModel.uiState.value.formattedTotalDistance)
        assertNotNull(completeViewModel.uiState.value.formattedTotalDuration)

        val incompleteViewModel = createViewModel(stops = stops, legs = emptyList())

        assertNull(incompleteViewModel.uiState.value.formattedTotalDistance)
        assertNull(incompleteViewModel.uiState.value.formattedTotalDuration)
    }

    // --- Section 3: Duration Wiring ---

    @Test
    fun `GIVEN a trip with legs of varying durationSeconds WHEN observeLegsUseCase emits those legs THEN uiState formattedLegDurations contains one formatted entry per leg keyed by fromStopId`() = runTest(testDispatcher) {
        val legs = listOf(
            Leg("leg-1", TRIP_ID, "stop-a", "stop-b", 1000, 540, ""),
            Leg("leg-2", TRIP_ID, "stop-b", "stop-c", 2000, 5400, ""),
        )
        val viewModel = createViewModel(legs = legs)

        val expectedFormattedLegDurations = mapOf(
            "stop-a" to "0 min",
            "stop-b" to "0h 0min",
        )
        assertEquals(expectedFormattedLegDurations, viewModel.uiState.value.formattedLegDurations)
    }
}
