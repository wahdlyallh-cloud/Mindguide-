package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Delete
import com.example.data.model.DiaryEntry
import com.example.data.model.LifeEvent
import com.example.data.model.Habit
import com.example.data.model.HabitLog
import com.example.data.model.ChatMessage
import kotlinx.coroutines.flow.Flow

@Dao
interface DiaryDao {
    @Query("SELECT * FROM diary_entries ORDER BY timestamp DESC")
    fun getAllEntries(): Flow<List<DiaryEntry>>

    @Query("SELECT * FROM diary_entries WHERE id = :id")
    suspend fun getEntryById(id: Int): DiaryEntry?

    @Query("SELECT * FROM diary_entries WHERE dateString = :dateString ORDER BY timestamp ASC")
    fun getEntriesForDate(dateString: String): Flow<List<DiaryEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: DiaryEntry): Long

    @Update
    suspend fun updateEntry(entry: DiaryEntry)

    @Delete
    suspend fun deleteEntry(entry: DiaryEntry)

    @Query("DELETE FROM diary_entries WHERE id = :id")
    suspend fun deleteEntryById(id: Int)
}

@Dao
interface LifeEventDao {
    @Query("SELECT * FROM life_events ORDER BY timestamp DESC")
    fun getAllEvents(): Flow<List<LifeEvent>>

    @Query("SELECT * FROM life_events WHERE dateString = :dateString ORDER BY timestamp ASC")
    fun getEventsForDate(dateString: String): Flow<List<LifeEvent>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: LifeEvent): Long

    @Delete
    suspend fun deleteEvent(event: LifeEvent)

    @Query("DELETE FROM life_events WHERE id = :id")
    suspend fun deleteEventById(id: Int)
}

@Dao
interface HabitDao {
    @Query("SELECT * FROM habits ORDER BY timestamp DESC")
    fun getAllHabits(): Flow<List<Habit>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabit(habit: Habit): Long

    @Query("DELETE FROM habits WHERE id = :id")
    suspend fun deleteHabitById(id: Int)

    // Habit completion logging
    @Query("SELECT * FROM habit_logs")
    fun getAllHabitLogs(): Flow<List<HabitLog>>

    @Query("SELECT * FROM habit_logs WHERE dateString = :dateString")
    fun getHabitLogsForDate(dateString: String): Flow<List<HabitLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabitLog(log: HabitLog): Long

    @Query("DELETE FROM habit_logs WHERE habitId = :habitId AND dateString = :dateString")
    suspend fun deleteHabitLog(habitId: Int, dateString: String)
}

@Dao
interface ChatMessageDao {
    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC")
    fun getAllMessages(): Flow<List<ChatMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(msg: ChatMessage): Long

    @Query("DELETE FROM chat_messages")
    suspend fun clearMessages()
}

@Database(
    entities = [
        DiaryEntry::class,
        LifeEvent::class,
        Habit::class,
        HabitLog::class,
        ChatMessage::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun diaryDao(): DiaryDao
    abstract fun lifeEventDao(): LifeEventDao
    abstract fun habitDao(): HabitDao
    abstract fun chatMessageDao(): ChatMessageDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "yawmiyati_ai_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
