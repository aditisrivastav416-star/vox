package com.example.vox.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.example.vox.R
import com.example.vox.core.common.UiText
import com.example.vox.core.designsystem.icon.VoxIcons
import com.example.vox.core.designsystem.theme.VoxSpacing
import com.example.vox.core.designsystem.theme.VoxTheme
import com.example.vox.core.ui.UiState

@Composable
fun LoadingState(modifier: Modifier = Modifier) {
    val description = stringResource(R.string.cd_loading)
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(Modifier.semantics { contentDescription = description })
    }
}

@Composable
fun ErrorState(message: UiText, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(VoxSpacing.Large),
        verticalArrangement = Arrangement.spacedBy(VoxSpacing.Small, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(VoxIcons.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error)
        Text(message.asString(), style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
    }
}

@Composable
fun EmptyState(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: () -> Unit = {},
) {
    Column(
        modifier = modifier.fillMaxSize().padding(VoxSpacing.ExtraLarge),
        verticalArrangement = Arrangement.spacedBy(VoxSpacing.Medium, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Text(
            body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (actionLabel != null) Button(onClick = onAction) { Text(actionLabel) }
    }
}

/** Renders Loading / Error automatically and hands Success data to [content]. */
@Composable
fun <T> UiStateContent(
    state: UiState<T>,
    modifier: Modifier = Modifier,
    content: @Composable (T) -> Unit,
) {
    when (state) {
        UiState.Loading -> LoadingState(modifier)
        is UiState.Error -> ErrorState(state.message, modifier)
        is UiState.Success -> content(state.data)
    }
}

@Preview(showBackground = true)
@Composable
private fun EmptyStatePreview() {
    VoxTheme {
        EmptyState(
            title = stringResource(R.string.groups_empty_title),
            body = stringResource(R.string.groups_empty_body),
            actionLabel = stringResource(R.string.groups_empty_action),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ErrorStatePreview() {
    VoxTheme { ErrorState(UiText.Res(R.string.error_generic)) }
}
