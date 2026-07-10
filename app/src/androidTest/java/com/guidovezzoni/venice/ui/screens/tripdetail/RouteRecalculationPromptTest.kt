package com.guidovezzoni.venice.ui.screens.tripdetail

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.guidovezzoni.venice.ui.theme.HeadingToVeniceTheme
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class RouteRecalculationPromptTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // region Display

    @Test
    fun givenIsEnabledTrue_whenRouteRecalculationPromptIsRendered_thenBothMessageAndActionTextsAreDisplayed() {
        composeTestRule.setContent {
            HeadingToVeniceTheme {
                RouteRecalculationPrompt(isEnabled = true)
            }
        }

        composeTestRule
            .onNodeWithText("Route not calculated")
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("Tap to calculate")
            .assertIsDisplayed()
    }

    // endregion

    // region Click — enabled

    @Test
    fun givenIsEnabledTrue_whenUserTapsPrompt_thenOnClickLambdaIsInvoked() {
        var clicked = false
        composeTestRule.setContent {
            HeadingToVeniceTheme {
                RouteRecalculationPrompt(
                    isEnabled = true,
                    onClick = { clicked = true },
                )
            }
        }

        composeTestRule
            .onNodeWithText("Route not calculated")
            .performClick()

        assertTrue(clicked)
    }

    // endregion

    // region Click — disabled

    @Test
    fun givenIsEnabledFalse_whenUserTapsPrompt_thenOnClickLambdaIsNotInvoked() {
        var clicked = false
        composeTestRule.setContent {
            HeadingToVeniceTheme {
                RouteRecalculationPrompt(
                    isEnabled = false,
                    onClick = { clicked = true },
                )
            }
        }

        composeTestRule
            .onNodeWithText("Route not calculated")
            .performClick()

        assertFalse(clicked)
    }

    // endregion
}
