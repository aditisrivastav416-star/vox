package com.example.vox.feature.vox

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vox.domain.model.PaymentMethod
import com.example.vox.domain.model.Transaction
import com.example.vox.domain.model.TransactionSource
import com.example.vox.domain.model.VoiceResult
import com.example.vox.domain.model.VoiceInteraction
import com.example.vox.domain.model.TransactionType
import com.example.vox.domain.repository.TransactionRepository
import com.example.vox.domain.repository.VoiceAssistant
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import java.text.NumberFormat
import java.util.Locale
import java.util.UUID

sealed interface VoxUiState {
    data object Idle : VoxUiState
    data object Listening : VoxUiState
    data class Recognized(val result: VoiceResult) : VoxUiState
    data class Answer(val transcript: String, val reply: String) : VoxUiState
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
                    try {
                        when (val interaction = voiceAssistant.listen()) {
                            is VoiceInteraction.TransactionCommand -> _state.value = VoxUiState.Recognized(interaction.result)
                            is VoiceInteraction.SpendingQuestion -> {
                                val reply = spendingAnswer(interaction.transcript, transactionRepository.observeTransactions().first())
                                _state.value = VoxUiState.Answer(interaction.transcript, reply)
                                voiceAssistant.speak(reply)
                            }
                        }
                    }
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

internal fun spendingAnswer(question: String, transactions: List<Transaction>): String {
    val month = if ("last month" in question.lowercase()) YearMonth.now().minusMonths(1) else YearMonth.now()
    val spent = transactions.filter {
        it.type == TransactionType.EXPENSE && it.amount.currencyCode == "INR" &&
            YearMonth.from(it.timestamp.atZone(ZoneId.systemDefault())) == month
    }.sumOf { it.amount.amountMinor }
    val rupees = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-IN")).format(spent / 100.0)
    return if (month == YearMonth.now()) "You have spent $rupees so far this month."
    else "You spent $rupees last month."
}
