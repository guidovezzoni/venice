package com.guidovezzoni.venice.ui.screens.tripdetail

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.guidovezzoni.venice.domain.model.Leg
import com.guidovezzoni.venice.ui.theme.HeadingToTheAlpsTheme
import org.junit.Rule
import org.junit.Test

class LegSummaryTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun setContent(leg: Leg) {
        composeTestRule.setContent {
            HeadingToTheAlpsTheme {
                LegSummary(leg = leg)
            }
        }
    }

    // region Distance display

    @Test
    fun legSummary_withShortDistance_showsDistanceTextBetweenStops() {
        val leg = Leg(
            id = "leg-1",
            tripId = "trip-1",
            fromStopId = "stop-1",
            toStopId = "stop-2",
            distanceMetres = 750,
            durationSeconds = 540,
            encodedPolyline = "",
        )

        setContent(leg)

        // The device is configured with en-GB locale (imperial), so distance is formatted in miles.
        // 750 metres ≈ 0.5 miles
        composeTestRule
            .onNodeWithText("0.5 mi · 9 min", substring = true)
            .assertIsDisplayed()
    }

    // endregion

    // region Read-only semantics

    @Test
    fun legSummary_exposesNoClickableSemantics() {
        val leg = Leg(
            id = "leg-1",
            tripId = "trip-1",
            fromStopId = "stop-1",
            toStopId = "stop-2",
            distanceMetres = 750,
            durationSeconds = 540,
            encodedPolyline = "",
        )

        setContent(leg)

        composeTestRule
            .onAllNodes(hasClickAction())
            .assertCountEquals(0)
    }

    // endregion
}
