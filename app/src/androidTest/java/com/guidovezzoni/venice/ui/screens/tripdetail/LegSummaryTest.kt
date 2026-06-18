package com.guidovezzoni.venice.ui.screens.tripdetail

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.guidovezzoni.venice.domain.model.Leg
import com.guidovezzoni.venice.ui.theme.HeadingToTheAlpsTheme
import org.junit.Rule
import org.junit.Test

private const val TRIP_ID = "trip-1"

private val BASE_LEG = Leg(
    id = "leg-1",
    tripId = TRIP_ID,
    fromStopId = "stop-1",
    toStopId = "stop-2",
    distanceMetres = 750,
    durationSeconds = 540,
    encodedPolyline = "",
)

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

    @Test
    fun shortDistance_displays750m() {
        setContent(leg = BASE_LEG.copy(distanceMetres = 750))

        composeTestRule
            .onNodeWithText("750 m", substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun boundaryDistance_displays1Point0Km() {
        setContent(leg = BASE_LEG.copy(distanceMetres = 1000))

        composeTestRule
            .onNodeWithText("1.0 km", substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun longDistance_displays12Point5Km() {
        setContent(leg = BASE_LEG.copy(distanceMetres = 12500))

        composeTestRule
            .onNodeWithText("12.5 km", substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun combinedDistanceAndDuration_displays5Point0KmAnd10Min() {
        setContent(leg = BASE_LEG.copy(distanceMetres = 5000, durationSeconds = 600))

        composeTestRule
            .onNodeWithText("5.0 km · 10 min")
            .assertIsDisplayed()
    }
}
