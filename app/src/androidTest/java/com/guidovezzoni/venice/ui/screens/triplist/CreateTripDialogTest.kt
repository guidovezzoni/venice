package com.guidovezzoni.venice.ui.screens.triplist

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import com.guidovezzoni.venice.ui.theme.HeadingToVeniceTheme
import org.junit.Rule
import org.junit.Test

class CreateTripDialogTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun setContent(
        isLoading: Boolean = false,
        tripName: String = "",
    ) {
        composeTestRule.setContent {
            HeadingToVeniceTheme {
                CreateTripDialog(
                    isLoading = isLoading,
                    tripName = tripName,
                )
            }
        }
    }

    @Test
    fun loadingState_confirmButtonIsDisabled() {
        setContent(isLoading = true, tripName = "Summer Drive")

        composeTestRule
            .onNodeWithText("Confirm")
            .assertIsNotEnabled()
    }

    @Test
    fun loadingState_progressIndicatorIsVisible() {
        setContent(isLoading = true, tripName = "Summer Drive")

        composeTestRule
            .onNodeWithContentDescription("Loading")
            .assertExists()
    }

    @Test
    fun notLoadingState_withValidName_confirmButtonIsEnabled() {
        setContent(isLoading = false, tripName = "Summer Drive")

        composeTestRule
            .onNodeWithText("Confirm")
            .assertIsEnabled()
    }
}
