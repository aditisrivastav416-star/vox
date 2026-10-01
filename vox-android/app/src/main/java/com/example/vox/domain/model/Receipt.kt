package com.example.vox.domain.model

import java.time.Instant

data class ReceiptItem(
    val id: String,
    val name: String,
    val amount: Money,
)

data class Receipt(
    val id: String,
    val merchant: String,
    val timestamp: Instant,
    val items: List<ReceiptItem>,
    val tax: Money,
) {
    val subtotal: Money get() = items.fold(Money.zero(tax.currencyCode)) { acc, item -> acc + item.amount }
    val total: Money get() = subtotal + tax
}
