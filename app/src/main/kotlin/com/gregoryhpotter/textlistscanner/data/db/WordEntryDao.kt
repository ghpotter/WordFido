package com.gregoryhpotter.textlistscanner.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WordEntryDao {

    @Query("SELECT * FROM word_entries WHERE profileId = :profileId ORDER BY id ASC")
    fun watchByProfile(profileId: Long): Flow<List<WordEntryEntity>>

    @Query("SELECT * FROM word_entries WHERE profileId = :profileId ORDER BY id ASC")
    suspend fun getByProfile(profileId: Long): List<WordEntryEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entry: WordEntryEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(entries: List<WordEntryEntity>)

    @Query("DELETE FROM word_entries WHERE profileId = :profileId AND lower(text) = lower(:text)")
    suspend fun deleteByText(profileId: Long, text: String)

    @Query("DELETE FROM word_entries WHERE profileId = :profileId")
    suspend fun deleteAllByProfile(profileId: Long)

    @Query("UPDATE word_entries SET enabled = :enabled WHERE profileId = :profileId AND lower(text) = lower(:text)")
    suspend fun setEnabled(profileId: Long, text: String, enabled: Boolean)

    @Query("UPDATE word_entries SET color = :color WHERE profileId = :profileId AND lower(text) = lower(:text)")
    suspend fun updateColor(profileId: Long, text: String, color: Int)

    @Query("SELECT COUNT(*) FROM word_entries WHERE profileId = :profileId AND lower(text) = lower(:text)")
    suspend fun countByText(profileId: Long, text: String): Int
}
