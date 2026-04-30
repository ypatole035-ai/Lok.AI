package com.lokai.app.data.models

import android.content.Context
import android.util.Log
import com.lokai.app.model.ModelEntry
import com.lokai.app.model.ModelVariant
import org.json.JSONObject

private const val TAG = "ModelCatalog"

/**
 * Parses the bundled assets/models.json and exposes the full model list.
 *
 * This is read once and cached. The catalog never changes at runtime —
 * it reflects the models included at build time.
 */
class ModelCatalog(private val context: Context) {

    private var _models: List<ModelEntry>? = null

    /**
     * Returns all models from the catalog.
     * Parsed lazily on first access, then cached.
     */
    fun allModels(): List<ModelEntry> {
        return _models ?: parseJson().also { _models = it }
    }

    /**
     * Returns (compatible, incompatible) lists filtered against [availableRamGb].
     *
     * compatible   — models whose smallest variant fits in available RAM
     * incompatible — models that need more RAM, along with the reason
     */
    fun filterByRam(availableRamGb: Float): Pair<List<ModelEntry>, List<ModelEntry>> {
        val all = allModels()
        val compatible   = all.filter { it.isCompatibleWith(availableRamGb) }
        val incompatible = all.filter { !it.isCompatibleWith(availableRamGb) }
        return compatible to incompatible
    }

    // ─── JSON parsing ─────────────────────────────────────────────────────────

    private fun parseJson(): List<ModelEntry> {
        return try {
            val json = context.assets.open("models.json")
                .bufferedReader()
                .use { it.readText() }

            val root   = JSONObject(json)
            val array  = root.getJSONArray("models")
            val result = mutableListOf<ModelEntry>()

            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                try {
                    result.add(parseModel(obj))
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to parse model at index $i: ${e.message}")
                }
            }

            Log.i(TAG, "Loaded ${result.size} models from catalog")
            result
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse models.json: ${e.message}")
            emptyList()
        }
    }

    private fun parseModel(obj: JSONObject): ModelEntry {
        val variantsArray = obj.getJSONArray("variants")
        val variants = mutableListOf<ModelVariant>()

        for (j in 0 until variantsArray.length()) {
            val v = variantsArray.getJSONObject(j)
            variants.add(
                ModelVariant(
                    quant         = v.getString("quant"),
                    sizeGb        = v.getDouble("sizeGb").toFloat(),
                    ramRequiredGb = v.getDouble("ramRequiredGb").toFloat(),
                    downloadUrl   = v.getString("downloadUrl"),
                    sha256        = v.optString("sha256", "")
                )
            )
        }

        return ModelEntry(
            id                = obj.getString("id"),
            name              = obj.getString("name"),
            family            = obj.getString("family"),
            params            = obj.getString("params"),
            bestFor           = obj.getString("bestFor"),
            minRamGb          = obj.getDouble("minRamGb").toFloat(),
            recommendedRamGb  = obj.getDouble("recommendedRamGb").toFloat(),
            thinkingTrained   = obj.getBoolean("thinkingTrained"),
            benchmarkNote     = obj.optString("benchmarkNote", ""),
            variants          = variants
        )
    }
}
