package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.LifeEvent
import com.example.viewmodel.DiaryViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: DiaryViewModel,
    onNavigateToCompose: () -> Unit
) {
    val entries by viewModel.allEntries.collectAsState()
    val events by viewModel.allEvents.collectAsState()
    val habits by viewModel.allHabits.collectAsState()
    val habitLogs by viewModel.allHabitLogs.collectAsState()
    val streak by viewModel.streakCount.collectAsState()

    val isReportLoading by viewModel.isReportLoading.collectAsState()
    val generatedReport by viewModel.generatedReport.collectAsState()

    // Interactive selected date (defaults to today)
    var selectedDateString by remember { mutableStateOf(viewModel.getCurrentDateString()) }

    // Dialog flags
    var showMoodDialog by remember { mutableStateOf(false) }
    var showAddHabitDialog by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }
    var showQuickConsultantDialog by remember { mutableStateOf(false) }

    // Find existing sleep/sports/meds for the selected date to initialize sliders
    val sleepEvent = events.find { it.dateString == selectedDateString && it.type == "SLEEP" }
    val sportsEvent = events.find { it.dateString == selectedDateString && it.type == "EXERCISE" }
    val medEvent = events.find { it.dateString == selectedDateString && it.type == "MEDICINE" }

    var sleepHours by remember(selectedDateString, sleepEvent) {
        val floatVal = sleepEvent?.value?.replace(" س", "")?.replace(" ساعات", "")?.trim()?.toFloatOrNull()
        mutableStateOf(floatVal ?: 8f)
    }

    var sportsMinutes by remember(selectedDateString, sportsEvent) {
        val intVal = sportsEvent?.value?.replace(" د", "")?.replace(" دقيقة", "")?.trim()?.toIntOrNull()
        mutableStateOf(intVal ?: 10)
    }

    var medTaken by remember(selectedDateString, medEvent) {
        mutableStateOf(medEvent != null)
    }

    // Active habit logs for selected date
    val completedHabitIdsForSelectedDate = habitLogs
        .filter { it.dateString == selectedDateString && it.isCompleted }
        .map { it.habitId }

    // DYNAMIC HEALTH INDEX CALCULATION
    val healthIndex = remember(sleepHours, sportsMinutes, medTaken, completedHabitIdsForSelectedDate, habits) {
        val sleepPoints = (sleepHours / 8f).coerceAtMost(1f) * 25f
        val sportsPoints = (sportsMinutes / 30f).coerceAtMost(1f) * 25f
        val medPoints = if (medTaken) 25f else 0f
        val habitsCompletedCount = completedHabitIdsForSelectedDate.size
        val totalHabitsCount = habits.size
        val habitsPoints = if (totalHabitsCount > 0) (habitsCompletedCount.toFloat() / totalHabitsCount) * 25f else 25f
        (sleepPoints + sportsPoints + medPoints + habitsPoints).toInt()
    }

    // Selected Date Description
    val dateArabic = viewModel.getFormattedDateArabic(selectedDateString)
    val dayArabic = viewModel.getDayOfWeekArabic(selectedDateString)

    // Current Time Dynamic Greetings
    val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val greetingTitle = if (currentHour < 12) "صباح التفاؤل والنشاط والتصالح ☀️" else "مساء الهدوء وراحة البال والاسترخاء 🌙"

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFAF7F0)) // Soothing Warm Cream
            .testTag("home_screen_lazy_column"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. App Header Calming Hero
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF4A6B5D)), // Premium Sage Green
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "✨ مساحتك النفسية الآمنة والمشفرة بالكامل",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.8f),
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = greetingTitle,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            lineHeight = 28.sp
                        ),
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "اليوم هو يوم جديد للنمو والتصالح مع الذات. تذكر أن تدوين أفكارك البسيطة اليوم يتيح لمستشارك النفسي الذكي تقديم أفضل نصائح وتوصيات سلوكية غداً.",
                        style = MaterialTheme.typography.bodySmall.copy(lineHeight = 16.sp),
                        color = Color.White.copy(alpha = 0.9f),
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // 2. Active Now & Quick actions
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE3D9C6).copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .background(Color(0xFFE9F0EC), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("إشعار مثبت بالأندرويد", fontSize = 9.sp, color = Color(0xFF4A6B5D), fontWeight = FontWeight.Bold)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("يومياتي AI • نشط الآن", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF26332C))
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(Color(0xFF4CAF50), CircleShape)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "كيف تشعر الآن يا صديقي؟ 😊 اضغط على أي من الاختصارات السريعة أدناه لتدوين مذكراتك أو رصد حالتك المزاجية مباشرة وبمنتهى الخصوصية والأمان.",
                        style = MaterialTheme.typography.bodySmall.copy(lineHeight = 16.sp),
                        color = Color.Gray,
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val quickActions = listOf(
                            Triple("صورة", Icons.Default.PhotoCamera, "photo"),
                            Triple("مهمة", Icons.Default.CheckCircle, "task"),
                            Triple("مستشار", Icons.Default.Psychology, "consultant"),
                            Triple("مزاجي", Icons.Default.Favorite, "mood"),
                            Triple("تسجيل", Icons.Default.Mic, "record"),
                            Triple("كتابة", Icons.Default.Create, "write")
                        )

                        quickActions.forEach { (title, icon, actionKey) ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        when (actionKey) {
                                            "write", "record", "photo" -> onNavigateToCompose()
                                            "mood" -> showMoodDialog = true
                                            "task" -> showAddHabitDialog = true
                                            "consultant" -> showQuickConsultantDialog = true
                                        }
                                    }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .background(Color(0xFFFAF7F0), RoundedCornerShape(12.dp))
                                        .border(1.dp, Color(0xFFE3D9C6).copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = title,
                                        tint = when (actionKey) {
                                            "write" -> Color(0xFF3F51B5)
                                            "record" -> Color(0xFF4CAF50)
                                            "mood" -> Color(0xFFE91E63)
                                            "consultant" -> Color(0xFF009688)
                                            "task" -> Color(0xFF9C27B0)
                                            "photo" -> Color(0xFF03A9F4)
                                            else -> Color.Gray
                                        },
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(title, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF26332C))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Divider(color = Color(0xFFE3D9C6).copy(alpha = 0.4f))
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("الخصوصية محمية بقفل PIN 🔒", fontSize = 10.sp, color = Color.Gray)
                        Text("انقر على أي اختصار لتشغيله في التطبيق", fontSize = 10.sp, color = Color.Gray)
                    }
                }
            }
        }

        // 3. Streak Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE3D9C6).copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text("سلسلة الالتزام والتدوين", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("$streak يوم متتالي", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE39B5A))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(Color(0xFFFEF3C7), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🔥", fontSize = 24.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "✨ رائع! لقد دونت أفكارك اليوم وحافظت على توهج شعلتك. استمر في رعاية صحتك النفسية غداً!",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF4A6B5D),
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFFAF7F0), RoundedCornerShape(8.dp))
                            .border(1.dp, Color(0xFFE3D9C6).copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🏆 ${streak + 1} يوم", fontWeight = FontWeight.Bold, color = Color(0xFFD69F45), fontSize = 13.sp)
                            Text("أطول سلسلة تاريخية للتدوين:", fontSize = 11.sp, color = Color.Gray)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Text("متابعة الأسبوع الأخير:", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val weekDaysArabic = listOf("الجمعة", "السبت", "الأحد", "الاثنين", "الثلاثاء", "الأربعاء", "الخميس")
                        weekDaysArabic.forEachIndexed { index, day ->
                            val hasEntryOnThisDay = index <= (streak % 7)
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(day, fontSize = 9.sp, color = Color.Gray)
                                Spacer(modifier = Modifier.height(4.dp))
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .background(
                                            if (hasEntryOnThisDay) Color(0xFFFDF2E9) else Color.Transparent,
                                            CircleShape
                                        )
                                        .border(
                                            width = 1.dp,
                                            color = if (hasEntryOnThisDay) Color(0xFFE39B5A) else Color(0xFFE0DCCF),
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (hasEntryOnThisDay) {
                                        Text("🔥", fontSize = 12.sp)
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .background(Color(0xFFE0DCCF), CircleShape)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. Horizontal Day Picker (Capsule Calendar)
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "اختر اليوم لتسجيل أو عرض الأنشطة:",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF26332C),
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val calendarDays = listOf(
                        Triple("الجمعة", "10", "2026-07-10"),
                        Triple("السبت", "11", "2026-07-11"),
                        Triple("الأحد", "12", "2026-07-12"),
                        Triple("الاثنين", "13", "2026-07-13"),
                        Triple("الثلاثاء", "14", "2026-07-14"),
                        Triple("الأربعاء", "15", "2026-07-15"),
                        Triple("الخميس", "16", "2026-07-16")
                    )

                    calendarDays.forEach { (dayName, dayNum, fullDate) ->
                        val isSelected = selectedDateString == fullDate
                        Card(
                            modifier = Modifier
                                .width(46.dp)
                                .height(64.dp)
                                .clickable { selectedDateString = fullDate },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) Color(0xFF4A6B5D) else Color.White
                            ),
                            border = BorderStroke(
                                width = 1.dp,
                                color = if (isSelected) Color(0xFF4A6B5D) else Color(0xFFE3D9C6).copy(alpha = 0.5f)
                            )
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = dayName,
                                    fontSize = 8.sp,
                                    color = if (isSelected) Color.White else Color.Gray,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = dayNum,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else Color(0xFF26332C)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 5. Self-Care & Wellness Goals
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE3D9C6).copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("تلقائي الحفظ", fontSize = 10.sp, color = Color.Gray)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("أهداف الرعاية الذاتية والصحة النفسية", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = Color(0xFF26332C))
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF4A6B5D), modifier = Modifier.size(18.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Sleep goal slider
                    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.End) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("${sleepHours.toInt()} ساعات", fontWeight = FontWeight.Bold, color = Color(0xFF3F51B5), fontSize = 13.sp)
                            Text("😴 ساعات النوم:", fontSize = 12.sp, color = Color(0xFF26332C))
                        }
                        Slider(
                            value = sleepHours,
                            onValueChange = { sleepHours = it },
                            onValueChangeFinished = {
                                viewModel.logSleepForDate(selectedDateString, sleepHours)
                            },
                            valueRange = 0f..12f,
                            colors = SliderDefaults.colors(
                                activeTrackColor = Color(0xFF3F51B5),
                                inactiveTrackColor = Color(0xFFE0E0E0),
                                thumbColor = Color(0xFF3F51B5)
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Sports goal slider
                    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.End) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("$sportsMinutes دقيقة", fontWeight = FontWeight.Bold, color = Color(0xFF4A6B5D), fontSize = 13.sp)
                            Text("🏃‍♂️ التمارين الرياضية:", fontSize = 12.sp, color = Color(0xFF26332C))
                        }
                        Slider(
                            value = sportsMinutes.toFloat(),
                            onValueChange = { sportsMinutes = it.toInt() },
                            onValueChangeFinished = {
                                viewModel.logSportsForDate(selectedDateString, sportsMinutes)
                            },
                            valueRange = 0f..120f,
                            colors = SliderDefaults.colors(
                                activeTrackColor = Color(0xFF4A6B5D),
                                inactiveTrackColor = Color(0xFFE0E0E0),
                                thumbColor = Color(0xFF4A6B5D)
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Medicine taken chip
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        InputChip(
                            selected = medTaken,
                            onClick = {
                                medTaken = !medTaken
                                if (medTaken) {
                                    viewModel.logMedicineForDate(selectedDateString, "مكمل فيتامين D")
                                } else {
                                    viewModel.removeMedicineForDate(selectedDateString)
                                }
                            },
                            label = { Text(if (medTaken) "تم تناول العلاج اليوم" else "متبقي") },
                            trailingIcon = {
                                if (medTaken) Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            },
                            colors = InputChipDefaults.inputChipColors(
                                selectedContainerColor = Color(0xFFE2F0D9),
                                selectedLabelColor = Color(0xFF385723)
                            )
                        )
                        Text("💊 جرعة العلاج/الفيتامينات:", fontSize = 12.sp, color = Color(0xFF26332C))
                    }
                }
            }
        }

        // 6. Habit Checklist Panel
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE3D9C6).copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { showAddHabitDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A6B5D)),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("+ إضافة عادة", fontSize = 11.sp, color = Color.White)
                        }
                        Text("تتبع العادات السلوكية والروتين", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = Color(0xFF26332C))
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "اختر يوماً من التقويم ثم حدد العادات المنجزة لتسجيل التزامك ودعم تحليلك السلوكي اليومي.",
                        style = MaterialTheme.typography.bodySmall.copy(lineHeight = 16.sp),
                        color = Color.Gray,
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Habit Filter tags
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AssistChip(onClick = {}, label = { Text("صحة بدنية 🥦") })
                        AssistChip(onClick = {}, label = { Text("تأمل وذهن 🧠") })
                        AssistChip(onClick = {}, label = { Text("روتين يومي ☀️") })
                        AssistChip(onClick = {}, label = { Text("الكل") }, colors = AssistChipDefaults.assistChipColors(containerColor = Color(0xFFFAF7F0)))
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Habits list layout
                    habits.forEach { habit ->
                        val isDone = completedHabitIdsForSelectedDate.contains(habit.id)
                        val categoryBadge = when {
                            habit.name.contains("ماء") || habit.name.contains("رياضة") || habit.name.contains("جري") -> "صحة بدنية 🥦"
                            habit.name.contains("تأمل") || habit.name.contains("استرخاء") || habit.name.contains("اليوميات") -> "تأمل وذهن 🧠"
                            else -> "روتين يومي ☀️"
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFAF7F0)),
                            border = BorderStroke(1.dp, Color(0xFFE3D9C6).copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.toggleHabitForDate(habit.id, !isDone, selectedDateString) }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                // Delete/Alarm icons on the left
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = { viewModel.deleteHabit(habit.id) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "حذف العادة", tint = Color.LightGray, modifier = Modifier.size(16.dp))
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(Icons.Default.Notifications, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(16.dp))
                                }

                                // Text & Badges details
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.End,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.End,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            text = habit.name,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                            color = Color(0xFF26332C),
                                            textAlign = TextAlign.End
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(
                                            horizontalArrangement = Arrangement.End,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("⏰ 08:00 ص", fontSize = 10.sp, color = Color.Gray)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Box(
                                                modifier = Modifier
                                                    .background(Color(0xFFE9F0EC), RoundedCornerShape(4.dp))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(categoryBadge, fontSize = 9.sp, color = Color(0xFF4A6B5D), fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    // Circular check toggle
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .background(
                                                if (isDone) Color(0xFF4A6B5D) else Color.White,
                                                CircleShape
                                            )
                                            .border(
                                                width = 1.dp,
                                                color = if (isDone) Color(0xFF4A6B5D) else Color(0xFFE0DCCF),
                                                shape = CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isDone) {
                                            Icon(Icons.Default.Check, contentDescription = "مكتمل", tint = Color.White, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 7. AI Behavioral Reports Section
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE3D9C6).copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    Text("التقييم السلوكي وتقارير العادات الذكية (AI)", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = Color(0xFF26332C))
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "حدد الفترة الزمنية المطلوبة ليقوم الذكاء الاصطناعي برصد اتجاهاتك السلوكية وتقديم توصيات علمية لتعزيز صحتك النفسية.",
                        style = MaterialTheme.typography.bodySmall.copy(lineHeight = 16.sp),
                        color = Color.Gray,
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Buttons Grid Layout (1 Column for custom look matching screenshot)
                    val reportOptions = listOf(
                        Pair("تقييم يومي 📅", 1),
                        Pair("تقييم أسبوعي 📅", 7),
                        Pair("تقييم شهري 📅", 30),
                        Pair("ربع سنوي (90 يوماً) 📅", 90),
                        Pair("نصف سنوي (180 يوماً) 📅", 180),
                        Pair("تقييم سنوي 📅", 365),
                        Pair("فترة مخصصة 🎯", 15)
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        reportOptions.chunked(2).forEach { row ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                row.forEach { (label, days) ->
                                    Button(
                                        onClick = {
                                            val cal = Calendar.getInstance()
                                            val end = viewModel.getCurrentDateString()
                                            cal.add(Calendar.DAY_OF_YEAR, -days)
                                            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                                            val start = sdf.format(cal.time)
                                            viewModel.generateTherapyReport(start, end)
                                            showReportDialog = true
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFFFAF7F0),
                                            contentColor = Color(0xFF26332C)
                                        ),
                                        border = BorderStroke(1.dp, Color(0xFFE3D9C6).copy(alpha = 0.5f)),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            val cal = Calendar.getInstance()
                            val end = viewModel.getCurrentDateString()
                            cal.add(Calendar.DAY_OF_YEAR, -7)
                            val start = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)
                            viewModel.generateTherapyReport(start, end)
                            showReportDialog = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A6B5D)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("🧠 توليد تقرير التقييم السلوكي بالذكاء الاصطناعي", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }

        // 8. Daily Life Map & Quality Indicators
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE3D9C6).copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    Text("خريطة ومؤشرات الحياة اليومية", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = Color(0xFF26332C))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("لوحة تحليلية متكاملة لربط الأنشطة السلوكية بجودة صحتك النفسية", fontSize = 11.sp, color = Color.Gray)

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .background(Color(0xFFFAF7F0), RoundedCornerShape(6.dp))
                                .border(1.dp, Color(0xFFE3D9C6).copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("📅 $selectedDateString", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF26332C))
                        }
                        Text(
                            text = "مؤشر التوازن والصحة اليومي: $healthIndex% توازن ممتاز",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4A6B5D)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    LinearProgressIndicator(
                        progress = healthIndex / 100f,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(12.dp)
                            .clip(CircleShape),
                        color = Color(0xFF4A6B5D),
                        trackColor = Color(0xFFFAF7F0)
                    )

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "تم حساب توازن يومك تلقائياً بناءً على النوم، الأنشطة البدنية، الالتزام بالأدوية وسجل يومياتك النفسي في هذا التاريخ.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        lineHeight = 14.sp,
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // 4 Neat Indicator Metric cards in a grid
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Sleep Card
                            Card(
                                modifier = Modifier.weight(1f),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F4F8)),
                                border = BorderStroke(1.dp, Color(0xFFD0E0F0)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("جودة النوم 😴", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("${sleepHours.toInt()} ساعات", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF3F51B5))
                                    Spacer(modifier = Modifier.height(4.dp))
                                    LinearProgressIndicator(progress = (sleepHours / 8f).coerceAtMost(1f), color = Color(0xFF3F51B5), modifier = Modifier.fillMaxWidth().height(4.dp))
                                }
                            }
                            // Exercise Card
                            Card(
                                modifier = Modifier.weight(1f),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF8F2)),
                                border = BorderStroke(1.dp, Color(0xFFD5ECD9)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("الحركة والرياضة 🏃‍♂️", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("$sportsMinutes دقيقة", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4A6B5D))
                                    Spacer(modifier = Modifier.height(4.dp))
                                    LinearProgressIndicator(progress = (sportsMinutes / 30f).coerceAtMost(1f), color = Color(0xFF4A6B5D), modifier = Modifier.fillMaxWidth().height(4.dp))
                                }
                            }
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Medicine Card
                            Card(
                                modifier = Modifier.weight(1f),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFEFDF0)),
                                border = BorderStroke(1.dp, Color(0xFFFBF4C4)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("التزام الأدوية 💊", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(if (medTaken) "1 من أصل 1" else "0 من أصل 1", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD69F45))
                                    Spacer(modifier = Modifier.height(4.dp))
                                    LinearProgressIndicator(progress = if (medTaken) 1f else 0f, color = Color(0xFFD69F45), modifier = Modifier.fillMaxWidth().height(4.dp))
                                }
                            }
                            // Habits Complete Card
                            Card(
                                modifier = Modifier.weight(1f),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFAF5FF)),
                                border = BorderStroke(1.dp, Color(0xFFECD5FD)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("إكمال العادات 🎯", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    val completedCount = completedHabitIdsForSelectedDate.size
                                    Text("$completedCount من أصل ${habits.size}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF9C27B0))
                                    Spacer(modifier = Modifier.height(4.dp))
                                    val progressVal = if (habits.isNotEmpty()) completedCount.toFloat() / habits.size else 0f
                                    LinearProgressIndicator(progress = progressVal, color = Color(0xFF9C27B0), modifier = Modifier.fillMaxWidth().height(4.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        // 9. Chronological Timeline Progress List
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onNavigateToCompose,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A6B5D)),
                    modifier = Modifier.testTag("add_diary_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "أضف", tint = Color.White)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("تدوين جديد", color = Color.White)
                }
                Text(
                    text = "شريط الزمن المتكامل وخطوات اليوم ⏳",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF26332C)
                )
            }
        }

        // Timeline Items
        val dayEvents = events.filter { it.dateString == selectedDateString }
        if (dayEvents.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE3D9C6).copy(alpha = 0.5f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "شريط حياتك لليوم المختار فارغ حالياً.\nابدأ بتسجيل يومياتك، ساعات النوم، الأنشطة البدنية، أو أخذ أدويتك ليظهر تتبع يومك الإرشادي هنا بالترتيب الزمني.",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = Color.Gray,
                            lineHeight = 20.sp
                        )
                    }
                }
            }
        } else {
            items(dayEvents) { event ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Color(0xFFE3D9C6).copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(14.dp)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.End
                            ) {
                                Text(
                                    text = event.description,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    textAlign = TextAlign.End,
                                    color = Color(0xFF26332C)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "$dayArabic، $dateArabic • ${event.timeString}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.Gray,
                                    textAlign = TextAlign.End
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(
                                        color = when (event.type) {
                                            "DIARY" -> Color(0xFFFAF7F0)
                                            "SLEEP" -> Color(0xFFE0F2FE)
                                            "EXERCISE" -> Color(0xFFFEE2E2)
                                            "MEDICINE" -> Color(0xFFFEF3C7)
                                            "MOOD" -> Color(0xFFECFDF5)
                                            else -> Color(0xFFFAF7F0)
                                        },
                                        shape = CircleShape
                                    )
                                    .border(1.dp, Color(0xFFE3D9C6).copy(alpha = 0.5f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = event.moodIcon ?: "📌", fontSize = 20.sp)
                            }
                        }
                    }
                }
            }
        }

        // 10. Mental Health Tip of the Day
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFEFDF0)), // Soft warm yellow
                border = BorderStroke(1.dp, Color(0xFFFBF4C4)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("نصيحة اليوم للصحة النفسية", fontWeight = FontWeight.Bold, color = Color(0xFFD69F45), fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("💡", fontSize = 16.sp)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "عندما تشعر بضغط الأفكار أو القلق المفرط، خذ دقيقة كاملة لممارسة 'تنفس الصندوق' (شهيق 4 ثوانٍ، كتم النفس 4 ثوانٍ، زفير 4 ثوانٍ، كتم 4 ثوانٍ). هذا التمرين يرسل إشارات حيوية فورية للجهاز العصبي للتهدئة وخفض معدلات الكورتيزول.",
                        fontSize = 11.sp,
                        color = Color.DarkGray,
                        lineHeight = 16.sp,
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }

    // -------------------------------------------------------------
    // DIALOGS & OVERLAYS
    // -------------------------------------------------------------

    // 1. Mood Selection Dialog
    if (showMoodDialog) {
        val moodsList = listOf(
            Pair("😊", "سعيد"),
            Pair("😌", "مرتاح"),
            Pair("🤩", "متحمس"),
            Pair("😐", "طبيعي"),
            Pair("😢", "حزين"),
            Pair("😞", "مكتئب"),
            Pair("😨", "قلق"),
            Pair("😡", "غاضب"),
            Pair("🥱", "مرهق"),
            Pair("🙏", "ممتن")
        )
        Dialog(onDismissRequest = { showMoodDialog = false }) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE3D9C6).copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("سجل مزاجك الحالي 🧠", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFF26332C))
                    Spacer(modifier = Modifier.height(16.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        moodsList.chunked(2).forEach { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                row.forEach { pair ->
                                    Button(
                                        onClick = {
                                            viewModel.logCustomMood(pair.first, pair.second)
                                            showMoodDialog = false
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFFFAF7F0),
                                            contentColor = Color(0xFF26332C)
                                        ),
                                        border = BorderStroke(1.dp, Color(0xFFE3D9C6).copy(alpha = 0.5f))
                                    ) {
                                        Text("${pair.first} ${pair.second}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    TextButton(onClick = { showMoodDialog = false }) {
                        Text("إغلاق", color = Color(0xFF4A6B5D), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // 2. Add Habit Dialog
    if (showAddHabitDialog) {
        var newHabitName by remember { mutableStateOf("") }
        Dialog(onDismissRequest = { showAddHabitDialog = false }) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE3D9C6).copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    Text("إضافة عادة سلوكية جديدة 🌱", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFF26332C))
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = newHabitName,
                        onValueChange = { newHabitName = it },
                        label = { Text("اسم العادة (مثال: شرب الماء)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF4A6B5D),
                            focusedLabelColor = Color(0xFF4A6B5D)
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(onClick = { showAddHabitDialog = false }) {
                            Text("إلغاء", color = Color.Gray)
                        }
                        Button(
                            onClick = {
                                if (newHabitName.isNotBlank()) {
                                    viewModel.addCustomHabit(newHabitName)
                                }
                                showAddHabitDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A6B5D))
                        ) {
                            Text("إضافة", color = Color.White)
                        }
                    }
                }
            }
        }
    }

    // 3. AI Behavioral Evaluation Report Dialog
    if (showReportDialog) {
        Dialog(onDismissRequest = { showReportDialog = false }) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE3D9C6).copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 500.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .fillMaxSize()
                ) {
                    Text(
                        text = "تقرير التقييم السلوكي بالذكاء الاصطناعي 🧠",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF4A6B5D),
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "تحليل نفسي وسلوكي موثوق من مستشارك الذكي",
                        fontSize = 11.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Divider(modifier = Modifier.padding(vertical = 12.dp))

                    if (isReportLoading) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = Color(0xFF4A6B5D))
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    "جاري تحليل عاداتك وأنشطتك وتوليد التقرير السلوكي بالذكاء الاصطناعي... 🧠",
                                    fontSize = 12.sp,
                                    color = Color.Gray,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        ) {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize()
                            ) {
                                item {
                                    Text(
                                        text = generatedReport.ifEmpty { "لا توجد بيانات سلوكية كافية في هذه الفترة لتوليد التقرير. استمر في رصد عاداتك وكتابة مذكراتك." },
                                        style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                                        color = Color(0xFF26332C),
                                        textAlign = TextAlign.End,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { showReportDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A6B5D)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("تمت القراءة • فك التشفير آمن 🔐", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // 4. Quick Consultant Greeting Dialog
    if (showQuickConsultantDialog) {
        Dialog(onDismissRequest = { showQuickConsultantDialog = false }) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE3D9C6).copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    Text("المستشار النفسي الذكي (AI) 🧠", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFF4A6B5D))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "مرحباً بك! أنا مستشارك وصديقك النفسي الذكي. لمناقشة حالتك المزاجية أو اليوميات أو الفضفضة العميقة حول أي شيء، انتقل إلى علامة تبويب 'المستشار' في الأسفل لبدء محادثة مشفرة وتلقي دعم إرشادي مخصص وسريع ومثمر.",
                        style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
                        color = Color(0xFF26332C),
                        textAlign = TextAlign.End
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { showQuickConsultantDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A6B5D)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("حسناً، فهمت", color = Color.White)
                    }
                }
            }
        }
    }
}
