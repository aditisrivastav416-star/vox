package com.example.vox.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.vox.R
import com.example.vox.core.designsystem.component.Avatar
import com.example.vox.core.designsystem.component.UiStateContent
import com.example.vox.core.designsystem.component.VoxDropdown
import com.example.vox.core.designsystem.icon.VoxIcons
import com.example.vox.core.designsystem.theme.VoxAppearance
import com.example.vox.core.designsystem.theme.VoxTheme
import com.example.vox.core.ui.ObserveEvents
import com.example.vox.core.ui.UiState
import com.example.vox.domain.model.UserProfile
import com.example.vox.feature.auth.currencyLabel
import com.example.vox.core.designsystem.theme.LocalMoneyFormatter
import com.example.vox.core.designsystem.theme.LocalDateFormatter

@Composable
fun SettingsRoute(viewModel: SettingsViewModel, onBack: () -> Unit, onOpenGroups: () -> Unit, onOpenRecurring: () -> Unit, onSignedOut: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ObserveEvents(viewModel.events) { if (it == SettingsEvent.SignedOut) onSignedOut() }
    SettingsScreen(state, onBack) { action ->
        when (action) {
            SettingsAction.OpenGroups -> onOpenGroups()
            SettingsAction.OpenRecurring -> onOpenRecurring()
            else -> viewModel.onAction(action)
        }
    }
}

@Composable
fun SettingsScreen(state: UiState<SettingsUiModel>, onBack: () -> Unit, onAction: (SettingsAction) -> Unit) {
    val context = LocalContext.current
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        UiStateContent(state) { model ->
            Column(
                Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background,
                    RoundedCornerShape(topStart = 29.dp, topEnd = 29.dp))
                    .verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Box(Modifier.fillMaxWidth(.12f).height(4.dp).background(MaterialTheme.colorScheme.outline, RoundedCornerShape(4.dp)))
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Your space", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
                    IconButton(onClick = onBack) { Icon(VoxIcons.Close, contentDescription = "Close account") }
                }
                Row(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainerLow, RoundedCornerShape(17.dp)).padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    val name = model.profile?.name.orEmpty()
                    Avatar(name.ifBlank { "A" })
                    Column {
                        Text(name.ifBlank { "Your Vox account" }, style = MaterialTheme.typography.titleMedium)
                        Text("Your Vox account", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                    }
                }
                SectionLabel("SHARED MONEY", "Your groups")
                if (model.groups.isEmpty()) SettingsRow(VoxIcons.Groups, stringResource(R.string.groups)) { onAction(SettingsAction.OpenGroups) }
                model.groups.take(2).forEach { group ->
                    SettingsRow(VoxIcons.Groups, group.name, "${group.members.size} members · Shared expenses") {
                        onAction(SettingsAction.OpenGroups)
                    }
                }
                SectionLabel("ON REPEAT", "Recurring payments")
                if (model.recurring.isEmpty()) SettingsRow(VoxIcons.Recurring, stringResource(R.string.recurring_title)) { onAction(SettingsAction.OpenRecurring) }
                model.recurring.take(2).forEach { payment ->
                    SettingsRow(VoxIcons.Recurring, payment.name,
                        "${payment.amount?.let(LocalMoneyFormatter.current::format) ?: "Amount varies"} · ${payment.frequency.name.lowercase().replaceFirstChar { it.uppercase() }} · Due ${LocalDateFormatter.current.dueDate(payment.nextDueDate)}") {
                        onAction(SettingsAction.OpenRecurring)
                    }
                }
                model.profile?.let { profile ->
                    VoxDropdown("Currency", profile.currencyCode, model.currencies, ::currencyLabel,
                        onSelect = { onAction(SettingsAction.CurrencyChanged(it)) })
                }
                SettingsRow(VoxIcons.Settings, if (VoxAppearance.dark) "Light appearance" else "Dark appearance") {
                    VoxAppearance.toggle(context)
                }
                SettingsRow(VoxIcons.Back, stringResource(R.string.settings_sign_out)) { onAction(SettingsAction.SignOut) }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun SectionLabel(eyebrow: String, title: String) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(eyebrow, color = MaterialTheme.colorScheme.primary, fontSize = 10.sp, letterSpacing = 1.5.sp)
        Text(title, style = MaterialTheme.typography.titleLarge)
    }
}

@Composable
private fun SettingsRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, subtitle: String? = null, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainerLow, RoundedCornerShape(15.dp))
        .clickable(role = Role.Button, onClick = onClick).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(13.dp)) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
        }
        Icon(VoxIcons.Forward, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Preview
@Composable
private fun SettingsPreview() { VoxTheme { SettingsScreen(UiState.Success(SettingsUiModel(UserProfile("Aditi", "INR"))), {}, {}) } }
