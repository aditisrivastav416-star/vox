package com.example.vox

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.vox.data.local.DeviceStore
import com.example.vox.data.local.LocalReceiptRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class ReceiptOcrTest {
    @Test fun scansClearReceiptImage() = runBlocking<Unit> {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val store = DeviceStore(context)
        val priorReceipts = store.list("receipts")
        val bitmap = Bitmap.createBitmap(1200, 900, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 70f
            typeface = Typeface.DEFAULT_BOLD
        }
        canvas.drawText("CAFE", 70f, 140f, paint)
        canvas.drawText("Tea 100.00", 70f, 320f, paint)
        canvas.drawText("TOTAL 100.00", 70f, 520f, paint)
        val file = File(context.cacheDir, "ocr-test.png")
        file.parentFile?.mkdirs()
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        try {
            val receipt = LocalReceiptRepository(context, store).scan(Uri.fromFile(file).toString())
            assertEquals(10_000L, receipt.total.amountMinor)
        } finally {
            store.save("receipts", priorReceipts)
            file.delete()
        }
        Unit
    }
}
