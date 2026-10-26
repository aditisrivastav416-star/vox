package com.example.vox.data.remote

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.media.MediaPlayer
import android.util.Base64
import androidx.core.content.ContextCompat
import com.example.vox.domain.model.Category
import com.example.vox.domain.model.Money
import com.example.vox.domain.model.TransactionType
import com.example.vox.domain.model.VoiceResult
import com.example.vox.domain.model.VoiceInteraction
import com.example.vox.domain.repository.VoiceAssistant
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import org.json.JSONObject
import java.io.File

class GnaniVoiceAssistant(private val context: Context, private val api: Api) : VoiceAssistant {
    override suspend fun listen(languageCode: String): VoiceInteraction {
        check(ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            "Microphone permission is required"
        }
        val file = File.createTempFile("vox-recording", ".m4a", context.cacheDir)
        val recorder = MediaRecorder(context)
        try {
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            recorder.setOutputFile(file.absolutePath)
            recorder.prepare()
            recorder.start()
            delay(5_000)
            recorder.stop()
            val audio = Base64.encodeToString(file.readBytes(), Base64.NO_WRAP)
            val transcript = api.request("POST", "/voice/transcribe", JSONObject()
                .put("audioBase64", audio).put("languageCode", languageCode)).getString("transcript")
            val budget = parseBudgetCommand(transcript)
            return when {
                budget != null -> VoiceInteraction.SetBudget(transcript, budget)
                isSpendingQuestion(transcript) -> VoiceInteraction.SpendingQuestion(transcript)
                else -> VoiceInteraction.TransactionCommand(parseTransaction(transcript))
            }
        } finally {
            recorder.release()
            file.delete()
        }
    }

    override suspend fun speak(text: String, languageCode: String) {
        val audio = api.request("POST", "/voice/speak", JSONObject().put("text", text)
            .put("languageCode", languageCode)).getString("audioBase64")
        val file = File.createTempFile("vox-reply", ".mp3", context.cacheDir)
        val player = MediaPlayer()
        try {
            file.writeBytes(Base64.decode(audio, Base64.DEFAULT))
            player.setDataSource(file.absolutePath)
            player.prepare()
            suspendCancellableCoroutine<Unit> { continuation ->
                player.setOnCompletionListener { if (continuation.isActive) continuation.resumeWith(Result.success(Unit)) }
                player.setOnErrorListener { _, _, _ ->
                    if (continuation.isActive) continuation.resumeWith(Result.failure(IllegalStateException("Could not play voice reply")))
                    true
                }
                continuation.invokeOnCancellation { runCatching { player.stop() } }
                player.start()
            }
        } finally {
            player.release()
            file.delete()
        }
    }
}

internal fun parseBudgetCommand(transcript: String): Long? {
    val lower = transcript.lowercase()
    if ("budget" !in lower && "बजट" !in lower) return null
    if (listOf("set", "सेट", "रख", "rak", "कर दी", "kar d", "बना").none { it in lower }) return null
    val value = Regex("[0-9][0-9,]*(?:\\.[0-9]{1,2})?").find(transcript)?.value?.replace(",", "")?.toDoubleOrNull()
        ?: error("Could not find the budget amount. Try: set my budget to 4000 rupees")
    val minor = (value * 100).toLong()
    require(minor > 0) { "Budget must be positive" }
    return minor
}

internal fun isSpendingQuestion(transcript: String): Boolean {
    val lower = transcript.lowercase()
    if (("कितना" in lower || "कैसा" in lower) && "खर्च" in lower) return true
    return listOf("how much", "what has", "what's", "what is", "tell me", "show me")
        .any { it in lower } && listOf("spend", "spent", "expense", "money").any { it in lower }
}

internal fun parseTransaction(transcript: String): VoiceResult {
    val amount = Regex("(?:₹|rs\\.?|rupees?\\s*)?([0-9][0-9,]*(?:\\.[0-9]{1,2})?)", RegexOption.IGNORE_CASE)
        .find(transcript)?.groupValues?.get(1)?.replace(",", "")?.toDoubleOrNull()
        ?: error("Could not find an amount. Try saying: spent 400 rupees on groceries")
    val minor = (amount * 100).toLong()
    require(minor > 0) { "Amount must be positive" }
    val lower = transcript.lowercase()
    val income = listOf("received", "earned", "salary", "income").any { it in lower }
    val type = if (income) TransactionType.INCOME else TransactionType.EXPENSE
    val category = when {
        income -> Category.OTHER_INCOME
        "grocery" in lower || "groceries" in lower -> Category.GROCERIES
        "food" in lower || "coffee" in lower || "lunch" in lower -> Category.FOOD
        "taxi" in lower || "uber" in lower || "bus" in lower -> Category.TRANSPORT
        "rent" in lower -> Category.RENT
        "bill" in lower -> Category.BILLS
        else -> Category.OTHER
    }
    return VoiceResult(transcript, type, Money(minor, "INR"), transcript, category)
}
