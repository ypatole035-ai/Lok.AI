package com.lokai.app.data.session

import androidx.room.*
import com.lokai.app.model.DownloadedModel

// ─── Entity ──────────────────────────────────────────────────────────────────

@Entity(tableName = "downloaded_models")
data class DownloadedModelEntity(
    @PrimaryKey val modelId: String,
    val name: String,
    val quant: String,
    val localPath: String,
    val sizeBytes: Long,
    val ramRequiredGb: Float,
    val thinkingTrained: Boolean,
    val downloadedAt: Long,
    val avgTokensPerSec: Float
)

// ─── DAO ─────────────────────────────────────────────────────────────────────

@Dao
interface DownloadedModelDao {

    @Query("SELECT * FROM downloaded_models ORDER BY downloadedAt DESC")
    suspend fun getAll(): List<DownloadedModelEntity>

    @Query("SELECT * FROM downloaded_models WHERE modelId = :modelId LIMIT 1")
    suspend fun getById(modelId: String): DownloadedModelEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: DownloadedModelEntity)

    @Query("DELETE FROM downloaded_models WHERE modelId = :modelId")
    suspend fun delete(modelId: String)

    @Query("UPDATE downloaded_models SET avgTokensPerSec = :tokensPerSec WHERE modelId = :modelId")
    suspend fun updateBenchmark(modelId: String, tokensPerSec: Float)
}

// ─── Mappers ─────────────────────────────────────────────────────────────────

fun DownloadedModelEntity.toDomain() = DownloadedModel(
    modelId        = modelId,
    name           = name,
    quant          = quant,
    localPath      = localPath,
    sizeBytes      = sizeBytes,
    ramRequiredGb  = ramRequiredGb,
    thinkingTrained= thinkingTrained,
    downloadedAt   = downloadedAt,
    avgTokensPerSec= avgTokensPerSec
)

fun DownloadedModel.toEntity() = DownloadedModelEntity(
    modelId        = modelId,
    name           = name,
    quant          = quant,
    localPath      = localPath,
    sizeBytes      = sizeBytes,
    ramRequiredGb  = ramRequiredGb,
    thinkingTrained= thinkingTrained,
    downloadedAt   = downloadedAt,
    avgTokensPerSec= avgTokensPerSec
)
