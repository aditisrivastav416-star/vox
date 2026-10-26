package com.example.vox.domain

import com.example.vox.data.remote.parseTransaction
import com.example.vox.data.remote.isSpendingQuestion
import com.example.vox.feature.vox.spendingAnswer
import com.example.vox.domain.model.Category
import com.example.vox.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Test
import com.example.vox.domain.model.Transaction
import com.example.vox.domain.model.Money
import com.example.vox.domain.model.PaymentMethod
import com.example.vox.domain.model.TransactionSource
import java.time.Instant

class VoiceParsingTest {
    @Test fun parsesExpenseAndIncome() {
        val expense = parseTransaction("Spent ₹400 on groceries")
        assertEquals(40_000L, expense.amount.amountMinor)
        assertEquals(Category.GROCERIES, expense.category)
        val income = parseTransaction("Received 1200 rupees salary")
        assertEquals(TransactionType.INCOME, income.type)
        assertEquals(120_000L, income.amount.amountMinor)
    }

    @Test fun answersMonthlySpendingFromLocalTransactions() {
        assertEquals(true, isSpendingQuestion("Hey Vox, what has my spending been like this month?"))
        val transaction = Transaction("1", TransactionType.EXPENSE, Money(40_000, "INR"), "Groceries",
            Category.GROCERIES, PaymentMethod.UPI, Instant.now(), TransactionSource.MANUAL)
        assertEquals(true, spendingAnswer("What has spending been like this month?", listOf(transaction)).contains("400"))
    }
}
