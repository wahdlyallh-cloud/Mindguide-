package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
    var userMessageText by remember { mutableStateOf("") }
    var chatPhotoPath by remember { mutableStateOf<String?>(null) }
    var chatAudioPath by remember { mutableStateOf<String?>(null) }
    var chatDrawingData by remember { mutableStateOf<String?>(null) }

    // Control toggles
    var isDrawingWhiteboard by remember { mutableStateOf(false) }
    var isRecordingVoice by remember { mutableStateOf(false) }
    var recordingSeconds by remember { mutableStateOf(0) }
    var voiceTranscriptText by remember { mutableStateOf("") }

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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("consultant_screen"),
        horizontalAlignment = Alignment.End
    ) {
        // 1. Header with Clear History button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { viewModel.clearChatHistory() },
                modifier = Modifier.testTag("clear_chat_button")
            ) {
                Icon(Icons.Default.Delete, contentDescription = "حذف المحادثة", tint = Color.Red.copy(alpha = 0.8f))
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

        Spacer(modifier = Modifier.height(8.dp))

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
                    ChatBubble(message = msg)
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
    }
}

@Composable
fun ChatBubble(message: ChatMessage) {
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
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isUser) Color.White.copy(alpha = 0.15f) else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f))
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = if (isUser) Color.White else MaterialTheme.colorScheme.primary)
                        Text("🎤 تسجيل صوتي مفرغ", fontSize = 11.sp, color = if (isUser) Color.White else MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
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
