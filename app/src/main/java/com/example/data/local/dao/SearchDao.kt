package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RawQuery
import androidx.sqlite.db.SupportSQLiteQuery
import com.example.data.local.entity.AssetEntity
import com.example.data.local.entity.OcrSearchFtsEntity

@Dao
interface SearchDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSearchDoc(searchDoc: OcrSearchFtsEntity)

    @Query("SELECT asset_id FROM ocr_search_fts WHERE ocr_search_fts MATCH :query")
    suspend fun searchFtsAssetIds(query: String): List<Long>

    @Query("""
        SELECT DISTINCT a.* FROM assets a
        LEFT JOIN ocr_documents d ON d.asset_id = a.id
        WHERE :keyword = ''
           OR d.normalized_text LIKE '%' || :keyword || '%'
           OR a.filename LIKE '%' || :keyword || '%'
           OR a.album_name LIKE '%' || :keyword || '%'
        ORDER BY a.created_at DESC
        LIMIT :limit OFFSET :offset
    """)
    suspend fun searchAssetsByText(keyword: String, limit: Int, offset: Int): List<AssetEntity>

    @Query("""
        SELECT DISTINCT a.* FROM assets a
        LEFT JOIN ocr_documents d ON d.asset_id = a.id
        WHERE a.is_screenshot = 1
          AND (:keyword = ''
               OR d.normalized_text LIKE '%' || :keyword || '%'
               OR a.filename LIKE '%' || :keyword || '%')
        ORDER BY a.created_at DESC
        LIMIT :limit OFFSET :offset
    """)
    suspend fun searchScreenshotAssetsByText(keyword: String, limit: Int, offset: Int): List<AssetEntity>

    @RawQuery
    suspend fun searchRaw(query: SupportSQLiteQuery): List<AssetEntity>

    @Query("DELETE FROM ocr_search_fts WHERE asset_id = :assetId")
    suspend fun deleteSearchDocForAsset(assetId: Long)

    @Query("DELETE FROM ocr_search_fts")
    suspend fun clearFtsIndex()
}
