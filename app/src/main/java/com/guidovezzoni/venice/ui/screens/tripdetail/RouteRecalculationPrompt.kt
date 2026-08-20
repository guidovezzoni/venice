package com.guidovezzoni.venice.ui.screens.tripdetail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.guidovezzoni.venice.R
import com.guidovezzoni.venice.ui.theme.HeadingToVeniceTheme

private val ROUTE_RECALCULATION_PROMPT_PADDING = 16.dp

@Composable
fun RouteRecalculationPrompt(
    isEnabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = isEnabled, onClick = onClick),
    ) {
        Column(
            modifier = Modifier.padding(ROUTE_RECALCULATION_PROMPT_PADDING),
        ) {
            Text(
                text = stringResource(R.string.trip_detail_recalculation_prompt_message),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = stringResource(R.string.trip_detail_recalculation_prompt_action),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewRouteRecalculationPromptEnabled() {
    HeadingToVeniceTheme {
        RouteRecalculationPrompt(
            isEnabled = true,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewRouteRecalculationPromptDisabled() {
    HeadingToVeniceTheme {
        RouteRecalculationPrompt(
            isEnabled = false,
        )
    }
}

