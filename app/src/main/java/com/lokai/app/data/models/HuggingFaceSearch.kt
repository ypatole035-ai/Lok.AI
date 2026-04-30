package com.lokai.app.data.models

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL

private const val TAG = "HuggingFaceSearch"

data class HFSearchResult(
    val modelId:          String,
    val sizeGb:           Float,
    val isCompatible:     Boolean,
    val isThinkingTrained:Boolean,
    val downloads:        Int
)

data class HFSearchState(
    val results:    List<HFSearchResult> = emptyList(),
    val isLoading:  Boolean              = false,
    val error:      String?              = null,
    val hasSearched:Boolean              = false
)

/**
 * Searches HuggingFace Hub for GGUF models matching [query].
 * Filters by the "gguf" tag and returns basic metadata.
 * Compatible flag is set based on [availableRamGb].
 */
object HuggingFaceSearch {

    private val THINKING_PATTERNS = listOf("r1", "qwq", "thinking", "reason", "deepseek-r")

    suspend fun search(
        query:          String,
        availableRamGb: Float,
        maxResults:     Int = 20
    ): List<HFSearchResult> = withContext(Dispatchers.IO) {
        try {
            val encoded = java.net.URLEncoder.encode("$query gguf", "UTF-8")
            val url = URL("https://huggingface.co/api/models?search=$encoded&filter=gguf&limit=$maxResults&sort=downloads&direction=-1")
            Log.d(TAG, "Searching: $url")

            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 8_000
            conn.readTimeout    = 8_000
            conn.setRequestProperty("Accept", "application/json")

            val responseCode = conn.responseCode
            if (responseCode != 200) {
                Log.e(TAG, "HTTP $responseCode")
                return@withContext emptyList()
            }

            val json = conn.inputStream.bufferedReader().readText()
            parseResults(json, availableRamGb)
        } catch (e: Exception) {
            Log.e(TAG, "Search failed", e)
            throw e
        }
    }

    private fun parseResults(json: String, availableRamGb: Float): List<HFSearchResult> {
        val arr = JSONArray(json)
        val out = mutableListOf<HFSearchResult>()

        for (i in 0 until arr.length()) {
            val obj       = arr.getJSONObject(i)
            val modelId   = obj.optString("modelId", "")
            val downloads = obj.optInt("downloads", 0)
            if (modelId.isBlank()) continue

            // Estimate size from safetensors/siblings metadata
            val siblings  = obj.optJSONArray("siblings")
            var sizeBytes = 0L
            if (siblings != null) {
                for (j in 0 until siblings.length()) {
                    val sib = siblings.getJSONObject(j)
                    val rfilename = sib.optString("rfilename", "")
                    if (rfilename.endsWith(".gguf")) {
                        sizeBytes += sib.optLong("size", 0L)
                    }
                }
            }

            val sizeGb = sizeBytes / 1_073_741_824f
            // If size unknown, estimate as 4GB (conservative)
            val effectiveSize = if (sizeGb < 0.01f) 4f else sizeGb

            val isCompatible = effectiveSize <= availableRamGb * 0.85f
            val idLower      = modelId.lowercase()
            val isThinking   = THINKING_PATTERNS.any { idLower.contains(it) }

            out.add(HFSearchResult(
                modelId           = modelId,
                sizeGb            = effectiveSize,
                isCompatible      = isCompatible,
                isThinkingTrained = isThinking,
                downloads         = downloads
            ))
        }
        return out
    }
}
