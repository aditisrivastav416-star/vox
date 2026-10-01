package com.example.vox.feature.recurring

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vox.core.common.MoneyFormatter
import com.example.vox.domain.model.Category
import com.example.vox.domain.model.Frequency
import com.example.vox.domain.model.RecurringPayment
import com.example.vox.domain.model.TransactionType
import com.example.vox.domain.repository.RecurringPaymentRepository
import com.example.vox.domain.repository.UserRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID

data class CreateRecurringUiState(
    val name: String = "",
    val category: Category = Category.BILLS,
    val isFixedAmount: Boolean = true,
    val amount: String = "",
    val currencyCode: String = "INR",
    val frequency: Frequency = Frequency.MONTHLY,
    val customIntervalDays: String = "",
    val nextDueDate: LocalDate = LocalDate.now().plusDays(1),
    val remindDaysBefore: Boolean = true,
    val remindOnDueDate: Boolean = true,
    val showDatePicker: Boolean = false,
    val showNameError: Boolean = false,
    val showAmountError: Boolean = false,
    val showIntervalError: Boolean = false,
) {
    val categories: List<Category> get() = Category.forType(TransactionType.EXPENSE)
}

sealed interface CreateRecurringAction {
    data class NameChanged(val value: String) : CreateRecurringAction
    data class CategoryChanged(val category: Category) : CreateRecurringAction
    data class FixedAmountChanged(val isFixed: Boolean) : CreateRecurringAction
    data class AmountChanged(val value: String) : CreateRecurringAction
    data class FrequencyChanged(val frequency: Frequency) : CreateRecurringAction
    data class IntervalChanged(val value: String) : CreateRecurringAction
    data class ShowDatePicker(val show: Boolean) : CreateRecurringAction
    data class DueDateChanged(val date: LocalDate) : CreateRecurringAction
    data class RemindDaysBeforeChanged(val checked: Boolean) : CreateRecurringAction
    data class RemindOnDueDateChanged(val checked: Boolean) : CreateRecurringAction
    data object Save : CreateRecurringAction
}

sealed interface CreateRecurringEvent {
    data object Saved : CreateRecurringEvent
}

class CreateRecurringViewModel(
    private val repository: RecurringPaymentRepository,
    userRepository: UserRepository,
    private val moneyFormatter: MoneyFormatter = MoneyFormatter(),
) : ViewModel() {
    private val _state = MutableStateFlow(CreateRecurringUiState())
    val state: StateFlow<CreateRecurringUiState> = _state.asStateFlow()

    private val _events = Channel<CreateRecurringEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            userRepository.observeProfile().first()?.let { p -> _state.update { it.copy(currencyCode = p.currencyCode) } }
        }
    }

    fun onAction(action: CreateRecurringAction) {
        when (action) {
            is CreateRecurringAction.NameChanged -> _state.update { it.copy(name = action.value, showNameError = false) }
            is CreateRecurringAction.CategoryChanged -> _state.update { it.copy(category = action.category) }
            is CreateRecurringAction.FixedAmountChanged -> _state.update { it.copy(isFixedAmount = action.isFixed, showAmountError = false) }
            is CreateRecurringAction.AmountChanged -> _state.update { it.copy(amount = action.value, showAmountError = false) }
            is CreateRecurringAction.FrequencyChanged -> _state.update { it.copy(frequency = action.frequency, showIntervalError = false) }
            is CreateRecurringAction.IntervalChanged -> _state.update { it.copy(customIntervalDays = action.value, showIntervalError = false) }
            is CreateRecurringAction.ShowDatePicker -> _state.update { it.copy(showDatePicker = action.show) }
            is CreateRecurringAction.DueDateChanged -> _state.update { it.copy(nextDueDate = action.date) }
            is CreateRecurringAction.RemindDaysBeforeChanged -> _state.update { it.copy(remindDaysBefore = action.checked) }
            is CreateRecurringAction.RemindOnDueDateChanged -> _state.update { it.copy(remindOnDueDate = action.checked) }
            CreateRecurringAction.Save -> save()
        }
    }

    private fun save() {
        val s = _state.value
        // Amount is only required for fixed payments; "ask me every time" stores none.
        val amount = if (s.isFixedAmount) moneyFormatter.parse(s.amount, s.currencyCode) else null
        val interval = s.customIntervalDays.toIntOrNull()?.takeIf { it > 0 }
        val nameError = s.name.isBlank()
        val amountError = s.isFixedAmount && amount == null
        val intervalError = s.frequency == Frequency.CUSTOM && interval == null
        if (nameError || amountError || intervalError) {
            _state.update { it.copy(showNameError = nameError, showAmountError = amountError, showIntervalError = intervalError) }
            return
        }
        viewModelScope.launch {
            repository.upsert(
                RecurringPayment(
                    id = UUID.randomUUID().toString(),
                    name = s.name.trim(),
                    category = s.category,
                    amount = amount,
                    frequency = s.frequency,
                    customIntervalDays = interval.takeIf { s.frequency == Frequency.CUSTOM },
                    nextDueDate = s.nextDueDate,
                    remindDaysBefore = s.remindDaysBefore,
                    remindOnDueDate = s.remindOnDueDate,
                ),
            )
            _events.send(CreateRecurringEvent.Saved)
        }
    }
}
