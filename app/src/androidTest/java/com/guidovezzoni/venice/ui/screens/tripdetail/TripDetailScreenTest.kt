package com.guidovezzoni.venice.ui.screens.tripdetail

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.guidovezzoni.venice.domain.model.Stop
import com.guidovezzoni.venice.domain.model.StopStatus
import com.guidovezzoni.venice.ui.intent.TripDetailUiIntent
import com.guidovezzoni.venice.ui.state.DialogState
import com.guidovezzoni.venice.ui.state.RouteCalculationState
import com.guidovezzoni.venice.ui.state.TripDetailUiState
import com.guidovezzoni.venice.ui.theme.HeadingToVeniceTheme
import com.guidovezzoni.venice.ui.util.formatCoordinates
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

private const val TRIP_ID = "trip-1"

private val STARTING_POINT = Stop(
    id = "s0",
    tripId = TRIP_ID,
    placeName = "Rome, Italy",
    latitude = 41.9028,
    longitude = 12.4964,
    order = 0,
    status = StopStatus.PENDING,
)

private val INTERMEDIATE_1 = Stop(
    id = "s1",
    tripId = TRIP_ID,
    placeName = "Florence, Italy",
    latitude = 43.7696,
    longitude = 11.2558,
    order = 1,
    status = StopStatus.PENDING,
)

private val INTERMEDIATE_2 = Stop(
    id = "s2",
    tripId = TRIP_ID,
    placeName = "Nice, France",
    latitude = 43.7102,
    longitude = 7.2620,
    order = 2,
    status = StopStatus.PENDING,
)

private val DESTINATION = Stop(
    id = "s3",
    tripId = TRIP_ID,
    placeName = "Barcelona, Spain",
    latitude = 41.3851,
    longitude = 2.1734,
    order = 3,
    status = StopStatus.PENDING,
)

class TripDetailScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun setContent(
        uiState: TripDetailUiState = TripDetailUiState(),
        onIntent: (TripDetailUiIntent) -> Unit = {},
    ) {
        composeTestRule.setContent {
            HeadingToVeniceTheme {
                TripDetailScreen(
                    uiState = uiState,
                    onIntent = onIntent,
                )
            }
        }
    }

    // region Empty state

    @Test
    fun emptyState_showsSetStartingPointButton() {
        setContent()

        composeTestRule
            .onNodeWithContentDescription("Set starting point")
            .assertIsDisplayed()
    }

    @Test
    fun emptyState_showsSetDestinationButton() {
        setContent()

        composeTestRule
            .onNodeWithContentDescription("Set destination")
            .assertIsDisplayed()
    }

    @Test
    fun emptyState_addStopButtonIsNotDisplayed() {
        setContent()

        composeTestRule
            .onNodeWithContentDescription("Add stop")
            .assertDoesNotExist()
    }

    // endregion

    // region Starting point

    @Test
    fun withStartingPoint_showsPlaceName() {
        setContent(uiState = TripDetailUiState(startingPoint = STARTING_POINT))

        composeTestRule
            .onNodeWithText("Rome, Italy")
            .assertIsDisplayed()
    }

    @Test
    fun withStartingPoint_showsCoordinates() {
        val coordinatesText = formatCoordinates(STARTING_POINT.latitude, STARTING_POINT.longitude)
        setContent(
            uiState = TripDetailUiState(
                startingPoint = STARTING_POINT,
                formattedStopCoordinates = mapOf(STARTING_POINT.id to coordinatesText),
            ),
        )

        composeTestRule
            .onNodeWithText(coordinatesText)
            .assertIsDisplayed()
    }

    @Test
    fun withStartingPoint_showsDeleteButton() {
        setContent(uiState = TripDetailUiState(startingPoint = STARTING_POINT))

        composeTestRule
            .onNodeWithContentDescription("Remove stop")
            .assertIsDisplayed()
    }

    @Test
    fun withStartingPoint_clickingCard_firesOnSetStartingPointClicked() {
        val intents = mutableListOf<TripDetailUiIntent>()
        setContent(
            uiState = TripDetailUiState(startingPoint = STARTING_POINT),
            onIntent = { intents.add(it) },
        )

        composeTestRule
            .onNodeWithContentDescription("Change starting point")
            .performClick()

        assertTrue(intents.any { it is TripDetailUiIntent.OnSetStartingPointClicked })
    }

    @Test
    fun withStartingPoint_clickingDelete_firesOnRemoveStopClicked() {
        val intents = mutableListOf<TripDetailUiIntent>()
        setContent(
            uiState = TripDetailUiState(startingPoint = STARTING_POINT),
            onIntent = { intents.add(it) },
        )

        composeTestRule
            .onNodeWithContentDescription("Remove stop")
            .performClick()

        val removeIntent = intents.filterIsInstance<TripDetailUiIntent.OnRemoveStopClicked>().first()
        assertEquals(STARTING_POINT, removeIntent.stop)
    }

    @Test
    fun emptyStartingPoint_clickingSetButton_firesOnSetStartingPointClicked() {
        val intents = mutableListOf<TripDetailUiIntent>()
        setContent(onIntent = { intents.add(it) })

        composeTestRule
            .onNodeWithContentDescription("Set starting point")
            .performClick()

        assertTrue(intents.any { it is TripDetailUiIntent.OnSetStartingPointClicked })
    }

    // endregion

    // region Destination

    @Test
    fun withDestination_showsPlaceName() {
        setContent(
            uiState = TripDetailUiState(
                destination = DESTINATION.copy(order = 1),
            ),
        )

        composeTestRule
            .onNodeWithText("Barcelona, Spain")
            .assertIsDisplayed()
    }

    @Test
    fun emptyDestination_clickingSetButton_firesOnSetDestinationClicked() {
        val intents = mutableListOf<TripDetailUiIntent>()
        setContent(onIntent = { intents.add(it) })

        composeTestRule
            .onNodeWithContentDescription("Set destination")
            .performClick()

        assertTrue(intents.any { it is TripDetailUiIntent.OnSetDestinationClicked })
    }

    @Test
    fun withDestination_clickingCard_firesOnSetDestinationClicked() {
        val intents = mutableListOf<TripDetailUiIntent>()
        setContent(
            uiState = TripDetailUiState(
                destination = DESTINATION.copy(order = 1),
            ),
            onIntent = { intents.add(it) },
        )

        composeTestRule
            .onNodeWithContentDescription("Change destination")
            .performClick()

        assertTrue(intents.any { it is TripDetailUiIntent.OnSetDestinationClicked })
    }

    // endregion

    // region Intermediate stops

    @Test
    fun withIntermediateStops_showsAllStopNames() {
        setContent(
            uiState = TripDetailUiState(
                startingPoint = STARTING_POINT,
                intermediateStops = listOf(INTERMEDIATE_1, INTERMEDIATE_2),
                destination = DESTINATION,
                canAddMoreStops = true,
            ),
        )

        composeTestRule.onNodeWithText("Florence, Italy").assertExists()
        composeTestRule.onNodeWithText("Nice, France").assertExists()
    }

    @Test
    fun withIntermediateStops_clickingStop_firesOnEditStopClicked() {
        val intents = mutableListOf<TripDetailUiIntent>()
        setContent(
            uiState = TripDetailUiState(
                startingPoint = STARTING_POINT,
                intermediateStops = listOf(INTERMEDIATE_1),
                destination = DESTINATION.copy(order = 2),
                canAddMoreStops = true,
            ),
            onIntent = { intents.add(it) },
        )

        composeTestRule
            .onNodeWithContentDescription("Change stop")
            .performClick()

        val editIntent = intents.filterIsInstance<TripDetailUiIntent.OnEditStopClicked>().first()
        assertEquals(INTERMEDIATE_1, editIntent.stop)
    }

    @Test
    fun withTwoIntermediateStops_showsMoveUpAndDownButtons() {
        setContent(
            uiState = TripDetailUiState(
                startingPoint = STARTING_POINT,
                intermediateStops = listOf(INTERMEDIATE_1, INTERMEDIATE_2),
                destination = DESTINATION,
                canAddMoreStops = true,
            ),
        )

        composeTestRule.onNodeWithContentDescription("Move down").assertExists()
        composeTestRule.onNodeWithContentDescription("Move up").assertExists()
    }

    @Test
    fun withOneIntermediateStop_moveButtonsAreNotShown() {
        setContent(
            uiState = TripDetailUiState(
                startingPoint = STARTING_POINT,
                intermediateStops = listOf(INTERMEDIATE_1),
                destination = DESTINATION.copy(order = 2),
                canAddMoreStops = true,
            ),
        )

        composeTestRule.onNodeWithContentDescription("Move up").assertDoesNotExist()
        composeTestRule.onNodeWithContentDescription("Move down").assertDoesNotExist()
    }

    // endregion

    // region Add stop button

    @Test
    fun canAddMoreStops_showsAddStopButton() {
        setContent(
            uiState = TripDetailUiState(
                startingPoint = STARTING_POINT,
                destination = DESTINATION.copy(order = 1),
                canAddMoreStops = true,
            ),
        )

        composeTestRule
            .onNodeWithContentDescription("Add stop")
            .assertIsDisplayed()
    }

    @Test
    fun cannotAddMoreStops_addStopButtonNotShown() {
        setContent(
            uiState = TripDetailUiState(
                startingPoint = STARTING_POINT,
                destination = DESTINATION.copy(order = 1),
                canAddMoreStops = false,
            ),
        )

        composeTestRule
            .onNodeWithContentDescription("Add stop")
            .assertDoesNotExist()
    }

    @Test
    fun clickingAddStopButton_firesOnAddStopClicked() {
        val intents = mutableListOf<TripDetailUiIntent>()
        setContent(
            uiState = TripDetailUiState(
                startingPoint = STARTING_POINT,
                destination = DESTINATION.copy(order = 1),
                canAddMoreStops = true,
            ),
            onIntent = { intents.add(it) },
        )

        composeTestRule
            .onNodeWithContentDescription("Add stop")
            .performClick()

        assertTrue(intents.any { it is TripDetailUiIntent.OnAddStopClicked })
    }

    // endregion

    // region Set starting point dialog

    @Test
    fun startingPointDialogVisible_showsDialogTitle() {
        setContent(
            uiState = TripDetailUiState(dialogState = DialogState.SetStartingPoint),
        )

        composeTestRule
            .onAllNodesWithText("Set starting point")[0]
            .assertIsDisplayed()
    }

    @Test
    fun startingPointDialogVisible_showsConfirmAndCancelButtons() {
        setContent(
            uiState = TripDetailUiState(dialogState = DialogState.SetStartingPoint),
        )

        composeTestRule.onNodeWithText("Confirm").assertIsDisplayed()
        composeTestRule.onNodeWithText("Cancel").assertIsDisplayed()
    }

    @Test
    fun startingPointDialog_clickingCancel_firesOnDismissStartingPointDialog() {
        val intents = mutableListOf<TripDetailUiIntent>()
        setContent(
            uiState = TripDetailUiState(dialogState = DialogState.SetStartingPoint),
            onIntent = { intents.add(it) },
        )

        composeTestRule
            .onNodeWithText("Cancel")
            .performClick()

        assertTrue(intents.any { it is TripDetailUiIntent.OnDismissStartingPointDialog })
    }

    // endregion

    // region Set destination dialog

    @Test
    fun destinationDialogVisible_showsDialogTitle() {
        setContent(
            uiState = TripDetailUiState(dialogState = DialogState.SetDestination),
        )

        composeTestRule
            .onAllNodesWithText("Set destination")[0]
            .assertIsDisplayed()
    }

    @Test
    fun destinationDialog_clickingCancel_firesOnDismissDestinationDialog() {
        val intents = mutableListOf<TripDetailUiIntent>()
        setContent(
            uiState = TripDetailUiState(dialogState = DialogState.SetDestination),
            onIntent = { intents.add(it) },
        )

        composeTestRule
            .onNodeWithText("Cancel")
            .performClick()

        assertTrue(intents.any { it is TripDetailUiIntent.OnDismissDestinationDialog })
    }

    // endregion

    // region Add stop dialog

    @Test
    fun addStopDialogVisible_showsDialogTitle() {
        setContent(
            uiState = TripDetailUiState(dialogState = DialogState.AddStop),
        )

        composeTestRule
            .onNodeWithText("Add intermediate stop")
            .assertIsDisplayed()
    }

    @Test
    fun addStopDialog_clickingCancel_firesOnDismissAddStopDialog() {
        val intents = mutableListOf<TripDetailUiIntent>()
        setContent(
            uiState = TripDetailUiState(dialogState = DialogState.AddStop),
            onIntent = { intents.add(it) },
        )

        composeTestRule
            .onNodeWithText("Cancel")
            .performClick()

        assertTrue(intents.any { it is TripDetailUiIntent.OnDismissAddStopDialog })
    }

    // endregion

    // region Edit stop dialog

    @Test
    fun editStopDialogVisible_showsDialogTitle() {
        setContent(
            uiState = TripDetailUiState(
                dialogState = DialogState.EditStop(INTERMEDIATE_1),
            ),
        )

        composeTestRule
            .onNodeWithText("Edit stop")
            .assertIsDisplayed()
    }

    @Test
    fun editStopDialogVisible_prePopulatesFields() {
        setContent(
            uiState = TripDetailUiState(
                dialogState = DialogState.EditStop(INTERMEDIATE_1),
            ),
        )

        composeTestRule.onNodeWithText("Florence, Italy").assertIsDisplayed()
        composeTestRule.onNodeWithText("43.7696").assertIsDisplayed()
        composeTestRule.onNodeWithText("11.2558").assertIsDisplayed()
    }

    @Test
    fun editStopDialog_clickingCancel_firesOnDismissEditStopDialog() {
        val intents = mutableListOf<TripDetailUiIntent>()
        setContent(
            uiState = TripDetailUiState(
                dialogState = DialogState.EditStop(INTERMEDIATE_1),
            ),
            onIntent = { intents.add(it) },
        )

        composeTestRule
            .onNodeWithText("Cancel")
            .performClick()

        assertTrue(intents.any { it is TripDetailUiIntent.OnDismissEditStopDialog })
    }

    // endregion

    // region Remove stop dialog

    @Test
    fun removeStopDialogVisible_showsConfirmationMessage() {
        setContent(
            uiState = TripDetailUiState(
                dialogState = DialogState.RemoveStop(INTERMEDIATE_1),
            ),
        )

        composeTestRule
            .onNodeWithText("Remove stop?")
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("Are you sure you want to remove \"Florence, Italy\"?")
            .assertIsDisplayed()
    }

    @Test
    fun removeStopDialog_showsRemoveAndCancelButtons() {
        setContent(
            uiState = TripDetailUiState(
                dialogState = DialogState.RemoveStop(INTERMEDIATE_1),
            ),
        )

        composeTestRule.onNodeWithText("Remove").assertIsDisplayed()
        composeTestRule.onNodeWithText("Cancel").assertIsDisplayed()
    }

    @Test
    fun removeStopDialog_clickingRemove_firesOnRemoveStopConfirmed() {
        val intents = mutableListOf<TripDetailUiIntent>()
        setContent(
            uiState = TripDetailUiState(
                dialogState = DialogState.RemoveStop(INTERMEDIATE_1),
            ),
            onIntent = { intents.add(it) },
        )

        composeTestRule
            .onNodeWithText("Remove")
            .performClick()

        assertTrue(intents.any { it is TripDetailUiIntent.OnRemoveStopConfirmed })
    }

    @Test
    fun removeStopDialog_clickingCancel_firesOnDismissRemoveStopDialog() {
        val intents = mutableListOf<TripDetailUiIntent>()
        setContent(
            uiState = TripDetailUiState(
                dialogState = DialogState.RemoveStop(INTERMEDIATE_1),
            ),
            onIntent = { intents.add(it) },
        )

        composeTestRule
            .onNodeWithText("Cancel")
            .performClick()

        assertTrue(intents.any { it is TripDetailUiIntent.OnDismissRemoveStopDialog })
    }

    @Test
    fun removeStopDialogNotVisible_dialogIsNotShown() {
        setContent(
            uiState = TripDetailUiState(),
        )

        composeTestRule
            .onNodeWithText("Remove stop?")
            .assertDoesNotExist()
    }

    // endregion

    // region Title

    @Test
    fun screen_showsTitle() {
        setContent()

        composeTestRule
            .onNodeWithText("Trip Detail")
            .assertIsDisplayed()
    }

    // endregion

    // region Stop progress — mark departed / undo

    @Test
    fun currentStop_showsMarkAsDepartedButton() {
        setContent(
            uiState = TripDetailUiState(
                startingPoint = STARTING_POINT,
                intermediateStops = listOf(INTERMEDIATE_1),
                destination = DESTINATION,
                canAddMoreStops = true,
            ),
        )

        composeTestRule
            .onNodeWithText("Mark as departed")
            .assertIsDisplayed()
    }

    @Test
    fun departedStop_showsUndoDepartureButton() {
        setContent(
            uiState = TripDetailUiState(
                startingPoint = STARTING_POINT.copy(status = StopStatus.VISITED),
                intermediateStops = listOf(INTERMEDIATE_1),
                destination = DESTINATION,
                canAddMoreStops = true,
            ),
        )

        composeTestRule
            .onNodeWithText("Undo departure")
            .assertIsDisplayed()
    }

    @Test
    fun upcomingStop_doesNotShowMarkOrUndoButtons() {
        setContent(
            uiState = TripDetailUiState(
                startingPoint = STARTING_POINT,
                intermediateStops = listOf(INTERMEDIATE_1, INTERMEDIATE_2),
                destination = DESTINATION,
                canAddMoreStops = true,
            ),
        )

        composeTestRule
            .onAllNodesWithText("Undo departure")
            .fetchSemanticsNodes()
            .let { assertTrue(it.isEmpty()) }
    }

    @Test
    fun clickingMarkAsDeparted_firesOnMarkStopDepartedClicked() {
        val intents = mutableListOf<TripDetailUiIntent>()
        setContent(
            uiState = TripDetailUiState(
                startingPoint = STARTING_POINT,
                destination = DESTINATION.copy(order = 1),
                canAddMoreStops = true,
            ),
            onIntent = { intents.add(it) },
        )

        composeTestRule
            .onNodeWithText("Mark as departed")
            .performClick()

        val intent = intents.filterIsInstance<TripDetailUiIntent.OnMarkStopDepartedClicked>().first()
        assertEquals(STARTING_POINT.id, intent.stopId)
    }

    @Test
    fun clickingUndoDeparture_firesOnUndoMarkStopDepartedClicked() {
        val intents = mutableListOf<TripDetailUiIntent>()
        setContent(
            uiState = TripDetailUiState(
                startingPoint = STARTING_POINT.copy(status = StopStatus.VISITED),
                intermediateStops = listOf(INTERMEDIATE_1),
                destination = DESTINATION,
                canAddMoreStops = true,
            ),
            onIntent = { intents.add(it) },
        )

        composeTestRule
            .onNodeWithText("Undo departure")
            .performClick()

        val intent = intents.filterIsInstance<TripDetailUiIntent.OnUndoMarkStopDepartedClicked>().first()
        assertEquals(STARTING_POINT.id, intent.stopId)
    }

    // endregion

    // region Progress summary

    @Test
    fun withStopsAndOneDeparted_showsProgressSummary() {
        setContent(
            uiState = TripDetailUiState(
                startingPoint = STARTING_POINT.copy(status = StopStatus.VISITED),
                intermediateStops = listOf(INTERMEDIATE_1, INTERMEDIATE_2),
                destination = DESTINATION,
                canAddMoreStops = true,
            ),
        )

        composeTestRule
            .onNodeWithText("1 of 4 stops completed")
            .assertIsDisplayed()
    }

    @Test
    fun withNoStops_progressSummaryNotDisplayed() {
        setContent(uiState = TripDetailUiState())

        composeTestRule
            .onNodeWithText("0 of 0 stops completed")
            .assertDoesNotExist()
    }

    // endregion

    // region Calculate route

    @Test
    fun calculateRoute_withTwoOrMoreStops_buttonIsVisible() {
        setContent(
            uiState = TripDetailUiState(
                tripId = TRIP_ID,
                startingPoint = STARTING_POINT,
                destination = DESTINATION,
            ),
        )
        composeTestRule.onNodeWithText("Calculate route").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun calculateRoute_withFewerThanTwoStops_buttonIsNotVisible() {
        setContent(
            uiState = TripDetailUiState(
                tripId = TRIP_ID,
                startingPoint = STARTING_POINT,
            ),
        )
        composeTestRule.onNodeWithText("Calculate route").assertDoesNotExist()
    }

    @Test
    fun calculateRoute_whileCalculating_buttonIsDisabled() {
        setContent(
            uiState = TripDetailUiState(
                tripId = TRIP_ID,
                startingPoint = STARTING_POINT,
                destination = DESTINATION,
                routeCalculationState = RouteCalculationState.Calculating,
            ),
        )
        composeTestRule.onNodeWithText("Calculating route…")
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Calculating route…").assertIsNotEnabled()
    }

    @Test
    fun calculateRoute_withRouteError_errorMessageIsDisplayed() {
        val errorMessage = "Network error"
        setContent(
            uiState = TripDetailUiState(
                tripId = TRIP_ID,
                startingPoint = STARTING_POINT,
                destination = DESTINATION,
                routeCalculationState = RouteCalculationState.Error(errorMessage),
            ),
        )
        composeTestRule.onNodeWithText(errorMessage).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun calculateRoute_onButtonTap_intentIsDispatched() {
        val capturedIntents = mutableListOf<TripDetailUiIntent>()
        setContent(
            uiState = TripDetailUiState(
                tripId = TRIP_ID,
                startingPoint = STARTING_POINT,
                destination = DESTINATION,
            ),
            onIntent = { capturedIntents.add(it) },
        )
        composeTestRule.onNodeWithText("Calculate route").performScrollTo().performClick()
        assertTrue(capturedIntents.any { it is TripDetailUiIntent.OnCalculateRouteClicked })
    }

    // endregion

    // region Total distance summary

    @Test
    fun withNonNullFormattedTotalDistance_showsTotalDistanceLabelAndValue() {
        setContent(
            uiState = TripDetailUiState(
                tripId = TRIP_ID,
                startingPoint = STARTING_POINT,
                destination = DESTINATION,
                formattedTotalDistance = "12.5 km",
            ),
        )

        composeTestRule.onNodeWithText("Total distance").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("12.5 km").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun withNullFormattedTotalDistanceAndNonNullDuration_showsTotalDistanceUnavailableText() {
        setContent(
            uiState = TripDetailUiState(
                tripId = TRIP_ID,
                startingPoint = STARTING_POINT,
                destination = DESTINATION,
                formattedTotalDistance = null,
                formattedTotalDuration = "25 min",
            ),
        )

        composeTestRule.onNodeWithText("Total distance unavailable").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun withBothNonNullFormattedTotals_showsBothTotalDistanceAndTotalDurationLabelsAndValues() {
        setContent(
            uiState = TripDetailUiState(
                tripId = TRIP_ID,
                startingPoint = STARTING_POINT,
                destination = DESTINATION,
                formattedTotalDistance = "12.5 km",
                formattedTotalDuration = "25 min",
            ),
        )

        composeTestRule.onNodeWithText("Total distance").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("12.5 km").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Est. driving time").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("25 min").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun withBothNullFormattedTotals_showsCombinedTotalsUnavailableText() {
        setContent(
            uiState = TripDetailUiState(
                tripId = TRIP_ID,
                startingPoint = STARTING_POINT,
                destination = DESTINATION,
                formattedTotalDistance = null,
                formattedTotalDuration = null,
            ),
        )

        composeTestRule.onNodeWithText("Trip totals unavailable").performScrollTo().assertIsDisplayed()
    }

    // endregion

    // region Loading state

    @Test
    fun loadingState_withMarkDepartedButton_actionButtonsAreDisabled() {
        setContent(
            uiState = TripDetailUiState(
                tripId = TRIP_ID,
                startingPoint = STARTING_POINT.copy(status = StopStatus.PENDING),
                intermediateStops = listOf(INTERMEDIATE_1),
                destination = DESTINATION,
                canAddMoreStops = true,
                isLoading = true,
            ),
        )

        composeTestRule
            .onNodeWithText("Mark as departed")
            .assertIsNotEnabled()
    }

    @Test
    fun loadingState_addStopButtonIsDisabled() {
        setContent(
            uiState = TripDetailUiState(
                tripId = TRIP_ID,
                startingPoint = STARTING_POINT,
                destination = DESTINATION,
                canAddMoreStops = true,
                isLoading = true,
            ),
        )

        composeTestRule
            .onNodeWithContentDescription("Add stop")
            .assertIsNotEnabled()
    }

    @Test
    fun loadingState_progressIndicatorIsVisible() {
        setContent(
            uiState = TripDetailUiState(
                tripId = TRIP_ID,
                startingPoint = STARTING_POINT,
                destination = DESTINATION,
                isLoading = true,
            ),
        )

        composeTestRule
            .onNodeWithContentDescription("Loading")
            .assertExists()
    }

    // endregion
}
