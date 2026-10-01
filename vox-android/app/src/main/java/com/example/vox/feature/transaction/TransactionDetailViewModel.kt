package com.example.vox.feature.transaction

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vox.core.ui.UiState
import com.example.vox.core.ui.stateInUi
import com.example.vox.domain.model.Transaction
import com.example.vox.domain.repository.GroupRepository
import com.example.vox.domain.repository.TransactionRepository
import com.example.vox.navigation.VoxDestination
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

data class TransactionDetailUiModel(
    /** Null when the transaction no longer exists. */
    val transaction: Transaction?,
    val groupName: String?,
    val showDeleteConfirmation: Boolean,
)

sealed interface TransactionDetailAction {
    data object Edit : TransactionDetailAction
    data class ShowDeleteConfirmation(val show: Boolean) : TransactionDetailAction
    data object ConfirmDelete : TransactionDetailAction
}

sealed interface TransactionDetailEvent {
    data object Deleted : TransactionDetailEvent
}

class TransactionDetailViewModel(
    savedStateHandle: SavedStateHandle,
    private val transactionRepository: TransactionRepository,
    groupRepository: GroupRepository,
) : ViewModel() {
    private val id: String = checkNotNull(savedStateHandle[VoxDestination.ARG_TRANSACTION_ID])
    private val showDelete = MutableStateFlow(false)

    private val _events = Channel<TransactionDetailEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    val state: StateFlow<UiState<TransactionDetailUiModel>> = combine(
        transactionRepository.observeTransaction(id),
        groupRepository.observeGroups(),
        showDelete,
    ) { tx, groups, delete ->
        TransactionDetailUiModel(tx, groups.find { it.id == tx?.groupId }?.name, delete)
    }.stateInUi(viewModelScope)

    fun onAction(action: TransactionDetailAction) {
        when (action) {
            is TransactionDetailAction.ShowDeleteConfirmation -> showDelete.value = action.show
            TransactionDetailAction.ConfirmDelete -> viewModelScope.launch {
                showDelete.value = false
                _events.send(TransactionDetailEvent.Deleted)
                transactionRepository.delete(id)
            }
            TransactionDetailAction.Edit -> Unit // navigation
        }
    }
}
