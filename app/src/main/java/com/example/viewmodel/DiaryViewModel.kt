package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.util.Log
import org.json.JSONObject
import org.json.JSONArray
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.model.ChatMessage
import com.example.data.model.DiaryEntry
import com.example.data.model.Habit
import com.example.data.model.HabitLog
import com.example.data.model.LifeEvent
import com.example.data.repository.DiaryRepository
import com.example.data.api.GeminiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class DiaryViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val repository = DiaryRepository(
        database.diaryDao(),
        database.lifeEventDao(),
        database.habitDao(),
        database.chatMessageDao()
    )

    private val prefs = application.getSharedPreferences("yawmiyati_prefs", Context.MODE_PRIVATE)

    var isFloatingBallEnabled by mutableStateOf(prefs.getBoolean("floating_ball_enabled", true))
        private set

    fun updateFloatingBallEnabled(enabled: Boolean) {
        isFloatingBallEnabled = enabled
        prefs.edit().putBoolean("floating_ball_enabled", enabled).apply()
    }

    var isGoogleBackupEnabled by mutableStateOf(prefs.getBoolean("google_backup_enabled", false))
        private set

    fun updateGoogleBackupEnabled(enabled: Boolean) {
        isGoogleBackupEnabled = enabled
        prefs.edit().putBoolean("google_backup_enabled", enabled).apply()
    }

    var googleAccountEmail by mutableStateOf(prefs.getString("google_account_email", "wahdlyallh@gmail.com") ?: "wahdlyallh@gmail.com")
        private set

    fun updateGoogleAccountEmail(email: String) {
        googleAccountEmail = email
        prefs.edit().putString("google_account_email", email).apply()
    }

    // Flows from Repository
    val allEntries: StateFlow<List<DiaryEntry>> = repository.allDiaryEntries.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val allEvents: StateFlow<List<LifeEvent>> = repository.allLifeEvents.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val allHabits: StateFlow<List<Habit>> = repository.allHabits.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val allHabitLogs: StateFlow<List<HabitLog>> = repository.allHabitLogs.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val chatMessages: StateFlow<List<ChatMessage>> = repository.allChatMessages.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Current Active Draft (Auto-saves continuously so nothing is lost)
    private var isSuppressAutoSave = false

    private inline fun suppressAutoSave(block: () -> Unit) {
        val wasSuppressed = isSuppressAutoSave
        isSuppressAutoSave = true
        try {
            block()
        } finally {
            isSuppressAutoSave = wasSuppressed
        }
    }

    private val _draftIdState = mutableStateOf<Int?>(null)
    var draftId: Int?
        get() = _draftIdState.value
        set(value) {
            _draftIdState.value = value
            if (!isSuppressAutoSave) autoSaveCurrentDraft()
        }

    private val _draftTitleState = mutableStateOf("")
    var draftTitle: String
        get() = _draftTitleState.value
        set(value) {
            _draftTitleState.value = value
            if (!isSuppressAutoSave) autoSaveCurrentDraft()
        }

    private val _draftContentState = mutableStateOf("")
    var draftContent: String
        get() = _draftContentState.value
        set(value) {
            _draftContentState.value = value
            if (!isSuppressAutoSave) autoSaveCurrentDraft()
        }

    private val _draftSelectedMoodsState = mutableStateOf<List<String>>(emptyList())
    var draftSelectedMoods: List<String>
        get() = _draftSelectedMoodsState.value
        set(value) {
            _draftSelectedMoodsState.value = value
            if (!isSuppressAutoSave) autoSaveCurrentDraft()
        }

    private val _draftImportanceState = mutableStateOf(3)
    var draftImportance: Int
        get() = _draftImportanceState.value
        set(value) {
            _draftImportanceState.value = value
            if (!isSuppressAutoSave) autoSaveCurrentDraft()
        }

    private val _draftAudioPathState = mutableStateOf<String?>(null)
    var draftAudioPath: String?
        get() = _draftAudioPathState.value
        set(value) {
            _draftAudioPathState.value = value
            if (!isSuppressAutoSave) autoSaveCurrentDraft()
        }

    private val _draftPhotoPathState = mutableStateOf<String?>(null)
    var draftPhotoPath: String?
        get() = _draftPhotoPathState.value
        set(value) {
            _draftPhotoPathState.value = value
            if (!isSuppressAutoSave) autoSaveCurrentDraft()
        }

    private val _draftVideoPathState = mutableStateOf<String?>(null)
    var draftVideoPath: String?
        get() = _draftVideoPathState.value
        set(value) {
            _draftVideoPathState.value = value
            if (!isSuppressAutoSave) autoSaveCurrentDraft()
        }

    private val _draftPdfPathState = mutableStateOf<String?>(null)
    var draftPdfPath: String?
        get() = _draftPdfPathState.value
        set(value) {
            _draftPdfPathState.value = value
            if (!isSuppressAutoSave) autoSaveCurrentDraft()
        }

    private val _draftWebLinksState = mutableStateOf("")
    var draftWebLinks: String
        get() = _draftWebLinksState.value
        set(value) {
            _draftWebLinksState.value = value
            if (!isSuppressAutoSave) autoSaveCurrentDraft()
        }

    private val _draftDrawingDataState = mutableStateOf<String?>(null)
    var draftDrawingData: String?
        get() = _draftDrawingDataState.value
        set(value) {
            _draftDrawingDataState.value = value
            if (!isSuppressAutoSave) autoSaveCurrentDraft()
        }

    private val _draftAiMoodAnalysisState = mutableStateOf("")
    var draftAiMoodAnalysis: String
        get() = _draftAiMoodAnalysisState.value
        set(value) {
            _draftAiMoodAnalysisState.value = value
            if (!isSuppressAutoSave) autoSaveCurrentDraft()
        }

    private val _draftIsPinnedState = mutableStateOf(false)
    var draftIsPinned: Boolean
        get() = _draftIsPinnedState.value
        set(value) {
            _draftIsPinnedState.value = value
            if (!isSuppressAutoSave) autoSaveCurrentDraft()
        }

    private val _draftIsArchivedState = mutableStateOf(false)
    var draftIsArchived: Boolean
        get() = _draftIsArchivedState.value
        set(value) {
            _draftIsArchivedState.value = value
            if (!isSuppressAutoSave) autoSaveCurrentDraft()
        }

    private val _draftIsFavoriteState = mutableStateOf(false)
    var draftIsFavorite: Boolean
        get() = _draftIsFavoriteState.value
        set(value) {
            _draftIsFavoriteState.value = value
            if (!isSuppressAutoSave) autoSaveCurrentDraft()
        }

    private val _draftIsDeletedState = mutableStateOf(false)
    var draftIsDeleted: Boolean
        get() = _draftIsDeletedState.value
        set(value) {
            _draftIsDeletedState.value = value
            if (!isSuppressAutoSave) autoSaveCurrentDraft()
        }

    private val _draftReminderTimestampState = mutableStateOf<Long?>(null)
    var draftReminderTimestamp: Long?
        get() = _draftReminderTimestampState.value
        set(value) {
            _draftReminderTimestampState.value = value
            if (!isSuppressAutoSave) autoSaveCurrentDraft()
        }

    private val _draftEntryTypeState = mutableStateOf("diary")
    var draftEntryType: String
        get() = _draftEntryTypeState.value
        set(value) {
            _draftEntryTypeState.value = value
            if (!isSuppressAutoSave) autoSaveCurrentDraft()
        }

    /**
     * Auto-saves the current draft properties immediately to the Room SQLite database
     */
    fun autoSaveCurrentDraft() {
        // Skip auto-saving completely empty new drafts
        if (draftId == null && draftTitle.isEmpty() && draftContent.isEmpty() && 
            draftSelectedMoods.isEmpty() && draftAudioPath.isNullOrEmpty() && 
            draftPhotoPath.isNullOrEmpty() && draftVideoPath.isNullOrEmpty() && 
            draftPdfPath.isNullOrEmpty() && draftDrawingData.isNullOrEmpty() && 
            draftWebLinks.isEmpty()
        ) {
            return
        }

        viewModelScope.launch {
            val dateStr = getCurrentDateString()
            val timeStr = getCurrentTimeString()
            val moodsJoined = draftSelectedMoods.joinToString(",")

            val entry = DiaryEntry(
                id = draftId ?: 0,
                dateString = dateStr,
                timeString = timeStr,
                title = draftTitle.ifEmpty { if (draftEntryType == "diary") "يومية بدون عنوان" else "خاطرة بدون عنوان" },
                content = draftContent,
                moods = moodsJoined,
                aiMoodAnalysis = draftAiMoodAnalysis,
                importance = draftImportance,
                hasAudio = !draftAudioPath.isNullOrEmpty(),
                audioPath = draftAudioPath,
                hasPhoto = !draftPhotoPath.isNullOrEmpty(),
                photoPath = draftPhotoPath,
                hasVideo = !draftVideoPath.isNullOrEmpty(),
                videoPath = draftVideoPath,
                hasPdf = !draftPdfPath.isNullOrEmpty() || !draftDrawingData.isNullOrEmpty(),
                pdfPath = if (!draftDrawingData.isNullOrEmpty()) "drawing:$draftDrawingData" else draftPdfPath,
                webLinks = draftWebLinks.ifEmpty { null },
                isPinned = draftIsPinned,
                isArchived = draftIsArchived,
                isFavorite = draftIsFavorite,
                isDeleted = draftIsDeleted,
                reminderTimestamp = draftReminderTimestamp,
                entryType = draftEntryType
            )

            if (draftId == null) {
                // Insert new entry into the database and capture the auto-generated ID
                val newId = repository.insertDiaryEntry(entry).toInt()
                // Update the ID without triggering a re-entrant auto-save
                suppressAutoSave {
                    draftId = newId
                }
            } else {
                // Update the existing entry in the database
                repository.updateDiaryEntry(entry)
            }
        }
    }

    // Chat Consultant State
    private val _isChatLoading = MutableStateFlow(false)
    val isChatLoading: StateFlow<Boolean> = _isChatLoading.asStateFlow()

    var hasChatAccessPermission by mutableStateOf(true)

    // Therapy Report State
    private val _isReportLoading = MutableStateFlow(false)
    val isReportLoading: StateFlow<Boolean> = _isReportLoading.asStateFlow()

    private val _generatedReport = MutableStateFlow("")
    val generatedReport: StateFlow<String> = _generatedReport.asStateFlow()

    // Streak and Metrics
    val streakCount: StateFlow<Int> = allEntries.combine(MutableStateFlow(System.currentTimeMillis())) { entries, _ ->
        calculateStreak(entries)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    init {
        // Pre-populate default habits if database is empty
        viewModelScope.launch {
            val list = repository.allHabits.first()
            if (list.isEmpty()) {
                repository.insertHabit(Habit(name = "شرب الماء (٨ أكواب)"))
                repository.insertHabit(Habit(name = "ممارسة الرياضة (٣٠ دقيقة)"))
                repository.insertHabit(Habit(name = "التأمل والاسترخاء"))
                repository.insertHabit(Habit(name = "كتابة اليوميات"))
                repository.insertHabit(Habit(name = "النوم لـ ٨ ساعات"))
            }
        }
    }

    // Helper to get formatted date string and time string
    fun getCurrentDateString(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    }

    fun getCurrentTimeString(): String {
        return SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
    }

    fun getDayOfWeekArabic(dateStr: String): String {
        return try {
            val date = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(dateStr) ?: Date()
            val dayName = SimpleDateFormat("EEEE", Locale("ar")).format(date)
            dayName
        } catch (e: Exception) {
            "اليوم"
        }
    }

    fun getFormattedDateArabic(dateStr: String): String {
        return try {
            val date = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(dateStr) ?: Date()
            SimpleDateFormat("dd MMMM yyyy", Locale("ar")).format(date)
        } catch (e: Exception) {
            dateStr
        }
    }

    /**
     * Auto-save or insert current entry
     */
    fun saveCurrentEntry(onComplete: () -> Unit) {
        viewModelScope.launch {
            val dateStr = getCurrentDateString()
            val timeStr = getCurrentTimeString()
            val moodsJoined = draftSelectedMoods.joinToString(",")

            val entry = DiaryEntry(
                id = draftId ?: 0,
                dateString = dateStr,
                timeString = timeStr,
                title = draftTitle.ifEmpty { if (draftEntryType == "diary") "يومية بدون عنوان" else "خاطرة بدون عنوان" },
                content = draftContent,
                moods = moodsJoined,
                aiMoodAnalysis = draftAiMoodAnalysis,
                importance = draftImportance,
                hasAudio = !draftAudioPath.isNullOrEmpty(),
                audioPath = draftAudioPath,
                hasPhoto = !draftPhotoPath.isNullOrEmpty(),
                photoPath = draftPhotoPath,
                hasVideo = !draftVideoPath.isNullOrEmpty(),
                videoPath = draftVideoPath,
                hasPdf = !draftPdfPath.isNullOrEmpty() || !draftDrawingData.isNullOrEmpty(),
                pdfPath = if (!draftDrawingData.isNullOrEmpty()) "drawing:$draftDrawingData" else draftPdfPath,
                webLinks = draftWebLinks.ifEmpty { null },
                isPinned = draftIsPinned,
                isArchived = draftIsArchived,
                isFavorite = draftIsFavorite,
                isDeleted = draftIsDeleted,
                reminderTimestamp = draftReminderTimestamp,
                entryType = draftEntryType
            )

            val newId = if (draftId == null) {
                repository.insertDiaryEntry(entry).toInt()
            } else {
                repository.updateDiaryEntry(entry)
                draftId!!
            }
            
            // Add Life Timeline Event
            val desc = if (draftEntryType == "diary") {
                "كتبت ملاحظة: ${entry.title.take(20)}..."
            } else {
                "كتبت خاطرة: ${entry.title.take(20)}..."
            }
            repository.insertLifeEvent(
                LifeEvent(
                    dateString = dateStr,
                    timeString = timeStr,
                    type = "DIARY",
                    description = desc,
                    moodIcon = draftSelectedMoods.firstOrNull() ?: "📝"
                )
            )

            // Trigger Background AI Mood Analysis if not already computed
            if (draftAiMoodAnalysis.isEmpty()) {
                triggerBackgroundMoodAnalysis(newId, draftContent)
            }

            // Reset draft states
            clearDraft()
            onComplete()
        }
    }

    private fun triggerBackgroundMoodAnalysis(id: Int, text: String) {
        viewModelScope.launch {
            val analysis = GeminiService.analyzePsychologicalMood(text)
            val existing = repository.getEntryById(id)
            if (existing != null) {
                repository.updateDiaryEntry(existing.copy(aiMoodAnalysis = analysis))
            }
        }
    }

    /**
     * Load an entry into draft for editing
     */
    fun loadEntryToDraft(entry: DiaryEntry) {
        suppressAutoSave {
            draftId = entry.id
            draftTitle = entry.title
            draftContent = entry.content
            draftSelectedMoods = if (entry.moods.isNotEmpty()) entry.moods.split(",") else emptyList()
            draftImportance = entry.importance
            draftAudioPath = entry.audioPath
            draftPhotoPath = entry.photoPath
            draftVideoPath = entry.videoPath
            if (entry.pdfPath?.startsWith("drawing:") == true) {
                draftDrawingData = entry.pdfPath.substringAfter("drawing:")
                draftPdfPath = null
            } else {
                draftPdfPath = entry.pdfPath
                draftDrawingData = null
            }
            draftWebLinks = entry.webLinks ?: ""
            draftAiMoodAnalysis = entry.aiMoodAnalysis
            draftIsPinned = entry.isPinned
            draftIsArchived = entry.isArchived
            draftIsFavorite = entry.isFavorite
            draftIsDeleted = entry.isDeleted
            draftReminderTimestamp = entry.reminderTimestamp
            draftEntryType = entry.entryType
        }
    }

    /**
     * Edit or Append content to note (Adds revision log at bottom)
     */
    fun appendOrEditEntry(entryId: Int, appendText: String, onComplete: () -> Unit) {
        viewModelScope.launch {
            val entry = repository.getEntryById(entryId) ?: return@launch
            val dateStr = getCurrentDateString()
            val timeStr = getCurrentTimeString()
            val dayStr = getDayOfWeekArabic(dateStr)

            // Format edit history log
            val logMessage = "\n\n[تمت الإضافة والتعديل في $dayStr $dateStr - $timeStr]\n$appendText"
            val updatedContent = entry.content + logMessage
            
            val updatedEntry = entry.copy(
                content = updatedContent,
                isEdited = true,
                editHistory = (entry.editHistory ?: "") + logMessage,
                timestamp = System.currentTimeMillis()
            )

            repository.updateDiaryEntry(updatedEntry)

            // Re-trigger mood analysis
            triggerBackgroundMoodAnalysis(entryId, updatedContent)

            // Log event
            repository.insertLifeEvent(
                LifeEvent(
                    dateString = dateStr,
                    timeString = timeStr,
                    type = "DIARY",
                    description = "عدلت على مذكرتك: ${entry.title.take(20)}...",
                    moodIcon = "✏️"
                )
            )

            onComplete()
        }
    }

    fun deleteEntry(entryId: Int) {
        viewModelScope.launch {
            repository.deleteDiaryEntryById(entryId)
            repository.insertLifeEvent(
                LifeEvent(
                    dateString = getCurrentDateString(),
                    timeString = getCurrentTimeString(),
                    type = "DIARY",
                    description = "تم حذف مذكرة نهائياً",
                    moodIcon = "🗑️"
                )
            )
        }
    }

    fun clearDraft() {
        suppressAutoSave {
            draftId = null
            draftTitle = ""
            draftContent = ""
            draftSelectedMoods = emptyList()
            draftImportance = 3
            draftAudioPath = null
            draftPhotoPath = null
            draftVideoPath = null
            draftPdfPath = null
            draftWebLinks = ""
            draftDrawingData = null
            draftAiMoodAnalysis = ""
            draftIsPinned = false
            draftIsArchived = false
            draftIsFavorite = false
            draftIsDeleted = false
            draftReminderTimestamp = null
            draftEntryType = "diary"
        }
    }

    // Direct Events Logging (Sleep, Medicine, Sports, Moods)
    fun logSleepForDate(dateStr: String, hours: Float) {
        viewModelScope.launch {
            val currentEvents = allEvents.value
            val existing = currentEvents.find { it.dateString == dateStr && it.type == "SLEEP" }
            if (existing != null) {
                repository.deleteLifeEvent(existing)
            }
            val timeStr = getCurrentTimeString()
            repository.insertLifeEvent(
                LifeEvent(
                    dateString = dateStr,
                    timeString = timeStr,
                    type = "SLEEP",
                    description = "سجلت نومًا بمقدار ${hours.toInt()} ساعات",
                    value = "$hours س",
                    moodIcon = "😴"
                )
            )
        }
    }

    fun logSportsForDate(dateStr: String, minutes: Int) {
        viewModelScope.launch {
            val currentEvents = allEvents.value
            val existing = currentEvents.find { it.dateString == dateStr && it.type == "EXERCISE" }
            if (existing != null) {
                repository.deleteLifeEvent(existing)
            }
            val timeStr = getCurrentTimeString()
            repository.insertLifeEvent(
                LifeEvent(
                    dateString = dateStr,
                    timeString = timeStr,
                    type = "EXERCISE",
                    description = "مارست الرياضة والنشاط البدني لمدّة $minutes دقيقة",
                    value = "$minutes د",
                    moodIcon = "🏃"
                )
            )
        }
    }

    fun logMedicineForDate(dateStr: String, medName: String) {
        viewModelScope.launch {
            val currentEvents = allEvents.value
            val existing = currentEvents.find { it.dateString == dateStr && it.type == "MEDICINE" }
            if (existing != null) {
                repository.deleteLifeEvent(existing)
            }
            val timeStr = getCurrentTimeString()
            repository.insertLifeEvent(
                LifeEvent(
                    dateString = dateStr,
                    timeString = timeStr,
                    type = "MEDICINE",
                    description = "أخذت الدواء: $medName",
                    value = medName,
                    moodIcon = "💊"
                )
            )
        }
    }

    fun removeMedicineForDate(dateStr: String) {
        viewModelScope.launch {
            val currentEvents = allEvents.value
            val existing = currentEvents.find { it.dateString == dateStr && it.type == "MEDICINE" }
            if (existing != null) {
                repository.deleteLifeEvent(existing)
            }
        }
    }

    fun logSleep(hours: Float) {
        logSleepForDate(getCurrentDateString(), hours)
    }

    fun logSports(minutes: Int) {
        logSportsForDate(getCurrentDateString(), minutes)
    }

    fun logMedicine(medName: String) {
        viewModelScope.launch {
            val dateStr = getCurrentDateString()
            val timeStr = getCurrentTimeString()
            repository.insertLifeEvent(
                LifeEvent(
                    dateString = dateStr,
                    timeString = timeStr,
                    type = "MEDICINE",
                    description = "أخذت الدواء: $medName",
                    value = medName,
                    moodIcon = "💊"
                )
            )
        }
    }

    fun logCustomMood(emoji: String, name: String) {
        viewModelScope.launch {
            val dateStr = getCurrentDateString()
            val timeStr = getCurrentTimeString()
            repository.insertLifeEvent(
                LifeEvent(
                    dateString = dateStr,
                    timeString = timeStr,
                    type = "MOOD",
                    description = "سجلت حالة مزاجية مباشرة: $name",
                    moodIcon = emoji
                )
            )
        }
    }

    // Habits Toggle Complete
    fun toggleHabitForDate(habitId: Int, isCompleted: Boolean, dateStr: String) {
        viewModelScope.launch {
            if (isCompleted) {
                repository.insertHabitLog(HabitLog(habitId = habitId, dateString = dateStr, isCompleted = true))
            } else {
                repository.deleteHabitLog(habitId, dateStr)
            }
        }
    }

    fun toggleHabit(habitId: Int, isCompleted: Boolean) {
        toggleHabitForDate(habitId, isCompleted, getCurrentDateString())
    }

    fun addCustomHabit(name: String) {
        viewModelScope.launch {
            repository.insertHabit(Habit(name = name))
        }
    }

    fun deleteHabit(habitId: Int) {
        viewModelScope.launch {
            repository.deleteHabitById(habitId)
        }
    }

    /**
     * Send message to the Intelligent Consultant (🧠)
     */
    fun sendChatMessage(
        messageText: String,
        audioPath: String? = null,
        photoPath: String? = null,
        drawingData: String? = null
    ) {
        if (messageText.trim().isEmpty() && audioPath == null && photoPath == null && drawingData == null) return

        viewModelScope.launch {
            // Save User Message immediately
            val userMsg = ChatMessage(
                sender = "USER",
                content = messageText,
                audioPath = audioPath,
                photoPath = photoPath,
                drawingData = drawingData
            )
            repository.insertChatMessage(userMsg)

            _isChatLoading.value = true

            // Gather context if allowed
            val contextBuilder = java.lang.StringBuilder()
            if (hasChatAccessPermission) {
                val entries = allEntries.value
                contextBuilder.append("اليوميات التاريخية للمستخدم:\n")
                if (entries.isEmpty()) {
                    contextBuilder.append("لا توجد مذكرات مكتوبة بعد.\n")
                } else {
                    entries.forEach { entry ->
                        contextBuilder.append("- التاريخ: ${entry.dateString} ${entry.timeString}\n")
                        contextBuilder.append("  العنوان: ${entry.title}\n")
                        contextBuilder.append("  المحتوى: ${entry.content}\n")
                        contextBuilder.append("  الحالة النفسية (تحليل AI): ${entry.aiMoodAnalysis}\n")
                        contextBuilder.append("  المزاج المختار: ${entry.moods}\n")
                        contextBuilder.append("---------------------------------------\n")
                    }
                }
                
                val events = allEvents.value
                contextBuilder.append("\nخريطة حياة المستخدم (الأنشطة):\n")
                events.take(50).forEach { event ->
                    contextBuilder.append("- ${event.dateString} ${event.timeString}: [${event.type}] ${event.description}\n")
                }
            } else {
                contextBuilder.append("المستخدم لم يمنح صلاحية الوصول لليوميات. تحدث معه بشكل عام فقط دون استخدام تفاصيل مذكراته.")
            }

            val response = GeminiService.getConsultantResponse(messageText, contextBuilder.toString())
            
            // Save Assistant Message immediately
            val botMsg = ChatMessage(sender = "ASSISTANT", content = response)
            repository.insertChatMessage(botMsg)

            _isChatLoading.value = false
        }
    }

    fun clearChatHistory() {
        viewModelScope.launch {
            repository.clearChatMessages()
        }
    }

    /**
     * Single Diary helper actions
     */
    suspend fun getDiaryAssistantAction(type: String, content: String): String {
        return GeminiService.getDiaryAssistantFeature(type, content)
    }

    /**
     * Generate Therapy Report
     */
    fun generateTherapyReport(startDate: String, endDate: String) {
        viewModelScope.launch {
            _isReportLoading.value = true

            val contextBuilder = java.lang.StringBuilder()
            val entries = allEntries.value.filter { it.dateString in startDate..endDate }
            val events = allEvents.value.filter { it.dateString in startDate..endDate }
            val habits = allHabits.value
            val habitLogs = allHabitLogs.value.filter { it.dateString in startDate..endDate }

            contextBuilder.append("اليوميات للفترة المحددة ($startDate إلى $endDate):\n")
            if (entries.isEmpty()) {
                contextBuilder.append("لا توجد مذكرات في هذه الفترة.\n")
            } else {
                entries.forEach { entry ->
                    contextBuilder.append("- التاريخ: ${entry.dateString} ${entry.timeString}\n")
                    contextBuilder.append("  العنوان: ${entry.title}\n")
                    contextBuilder.append("  المحتوى: ${entry.content}\n")
                    contextBuilder.append("  المزاج: ${entry.moods}\n")
                    contextBuilder.append("  التحليل النفسي: ${entry.aiMoodAnalysis}\n")
                }
            }

            contextBuilder.append("\nالأنشطة والرياضة والأدوية للفترة:\n")
            events.forEach { event ->
                contextBuilder.append("- ${event.dateString} ${event.timeString}: [${event.type}] ${event.description} ${event.value ?: ""}\n")
            }

            contextBuilder.append("\nإحصائيات العادات للفترة:\n")
            habits.forEach { habit ->
                val completions = habitLogs.count { it.habitId == habit.id }
                contextBuilder.append("- العادة: ${habit.name} - تم إنجازها $completions مرات\n")
            }

            val report = GeminiService.generateTherapyReport(startDate, endDate, contextBuilder.toString())
            _generatedReport.value = report
            _isReportLoading.value = false
        }
    }

    /**
     * Backup & Export JSON
     */
    fun exportBackupJson(): String {
        val root = JSONObject()
        val entriesArray = JSONArray()
        allEntries.value.forEach { entry ->
            val obj = JSONObject().apply {
                put("id", entry.id)
                put("dateString", entry.dateString)
                put("timeString", entry.timeString)
                put("title", entry.title)
                put("content", entry.content)
                put("moods", entry.moods)
                put("aiMoodAnalysis", entry.aiMoodAnalysis)
                put("importance", entry.importance)
                put("timestamp", entry.timestamp)
                put("audioPath", entry.audioPath ?: "")
                put("photoPath", entry.photoPath ?: "")
                put("videoPath", entry.videoPath ?: "")
                put("pdfPath", entry.pdfPath ?: "")
                put("webLinks", entry.webLinks ?: "")
                put("hasAudio", entry.hasAudio)
                put("hasPhoto", entry.hasPhoto)
                put("hasVideo", entry.hasVideo)
                put("hasPdf", entry.hasPdf)
                put("isPinned", entry.isPinned)
                put("isArchived", entry.isArchived)
                put("isFavorite", entry.isFavorite)
                put("isDeleted", entry.isDeleted)
                put("reminderTimestamp", entry.reminderTimestamp ?: 0L)
                put("entryType", entry.entryType)
            }
            entriesArray.put(obj)
        }
        root.put("diary_entries", entriesArray)

        val eventsArray = JSONArray()
        allEvents.value.forEach { ev ->
            val obj = JSONObject().apply {
                put("id", ev.id)
                put("dateString", ev.dateString)
                put("timeString", ev.timeString)
                put("type", ev.type)
                put("description", ev.description)
                put("moodIcon", ev.moodIcon ?: "")
                put("value", ev.value ?: "")
                put("timestamp", ev.timestamp)
            }
            eventsArray.put(obj)
        }
        root.put("life_events", eventsArray)

        val habitsArray = JSONArray()
        allHabits.value.forEach { habit ->
            val obj = JSONObject().apply {
                put("id", habit.id)
                put("name", habit.name)
                put("reminderTime", habit.reminderTime ?: "")
                put("isReminderEnabled", habit.isReminderEnabled)
            }
            habitsArray.put(obj)
        }
        root.put("habits", habitsArray)

        val habitLogsArray = JSONArray()
        allHabitLogs.value.forEach { log ->
            val obj = JSONObject().apply {
                put("id", log.id)
                put("habitId", log.habitId)
                put("dateString", log.dateString)
                put("isCompleted", log.isCompleted)
            }
            habitLogsArray.put(obj)
        }
        root.put("habit_logs", habitLogsArray)

        return root.toString(2)
    }

    fun exportSingleDiaryJson(entry: DiaryEntry): String {
        return try {
            val root = JSONObject()
            val obj = JSONObject().apply {
                put("dateString", entry.dateString)
                put("timeString", entry.timeString)
                put("title", entry.title)
                put("content", entry.content)
                put("moods", entry.moods)
                put("aiMoodAnalysis", entry.aiMoodAnalysis)
                put("importance", entry.importance)
                put("timestamp", entry.timestamp)
                put("audioPath", entry.audioPath ?: "")
                put("photoPath", entry.photoPath ?: "")
                put("videoPath", entry.videoPath ?: "")
                put("pdfPath", entry.pdfPath ?: "")
                put("webLinks", entry.webLinks ?: "")
                put("hasAudio", entry.hasAudio)
                put("hasPhoto", entry.hasPhoto)
                put("hasVideo", entry.hasVideo)
                put("hasPdf", entry.hasPdf)
                put("isPinned", entry.isPinned)
                put("isArchived", entry.isArchived)
                put("isFavorite", entry.isFavorite)
                put("isDeleted", entry.isDeleted)
                put("reminderTimestamp", entry.reminderTimestamp ?: 0L)
                put("entryType", entry.entryType)
            }
            root.put("single_diary", obj)
            root.toString(2)
        } catch (e: Exception) {
            ""
        }
    }

    fun importBackupJson(jsonString: String): Boolean {
        return try {
            val root = JSONObject(jsonString)
            
            // Handle single diary entry import
            val singleDiaryObj = root.optJSONObject("single_diary")
            if (singleDiaryObj != null) {
                viewModelScope.launch {
                    val entry = DiaryEntry(
                        id = 0, // auto-generate brand new id to prevent clashes
                        dateString = singleDiaryObj.optString("dateString"),
                        timeString = singleDiaryObj.optString("timeString"),
                        title = singleDiaryObj.optString("title"),
                        content = singleDiaryObj.optString("content"),
                        moods = singleDiaryObj.optString("moods"),
                        aiMoodAnalysis = singleDiaryObj.optString("aiMoodAnalysis"),
                        importance = singleDiaryObj.optInt("importance", 3),
                        hasAudio = singleDiaryObj.optBoolean("hasAudio", false),
                        audioPath = singleDiaryObj.optString("audioPath").takeIf { it.isNotEmpty() },
                        hasPhoto = singleDiaryObj.optBoolean("hasPhoto", false),
                        photoPath = singleDiaryObj.optString("photoPath").takeIf { it.isNotEmpty() },
                        hasVideo = singleDiaryObj.optBoolean("hasVideo", false),
                        videoPath = singleDiaryObj.optString("videoPath").takeIf { it.isNotEmpty() },
                        hasPdf = singleDiaryObj.optBoolean("hasPdf", false),
                        pdfPath = singleDiaryObj.optString("pdfPath").takeIf { it.isNotEmpty() },
                        webLinks = singleDiaryObj.optString("webLinks").takeIf { it.isNotEmpty() },
                        isPinned = singleDiaryObj.optBoolean("isPinned", false),
                        isArchived = singleDiaryObj.optBoolean("isArchived", false),
                        isFavorite = singleDiaryObj.optBoolean("isFavorite", false),
                        isDeleted = singleDiaryObj.optBoolean("isDeleted", false),
                        reminderTimestamp = singleDiaryObj.optLong("reminderTimestamp", 0L).takeIf { it > 0L },
                        entryType = singleDiaryObj.optString("entryType", "diary"),
                        timestamp = singleDiaryObj.optLong("timestamp", System.currentTimeMillis())
                    )
                    repository.insertDiaryEntry(entry)
                }
                return true
            }
            
            // Handle full backup entries import
            val entriesArray = root.optJSONArray("diary_entries")
            if (entriesArray != null) {
                viewModelScope.launch {
                    for (i in 0 until entriesArray.length()) {
                        val obj = entriesArray.getJSONObject(i)
                        val entry = DiaryEntry(
                            id = 0, // auto-generate brand new id to prevent clashes
                            dateString = obj.optString("dateString"),
                            timeString = obj.optString("timeString"),
                            title = obj.optString("title"),
                            content = obj.optString("content"),
                            moods = obj.optString("moods"),
                            aiMoodAnalysis = obj.optString("aiMoodAnalysis"),
                            importance = obj.optInt("importance", 3),
                            hasAudio = obj.optBoolean("hasAudio", false),
                            audioPath = obj.optString("audioPath").takeIf { it.isNotEmpty() },
                            hasPhoto = obj.optBoolean("hasPhoto", false),
                            photoPath = obj.optString("photoPath").takeIf { it.isNotEmpty() },
                            hasVideo = obj.optBoolean("hasVideo", false),
                            videoPath = obj.optString("videoPath").takeIf { it.isNotEmpty() },
                            hasPdf = obj.optBoolean("hasPdf", false),
                            pdfPath = obj.optString("pdfPath").takeIf { it.isNotEmpty() },
                            webLinks = obj.optString("webLinks").takeIf { it.isNotEmpty() },
                            isPinned = obj.optBoolean("isPinned", false),
                            isArchived = obj.optBoolean("isArchived", false),
                            isFavorite = obj.optBoolean("isFavorite", false),
                            isDeleted = obj.optBoolean("isDeleted", false),
                            reminderTimestamp = obj.optLong("reminderTimestamp", 0L).takeIf { it > 0L },
                            entryType = obj.optString("entryType", "diary"),
                            timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                        )
                        repository.insertDiaryEntry(entry)
                    }
                }
            }
            
            val eventsArray = root.optJSONArray("life_events")
            if (eventsArray != null) {
                viewModelScope.launch {
                    for (i in 0 until eventsArray.length()) {
                        val obj = eventsArray.getJSONObject(i)
                        val ev = LifeEvent(
                            id = 0,
                            dateString = obj.optString("dateString"),
                            timeString = obj.optString("timeString"),
                            type = obj.optString("type"),
                            description = obj.optString("description"),
                            moodIcon = obj.optString("moodIcon").takeIf { it.isNotEmpty() },
                            value = obj.optString("value").takeIf { it.isNotEmpty() },
                            timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                        )
                        repository.insertLifeEvent(ev)
                    }
                }
            }

            val habitsArray = root.optJSONArray("habits")
            if (habitsArray != null) {
                viewModelScope.launch {
                    for (i in 0 until habitsArray.length()) {
                        val obj = habitsArray.getJSONObject(i)
                        val habit = Habit(
                            id = 0,
                            name = obj.optString("name"),
                            reminderTime = obj.optString("reminderTime").takeIf { it.isNotEmpty() },
                            isReminderEnabled = obj.optBoolean("isReminderEnabled", false)
                        )
                        repository.insertHabit(habit)
                    }
                }
            }

            val habitLogsArray = root.optJSONArray("habit_logs")
            if (habitLogsArray != null) {
                viewModelScope.launch {
                    for (i in 0 until habitLogsArray.length()) {
                        val obj = habitLogsArray.getJSONObject(i)
                        val log = HabitLog(
                            id = 0,
                            habitId = obj.optInt("habitId"),
                            dateString = obj.optString("dateString"),
                            isCompleted = obj.optBoolean("isCompleted", false)
                        )
                        repository.insertHabitLog(log)
                    }
                }
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Import failed", e)
            false
        }
    }

    fun syncBackupToCloud(email: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                // Get the real backup JSON string
                val backupJson = exportBackupJson()
                
                // Save it to shared prefs using a separate "cloud_mock" pref space keyed by the email
                val cloudPrefs = getApplication<Application>().getSharedPreferences("mock_google_cloud", Context.MODE_PRIVATE)
                cloudPrefs.edit().putString(email, backupJson).apply()
                
                // Store last sync time
                val sdf = SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.getDefault())
                prefs.edit().putString("last_cloud_sync_$email", sdf.format(Date())).apply()
                
                onComplete(true)
            } catch (e: Exception) {
                onComplete(false)
            }
        }
    }
    
    fun restoreBackupFromCloud(email: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val cloudPrefs = getApplication<Application>().getSharedPreferences("mock_google_cloud", Context.MODE_PRIVATE)
                val backupJson = cloudPrefs.getString(email, null)
                if (backupJson != null) {
                    val success = importBackupJson(backupJson)
                    onComplete(success)
                } else {
                    onComplete(false) // No backup found for this email
                }
            } catch (e: Exception) {
                onComplete(false)
            }
        }
    }

    fun getLastSyncTime(email: String): String {
        return prefs.getString("last_cloud_sync_$email", "لم يتم المزامنة بعد") ?: "لم يتم المزامنة بعد"
    }

    var floatingBallResponse by mutableStateOf("")
    var isFloatingBallLoading by mutableStateOf(false)

    fun askFloatingBallAssistant(prompt: String) {
        viewModelScope.launch {
            isFloatingBallLoading = true
            floatingBallResponse = ""
            try {
                val systemPrompt = """
                    أنت مساعد الكتابة والتحليل النفسي الذكي والودود جداً (Yawmiyati Assistant).
                    تساعد المستخدم في كتابة المقالات، وتوليد النصائح الصحية والنفسية المتكاملة، وتحسين جودة الحياة، وإدارة المشاريع وتنظيم المهام بكفاءة بالغة باللغة العربية.
                    اجعل إجاباتك منظمة بشكل رائع، غنية بالمعلومات النفسية والممارسات المفيدة، واستخدم التنسيق الجميل والرموز التعبيرية المعبرة.
                """.trimIndent()
                val response = GeminiService.getQuickResponse(prompt, systemPrompt)
                floatingBallResponse = response
            } catch (e: Exception) {
                floatingBallResponse = "حدث خطأ أثناء جلب الرد من خادم الذكاء الاصطناعي: ${e.localizedMessage}"
            } finally {
                isFloatingBallLoading = false
            }
        }
    }

    // --- DAILY TASKS REPORT STATES & METHODS ---
    var isTasksReportLoading by mutableStateOf(false)
        private set
    var tasksReportResult by mutableStateOf("")

    fun generateTasksReport(startDate: String, endDate: String) {
        viewModelScope.launch {
            isTasksReportLoading = true
            tasksReportResult = ""

            val contextBuilder = java.lang.StringBuilder()
            val entries = allEntries.value.filter { it.dateString in startDate..endDate }
            val habits = allHabits.value
            val habitLogs = allHabitLogs.value.filter { it.dateString in startDate..endDate }

            contextBuilder.append("تقرير المهام والالتزام السلوكي للفترة ($startDate إلى $endDate):\n")
            contextBuilder.append("قائمة المهام اليومية المحددة:\n")
            habits.forEach { habit ->
                val completions = habitLogs.count { it.habitId == habit.id }
                contextBuilder.append("- المهمة/العادة: ${habit.name}\n")
                contextBuilder.append("  عدد مرات الإنجاز: $completions\n")
            }

            if (entries.isNotEmpty()) {
                contextBuilder.append("\nاليوميات المرتبطة بالفترة للربط النفسي والسلوكي:\n")
                entries.forEach { entry ->
                    contextBuilder.append("- التاريخ: ${entry.dateString}\n")
                    contextBuilder.append("  المحتوى: ${entry.content}\n")
                }
            }

            try {
                val systemPrompt = """
                    أنت خبير نفسي وسلوكي ومستشار تنظيم حياتي ذكي جداً (Yawmiyati Tasks Assistant).
                    قم بتحليل المهام اليومية ونسبة التزام المستخدم بها للفترة المحددة من ($startDate إلى $endDate).
                    قدم تقريراً باللغة العربية الفصحى يتضمن:
                    ١. رصد الالتزام: تقييم لمدى انضباط المستخدم في إنجاز مهامه اليومية ونسبة الإنجاز.
                    ٢. الرابط النفسي: كيف يؤثر تنظيم وإنجاز المهام على جودة حياته النفسية واستقراره المزاجي بناءً على ما دونه في يومياته (إن وجد).
                    ٣. نصائح وتوصيات مخصصة: خطوات عملية وسلوكية ذكية لزيادة الإنتاجية وتقليل الضغوط وتحقيق التوازن النفسي.
                    اجعل إجاباتك غنية ومفصلة ومنظمة بشكل رائع، واستخدم التنسيق الجميل والرموز التعبيرية المعبرة.
                """.trimIndent()

                val prompt = "قم بإعداد تقرير المهام اليومية للفترة من $startDate إلى $endDate بناءً على البيانات التالية:\n\n$contextBuilder"
                val response = GeminiService.getQuickResponse(prompt, systemPrompt)
                tasksReportResult = response
            } catch (e: Exception) {
                tasksReportResult = "حدث خطأ أثناء توليد تقرير المهام اليومية: ${e.localizedMessage}"
            } finally {
                isTasksReportLoading = false
            }
        }
    }

    fun togglePin(entryId: Int) {
        viewModelScope.launch {
            val entry = repository.getEntryById(entryId) ?: return@launch
            repository.updateDiaryEntry(entry.copy(isPinned = !entry.isPinned))
        }
    }

    fun toggleFavorite(entryId: Int) {
        viewModelScope.launch {
            val entry = repository.getEntryById(entryId) ?: return@launch
            repository.updateDiaryEntry(entry.copy(isFavorite = !entry.isFavorite))
        }
    }

    fun toggleArchive(entryId: Int) {
        viewModelScope.launch {
            val entry = repository.getEntryById(entryId) ?: return@launch
            repository.updateDiaryEntry(entry.copy(isArchived = !entry.isArchived))
        }
    }

    fun moveToTrash(entryId: Int) {
        viewModelScope.launch {
            val entry = repository.getEntryById(entryId) ?: return@launch
            repository.updateDiaryEntry(entry.copy(isDeleted = true))
        }
    }

    fun restoreFromTrash(entryId: Int) {
        viewModelScope.launch {
            val entry = repository.getEntryById(entryId) ?: return@launch
            repository.updateDiaryEntry(entry.copy(isDeleted = false))
        }
    }

    fun setReminderForEntry(entryId: Int, timestamp: Long?) {
        viewModelScope.launch {
            val entry = repository.getEntryById(entryId) ?: return@launch
            repository.updateDiaryEntry(entry.copy(reminderTimestamp = timestamp))
            if (timestamp != null) {
                com.example.receiver.ReminderScheduler.scheduleDiaryReminder(
                    getApplication(),
                    entryId,
                    entry.title,
                    timestamp
                )
            } else {
                com.example.receiver.ReminderScheduler.cancelDiaryReminder(getApplication(), entryId)
            }
        }
    }

    fun updateHabitReminder(habitId: Int, reminderTime: String?, isEnabled: Boolean) {
        viewModelScope.launch {
            val habitsList = allHabits.value
            val habit = habitsList.find { it.id == habitId } ?: return@launch
            val updated = habit.copy(reminderTime = reminderTime, isReminderEnabled = isEnabled)
            repository.insertHabit(updated)
            
            if (isEnabled && reminderTime != null) {
                com.example.receiver.ReminderScheduler.scheduleHabitReminder(
                    getApplication(),
                    habitId,
                    habit.name,
                    reminderTime
                )
            } else {
                com.example.receiver.ReminderScheduler.cancelHabitReminder(getApplication(), habitId)
            }
        }
    }

    private fun calculateStreak(entries: List<DiaryEntry>): Int {
        if (entries.isEmpty()) return 0
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        
        // Group by dateString, parse them, sort descending (newest to oldest)
        val entryDates = entries.map { it.dateString }
            .distinct()
            .mapNotNull {
                try { sdf.parse(it) } catch (e: Exception) { null }
            }
            .sortedDescending()
        
        if (entryDates.isEmpty()) return 0
        
        val cal = Calendar.getInstance()
        // Reset time to exact midnight
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val today = cal.time
        
        cal.add(Calendar.DAY_OF_YEAR, -1)
        val yesterday = cal.time
        
        val newestDate = entryDates[0]
        
        // If the newest entry is older than yesterday, streak is broken
        val diffToday = (today.time - newestDate.time) / (1000 * 60 * 60 * 24)
        if (diffToday > 1) {
            return 0
        }
        
        var streak = 1
        for (i in 0 until entryDates.size - 1) {
            val current = entryDates[i]
            val next = entryDates[i + 1]
            val diff = (current.time - next.time) / (1000 * 60 * 60 * 24)
            if (diff == 1L) {
                streak++
            } else if (diff > 1L) {
                break
            }
        }
        return streak
    }

    companion object {
        private const val TAG = "DiaryViewModel"
    }
}
