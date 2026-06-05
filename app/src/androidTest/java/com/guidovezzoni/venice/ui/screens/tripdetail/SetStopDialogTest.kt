package com.guidovezzoni.venice.ui.screens.tripdetail

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import com.guidovezzoni.venice.R
import com.guidovezzoni.venice.ui.theme.HeadingToTheAlpsTheme
import org.junit.Rule
import org.junit.Test

class SetStopDialogTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun setContent(
        isLoading: Boolean = false,
        initialPlaceName: String = "",
    ) {
        composeTestRule.setContent {
            HeadingToTheAlpsTheme {
                SetStopDialog(
                    isLoading = isLoading,
                    dialogTitleRes = R.string.trip_detail_starting_point_label,
                    placeNameHintRes = R.string.trip_detail_starting_point_place_name_hint,
                    placeNameErrorRes = R.string.trip_detail_starting_point_place_name_error,
                    latitudeHintRes = R.string.trip_detail_starting_point_latitude_hint,
                    latitudeErrorRes = R.string.trip_detail_starting_point_latitude_error,
                    longitudeHintRes = R.string.trip_detail_starting_point_longitude_hint,
                    longitudeErrorRes = R.string.trip_detail_starting_point_longitude_error,
                    initialPlaceName = initialPlaceName,
                )
            }
        }
    }

    @Test
    fun loadingState_confirmButtonIsDisabled() {
        setContent(isLoading = true)

        composeTestRule
            .onNodeWithText("Confirm")
            .assertIsNotEnabled()
    }

    @Test
    fun loadingState_progressIndicatorIsVisible() {
        setContent(isLoading = true)

        composeTestRule
            .onNodeWithContentDescription("Loading")
            .assertExists()
    }

    @Test
    fun notLoadingState_confirmButtonIsEnabled() {
        setContent(isLoading = false)

        composeTestRule
            .onNodeWithText("Confirm")
            .assertIsEnabled()
    }
}
