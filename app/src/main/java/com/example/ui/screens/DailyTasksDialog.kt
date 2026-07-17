package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.viewmodel.DiaryViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyTasksDialog(
    viewModel: DiaryViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val habits by viewModel.allHabits.collectAsState()
    val habitLogs by viewModel.allHabitLogs.collectAsState()

    var activeTab by remember { mutableStateOf(0) } // 0: Checklist, 1: Reports
    var newTaskName by remember { mutableStateOf("") }

    // Selected date for checklist (defaults to today)
    val todayStr = remember { viewModel.getCurrentDateString() }
    
    // Active habit logs for today
    val completedHabitIdsForToday = habitLogs
        .filter { it.dateString == todayStr && it.isCompleted }
        .map { it.habitId }

    // AI report configuration
    var reportType by remember { mutableStateOf("WEEKLY") } // DAILY, WEEKLY, MONTHLY, CUSTOM
    var customStartDate by remember { mutableStateOf("") }
    var customEndDate by remember { mutableStateOf("") }

    // Initialize custom dates
    LaunchedEffect(Unit) {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val cal = Calendar.getInstance()
        customEndDate = sdf.format(cal.time)
        cal.add(Calendar.DAY_OF_YEAR, -7)
        customStartDate = sdf.format(cal.time)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp),
                horizontalAlignment = Alignment.End
            ) {
                // Header Row
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
                            text = "المهام اليومية والالتزام 📋",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Divider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                Spacer(modifier = Modifier.height(10.dp))

                // Custom Two-Tab Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFFAF7F0), RoundedCornerShape(12.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Tab 2: Reports
                    Button(
                        onClick = { activeTab = 1 },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (activeTab == 1) MaterialTheme.colorScheme.primary else Color.Transparent,
                            contentColor = if (activeTab == 1) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("تقارير الذكاء الاصطناعي 🧠", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Tab 1: Checklist
                    Button(
                        onClick = { activeTab = 0 },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (activeTab == 0) MaterialTheme.colorScheme.primary else Color.Transparent,
                            contentColor = if (activeTab == 0) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("قائمة المهام اليوم 📝", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Tab Content
                Box(modifier = Modifier.weight(1f)) {
                    if (activeTab == 0) {
                        // --- CHECKLIST TAB ---
                        Column(modifier = Modifier.fillMaxSize()) {
                            // Add Task Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        if (newTaskName.trim().isNotEmpty()) {
                                            viewModel.addCustomHabit(newTaskName.trim())
                                            newTaskName = ""
                                            Toast.makeText(context, "تم إضافة المهمة بنجاح! 🎯", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                                    modifier = Modifier.height(50.dp)
                                ) {
                                    Text("+ إضافة", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }

                                OutlinedTextField(
                                    value = newTaskName,
                                    onValueChange = { newTaskName = it },
                                    placeholder = { Text("مثال: القراءة اليومية، الصلاة، الجري...", fontSize = 12.sp, textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth()) },
                                    modifier = Modifier.weight(1f),
                                    textStyle = LocalTextStyle.current.copy(fontSize = 13.sp, textAlign = TextAlign.End),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))
                            Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                            Spacer(modifier = Modifier.height(10.dp))

                            // Tasks List
                            if (habits.isEmpty()) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("📝", fontSize = 48.sp)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text("لا توجد مهام حالية مضافة بعد.", fontSize = 13.sp, color = Color.Gray, textAlign = TextAlign.Center)
                                    }
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(habits) { habit ->
                                        val isDone = completedHabitIdsForToday.contains(habit.id)
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFAF7F0).copy(alpha = 0.6f)),
                                            shape = RoundedCornerShape(14.dp),
                                            border = BorderStroke(1.dp, Color(0xFFE3D9C6).copy(alpha = 0.3f))
                                        ) {
                                            Column(modifier = Modifier.fillMaxWidth()) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clickable {
                                                            viewModel.toggleHabitForDate(habit.id, !isDone, todayStr)
                                                        }
                                                        .padding(12.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    // Left: Delete Action
                                                    IconButton(
                                                        onClick = { viewModel.deleteHabit(habit.id) },
                                                        modifier = Modifier.size(28.dp)
                                                    ) {
                                                        Icon(Icons.Default.Delete, contentDescription = "حذف المهمة", tint = Color.Red.copy(alpha = 0.5f), modifier = Modifier.size(18.dp))
                                                    }

                                                    // Right: Info & Status
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                                    ) {
                                                        Column(horizontalAlignment = Alignment.End) {
                                                            Text(
                                                                text = habit.name,
                                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                                color = MaterialTheme.colorScheme.onSurface,
                                                                textAlign = TextAlign.End
                                                            )
                                                            Text(
                                                                text = if (habit.isReminderEnabled) "🔔 منبه نشط: ${habit.reminderTime}" else "🔕 لا توجد تنبيهات",
                                                                fontSize = 10.sp,
                                                                color = if (habit.isReminderEnabled) MaterialTheme.colorScheme.primary else Color.Gray
                                                            )
                                                        }

                                                        // Customized Circular Checkbox
                                                        Box(
                                                            modifier = Modifier
                                                                .size(24.dp)
                                                                .background(
                                                                    if (isDone) MaterialTheme.colorScheme.primary else Color.White,
                                                                    RoundedCornerShape(6.dp)
                                                                )
                                                                .border(
                                                                    1.dp,
                                                                    if (isDone) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.5f),
                                                                    RoundedCornerShape(6.dp)
                                                                ),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            if (isDone) {
                                                                Icon(Icons.Default.Check, contentDescription = "مكتمل", tint = Color.White, modifier = Modifier.size(14.dp))
                                                            }
                                                        }
                                                    }
                                                }

                                                // Expanded inline quick set reminder area
                                                Divider(color = Color(0xFFE3D9C6).copy(alpha = 0.15f))
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    // Minute and hour adjusters
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                    ) {
                                                        val timeParts = (habit.reminderTime ?: "08:30").split(":")
                                                        var hour = timeParts.getOrNull(0)?.toIntOrNull() ?: 8
                                                        var minute = timeParts.getOrNull(1)?.toIntOrNull() ?: 30

                                                        IconButton(
                                                            onClick = {
                                                                hour = (hour + 1) % 24
                                                                viewModel.updateHabitReminder(habit.id, String.format(Locale.US, "%02d:%02d", hour, minute), habit.isReminderEnabled)
                                                            },
                                                            modifier = Modifier.size(26.dp)
                                                        ) {
                                                            Text("+س", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                                        }

                                                        Text(
                                                            text = String.format(Locale.US, "%02d:%02d", hour, minute),
                                                            fontSize = 12.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.primary
                                                        )

                                                        IconButton(
                                                            onClick = {
                                                                minute = (minute + 5) % 60
                                                                viewModel.updateHabitReminder(habit.id, String.format(Locale.US, "%02d:%02d", hour, minute), habit.isReminderEnabled)
                                                            },
                                                            modifier = Modifier.size(26.dp)
                                                        ) {
                                                            Text("+د", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                                        }
                                                    }

                                                    // Enable reminder switch
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                    ) {
                                                        Text("منبه المهمة", fontSize = 10.sp, color = Color.Gray)
                                                        Switch(
                                                            checked = habit.isReminderEnabled,
                                                            onCheckedChange = { isEnabled ->
                                                                viewModel.updateHabitReminder(habit.id, habit.reminderTime ?: "08:30", isEnabled)
                                                                val msg = if (isEnabled) "تم تفعيل منبه: ${habit.name}" else "تم إلغاء المنبه"
                                                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                                            }
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // --- REPORTS TAB ---
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.End
                        ) {
                            Text(
                                text = "توليد تقرير الالتزام بالمهام بالذكاء الاصطناعي 🧠",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary,
                                textAlign = TextAlign.End
                            )
                            Text(
                                text = "اختر الفترة الزمنية المناسبة وسيقوم المستشار الذكي بتحليل أدائك وسلوكياتك وربطها بالصحة النفسية:",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray,
                                textAlign = TextAlign.End,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Period Selection Row (Scrollable or Row of Chips)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                val types = listOf(
                                    Triple("CUSTOM", "فترة مخصصة 🎯", "مخصص"),
                                    Triple("MONTHLY", "تقرير شهري 📅", "شهر"),
                                    Triple("WEEKLY", "تقرير أسبوعي 📅", "أسبوع"),
                                    Triple("DAILY", "تقرير يومي 📅", "يوم")
                                )

                                types.forEach { (typeKey, label, shortLabel) ->
                                    val isSelected = reportType == typeKey
                                    Surface(
                                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color(0xFFFAF7F0),
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFFE3D9C6).copy(alpha = 0.5f)),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable {
                                                reportType = typeKey
                                                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                                                val cal = Calendar.getInstance()
                                                customEndDate = sdf.format(cal.time)
                                                when (typeKey) {
                                                    "DAILY" -> {
                                                        customStartDate = sdf.format(cal.time)
                                                    }
                                                    "WEEKLY" -> {
                                                        cal.add(Calendar.DAY_OF_YEAR, -7)
                                                        customStartDate = sdf.format(cal.time)
                                                    }
                                                    "MONTHLY" -> {
                                                        cal.add(Calendar.DAY_OF_YEAR, -30)
                                                        customStartDate = sdf.format(cal.time)
                                                    }
                                                }
                                            }
                                    ) {
                                        Box(
                                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 2.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = shortLabel,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Custom date fields shown if "CUSTOM" or always for extra precision
                            AnimatedVisibility(visible = reportType == "CUSTOM") {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFAF7F0).copy(alpha = 0.5f)),
                                    shape = RoundedCornerShape(14.dp),
                                    border = BorderStroke(1.dp, Color(0xFFE3D9C6).copy(alpha = 0.3f))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        horizontalAlignment = Alignment.End
                                    ) {
                                        Text("أدخل التواريخ المطلوبة بدقة (YYYY-MM-DD):", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.height(8.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            OutlinedTextField(
                                                value = customStartDate,
                                                onValueChange = { customStartDate = it },
                                                label = { Text("تاريخ البدء", fontSize = 10.sp) },
                                                textStyle = LocalTextStyle.current.copy(fontSize = 12.sp, textAlign = TextAlign.Center),
                                                modifier = Modifier.weight(1f),
                                                singleLine = true
                                            )
                                            OutlinedTextField(
                                                value = customEndDate,
                                                onValueChange = { customEndDate = it },
                                                label = { Text("تاريخ الانتهاء", fontSize = 10.sp) },
                                                textStyle = LocalTextStyle.current.copy(fontSize = 12.sp, textAlign = TextAlign.Center),
                                                modifier = Modifier.weight(1f),
                                                singleLine = true
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // AI Action Button
                            Button(
                                onClick = {
                                    if (customStartDate.isNotEmpty() && customEndDate.isNotEmpty()) {
                                        viewModel.generateTasksReport(customStartDate, customEndDate)
                                    } else {
                                        Toast.makeText(context, "يرجى تحديد التواريخ بشكل صحيح أولاً", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                enabled = !viewModel.isTasksReportLoading
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (viewModel.isTasksReportLoading) {
                                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                                        Text("جاري توليد التقرير ذهنياً...", fontSize = 12.sp)
                                    } else {
                                        Text("🧠 توليد وتحليل التقرير بالذكاء الاصطناعي", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Display AI Report output
                            if (viewModel.tasksReportResult.isNotEmpty() && !viewModel.isTasksReportLoading) {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFAF7F0).copy(alpha = 0.8f)),
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(1.dp, Color(0xFFE3D9C6).copy(alpha = 0.5f))
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Box(modifier = Modifier.weight(1f)) {
                                            LazyColumn(modifier = Modifier.fillMaxSize()) {
                                                item {
                                                    Text(
                                                        text = viewModel.tasksReportResult,
                                                        fontSize = 12.sp,
                                                        lineHeight = 18.sp,
                                                        textAlign = TextAlign.End,
                                                        modifier = Modifier.fillMaxWidth()
                                                    )
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))
                                        Divider(color = Color(0xFFE3D9C6).copy(alpha = 0.4f))
                                        Spacer(modifier = Modifier.height(6.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            IconButton(
                                                onClick = { viewModel.tasksReportResult = "" },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(Icons.Default.Delete, contentDescription = "مسح", tint = Color.Red.copy(alpha = 0.6f))
                                            }

                                            Button(
                                                onClick = {
                                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                    val clip = ClipData.newPlainText("AI Generated Tasks Report", viewModel.tasksReportResult)
                                                    clipboard.setPrimaryClip(clip)
                                                    Toast.makeText(context, "تم نسخ تقرير الالتزام للحافظة بنجاح! 📋", Toast.LENGTH_SHORT).show()
                                                },
                                                shape = RoundedCornerShape(8.dp),
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                                modifier = Modifier.height(32.dp)
                                            ) {
                                                Text("نسخ التقرير 📋", fontSize = 10.sp)
                                            }
                                        }
                                    }
                                }
                            } else if (viewModel.isTasksReportLoading) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(
                                            text = "يقوم الذكاء الاصطناعي الآن بقراءة وتحليل التزامك بالمهام اليومية وربطها بالصحة النفسية بدقة بالغة...",
                                            fontSize = 11.sp,
                                            color = Color.Gray,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.padding(horizontal = 24.dp)
                                        )
                                    }
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f)
                                        .background(Color(0xFFFAF7F0).copy(alpha = 0.4f), RoundedCornerShape(16.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "اختر الفترة بالأعلى ثم اضغط توليد للحصول على تحليل نفسي وسلوكي مذهل لمستواك وإنتاجيتك ⚡",
                                        fontSize = 11.sp,
                                        color = Color.Gray.copy(alpha = 0.7f),
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(horizontal = 24.dp)
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
