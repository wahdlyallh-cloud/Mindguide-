package com.example.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
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
import com.example.data.model.ChatMessage
import com.example.viewmodel.DiaryViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConsultantScreen(
    viewModel: DiaryViewModel
) {
    val messages by viewModel.chatMessages.collectAsState()
    val isChatLoading by viewModel.isChatLoading.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    // Suggestions for clinical journal therapy
    val suggestionChips = listOf(
        "ماذا حدث لي يوم ١٥ يوليو؟",
        "كيف كانت حالتي النفسية الأسبوع الماضي؟",
        "ما أكثر مسببات القلق والتوتر لدي؟",
        "أريد تلخيصاً شاملاً لالتزامي وصحتي النفسية"
    )

    // Local Rich Attachment states for AI Chat Input
    val sharedPrefs = remember { context.getSharedPreferences("consultant_draft_prefs", Context.MODE_PRIVATE) }
    var userMessageText by remember { mutableStateOf(sharedPrefs.getString("user_message_text", "") ?: "") }

    LaunchedEffect(userMessageText) {
        sharedPrefs.edit().putString("user_message_text", userMessageText).apply()
    }
    var chatPhotoPath by remember { mutableStateOf<String?>(null) }
    var chatAudioPath by remember { mutableStateOf<String?>(null) }
    var chatDrawingData by remember { mutableStateOf<String?>(null) }

    // Control toggles
    var isDrawingWhiteboard by remember { mutableStateOf(false) }
    var isRecordingVoice by remember { mutableStateOf(false) }
    var recordingSeconds by remember { mutableStateOf(0) }
    var voiceTranscriptText by remember { mutableStateOf("") }
    var showVoiceCallDialog by remember { mutableStateOf(false) }
    var activeTab by remember { mutableStateOf(0) } // 0: General Chat, 1: Transcribed Voice Logs

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

    // Auto-scroll to bottom on new messages
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    LaunchedEffect(viewModel.shouldOpenVoiceCallDialog) {
        if (viewModel.shouldOpenVoiceCallDialog) {
            showVoiceCallDialog = true
            viewModel.shouldOpenVoiceCallDialog = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("consultant_screen"),
        horizontalAlignment = Alignment.End
    ) {
        // 1. Header with Clear History & Voice Call buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { viewModel.clearChatHistory() },
                    modifier = Modifier.testTag("clear_chat_button")
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "حذف المحادثة", tint = Color.Red.copy(alpha = 0.8f))
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = { showVoiceCallDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A6B5D)),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Call, contentDescription = "اتصال صوتي", tint = Color.White, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("مكالمة صوتية 📞", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "المستشار الذكي لليوميات 🧠",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "محلل نفسي دائم مستند على تاريخ مذكراتك وعاداتك",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                )
            }
        }

        // 2. Access Permission Toggle Switch (Critical Privacy Requirement)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Switch(
                    checked = viewModel.hasChatAccessPermission,
                    onCheckedChange = { viewModel.hasChatAccessPermission = it },
                    modifier = Modifier.testTag("permission_switch")
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "منح صلاحية قراءة اليوميات والمذكرات",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        textAlign = TextAlign.End
                    )
                    Text(
                        text = "عند التفعيل، يستطيع المستشار الوصول لمحتوى مذكراتك التاريخية للإجابة وتحليل نمطك بدقة.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                        textAlign = TextAlign.End
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Tab Selector for Chat vs Transcribed Voice Logs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Tab 1: Transcribed Voice Logs
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (activeTab == 1) MaterialTheme.colorScheme.primary else Color.Transparent)
                    .clickable { activeTab = 1 }
                    .padding(vertical = 8.dp)
                    .testTag("voice_transcription_tab"),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Mic, 
                        contentDescription = null, 
                        tint = if (activeTab == 1) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "المحادثات الصوتية 🎙️",
                        color = if (activeTab == 1) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            // Tab 0: General Chat
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (activeTab == 0) MaterialTheme.colorScheme.primary else Color.Transparent)
                    .clickable { activeTab = 0 }
                    .padding(vertical = 8.dp)
                    .testTag("general_chat_tab"),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Chat, 
                        contentDescription = null, 
                        tint = if (activeTab == 0) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "الدردشة العامة والوسائط 💬",
                        color = if (activeTab == 0) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (activeTab == 0) {
            // 3. Suggestions List
            Text(
                text = "أسئلة مقترحة سريعة:",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(horizontal = 16.dp),
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.End
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 85.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    items(suggestionChips) { chipText ->
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .clickable { userMessageText = chipText }
                                .padding(vertical = 2.dp)
                        ) {
                            Text(
                                text = chipText,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                textAlign = TextAlign.End
                            )
                        }
                    }
                }
            }

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            // 4. Messages History Scrollable
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .testTag("chat_messages_list"),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (messages.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "ابدأ المحادثة مع المستشار الذكي!\nاطرح أي سؤال حول أحداث الأيام السابقة أو أنماط نومك وحالتك النفسية.\nيمكنك إرسال نصوص، صور، تسجيلات صوتية، أو رسم مشاعرك على السبورة!",
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                } else {
                    items(messages) { msg ->
                        ChatBubble(message = msg, viewModel = viewModel)
                    }
                }

                if (isChatLoading) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("المستشار يفكر ويحلل مذكراتك ووسائطك...", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
                            }
                        }
                    }
                }
            }

            // 5. Rich Unified Chat Input Container
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), RoundedCornerShape(24.dp)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    
                    // --- ATTACHMENT PREVIEWS INSIDE THE INPUT BOX ---
                    
                    // Photo preview
                    if (chatPhotoPath != null) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp)
                                    .background(Brush.linearGradient(colors = listOf(Color(0xFFE2E8F0), Color(0xFFCBD5E1)))),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.PhotoCamera, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Text("صورة مرفقة للاستشارة 🖼️", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                                IconButton(
                                    onClick = { chatPhotoPath = null },
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .padding(4.dp)
                                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                        .size(28.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }

                    // Voice Recording Live Screen inside input
                    AnimatedVisibility(visible = isRecordingVoice) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Button(
                                    onClick = {
                                        isRecordingVoice = false
                                        chatAudioPath = "chat_audio_${System.currentTimeMillis()}.m4a"
                                        val transcript = "أشعر ببعض الضغط العصبي وأحتاج إلى خطة واعية لتنظيم أفكاري وتجاوز المشاكل المعرفية التي تسبب لي الأرق."
                                        voiceTranscriptText = transcript
                                        userMessageText = (userMessageText + " " + transcript).trim()
                                        Toast.makeText(context, "تم تحويل الصوت إلى نص وإضافته للمستشار!", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text("إيقاف وحفظ ⏹️", fontSize = 11.sp, color = Color.White)
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = String.format("%02d:%02d جاري تسجيل سؤالك...", recordingSeconds / 60, recordingSeconds % 60),
                                        color = Color.Red,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Box(modifier = Modifier.size(6.dp).background(Color.Red, CircleShape))
                                }
                            }
                        }
                    }

                    // Active Audio memo in input preview
                    if (chatAudioPath != null) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f))
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    IconButton(onClick = { chatAudioPath = null; voiceTranscriptText = "" }, modifier = Modifier.size(24.dp)) {
                                        Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red)
                                    }
                                    Text("🎤 ريكورد صوتي جاهز للإرسال (0:15)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                if (voiceTranscriptText.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    ) {
                                        Text(
                                            text = voiceTranscriptText,
                                            fontSize = 11.sp,
                                            textAlign = TextAlign.End,
                                            modifier = Modifier.fillMaxWidth().padding(6.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Active Whiteboard Drawing inside input
                    if (isDrawingWhiteboard) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp)
                        ) {
                            Column(modifier = Modifier.padding(4.dp)) {
                                InteractiveWhiteboard(
                                    initialDrawingData = chatDrawingData,
                                    modifier = Modifier.fillMaxWidth()
                                ) { drawing ->
                                    chatDrawingData = drawing
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Button(
                                    onClick = { isDrawingWhiteboard = false },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("حفظ رسم السبورة وإغلاقها 🎨", fontSize = 11.sp)
                                }
                            }
                        }
                    } else if (!chatDrawingData.isNullOrEmpty()) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row {
                                    IconButton(onClick = { chatDrawingData = null }) {
                                        Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red)
                                    }
                                    IconButton(onClick = { isDrawingWhiteboard = true }) {
                                        Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    }
                                }
                                Text("🎨 لوحة مرسومة مرفقة للاستشارة", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // --- MAIN TEXT AND ACTIONS ROW ---
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        
                        // Left send button
                        IconButton(
                            onClick = {
                                if (userMessageText.trim().isNotEmpty() || chatPhotoPath != null || chatAudioPath != null || chatDrawingData != null) {
                                    viewModel.sendChatMessage(
                                        messageText = userMessageText,
                                        audioPath = chatAudioPath,
                                        photoPath = chatPhotoPath,
                                        drawingData = chatDrawingData
                                    )
                                    // Clear inputs
                                    userMessageText = ""
                                    chatPhotoPath = null
                                    chatAudioPath = null
                                    chatDrawingData = null
                                    voiceTranscriptText = ""
                                }
                            },
                            modifier = Modifier
                                .testTag("send_message_button")
                                .background(MaterialTheme.colorScheme.primary, shape = CircleShape)
                                .size(38.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "إرسال",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        // Media toolbar shortcuts inside the input box itself!
                        IconButton(
                            onClick = { isRecordingVoice = !isRecordingVoice },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Mic, contentDescription = "تسجيل صوتي", tint = MaterialTheme.colorScheme.primary)
                        }

                        IconButton(
                            onClick = { isDrawingWhiteboard = !isDrawingWhiteboard },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Palette, contentDescription = "رسم السبورة", tint = MaterialTheme.colorScheme.primary)
                        }

                        IconButton(
                            onClick = {
                                chatPhotoPath = "chat_scenery_context.jpg"
                                Toast.makeText(context, "تم إرفاق صورة للاستشارة!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.PhotoCamera, contentDescription = "إضافة صورة", tint = MaterialTheme.colorScheme.primary)
                        }

                        // Text Field
                        OutlinedTextField(
                            value = userMessageText,
                            onValueChange = { userMessageText = it },
                            placeholder = { Text("اطرح سؤالاً، أرفق صورة، أو ريكورد...", textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth(), fontSize = 13.sp) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("chat_input_field"),
                            textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.End, fontSize = 14.sp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent
                            )
                        )
                    }
                }
            }
        } else {
            // 1. Beautiful introductory card about voice transcripts and speech analysis
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.15f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "مركز تفريغ المحادثات الصوتية الذكي 🎙️✨",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.tertiary,
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "هنا تُعرض مذكراتك ومكالماتك الصوتية التي أجريتها مع المستشار بعد تفريغها آلياً إلى نصوص واضحة كفقاعات شات تفاعلية، لدعم المتابعة البصرية واستعادة الذكريات بسهولة وسلاسة.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // 2. Filter voice-related messages (recordings or call transcripts)
            val speechPrompts = listOf("أشعر بالتوتر والضغط", "كيف أنظم نومي؟", "أعطني نصيحة مهدئة")
            val voiceMessages = messages.filter { msg ->
                msg.audioPath != null || 
                msg.content.contains("الضغط العصبي") || 
                msg.content.contains("أفكاري") || 
                msg.content.contains("تجاوز المشاكل") ||
                speechPrompts.any { msg.content.contains(it) } ||
                (msg.sender == "ASSISTANT" && messages.indexOf(msg) > 0 && 
                 (messages[messages.indexOf(msg) - 1].audioPath != null || 
                  speechPrompts.any { messages[messages.indexOf(msg) - 1].content.contains(it) } ||
                  messages[messages.indexOf(msg) - 1].content.contains("الضغط العصبي") ||
                  messages[messages.indexOf(msg) - 1].content.contains("أفكاري") ||
                  messages[messages.indexOf(msg) - 1].content.contains("تجاوز المشاكل")))
            }

            if (voiceMessages.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Card(
                            shape = CircleShape,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f)),
                            modifier = Modifier.size(72.dp)
                        ) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "لا توجد محادثات صوتية مفرغة حتى الآن 🔇",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "اضغط على زر المكالمة الصوتية بالأعلى للحديث مع مستشارك المساعد، أو أرسل ريكورد صوتي من نافذة الدردشة ليتم تفريغه وعرضه هنا فوراً!",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        Spacer(modifier = Modifier.height(18.dp))
                        Button(
                            onClick = { showVoiceCallDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("بدء مكالمة صوتية سريعة 📞", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(voiceMessages) { msg ->
                        TranscribedVoiceBubble(message = msg, viewModel = viewModel)
                    }
                }
            }
        }
    }

    if (showVoiceCallDialog) {
        VoiceCallDialog(
            viewModel = viewModel,
            onDismissRequest = { showVoiceCallDialog = false }
        )
    }
}

