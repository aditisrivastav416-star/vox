package com.example.vox.domain

import com.example.vox.data.local.parseReceiptLines
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class ReceiptParsingTest {
    @Test fun extractsItemsTaxAndTotal() {
        val receipt = parseReceiptLines(listOf("Cafe", "Tea 100.00", "Coffee 50.00", "GST 9.00", "TOTAL ₹159.00"))
        assertEquals("Cafe", receipt.merchant)
        assertEquals(2, receipt.items.size)
        assertEquals(900L, receipt.tax.amountMinor)
        assertEquals(15_900L, receipt.total.amountMinor)
    }

    @Test fun preservesTotalWhenItemsAreUnclear() {
        val receipt = parseReceiptLines(listOf("Store", "Amount Due 250.00"))
        assertEquals(25_000L, receipt.total.amountMinor)
        assertEquals("Receipt total", receipt.items.single().name)
    }

    @Test fun readsTotalOnFollowingLine() {
        val receipt = parseReceiptLines(listOf("Store", "Tea 100.00", "Coffee 50.00", "GST 9.00", "TOTAL", "159.00"))
        assertEquals(15_900L, receipt.total.amountMinor)
        assertEquals(2, receipt.items.size)
    }

    @Test fun readsDmartGrandTotalAndReceiptDate() {
        val receipt = parseReceiptLines(listOf(
            "D Mart", "DATE: 25/10/2023", "TOTAL ITEMS: 8", "GROSS TOTAL: ₹ 1810.00",
            "CGST (9.00%): ₹ 81.45", "SGST (9.00%): ₹ 81.45",
            "NET AMOUNT: ₹ 1972.90", "ROUND OFF: ₹ 0.10", "GRAND TOTAL: ₹ 1973.00",
            "PAID: CASH - 1973.00", "SAVINGS: You Saved ₹ 418.00",
        ))
        assertEquals("D Mart", receipt.merchant)
        assertEquals(197_300L, receipt.total.amountMinor)
        assertEquals(16_290L, receipt.tax.amountMinor)
        assertEquals(LocalDate.of(2023, 10, 25), receipt.timestamp.atZone(ZoneId.systemDefault()).toLocalDate())
    }

    @Test fun ignoresItemCountWhenGrandTotalAmountIsOnNextLine() {
        val receipt = parseReceiptLines(listOf(
            "D Mart", "DATE: 25/10/2023", "TOTAL ITEMS: 8", "GRAND TOTAL:", "₹ 1973.00",
        ))
        assertEquals(197_300L, receipt.total.amountMinor)
    }
}
