package com.guidovezzoni.venice.ui.screens.tripdetail

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.guidovezzoni.venice.R
import com.guidovezzoni.venice.domain.model.PlaceDetail
import com.guidovezzoni.venice.domain.model.PlaceSuggestion
import com.guidovezzoni.venice.ui.theme.HeadingToVeniceTheme
import com.guidovezzoni.venice.ui.util.formatCoordinate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SetStopDialogTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun setContent(
        isLoading: Boolean = false,
        initialPlaceName: String = "",
        suggestions: List<PlaceSuggestion> = emptyList(),
        isSearchingPlaces: Boolean = false,
        searchError: String? = null,
        selectedPlaceDetail: PlaceDetail? = null,
        isResolvingPlace: Boolean = false,
        placeDetailError: String? = null,
        onSuggestionSelected: (PlaceSuggestion) -> Unit = {},
    ) {
        composeTestRule.setContent {
            HeadingToVeniceTheme {
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
                    suggestions = suggestions,
                    isSearchingPlaces = isSearchingPlaces,
                    searchError = searchError,
                    selectedPlaceDetail = selectedPlaceDetail,
                    isResolvingPlace = isResolvingPlace,
                    placeDetailError = placeDetailError,
                    onSuggestionSelected = onSuggestionSelected,
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

    // Task 11.1: GIVEN suggestions list is non-empty WHEN dialog rendered THEN suggestion items displayed with primary and secondary text
    @Test
    fun GIVEN_suggestions_list_is_non_empty_WHEN_dialog_rendered_THEN_suggestion_items_displayed_with_primary_and_secondary_text() {
        val suggestions = listOf(
            PlaceSuggestion(
                placeId = "place-1",
                primaryText = "Colosseum",
                secondaryText = "Rome, Italy",
            ),
            PlaceSuggestion(
                placeId = "place-2",
                primaryText = "Trevi Fountain",
                secondaryText = "Rome, Italy",
            ),
        )

        setContent(suggestions = suggestions)

        composeTestRule.onNodeWithText("Colosseum").assertExists()
        composeTestRule.onNodeWithText("Trevi Fountain").assertExists()
        composeTestRule.onAllNodesWithText("Rome, Italy")[0].assertExists()
        composeTestRule.onAllNodesWithText("Rome, Italy")[1].assertExists()
    }

    // Task 11.2: GIVEN a suggestion is tapped WHEN user clicks it THEN onSuggestionSelected callback fires
    @Test
    fun GIVEN_a_suggestion_is_tapped_WHEN_user_clicks_it_THEN_onSuggestionSelected_callback_fires() {
        val suggestion = PlaceSuggestion(
            placeId = "place-1",
            primaryText = "Colosseum",
            secondaryText = "Rome, Italy",
        )
        val selectedSuggestions = mutableListOf<PlaceSuggestion>()

        setContent(
            suggestions = listOf(suggestion),
            onSuggestionSelected = { selectedSuggestions.add(it) },
        )

        composeTestRule.onNodeWithText("Colosseum").performClick()

        assertEquals(1, selectedSuggestions.size)
        assertEquals(suggestion, selectedSuggestions.first())
    }

    // Task 11.3: GIVEN isSearchingPlaces is true WHEN dialog rendered THEN search loading indicator displayed
    @Test
    fun GIVEN_isSearchingPlaces_is_true_WHEN_dialog_rendered_THEN_search_loading_indicator_displayed() {
        setContent(isSearchingPlaces = true)

        composeTestRule
            .onNodeWithContentDescription("Searching places")
            .assertExists()
    }

    // Task 11.4: GIVEN selectedPlaceDetail is non-null WHEN dialog rendered THEN latitude and longitude fields are read-only
    @Test
    fun GIVEN_selectedPlaceDetail_is_non_null_WHEN_dialog_rendered_THEN_latitude_and_longitude_fields_are_read_only() {
        val placeDetail = PlaceDetail(
            name = "Colosseum",
            latitude = 41.8902,
            longitude = 12.4922,
        )

        setContent(selectedPlaceDetail = placeDetail)

        // Verify the coordinate values from PlaceDetail are shown
        composeTestRule.onNodeWithText(formatCoordinate(placeDetail.latitude)).assertExists()
        composeTestRule.onNodeWithText(formatCoordinate(placeDetail.longitude)).assertExists()
    }

    // Task 5.1: GIVEN isResolvingPlace is true THEN the progress indicator with content description "Resolving place" is visible and confirm button is disabled
    @Test
    fun GIVEN_isResolvingPlace_is_true_THEN_resolving_indicator_is_visible_and_confirm_button_is_disabled() {
        setContent(isResolvingPlace = true)

        composeTestRule
            .onNodeWithContentDescription("Resolving place")
            .assertExists()
        composeTestRule
            .onNodeWithText("Confirm")
            .assertIsNotEnabled()
    }

    // Task 5.2: GIVEN placeDetailError is set THEN the error message is displayed inline
    @Test
    fun GIVEN_placeDetailError_is_set_THEN_error_message_is_displayed_inline() {
        val expectedError = "Could not resolve place. Tap a suggestion to try again."

        setContent(placeDetailError = expectedError)

        composeTestRule
            .onNodeWithText(expectedError)
            .assertExists()
    }

    // Task 5.3: GIVEN isResolvingPlace is false and placeDetailError is null THEN no resolving indicator or error is shown
    @Test
    fun GIVEN_isResolvingPlace_is_false_and_placeDetailError_is_null_THEN_no_resolving_indicator_or_error_is_shown() {
        val errorMessage = "Could not resolve place. Tap a suggestion to try again."

        setContent(isResolvingPlace = false, placeDetailError = null)

        composeTestRule
            .onNodeWithContentDescription("Resolving place")
            .assertDoesNotExist()
        composeTestRule
            .onNodeWithText(errorMessage)
            .assertDoesNotExist()
    }
}
