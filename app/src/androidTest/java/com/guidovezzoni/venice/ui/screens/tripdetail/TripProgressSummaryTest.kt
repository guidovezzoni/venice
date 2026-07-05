package com.guidovezzoni.venice.ui.screens.tripdetail

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.guidovezzoni.venice.ui.theme.HeadingToVeniceTheme
import org.junit.Rule
import org.junit.Test

class TripProgressSummaryTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun setContent(departedCount: Int, totalCount: Int) {
        composeTestRule.setContent {
            HeadingToVeniceTheme {
                TripProgressSummary(
                    departedCount = departedCount,
                    totalCount = totalCount,
                )
            }
        }
    }

    // region Progress text

    @Test
    fun partialProgress_showsCorrectProgressText() {
        setContent(departedCount = 3, totalCount = 6)

        composeTestRule
            .onNodeWithText("3 of 6 stops completed")
            .assertIsDisplayed()
    }

    @Test
    fun zeroProgress_showsZeroProgressText() {
        setContent(departedCount = 0, totalCount = 4)

        composeTestRule
            .onNodeWithText("0 of 4 stops completed")
            .assertIsDisplayed()
    }

    @Test
    fun allStopsDeparted_showsCompleteProgressText() {
        setContent(departedCount = 6, totalCount = 6)

        composeTestRule
            .onNodeWithText("6 of 6 stops completed")
            .assertIsDisplayed()
    }

    // endregion
}
