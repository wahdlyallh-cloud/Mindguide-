package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "habits")
data class Habit(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,             // e.g. "شرب الماء", "تأمل", "قراءة كتاب"
    val frequency: String = "DAILY", // DAILY, WEEKLY, MONTHLY
    val timestamp: Long = System.currentTimeMillis(),
    val reminderTime: String? = null, // e.g. "08:30" (24-hour format)
    val isReminderEnabled: Boolean = false
)
