package com.example.vox.data.local

import android.content.Context
import android.net.Uri
import com.example.vox.domain.model.Money
import com.example.vox.domain.model.Receipt
import com.example.vox.domain.model.ReceiptItem
import com.example.vox.domain.repository.ReceiptRepository
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.devanagari.DevanagariTextRecognizerOptions
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

class LocalReceiptRepository(private val context: Context, private val store: DeviceStore) : ReceiptRepository {
    private val rows = MutableStateFlow(store.list("receipts").map { it.toReceipt() })
    override fun observeReceipt(id: String): Flow<Receipt?> = rows.map { list -> list.find { it.id == id } }

    override suspend fun scan(imageUri: String?): Receipt = withContext(Dispatchers.IO) {
        require(!imageUri.isNullOrBlank()) { "Choose or capture a receipt image" }
        val image = InputImage.fromFilePath(context, Uri.parse(imageUri))
        val latin = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        val devanagari = TextRecognition.getClient(DevanagariTextRecognizerOptions.Builder().build())
        try {
            val lines = listOf(latin, devanagari).flatMap { recognizer ->
                Tasks.await(recognizer.process(image)).textBlocks.flatMap { block -> block.lines.map { it.text } }
            }.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
            val receipt = parseReceiptLines(lines)
            rows.value = rows.value + receipt
            store.save("receipts", rows.value.map { it.toJson() })
            receipt
        } finally {
            latin.close()
            devanagari.close()
        }
    }
}

private val amountAtEnd = Regex("""(?:₹\s*|Rs\.?\s*)?([0-9][0-9,]*(?:\.[0-9]{1,2})?)\s*$""", RegexOption.IGNORE_CASE)
private fun amountMinor(line: String): Long? {
    val raw = amountAtEnd.find(line)?.groupValues?.get(1)?.replace(",", "") ?: return null
    return runCatching { BigDecimal(raw).movePointRight(2).longValueExact() }.getOrNull()?.takeIf { it > 0 }
}

internal fun parseReceiptLines(lines: List<String>): Receipt {
    val clean = lines.map { it.trim() }.filter { it.isNotEmpty() }
    require(clean.isNotEmpty()) { "No text found in receipt" }
    val totalLine = clean.lastOrNull {
        Regex("""\b(grand total|net total|amount due|total)\b|कुल|योग""", RegexOption.IGNORE_CASE).containsMatchIn(it) &&
            !Regex("""subtotal|sub total""", RegexOption.IGNORE_CASE).containsMatchIn(it) && amountMinor(it) != null
    }
    val total = totalLine?.let(::amountMinor) ?: clean.asReversed().firstNotNullOfOrNull(::amountMinor)
        ?: error("Could not find a receipt total")
    val tax = clean.lastOrNull { Regex("""\b(tax|gst|cgst|sgst)\b|कर""", RegexOption.IGNORE_CASE).containsMatchIn(it) }
        ?.let(::amountMinor) ?: 0L
    val items = clean.mapNotNull { line ->
        if (line == totalLine || Regex("""\b(total|subtotal|tax|gst|invoice|bill no|date|change|cash|card)\b|कुल|योग|कर""", RegexOption.IGNORE_CASE).containsMatchIn(line)) return@mapNotNull null
        val amount = amountMinor(line) ?: return@mapNotNull null
        val name = line.replace(amountAtEnd, "").trim(' ', ':', '-', '₹')
        if (name.isBlank() || amount > total) null else ReceiptItem(UUID.randomUUID().toString(), name, Money(amount, "INR"))
    }
    val recognizedSum = items.sumOf { it.amount.amountMinor }
    val finalItems = if (items.isNotEmpty() && recognizedSum + tax == total) items
        else listOf(ReceiptItem(UUID.randomUUID().toString(), "Receipt total", Money(total, "INR")))
    val finalTax = if (finalItems === items) tax else 0L
    return Receipt(UUID.randomUUID().toString(), clean.first(), Instant.now(), finalItems, Money(finalTax, "INR"))
}

private fun Receipt.toJson() = JSONObject().apply {
    put("id", id); put("merchant", merchant); put("timestamp", timestamp.toString())
    put("taxMinor", tax.amountMinor.toString())
    put("items", JSONArray(items.map { JSONObject().put("id", it.id).put("name", it.name)
        .put("amountMinor", it.amount.amountMinor.toString()) }))
}
private fun JSONObject.toReceipt() = Receipt(
    getString("id"), getString("merchant"), Instant.parse(getString("timestamp")),
    getJSONArray("items").let { array -> (0 until array.length()).map { i ->
        array.getJSONObject(i).let { item -> ReceiptItem(item.getString("id"), item.getString("name"),
            Money(item.getString("amountMinor").toLong(), "INR")) }
    } },
    Money(getString("taxMinor").toLong(), "INR"),
)
