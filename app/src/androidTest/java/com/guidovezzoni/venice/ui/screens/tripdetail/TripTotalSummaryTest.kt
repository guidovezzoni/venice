package com.guidovezzoni.venice.ui.screens.tripdetail

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.guidovezzoni.venice.ui.theme.HeadingToVeniceTheme
import org.junit.Rule
import org.junit.Test

class TripTotalSummaryTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun setContent(
        formattedTotalDistance: String? = null,
        formattedTotalDuration: String? = null,
    ) {
        composeTestRule.setContent {
            HeadingToVeniceTheme {
                TripTotalSummary(
                    formattedTotalDistance = formattedTotalDistance,
                    formattedTotalDuration = formattedTotalDuration,
                )
            }
        }
    }

    // region Both values available

    @Test
    fun bothValuesAvailable_showsBothDistanceAndDurationLabelsAndValues() {
        setContent(formattedTotalDistance = "12.5 km", formattedTotalDuration = "25 min")

        composeTestRule
            .onNodeWithText("Total distance")
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("12.5 km")
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("Est. driving time")
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("25 min")
            .assertIsDisplayed()
    }

    // endregion

    // region Both values unavailable

    @Test
    fun bothValuesUnavailable_showsCombinedTotalsUnavailableTextAndNoPerMetricText() {
        setContent(formattedTotalDistance = null, formattedTotalDuration = null)

        composeTestRule
            .onNodeWithText("Trip totals unavailable")
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("Total distance unavailable")
            .assertDoesNotExist()
        composeTestRule
            .onNodeWithText("Est. driving time unavailable")
            .assertDoesNotExist()
    }

    // endregion

    // region Mixed availability

    @Test
    fun distanceAvailableDurationUnavailable_showsDistanceLabelAndValueAlongsideDurationUnavailableText() {
        setContent(formattedTotalDistance = "12.5 km", formattedTotalDuration = null)

        composeTestRule
            .onNodeWithText("Total distance")
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("12.5 km")
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("Est. driving time unavailable")
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("Trip totals unavailable")
            .assertDoesNotExist()
    }

    @Test
    fun durationAvailableDistanceUnavailable_showsDurationLabelAndValueAlongsideDistanceUnavailableText() {
        setContent(formattedTotalDistance = null, formattedTotalDuration = "25 min")

        composeTestRule
            .onNodeWithText("Est. driving time")
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("25 min")
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("Total distance unavailable")
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("Trip totals unavailable")
            .assertDoesNotExist()
    }

    // endregion
}
