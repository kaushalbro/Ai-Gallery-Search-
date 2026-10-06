package com.example.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "assets",
    indices = [
        Index(value = ["platform_asset_id"], unique = true),
        Index(value = ["indexing_status"]),
        Index(value = ["exact_hash"]),
        Index(value = ["created_at"]),
        Index(value = ["canonical_document_id"]),
        Index(value = ["album_name"]),
        Index(value = ["is_screenshot"])
    ]
)
data class AssetEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "platform_asset_id")
    val platformAssetId: String,

    @ColumnInfo(name = "uri")
    val uri: String,

    @ColumnInfo(name = "filename")
    val filename: String,

    @ColumnInfo(name = "album_name")
    val albumName: String,

    @ColumnInfo(name = "mime_type")
    val mimeType: String,

    @ColumnInfo(name = "width")
    val width: Int,

    @ColumnInfo(name = "height")
    val height: Int,

    @ColumnInfo(name = "file_size")
    val fileSize: Long,

    @ColumnInfo(name = "created_at")
    val createdAt: Long,

    @ColumnInfo(name = "modified_at")
    val modifiedAt: Long,

    @ColumnInfo(name = "is_screenshot")
    val isScreenshot: Boolean = false,

    @ColumnInfo(name = "exact_hash")
    val exactHash: String? = null,

    @ColumnInfo(name = "perceptual_hash")
    val perceptualHash: String? = null,

    @ColumnInfo(name = "canonical_document_id")
    val canonicalDocumentId: Long? = null,

    @ColumnInfo(name = "indexing_status")
    val indexingStatus: String,

    @ColumnInfo(name = "last_error")
    val lastError: String? = null,

    @ColumnInfo(name = "retry_count")
    val retryCount: Int = 0,

    @ColumnInfo(name = "discovered_at")
    val discoveredAt: Long,

    @ColumnInfo(name = "indexed_at")
    val indexedAt: Long? = null
)
