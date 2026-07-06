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

    private fun setContent(formattedTotalDistance: String?) {
        composeTestRule.setContent {
            HeadingToVeniceTheme {
                TripTotalSummary(formattedTotalDistance = formattedTotalDistance)
            }
        }
    }

    // region Non-null total distance

    @Test
    fun nonNullFormattedTotalDistance_showsLabelAndFormattedValue() {
        setContent(formattedTotalDistance = "12.5 km")

        composeTestRule
            .onNodeWithText("Total distance")
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("12.5 km")
            .assertIsDisplayed()
    }

    // endregion

    // region Null total distance

    @Test
    fun nullFormattedTotalDistance_showsUnavailableTextAndNoNumericValue() {
        setContent(formattedTotalDistance = null)

        composeTestRule
            .onNodeWithText("Total distance unavailable")
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("12.5 km")
            .assertDoesNotExist()
    }

    // endregion
}
