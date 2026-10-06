package com.example.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "ocr_blocks",
    foreignKeys = [
        ForeignKey(
            entity = OcrDocumentEntity::class,
            parentColumns = ["id"],
            childColumns = ["document_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["document_id"]),
        Index(value = ["asset_id"])
    ]
)
data class OcrBlockEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "document_id")
    val documentId: Long,

    @ColumnInfo(name = "asset_id")
    val assetId: Long,

    @ColumnInfo(name = "block_index")
    val blockIndex: Int,

    @ColumnInfo(name = "text")
    val text: String,

    @ColumnInfo(name = "normalized_text")
    val normalizedText: String,

    @ColumnInfo(name = "confidence")
    val confidence: Float,

    @ColumnInfo(name = "x1")
    val x1: Float,

    @ColumnInfo(name = "y1")
    val y1: Float,

    @ColumnInfo(name = "x2")
    val x2: Float,

    @ColumnInfo(name = "y2")
    val y2: Float,

    @ColumnInfo(name = "x3")
    val x3: Float,

    @ColumnInfo(name = "y3")
    val y3: Float,

    @ColumnInfo(name = "x4")
    val x4: Float,

    @ColumnInfo(name = "y4")
    val y4: Float
)
