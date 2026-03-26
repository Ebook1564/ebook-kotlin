package com.freeebookbundle.voice

import android.util.Log
import com.freeebookbundle.utils.ServerConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

class PiperSynthesizer {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val baseUrl = ServerConfig.getTtsUrl()
    
    var speechRate: Float = 1.0f

    companion object {
        private const val TAG = "PiperSynthesizer"
    }

    suspend fun synthesizeToFile(text: String, outputFile: File): Boolean = withContext(Dispatchers.IO) {
        try {
            val lengthScale = 1.0f / speechRate.coerceIn(0.5f, 3.0f)
            val jsonBody = JSONObject().apply {
                put("text", text)
                put("length_scale", lengthScale.toDouble())
            }.toString()
            val mediaType = "application/json; charset=utf-8".toMediaType()

            val request = Request.Builder()
                .url(baseUrl)
                .post(jsonBody.toRequestBody(mediaType))
                .header("Content-Type", "application/json")
                .build()

            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                response.body?.byteStream()?.use { inputStream ->
                    outputFile.outputStream().use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                Log.d(TAG, "Synthesis successful: ${outputFile.absolutePath}")
                true
            } else {
                Log.e(TAG, "Synthesis failed: ${response.code} ${response.message}")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Synthesis error: ${e.message}")
            false
        }
    }
}
