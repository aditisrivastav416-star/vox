package com.example.vox.domain.model

import java.time.Instant

enum class TransactionType { EXPENSE, INCOME }

enum class Category(val type: TransactionType) {
    FOOD(TransactionType.EXPENSE),
    GROCERIES(TransactionType.EXPENSE),
    SHOPPING(TransactionType.EXPENSE),
    TRAVEL(TransactionType.EXPENSE),
    TRANSPORT(TransactionType.EXPENSE),
    BILLS(TransactionType.EXPENSE),
    RENT(TransactionType.EXPENSE),
    ENTERTAINMENT(TransactionType.EXPENSE),
    EDUCATION(TransactionType.EXPENSE),
    HEALTH(TransactionType.EXPENSE),
    OTHER(TransactionType.EXPENSE),
    SALARY(TransactionType.INCOME),
    GIFT(TransactionType.INCOME),
    OTHER_INCOME(TransactionType.INCOME);

    companion object {
        fun forType(type: TransactionType) = entries.filter { it.type == type }
    }
}

enum class PaymentMethod { UPI, CARD, CASH, BANK }

enum class TransactionSource { MANUAL, VOICE, SMS, RECEIPT, GROUP }

data class Transaction(
    val id: String,
    val type: TransactionType,
    val amount: Money,
    val title: String,
    /** Null means the transaction still needs the user's attention. */
    val category: Category?,
    val paymentMethod: PaymentMethod,
    val timestamp: Instant,
    val source: TransactionSource,
    val groupId: String? = null,
    val notes: String? = null,
)
