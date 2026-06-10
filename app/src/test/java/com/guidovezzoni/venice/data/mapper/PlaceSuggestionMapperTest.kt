package com.guidovezzoni.venice.data.mapper

import android.text.SpannableString
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.guidovezzoni.venice.domain.model.PlaceSuggestion
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

private const val PLACE_ID = "abc"
private const val PRIMARY_TEXT = "Colosseum"
private const val SECONDARY_TEXT = "Rome, Italy"

class PlaceSuggestionMapperTest {

    private lateinit var mapper: PlaceSuggestionMapper

    @Before
    fun setUp() {
        mapper = PlaceSuggestionMapper()
    }

    @Test
    fun `GIVEN an AutocompletePrediction with placeId primaryText and secondaryText WHEN mapped THEN returns PlaceSuggestion with correct fields`() {
        val primarySpannable = mockk<SpannableString>(relaxed = true)
        val secondarySpannable = mockk<SpannableString>(relaxed = true)
        every { primarySpannable.toString() } returns PRIMARY_TEXT
        every { secondarySpannable.toString() } returns SECONDARY_TEXT

        val prediction = mockk<AutocompletePrediction>(relaxed = true)
        every { prediction.placeId } returns PLACE_ID
        every { prediction.getPrimaryText(null) } returns primarySpannable
        every { prediction.getSecondaryText(null) } returns secondarySpannable

        val result = mapper.map(prediction)

        val expected = PlaceSuggestion(
            placeId = PLACE_ID,
            primaryText = PRIMARY_TEXT,
            secondaryText = SECONDARY_TEXT,
        )
        assertEquals(expected, result)
    }
}
