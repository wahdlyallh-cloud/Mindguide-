package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.DiaryEntry
import com.example.viewmodel.DiaryViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiariesListScreen(
    viewModel: DiaryViewModel,
    onEditEntryInDraft: (DiaryEntry) -> Unit,
    onNavigateToCompose: () -> Unit
) {
    val entries by viewModel.allEntries.collectAsState()
    val scope = rememberCoroutineScope()

    // Single-Diary AI helper state
    var showAiHelperDialog by remember { mutableStateOf(false) }
    var selectedEntryForAi by remember { mutableStateOf<DiaryEntry?>(null) }
    var aiHelperResult by remember { mutableStateOf("") }
    var aiHelperLoading by remember { mutableStateOf(false) }

    // Append Text / Edit State
    var showAppendDialog by remember { mutableStateOf(false) }
    var selectedEntryForAppend by remember { mutableStateOf<DiaryEntry?>(null) }
    var appendTextValue by remember { mutableStateOf("") }

    // Selected Type Filter: "diary" (اليومية) or "reflection" (خاطرة)
    var selectedTypeFilter by remember { mutableStateOf("diary") }

    // Grouping entries by dateString (yyyy-MM-dd)
    val groupedEntries = remember(entries, selectedTypeFilter) {
        entries.filter { !it.isArchived && !it.isDeleted && it.entryType == selectedTypeFilter }
            .groupBy { it.dateString }
            .toList()
            .sortedByDescending { it.first }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    viewModel.draftEntryType = selectedTypeFilter
                    onNavigateToCompose()
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                modifier = Modifier.testTag("diaries_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = if (selectedTypeFilter == "diary") "أضف يومية" else "أضف خاطرة")
            }
        },
        floatingActionButtonPosition = FabPosition.End
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.End
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            var showTasksDialogInsideDiaries by remember { mutableStateOf(false) }
            
            if (showTasksDialogInsideDiaries) {
                DailyTasksDialog(
                    viewModel = viewModel,
                    onDismiss = { showTasksDialogInsideDiaries = false }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { showTasksDialogInsideDiaries = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4A6B5D),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Assignment,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Text("المهام اليومية 📋", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Text(
                    text = if (selectedTypeFilter == "diary") "سجل مذكراتك اليومية 📚" else "سجل خواطرك وأفكارك 💭",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    textAlign = TextAlign.End,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = if (selectedTypeFilter == "diary") {
                    "يتم تجميع مشاركاتك المتعددة في نفس اليوم تلقائياً داخل بطاقة يومية موحدة تفادياً للتشتت."
                } else {
                    "اكتب خواطرك، تطلعاتك، والرسائل السريعة لقلبك وعقلك في مساحة آمنة ومنعزلة."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(14.dp))
            
            // Symmetrical Selector Row: Left is خاطرة, Right is يومية
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Left Card: خاطرة (Reflection)
                val isReflection = selectedTypeFilter == "reflection"
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { selectedTypeFilter = "reflection" }
                        .border(
                            width = 1.5.dp,
                            color = if (isReflection) MaterialTheme.colorScheme.primary else Color.Transparent,
                            shape = RoundedCornerShape(16.dp)
                        )
                        .testTag("filter_reflection_tab"),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isReflection) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                        }
                    ),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = if (isReflection) 3.dp else 1.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("💭", fontSize = 24.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "خاطرة",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (isReflection) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Right Card: يومية (Diary)
                val isDiary = selectedTypeFilter == "diary"
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { selectedTypeFilter = "diary" }
                        .border(
                            width = 1.5.dp,
                            color = if (isDiary) MaterialTheme.colorScheme.primary else Color.Transparent,
                            shape = RoundedCornerShape(16.dp)
                        )
                        .testTag("filter_diary_tab"),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDiary) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                        }
                    ),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = if (isDiary) 3.dp else 1.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("📚", fontSize = 24.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "يومية",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (isDiary) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (groupedEntries.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Text("💭", fontSize = 64.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (selectedTypeFilter == "diary") "لا توجد يوميات مكتوبة بعد." else "لا توجد خواطر مكتوبة بعد.",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (selectedTypeFilter == "diary") {
                                "اضغط على زر الإضافة بالأسفل لتبدأ بكتابة مذكراتك وصنع شريط ذكرياتك الملون."
                            } else {
                                "اضغط على زر الإضافة بالأسفل لتبدأ بكتابة خاطرتك الأولى وتدوين تأملاتك السريعة."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("diaries_lazy_list"),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(groupedEntries) { (dateStr, dayEntries) ->
                        DayGroupCard(
                            dateString = dateStr,
                            dayEntries = dayEntries,
                            viewModel = viewModel,
                            onAiHelper = { entry ->
                                selectedEntryForAi = entry
                                aiHelperResult = ""
                                showAiHelperDialog = true
                            },
                            onAppend = { entry ->
                                selectedEntryForAppend = entry
                                appendTextValue = ""
                                showAppendDialog = true
                            },
                            onDelete = { id ->
                                viewModel.deleteEntry(id)
                            }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(80.dp)) // Space for FAB
                    }
                }
            }
        }
    }

    // AI Helper Dialog (Summarize / CBT Distortions / Tomorrow Plan)
    if (showAiHelperDialog && selectedEntryForAi != null) {
        val entry = selectedEntryForAi!!
        Dialog(onDismissRequest = { showAiHelperDialog = false }) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 550.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "مساعد مذكرتك الذكية 🧠",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "اختر أحد المحاور لمساعدة الذكاء الاصطناعي في تحليل وتلخيص مذكرتك وتوجيهك بوعي:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Button(
                            onClick = {
                                aiHelperLoading = true
                                scope.launch {
                                    aiHelperResult = viewModel.getDiaryAssistantAction("PLAN", entry.content)
                                    aiHelperLoading = false
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer)
                        ) {
                            Text("خطة الغد", fontSize = 11.sp)
                        }
                        Button(
                            onClick = {
                                aiHelperLoading = true
                                scope.launch {
                                    aiHelperResult = viewModel.getDiaryAssistantAction("DISTORTIONS", entry.content)
                                    aiHelperLoading = false
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
                        ) {
                            Text("أخطاء CBT", fontSize = 11.sp)
                        }
                        Button(
                            onClick = {
                                aiHelperLoading = true
                                scope.launch {
                                    aiHelperResult = viewModel.getDiaryAssistantAction("SUMMARIZE", entry.content)
                                    aiHelperLoading = false
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("تلخيص", fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Divider()
                    Spacer(modifier = Modifier.height(12.dp))

                    if (aiHelperLoading) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    } else if (aiHelperResult.isNotEmpty()) {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            horizontalAlignment = Alignment.End
                        ) {
                            item {
                                Text(
                                    text = aiHelperResult,
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.End,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "اضغط على أحد الخيارات في الأعلى للبدء بالتحليل التلقائي.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { showAiHelperDialog = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("إغلاق")
                    }
                }
            }
        }
    }

    // Append / Revision text Dialog
    if (showAppendDialog && selectedEntryForAppend != null) {
        val entry = selectedEntryForAppend!!
        Dialog(onDismissRequest = { showAppendDialog = false }) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "إضافة تعديل أو تحديث للمذكرة ✏️",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "اكتب الكلمات أو الجمل الإضافية بالأسفل، وسيتم ربطها مباشرة كملحق مذيل بالوقت والتاريخ الحاليين تلقائياً.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = appendTextValue,
                        onValueChange = { appendTextValue = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .testTag("append_text_input"),
                        placeholder = { Text("مثال: في المساء شعرت بتحسن كبير والحمد لله وسأخلد للنوم الآن...", textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth()) },
                        textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.End)
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        TextButton(onClick = { showAppendDialog = false }) { Text("إلغاء") }
                        Button(
                            onClick = {
                                if (appendTextValue.isNotBlank()) {
                                    viewModel.appendOrEditEntry(entry.id, appendTextValue) {
                                        showAppendDialog = false
                                    }
                                }
                            },
                            modifier = Modifier.testTag("save_append_button")
                        ) {
                            Text("حفظ التحديث")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DayGroupCard(
    dateString: String,
    dayEntries: List<DiaryEntry>,
    viewModel: DiaryViewModel,
    onAiHelper: (DiaryEntry) -> Unit,
    onAppend: (DiaryEntry) -> Unit,
    onDelete: (Int) -> Unit
) {
    val dayArabic = viewModel.getDayOfWeekArabic(dateString)
    val dateArabic = viewModel.getFormattedDateArabic(dateString)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.End
        ) {
            // Group Header Label (Day Name + Date)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Emojis summary for that day
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    dayEntries.flatMap { it.moods.split(",") }.filter { it.isNotEmpty() }.distinct().take(4).forEach { emoji ->
                        Text(text = emoji, fontSize = 20.sp)
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = dayArabic,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = dateArabic,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Divider()
            Spacer(modifier = Modifier.height(12.dp))

            // Group Child Diary Cards
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                dayEntries.forEach { entry ->
                    ChildDiaryCard(
                        entry = entry,
                        onAiHelper = { onAiHelper(entry) },
                        onAppend = { onAppend(entry) },
                        onDelete = { onDelete(entry.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun ChildDiaryCard(
    entry: DiaryEntry,
    onAiHelper: () -> Unit,
    onAppend: () -> Unit,
    onDelete: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("child_diary_card_${entry.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Card Time, Edit Status, Star Indicators Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left elements: edit tag, importance
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "حذف", tint = Color.Red.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onAppend, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "تعديل", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onAiHelper, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Psychology, contentDescription = "مساعد ذكي", tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
                    }

                    if (entry.isEdited) {
                        Surface(
                            color = Color(0xFFFEF3C7),
                            contentColor = Color(0xFFD97706),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.padding(horizontal = 4.dp)
                        ) {
                            Text(
                                "تم التعديل",
                                fontSize = 9.sp,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Right: Star rating and exact creation time
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Row(modifier = Modifier.padding(end = 8.dp)) {
                        repeat(5) { index ->
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = if (index < entry.importance) Color(0xFFFBBF24) else Color(0xFFE5E7EB),
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                    Text(
                        text = entry.timeString,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Title
            Text(
                text = entry.title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Content text (clickable to expand/collapse if long)
            Text(
                text = entry.content,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.End,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                maxLines = if (expanded) Int.MAX_VALUE else 4
            )

            // Render Attached Media Labels beautifully
            if (entry.hasPhoto || entry.hasAudio || entry.hasVideo || entry.hasPdf || !entry.webLinks.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (entry.hasPdf) {
                        AttachmentBadge(icon = "📄", label = "ملف PDF")
                    }
                    if (entry.hasVideo) {
                        AttachmentBadge(icon = "🎥", label = "فيديو")
                    }
                    if (entry.hasPhoto) {
                        AttachmentBadge(icon = "🖼️", label = "صورة")
                    }
                    if (entry.hasAudio) {
                        AttachmentBadge(icon = "🎤", label = "تسجيل")
                    }
                    if (!entry.webLinks.isNullOrEmpty()) {
                        AttachmentBadge(icon = "🔗", label = "رابط")
                    }
                    Text(
                        "المرفقات: ",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }

            // Automatic psychological mood analysis badge if calculated
            if (entry.aiMoodAnalysis.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                        .padding(10.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🧠 استنتاج الحالة المزاجية من الفضفضة",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.End
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    
                    // Parse and display individual mood tags
                    val moods = entry.aiMoodAnalysis.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.End),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        moods.take(4).forEach { moodStr ->
                            val percentPart = moodStr.takeWhile { it.isDigit() || it == '%' }.replace("%", "").trim().toIntOrNull() ?: 50
                            val namePart = moodStr.dropWhile { it.isDigit() || it == '%' || it.isWhitespace() }.trim()
                            
                            val moodColor = when (namePart) {
                                "سعيد", "متحمس", "ممتن", "مرتاح" -> Color(0xFF4A6B5D) // Calm/Happy green
                                "طبيعي" -> Color(0xFFD69F45) // Neutral Gold
                                "قلق", "غاضب" -> Color(0xFFC97A63) // Alert terracotta
                                "حزين", "مكتئب", "مرهق" -> Color(0xFF727D77) // Melancholic sage grey
                                else -> MaterialTheme.colorScheme.secondary
                            }
                            
                            Surface(
                                color = moodColor.copy(alpha = 0.08f),
                                contentColor = moodColor,
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, moodColor.copy(alpha = 0.25f)),
                                modifier = Modifier.padding(1.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = "$percentPart%", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(text = namePart, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AttachmentBadge(icon: String, label: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(6.dp),
        modifier = Modifier.padding(start = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, fontSize = 9.sp)
            Spacer(modifier = Modifier.width(2.dp))
            Text(icon, fontSize = 10.sp)
        }
    }
}
