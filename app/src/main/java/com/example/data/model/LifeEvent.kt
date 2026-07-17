package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "life_events")
data class LifeEvent(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val dateString: String,       // e.g. "2026-07-16"
    val timeString: String,       // e.g. "11:15 AM"
    val type: String,             // "DIARY", "AUDIO", "PHOTO", "MOOD", "MEDICINE", "EXERCISE", "SLEEP"
    val description: String,      // e.g. "كتبت ملاحظة جديدة", "أخذت دواء الضغط", "مارست الجري لمدة ٣٠ دقيقة"
    val moodIcon: String? = null, // mood emoji if relevant
    val value: String? = null,    // quantity or duration, e.g., "٨ ساعات" or "٤٥ دقيقة"
    val timestamp: Long = System.currentTimeMillis()
)
