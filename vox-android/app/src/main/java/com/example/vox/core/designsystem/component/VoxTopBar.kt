package com.example.vox.core.designsystem.component

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.example.vox.R
import com.example.vox.core.designsystem.icon.VoxIcons
import com.example.vox.core.designsystem.theme.VoxTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoxTopBar(
    title: String,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    TopAppBar(
        title = { Text(title) },
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(VoxIcons.Back, contentDescription = stringResource(R.string.cd_back))
                }
            }
        },
        actions = actions,
    )
}

@Preview
@Composable
private fun VoxTopBarPreview() {
    VoxTheme { VoxTopBar(title = "Goa Trip", onBack = {}) }
}
