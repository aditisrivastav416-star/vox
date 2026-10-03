package com.example.vox.feature.split

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vox.core.common.MoneyFormatter
import com.example.vox.domain.model.CURRENT_USER_ID
import com.example.vox.domain.model.Category
import com.example.vox.domain.model.GroupExpense
import com.example.vox.domain.model.Money
import com.example.vox.domain.model.PaymentMethod
import com.example.vox.domain.model.Person
import com.example.vox.domain.model.ReceiptItem
import com.example.vox.domain.model.SplitCalculator
import com.example.vox.domain.model.Transaction
import com.example.vox.domain.model.TransactionSource
import com.example.vox.domain.model.TransactionType
import com.example.vox.domain.repository.GroupRepository
import com.example.vox.domain.repository.ReceiptRepository
import com.example.vox.domain.repository.TransactionRepository
import com.example.vox.domain.repository.UserRepository
import com.example.vox.navigation.VoxDestination
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.UUID

enum class SplitStep { CHOOSE_METHOD, EQUAL, ITEMS, ITEMS_RESULT }

data class SplitUiState(
    val isLoading: Boolean = true,
    val step: SplitStep = SplitStep.CHOOSE_METHOD,
    val groupName: String? = null,
    val currencyCode: String = "INR",
    /** Editable only when not splitting a scanned receipt. */
    val amountInput: String = "",
    val description: String = "",
    val isFromReceipt: Boolean = false,
    val items: List<ReceiptItem> = emptyList(),
    val tax: Money? = null,
    val people: List<Person> = emptyList(),
    val selectedIds: Set<String> = setOf(CURRENT_USER_ID),
    val itemAssignees: Map<String, Set<String>> = emptyMap(),
    /** Person id -> share, for the current step's preview. */
    val shares: Map<String, Long> = emptyMap(),
    val total: Money? = null,
    val showAmountError: Boolean = false,
    val showDescriptionError: Boolean = false,
    val showAssignmentError: Boolean = false,
    val error: String? = null,
) {
    val canSplitByItems: Boolean get() = items.isNotEmpty()
}

sealed interface SplitAction {
    data class AmountChanged(val value: String) : SplitAction
    data class DescriptionChanged(val value: String) : SplitAction
    data object ChooseEqual : SplitAction
    data object ChooseItems : SplitAction
    data class TogglePerson(val personId: String) : SplitAction
    data class ToggleItemAssignee(val itemId: String, val personId: String) : SplitAction
    data object Calculate : SplitAction
    data object Confirm : SplitAction
    data object Back : SplitAction
}

sealed interface SplitEvent {
    data object Done : SplitEvent
    data object Exit : SplitEvent
}

