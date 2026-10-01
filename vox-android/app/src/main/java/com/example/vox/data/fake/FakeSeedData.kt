package com.example.vox.data.fake

import com.example.vox.domain.model.CURRENT_USER_ID
import com.example.vox.domain.model.Category
import com.example.vox.domain.model.Frequency
import com.example.vox.domain.model.Group
import com.example.vox.domain.model.GroupExpense
import com.example.vox.domain.model.GroupType
import com.example.vox.domain.model.Money
import com.example.vox.domain.model.PaymentMethod
import com.example.vox.domain.model.Person
import com.example.vox.domain.model.Receipt
import com.example.vox.domain.model.ReceiptItem
import com.example.vox.domain.model.RecurringPayment
import com.example.vox.domain.model.Settlement
import com.example.vox.domain.model.Transaction
import com.example.vox.domain.model.TransactionSource
import com.example.vox.domain.model.TransactionType
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/** Sample data for fake repositories. Dates are relative to today so the UI always looks alive. */
internal object FakeSeedData {
    private const val INR = "INR"
    private val zone: ZoneId get() = ZoneId.systemDefault()

    fun inr(rupees: Long) = Money(rupees * 100, INR)

    private fun at(daysAgo: Long, hour: Int, minute: Int): Instant =
        LocalDate.now().minusDays(daysAgo).atTime(LocalTime.of(hour, minute)).atZone(zone).toInstant()

    val me = Person(CURRENT_USER_ID, "You")
    val rahul = Person("rahul", "Rahul", "+91 98200 11111")
    val riya = Person("riya", "Riya", "riya@example.com")
    val kabir = Person("kabir", "Kabir", "+91 98200 22222")
    val meera = Person("meera", "Meera", "meera@example.com")
    val arjun = Person("arjun", "Arjun", "+91 98200 33333")
    val contacts = listOf(rahul, riya, kabir, meera, arjun)

    fun transactions(): List<Transaction> {
        var n = 0
        fun tx(
            title: String, rupees: Long, category: Category?, daysAgo: Long, hour: Int, minute: Int,
            method: PaymentMethod = PaymentMethod.UPI,
            type: TransactionType = TransactionType.EXPENSE,
            source: TransactionSource = TransactionSource.SMS,
            groupId: String? = null,
        ) = Transaction("tx${n++}", type, inr(rupees), title, category, method, at(daysAgo, hour, minute), source, groupId)

        return listOf(
            tx("Swiggy", 740, Category.FOOD, 0, 20, 42),
            tx("Bonus", 20_000, Category.SALARY, 0, 15, 21, PaymentMethod.BANK, TransactionType.INCOME),
            tx("Uber", 284, Category.TRANSPORT, 0, 9, 12),
            tx("Electricity", 2_410, Category.BILLS, 1, 18, 5),
            tx("Dinner", 1_200, Category.FOOD, 1, 21, 30, source = TransactionSource.GROUP, groupId = "goa"),
            tx("XYZ Enterprises", 1_450, null, 2, 13, 10, PaymentMethod.CARD),
            tx("BigBasket", 2_340, Category.GROCERIES, 3, 11, 0),
            tx("Myntra", 4_120, Category.SHOPPING, 4, 22, 15, PaymentMethod.CARD),
            tx("IndiGo", 2_800, Category.TRAVEL, 6, 8, 45, PaymentMethod.CARD),
            tx("Zomato", 1_860, Category.FOOD, 8, 20, 10),
            tx("Unknown UPI transfer", 600, null, 9, 17, 40),
            tx("Salary", 85_000, Category.SALARY, 10, 10, 0, PaymentMethod.BANK, TransactionType.INCOME),
            tx("Cafe Coffee Day", 420, Category.FOOD, 12, 16, 20, PaymentMethod.CASH),
            tx("Netflix", 649, Category.ENTERTAINMENT, 14, 0, 5, PaymentMethod.CARD),
            tx("Restaurant", 2_220, Category.FOOD, 35, 21, 0),
            tx("Amazon", 3_499, Category.SHOPPING, 38, 14, 0, PaymentMethod.CARD),
            tx("Salary", 85_000, Category.SALARY, 40, 10, 0, PaymentMethod.BANK, TransactionType.INCOME),
            tx("Rent", 18_000, Category.RENT, 42, 9, 0, PaymentMethod.BANK),
        )
    }

    fun groups(): List<Group> = listOf(
        Group(
            id = "goa", name = "Goa Trip", type = GroupType.TRIP, currencyCode = INR,
            members = listOf(me, rahul, riya, kabir),
            expenses = listOf(
                expense("goa1", "Dinner at Thalassa", 4_800, CURRENT_USER_ID, listOf(me, rahul, riya, kabir), at(1, 21, 30)),
                expense("goa2", "Taxi", 1_200, rahul.id, listOf(me, rahul, riya, kabir), at(2, 10, 0)),
                expense("goa3", "Hotel", 12_000, CURRENT_USER_ID, listOf(me, rahul, riya, kabir), at(3, 14, 0)),
            ),
            settlements = listOf(
                Settlement(rahul.id, CURRENT_USER_ID, 270_000),
                Settlement(riya.id, CURRENT_USER_ID, 345_000),
                Settlement(kabir.id, CURRENT_USER_ID, 370_000),
            ),
        ),
        Group(
            id = "flat402", name = "Flat 402", type = GroupType.HOME, currencyCode = INR,
            members = listOf(me, meera, arjun),
            expenses = listOf(
                expense("flat1", "Groceries", 2_220, meera.id, listOf(me, meera, arjun), at(5, 19, 0)),
            ),
        ),
        Group(
            id = "college", name = "College Friends", type = GroupType.FRIENDS, currencyCode = INR,
            members = listOf(me, rahul, arjun),
            expenses = listOf(
                expense("col1", "Lunch", 1_500, CURRENT_USER_ID, listOf(me, rahul, arjun), at(20, 13, 0)),
            ),
            settlements = listOf(
                Settlement(rahul.id, CURRENT_USER_ID, 50_000),
                Settlement(arjun.id, CURRENT_USER_ID, 50_000),
            ),
        ),
    )

    private fun expense(id: String, description: String, rupees: Long, paidBy: String, people: List<Person>, time: Instant): GroupExpense {
        val total = rupees * 100
        return GroupExpense(id, description, Money(total, INR), paidBy, people.associate { it.id to total / people.size }, time)
    }

    fun recurringPayments(): List<RecurringPayment> {
        val today = LocalDate.now()
        return listOf(
            RecurringPayment("rent", "Rent", Category.RENT, inr(18_000), Frequency.MONTHLY, nextDueDate = today.plusDays(4)),
            RecurringPayment("electricity", "Electricity", Category.BILLS, null, Frequency.MONTHLY, nextDueDate = today.plusDays(7)),
            RecurringPayment("netflix", "Netflix", Category.ENTERTAINMENT, inr(649), Frequency.MONTHLY, nextDueDate = today.plusDays(13)),
            RecurringPayment("school", "School fees", Category.EDUCATION, inr(36_000), Frequency.QUARTERLY, nextDueDate = today.plusDays(45)),
        )
    }

    fun receipt(id: String) = Receipt(
        id = id,
        merchant = "Social",
        timestamp = Instant.now(),
        items = listOf(
            ReceiptItem("pasta", "Pasta", inr(650)),
            ReceiptItem("pizza", "Pizza", inr(900)),
            ReceiptItem("coke", "Coke", inr(300)),
        ),
        tax = inr(185),
    )
}
