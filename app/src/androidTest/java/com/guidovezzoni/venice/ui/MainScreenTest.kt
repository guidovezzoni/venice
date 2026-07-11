package com.guidovezzoni.venice.ui

import android.content.Intent
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.remember
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.guidovezzoni.venice.ui.effect.TripDetailUiEffect
import com.guidovezzoni.venice.ui.state.DialogState
import com.guidovezzoni.venice.ui.theme.HeadingToVeniceTheme
import kotlinx.coroutines.flow.MutableSharedFlow
import org.junit.Rule
import org.junit.Test

private const val TEST_LATITUDE = 41.9028
private const val TEST_LONGITUDE = 12.4964
private const val TEST_PLACE_NAME = "Rome"
private const val TEST_NO_APP_ERROR = "No navigation app found"
private const val TEST_NAVIGATION_ERROR = "Navigation service unavailable"
private const val TEST_START_ERROR = "Starting point error"
private const val TEST_DEST_ERROR = "Destination error"

class MainScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun setContent(
        effectFlow: MutableSharedFlow<TripDetailUiEffect>,
        resolveActivity: ((Intent) -> Boolean)? = null,
        startActivity: ((Intent) -> Unit)? = null,
    ) {
        composeTestRule.setContent {
            HeadingToVeniceTheme {
                val snackbarHostState = remember { SnackbarHostState() }
                Scaffold(snackbarHost = { SnackbarHost(hostState = snackbarHostState) }) { _ ->
                    TripDetailEffectHandler(
                        effects = effectFlow,
                        snackbarHostState = snackbarHostState,
                        startingPointErrorMessage = TEST_START_ERROR,
                        destinationErrorMessage = TEST_DEST_ERROR,
                        noAppAvailableError = TEST_NO_APP_ERROR,
                        getDialogState = { DialogState.None },
                        resolveActivity = resolveActivity,
                        startActivity = startActivity,
                    )
                }
            }
        }
    }

    @Test
    fun launchNavigation_whenIntentResolvable_noErrorSnackbarIsShown() {
        val effectFlow = MutableSharedFlow<TripDetailUiEffect>(replay = 1)
        effectFlow.tryEmit(
            TripDetailUiEffect.LaunchNavigation(TEST_LATITUDE, TEST_LONGITUDE, TEST_PLACE_NAME),
        )

        setContent(
            effectFlow = effectFlow,
            resolveActivity = { true },
            startActivity = {},
        )

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(TEST_NO_APP_ERROR).assertDoesNotExist()
    }

    @Test
    fun launchNavigation_whenIntentNotResolvable_errorSnackbarIsShown() {
        val effectFlow = MutableSharedFlow<TripDetailUiEffect>(replay = 1)
        effectFlow.tryEmit(
            TripDetailUiEffect.LaunchNavigation(TEST_LATITUDE, TEST_LONGITUDE, TEST_PLACE_NAME),
        )

        setContent(
            effectFlow = effectFlow,
            resolveActivity = { false },
        )

        composeTestRule.onNodeWithText(TEST_NO_APP_ERROR).assertIsDisplayed()
    }

    @Test
    fun showNavigationError_snackbarDisplaysEffectMessage() {
        val effectFlow = MutableSharedFlow<TripDetailUiEffect>(replay = 1)
        effectFlow.tryEmit(TripDetailUiEffect.ShowNavigationError(TEST_NAVIGATION_ERROR))

        setContent(effectFlow = effectFlow)

        composeTestRule.onNodeWithText(TEST_NAVIGATION_ERROR).assertIsDisplayed()
    }
}
