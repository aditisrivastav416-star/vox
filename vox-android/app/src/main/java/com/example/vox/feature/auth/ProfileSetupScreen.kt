package com.example.vox.feature.auth

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.vox.R
import com.example.vox.core.designsystem.component.Avatar
import com.example.vox.core.designsystem.component.VoxDropdown
import com.example.vox.core.designsystem.component.VoxTopBar
import com.example.vox.core.designsystem.theme.VoxSize
import com.example.vox.core.designsystem.theme.VoxSpacing
import com.example.vox.core.designsystem.theme.VoxTheme
import com.example.vox.core.ui.ObserveEvents
import java.util.Currency

@Composable
fun ProfileSetupRoute(viewModel: AuthViewModel, onBack: () -> Unit, onDone: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ObserveEvents(viewModel.events) { if (it == AuthEvent.SignedIn) onDone() }
    ProfileSetupScreen(state = state, onAction = viewModel::onAction, onBack = onBack)
}

@Composable
fun currencyLabel(code: String): String =
    stringResource(R.string.currency_option, code, Currency.getInstance(code).symbol)

@Composable
fun ProfileSetupScreen(
    state: ProfileSetupUiState,
    onAction: (AuthAction) -> Unit,
    onBack: () -> Unit,
) {
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) onAction(AuthAction.PhotoPicked(uri.toString()))
    }
    Scaffold(topBar = { VoxTopBar(stringResource(R.string.profile_setup_title), onBack = onBack) }) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(VoxSpacing.Medium),
            verticalArrangement = Arrangement.spacedBy(VoxSpacing.Medium),
        ) {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Avatar(state.name.ifBlank { "?" }, size = VoxSize.AvatarLarge, style = MaterialTheme.typography.displaySmall)
                TextButton(onClick = {
                    photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }) {
                    Text(stringResource(if (state.photoUri == null) R.string.profile_add_photo else R.string.profile_photo_added))
                }
            }
            OutlinedTextField(
                value = state.name,
                onValueChange = { onAction(AuthAction.NameChanged(it)) },
                label = { Text(stringResource(R.string.profile_name)) },
                isError = state.showNameError,
                supportingText = if (state.showNameError) {
                    { Text(stringResource(R.string.error_name_required)) }
                } else null,
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            VoxDropdown(
                label = stringResource(R.string.profile_currency),
                selected = state.currencyCode,
                options = state.currencies,
                optionLabel = { currencyLabel(it) },
                onSelect = { onAction(AuthAction.CurrencyChanged(it)) },
            )
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Button(
                onClick = { onAction(AuthAction.Save) },
                modifier = Modifier.fillMaxWidth().heightIn(min = VoxSize.MinTouchTarget),
            ) { Text(stringResource(R.string.profile_continue)) }
        }
    }
}

@Preview
@Composable
private fun ProfileSetupPreview() {
    VoxTheme { ProfileSetupScreen(ProfileSetupUiState(name = "Aditi"), onAction = {}, onBack = {}) }
}
