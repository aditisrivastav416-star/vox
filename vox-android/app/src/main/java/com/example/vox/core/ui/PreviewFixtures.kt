package com.example.vox.core.ui

import com.example.vox.domain.model.CURRENT_USER_ID
import com.example.vox.domain.model.Category
import com.example.vox.domain.model.Group
import com.example.vox.domain.model.GroupExpense
import com.example.vox.domain.model.GroupType
import com.example.vox.domain.model.Money
import com.example.vox.domain.model.PaymentMethod
import com.example.vox.domain.model.Person
import com.example.vox.domain.model.Transaction
import com.example.vox.domain.model.TransactionSource
import com.example.vox.domain.model.TransactionType
import java.time.Instant

/** Sample values for @Preview functions only. */
object PreviewFixtures {
    fun inr(rupees: Long) = Money(rupees * 100, "INR")

    val expense = Transaction(
        id = "preview-expense", type = TransactionType.EXPENSE, amount = inr(740), title = "Swiggy",
        category = Category.FOOD, paymentMethod = PaymentMethod.UPI, timestamp = Instant.now(), source = TransactionSource.SMS,
    )

    val income = expense.copy(
        id = "preview-income", type = TransactionType.INCOME, amount = inr(20_000), title = "Bonus",
        category = Category.SALARY, paymentMethod = PaymentMethod.BANK,
    )

    val uncategorized = expense.copy(id = "preview-attention", title = "XYZ Enterprises", amount = inr(1_450), category = null)

    private val me = Person(CURRENT_USER_ID, "You")
    private val rahul = Person("rahul", "Rahul")
    private val riya = Person("riya", "Riya")

    val group = Group(
        id = "preview-group", name = "Goa Trip", type = GroupType.TRIP, currencyCode = "INR",
        members = listOf(me, rahul, riya),
        expenses = listOf(
            GroupExpense(
                "e1", "Dinner at Thalassa", inr(3_600), CURRENT_USER_ID,
                mapOf(CURRENT_USER_ID to 120_000L, "rahul" to 120_000L, "riya" to 120_000L), Instant.now(),
            ),
        ),
    )
}
