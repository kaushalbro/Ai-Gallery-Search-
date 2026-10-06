package com.example.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "persons")
data class PersonEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "cover_face_id")
    val coverFaceId: Long? = null,

    @ColumnInfo(name = "cover_asset_uri")
    val coverAssetUri: String? = null,

    @ColumnInfo(name = "cover_box_left")
    val coverBoxLeft: Float = 0f,

    @ColumnInfo(name = "cover_box_top")
    val coverBoxTop: Float = 0f,

    @ColumnInfo(name = "cover_box_right")
    val coverBoxRight: Float = 1f,

    @ColumnInfo(name = "cover_box_bottom")
    val coverBoxBottom: Float = 1f,

    @ColumnInfo(name = "face_count")
    val faceCount: Int = 0,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "last_seen_at")
    val lastSeenAt: Long = System.currentTimeMillis()
)
