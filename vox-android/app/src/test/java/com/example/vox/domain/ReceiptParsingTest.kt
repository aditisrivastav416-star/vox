package com.example.vox.domain

import com.example.vox.data.local.parseReceiptLines
import org.junit.Assert.assertEquals
import org.junit.Test

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
}
