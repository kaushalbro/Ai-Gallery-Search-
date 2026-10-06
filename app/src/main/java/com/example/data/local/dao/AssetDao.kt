package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.AssetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AssetDao {
    @Query("SELECT * FROM assets ORDER BY created_at DESC")
    fun getAllAssetsFlow(): Flow<List<AssetEntity>>

    @Query("SELECT * FROM assets ORDER BY created_at DESC LIMIT :limit OFFSET :offset")
    suspend fun getAssetsPaged(limit: Int, offset: Int): List<AssetEntity>

    @Query("SELECT * FROM assets WHERE id = :id LIMIT 1")
    suspend fun getAssetById(id: Long): AssetEntity?

    @Query("SELECT * FROM assets WHERE platform_asset_id = :platformAssetId LIMIT 1")
    suspend fun getAssetByPlatformId(platformAssetId: String): AssetEntity?

    @Query("SELECT * FROM assets WHERE exact_hash = :hash AND canonical_document_id IS NOT NULL LIMIT 1")
    suspend fun findCanonicalByExactHash(hash: String): AssetEntity?

    @Query("SELECT * FROM assets WHERE indexing_status = :status ORDER BY created_at DESC")
    suspend fun getAssetsByStatus(status: String): List<AssetEntity>

    @Query("SELECT * FROM assets WHERE is_screenshot = 1 ORDER BY created_at DESC")
    fun getScreenshotsFlow(): Flow<List<AssetEntity>>

    @Query("SELECT COUNT(*) FROM assets")
    fun getTotalAssetCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM assets WHERE indexing_status = 'INDEXED'")
    fun getIndexedCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM assets WHERE indexing_status = 'QUEUED' OR indexing_status = 'DISCOVERED'")
    fun getQueuedCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM assets WHERE indexing_status = 'SKIPPED_DUPLICATE'")
    fun getDuplicateCountFlow(): Flow<Int>

    @Query("SELECT * FROM assets WHERE indexing_status NOT IN ('INDEXED', 'SKIPPED_DUPLICATE', 'FAILED_PERMANENT') ORDER BY created_at DESC LIMIT :limit")
    suspend fun getUnindexedAssets(limit: Int): List<AssetEntity>

    @Query("SELECT COUNT(*) FROM assets WHERE indexing_status LIKE 'FAILED%'")
    fun getFailedCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM assets")
    suspend fun getTotalAssetCount(): Int

    @Query("SELECT COUNT(*) FROM assets WHERE indexing_status = 'INDEXED'")
    suspend fun getIndexedCount(): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAsset(asset: AssetEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAssets(assets: List<AssetEntity>): List<Long>

    @Update
    suspend fun updateAsset(asset: AssetEntity)

    @Query("UPDATE assets SET indexing_status = :status, last_error = :error WHERE id = :id")
    suspend fun updateAssetStatus(id: Long, status: String, error: String? = null)

    @Query("UPDATE assets SET indexing_status = 'QUEUED' WHERE indexing_status = 'HASHING' OR indexing_status = 'OCR_RUNNING'")
    suspend fun resetStalledAssets()

    @Query("UPDATE assets SET retry_count = retry_count + 1 WHERE id = :id")
    suspend fun incrementRetryCount(id: Long)

    @Query("UPDATE assets SET indexing_status = :status, indexed_at = :indexedAt, canonical_document_id = :canonicalDocId, exact_hash = :exactHash, perceptual_hash = :pHash WHERE id = :id")
    suspend fun markAssetIndexed(
        id: Long,
        status: String,
        indexedAt: Long,
        canonicalDocId: Long?,
        exactHash: String?,
        pHash: String?
    )

    @Query("UPDATE assets SET indexing_status = 'QUEUED', retry_count = 0, last_error = null WHERE indexing_status LIKE 'FAILED%'")
    suspend fun retryAllFailedAssets()

    @Query("UPDATE assets SET indexing_status = 'QUEUED', retry_count = 0, last_error = null")
    suspend fun requeueAllAssets()

    @Query("DELETE FROM assets WHERE id = :id")
    suspend fun deleteAssetById(id: Long)

    @Query("DELETE FROM assets WHERE platform_asset_id LIKE 'seed_asset_%'")
    suspend fun deleteSeedAssets()

    @Query("DELETE FROM assets")
    suspend fun deleteAllAssets()
}
