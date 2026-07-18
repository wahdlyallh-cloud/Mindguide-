package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.DiaryEntry
import com.example.data.model.LifeEvent
import com.example.viewmodel.DiaryViewModel
import com.example.data.api.GeminiService
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullCalendarReadDialog(
    viewModel: DiaryViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    val entries by viewModel.allEntries.collectAsState()
    val events by viewModel.allEvents.collectAsState()
    val chatMessages by viewModel.chatMessages.collectAsState()

    var selectedDate by remember { mutableStateOf(Calendar.getInstance()) }
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val selectedDateString = sdf.format(selectedDate.time)

    // View state
    var currentTab by remember { mutableStateOf(0) } // 0: Read/Daily Log (اقرأ), 1: Library (مكتبتي الشاملة), 2: AI Wisdom (الذكاء الاصطناعي)

    // Library Add Book Dialog
    var showAddBookForm by remember { mutableStateOf(false) }

    // Active calendar navigation
    var calendarMonth by remember { mutableStateOf(Calendar.getInstance()) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.95f)
                .padding(12.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFAF7F0)),
            border = BorderStroke(1.dp, Color(0xFFE3D9C6).copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.End
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق")
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "التقويم والمكتبة الشاملة • اقرأ 📅📖",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF385723)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Divider(color = Color(0xFFE3D9C6).copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(10.dp))

                // Custom Tab Switcher
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White, RoundedCornerShape(12.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val tabs = listOf(
                        "الحكمة والتقارير 🧠",
                        "مكتبتي الشاملة 📖",
                        "يومياتي وأنشطتي (اقرأ) ☀️"
                    )
                    tabs.forEachIndexed { index, title ->
                        // Reverse index for RTL presentation
                        val actualIndex = 2 - index
                        val isSelected = currentTab == actualIndex
                        Button(
                            onClick = { currentTab = actualIndex },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) Color(0xFF4A6B5D) else Color.Transparent,
                                contentColor = if (isSelected) Color.White else Color.Gray
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            Text(title, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 2-Column Split View for large screens / Foldables, or Column View for standard screens
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Left Column / Full Content Area
                    Box(
                        modifier = Modifier
                            .weight(1.3f)
                            .fillMaxHeight()
                            .background(Color.White, RoundedCornerShape(18.dp))
                            .border(1.dp, Color(0xFFE3D9C6).copy(alpha = 0.3f), RoundedCornerShape(18.dp))
                            .padding(14.dp)
                    ) {
                        when (currentTab) {
                            0 -> ReadDailyLogSection(
                                viewModel = viewModel,
                                selectedDateString = selectedDateString,
                                entries = entries,
                                events = events,
                                chatMessages = chatMessages,
                                onAddBookClick = { showAddBookForm = true }
                            )
                            1 -> LibrarySection(
                                viewModel = viewModel,
                                selectedDateString = selectedDateString,
                                entries = entries,
                                onAddBookClick = { showAddBookForm = true }
                            )
                            2 -> AIWisdomAndReportsSection(
                                viewModel = viewModel,
                                selectedDateString = selectedDateString
                            )
                        }
                    }

                    // Right Column / Fixed Interactive Calendar
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = "اختر تاريخاً لاستعراض التفاصيل:",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray,
                            textAlign = TextAlign.End,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        InteractiveCalendarGrid(
                            currentMonth = calendarMonth,
                            selectedDate = selectedDate,
                            onMonthChange = { calendarMonth = it },
                            onDateSelect = { selectedDate = it },
                            entries = entries,
                            events = events
                        )
                    }
                }
            }
        }
    }

    // Modal Add Book Dialog
    if (showAddBookForm) {
        AddBookDialog(
            viewModel = viewModel,
            selectedDateString = selectedDateString,
            onDismiss = { showAddBookForm = false }
        )
    }
}

