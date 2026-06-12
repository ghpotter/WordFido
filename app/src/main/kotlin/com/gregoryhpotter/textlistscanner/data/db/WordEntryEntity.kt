package com.gregoryhpotter.textlistscanner.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.gregoryhpotter.textlistscanner.data.model.WordEntry

@Entity(
    tableName = "word_entries",
    foreignKeys = [ForeignKey(
        entity = WordProfileEntity::class,
        parentColumns = ["id"],
        childColumns = ["profileId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("profileId")]
)
data class WordEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val profileId: Long,
    val text: String,
    val color: Int,
    val enabled: Boolean = true
) {
    fun toDomain() = WordEntry(text = text, color = color, enabled = enabled)
}

fun WordEntry.toEntity(profileId: Long) = WordEntryEntity(
    profileId = profileId,
    text = text,
    color = color,
    enabled = enabled
)
