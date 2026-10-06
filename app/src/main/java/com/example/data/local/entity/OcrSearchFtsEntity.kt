package com.example.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.PrimaryKey

@Entity(tableName = "ocr_search_fts")
@Fts4
data class OcrSearchFtsEntity(
    @PrimaryKey
    @ColumnInfo(name = "rowid")
    val rowid: Long,

    @ColumnInfo(name = "asset_id")
    val assetId: Long,

    @ColumnInfo(name = "text")
    val text: String,

    @ColumnInfo(name = "filename")
    val filename: String,

    @ColumnInfo(name = "album_name")
    val albumName: String
)
