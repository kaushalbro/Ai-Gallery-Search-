package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.OcrBlockEntity
import com.example.data.local.entity.OcrDocumentEntity

@Dao
interface OcrDao {
    @Query("SELECT * FROM ocr_documents WHERE asset_id = :assetId LIMIT 1")
    suspend fun getDocumentForAsset(assetId: Long): OcrDocumentEntity?

    @Query("SELECT * FROM ocr_documents WHERE id = :id LIMIT 1")
    suspend fun getDocumentById(id: Long): OcrDocumentEntity?

    @Query("SELECT * FROM ocr_blocks WHERE asset_id = :assetId ORDER BY block_index ASC")
    suspend fun getBlocksForAsset(assetId: Long): List<OcrBlockEntity>

    @Query("SELECT * FROM ocr_blocks WHERE document_id = :docId ORDER BY block_index ASC")
    suspend fun getBlocksForDocument(docId: Long): List<OcrBlockEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(doc: OcrDocumentEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBlocks(blocks: List<OcrBlockEntity>)

    @Query("DELETE FROM ocr_documents WHERE asset_id = :assetId")
    suspend fun deleteDocumentForAsset(assetId: Long)

    @Query("DELETE FROM ocr_documents")
    suspend fun deleteAllDocuments()

    @Query("SELECT SUM(LENGTH(raw_text)) FROM ocr_documents")
    suspend fun getTotalExtractedChars(): Long?

    @Query("SELECT AVG(inference_ms) FROM ocr_documents WHERE inference_ms > 0")
    suspend fun getAverageInferenceTimeMs(): Float?
}
