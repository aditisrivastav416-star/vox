package com.example.vox.feature.recurring

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vox.core.ui.UiState
import com.example.vox.core.ui.stateInUi
import com.example.vox.domain.model.RecurringPayment
import com.example.vox.domain.repository.RecurringPaymentRepository
import kotlinx.coroutines.flow.StateFlow

class RecurringViewModel(repository: RecurringPaymentRepository) : ViewModel() {
    /** Already sorted by next due date. */
    val state: StateFlow<UiState<List<RecurringPayment>>> = repository.observePayments().stateInUi(viewModelScope)
}
