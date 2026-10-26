package com.example.vox.domain.model

data class UserProfile(
    val name: String,
    val currencyCode: String,
    val phone: String? = null,
)

/** Result of interpreting one voice utterance. */
data class VoiceResult(
    val transcript: String,
    val type: TransactionType,
    val amount: Money,
    val title: String,
    val category: Category,
)

sealed interface VoiceInteraction {
    data class TransactionCommand(val result: VoiceResult) : VoiceInteraction
    data class SpendingQuestion(val transcript: String) : VoiceInteraction
    data class SetBudget(val transcript: String, val amountMinor: Long) : VoiceInteraction
}
