package com.example.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "faces",
    indices = [
        Index(value = ["asset_id"]),
        Index(value = ["person_id"])
    ]
)
data class FaceEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "asset_id")
    val assetId: Long,

    @ColumnInfo(name = "person_id")
    val personId: Long? = null,

    @ColumnInfo(name = "box_left")
    val boxLeft: Float,

    @ColumnInfo(name = "box_top")
    val boxTop: Float,

    @ColumnInfo(name = "box_right")
    val boxRight: Float,

    @ColumnInfo(name = "box_bottom")
    val boxBottom: Float,

    @ColumnInfo(name = "confidence")
    val confidence: Float = 0.95f,

    @ColumnInfo(name = "yaw")
    val yaw: Float = 0f,

    @ColumnInfo(name = "pitch")
    val pitch: Float = 0f,

    @ColumnInfo(name = "roll")
    val roll: Float = 0f,

    @ColumnInfo(name = "embedding_json")
    val embeddingJson: String,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)