// 1. Interactive Calendar Grid Composable
@Composable
fun InteractiveCalendarGrid(
    currentMonth: Calendar,
    selectedDate: Calendar,
    onMonthChange: (Calendar) -> Unit,
    onDateSelect: (Calendar) -> Unit,
    entries: List<DiaryEntry>,
    events: List<LifeEvent>
) {
    val monthYearFormat = SimpleDateFormat("MMMM yyyy", Locale("ar"))
    val dayNames = listOf("أح", "إث", "ثل", "أر", "خم", "جم", "سب")

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFFE3D9C6).copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Month Switcher Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    val prev = (currentMonth.clone() as Calendar).apply { add(Calendar.MONTH, -1) }
                    onMonthChange(prev)
                }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "الشهر السابق")
                }

                Text(
                    text = monthYearFormat.format(currentMonth.time),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF385723)
                )

                IconButton(onClick = {
                    val next = (currentMonth.clone() as Calendar).apply { add(Calendar.MONTH, 1) }
                    onMonthChange(next)
                }) {
                    Icon(Icons.Default.ArrowForward, contentDescription = "الشهر التالي")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Week Days Label Row
            Row(modifier = Modifier.fillMaxWidth()) {
                dayNames.forEach { dayName ->
                    Text(
                        text = dayName,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Days Grid
            val daysInMonth = currentMonth.getActualMaximum(Calendar.DAY_OF_MONTH)
            val monthStartCal = (currentMonth.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, 1) }
            val firstDayOfWeek = monthStartCal.get(Calendar.DAY_OF_WEEK) - 1 // 0-indexed

            val weeksList = mutableListOf<List<Calendar?>>()
            var currentWeek = mutableListOf<Calendar?>()

            // Pad beginning of first week
            for (i in 0 until firstDayOfWeek) {
                currentWeek.add(null)
            }

            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)

            for (day in 1..daysInMonth) {
                val dayCal = (currentMonth.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, day) }
                currentWeek.add(dayCal)
                if (currentWeek.size == 7) {
                    weeksList.add(currentWeek)
                    currentWeek = mutableListOf()
                }
            }

            if (currentWeek.isNotEmpty()) {
                while (currentWeek.size < 7) {
                    currentWeek.add(null)
                }
                weeksList.add(currentWeek)
            }

            weeksList.forEach { week ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    week.forEach { dateCal ->
                        if (dateCal == null) {
                            Spacer(modifier = Modifier.weight(1f))
                        } else {
                            val dateStr = sdf.format(dateCal.time)
                            val isSelected = sdf.format(selectedDate.time) == dateStr
                            val isToday = sdf.format(Date()) == dateStr

                            val dayEntries = entries.filter { it.dateString == dateStr && it.entryType == "diary" }
                            val dayBooks = entries.filter { it.dateString == dateStr && it.entryType == "book" }
                            val dayEvents = events.filter { it.dateString == dateStr }

                            val hasActivity = dayEntries.isNotEmpty() || dayBooks.isNotEmpty() || dayEvents.isNotEmpty()

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .padding(2.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            isSelected -> Color(0xFF4A6B5D)
                                            isToday -> Color(0xFFE2F0D9)
                                            else -> Color.Transparent
                                        }
                                    )
                                    .clickable { onDateSelect(dateCal) },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = dateCal.get(Calendar.DAY_OF_MONTH).toString(),
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                                        color = when {
                                            isSelected -> Color.White
                                            else -> Color.Black
                                        }
                                    )

                                    if (hasActivity && !isSelected) {
                                        Box(
                                            modifier = Modifier
                                                .size(4.dp)
                                                .background(
                                                    if (dayBooks.isNotEmpty()) Color(0xFFE28743) else Color(0xFF4A6B5D),
                                                    CircleShape
                                                )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// 2. Read Daily Log (اقرأ) Section
@Composable
fun ReadDailyLogSection(
    viewModel: DiaryViewModel,
    selectedDateString: String,
    entries: List<DiaryEntry>,
    events: List<LifeEvent>,
    chatMessages: List<com.example.data.model.ChatMessage>,
    onAddBookClick: () -> Unit
) {
    val dayEntries = entries.filter { it.dateString == selectedDateString && it.entryType == "diary" }
    val dayBooks = entries.filter { it.dateString == selectedDateString && it.entryType == "book" }
    val dayEvents = events.filter { it.dateString == selectedDateString }
    val dayChatMessages = chatMessages.filter { 
        val msgDate = try {
            val sdfOutput = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            sdfOutput.format(java.util.Date(it.timestamp))
        } catch (e: Exception) {
            ""
        }
        msgDate == selectedDateString
    }

    val moodEvent = dayEvents.find { it.type == "MOOD" }
    val sleepEvent = dayEvents.find { it.type == "SLEEP" }
    val sportsEvent = dayEvents.find { it.type == "EXERCISE" }
    val medEvent = dayEvents.find { it.type == "MEDICINE" }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(
                text = "ما حدث في يوم: $selectedDateString 🌟",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color(0xFF4A6B5D),
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(4.dp))
        }

        // 1. Health & Mind Metrics of the selected day
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFAF7F0)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "الالتزام والقياسات النفسية والصحية 📊",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4A6B5D)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Left Side
                        Column(horizontalAlignment = Alignment.Start) {
                            Text(text = "💊 جرعة العلاج: ${if (medEvent != null) "✅ تم التناول" else "❌ لم يسجل"}", fontSize = 11.sp)
                            Text(text = "🏃 الرياضة: ${sportsEvent?.value ?: "لا توجد تمارين"}", fontSize = 11.sp)
                        }

                        // Right Side
                        Column(horizontalAlignment = Alignment.End) {
                            Text(text = "😀 المزاج السائد: ${moodEvent?.moodIcon ?: "未 لم يحدد"}", fontSize = 11.sp)
                            Text(text = "😴 النوم: ${sleepEvent?.value ?: "لا توجد سجلات"}", fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // 2. User Diary Entries for this day
        if (dayEntries.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                        .background(Color(0xFFFAF7F0), RoundedCornerShape(12.dp))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "لم تكتب أي خاطرة أو يوميات في هذا اليوم 📝",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            item {
                Text(
                    text = "اليوميات المسجلة (${dayEntries.size}):",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.DarkGray,
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            items(dayEntries) { entry ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFFE3D9C6).copy(alpha = 0.2f))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = entry.timeString, fontSize = 10.sp, color = Color.Gray)
                            Text(
                                text = entry.title,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.Black
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = entry.content,
                            fontSize = 11.sp,
                            color = Color.DarkGray,
                            textAlign = TextAlign.End,
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Attachments rendering
                        RenderAttachmentsInsideRead(viewModel = viewModel, entry = entry)
                    }
                }
            }
        }

        // Books / Reading Notes Section for this day
        item {
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onAddBookClick,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF385723)),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.height(30.dp)
                ) {
                    Text("إضافة كتاب لهذا اليوم 📖➕", fontSize = 10.sp, color = Color.White)
                }
                Text(
                    text = "الكتب والمذكرات القرائية (${dayBooks.size}):",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.DarkGray,
                    textAlign = TextAlign.End
                )
            }
        }

        if (dayBooks.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .background(Color(0xFFFAF7F0), RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "لم تسجل قراءة أي كتاب في هذا اليوم 📚",
                        fontSize = 11.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            items(dayBooks) { book ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFAF7F0).copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFFE3D9C6).copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "⭐ " + "★".repeat(book.importance), fontSize = 9.sp, color = Color(0xFFF39C12))
                            Text(
                                text = book.title,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.Black
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = book.content,
                            fontSize = 11.sp,
                            color = Color.DarkGray,
                            textAlign = TextAlign.End,
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Attachments inside Book
                        RenderAttachmentsInsideRead(viewModel = viewModel, entry = book)

                        Spacer(modifier = Modifier.height(8.dp))

                        // AI helper for this Book
                        BookAIInteractions(viewModel = viewModel, book = book)
                    }
                }
            }
        }

        // 3. AI Consultant Chat History of the day
        if (dayChatMessages.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "محادثة المستشار النفسي في هذا اليوم:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.DarkGray,
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            items(dayChatMessages) { msg ->
                val isUser = msg.sender == "USER"
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    contentAlignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
                ) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isUser) Color(0xFFE2F0D9) else Color(0xFFFAF7F0)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.widthIn(max = 240.dp)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(
                                text = msg.content,
                                fontSize = 11.sp,
                                color = Color.Black,
                                textAlign = if (isUser) TextAlign.Right else TextAlign.Left,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}

