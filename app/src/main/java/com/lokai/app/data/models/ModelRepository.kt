package com.lokai.app.data.models

import com.lokai.app.model.DeviceProfile
import com.lokai.app.model.ModelEntry

/**
 * Combines the [ModelCatalog] with a [DeviceProfile] to produce
 * filtered and sorted model lists for display.
 */
class ModelRepository(private val catalog: ModelCatalog) {

    /**
     * Returns models split into compatible/incompatible based on the device profile.
     *
     * Compatible models are sorted: thinking-trained first, then by minRamGb descending
     * (bigger = more capable within what the device handles).
     *
     * Incompatible models are sorted by minRamGb ascending (closest to fitting first).
     */
    fun getModelsForDevice(profile: DeviceProfile): ModelResult {
        val (compatible, incompatible) = catalog.filterByRam(profile.effectiveRamGb)

        val sortedCompatible = compatible.sortedWith(
            compareByDescending<ModelEntry> { it.thinkingTrained }
                .thenByDescending { it.minRamGb }
        )

        val sortedIncompatible = incompatible.sortedBy { it.minRamGb }

        return ModelResult(
            compatible   = sortedCompatible,
            incompatible = sortedIncompatible,
            deviceRamGb  = profile.effectiveRamGb
        )
    }

    /**
     * Returns a model by ID, or null if not found.
     */
    fun getById(id: String): ModelEntry? = catalog.allModels().firstOrNull { it.id == id }
}

data class ModelResult(
    val compatible:   List<ModelEntry>,
    val incompatible: List<ModelEntry>,
    /** The effective RAM used for filtering */
    val deviceRamGb:  Float
)