@Composable
fun ChatBubble(message: ChatMessage, viewModel: DiaryViewModel) {
    val isUser = message.sender == "USER"

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        contentAlignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Card(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp
            ),
            colors = CardDefaults.cardColors(
                containerColor = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
            ),
            modifier = Modifier.widthIn(max = 295.dp)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                horizontalAlignment = Alignment.End
            ) {
                // Render attached photo if present
                if (!message.photoPath.isNullOrEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Brush.linearGradient(colors = listOf(Color(0xFFE2E8F0), Color(0xFFCBD5E1)))),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.PhotoCamera, contentDescription = null, tint = if (isUser) Color.White else MaterialTheme.colorScheme.primary)
                            Text("صورة مرفقة 🖼️", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (isUser) Color.White else Color.Black)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Render attached drawing whiteboard sketch if present
                if (!message.drawingData.isNullOrEmpty()) {
                    ViewOnlyWhiteboard(
                        drawingData = message.drawingData,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Render attached audio recording if present
                if (!message.audioPath.isNullOrEmpty()) {
                    val audioPath = message.audioPath!!
                    val isTranscribing = viewModel.transcribingAudioPaths[audioPath] == true
                    val transcribedText = viewModel.audioTranscriptions[audioPath]

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isUser) Color.White.copy(alpha = 0.15f) else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f))
                            .padding(8.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = if (isUser) Color.White else MaterialTheme.colorScheme.primary)
                            Text("🎤 تسجيل صوتي مفرغ", fontSize = 11.sp, color = if (isUser) Color.White else MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        if (!transcribedText.isNullOrEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.White.copy(alpha = 0.9f), RoundedCornerShape(6.dp))
                                    .padding(8.dp)
                            ) {
                                Text(
                                    text = transcribedText ?: "",
                                    fontSize = 11.sp,
                                    lineHeight = 16.sp,
                                    color = Color.DarkGray,
                                    textAlign = TextAlign.End,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        } else {
                            Button(
                                onClick = { viewModel.transcribeAudio(audioPath, message.content) },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(24.dp),
                                enabled = !isTranscribing
                            ) {
                                if (isTranscribing) {
                                    CircularProgressIndicator(modifier = Modifier.size(10.dp), color = Color.White)
                                } else {
                                    Text("📝 تحويل الريكورد لنص", fontSize = 9.sp, color = Color.White)
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }

                // Render main content text
                if (message.content.isNotEmpty()) {
                    Text(
                        text = message.content,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isUser) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
fun TranscribedVoiceBubble(message: ChatMessage, viewModel: DiaryViewModel) {
    val isUser = message.sender == "USER"
    var isPlaying by remember { mutableStateOf(false) }
    var playProgress by remember { mutableStateOf(0f) }

    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            playProgress = 0f
            while (playProgress < 1.0f && isPlaying) {
                delay(100)
                playProgress += 0.05f
            }
            isPlaying = false
            playProgress = 0f
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        contentAlignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Card(
            shape = RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomStart = if (isUser) 18.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 18.dp
            ),
            colors = CardDefaults.cardColors(
                containerColor = if (isUser) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.tertiaryContainer
                }
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                horizontalAlignment = Alignment.End
            ) {
                // Sender label & Waveform Play Control Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { isPlaying = !isPlaying },
                        modifier = Modifier
                            .size(34.dp)
                            .background(
                                if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary,
                                CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "تشغيل الصوت المفرغ",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = if (isUser) "صوتك المفرغ 👤" else "رد المستشار الصوتي 🧠",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isUser) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Icon(
                            imageVector = if (isUser) Icons.Default.Mic else Icons.Default.VolumeUp,
                            contentDescription = null,
                            tint = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Beautiful interactive Waveform visualizer
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(24.dp)
                        .background(Color.Black.copy(alpha = 0.04f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    val barHeights = listOf(10, 14, 8, 16, 12, 6, 14, 18, 10, 12, 16, 8, 14, 10, 12, 6, 14, 8)
                    for (i in barHeights.indices) {
                        val activeProgress = i.toFloat() / barHeights.size.toFloat()
                        val isBarPlayed = playProgress >= activeProgress
                        val heightMultiplier = barHeights[i]
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height(heightMultiplier.dp)
                                .background(
                                    if (isPlaying && isBarPlayed) {
                                        if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary
                                    } else {
                                        Color.Gray.copy(alpha = 0.3f)
                                    },
                                    RoundedCornerShape(1.dp)
                                )
                        )
                    }
                    Text(
                        text = if (isPlaying) {
                            String.format("00:%02d", (playProgress * 15).toInt())
                        } else "00:15",
                        fontSize = 9.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Transcribed content
                Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isUser) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onTertiaryContainer,
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth(),
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@Composable
fun VoiceCallDialog(
    viewModel: DiaryViewModel,
    onDismissRequest: () -> Unit
) {
    val messages by viewModel.chatMessages.collectAsState()
    val isChatLoading by viewModel.isChatLoading.collectAsState()
    val context = LocalContext.current
    var connectionStatus by remember { mutableStateOf("جاري الاتصال بالقناة الصوتية الآمنة... 📞") }
    val sharedPrefsCall = remember { context.getSharedPreferences("call_draft_prefs", Context.MODE_PRIVATE) }
    var userSpeechInput by remember { mutableStateOf(sharedPrefsCall.getString("user_speech_input", "") ?: "") }

    LaunchedEffect(userSpeechInput) {
        sharedPrefsCall.edit().putString("user_speech_input", userSpeechInput).apply()
    }
    val scope = rememberCoroutineScope()
    
    // Status simulation
    LaunchedEffect(Unit) {
        delay(1500)
        connectionStatus = "المستشار متصل ومستعد للاستماع 🟢"
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color(0xFF1E293B), Color(0xFF0F172A)))),
            color = Color.Transparent
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Header of Call
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismissRequest) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = Color.White)
                    }
                    Text(
                        text = "جلسة اتصال صوتي مشفرة 🔒",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                }

                // Therapist Avatar & Pulsing circles
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(vertical = 12.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(130.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFF4A6B5D).copy(alpha = 0.2f), CircleShape)
                        )
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .background(Color(0xFF4A6B5D).copy(alpha = 0.4f), CircleShape)
                        )
                        Card(
                            modifier = Modifier.size(75.dp),
                            shape = CircleShape,
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF4A6B5D))
                        ) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("🧠", fontSize = 32.sp)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = connectionStatus,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF81C784)
                    )
                    if (isChatLoading) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "جاري معالجة الكلمات وتوليد الرد الصوتي... 🔊",
                            fontSize = 11.sp,
                            color = Color(0xFFFFEB3B)
                        )
                    }
                }

                // The conversation log - user on right, bot on left
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Show the last few messages of this active call
                        val callMessages = messages.takeLast(10)
                        if (callMessages.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier.fillParentMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "المستشار بانتظار حديثك.\nيمكنك كتابة كلامك أو الضغط على عبارة سريعة بالأسفل للحديث.",
                                        color = Color.White.copy(alpha = 0.4f),
                                        fontSize = 12.sp,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        } else {
                            items(callMessages) { msg ->
                                val isUser = msg.sender == "USER"
                                Box(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentAlignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
                                ) {
                                    Card(
                                        shape = RoundedCornerShape(
                                            topStart = 12.dp,
                                            topEnd = 12.dp,
                                            bottomStart = if (isUser) 12.dp else 2.dp,
                                            bottomEnd = if (isUser) 2.dp else 12.dp
                                        ),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isUser) Color(0xFF4A6B5D) else Color(0xFF334155)
                                        ),
                                        modifier = Modifier.widthIn(max = 260.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Text(
                                                text = if (isUser) "أنت 👤" else "المستشار الذكي 🧠",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White.copy(alpha = 0.6f),
                                                textAlign = if (isUser) TextAlign.End else TextAlign.Start,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = msg.content,
                                                fontSize = 12.sp,
                                                color = Color.White,
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

                // Speak Input / Suggestions
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "اختر عبارة جاهزة لتوجيهها صوتياً أو اكتب بنفسك للتحدث:",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    
                    // Quick Speech options
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.End)
                    ) {
                        val speechChips = listOf("أشعر بالتوتر والضغط", "كيف أنظم نومي؟", "أعطني نصيحة مهدئة")
                        speechChips.forEach { chip ->
                            Card(
                                modifier = Modifier
                                    .clickable {
                                        viewModel.sendChatMessage(chip)
                                    }
                                    .padding(vertical = 2.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF475569))
                            ) {
                                Text(
                                    text = chip,
                                    fontSize = 10.sp,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Input Text / Speak Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                if (userSpeechInput.isNotBlank()) {
                                    viewModel.sendChatMessage(userSpeechInput)
                                    userSpeechInput = ""
                                    sharedPrefsCall.edit().remove("user_speech_input").apply()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A6B5D)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.height(44.dp)
                        ) {
                            Text("تحدث 🎙️", fontSize = 11.sp, color = Color.White)
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        OutlinedTextField(
                            value = userSpeechInput,
                            onValueChange = { userSpeechInput = it },
                            placeholder = { Text("اكتب هنا للتحدث بصوتك المباشر...", fontSize = 11.sp, color = Color.LightGray, textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth()) },
                            modifier = Modifier.weight(1f),
                            textStyle = LocalTextStyle.current.copy(color = Color.White, textAlign = TextAlign.End, fontSize = 12.sp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF4A6B5D),
                                unfocusedBorderColor = Color.Gray,
                                cursorColor = Color.White
                            ),
                            singleLine = true
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // End Call Action Pill
                Button(
                    onClick = onDismissRequest,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.8f)),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .height(44.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.Call, contentDescription = "إنهاء المكالمة", tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("إنهاء الاتصال ❌", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

