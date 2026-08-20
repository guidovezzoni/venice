package com.guidovezzoni.venice.ui.screens.triplist

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.guidovezzoni.venice.domain.model.Trip
import com.guidovezzoni.venice.ui.state.TripListUiState
import com.guidovezzoni.venice.ui.theme.HeadingToVeniceTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

private val TRIP_1 = Trip(
    id = "trip-1",
    name = "Summer Drive",
    createdAt = 0L,
    updatedAt = 0L,
    stopCount = 3,
)

private val TRIP_2 = Trip(
    id = "trip-2",
    name = "Coast Trip",
    createdAt = 0L,
    updatedAt = 0L,
    stopCount = 0,
)

class TripListScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun setContent(
        uiState: TripListUiState = TripListUiState(),
        onTripClick: (String) -> Unit = {},
        onCreateTripClick: () -> Unit = {},
        onNameChange: (String) -> Unit = {},
        onConfirmCreateTrip: () -> Unit = {},
        onDismissCreateDialog: () -> Unit = {},
    ) {
        composeTestRule.setContent {
            HeadingToVeniceTheme {
                TripListScreen(
                    uiState = uiState,
                    onTripClick = onTripClick,
                    onCreateTripClick = onCreateTripClick,
                    onNameChange = onNameChange,
                    onConfirmCreateTrip = onConfirmCreateTrip,
                    onDismissCreateDialog = onDismissCreateDialog,
                )
            }
        }
    }

    // region Title and FAB

    @Test
    fun screen_showsAppName() {
        setContent()

        composeTestRule
            .onNodeWithText("Heading to Venice")
            .assertIsDisplayed()
    }

    @Test
    fun screen_showsCreateTripFab() {
        setContent()

        composeTestRule
            .onNodeWithText("Create roadtrip", useUnmergedTree = true)
            .assertIsDisplayed()
    }

    @Test
    fun clickingFab_firesOnCreateTripClicked() {
        var clicked = false
        setContent(onCreateTripClick = { clicked = true })

        composeTestRule
            .onNodeWithText("Create roadtrip", useUnmergedTree = true)
            .performClick()

        assertTrue(clicked)
    }

    // endregion

    // region Empty state

    @Test
    fun emptyState_showsEmptyTitle() {
        setContent()

        composeTestRule
            .onNodeWithText("No trips yet")
            .assertIsDisplayed()
    }

    @Test
    fun emptyState_showsCreateFirstTripButton() {
        setContent()

        composeTestRule
            .onNodeWithText("Create your first trip")
            .assertIsDisplayed()
    }

    @Test
    fun emptyState_clickingCreateButton_firesOnCreateTripClicked() {
        var clicked = false
        setContent(onCreateTripClick = { clicked = true })

        composeTestRule
            .onNodeWithText("Create your first trip")
            .performClick()

        assertTrue(clicked)
    }

    // endregion

    // region Trip list

    @Test
    fun withTrips_showsTripNames() {
        setContent(uiState = TripListUiState(trips = listOf(TRIP_1, TRIP_2)))

        composeTestRule.onNodeWithText("Summer Drive").assertIsDisplayed()
        composeTestRule.onNodeWithText("Coast Trip").assertIsDisplayed()
    }

    @Test
    fun withTrips_showsStopCounts() {
        setContent(uiState = TripListUiState(trips = listOf(TRIP_1, TRIP_2)))

        composeTestRule.onNodeWithText("3 stops").assertIsDisplayed()
        composeTestRule.onNodeWithText("0 stops").assertIsDisplayed()
    }

    @Test
    fun withTrips_emptyStateIsNotShown() {
        setContent(uiState = TripListUiState(trips = listOf(TRIP_1)))

        composeTestRule
            .onNodeWithText("No trips yet")
            .assertDoesNotExist()
    }

    @Test
    fun clickingTrip_firesOnTripClicked() {
        val clickedIds = mutableListOf<String>()
        setContent(
            uiState = TripListUiState(trips = listOf(TRIP_1, TRIP_2)),
            onTripClick = { clickedIds.add(it) },
        )

        composeTestRule
            .onNodeWithText("Summer Drive")
            .performClick()

        assertEquals(listOf("trip-1"), clickedIds)
    }

    // endregion

    // region Create trip dialog

    @Test
    fun createDialogVisible_showsDialogTitle() {
        setContent(
            uiState = TripListUiState(
                isCreateDialogVisible = true,
                tripNameInput = "",
            ),
        )

        composeTestRule
            .onNodeWithText("New Roadtrip")
            .assertIsDisplayed()
    }

    @Test
    fun createDialogVisible_showsConfirmAndCancelButtons() {
        setContent(
            uiState = TripListUiState(
                isCreateDialogVisible = true,
                tripNameInput = "",
            ),
        )

        composeTestRule.onNodeWithText("Confirm").assertIsDisplayed()
        composeTestRule.onNodeWithText("Cancel").assertIsDisplayed()
    }

    @Test
    fun createDialogVisible_showsPrePopulatedName() {
        setContent(
            uiState = TripListUiState(
                isCreateDialogVisible = true,
                tripNameInput = "Alps 2026",
            ),
        )

        composeTestRule
            .onNodeWithText("Alps 2026")
            .assertIsDisplayed()
    }

    @Test
    fun createDialog_clickingCancel_firesOnDismissCreateDialog() {
        var dismissed = false
        setContent(
            uiState = TripListUiState(
                isCreateDialogVisible = true,
                tripNameInput = "",
            ),
            onDismissCreateDialog = { dismissed = true },
        )

        composeTestRule
            .onNodeWithText("Cancel")
            .performClick()

        assertTrue(dismissed)
    }

    @Test
    fun createDialog_clickingConfirmWithName_firesOnConfirmCreateTrip() {
        var confirmed = false
        setContent(
            uiState = TripListUiState(
                isCreateDialogVisible = true,
                tripNameInput = "My Trip",
            ),
            onConfirmCreateTrip = { confirmed = true },
        )

        composeTestRule
            .onNodeWithText("Confirm")
            .performClick()

        assertTrue(confirmed)
    }

    @Test
    fun createDialogNotVisible_dialogIsNotShown() {
        setContent(
            uiState = TripListUiState(isCreateDialogVisible = false),
        )

        composeTestRule
            .onNodeWithText("New Roadtrip")
            .assertDoesNotExist()
    }

    // endregion

    // region Loading state

    @Test
    fun loadingState_withDialogVisible_confirmButtonIsDisabled() {
        setContent(
            uiState = TripListUiState(
                isCreateDialogVisible = true,
                tripNameInput = "Summer Drive",
                isLoading = true,
            ),
        )

        composeTestRule
            .onNodeWithText("Confirm")
            .assertIsNotEnabled()
    }

    // endregion
}
