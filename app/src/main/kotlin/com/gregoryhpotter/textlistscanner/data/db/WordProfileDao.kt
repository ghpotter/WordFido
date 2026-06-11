package com.gregoryhpotter.textlistscanner.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WordProfileDao {

    @Query("SELECT * FROM profiles ORDER BY id ASC")
    fun watchAll(): Flow<List<WordProfileEntity>>

    @Query("SELECT * FROM profiles ORDER BY id ASC")
    suspend fun getAll(): List<WordProfileEntity>

    @Insert
    suspend fun insert(profile: WordProfileEntity): Long

    @Query("DELETE FROM profiles WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE profiles SET name = :name WHERE id = :id")
    suspend fun rename(id: Long, name: String)

    @Query("SELECT COUNT(*) FROM profiles")
    suspend fun count(): Int
}