class SplitViewModel(
    savedStateHandle: SavedStateHandle,
    private val groupRepository: GroupRepository,
    receiptRepository: ReceiptRepository,
    private val transactionRepository: TransactionRepository,
    userRepository: UserRepository,
    private val moneyFormatter: MoneyFormatter = MoneyFormatter(),
) : ViewModel() {
    private val groupId: String? = savedStateHandle[VoxDestination.ARG_GROUP_ID]
    private val receiptId: String? = savedStateHandle[VoxDestination.ARG_RECEIPT_ID]
    private val submissionId: String = savedStateHandle["submission_id"] ?: UUID.randomUUID().toString().also { savedStateHandle["submission_id"] = it }

    private val _state = MutableStateFlow(SplitUiState())
    val state: StateFlow<SplitUiState> = _state.asStateFlow()

    private val _events = Channel<SplitEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            val group = groupId?.let { groupRepository.observeGroup(it).first() }
            val receipt = receiptId?.let { receiptRepository.observeReceipt(it).first() }
            val people = group?.members ?: (listOf(Person(CURRENT_USER_ID, "")) + groupRepository.observeContacts().first())
            val currency = receipt?.total?.currencyCode ?: group?.currencyCode
                ?: userRepository.observeProfile().first()?.currencyCode ?: "INR"
            _state.update {
                it.copy(
                    isLoading = false,
                    groupName = group?.name,
                    currencyCode = currency,
                    isFromReceipt = receipt != null,
                    amountInput = receipt?.let { r -> moneyFormatter.formatInput(r.total) }.orEmpty(),
                    description = receipt?.merchant.orEmpty(),
                    items = receipt?.items.orEmpty(),
                    tax = receipt?.tax,
                    people = people,
                    // In a group everyone splits by default; otherwise start with just you.
                    selectedIds = if (group != null) people.map(Person::id).toSet() else setOf(CURRENT_USER_ID),
                    itemAssignees = receipt?.items.orEmpty().associate { item -> item.id to setOf(CURRENT_USER_ID) },
                )
            }
        }
    }

    fun onAction(action: SplitAction) {
        when (action) {
            is SplitAction.AmountChanged -> _state.update { it.copy(amountInput = action.value, showAmountError = false) }
            is SplitAction.DescriptionChanged -> _state.update { it.copy(description = action.value, showDescriptionError = false) }
            SplitAction.ChooseEqual -> {
                val s = _state.value
                val total = moneyFormatter.parse(s.amountInput, s.currencyCode)
                if (total == null || s.description.isBlank()) {
                    _state.update { it.copy(showAmountError = total == null, showDescriptionError = s.description.isBlank()) }
                } else {
                    _state.update { it.copy(step = SplitStep.EQUAL, total = total).withEqualShares() }
                }
            }
            SplitAction.ChooseItems -> _state.update { it.copy(step = SplitStep.ITEMS) }
            is SplitAction.TogglePerson -> _state.update {
                it.copy(selectedIds = it.selectedIds.toggle(action.personId)).withEqualShares()
            }
            is SplitAction.ToggleItemAssignee -> _state.update {
                val current = it.itemAssignees[action.itemId].orEmpty()
                it.copy(itemAssignees = it.itemAssignees + (action.itemId to current.toggle(action.personId)), showAssignmentError = false)
            }
            SplitAction.Calculate -> calculateItems()
            SplitAction.Confirm -> confirm()
            SplitAction.Back -> {
                val step = _state.value.step
                if (step == SplitStep.CHOOSE_METHOD) {
                    viewModelScope.launch { _events.send(SplitEvent.Exit) }
                } else {
                    _state.update {
                        it.copy(step = if (step == SplitStep.ITEMS_RESULT) SplitStep.ITEMS else SplitStep.CHOOSE_METHOD)
                    }
                }
            }
        }
    }

    private fun Set<String>.toggle(id: String) = if (id in this) this - id else this + id

    private fun SplitUiState.withEqualShares(): SplitUiState =
        copy(shares = total?.let { SplitCalculator.equal(it.amountMinor, people.map(Person::id).filter(selectedIds::contains)) }.orEmpty())

    private fun calculateItems() {
        val s = _state.value
        if (s.items.any { s.itemAssignees[it.id].isNullOrEmpty() }) {
            _state.update { it.copy(showAssignmentError = true) }
            return
        }
        val order = s.people.map(Person::id)
        val items = s.items.map { item -> item.amount.amountMinor to order.filter(s.itemAssignees.getValue(item.id)::contains) }
        val shares = SplitCalculator.byItems(items, s.tax?.amountMinor ?: 0)
        val total = Money(shares.values.sum(), s.currencyCode)
        _state.update { it.copy(step = SplitStep.ITEMS_RESULT, shares = shares, total = total) }
    }

    private fun confirm() {
        val s = _state.value
        val total = s.total ?: return
        if (s.shares.isEmpty()) return
        viewModelScope.launch {
            try {
            if (groupId != null) {
                groupRepository.addExpense(
                    groupId,
                    GroupExpense(submissionId, s.description.trim(), total, CURRENT_USER_ID, s.shares, Instant.now()),
                )
            }
            // ponytail: outside a group only your own share is recorded; per-person debts need a group to live in.
            val myShare = s.shares[CURRENT_USER_ID]
            if (myShare != null && myShare > 0) {
                transactionRepository.upsert(
                    Transaction(
                        id = submissionId,
                        type = TransactionType.EXPENSE,
                        amount = Money(myShare, s.currencyCode),
                        title = s.description.trim(),
                        category = Category.FOOD.takeIf { s.isFromReceipt } ?: Category.OTHER,
                        paymentMethod = PaymentMethod.UPI,
                        timestamp = Instant.now(),
                        source = if (groupId != null) TransactionSource.GROUP else TransactionSource.RECEIPT.takeIf { s.isFromReceipt } ?: TransactionSource.MANUAL,
                        groupId = groupId,
                    ),
                )
            }
            _events.send(SplitEvent.Done)
            } catch (e: Exception) { _state.update { it.copy(error = e.message ?: "Could not save split") } }
        }
    }
}
