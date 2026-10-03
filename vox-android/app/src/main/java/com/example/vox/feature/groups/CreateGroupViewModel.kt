package com.example.vox.feature.groups

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vox.domain.model.GroupType
import com.example.vox.domain.model.Person
import com.example.vox.domain.repository.GroupRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CreateGroupUiState(
    val name: String = "",
    val type: GroupType = GroupType.TRIP,
    val query: String = "",
    val contacts: List<Person> = emptyList(),
    val selectedIds: Set<String> = emptySet(),
    val showNameError: Boolean = false,
    val showMembersError: Boolean = false,
    val error: String? = null,
)

private data class CreateGroupForm(
    val name: String = "",
    val type: GroupType = GroupType.TRIP,
    val query: String = "",
    val selectedIds: Set<String> = emptySet(),
    val showNameError: Boolean = false,
    val showMembersError: Boolean = false,
    val error: String? = null,
)

sealed interface CreateGroupAction {
    data class NameChanged(val value: String) : CreateGroupAction
    data class TypeChanged(val type: GroupType) : CreateGroupAction
    data class QueryChanged(val value: String) : CreateGroupAction
    data class ToggleMember(val personId: String) : CreateGroupAction
    data object Create : CreateGroupAction
}

sealed interface CreateGroupEvent {
    data class Created(val groupId: String) : CreateGroupEvent
}

class CreateGroupViewModel(private val groupRepository: GroupRepository) : ViewModel() {
    fun syncContacts() = viewModelScope.launch {
        runCatching { groupRepository.syncContacts() }
            .onFailure { e -> form.update { it.copy(error = e.message ?: "Could not sync contacts") } }
    }
    private val form = MutableStateFlow(CreateGroupForm())

    val state: StateFlow<CreateGroupUiState> = combine(form, groupRepository.observeContacts()) { f, contacts ->
        val q = f.query.trim()
        CreateGroupUiState(
            name = f.name,
            type = f.type,
            query = f.query,
            // Selected people stay visible regardless of the search.
            contacts = contacts.filter {
                it.id in f.selectedIds || q.isEmpty() ||
                    it.name.contains(q, ignoreCase = true) || it.contact?.contains(q, ignoreCase = true) == true
            },
            selectedIds = f.selectedIds,
            showNameError = f.showNameError,
            showMembersError = f.showMembersError,
            error = f.error,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CreateGroupUiState())

    private val _events = Channel<CreateGroupEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun onAction(action: CreateGroupAction) {
        when (action) {
            is CreateGroupAction.NameChanged -> form.update { it.copy(name = action.value, showNameError = false) }
            is CreateGroupAction.TypeChanged -> form.update { it.copy(type = action.type) }
            is CreateGroupAction.QueryChanged -> form.update { it.copy(query = action.value) }
            is CreateGroupAction.ToggleMember -> form.update {
                val ids = if (action.personId in it.selectedIds) it.selectedIds - action.personId else it.selectedIds + action.personId
                it.copy(selectedIds = ids, showMembersError = false)
            }
            CreateGroupAction.Create -> {
                val f = form.value
                if (f.name.isBlank() || f.selectedIds.isEmpty()) {
                    form.update { it.copy(showNameError = f.name.isBlank(), showMembersError = f.selectedIds.isEmpty()) }
                    return
                }
                viewModelScope.launch {
                    try {
                        val id = groupRepository.createGroup(f.name.trim(), f.type, f.selectedIds.toList())
                        _events.send(CreateGroupEvent.Created(id))
                    } catch (e: Exception) { form.update { it.copy(error = e.message ?: "Could not create group") } }
                }
            }
        }
    }
}
