package com.example.data.repository

import com.example.data.database.DiaryDao
import com.example.data.database.LifeEventDao
import com.example.data.database.HabitDao
import com.example.data.database.ChatMessageDao
import com.example.data.model.DiaryEntry
import com.example.data.model.LifeEvent
import com.example.data.model.Habit
import com.example.data.model.HabitLog
import com.example.data.model.ChatMessage
import kotlinx.coroutines.flow.Flow

class DiaryRepository(
    private val diaryDao: DiaryDao,
    private val lifeEventDao: LifeEventDao,
    private val habitDao: HabitDao,
    private val chatMessageDao: ChatMessageDao
) {
    // Diary Entries
    val allDiaryEntries: Flow<List<DiaryEntry>> = diaryDao.getAllEntries()

    fun getEntriesForDate(dateString: String): Flow<List<DiaryEntry>> =
        diaryDao.getEntriesForDate(dateString)

    suspend fun getEntryById(id: Int): DiaryEntry? = diaryDao.getEntryById(id)

    suspend fun insertDiaryEntry(entry: DiaryEntry): Long = diaryDao.insertEntry(entry)

    suspend fun updateDiaryEntry(entry: DiaryEntry) = diaryDao.updateEntry(entry)

    suspend fun deleteDiaryEntry(entry: DiaryEntry) = diaryDao.deleteEntry(entry)

    suspend fun deleteDiaryEntryById(id: Int) = diaryDao.deleteEntryById(id)


    // Life Events
    val allLifeEvents: Flow<List<LifeEvent>> = lifeEventDao.getAllEvents()

    fun getEventsForDate(dateString: String): Flow<List<LifeEvent>> =
        lifeEventDao.getEventsForDate(dateString)

    suspend fun insertLifeEvent(event: LifeEvent): Long = lifeEventDao.insertEvent(event)

    suspend fun deleteLifeEvent(event: LifeEvent) = lifeEventDao.deleteEvent(event)


    // Habits & Logs
    val allHabits: Flow<List<Habit>> = habitDao.getAllHabits()
    val allHabitLogs: Flow<List<HabitLog>> = habitDao.getAllHabitLogs()

    fun getHabitLogsForDate(dateString: String): Flow<List<HabitLog>> =
        habitDao.getHabitLogsForDate(dateString)

    suspend fun insertHabit(habit: Habit): Long = habitDao.insertHabit(habit)

    suspend fun deleteHabitById(id: Int) = habitDao.deleteHabitById(id)

    suspend fun insertHabitLog(log: HabitLog): Long = habitDao.insertHabitLog(log)

    suspend fun deleteHabitLog(habitId: Int, dateString: String) =
        habitDao.deleteHabitLog(habitId, dateString)


    // AI Chat History
    val allChatMessages: Flow<List<ChatMessage>> = chatMessageDao.getAllMessages()

    suspend fun insertChatMessage(msg: ChatMessage): Long = chatMessageDao.insertMessage(msg)

    suspend fun clearChatMessages() = chatMessageDao.clearMessages()
}
