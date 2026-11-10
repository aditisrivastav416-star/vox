package com.example.vox.feature.vox

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vox.domain.model.PaymentMethod
import com.example.vox.domain.model.Transaction
import com.example.vox.domain.model.TransactionSource
import com.example.vox.domain.model.VoiceResult
import com.example.vox.domain.model.VoiceInteraction
import com.example.vox.domain.model.TransactionType
import com.example.vox.domain.model.Category
import com.example.vox.domain.model.Money
import com.example.vox.domain.repository.TransactionRepository
import com.example.vox.domain.repository.VoiceAssistant
import com.example.vox.domain.repository.BudgetRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import java.text.NumberFormat
import java.util.Locale
import java.util.UUID

sealed interface VoxUiState {
    data object Idle : VoxUiState
    data object Listening : VoxUiState
    data class Recognized(val result: VoiceResult, val isSaving: Boolean = false, val error: String? = null,
        val paymentMethod: PaymentMethod = PaymentMethod.UPI, val date: LocalDate = LocalDate.now(), val notes: String = "") : VoxUiState
    data class Answer(val transcript: String, val reply: String) : VoxUiState
    data class Error(val message: String) : VoxUiState
}

sealed interface VoxAction {
    data object StartListening : VoxAction
    data object CancelListening : VoxAction
    data object Confirm : VoxAction
    data class UpdateResult(val amount: Long? = null, val title: String? = null, val category: Category? = null,
        val type: TransactionType? = null, val paymentMethod: PaymentMethod? = null, val date: LocalDate? = null,
        val notes: String? = null) : VoxAction

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
    private val budgetRepository: BudgetRepository,
    private val transactionsOnly: Boolean = false,
) : ViewModel() {
    private val _state = MutableStateFlow<VoxUiState>(VoxUiState.Idle)
    val state: StateFlow<VoxUiState> = _state.asStateFlow()
    private val _languageCode = MutableStateFlow("en-IN")
    val languageCode: StateFlow<String> = _languageCode.asStateFlow()

    private val _events = Channel<VoxEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var listenJob: Job? = null
    private var transactionId = UUID.randomUUID().toString()

    fun onAction(action: VoxAction) {
        if ((_state.value as? VoxUiState.Recognized)?.isSaving == true) return
        when (action) {
            is VoxAction.UpdateResult -> {
                val recognized = _state.value as? VoxUiState.Recognized ?: return
                val result = recognized.result
                val type = action.type ?: result.type
                _state.value = recognized.copy(result = result.copy(
                    amount = action.amount?.let { Money(it, result.amount.currencyCode) } ?: result.amount,
                    title = action.title ?: result.title,
                    category = action.category?.takeIf { it.type == type }
                        ?: result.category.takeIf { it.type == type } ?: Category.forType(type).first(),
                    type = type,
                ), paymentMethod = action.paymentMethod ?: recognized.paymentMethod,
                    date = action.date ?: recognized.date, notes = action.notes ?: recognized.notes, error = null)
            }
            VoxAction.StartListening -> {
                listenJob?.cancel()
                _state.value = VoxUiState.Listening
                listenJob = viewModelScope.launch {
                    try {
                        val interaction = voiceAssistant.listen(_languageCode.value)
                        val spokenText = when (interaction) {
                            is VoiceInteraction.TransactionCommand -> interaction.result.transcript
                            is VoiceInteraction.SpendingQuestion -> interaction.transcript
                            is VoiceInteraction.SetBudget -> interaction.transcript
                        }
                        _languageCode.value = if (spokenText.any { it in '\u0900'..'\u097F' }) "hi-IN" else "en-IN"
                        check(!transactionsOnly || interaction is VoiceInteraction.TransactionCommand) {
                            "Say a transaction, for example: spent 400 rupees on groceries."
                        }
                        when (interaction) {
                            is VoiceInteraction.TransactionCommand -> {
                                transactionId = UUID.randomUUID().toString()
                                _state.value = VoxUiState.Recognized(interaction.result)
                            }
                            is VoiceInteraction.SpendingQuestion -> {
                                val reply = spendingAnswer(interaction.transcript, transactionRepository.observeTransactions().first(),
                                    _languageCode.value, budgetRepository.observeBudgets().first()[YearMonth.now().toString() to "INR"])
                                _state.value = VoxUiState.Answer(interaction.transcript, reply)
                                voiceAssistant.speak(reply, _languageCode.value)
                            }
                            is VoiceInteraction.SetBudget -> {
                                val amount = interaction.amountMinor
                                budgetRepository.setBudget(YearMonth.now().toString(), "INR", amount)
                                val reply = if (_languageCode.value == "hi-IN") "इस महीने का बजट ${amount / 100} रुपये सेट हो गया है।"
                                else "Your budget for this month is now ${amount / 100} rupees."
                                _state.value = VoxUiState.Answer(interaction.transcript, reply)
                                voiceAssistant.speak(reply, _languageCode.value)
                            }
                        }
                    }
                    catch (e: CancellationException) { throw e }
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
        val recognized = _state.value as? VoxUiState.Recognized ?: return
        val result = recognized.result
        _state.value = recognized.copy(isSaving = true, error = null)
        viewModelScope.launch {
            try {
                transactionRepository.upsert(
                    Transaction(
                        id = transactionId,
                        type = result.type,
                        amount = result.amount,
                        title = result.title,
                        category = result.category,
                        paymentMethod = recognized.paymentMethod,
                        timestamp = recognized.date.atTime(LocalTime.now()).atZone(ZoneId.systemDefault()).toInstant(),
                        source = TransactionSource.VOICE,
                        notes = recognized.notes.trim().ifBlank { null },
                    ),
                )
                _state.value = VoxUiState.Idle
                _events.send(VoxEvent.Saved)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.value = recognized.copy(error = e.message ?: "Could not save transaction")
            }
        }
    }
}

internal fun spendingAnswer(question: String, transactions: List<Transaction>, languageCode: String = "en-IN", budgetMinor: Long? = null): String {
    val month = if ("last month" in question.lowercase()) YearMonth.now().minusMonths(1) else YearMonth.now()
    val expenses = transactions.filter {
        it.type == TransactionType.EXPENSE && it.amount.currencyCode == "INR" &&
            YearMonth.from(it.timestamp.atZone(ZoneId.systemDefault())) == month
    }
    val spent = expenses.sumOf { it.amount.amountMinor }
    val money = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-IN"))
    val rupees = money.format(spent / 100.0)
    val lower = question.lowercase()
    if (month == YearMonth.now() && listOf("budget", "afford", "बजट").any { it in lower }) {
        if (budgetMinor == null) return if (languageCode == "hi-IN") "इस महीने का बजट अभी सेट नहीं है।" else "You haven't set a budget for this month yet."
        val remaining = budgetMinor - spent
        val balance = money.format(kotlin.math.abs(remaining) / 100.0)
        return if (languageCode == "hi-IN") "इस महीने $rupees खर्च हुए हैं। बजट ${money.format(budgetMinor / 100.0)} है। ${if (remaining >= 0) "$balance बाकी हैं।" else "$balance बजट से ज़्यादा खर्च हुए हैं।"}"
        else "You've spent $rupees of your ${money.format(budgetMinor / 100.0)} budget this month. ${if (remaining >= 0) "$balance remains." else "You're $balance over budget."}"
    }
    val category = Category.entries.filter { it.type == TransactionType.EXPENSE }.firstOrNull {
        val words = when (it) {
            Category.GROCERIES -> listOf("grocer", "किरान", "राशन")
            Category.FOOD -> listOf("food", "restaurant", "खान")
            Category.TRANSPORT -> listOf("transport", "taxi", "uber", "बस")
            else -> listOf(it.name.lowercase())
        }
        words.any { word -> word in lower }
    }
    if (category != null) {
        val categoryTotal = expenses.filter { it.category == category }.sumOf { it.amount.amountMinor }
        return if (languageCode == "hi-IN") "${category.name.lowercase()} पर ${money.format(categoryTotal / 100.0)} खर्च हुए हैं।"
        else "You spent ${money.format(categoryTotal / 100.0)} on ${category.name.lowercase()} ${if (month == YearMonth.now()) "this month" else "last month"}."
    }
    if (listOf("habit", "pattern", "most", "biggest", "कैसा", "summari", "summariz", "summary", "सारांश").any { it in lower }) {
        val top = expenses.groupBy { it.category ?: Category.OTHER }.maxByOrNull { (_, rows) -> rows.sumOf { it.amount.amountMinor } }
        if (top != null) return if (languageCode == "hi-IN") "${if (month == YearMonth.now()) "इस महीने" else "पिछले महीने"} $rupees खर्च हुए हैं। सबसे ज़्यादा ${top.key.name.lowercase()} पर ${money.format(top.value.sumOf { it.amount.amountMinor } / 100.0)} खर्च हुए।"
        else "You spent $rupees ${if (month == YearMonth.now()) "this month" else "last month"}. Your largest category was ${top.key.name.lowercase()} at ${money.format(top.value.sumOf { it.amount.amountMinor } / 100.0)}."
    }
    return if (languageCode == "hi-IN") {
        if (month == YearMonth.now()) "इस महीने आपने $rupees खर्च किए हैं।" else "पिछले महीने आपने $rupees खर्च किए थे।"
    } else if (month == YearMonth.now()) "You have spent $rupees so far this month."
    else "You spent $rupees last month."
}
