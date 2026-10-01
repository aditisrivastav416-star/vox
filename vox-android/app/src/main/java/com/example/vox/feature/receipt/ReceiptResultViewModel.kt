package com.example.vox.feature.receipt

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vox.core.ui.UiState
import com.example.vox.core.ui.stateInUi
import com.example.vox.domain.model.Category
import com.example.vox.domain.model.PaymentMethod
import com.example.vox.domain.model.Receipt
import com.example.vox.domain.model.Transaction
import com.example.vox.domain.model.TransactionSource
import com.example.vox.domain.model.TransactionType
import com.example.vox.domain.repository.ReceiptRepository
import com.example.vox.domain.repository.TransactionRepository
import com.example.vox.navigation.VoxDestination
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

sealed interface ReceiptResultAction {
    data object AddTransaction : ReceiptResultAction
    data object SplitBill : ReceiptResultAction
}

sealed interface ReceiptResultEvent {
    data object Saved : ReceiptResultEvent
}

class ReceiptResultViewModel(
    savedStateHandle: SavedStateHandle,
    receiptRepository: ReceiptRepository,
    private val transactionRepository: TransactionRepository,
) : ViewModel() {
    val receiptId: String = checkNotNull(savedStateHandle[VoxDestination.ARG_RECEIPT_ID])

    /** Success(null) means the receipt is gone (e.g. process death with the in-memory fake). */
    val state: StateFlow<UiState<Receipt?>> = receiptRepository.observeReceipt(receiptId).stateInUi(viewModelScope)

    private val _events = Channel<ReceiptResultEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun onAction(action: ReceiptResultAction) {
        when (action) {
            ReceiptResultAction.AddTransaction -> {
                val receipt = (state.value as? UiState.Success)?.data ?: return
                viewModelScope.launch {
                    transactionRepository.upsert(
                        Transaction(
                            id = receipt.id,
                            type = TransactionType.EXPENSE,
                            amount = receipt.total,
                            title = receipt.merchant,
                            category = Category.FOOD,
                            paymentMethod = PaymentMethod.CARD,
                            timestamp = receipt.timestamp,
                            source = TransactionSource.RECEIPT,
                        ),
                    )
                    _events.send(ReceiptResultEvent.Saved)
                }
            }
            ReceiptResultAction.SplitBill -> Unit // navigation
        }
    }
}