// 3. Comprehensive Library (مكتبتي) Section
@Composable
fun LibrarySection(
    viewModel: DiaryViewModel,
    selectedDateString: String,
    entries: List<DiaryEntry>,
    onAddBookClick: () -> Unit
) {
    val books = entries.filter { it.entryType == "book" }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onAddBookClick,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF385723)),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.height(34.dp)
            ) {
                Text("+ إضافة كتاب", fontSize = 11.sp, color = Color.White)
            }

            Text(
                text = "مكتبتي الشاملة وقراءاتي 📖",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color(0xFF385723)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (books.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📖", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "لا توجد كتب حالياً بمكتبتك.\nاضغط على الزر لإضافة كتابك المفضل بملحقاته الكاملة!",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(books) { book ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFAF7F0).copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color(0xFFE3D9C6).copy(alpha = 0.3f))
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            horizontalAlignment = Alignment.End
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Delete Action
                                IconButton(
                                    onClick = { viewModel.deleteEntry(book.id) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "حذف الكتاب", tint = Color.Red.copy(alpha = 0.5f))
                                }

                                // Book Title & Badge
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .background(Color(0xFFE2F0D9), RoundedCornerShape(6.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(text = "⭐ " + "★".repeat(book.importance), fontSize = 9.sp, color = Color(0xFF385723))
                                    }
                                    Text(
                                        text = book.title,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = Color.Black
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = book.content,
                                fontSize = 11.sp,
                                color = Color.DarkGray,
                                textAlign = TextAlign.End,
                                modifier = Modifier.fillMaxWidth()
                            )

                            // Render Attachments Inside Book
                            RenderAttachmentsInsideRead(viewModel = viewModel, entry = book)

                            // AI Action for Book
                            Spacer(modifier = Modifier.height(8.dp))
                            Divider(color = Color(0xFFE3D9C6).copy(alpha = 0.2f))
                            Spacer(modifier = Modifier.height(6.dp))
                            
                            BookAIInteractions(viewModel = viewModel, book = book)
                        }
                    }
                }
            }
        }
    }
}

