package com.offgridrescue.app.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.offgridrescue.app.ui.components.SimpleScreen

@Composable
fun SettingsScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    SimpleScreen(
        title = "Settings",
        description = "Settings is a placeholder. No settings are implemented yet.",
        onBackClick = onBackClick,
        modifier = modifier
    )
}
