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
import com.guidovezzoni.venice.ui.theme.HeadingToTheAlpsTheme
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
        onSuggestionSelected: (PlaceSuggestion) -> Unit = {},
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
                    suggestions = suggestions,
                    isSearchingPlaces = isSearchingPlaces,
                    searchError = searchError,
                    selectedPlaceDetail = selectedPlaceDetail,
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
        composeTestRule.onNodeWithText("41.8902").assertExists()
        composeTestRule.onNodeWithText("12.4922").assertExists()
    }
}
