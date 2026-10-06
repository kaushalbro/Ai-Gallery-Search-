package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.PersonEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PersonDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPerson(person: PersonEntity): Long

    @Update
    suspend fun updatePerson(person: PersonEntity)

    @Query("SELECT * FROM persons ORDER BY face_count DESC, last_seen_at DESC")
    fun getAllPersonsFlow(): Flow<List<PersonEntity>>

    @Query("SELECT * FROM persons ORDER BY face_count DESC, last_seen_at DESC")
    suspend fun getAllPersons(): List<PersonEntity>

    @Query("SELECT * FROM persons WHERE id = :id LIMIT 1")
    suspend fun getPersonById(id: Long): PersonEntity?

    @Query("UPDATE persons SET name = :newName WHERE id = :personId")
    suspend fun renamePerson(personId: Long, newName: String)

    @Query("UPDATE persons SET face_count = :count, last_seen_at = :lastSeenAt WHERE id = :personId")
    suspend fun updateFaceCount(personId: Long, count: Int, lastSeenAt: Long)

    @Query("DELETE FROM persons WHERE id = :personId")
    suspend fun deletePerson(personId: Long)

    @Query("DELETE FROM persons WHERE face_count = 0")
    suspend fun deleteEmptyPersons()

    @Query("SELECT COUNT(*) FROM persons")
    fun getPersonCountFlow(): Flow<Int>
}
