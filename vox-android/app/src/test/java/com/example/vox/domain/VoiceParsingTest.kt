package com.example.vox.domain

import com.example.vox.data.remote.parseTransaction
import com.example.vox.data.remote.isSpendingQuestion
import com.example.vox.data.remote.parseBudgetCommand
import com.example.vox.data.remote.transcriptScore
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

    @Test fun understandsEnglishAndHindiBudgetCommands() {
        assertEquals(400_000L, parseBudgetCommand("Hello Vox, mera iss mahine ka budget 4000 set kar deejiye"))
        assertEquals(400_000L, parseBudgetCommand("इस महीने का बजट 4000 सेट कर दीजिए"))
        assertEquals(null, parseBudgetCommand("What is my budget?"))
    }

    @Test fun categorizesHindiAndHinglishTransactions() {
        assertEquals(Category.GROCERIES, parseTransaction("किराने पर ₹४०० खर्च किए").category)
        assertEquals(40_000L, parseTransaction("किराने पर ₹४०० खर्च किए").amount.amountMinor)
        assertEquals(Category.TRANSPORT, parseTransaction("auto ke liye 120 rupaye diye").category)
        assertEquals(Category.SALARY, parseTransaction("वेतन 5000 मिला").category)
        assertEquals(Category.HEALTH, parseTransaction("दवाई पर 300 खर्च किए").category)
    }

    @Test fun detectsCommonSpokenCategories() {
        val examples = mapOf(
            "400 lunch ke liye" to Category.FOOD,
            "400 लंच के लिए" to Category.FOOD,
            "Spent 200 on food" to Category.FOOD,
            "groceries 450" to Category.GROCERIES,
            "bought clothes for 700" to Category.SHOPPING,
            "electricity bill 900" to Category.BILLS,
            "paid rent 12000" to Category.RENT,
            "खाने पर २०० खर्च हुए" to Category.FOOD,
            "कपड़े पर ७०० खर्च हुए" to Category.SHOPPING,
            "बिजली बिल ९००" to Category.BILLS,
            "किराया १२०००" to Category.RENT,
        )
        examples.forEach { (phrase, category) -> assertEquals(phrase, category, parseTransaction(phrase).category) }
    }

    @Test fun prefersTranscriptWithRecognizedCategory() {
        assertEquals(true, transcriptScore("किराने पर ₹४०० खर्च", "hi-IN") > transcriptScore("400", "en-IN"))
        assertEquals(true, transcriptScore("Spent 400 on groceries", "en-IN") > transcriptScore("400", "hi-IN"))
    }
}
