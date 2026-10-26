package com.example.vox.data.remote

import android.content.Context
import com.example.vox.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class Api(context: Context) {
    private val prefs = context.getSharedPreferences("vox_session", Context.MODE_PRIVATE)
    val hasSession get() = prefs.getString("token", null) != null
    val userId get() = prefs.getString("user_id", "") ?: ""

    fun saveSession(result: JSONObject) {
        prefs.edit().putString("token", result.getString("token"))
            .putString("user_id", result.getJSONObject("user").getString("id")).apply()
    }

    fun clearSession() { prefs.edit().clear().apply() }

    suspend fun request(method: String, path: String, body: JSONObject? = null): JSONObject = withContext(Dispatchers.IO) {
        val connection = URL(BuildConfig.API_BASE_URL + path).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = method
            connection.connectTimeout = 10_000
            connection.readTimeout = 35_000
            connection.setRequestProperty("Accept", "application/json")
            prefs.getString("token", null)?.let { connection.setRequestProperty("Authorization", "Bearer $it") }
            if (body != null) {
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json")
                connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            }
            val stream = if (connection.responseCode in 200..299) connection.inputStream else connection.errorStream
            val response = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (connection.responseCode !in 200..299) {
                val message = runCatching { JSONObject(response).optString("message") }.getOrNull().orEmpty()
                error(message.ifBlank { "Server returned ${connection.responseCode}" })
            }
            if (response.trimStart().startsWith("[")) JSONObject().put("items", JSONArray(response))
            else JSONObject(response)
        } finally { connection.disconnect() }
    }
}

internal fun json(vararg pairs: Pair<String, Any?>) = JSONObject().apply {
    pairs.forEach { (key, value) -> put(key, value ?: JSONObject.NULL) }
}
internal fun JSONArray.objects(): List<JSONObject> = (0 until length()).map { getJSONObject(it) }
internal fun JSONObject.nullable(key: String): String? = if (isNull(key)) null else optString(key).ifBlank { null }
