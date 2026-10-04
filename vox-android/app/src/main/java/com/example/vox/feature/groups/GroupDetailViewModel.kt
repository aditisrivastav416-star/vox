package com.example.vox.feature.groups

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vox.core.ui.UiState
import com.example.vox.core.ui.stateInUi
import com.example.vox.domain.model.CURRENT_USER_ID
import com.example.vox.domain.model.Group
import com.example.vox.domain.model.GroupExpense
import com.example.vox.domain.model.Money
import com.example.vox.domain.model.Person
import com.example.vox.domain.repository.GroupRepository
import com.example.vox.navigation.VoxDestination
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

data class GroupExpenseUi(
    val expense: GroupExpense,
    val paidBy: Person?,
    val yourShare: Money,
)

data class GroupDetailUiModel(
    /** Null when the group no longer exists. */
    val group: Group?,
    val balance: Money?,
    val memberBalances: List<MemberBalanceUi>,
    val expenses: List<GroupExpenseUi>,
    val showSettleConfirmation: Boolean,
)

sealed interface GroupDetailAction {
    data object AddExpense : GroupDetailAction
    data class ShowSettleConfirmation(val show: Boolean) : GroupDetailAction
    data object ConfirmSettle : GroupDetailAction
    data object DeleteGroup : GroupDetailAction
}

sealed interface GroupDetailEvent {
    data object Deleted : GroupDetailEvent
}

class GroupDetailViewModel(
    savedStateHandle: SavedStateHandle,
    private val groupRepository: GroupRepository,
) : ViewModel() {
    val groupId: String = checkNotNull(savedStateHandle[VoxDestination.ARG_GROUP_ID])
    private val showSettle = MutableStateFlow(false)

    private val _events = Channel<GroupDetailEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    val state: StateFlow<UiState<GroupDetailUiModel>> = combine(groupRepository.observeGroup(groupId), showSettle) { group, settle ->
        GroupDetailUiModel(
            group = group,
            balance = group?.netBalance,
            memberBalances = group?.memberBalances().orEmpty(),
            expenses = group?.expenses.orEmpty().sortedByDescending { it.timestamp }.map { e ->
                GroupExpenseUi(e, group?.members?.find { it.id == e.paidById }, Money(e.shares[CURRENT_USER_ID] ?: 0, e.total.currencyCode))
            },
            showSettleConfirmation = settle,
        )
    }.stateInUi(viewModelScope)

    fun onAction(action: GroupDetailAction) {
        when (action) {
            is GroupDetailAction.ShowSettleConfirmation -> showSettle.value = action.show
            GroupDetailAction.ConfirmSettle -> viewModelScope.launch {
                showSettle.value = false
                runCatching { groupRepository.settleUp(groupId) }
            }
            GroupDetailAction.DeleteGroup -> viewModelScope.launch {
                runCatching { groupRepository.deleteGroup(groupId) }
                    .onSuccess { _events.send(GroupDetailEvent.Deleted) }
            }
            GroupDetailAction.AddExpense -> Unit // navigation
        }
    }
}
