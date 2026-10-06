package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.FaceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FaceDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFaces(faces: List<FaceEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFace(face: FaceEntity): Long

    @Query("SELECT * FROM faces WHERE asset_id = :assetId ORDER BY id ASC")
    suspend fun getFacesForAsset(assetId: Long): List<FaceEntity>

    @Query("SELECT * FROM faces WHERE asset_id = :assetId ORDER BY id ASC")
    fun getFacesForAssetFlow(assetId: Long): Flow<List<FaceEntity>>

    @Query("SELECT * FROM faces WHERE person_id = :personId ORDER BY id ASC")
    suspend fun getFacesForPerson(personId: Long): List<FaceEntity>

    @Query("SELECT * FROM faces ORDER BY id ASC")
    suspend fun getAllFaces(): List<FaceEntity>

    @Query("SELECT * FROM faces WHERE person_id IS NULL ORDER BY id ASC")
    suspend fun getUnclusteredFaces(): List<FaceEntity>

    @Query("UPDATE faces SET person_id = :personId WHERE id = :faceId")
    suspend fun updateFacePersonId(faceId: Long, personId: Long)

    @Query("UPDATE faces SET person_id = :personId WHERE id IN (:faceIds)")
    suspend fun updateFacesPersonId(faceIds: List<Long>, personId: Long)

    @Query("DELETE FROM faces WHERE asset_id = :assetId")
    suspend fun deleteFacesForAsset(assetId: Long)

    @Query("SELECT COUNT(*) FROM faces")
    fun getTotalFaceCountFlow(): Flow<Int>

    @Query("SELECT COUNT(DISTINCT asset_id) FROM faces WHERE person_id = :personId")
    suspend fun getDistinctAssetCountForPerson(personId: Long): Int
}
