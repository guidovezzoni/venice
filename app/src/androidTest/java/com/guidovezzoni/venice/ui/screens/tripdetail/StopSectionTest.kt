package com.guidovezzoni.venice.ui.screens.tripdetail

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Place
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import com.guidovezzoni.venice.R
import com.guidovezzoni.venice.domain.model.Stop
import com.guidovezzoni.venice.domain.model.StopStatus
import com.guidovezzoni.venice.ui.theme.HeadingToVeniceTheme
import com.guidovezzoni.venice.ui.util.formatCoordinates
import org.junit.Rule
import org.junit.Test

private val SAMPLE_STOP = Stop(
    id = "s1",
    tripId = "trip-1",
    placeName = "Florence, Italy",
    latitude = 43.7696,
    longitude = 11.2558,
    order = 1,
    status = StopStatus.PENDING,
)

class StopSectionTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun setContent(
        stop: Stop? = SAMPLE_STOP,
        coordinatesText: String? = formatCoordinates(SAMPLE_STOP.latitude, SAMPLE_STOP.longitude),
        stopDisplayState: StopDisplayState = StopDisplayState.UPCOMING,
        isLoading: Boolean = false,
        onMoveUp: (() -> Unit)? = null,
        onMoveDown: (() -> Unit)? = null,
        onDelete: (() -> Unit)? = null,
        onMarkDeparted: (() -> Unit)? = null,
        onUndoDeparted: (() -> Unit)? = null,
    ) {
        composeTestRule.setContent {
            HeadingToVeniceTheme {
                StopSection(
                    stop = stop,
                    coordinatesText = coordinatesText,
                    icon = Icons.Filled.Place,
                    titleRes = R.string.trip_detail_intermediate_stop_label,
                    setButtonTextRes = R.string.trip_detail_add_stop,
                    changeDescriptionRes = R.string.trip_detail_change_intermediate_stop,
                    filledLabelRes = R.string.trip_detail_intermediate_stop_label,
                    stopDisplayState = stopDisplayState,
                    isLoading = isLoading,
                    onMoveUp = onMoveUp,
                    onMoveDown = onMoveDown,
                    onDelete = onDelete,
                    onMarkDeparted = onMarkDeparted,
                    onUndoDeparted = onUndoDeparted,
                )
            }
        }
    }

    // region Departed checkmark icon

    @Test
    fun departedStop_showsCheckmarkIcon() {
        setContent(stopDisplayState = StopDisplayState.DEPARTED)

        composeTestRule
            .onNodeWithContentDescription("Departed")
            .assertIsDisplayed()
    }

    @Test
    fun upcomingStop_doesNotShowCheckmarkIcon() {
        setContent(stopDisplayState = StopDisplayState.UPCOMING)

        composeTestRule
            .onNodeWithContentDescription("Departed")
            .assertDoesNotExist()
    }

    // endregion

    // region Current stop border

    @Test
    fun currentStop_cardIsDisplayed() {
        setContent(stopDisplayState = StopDisplayState.CURRENT)

        composeTestRule
            .onNodeWithText("Florence, Italy")
            .assertIsDisplayed()
    }

    @Test
    fun upcomingStop_cardIsDisplayed() {
        setContent(stopDisplayState = StopDisplayState.UPCOMING)

        composeTestRule
            .onNodeWithText("Florence, Italy")
            .assertIsDisplayed()
    }

    // endregion

    // region Loading state

    @Test
    fun loadingState_actionButtonsAreDisabled() {
        setContent(
            stopDisplayState = StopDisplayState.CURRENT,
            isLoading = true,
            onMoveUp = {},
            onMoveDown = {},
            onDelete = {},
            onMarkDeparted = {},
        )

        composeTestRule.onNodeWithContentDescription("Move up").assertIsNotEnabled()
        composeTestRule.onNodeWithContentDescription("Move down").assertIsNotEnabled()
        composeTestRule.onNodeWithContentDescription("Remove stop").assertIsNotEnabled()
        composeTestRule.onNodeWithText("Mark as departed").assertIsNotEnabled()
    }

    @Test
    fun notLoadingState_actionButtonsAreEnabled() {
        setContent(
            stopDisplayState = StopDisplayState.CURRENT,
            isLoading = false,
            onMoveUp = {},
            onMoveDown = {},
            onDelete = {},
            onMarkDeparted = {},
        )

        composeTestRule.onNodeWithContentDescription("Move up").assertIsEnabled()
        composeTestRule.onNodeWithContentDescription("Move down").assertIsEnabled()
        composeTestRule.onNodeWithContentDescription("Remove stop").assertIsEnabled()
        composeTestRule.onNodeWithText("Mark as departed").assertIsEnabled()
    }

    // endregion
}
