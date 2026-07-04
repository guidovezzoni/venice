package com.guidovezzoni.venice.ui.screens.tripdetail

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.guidovezzoni.venice.ui.theme.HeadingToTheAlpsTheme
import org.junit.Rule
import org.junit.Test

class LegSummaryTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun setContent(formattedDistance: String, formattedDuration: String = "") {
        composeTestRule.setContent {
            HeadingToTheAlpsTheme {
                LegSummary(formattedDistance = formattedDistance, formattedDuration = formattedDuration)
            }
        }
    }

    // region Distance display

    @Test
    fun legSummary_withShortDistance_showsDistanceTextBetweenStops() {
        setContent(formattedDistance = "750 m", formattedDuration = "9 min")

        composeTestRule
            .onNodeWithText("750 m · 9 min", substring = true)
            .assertIsDisplayed()
    }

    // endregion

    // region Duration display

    @Test
    fun legSummary_withFormattedDuration_showsDurationTextAlongsideDistance() {
        composeTestRule.setContent {
            HeadingToTheAlpsTheme {
                LegSummary(
                    formattedDistance = "750 m",
                    formattedDuration = "9 min",
                )
            }
        }

        composeTestRule
            .onNodeWithText("750 m · 9 min", substring = true)
            .assertIsDisplayed()
    }

    // endregion

    // region Read-only semantics

    @Test
    fun legSummary_exposesNoClickableSemantics() {
        setContent(formattedDistance = "750 m", formattedDuration = "9 min")

        composeTestRule
            .onAllNodes(hasClickAction())
            .assertCountEquals(0)
    }

    // endregion
}
