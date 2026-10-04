package com.example.vox.feature.transaction

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vox.core.common.MoneyFormatter
import com.example.vox.domain.model.Category
import com.example.vox.domain.model.Money
import com.example.vox.domain.model.PaymentMethod
import com.example.vox.domain.model.Transaction
import com.example.vox.domain.model.TransactionSource
import com.example.vox.domain.model.TransactionType
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
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.UUID

data class TransactionFormUiState(
    val isEdit: Boolean = false,
    val isLoading: Boolean = false,
    val type: TransactionType = TransactionType.EXPENSE,
    val amount: String = "",
    val currencyCode: String = "INR",
    val title: String = "",
    val category: Category = Category.FOOD,
    val date: LocalDate = LocalDate.now(),
    val paymentMethod: PaymentMethod = PaymentMethod.UPI,
    val notes: String = "",
    val showAmountError: Boolean = false,
    val showTitleError: Boolean = false,
    val showDatePicker: Boolean = false,
    val error: String? = null,
) {
    val categories: List<Category> get() = Category.forType(type)
}

sealed interface TransactionFormAction {
    data class TypeChanged(val type: TransactionType) : TransactionFormAction
    data class AmountChanged(val value: String) : TransactionFormAction
    data class TitleChanged(val value: String) : TransactionFormAction
    data class CategoryChanged(val category: Category) : TransactionFormAction
    data class PaymentMethodChanged(val method: PaymentMethod) : TransactionFormAction
    data class NotesChanged(val value: String) : TransactionFormAction
    data class ShowDatePicker(val show: Boolean) : TransactionFormAction
    data class DateChanged(val date: LocalDate) : TransactionFormAction
    data object Save : TransactionFormAction
}

sealed interface TransactionFormEvent {
    data object Saved : TransactionFormEvent
}

class TransactionFormViewModel(
    savedStateHandle: SavedStateHandle,
    private val transactionRepository: TransactionRepository,
    private val userRepository: UserRepository,
    private val moneyFormatter: MoneyFormatter = MoneyFormatter(),
) : ViewModel() {
    private val editingId: String? = savedStateHandle[VoxDestination.ARG_TRANSACTION_ID]
    private var original: Transaction? = null

    private val _state = MutableStateFlow(TransactionFormUiState(isEdit = editingId != null, isLoading = true))
    val state: StateFlow<TransactionFormUiState> = _state.asStateFlow()

    private val _events = Channel<TransactionFormEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            val currency = userRepository.observeProfile().first()?.currencyCode ?: "INR"
            val existing = editingId?.let { transactionRepository.observeTransaction(it).first() }
            original = existing
            _state.update { s ->
                if (existing != null) {
                    s.copy(
                        isLoading = false,
                        type = existing.type,
                        amount = moneyFormatter.formatInput(existing.amount),
                        currencyCode = existing.amount.currencyCode,
                        title = existing.title,
                        category = existing.category ?: Category.forType(existing.type).first(),
                        date = existing.timestamp.atZone(ZoneId.systemDefault()).toLocalDate(),
                        paymentMethod = existing.paymentMethod,
                        notes = existing.notes.orEmpty(),
                    )
                } else {
                    // Optional prefill, e.g. from Vox "Edit details".
                    val type = savedStateHandle.get<String>(VoxDestination.ARG_TYPE)?.let(TransactionType::valueOf) ?: s.type
                    val amountMinor = savedStateHandle.get<String>(VoxDestination.ARG_AMOUNT)?.toLongOrNull()
                    s.copy(
                        isLoading = false,
                        type = type,
                        currencyCode = currency,
                        amount = amountMinor?.let { moneyFormatter.formatInput(Money(it, currency)) }.orEmpty(),
                        title = savedStateHandle.get<String>(VoxDestination.ARG_TITLE).orEmpty(),
                        category = savedStateHandle.get<String>(VoxDestination.ARG_CATEGORY)?.let(Category::valueOf)
                            ?: Category.forType(type).first(),
                    )
                }
            }
        }
    }

    fun onAction(action: TransactionFormAction) {
        when (action) {
            is TransactionFormAction.TypeChanged -> _state.update {
                it.copy(type = action.type, category = Category.forType(action.type).first())
            }
            is TransactionFormAction.AmountChanged -> _state.update { it.copy(amount = action.value, showAmountError = false) }
            is TransactionFormAction.TitleChanged -> _state.update { it.copy(title = action.value, showTitleError = false) }
            is TransactionFormAction.CategoryChanged -> _state.update { it.copy(category = action.category) }
            is TransactionFormAction.PaymentMethodChanged -> _state.update { it.copy(paymentMethod = action.method) }
            is TransactionFormAction.NotesChanged -> _state.update { it.copy(notes = action.value) }
            is TransactionFormAction.ShowDatePicker -> _state.update { it.copy(showDatePicker = action.show) }
            is TransactionFormAction.DateChanged -> _state.update { it.copy(date = action.date) }
            TransactionFormAction.Save -> save()
        }
    }

    private fun save() {
        val s = _state.value
        val amount = moneyFormatter.parse(s.amount, s.currencyCode)
        if (amount == null || s.title.isBlank()) {
            _state.update { it.copy(showAmountError = amount == null, showTitleError = s.title.isBlank()) }
            return
        }
        val zone = ZoneId.systemDefault()
        val time = original?.timestamp?.atZone(zone)?.toLocalTime() ?: LocalTime.now()
        val transaction = Transaction(
            id = original?.id ?: UUID.randomUUID().toString(),
            type = s.type,
            amount = amount,
            title = s.title.trim(),
            category = s.category,
            paymentMethod = s.paymentMethod,
            timestamp = s.date.atTime(time).atZone(zone).toInstant(),
            source = original?.source ?: TransactionSource.MANUAL,
            groupId = original?.groupId,
            notes = s.notes.trim().ifBlank { null },
        )
        viewModelScope.launch {
            try {
                transactionRepository.upsert(transaction)
                _events.send(TransactionFormEvent.Saved)
            } catch (e: Exception) { _state.update { it.copy(error = e.message ?: "Could not save transaction") } }
        }
    }
}
