package com.guidovezzoni.venice.ui.screens.tripdetail

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Place
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import com.guidovezzoni.venice.R
import com.guidovezzoni.venice.domain.model.Stop
import com.guidovezzoni.venice.domain.model.StopStatus
import com.guidovezzoni.venice.ui.theme.HeadingToTheAlpsTheme
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
        stopDisplayState: StopDisplayState = StopDisplayState.UPCOMING,
    ) {
        composeTestRule.setContent {
            HeadingToTheAlpsTheme {
                StopSection(
                    stop = stop,
                    icon = Icons.Filled.Place,
                    titleRes = R.string.trip_detail_intermediate_stop_label,
                    setButtonTextRes = R.string.trip_detail_add_stop,
                    changeDescriptionRes = R.string.trip_detail_change_intermediate_stop,
                    filledLabelRes = R.string.trip_detail_intermediate_stop_label,
                    stopDisplayState = stopDisplayState,
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
}
