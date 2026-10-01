package com.example.vox.domain

import com.example.vox.core.common.MoneyFormatter
import com.example.vox.data.fake.FakeSeedData
import com.example.vox.domain.model.Category
import com.example.vox.domain.model.Money
import com.example.vox.domain.model.PaymentMethod
import com.example.vox.domain.model.SplitCalculator
import com.example.vox.domain.model.Transaction
import com.example.vox.domain.model.TransactionSource
import com.example.vox.domain.model.TransactionType
import com.example.vox.feature.home.SummaryMode
import com.example.vox.feature.home.buildSummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.YearMonth
import java.time.ZoneOffset
import java.util.Locale

class MoneyAndSplitTest {

    @Test
    fun equalSplit_sumsExactly() {
        assertEquals(mapOf("a" to 50_875L, "b" to 50_875L, "c" to 50_875L, "d" to 50_875L), SplitCalculator.equal(203_500, listOf("a", "b", "c", "d")))
        val odd = SplitCalculator.equal(100, listOf("a", "b", "c"))
        assertEquals(listOf(34L, 33L, 33L), odd.values.toList())
    }

    @Test
    fun itemSplit_prdExample() {
        // Pasta 650 (you), Pizza 900 (Rahul + Aditi), Coke 300 (you + Rahul), Tax 185 proportional.
        val shares = SplitCalculator.byItems(
            listOf(65_000L to listOf("me"), 90_000L to listOf("rahul", "aditi"), 30_000L to listOf("me", "rahul")),
            taxMinor = 18_500,
        )
        assertEquals(mapOf("me" to 88_000L, "rahul" to 66_000L, "aditi" to 49_500L), shares)
        assertEquals(203_500L, shares.values.sum())
    }

    @Test
    fun itemSplit_roundingLeftoverIsKept() {
        val shares = SplitCalculator.byItems(listOf(100L to listOf("a", "b", "c")), taxMinor = 10)
        assertEquals(110L, shares.values.sum())
    }

    @Test
    fun groupBalances_fromSeedData() {
        val goa = FakeSeedData.groups().first { it.id == "goa" }
        assertEquals(120_000L, goa.balanceWith("rahul").amountMinor)
        assertEquals(75_000L, goa.balanceWith("riya").amountMinor)
        assertEquals(50_000L, goa.balanceWith("kabir").amountMinor)
        val flat = FakeSeedData.groups().first { it.id == "flat402" }
        assertEquals(-74_000L, flat.netBalance.amountMinor)
        val college = FakeSeedData.groups().first { it.id == "college" }
        assertEquals(0L, college.netBalance.amountMinor)
    }

    @Test
    fun moneyFormatter_formatsAndParses() {
        val f = MoneyFormatter(Locale.US)
        assertEquals("₹18,000", f.format(Money(1_800_000, "INR")))
        assertEquals("₹508.75", f.format(Money(50_875, "INR")))
        assertEquals("-₹740", f.formatSigned(Money(-74_000, "INR")))
        assertEquals("+$5", f.formatSigned(Money(500, "USD")))
        assertEquals("508.75", f.formatInput(Money(50_875, "INR")))
        assertEquals(Money(125_050, "INR"), f.parse("1,250.50", "INR"))
        assertNull(f.parse("0", "INR"))
        assertNull(f.parse("-5", "INR"))
        assertNull(f.parse("1.234", "INR"))
        assertNull(f.parse("abc", "INR"))
        assertNull(f.parse("1.5", "JPY"))
    }

    @Test
    fun summary_totalsAndChange() {
        val month = YearMonth.of(2026, 10)
        fun tx(type: TransactionType, rupees: Long, day: Int, m: YearMonth = month, category: Category? = Category.FOOD) = Transaction(
            id = "$type$rupees$day$m", type = type, amount = Money(rupees * 100, "INR"), title = "t", category = category,
            paymentMethod = PaymentMethod.UPI, timestamp = m.atDay(day).atStartOfDay().toInstant(ZoneOffset.UTC), source = TransactionSource.MANUAL,
        )
        val txs = listOf(
            tx(TransactionType.EXPENSE, 880, 1),
            tx(TransactionType.EXPENSE, 200, 9, category = null),
            tx(TransactionType.INCOME, 5_000, 2, category = Category.SALARY),
            tx(TransactionType.EXPENSE, 1_000, 15, m = month.minusMonths(1)),
            Transaction("usd", TransactionType.EXPENSE, Money(100, "USD"), "t", null, PaymentMethod.CARD, month.atDay(3).atStartOfDay().toInstant(ZoneOffset.UTC), TransactionSource.MANUAL),
        )
        val spending = buildSummary(txs, month, SummaryMode.SPENDING, "INR", ZoneOffset.UTC)
        assertEquals(108_000L, spending.total.amountMinor)
        assertEquals(8, spending.changePercent)
        assertEquals(listOf(88_000L, 20_000L, 0L, 0L, 0L), spending.bars.map { it.value.amountMinor })
        assertEquals(listOf(Category.FOOD, Category.OTHER), spending.categories.map { it.category })

        val net = buildSummary(txs, month, SummaryMode.NET, "INR", ZoneOffset.UTC)
        assertEquals(392_000L, net.total.amountMinor)
        assertNull(net.changePercent)
    }
}
