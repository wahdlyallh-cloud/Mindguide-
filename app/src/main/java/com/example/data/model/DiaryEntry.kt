package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "diary_entries")
data class DiaryEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val dateString: String,      // e.g. "2026-07-16" (used to group multiple entries per day)
    val timeString: String,      // e.g. "11:15 AM"
    val title: String,
    val content: String,
    val moods: String,           // comma-separated list of emojis chosen, e.g. "😊,❤️"
    val aiMoodAnalysis: String = "",  // AI-analyzed mental mood percentages, e.g. "68% حزن, 25% إرهاق"
    val hasAudio: Boolean = false,
    val audioPath: String? = null,
    val hasPhoto: Boolean = false,
    val photoPath: String? = null,
    val hasVideo: Boolean = false,
    val videoPath: String? = null,
    val hasPdf: Boolean = false,
    val pdfPath: String? = null,
    val webLinks: String? = null,     // comma-separated URLs
    val isEdited: Boolean = false,
    val editHistory: String? = null,  // text changes with timestamps
    val timestamp: Long = System.currentTimeMillis(),
    val importance: Int = 3,           // 1 to 5 star rating
    val isPinned: Boolean = false,
    val isArchived: Boolean = false,
    val isFavorite: Boolean = false,
    val isDeleted: Boolean = false,
    val reminderTimestamp: Long? = null,
    val entryType: String = "diary"
)
