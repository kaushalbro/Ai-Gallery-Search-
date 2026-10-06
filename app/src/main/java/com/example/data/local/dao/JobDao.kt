package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.IndexingJobEntity

@Dao
interface JobDao {
    @Query("SELECT * FROM indexing_jobs WHERE state = 'PENDING' ORDER BY priority DESC, created_at ASC LIMIT :limit")
    suspend fun getNextPendingJobs(limit: Int): List<IndexingJobEntity>

    @Query("SELECT * FROM indexing_jobs WHERE asset_id = :assetId LIMIT 1")
    suspend fun getJobForAsset(assetId: Long): IndexingJobEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJob(job: IndexingJobEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJobs(jobs: List<IndexingJobEntity>)

    @Query("UPDATE indexing_jobs SET state = :state, started_at = :startedAt WHERE id = :jobId")
    suspend fun markJobStarted(jobId: Long, state: String, startedAt: Long)

    @Query("UPDATE indexing_jobs SET state = :state, completed_at = :completedAt, error = :error WHERE id = :jobId")
    suspend fun markJobCompleted(jobId: Long, state: String, completedAt: Long, error: String? = null)

    @Query("UPDATE indexing_jobs SET priority = :priority WHERE asset_id = :assetId")
    suspend fun updateAssetPriority(assetId: Long, priority: Int)

    @Query("DELETE FROM indexing_jobs WHERE asset_id = :assetId")
    suspend fun deleteJobForAsset(assetId: Long)

    @Query("DELETE FROM indexing_jobs")
    suspend fun deleteAllJobs()
}
