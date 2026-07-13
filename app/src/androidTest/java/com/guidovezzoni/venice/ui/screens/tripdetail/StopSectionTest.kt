package com.guidovezzoni.venice.ui.screens.tripdetail

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Place
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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
        onNavigate: (() -> Unit)? = null,
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
                    onNavigate = onNavigate,
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

    // region Navigate button visibility and enablement

    @Test
    fun pendingStop_withNonNullOnNavigate_navigateButtonIsDisplayed() {
        val navigateContentDescription = "Navigate to Florence, Italy"
        setContent(
            stop = SAMPLE_STOP,
            onNavigate = {},
        )

        composeTestRule
            .onNodeWithContentDescription(navigateContentDescription)
            .assertIsDisplayed()
    }

    @Test
    fun nullOnNavigate_navigateButtonDoesNotExist() {
        val navigateContentDescription = "Navigate to Florence, Italy"
        setContent(
            stop = SAMPLE_STOP,
            onNavigate = null,
        )

        composeTestRule
            .onNodeWithContentDescription(navigateContentDescription)
            .assertDoesNotExist()
    }

    @Test
    fun navigateButton_loadingTrue_isDisabled() {
        val navigateContentDescription = "Navigate to Florence, Italy"
        setContent(
            stop = SAMPLE_STOP,
            isLoading = true,
            onNavigate = {},
        )

        composeTestRule
            .onNodeWithContentDescription(navigateContentDescription)
            .assertIsNotEnabled()
    }

    @Test
    fun navigateButton_loadingFalse_isEnabled() {
        val navigateContentDescription = "Navigate to Florence, Italy"
        setContent(
            stop = SAMPLE_STOP,
            isLoading = false,
            onNavigate = {},
        )

        composeTestRule
            .onNodeWithContentDescription(navigateContentDescription)
            .assertIsEnabled()
    }

    @Test
    fun navigateButton_contentDescription_includesPlaceName() {
        val milanStop = Stop(
            id = "s2",
            tripId = "trip-1",
            placeName = "Milan",
            latitude = 45.4642,
            longitude = 9.1900,
            order = 1,
            status = StopStatus.PENDING,
        )
        val expectedContentDescription = "Navigate to Milan"
        composeTestRule.setContent {
            HeadingToVeniceTheme {
                StopSection(
                    stop = milanStop,
                    icon = Icons.Filled.Place,
                    titleRes = R.string.trip_detail_intermediate_stop_label,
                    setButtonTextRes = R.string.trip_detail_add_stop,
                    changeDescriptionRes = R.string.trip_detail_change_intermediate_stop,
                    filledLabelRes = R.string.trip_detail_intermediate_stop_label,
                    onNavigate = {},
                )
            }
        }

        composeTestRule
            .onNodeWithContentDescription(expectedContentDescription)
            .assertIsDisplayed()
    }

    @Test
    fun navigateButton_onClick_lambdaInvokedOnce() {
        val navigateContentDescription = "Navigate to Florence, Italy"
        val invokedEvents = mutableListOf<Unit>()
        setContent(
            stop = SAMPLE_STOP,
            onNavigate = { invokedEvents.add(Unit) },
        )

        composeTestRule
            .onNodeWithContentDescription(navigateContentDescription)
            .performClick()

        val expectedInvocationCount = 1
        assert(invokedEvents.size == expectedInvocationCount) {
            "Expected navigate lambda to be invoked $expectedInvocationCount time(s) but was invoked ${invokedEvents.size} time(s)"
        }
    }

    // endregion
}
