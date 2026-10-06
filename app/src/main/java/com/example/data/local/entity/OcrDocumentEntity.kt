package com.example.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "ocr_documents",
    foreignKeys = [
        ForeignKey(
            entity = AssetEntity::class,
            parentColumns = ["id"],
            childColumns = ["asset_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["asset_id"])
    ]
)
data class OcrDocumentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "asset_id")
    val assetId: Long,

    @ColumnInfo(name = "normalized_text")
    val normalizedText: String,

    @ColumnInfo(name = "raw_text")
    val rawText: String,

    @ColumnInfo(name = "model_name")
    val modelName: String,

    @ColumnInfo(name = "model_version")
    val modelVersion: String,

    @ColumnInfo(name = "runtime_provider")
    val runtimeProvider: String,

    @ColumnInfo(name = "inference_ms")
    val inferenceMs: Long,

    @ColumnInfo(name = "block_count")
    val blockCount: Int,

    @ColumnInfo(name = "word_count")
    val wordCount: Int,

    @ColumnInfo(name = "created_at")
    val createdAt: Long
)
