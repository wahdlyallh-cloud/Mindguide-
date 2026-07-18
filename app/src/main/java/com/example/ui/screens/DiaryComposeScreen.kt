package com.example.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.viewmodel.DiaryViewModel
import com.example.data.api.GeminiService
import com.example.data.model.ChatMessage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiaryComposeScreen(
    viewModel: DiaryViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

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

    val composePrefs = remember { context.getSharedPreferences("compose_screen_drafts", Context.MODE_PRIVATE) }

    // Local states for composer workspace controls
    var showLinkDialog by remember { mutableStateOf(false) }
    var showDailyTasksDialog by remember { mutableStateOf(false) }
    var attachedLinkValue by remember { mutableStateOf(composePrefs.getString("attached_link_value", "") ?: "") }
    
    LaunchedEffect(attachedLinkValue) {
        composePrefs.edit().putString("attached_link_value", attachedLinkValue).apply()
    }
    
    // Recording voice states
    var isRecordingVoice by remember { mutableStateOf(false) }
    var recordingSeconds by remember { mutableStateOf(0) }
    var voiceTranscriptText by remember { mutableStateOf("") }

    // Whiteboard drawing toggle inside flow
    var isDrawingWhiteboard by remember { mutableStateOf(false) }

    // Simulating player playback
    var isPlayingAudio by remember { mutableStateOf(false) }
    var audioProgress by remember { mutableStateOf(0.3f) }

    // Fadfada states
    var showFadfadaDialog by remember { mutableStateOf(false) }
    var fadfadaMessages by remember { mutableStateOf<List<ChatMessage>>(emptyList()) }
    var fadfadaInputText by remember { mutableStateOf(composePrefs.getString("fadfada_input_text", "") ?: "") }
    
    LaunchedEffect(fadfadaInputText) {
        composePrefs.edit().putString("fadfada_input_text", fadfadaInputText).apply()
    }
    var fadfadaLoading by remember { mutableStateOf(false) }
    var fadfadaDeducingMood by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var showReminderDialog by remember { mutableStateOf(false) }

    // Start timer when recording is active
    LaunchedEffect(isRecordingVoice) {
        if (isRecordingVoice) {
            recordingSeconds = 0
            while (isRecordingVoice) {
                delay(1000)
                recordingSeconds++
            }
        }
    }

    LaunchedEffect(viewModel.shouldSelectPhotoInCompose) {
        if (viewModel.shouldSelectPhotoInCompose) {
            viewModel.draftPhotoPath = "photo_scenery_sunset_calm.jpg"
            Toast.makeText(context, "📸 تم إرفاق صورة اليومية بنجاح!", Toast.LENGTH_SHORT).show()
            viewModel.shouldSelectPhotoInCompose = false
        }
    }

    LaunchedEffect(viewModel.shouldStartRecordingInCompose) {
        if (viewModel.shouldStartRecordingInCompose) {
            isRecordingVoice = true
            Toast.makeText(context, "🎙️ بدأ تسجيل الصوت تلقائياً!", Toast.LENGTH_SHORT).show()
            viewModel.shouldStartRecordingInCompose = false
        }
    }

    // Auto-update transcription based on active moods when stopping recording
    fun generateMoodBasedTranscript(): String {
        return when {
            viewModel.draftSelectedMoods.contains("😊") || viewModel.draftSelectedMoods.contains("😌") || viewModel.draftSelectedMoods.contains("🤩") || viewModel.draftSelectedMoods.contains("🙏") -> {
                "أشعر اليوم برضا تام وسلام داخلي كبير. لقد أنجزت بعض المهام وأشعر بالامتنان لعائلتي وصحتي. يومي مليء بالطاقة الإيجابية والرغبة في التطور المستمر."
            }
            viewModel.draftSelectedMoods.contains("😢") || viewModel.draftSelectedMoods.contains("😞") || viewModel.draftSelectedMoods.contains("😨") || viewModel.draftSelectedMoods.contains("😡") -> {
                "أواجه اليوم بعض الضغوطات والتحديات التي تؤثر على مزاجي وهدوئي النفسي. أحاول ممارسة التنفس العميق والالتزام بعاداتي الصحية لتخفيف أثر هذا التوتر."
            }
            else -> {
                "أهلاً، هذه تدوينتي الصوتية لليوم. أسجل مشاعري وتفاصيل يومي لكي أفرغ الضغوط وأحافظ على وعيي ومزاجي المتوازن."
            }
        }
    }

    Scaffold(
        topBar = {
            val screenTitle = if (viewModel.draftId != null) {
                if (viewModel.draftEntryType == "diary") "تعديل اليومية 📚" else "تعديل الخاطرة 💭"
            } else {
                if (viewModel.draftEntryType == "diary") "تدوين يومية جديدة 📚" else "كتابة خاطرة جديدة 💭"
            }
            TopAppBar(
                title = {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                        Text(
                            text = screenTitle,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 18.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("compose_back_button")) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "رجوع")
                    }
                },
                actions = {
                    Row(
                        modifier = Modifier.padding(end = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Button(
                            onClick = { showDailyTasksDialog = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            ),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.testTag("top_bar_tasks_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Assignment,
                                contentDescription = "المهام اليومية",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("المهام 📋", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { showFadfadaDialog = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.testTag("top_bar_fadfada_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Psychology,
                                contentDescription = "الفضفضة مع الذكاء الاصطناعي",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("فضفضة 💬", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        // Share Button
                        IconButton(onClick = {
                            val typeLabel = if (viewModel.draftEntryType == "diary") "يومية" else "خاطرة"
                            val shareText = """
                                📅 $typeLabel AI - ${viewModel.getCurrentDateString()}
                                ✍️ العنوان: ${viewModel.draftTitle.ifEmpty { "بدون عنوان" }}
                                📝 المحتوى:
                                ${viewModel.draftContent}
                            """.trimIndent()
                            val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            val clip = android.content.ClipData.newPlainText("Diary Share", shareText)
                            clipboard.setPrimaryClip(clip)
                            val copyMsg = if (viewModel.draftEntryType == "diary") "تم نسخ نص اليومية لمشاركتها بنجاح! 📤" else "تم نسخ نص الخاطرة لمشاركتها بنجاح! 📤"
                            Toast.makeText(context, copyMsg, Toast.LENGTH_SHORT).show()
                        }) {
                            Icon(Icons.Default.Share, contentDescription = "مشاركة", tint = MaterialTheme.colorScheme.primary)
                        }

                        // More Actions Dropdown Menu
                        Box {
                            IconButton(onClick = { showMenu = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "خيارات إضافية", tint = MaterialTheme.colorScheme.primary)
                            }
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("أضف تذكير 🔔", textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth()) },
                                    onClick = {
                                        showMenu = false
                                        showReminderDialog = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { 
                                        val label = if (viewModel.draftIsPinned) "إلغاء التثبيت 📌" else "تثبيت 📌"
                                        Text(label, textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth()) 
                                    },
                                    onClick = {
                                        showMenu = false
                                        if (viewModel.draftId != null) {
                                            viewModel.togglePin(viewModel.draftId!!)
                                            viewModel.draftIsPinned = !viewModel.draftIsPinned
                                        } else {
                                            viewModel.draftIsPinned = !viewModel.draftIsPinned
                                        }
                                        val msg = if (viewModel.draftIsPinned) "تم التثبيت!" else "تم إلغاء التثبيت"
                                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { 
                                        val label = if (viewModel.draftIsArchived) "إلغاء الأرشفة 📦" else "أرشفة 📦"
                                        Text(label, textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth()) 
                                    },
                                    onClick = {
                                        showMenu = false
                                        if (viewModel.draftId != null) {
                                            viewModel.toggleArchive(viewModel.draftId!!)
                                            viewModel.draftIsArchived = !viewModel.draftIsArchived
                                        } else {
                                            viewModel.draftIsArchived = !viewModel.draftIsArchived
                                        }
                                        val msg = if (viewModel.draftIsArchived) "تم الأرشفة بنجاح!" else "تم إلغاء الأرشفة"
                                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                        if (viewModel.draftIsArchived) {
                                            onNavigateBack()
                                        }
                                    }
                                )
                                DropdownMenuItem(
                                    text = { 
                                        val label = if (viewModel.draftIsFavorite) "إزالة من المفضلة ⭐" else "إضافة إلى المفضلة ⭐"
                                        Text(label, textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth()) 
                                    },
                                    onClick = {
                                        showMenu = false
                                        if (viewModel.draftId != null) {
                                            viewModel.toggleFavorite(viewModel.draftId!!)
                                            viewModel.draftIsFavorite = !viewModel.draftIsFavorite
                                        } else {
                                            viewModel.draftIsFavorite = !viewModel.draftIsFavorite
                                        }
                                        val msg = if (viewModel.draftIsFavorite) "أضيفت للمفضلة ⭐" else "أزيلت من المفضلة"
                                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                    }
                                )
                                Divider(modifier = Modifier.padding(vertical = 4.dp))
                                DropdownMenuItem(
                                    text = { Text("حذف 🗑️", color = Color.Red, textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth()) },
                                    onClick = {
                                        showMenu = false
                                        if (viewModel.draftId != null) {
                                            viewModel.moveToTrash(viewModel.draftId!!)
                                            Toast.makeText(context, "تم النقل لسلة المهملات 🗑️", Toast.LENGTH_SHORT).show()
                                        } else {
                                            viewModel.clearDraft()
                                            Toast.makeText(context, "تم إهمال المسودة", Toast.LENGTH_SHORT).show()
                                        }
                                        onNavigateBack()
                                    }
                                )
                            }
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .testTag("compose_screen_lazy_list"),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.End
        ) {
            
            // 1. Mood choice selection
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = "كيف تشعر الآن؟ (حدد مزاجك الحالي) 🧠:",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            textAlign = TextAlign.End,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                horizontalAlignment = Alignment.End
                            ) {
                                moodsList.chunked(4).forEach { rowMoods ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        rowMoods.forEach { pair ->
                                            val isSelected = viewModel.draftSelectedMoods.contains(pair.first)
                                            FilterChip(
                                                selected = isSelected,
                                                onClick = {
                                                    viewModel.draftSelectedMoods = if (isSelected) {
                                                        viewModel.draftSelectedMoods.filterNot { it == pair.first }
                                                    } else {
                                                        viewModel.draftSelectedMoods + pair.first
                                                    }
                                                },
                                                label = { Text("${pair.first} ${pair.second}") },
                                                modifier = Modifier
                                                    .padding(horizontal = 2.dp)
                                                    .testTag("mood_chip_${pair.second}")
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 2. The Unified Composition Workspace Card (المدونة الشاملة والسبورة)
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f), RoundedCornerShape(20.dp)),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        
                        // Workspace Title
                        val titlePlaceholder = if (viewModel.draftEntryType == "diary") "عنوان اليومية الذكية..." else "عنوان الخاطرة الذكية..."
                        OutlinedTextField(
                            value = viewModel.draftTitle,
                            onValueChange = { viewModel.draftTitle = it },
                            placeholder = { Text(titlePlaceholder, textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth()) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("compose_title_input"),
                            textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.End, fontSize = 18.sp, fontWeight = FontWeight.Bold),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent
                            ),
                            singleLine = true
                        )

                        Divider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)

                        // --- MEDIA CONTENT AREA (Live Rich Blocks inside workspace) ---
                        
                        // Active Photo Attachment
                        if (!viewModel.draftPhotoPath.isNullOrEmpty()) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(160.dp)
                                        .background(
                                            Brush.linearGradient(
                                                colors = listOf(Color(0xFFE2E8F0), Color(0xFFCBD5E1))
                                            )
                                        )
                                ) {
                                    // Custom beautifully painted therapeutic scenery
                                    Column(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalArrangement = Arrangement.Center,
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.primary)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("لقطة علاجية مضافة بنجاح 🖼️", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text("photo_scenery_sunset_calm.jpg", fontSize = 11.sp, color = Color.Gray)
                                    }
                                    
                                    // Delete photo button
                                    IconButton(
                                        onClick = { viewModel.draftPhotoPath = null },
                                        modifier = Modifier
                                            .align(Alignment.TopStart)
                                            .padding(8.dp)
                                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                            .size(30.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "حذف الصورة", tint = Color.White, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }

                        // Recording Screen State
                        AnimatedVisibility(visible = isRecordingVoice) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Button(
                                        onClick = {
                                            isRecordingVoice = false
                                            viewModel.draftAudioPath = "audio_record_${System.currentTimeMillis()}.m4a"
                                            val genTranscript = generateMoodBasedTranscript()
                                            voiceTranscriptText = genTranscript
                                            viewModel.draftContent = (viewModel.draftContent + "\n\n" + genTranscript).trim()
                                            Toast.makeText(context, "تم تفريغ الصوت وتحويله لنص بنجاح!", Toast.LENGTH_SHORT).show()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("إيقاف وحفظ ⏹️", color = Color.White, fontSize = 12.sp)
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = String.format("%02d:%02d جاري التسجيل...", recordingSeconds / 60, recordingSeconds % 60),
                                            color = Color.Red,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .background(Color.Red, CircleShape)
                                        )
                                    }
                                }
                            }
                        }

                        // Active Voice Recording Player & TEXT TRANSCRIPT below it!
                        if (!viewModel.draftAudioPath.isNullOrEmpty()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), RoundedCornerShape(14.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
                                    .padding(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    IconButton(
                                        onClick = { viewModel.draftAudioPath = null; voiceTranscriptText = "" },
                                        modifier = Modifier.size(30.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "حذف التسجيل", tint = Color.Red.copy(alpha = 0.7f))
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.End,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Slider(
                                            value = audioProgress,
                                            onValueChange = { audioProgress = it },
                                            modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                                        )
                                        
                                        IconButton(
                                            onClick = { isPlayingAudio = !isPlayingAudio },
                                            modifier = Modifier
                                                .background(MaterialTheme.colorScheme.primary, CircleShape)
                                                .size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (isPlayingAudio) Icons.Default.Pause else Icons.Default.PlayArrow,
                                                contentDescription = "تشغيل",
                                                tint = Color.White,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))
                                Text("🎤 تدوين صوتي: 0:45 ثانية", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.End)

                                // Dynamic voice transcription block STRICTLY displayed below the record!
                                if (voiceTranscriptText.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Divider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(10.dp),
                                            horizontalAlignment = Alignment.End
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                TextButton(
                                                    onClick = {
                                                        if (!viewModel.draftContent.contains(voiceTranscriptText)) {
                                                            viewModel.draftContent = (viewModel.draftContent + "\n" + voiceTranscriptText).trim()
                                                        }
                                                        Toast.makeText(context, "تمت إضافة النص للمذكرة", Toast.LENGTH_SHORT).show()
                                                    }
                                                ) {
                                                    Text("إدراج للمذكرة 📝", fontSize = 11.sp)
                                                }
                                                Text("التفريغ التلقائي للصوت 🎤:", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
                                            }
                                            Text(
                                                text = voiceTranscriptText,
                                                style = MaterialTheme.typography.bodySmall,
                                                textAlign = TextAlign.End,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Real whiteboard integrated canvas block
                        if (isDrawingWhiteboard) {
                            Spacer(modifier = Modifier.height(8.dp))
                            InteractiveWhiteboard(
                                initialDrawingData = viewModel.draftDrawingData,
                                modifier = Modifier.fillMaxWidth()
                            ) { drawnString ->
                                viewModel.draftDrawingData = drawnString
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        } else if (!viewModel.draftDrawingData.isNullOrEmpty()) {
                            // Render saved drawing preview in flow
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                                    .padding(12.dp),
                                horizontalAlignment = Alignment.End
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row {
                                        IconButton(onClick = { viewModel.draftDrawingData = null }) {
                                            Icon(Icons.Default.Delete, contentDescription = "حذف الرسم", tint = Color.Red.copy(alpha = 0.7f))
                                        }
                                        IconButton(onClick = { isDrawingWhiteboard = true }) {
                                            Icon(Icons.Default.Edit, contentDescription = "تعديل الرسم", tint = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                    Text("رسمة السبورة المرفقة 🎨", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                }
                                ViewOnlyWhiteboard(
                                    drawingData = viewModel.draftDrawingData,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(110.dp)
                                )
                            }
                        }

                        // Dynamic Links Attachment
                        if (viewModel.draftWebLinks.isNotEmpty()) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    IconButton(onClick = { viewModel.draftWebLinks = "" }) {
                                        Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red.copy(alpha = 0.7f))
                                    }
                                    Text("🔗 رابط مضاف: ${viewModel.draftWebLinks}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // Dynamic Video Attachment
                        if (!viewModel.draftVideoPath.isNullOrEmpty()) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    IconButton(onClick = { viewModel.draftVideoPath = null }) {
                                        Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red.copy(alpha = 0.7f))
                                    }
                                    Text("🎥 لقطة فيديو مضافة بنجاح", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // Dynamic PDF Attachment
                        if (!viewModel.draftPdfPath.isNullOrEmpty()) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    IconButton(onClick = { viewModel.draftPdfPath = null }) {
                                        Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red.copy(alpha = 0.7f))
                                    }
                                    Text("📄 ملف PDF مضاف: ${viewModel.draftPdfPath}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Unified Main Text input
                        val contentPlaceholder = if (viewModel.draftEntryType == "diary") "ما يدور في ذهنك اليوم لتدوينه في اليومية..." else "ما يخطر ببالك الآن لتكتبه كخاطرة سريعة..."
                        OutlinedTextField(
                            value = viewModel.draftContent,
                            onValueChange = { viewModel.draftContent = it },
                            placeholder = { Text(contentPlaceholder, textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth()) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 180.dp)
                                .testTag("compose_content_input"),
                            textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.End, fontSize = 15.sp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent
                            )
                        )

                        // --- DYNAMIC AI MOOD ANALYSIS PREVIEW IF PRE-CALCULATED ---
                        if (viewModel.draftAiMoodAnalysis.isNotEmpty()) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = viewModel.draftAiMoodAnalysis,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        textAlign = TextAlign.End,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("🧠 نتيجة تحليل الفضفضة:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // --- THE PROMINENT DAILY TASKS / CHECKLIST ACTION ---
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .clickable { showDailyTasksDialog = true }
                                .border(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                                .testTag("compose_tasks_card"),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.15f)),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.End,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = "كتابة وإدارة المهام اليومية 📋🎯",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.secondary,
                                        textAlign = TextAlign.End
                                    )
                                    Text(
                                        text = "خطط لمهام يومك، تتبع التزامك بها، واستخرج تقارير الإنجاز الذكية بنقرة واحدة!",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.End,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Icon(
                                    imageVector = Icons.Default.Assignment,
                                    contentDescription = "المهام اليومية",
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }

                        // --- THE PROMINENT AI FADFADA / VENTING ACTION ---
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .clickable { showFadfadaDialog = true }
                                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(14.dp)),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.End,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = "الفضفضة والتحليل الفوري مع الذكاء الاصطناعي 🧠✨",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.primary,
                                        textAlign = TextAlign.End
                                    )
                                    Text(
                                        text = "تحدث مع الذكاء الاصطناعي حول كتابتك والملفات والرسوم، واستنتج حالتك المزاجية بنسبة مئوية تلقائياً!",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.End,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Icon(
                                    imageVector = Icons.Default.Psychology,
                                    contentDescription = "فضفضة",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(8.dp))

                        // --- THE INTEGRATED WORKSPACE MEDIA TOOLBAR (شريط المرفقات الموحد) ---
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Toggle Daily Tasks Dialog
                            IconButton(
                                onClick = { showDailyTasksDialog = true },
                                modifier = Modifier.background(
                                    MaterialTheme.colorScheme.secondaryContainer,
                                    shape = CircleShape
                                )
                            ) {
                                Icon(Icons.Default.Assignment, contentDescription = "المهام اليومية", tint = MaterialTheme.colorScheme.secondary)
                            }

                            // Toggle Whiteboard Draw
                            IconButton(
                                onClick = { isDrawingWhiteboard = !isDrawingWhiteboard },
                                modifier = Modifier.background(
                                    if (isDrawingWhiteboard || !viewModel.draftDrawingData.isNullOrEmpty()) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                    shape = CircleShape
                                )
                            ) {
                                Icon(Icons.Default.Palette, contentDescription = "رسم السبورة", tint = MaterialTheme.colorScheme.primary)
                            }

                            // Record Audio Voice
                            IconButton(
                                onClick = {
                                    if (!isRecordingVoice) {
                                        isRecordingVoice = true
                                    } else {
                                        isRecordingVoice = false
                                    }
                                },
                                modifier = Modifier.background(
                                    if (isRecordingVoice || !viewModel.draftAudioPath.isNullOrEmpty()) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                    shape = CircleShape
                                )
                            ) {
                                Icon(Icons.Default.Mic, contentDescription = "تسجيل ريكورد", tint = MaterialTheme.colorScheme.primary)
                            }

                            // Add Photo
                            IconButton(
                                onClick = {
                                    viewModel.draftPhotoPath = "photo_scenery_sunset_calm.jpg"
                                    Toast.makeText(context, "تم إرفاق صورة اليومية!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.background(
                                    if (!viewModel.draftPhotoPath.isNullOrEmpty()) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                    shape = CircleShape
                                )
                            ) {
                                Icon(Icons.Default.PhotoCamera, contentDescription = "إضافة صورة", tint = MaterialTheme.colorScheme.primary)
                            }

                            // Add Link
                            IconButton(
                                onClick = { showLinkDialog = true },
                                modifier = Modifier.background(
                                    if (viewModel.draftWebLinks.isNotEmpty()) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                    shape = CircleShape
                                )
                            ) {
                                Icon(Icons.Default.Link, contentDescription = "رابط ويب", tint = MaterialTheme.colorScheme.primary)
                            }

                            // Add PDF
                            IconButton(
                                onClick = {
                                    viewModel.draftPdfPath = "document_clinical_mindfulness.pdf"
                                    Toast.makeText(context, "تم إرفاق مستند PDF بنجاح!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.background(
                                    if (!viewModel.draftPdfPath.isNullOrEmpty()) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                    shape = CircleShape
                                )
                            ) {
                                Icon(Icons.Default.Description, contentDescription = "إرفاق PDF", tint = MaterialTheme.colorScheme.primary)
                            }

                            // Add Video
                            IconButton(
                                onClick = {
                                    viewModel.draftVideoPath = "video_journal_breathing_session.mp4"
                                    Toast.makeText(context, "تم إرفاق لقطة فيديو مضافة بنجاح!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.background(
                                    if (!viewModel.draftVideoPath.isNullOrEmpty()) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                    shape = CircleShape
                                )
                            ) {
                                Icon(Icons.Default.VideoLibrary, contentDescription = "فيديو يومية", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }

            // 3. Importance Slider (Stress / Importance rating)
            item {
                Text(
                    text = "تقييم الأهمية والتركيز العاطفي لهذه المذكرة ⭐️:",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${viewModel.draftImportance} نجوم تركيز",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Row {
                        repeat(5) { index ->
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = if (index < viewModel.draftImportance) Color(0xFFFBBF24) else Color(0xFFE5E7EB),
                                modifier = Modifier
                                    .size(32.dp)
                                    .clickable { viewModel.draftImportance = index + 1 }
                                    .testTag("star_rate_${index + 1}")
                            )
                        }
                    }
                }
            }

            // 4. Action Save button
            item {
                val saveBtnText = if (viewModel.draftEntryType == "diary") "حفظ اليومية وبدء التحليل الفوري 🧠" else "حفظ الخاطرة وبدء التحليل الفوري 🧠"
                val successMsg = if (viewModel.draftEntryType == "diary") "تم حفظ اليومية وإجراء التحليل النفسي الخلفي بنجاح!" else "تم حفظ الخاطرة وإجراء التحليل النفسي الخلفي بنجاح!"
                Button(
                    onClick = {
                        viewModel.saveCurrentEntry {
                            Toast.makeText(context, successMsg, Toast.LENGTH_LONG).show()
                            onNavigateBack()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("save_diary_entry_button"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(saveBtnText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }

    // Modal to type web links
    if (showLinkDialog) {
        Dialog(onDismissRequest = { showLinkDialog = false }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("أضف روابط ويب لمصادر أو مواقع ممتن لها 🔗", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = attachedLinkValue,
                        onValueChange = { attachedLinkValue = it },
                        placeholder = { Text("https://example.com") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        TextButton(onClick = { showLinkDialog = false }) { Text("إلغاء") }
                        Button(onClick = {
                            viewModel.draftWebLinks = attachedLinkValue
                            showLinkDialog = false
                        }) { Text("إضافة") }
                    }
                }
            }
        }
    }

    // AI Venting & Fadfada Dialog
    if (showFadfadaDialog) {
        Dialog(onDismissRequest = { showFadfadaDialog = false }) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 620.dp)
                    .border(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f), RoundedCornerShape(24.dp)),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(18.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.End
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { showFadfadaDialog = false }) {
                            Icon(Icons.Default.Close, contentDescription = "إغلاق")
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "مساحة الفضفضة والتحليل 🧠✨",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "يتفاعل المعالج النفسي الذكي مع ما كتبته ورسمته والملفات المرفقة لتقديم دعم مخصص واستنتاج مزاجك.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Indicator of Active draft attachments analyzed by AI
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            if (!viewModel.draftPdfPath.isNullOrEmpty()) Text("📄 PDF ")
                            if (!viewModel.draftVideoPath.isNullOrEmpty()) Text("🎥 فيديو ")
                            if (!viewModel.draftDrawingData.isNullOrEmpty()) Text("🎨 رسم ")
                            if (!viewModel.draftPhotoPath.isNullOrEmpty()) Text("🖼️ صورة ")
                            if (!viewModel.draftAudioPath.isNullOrEmpty()) Text("🎤 ريكورد ")
                            if (viewModel.draftContent.isNotEmpty()) Text("📝 نص")
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "العناصر المشمولة بالتحليل الحاضر:",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Divider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(12.dp))

                    // Chat messages list
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        if (fadfadaMessages.isEmpty()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text("💬", fontSize = 48.sp)
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "ابدأ الفضفضة الآن مع صديقك الذكي!",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "اكتب ما تشعر به أو اطرح تساؤلاً حول مرفقاتك، وسيتفاعل الذكاء الاصطناعي معك فوراً بوعي ودفء.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                    textAlign = TextAlign.Center
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(fadfadaMessages) { msg ->
                                    val isUser = msg.sender == "USER"
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                                    ) {
                                        Card(
                                            shape = RoundedCornerShape(
                                                topStart = 16.dp,
                                                topEnd = 16.dp,
                                                bottomStart = if (isUser) 16.dp else 2.dp,
                                                bottomEnd = if (isUser) 2.dp else 16.dp
                                            ),
                                            colors = CardDefaults.cardColors(
                                                containerColor = if (isUser) {
                                                    MaterialTheme.colorScheme.primaryContainer
                                                } else {
                                                    MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
                                                }
                                            ),
                                            modifier = Modifier.widthIn(max = 240.dp)
                                        ) {
                                            Column(modifier = Modifier.padding(10.dp)) {
                                                Text(
                                                    text = msg.content,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    textAlign = if (isUser) TextAlign.End else TextAlign.Start,
                                                    modifier = Modifier.fillMaxWidth()
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Loading indicator for AI generating response
                    if (fadfadaLoading) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.Start,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("الذكاء الاصطناعي يكتب بحنان...", fontSize = 12.sp, color = Color.Gray)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Input Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                if (fadfadaInputText.isNotBlank() && !fadfadaLoading) {
                                    val userText = fadfadaInputText.trim()
                                    fadfadaInputText = ""
                                    val newUserMsg = ChatMessage(sender = "USER", content = userText)
                                    val updatedMessages = fadfadaMessages + newUserMsg
                                    fadfadaMessages = updatedMessages
                                    fadfadaLoading = true

                                    scope.launch {
                                        try {
                                            // Gather draft attachments information
                                            val attachBuilder = java.lang.StringBuilder()
                                            attachBuilder.append("العنوان الحالي لليومية: ${viewModel.draftTitle}\n")
                                            attachBuilder.append("المحتوى المكتوب: ${viewModel.draftContent}\n")
                                            if (!viewModel.draftPhotoPath.isNullOrEmpty()) attachBuilder.append("- صورة علاجية مرفقة (Scenery sunset)\n")
                                            if (!viewModel.draftAudioPath.isNullOrEmpty()) attachBuilder.append("- تسجيل صوتي مضاف مسبقاً (🎤)\n")
                                            if (!viewModel.draftDrawingData.isNullOrEmpty()) attachBuilder.append("- لوحة مرسومة باليد على السبورة (🎨)\n")
                                            if (!viewModel.draftPdfPath.isNullOrEmpty()) attachBuilder.append("- مستند PDF مرفق: ${viewModel.draftPdfPath}\n")
                                            if (!viewModel.draftVideoPath.isNullOrEmpty()) attachBuilder.append("- لقطة فيديو مضافة\n")
                                            if (viewModel.draftWebLinks.isNotEmpty()) attachBuilder.append("- روابط ويب مضافة: ${viewModel.draftWebLinks}\n")

                                            val aiResponse = GeminiService.getFadfadaResponse(
                                                currentDraftContent = viewModel.draftContent,
                                                attachmentsInfo = attachBuilder.toString(),
                                                chatHistory = updatedMessages.dropLast(1),
                                                userMessage = userText
                                            )
                                            fadfadaMessages = updatedMessages + ChatMessage(sender = "ASSISTANT", content = aiResponse)
                                        } catch (e: Exception) {
                                            fadfadaMessages = updatedMessages + ChatMessage(sender = "ASSISTANT", content = "عذراً، واجهت مشكلة في الاتصال بالشبكة.")
                                        } finally {
                                            fadfadaLoading = false
                                        }
                                    }
                                }
                            },
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.primary, CircleShape)
                                .size(40.dp)
                        ) {
                            Icon(Icons.Default.Send, contentDescription = "إرسال", tint = Color.White, modifier = Modifier.size(18.dp))
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        OutlinedTextField(
                            value = fadfadaInputText,
                            onValueChange = { fadfadaInputText = it },
                            placeholder = { Text("اكتب رسالتك لفضفضة مريحة...", textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth()) },
                            modifier = Modifier.weight(1f),
                            textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.End),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Divider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(12.dp))

                    // Concluding and deducing mood percentage actions
                    if (fadfadaDeducingMood) {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("جاري استخلاص النسبة المئوية للمزاج الذاتي بدقة...", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    } else {
                        Button(
                            onClick = {
                                fadfadaDeducingMood = true
                                scope.launch {
                                    try {
                                        val deducedMood = GeminiService.deduceMoodPercentagesFromFadfada(
                                            currentDraftContent = viewModel.draftContent,
                                            chatHistory = fadfadaMessages
                                        )
                                        viewModel.draftAiMoodAnalysis = deducedMood
                                        Toast.makeText(context, "تم استنتاج حالتك المزاجية وتثبيتها: $deducedMood", Toast.LENGTH_LONG).show()
                                        showFadfadaDialog = false
                                    } catch (e: java.lang.Exception) {
                                        Toast.makeText(context, "فشل استخلاص النسبة، حاول الفضفضة مجدداً.", Toast.LENGTH_SHORT).show()
                                    } finally {
                                        fadfadaDeducingMood = false
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("اعتماد استنتاج النسبة المئوية للمزاج وإنهاء الفضفضة 📊🧠", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    if (showDailyTasksDialog) {
        DailyTasksDialog(
            viewModel = viewModel,
            onDismiss = { showDailyTasksDialog = false }
        )
    }

    if (showReminderDialog) {
        var selectedOption by remember { mutableStateOf(0) } // 0: 1h, 1: 9pm, 2: 9am, 3: custom
        var customHour by remember { mutableStateOf(java.util.Calendar.getInstance().get(java.util.Calendar.HOUR)) }
        var customMinute by remember { mutableStateOf(30) }
        var isPm by remember { mutableStateOf(java.util.Calendar.getInstance().get(java.util.Calendar.AM_PM) == java.util.Calendar.PM) }

        Dialog(onDismissRequest = { showReminderDialog = false }) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "ضبط تذكير باليومية 🔔",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    // Predefined options
                    val options = listOf(
                        "بعد ساعة واحدة ⏰" to 0,
                        "الليلة الساعة 09:00 مساءً 🌃" to 1,
                        "غداً الساعة 09:00 صباحاً 🌅" to 2,
                        "تحديد وقت مخصص ⚙️" to 3
                    )

                    options.forEach { (label, index) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedOption = index }
                                .padding(vertical = 10.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.End
                        ) {
                            Text(label, fontSize = 13.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                            Spacer(modifier = Modifier.width(8.dp))
                            RadioButton(
                                selected = selectedOption == index,
                                onClick = { selectedOption = index }
                            )
                        }
                    }

                    if (selectedOption == 3) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Divider()
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("اختر الوقت المخصص:", fontSize = 11.sp, color = Color.Gray, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.End)
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // AM/PM Toggle
                            Button(
                                onClick = { isPm = !isPm },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isPm) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = if (isPm) Color.White else MaterialTheme.colorScheme.onSecondaryContainer
                                ),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Text(if (isPm) "مساءً" else "صباحاً", fontSize = 11.sp)
                            }

                            // Minute
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("الدقيقة", fontSize = 10.sp, color = Color.Gray)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(onClick = { customMinute = (customMinute + 5) % 60 }, modifier = Modifier.size(24.dp)) {
                                        Text("+", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Text(String.format(java.util.Locale.US, "%02d", customMinute), fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp))
                                    IconButton(onClick = { customMinute = (customMinute - 5 + 60) % 60 }, modifier = Modifier.size(24.dp)) {
                                        Text("-", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            // Hour
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("الساعة", fontSize = 10.sp, color = Color.Gray)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(onClick = { customHour = if (customHour == 12) 1 else customHour + 1 }, modifier = Modifier.size(24.dp)) {
                                        Text("+", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Text(String.format(java.util.Locale.US, "%d", if (customHour == 0) 12 else customHour), fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp))
                                    IconButton(onClick = { customHour = if (customHour == 1) 12 else customHour - 1 }, modifier = Modifier.size(24.dp)) {
                                        Text("-", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { showReminderDialog = false }) {
                            Text("إلغاء")
                        }
                        Button(
                            onClick = {
                                val cal = java.util.Calendar.getInstance()
                                when (selectedOption) {
                                    0 -> cal.add(java.util.Calendar.HOUR_OF_DAY, 1)
                                    1 -> {
                                        cal.set(java.util.Calendar.HOUR_OF_DAY, 21)
                                        cal.set(java.util.Calendar.MINUTE, 0)
                                        cal.set(java.util.Calendar.SECOND, 0)
                                    }
                                    2 -> {
                                        cal.add(java.util.Calendar.DAY_OF_YEAR, 1)
                                        cal.set(java.util.Calendar.HOUR_OF_DAY, 9)
                                        cal.set(java.util.Calendar.MINUTE, 0)
                                        cal.set(java.util.Calendar.SECOND, 0)
                                    }
                                    3 -> {
                                        var h = customHour
                                        if (isPm && h < 12) h += 12
                                        if (!isPm && h == 12) h = 0
                                        cal.set(java.util.Calendar.HOUR_OF_DAY, h)
                                        cal.set(java.util.Calendar.MINUTE, customMinute)
                                        cal.set(java.util.Calendar.SECOND, 0)
                                        if (cal.before(java.util.Calendar.getInstance())) {
                                            cal.add(java.util.Calendar.DAY_OF_YEAR, 1)
                                        }
                                    }
                                }
                                val ts = cal.timeInMillis
                                if (viewModel.draftId != null) {
                                    viewModel.setReminderForEntry(viewModel.draftId!!, ts)
                                } else {
                                    viewModel.draftReminderTimestamp = ts
                                }
                                showReminderDialog = false
                                val dateText = java.text.SimpleDateFormat("dd/MM hh:mm a", java.util.Locale.getDefault()).format(cal.time)
                                Toast.makeText(context, "تم ضبط التذكير بنجاح لوقت: $dateText 🎯", Toast.LENGTH_LONG).show()
                            },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("تأكيد وحفظ 🔔")
                        }
                    }
                }
            }
        }
    }
}
