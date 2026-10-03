package com.example.vox.feature.vox

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vox.domain.model.PaymentMethod
import com.example.vox.domain.model.Transaction
import com.example.vox.domain.model.TransactionSource
import com.example.vox.domain.model.VoiceResult
import com.example.vox.domain.repository.TransactionRepository
import com.example.vox.domain.repository.VoiceAssistant
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.UUID

sealed interface VoxUiState {
    data object Idle : VoxUiState
    data object Listening : VoxUiState
    data class Recognized(val result: VoiceResult) : VoxUiState
    data class Error(val message: String) : VoxUiState
}

sealed interface VoxAction {
    data object StartListening : VoxAction
    data object CancelListening : VoxAction
    data object Confirm : VoxAction

    // Navigation
    data object Close : VoxAction
    data class TypeInstead(val text: String = "") : VoxAction
    data object ScanReceipt : VoxAction
    data object SplitBill : VoxAction
    data class EditDetails(val result: VoiceResult) : VoxAction
}

sealed interface VoxEvent {
    data object Saved : VoxEvent
}

class VoxViewModel(
    private val voiceAssistant: VoiceAssistant,
    private val transactionRepository: TransactionRepository,
) : ViewModel() {
    private val _state = MutableStateFlow<VoxUiState>(VoxUiState.Idle)
    val state: StateFlow<VoxUiState> = _state.asStateFlow()

    private val _events = Channel<VoxEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var listenJob: Job? = null

    fun onAction(action: VoxAction) {
        when (action) {
            VoxAction.StartListening -> {
                listenJob?.cancel()
                _state.value = VoxUiState.Listening
                listenJob = viewModelScope.launch {
                    try { _state.value = VoxUiState.Recognized(voiceAssistant.listen()) }
                    catch (e: Exception) { _state.value = VoxUiState.Error(e.message ?: "Voice recognition unavailable") }
                }
            }
            VoxAction.CancelListening -> {
                listenJob?.cancel()
                _state.value = VoxUiState.Idle
            }
            VoxAction.Confirm -> confirm()
            VoxAction.Close, is VoxAction.TypeInstead, VoxAction.ScanReceipt, VoxAction.SplitBill,
            is VoxAction.EditDetails -> Unit // navigation
        }
    }

    private fun confirm() {
        val result = (_state.value as? VoxUiState.Recognized)?.result ?: return
        viewModelScope.launch {
            try {
            transactionRepository.upsert(
                Transaction(
                    id = UUID.randomUUID().toString(),
                    type = result.type,
                    amount = result.amount,
                    title = result.title,
                    category = result.category,
                    paymentMethod = PaymentMethod.UPI,
                    timestamp = Instant.now(),
                    source = TransactionSource.VOICE,
                ),
            )
            _events.send(VoxEvent.Saved)
            } catch (e: Exception) { _state.value = VoxUiState.Error(e.message ?: "Could not save transaction") }
        }
    }
}
