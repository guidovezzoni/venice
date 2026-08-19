package com.guidovezzoni.venice.ui.viewmodel

import com.guidovezzoni.venice.core.analytics.AnalyticsClient
import com.guidovezzoni.venice.core.analytics.AnalyticsErrorType
import com.guidovezzoni.venice.core.analytics.AnalyticsEvent
import com.guidovezzoni.venice.core.analytics.AnalyticsOperation
import com.guidovezzoni.venice.core.analytics.AnalyticsScreen
import com.guidovezzoni.venice.core.analytics.AnalyticsUserProperty
import com.guidovezzoni.venice.core.analytics.CountBand
import com.guidovezzoni.venice.domain.model.Trip
import com.guidovezzoni.venice.domain.repository.TripRepository
import com.guidovezzoni.venice.domain.usecase.CreateTripUseCase
import com.guidovezzoni.venice.ui.effect.TripListUiEffect
import com.guidovezzoni.venice.ui.intent.TripListUiIntent
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class TripListViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var createTripUseCase: CreateTripUseCase
    private lateinit var analyticsClient: AnalyticsClient
    private lateinit var tripRepository: TripRepository
    private lateinit var viewModel: TripListViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        createTripUseCase = mockk()
        analyticsClient = mockk(relaxed = true)
        tripRepository = mockk()
        viewModel = createViewModel()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(trips: List<Trip> = emptyList()): TripListViewModel {
        every { tripRepository.observeTrips() } returns flowOf(trips)
        return TripListViewModel(createTripUseCase, analyticsClient, tripRepository)
    }

    @Test
    fun `GIVEN initial state WHEN OnCreateTripClicked intent is dispatched THEN isCreateDialogVisible becomes true`() = runTest(testDispatcher) {
        viewModel.onIntent(TripListUiIntent.OnCreateTripClicked)

        assertTrue(viewModel.uiState.value.isCreateDialogVisible)
    }

    @Test
    fun `GIVEN dialog visible WHEN OnDismissCreateDialog intent is dispatched THEN isCreateDialogVisible becomes false`() = runTest(testDispatcher) {
        viewModel.onIntent(TripListUiIntent.OnCreateTripClicked)
        viewModel.onIntent(TripListUiIntent.OnDismissCreateDialog)

        assertFalse(viewModel.uiState.value.isCreateDialogVisible)
    }

    @Test
    fun `GIVEN a valid name WHEN ConfirmCreateTrip intent is dispatched THEN NavigateToTripDetail effect is emitted`() = runTest(testDispatcher) {
        val trip = Trip(id = "trip-id", name = "Summer Drive", createdAt = 0L, updatedAt = 0L)
        coEvery { createTripUseCase(any()) } returns Result.success(trip)

        val effects = mutableListOf<TripListUiEffect>()
        val collectJob = launch {
            viewModel.uiEffect.collect { effects.add(it) }
        }

        viewModel.onIntent(TripListUiIntent.OnTripNameChanged("Summer Drive"))
        viewModel.onIntent(TripListUiIntent.ConfirmCreateTrip)
        advanceUntilIdle()

        val expectedEffect = TripListUiEffect.NavigateToTripDetail("trip-id")
        assertEquals(expectedEffect, effects.first())
        collectJob.cancel()
    }

    @Test
    fun `GIVEN an empty trip list WHEN ViewModel is initialised THEN uiState trips is empty`() = runTest(testDispatcher) {
        val expectedTrips = emptyList<Trip>()
        assertEquals(expectedTrips, viewModel.uiState.value.trips)
    }

    @Test
    fun `GIVEN a trip id WHEN OnTripClicked is dispatched THEN NavigateToTripDetail effect is emitted with that tripId`() = runTest(testDispatcher) {
        val effects = mutableListOf<TripListUiEffect>()
        val collectJob = launch {
            viewModel.uiEffect.collect { effects.add(it) }
        }

        viewModel.onIntent(TripListUiIntent.OnTripClicked("trip-123"))

        val expectedEffect = TripListUiEffect.NavigateToTripDetail("trip-123")
        assertEquals(expectedEffect, effects.first())
        collectJob.cancel()
    }

    @Test
    fun `GIVEN ViewModel initialised WHEN init completes THEN ScreenViewed is tracked`() = runTest(testDispatcher) {
        verify(exactly = 1) {
            analyticsClient.logEvent(match { it is AnalyticsEvent.ScreenViewed && it.screen == AnalyticsScreen.TRIP_LIST })
        }
    }

    @Test
    fun givenObservedTripListEmits3Trips_whenInitCompletes_thenTripCountBandRange2To5IsSet() = runTest(testDispatcher) {
        val trips = listOf(
            Trip(id = "1", name = "Trip 1", createdAt = 0L, updatedAt = 0L),
            Trip(id = "2", name = "Trip 2", createdAt = 0L, updatedAt = 0L),
            Trip(id = "3", name = "Trip 3", createdAt = 0L, updatedAt = 0L),
        )
        createViewModel(trips = trips)

        val expectedProperty = AnalyticsUserProperty.TripCountBand(band = CountBand.RANGE_2_5)
        verify(exactly = 1) {
            analyticsClient.setUserProperty(expectedProperty)
        }
    }

    @Test
    fun `GIVEN a valid name WHEN ConfirmCreateTrip succeeds THEN TripCreated is tracked`() = runTest(testDispatcher) {
        val trip = Trip(id = "trip-id", name = "Summer Drive", createdAt = 0L, updatedAt = 0L)
        coEvery { createTripUseCase(any()) } returns Result.success(trip)

        viewModel.onIntent(TripListUiIntent.OnTripNameChanged("Summer Drive"))
        viewModel.onIntent(TripListUiIntent.ConfirmCreateTrip)
        advanceUntilIdle()

        verify(exactly = 1) {
            analyticsClient.logEvent(match { it is AnalyticsEvent.TripCreated })
        }
    }

    @Test
    fun given0TripsBeforeCreation_whenConfirmCreateTripSucceeds_thenTripCreatedIsFirstTripTrue() = runTest(testDispatcher) {
        val newTrip = Trip(id = "new", name = "New Trip", createdAt = 0L, updatedAt = 0L)
        coEvery { createTripUseCase(any()) } returns Result.success(newTrip)

        viewModel.onIntent(TripListUiIntent.OnTripNameChanged("New Trip"))
        viewModel.onIntent(TripListUiIntent.ConfirmCreateTrip)
        advanceUntilIdle()

        val expectedEvent = AnalyticsEvent.TripCreated(isFirstTrip = true)
        verify(exactly = 1) {
            analyticsClient.logEvent(expectedEvent)
        }
    }

    @Test
    fun given1TripBeforeCreation_whenConfirmCreateTripSucceeds_thenTripCreatedIsFirstTripFalseAndTripCountBandRange2To5IsSet() = runTest(testDispatcher) {
        val existingTrip = Trip(id = "existing", name = "Existing", createdAt = 0L, updatedAt = 0L)
        val newTrip = Trip(id = "new", name = "New Trip", createdAt = 0L, updatedAt = 0L)
        val localViewModel = createViewModel(trips = listOf(existingTrip))
        coEvery { createTripUseCase(any()) } returns Result.success(newTrip)

        localViewModel.onIntent(TripListUiIntent.OnTripNameChanged("New Trip"))
        localViewModel.onIntent(TripListUiIntent.ConfirmCreateTrip)
        advanceUntilIdle()

        val expectedEvent = AnalyticsEvent.TripCreated(isFirstTrip = false)
        val expectedProperty = AnalyticsUserProperty.TripCountBand(band = CountBand.RANGE_2_5)
        verify(exactly = 1) {
            analyticsClient.logEvent(expectedEvent)
        }
        verify(exactly = 1) {
            analyticsClient.setUserProperty(expectedProperty)
        }
    }

    @Test
    fun givenOnTripClickedIsProcessed_whenAnalyticsCallsAreInspected_thenNoTripOpenedEventIsLogged() = runTest(testDispatcher) {
        viewModel.onIntent(TripListUiIntent.OnTripClicked("trip-123"))

        verify(exactly = 0) {
            analyticsClient.logEvent(match { it is AnalyticsEvent.TripOpened })
        }
    }

    @Test
    fun givenCreateTripUseCaseFailsWithIoException_whenConfirmCreateTrip_thenTrackFailureIsCalledWithNetworkError() = runTest(testDispatcher) {
        val exception = IOException("network error")
        coEvery { createTripUseCase(any()) } returns Result.failure(exception)

        viewModel.onIntent(TripListUiIntent.OnTripNameChanged("Trip"))
        viewModel.onIntent(TripListUiIntent.ConfirmCreateTrip)
        advanceUntilIdle()

        val expectedOperation = AnalyticsOperation.CREATE_TRIP
        val expectedErrorType = AnalyticsErrorType.NETWORK
        verify(exactly = 1) {
            analyticsClient.trackFailure(expectedOperation, expectedErrorType, exception)
        }
    }

    @Test
    fun `GIVEN use case failure WHEN ConfirmCreateTrip intent is dispatched THEN ShowError effect is emitted`() = runTest(testDispatcher) {
        coEvery { createTripUseCase(any()) } returns Result.failure(RuntimeException("error"))

        val effects = mutableListOf<TripListUiEffect>()
        val collectJob = launch {
            viewModel.uiEffect.collect { effects.add(it) }
        }

        viewModel.onIntent(TripListUiIntent.OnTripNameChanged("Trip"))
        viewModel.onIntent(TripListUiIntent.ConfirmCreateTrip)
        advanceUntilIdle()

        assertTrue(effects.any { it is TripListUiEffect.ShowError })
        collectJob.cancel()
    }
}
