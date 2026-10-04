package com.example.vox.feature.groups

import androidx.compose.foundation.layout.Arrangement
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.vox.R
import com.example.vox.core.designsystem.component.Avatar
import com.example.vox.core.designsystem.component.VoxTopBar
import com.example.vox.core.designsystem.icon.VoxIcons
import com.example.vox.core.designsystem.theme.VoxSize
import com.example.vox.core.designsystem.theme.VoxSpacing
import com.example.vox.core.designsystem.theme.VoxTheme
import com.example.vox.core.ui.ObserveEvents
import com.example.vox.core.ui.labelRes
import com.example.vox.domain.model.GroupType
import com.example.vox.domain.model.Person

@Composable
fun CreateGroupRoute(viewModel: CreateGroupViewModel, onBack: () -> Unit, onCreated: (String) -> Unit) {
    val context = LocalContext.current
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) viewModel.syncContacts()
    }
    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) viewModel.syncContacts()
        else permission.launch(Manifest.permission.READ_CONTACTS)
    }
    val state by viewModel.state.collectAsStateWithLifecycle()
    ObserveEvents(viewModel.events) { if (it is CreateGroupEvent.Created) onCreated(it.groupId) }
    CreateGroupScreen(state, viewModel::onAction, onBack) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) viewModel.syncContacts()
        else permission.launch(Manifest.permission.READ_CONTACTS)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CreateGroupScreen(state: CreateGroupUiState, onAction: (CreateGroupAction) -> Unit, onBack: () -> Unit, onSyncContacts: () -> Unit = {}) {
    Scaffold(
        topBar = { VoxTopBar(stringResource(R.string.create_group), onBack = onBack) },
        bottomBar = {
            Button(
                onClick = { onAction(CreateGroupAction.Create) },
                modifier = Modifier.fillMaxWidth().imePadding().padding(VoxSpacing.Medium).heightIn(min = VoxSize.MinTouchTarget),
            ) { Text(stringResource(R.string.create_group)) }
        },
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).padding(horizontal = VoxSpacing.Medium)) {
            item(key = "name") {
                OutlinedTextField(
                    value = state.name,
                    onValueChange = { onAction(CreateGroupAction.NameChanged(it)) },
                    label = { Text(stringResource(R.string.group_name)) },
                    isError = state.showNameError,
                    supportingText = if (state.showNameError) {
                        { Text(stringResource(R.string.error_group_name_required)) }
                    } else null,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item(key = "type") {
                Text(stringResource(R.string.group_type), style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = VoxSpacing.Small))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(VoxSpacing.Small)) {
                    GroupType.entries.forEach { type ->
                        FilterChip(
                            selected = state.type == type,
                            onClick = { onAction(CreateGroupAction.TypeChanged(type)) },
                            label = { Text(stringResource(type.labelRes)) },
                        )
                    }
                }
            }
            item(key = "people") {
                Text(stringResource(R.string.group_add_people), style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = VoxSpacing.Small))
                TextButton(onClick = onSyncContacts) { Text("Sync contacts") }
                OutlinedTextField(
                    value = state.query,
                    onValueChange = { onAction(CreateGroupAction.QueryChanged(it)) },
                    placeholder = { Text(stringResource(R.string.group_search_people)) },
                    leadingIcon = { Icon(VoxIcons.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (state.showMembersError) {
                    Text(stringResource(R.string.error_group_members_required), color = MaterialTheme.colorScheme.error)
                }
                state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                if (state.contacts.isEmpty()) Text("Registered Vox contacts will appear here after sync.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            items(state.contacts, key = { it.id }) { person ->
                ContactRow(person, selected = person.id in state.selectedIds) { onAction(CreateGroupAction.ToggleMember(person.id)) }
            }
        }
    }
}

@Composable
private fun ContactRow(person: Person, selected: Boolean, onToggle: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = VoxSize.MinTouchTarget)
            .toggleable(value = selected, role = Role.Checkbox, onValueChange = { onToggle() })
            .padding(vertical = VoxSpacing.Small),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(VoxSpacing.Medium),
    ) {
        Avatar(person.name)
        Text(person.name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        // Row is a toggleable checkbox; TalkBack announces the state, so the icon is decorative.
        Icon(if (selected) VoxIcons.Check else VoxIcons.Add, contentDescription = null)
    }
}

@Preview
@Composable
private fun CreateGroupPreview() {
    VoxTheme {
        CreateGroupScreen(
            CreateGroupUiState(name = "Goa Trip", contacts = listOf(Person("rahul", "Rahul"), Person("riya", "Riya")), selectedIds = setOf("rahul")),
            {}, {},
        )
    }
}
