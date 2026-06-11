package com.gregoryhpotter.textlistscanner.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.gregoryhpotter.textlistscanner.data.model.WordProfile

@Entity(tableName = "profiles")
data class WordProfileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String
) {
    fun toDomain() = WordProfile(id = id, name = name)
}