// 4. AI Wisdom & Reports Section
@Composable
fun AIWisdomAndReportsSection(
    viewModel: DiaryViewModel,
    selectedDateString: String
) {
    val context = LocalContext.current
    
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.End
    ) {
        Text(
            text = "التقارير التحليلية والحكمة النفسية 🧠📊",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = Color(0xFF385723)
        )
        Text(
            text = "يقوم الذكاء الاصطناعي بقراءة حالتك ومزاجك للأسبوع وتوليد تقرير متكامل.",
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray,
            textAlign = TextAlign.End
        )

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = { viewModel.generateWeeklyMoodReport() },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A6B5D)),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            enabled = !viewModel.isWeeklyMoodAnalysisLoading
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (viewModel.isWeeklyMoodAnalysisLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White)
                    Text("جاري استنطاق الذكاء الاصطناعي...", fontSize = 12.sp)
                } else {
                    Text("🧠 توليد التقرير السلوكي والمزاجي الأسبوعي", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFAF7F0)),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color(0xFFE3D9C6).copy(alpha = 0.4f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
            ) {
                if (viewModel.isWeeklyMoodAnalysisLoading) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = Color(0xFF4A6B5D))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("جاري تحليل مزاجك وأنماطك العاطفية...", fontSize = 11.sp, color = Color.Gray)
                    }
                } else if (viewModel.weeklyMoodAnalysisResult.isNotEmpty()) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Box(modifier = Modifier.weight(1f)) {
                            LazyColumn(modifier = Modifier.fillMaxSize()) {
                                item {
                                    Text(
                                        text = viewModel.weeklyMoodAnalysisResult,
                                        fontSize = 12.sp,
                                        lineHeight = 18.sp,
                                        textAlign = TextAlign.End,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { viewModel.weeklyMoodAnalysisResult = "" }) {
                                Icon(Icons.Default.Delete, contentDescription = "حذف التقرير", tint = Color.Red.copy(alpha = 0.6f))
                            }

                            Button(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("AI Mood Report", viewModel.weeklyMoodAnalysisResult)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "تم نسخ تقرير المزاج إلى الحافظة! 📋", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Text("نسخ التقرير 📋", fontSize = 10.sp)
                            }
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "اضغط بالأعلى لتوليد تحليل نفسي عميق وتوصيات لمزاجك وسلوكياتك الأسبوعية ⚡",
                            fontSize = 11.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            }
        }
    }
}

// 5. Shared Attachment Rendering Component (Supporting Audio Transcription & Players)
@Composable
fun RenderAttachmentsInsideRead(
    viewModel: DiaryViewModel,
    entry: DiaryEntry
) {
    val context = LocalContext.current
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalAlignment = Alignment.End
    ) {
        // PDF Attachment
        if (entry.hasPdf && !entry.pdfPath.isNullOrEmpty() && !entry.pdfPath.startsWith("drawing:")) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFE2F0D9), RoundedCornerShape(8.dp))
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = Color(0xFFC0392B))
                Text("📄 ملف PDF ملحق: ${entry.pdfPath}", fontSize = 11.sp, color = Color.Black)
            }
        }

        // Web Link Attachment
        if (!entry.webLinks.isNullOrEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFFAF7F0), RoundedCornerShape(8.dp))
                    .padding(8.dp)
                    .clickable {
                        Toast.makeText(context, "فتح الرابط: ${entry.webLinks}", Toast.LENGTH_SHORT).show()
                    },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Icon(Icons.Default.Link, contentDescription = null, tint = Color.Blue)
                Text("🔗 رابط ملحق: ${entry.webLinks}", fontSize = 11.sp, color = Color.Blue)
            }
        }

        // Drawing Attachment
        val isDrawing = entry.hasPdf && entry.pdfPath?.startsWith("drawing:") == true
        if (isDrawing) {
            val drawingData = entry.pdfPath!!.substringAfter("drawing:")
            Text("🎨 رسم لوحي ملحق:", fontSize = 10.sp, color = Color.Gray)
            ViewOnlyWhiteboard(
                drawingData = drawingData,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
            )
        }

        // Photo Attachment
        if (entry.hasPhoto && !entry.photoPath.isNullOrEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF0F4F8), RoundedCornerShape(8.dp))
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Icon(Icons.Default.Photo, contentDescription = null, tint = Color(0xFF385723))
                Text("📷 صورة ملحقة: ${entry.photoPath.substringAfterLast("/")}", fontSize = 11.sp)
            }
        }

        // Video Attachment
        if (entry.hasVideo && !entry.videoPath.isNullOrEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFFAF0F0), RoundedCornerShape(8.dp))
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Icon(Icons.Default.VideoLibrary, contentDescription = null, tint = Color.Red)
                Text("📹 مقطع فيديو ملحق: ${entry.videoPath.substringAfterLast("/")}", fontSize = 11.sp)
            }
        }

        // Audio Recording Player with Speech-to-Text Transcription! (MANDATORY REQUIREMENT)
        if (entry.hasAudio && !entry.audioPath.isNullOrEmpty()) {
            val audioPath = entry.audioPath
            val isTranscribing = viewModel.transcribingAudioPaths[audioPath] == true
            val transcribedText = viewModel.audioTranscriptions[audioPath]

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFE2F0D9).copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                    .padding(10.dp),
                horizontalAlignment = Alignment.End
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "تشغيل الصوت", tint = Color(0xFF385723))
                    Text("🎤 تسجيل صوتي ملحق (اضغط للتشغيل)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF385723))
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Transcribe Button & View
                if (!transcribedText.isNullOrEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White, RoundedCornerShape(6.dp))
                            .border(1.dp, Color.LightGray.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                            .padding(8.dp)
                    ) {
                        Text(
                            text = transcribedText,
                            fontSize = 11.sp,
                            lineHeight = 16.sp,
                            color = Color.DarkGray,
                            textAlign = TextAlign.End,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                } else {
                    Button(
                        onClick = { viewModel.transcribeAudio(audioPath, entry.title + " " + entry.content) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF385723)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                        modifier = Modifier.height(28.dp),
                        enabled = !isTranscribing
                    ) {
                        if (isTranscribing) {
                            CircularProgressIndicator(modifier = Modifier.size(12.dp), color = Color.White, strokeWidth = 1.5.dp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("جاري تحويل الريكورد...", fontSize = 9.sp)
                        } else {
                            Text("📝 تحويل الريكورد إلى نص مكتوب", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// 6. Add Book Dialog Composable (With drawing whiteboard canvas & multiple media options)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBookDialog(
    viewModel: DiaryViewModel,
    selectedDateString: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("book_draft_prefs", Context.MODE_PRIVATE) }
    var bookTitle by remember { mutableStateOf(sharedPrefs.getString("book_title", "") ?: "") }
    var bookNotes by remember { mutableStateOf(sharedPrefs.getString("book_notes", "") ?: "") }
    var rating by remember { mutableStateOf(sharedPrefs.getInt("book_rating", 3)) }

    // Media Paths
    var pdfPath by remember { mutableStateOf(sharedPrefs.getString("book_pdf_path", "") ?: "") }
    var audioPath by remember { mutableStateOf(sharedPrefs.getString("book_audio_path", "") ?: "") }
    var photoPath by remember { mutableStateOf(sharedPrefs.getString("book_photo_path", "") ?: "") }
    var videoPath by remember { mutableStateOf(sharedPrefs.getString("book_video_path", "") ?: "") }
    var webLink by remember { mutableStateOf(sharedPrefs.getString("book_web_link", "") ?: "") }
    var drawingData by remember { mutableStateOf(sharedPrefs.getString("book_drawing_data", "") ?: "") }

    LaunchedEffect(bookTitle, bookNotes, rating, pdfPath, audioPath, photoPath, videoPath, webLink, drawingData) {
        sharedPrefs.edit().apply {
            putString("book_title", bookTitle)
            putString("book_notes", bookNotes)
            putInt("book_rating", rating)
            putString("book_pdf_path", pdfPath)
            putString("book_audio_path", audioPath)
            putString("book_photo_path", photoPath)
            putString("book_video_path", videoPath)
            putString("book_web_link", webLink)
            putString("book_drawing_data", drawingData)
            apply()
        }
    }

    var isDrawingActive by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .padding(8.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFE3D9C6))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "إضافة كتاب جديد للمكتبة 📖✨",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF385723)
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = bookTitle,
                    onValueChange = { bookTitle = it },
                    label = { Text("عنوان الكتاب", textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth()) },
                    textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.End),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = bookNotes,
                    onValueChange = { bookNotes = it },
                    label = { Text("ملاحظات واقتباسات أو مؤلف الكتاب", textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth()) },
                    textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.End),
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 4
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Rating Selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row {
                        (1..5).forEach { star ->
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = if (star <= rating) Color(0xFFF39C12) else Color.LightGray,
                                modifier = Modifier
                                    .size(28.dp)
                                    .clickable { rating = star }
                            )
                        }
                    }
                    Text("تقييمك للكتاب:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(12.dp))
                Divider(color = Color.LightGray.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(8.dp))

                Text("الملحقات الكاملة للكتاب (PDF، صوت، رسم، إلخ) 📎:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)

                Spacer(modifier = Modifier.height(8.dp))

                // Media Inputs Row / Columns
                OutlinedTextField(
                    value = pdfPath,
                    onValueChange = { pdfPath = it },
                    label = { Text("مسار ملف PDF (أو انقر للمحاكاة)", textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth()) },
                    leadingIcon = {
                        IconButton(onClick = { pdfPath = "/storage/emulated/0/Documents/book_${bookTitle.take(3)}.pdf" }) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                        }
                    },
                    textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.End, fontSize = 11.sp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = webLink,
                    onValueChange = { webLink = it },
                    label = { Text("رابط مرجعي للموقع أو الكتاب الإلكتروني", textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth()) },
                    leadingIcon = { Icon(Icons.Default.Link, contentDescription = null) },
                    textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.End, fontSize = 11.sp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Quick Simulators for Audio/Photo/Video
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Button(
                        onClick = { audioPath = "/storage/emulated/0/VoiceRecordings/book_notes.m4a" },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = if (audioPath.isNotEmpty()) Color(0xFFE2F0D9) else Color(0xFFFAF7F0)),
                        contentPadding = PaddingValues(vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(if (audioPath.isNotEmpty()) "🎤 صوت ملحق" else "🎤 أضف صوت", fontSize = 10.sp, color = Color.DarkGray)
                    }

                    Button(
                        onClick = { photoPath = "/storage/emulated/0/DCIM/book_cover.jpg" },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = if (photoPath.isNotEmpty()) Color(0xFFE2F0D9) else Color(0xFFFAF7F0)),
                        contentPadding = PaddingValues(vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(if (photoPath.isNotEmpty()) "📷 غلاف ملحق" else "📷 أضف غلاف", fontSize = 10.sp, color = Color.DarkGray)
                    }

                    Button(
                        onClick = { videoPath = "/storage/emulated/0/DCIM/book_review.mp4" },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = if (videoPath.isNotEmpty()) Color(0xFFE2F0D9) else Color(0xFFFAF7F0)),
                        contentPadding = PaddingValues(vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(if (videoPath.isNotEmpty()) "📹 فيديو ملحق" else "📹 أضف فيديو", fontSize = 10.sp, color = Color.DarkGray)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Hand-drawing Interactive Whiteboard
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Switch(
                        checked = isDrawingActive,
                        onCheckedChange = { isDrawingActive = it }
                    )
                    Text("رسم مخطط ذهني أو انطباع للرواية يدوياً 🎨:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                if (isDrawingActive) {
                    Spacer(modifier = Modifier.height(6.dp))
                    InteractiveWhiteboard(
                        initialDrawingData = drawingData,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        onDrawingChanged = { drawingData = it }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.LightGray),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("إلغاء", color = Color.Black)
                    }

                    Button(
                        onClick = {
                            if (bookTitle.trim().isNotEmpty()) {
                                val finalPdf = if (drawingData.isNotEmpty()) "drawing:$drawingData" else pdfPath
                                viewModel.insertBook(
                                    title = bookTitle.trim(),
                                    notes = bookNotes.trim(),
                                    dateString = selectedDateString,
                                    audioPath = audioPath.ifEmpty { null },
                                    photoPath = photoPath.ifEmpty { null },
                                    videoPath = videoPath.ifEmpty { null },
                                    pdfPath = finalPdf.ifEmpty { null },
                                    webLinks = webLink.ifEmpty { null },
                                    importance = rating
                                )
                                Toast.makeText(context, "تم حفظ الكتاب وملحقاته بنجاح! 📖", Toast.LENGTH_SHORT).show()
                                sharedPrefs.edit().clear().apply()
                                onDismiss()
                            } else {
                                Toast.makeText(context, "يرجى كتابة عنوان الكتاب أولاً", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF385723)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("حفظ الكتاب", color = Color.White)
                    }
                }
            }
        }
    }
}

// 7. Book AI interactions Composable (Reading assistance / Summarization Chat)
@Composable
fun BookAIInteractions(
    viewModel: DiaryViewModel,
    book: DiaryEntry
) {
    var isAILoading by remember { mutableStateOf(false) }
    var aiResponseText by remember { mutableStateOf("") }
    
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.End
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    isAILoading = true
                    scope.launch {
                        try {
                            val prompt = "قم بتوليد خطة تطبيقية وسلوكية ممتازة لممارسة حكمة هذا الكتاب وتلخيص نقاطه الأساسية لمدة ٧ أيام:\nالكتاب: ${book.title}\nالملاحظات المرفقة: ${book.content}"
                            val systemPrompt = "أنت معالج سلوكي متميز ومرشد قرائي ممتع. تقوم بتوليد خطط دقيقة ومحفزة في نقاط وبأسلوب عربي راقٍ."
                            aiResponseText = GeminiService.getQuickResponse(prompt, systemPrompt)
                        } catch (e: Exception) {
                            aiResponseText = "عذراً، فشل توليد الخطة: ${e.localizedMessage}"
                        } finally {
                            isAILoading = false
                        }
                    }
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFAF7F0)),
                border = BorderStroke(1.dp, Color(0xFFE3D9C6)),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(vertical = 4.dp),
                enabled = !isAILoading
            ) {
                Text("📅 خطة تطبيقية ٧ أيام", fontSize = 9.sp, color = Color.DarkGray)
            }

            Button(
                onClick = {
                    isAILoading = true
                    scope.launch {
                        try {
                            val prompt = "أريد تلخيصاً شاملاً ومكثفاً جداً لأهم فكرة فلسفية وتطبيق سلوكي لهذا الكتاب وملاحظاته المكتوبة:\nالكتاب: ${book.title}\nالملاحظات المكتوبة: ${book.content}"
                            val systemPrompt = "أنت مستشار قراءة ذكي تلخص الأفكار المعقدة بسرعة فائقة ودفء عربي."
                            aiResponseText = GeminiService.getQuickResponse(prompt, systemPrompt)
                        } catch (e: Exception) {
                            aiResponseText = "عذراً، فشل توليد التلخيص: ${e.localizedMessage}"
                        } finally {
                            isAILoading = false
                        }
                    }
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFAF7F0)),
                border = BorderStroke(1.dp, Color(0xFFE3D9C6)),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(vertical = 4.dp),
                enabled = !isAILoading
            ) {
                Text("🧠 تلخيص فكرة الكتاب", fontSize = 9.sp, color = Color.DarkGray)
            }
        }

        if (isAILoading) {
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(modifier = Modifier.size(14.dp), color = Color(0xFF385723))
                Spacer(modifier = Modifier.width(6.dp))
                Text("يتفكر الذكاء الاصطناعي في سطور كتابك...", fontSize = 9.sp, color = Color.Gray)
            }
        }

        if (aiResponseText.isNotEmpty() && !isAILoading) {
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFE2F0D9).copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                    .padding(10.dp)
            ) {
                Column {
                    Text(
                        text = aiResponseText,
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                        color = Color.DarkGray,
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { aiResponseText = "" },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "مسح", tint = Color.Gray, modifier = Modifier.size(14.dp))
                        }

                        TextButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Book AI Wisdom", aiResponseText)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "تم نسخ التلخيص بنجاح! 📋", Toast.LENGTH_SHORT).show()
                            },
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("نسخ 📋", fontSize = 9.sp, color = Color(0xFF385723), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
