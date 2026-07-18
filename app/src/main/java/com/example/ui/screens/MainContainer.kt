package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandIn
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.viewmodel.DiaryViewModel
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainContainer(
    viewModel: DiaryViewModel,
    onNavigateToCompose: () -> Unit
) {
    val currentTab = viewModel.currentTabState
    var isBubbleExpanded by remember { mutableStateOf(false) }
    var showDailyTasksDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    var floatingInputText by remember { mutableStateOf("") }

    // Collect habit flow states to calculate daily progress / pending count
    val habits by viewModel.allHabits.collectAsState()
    val habitLogs by viewModel.allHabitLogs.collectAsState()
    val todayStr = remember { viewModel.getCurrentDateString() }
    val completedHabitIdsForToday = habitLogs
        .filter { it.dateString == todayStr && it.isCompleted }
        .map { it.habitId }
    val pendingCount = habits.size - completedHabitIdsForToday.size

    // Floating Bubble (الكرة العائمة) drag offset
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }

    Box(modifier = Modifier.fillMaxSize()) {
        // 1. Primary Navigation Scaffold
        Scaffold(
            topBar = {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.background,
                    tonalElevation = 1.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left Side (Lock & Prepare button)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Lock icon container
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "خصوصية تامة وآمنة",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            // Green button "تجهيز جلسة العلاج 🎓"
                            Button(
                                onClick = { viewModel.currentTabState = 3 },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF4A6B5D),
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                modifier = Modifier.height(36.dp)
                            ) {
                                Text(
                                    text = "تجهيز جلسة العلاج 🎓",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Right Side (Title & Subtitle with Brain Icon)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Daily Tasks button (المهام اليومية) styled beautifully with dynamic pending task badge
                            Box(
                                modifier = Modifier
                                    .padding(end = 4.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFFE9F0EC))
                                    .clickable { showDailyTasksDialog = true }
                                    .border(1.dp, Color(0xFF4A6B5D).copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                                    .testTag("top_bar_tasks_badge_button")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    // Soft red badge for pending tasks
                                    if (pendingCount > 0) {
                                        Box(
                                            modifier = Modifier
                                                .background(Color(0xFFE53935), CircleShape)
                                                .size(16.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = pendingCount.toString(),
                                                color = Color.White,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    } else if (habits.isNotEmpty()) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = Color(0xFF4CAF50),
                                            modifier = Modifier.size(14.dp)
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.Assignment,
                                            contentDescription = null,
                                            tint = Color(0xFF4A6B5D),
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                    
                                    Text(
                                        text = "المهام اليومية",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF4A6B5D)
                                    )
                                }
                            }

                            Column(
                                horizontalAlignment = Alignment.End
                            ) {
                                Text(
                                    text = "يومياتي AI",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Text(
                                    text = "مساعد الصحة النفسية المتكامل",
                                    fontSize = 9.sp,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                                )
                            }

                            // Brain Icon in circle
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF4A6B5D)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("🧠", fontSize = 18.sp)
                            }
                        }
                    }
                }
            },
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.testTag("bottom_nav_bar")
                ) {
                    NavigationBarItem(
                        selected = currentTab == 4,
                        onClick = { viewModel.currentTabState = 4 },
                        icon = { Icon(Icons.Default.Settings, contentDescription = "الإعدادات") },
                        label = { Text("الإعدادات", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        modifier = Modifier.testTag("nav_settings")
                    )
                    NavigationBarItem(
                        selected = currentTab == 3,
                        onClick = { viewModel.currentTabState = 3 },
                        icon = { Icon(Icons.Default.TrendingUp, contentDescription = "التقدم والبيانات") },
                        label = { Text("التقدم والبيانات", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        modifier = Modifier.testTag("nav_report")
                    )
                    NavigationBarItem(
                        selected = currentTab == 2,
                        onClick = { viewModel.currentTabState = 2 },
                        icon = { Icon(Icons.Default.Psychology, contentDescription = "المستشار الذكي") },
                        label = { Text("المستشار", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        modifier = Modifier.testTag("nav_consultant")
                    )
                    NavigationBarItem(
                        selected = currentTab == 1,
                        onClick = { viewModel.currentTabState = 1 },
                        icon = { Icon(Icons.Default.Book, contentDescription = "اليوميات") },
                        label = { Text("اليوميات", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        modifier = Modifier.testTag("nav_diaries")
                    )
                    NavigationBarItem(
                        selected = currentTab == 0,
                        onClick = { viewModel.currentTabState = 0 },
                        icon = { Icon(Icons.Default.Home, contentDescription = "الرئيسية") },
                        label = { Text("الرئيسية", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        modifier = Modifier.testTag("nav_home")
                    )
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (currentTab) {
                    0 -> HomeScreen(viewModel = viewModel, onNavigateToCompose = onNavigateToCompose)
                    1 -> DiariesListScreen(
                        viewModel = viewModel,
                        onEditEntryInDraft = { entry ->
                            viewModel.loadEntryToDraft(entry)
                            onNavigateToCompose()
                        },
                        onNavigateToCompose = onNavigateToCompose
                    )
                    2 -> ConsultantScreen(viewModel = viewModel)
                    3 -> ReportScreen(viewModel = viewModel)
                    4 -> SettingsScreen(viewModel = viewModel)
                }
            }
        }

        // 2. Glowing Expandable Draggable Floating Ball (الكرة العائمة 🧠)
        if (viewModel.isFloatingBallEnabled) {
            Box(
                modifier = Modifier
                    .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 90.dp, end = 16.dp)
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            offsetX += dragAmount.x
                            offsetY += dragAmount.y
                        }
                    }
                    .testTag("floating_bubble_container")
            ) {
                Column(horizontalAlignment = Alignment.End) {
                    // Expanded Overlay Action Dashboard
                    AnimatedVisibility(
                        visible = isBubbleExpanded,
                        enter = fadeIn() + expandIn(expandFrom = Alignment.BottomEnd),
                        exit = fadeOut() + shrinkOut(shrinkTowards = Alignment.BottomEnd)
                    ) {
                        Card(
                            modifier = Modifier
                                .width(310.dp)
                                .heightIn(max = 520.dp)
                                .padding(bottom = 12.dp)
                                .shadow(16.dp, shape = RoundedCornerShape(24.dp)),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(24.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.End
                            ) {
                                // Header
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(
                                        onClick = { isBubbleExpanded = false },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "إغلاق", modifier = Modifier.size(16.dp))
                                    }
                                    Text(
                                        "مساعد الكتابة الذكي 🖊️",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                Divider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                                Spacer(modifier = Modifier.height(10.dp))

                                // Input Field & Ask Button
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    IconButton(
                                        onClick = {
                                            if (floatingInputText.trim().isNotEmpty()) {
                                                viewModel.askFloatingBallAssistant(floatingInputText)
                                                floatingInputText = ""
                                            }
                                        },
                                        modifier = Modifier
                                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                                            .size(36.dp)
                                    ) {
                                        Icon(Icons.Default.Send, contentDescription = "إرسال", tint = Color.White, modifier = Modifier.size(16.dp))
                                    }

                                    OutlinedTextField(
                                        value = floatingInputText,
                                        onValueChange = { floatingInputText = it },
                                        placeholder = { Text("اطلب من الذكاء الاصطناعي أن يكتب مثلاً...", fontSize = 11.sp, textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth()) },
                                        modifier = Modifier.weight(1f),
                                        textStyle = LocalTextStyle.current.copy(fontSize = 12.sp, textAlign = TextAlign.End),
                                        singleLine = true,
                                        shape = RoundedCornerShape(20.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                                            unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                                        )
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // List of quick presets (As requested from video)
                                Text(
                                    text = "اقتراحات سريعة للكتابة والبحث:",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                    textAlign = TextAlign.End,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(6.dp))

                                // Quick presets chips
                                val presets = listOf(
                                    "توليد نصائح صحية 🩺" to "قدم لي 5 نصائح صحية ونفسية ممتازة لتحسين جودة الحياة اليومية وتجاوز الضغوط.",
                                    "كتابة مقال 📝" to "اكتب لي مقالاً قصيراً وملهماً ومؤثراً عن التوازن النفسي والوعي الذاتي والصحة النفسية.",
                                    "اتجاهات المستقبل 🔮" to "ما هي أهم اتجاهات المستقبل في التكنولوجيا ومستقبل الذكاء الاصطناعي وعلم النفس الحديث؟",
                                    "التسويق وخدمة العملاء 📈" to "قدم لي نصائح مبتكرة وقيمة لتحسين خدمة العملاء والتسويق الرقمي الحديث للمشاريع.",
                                    "إدارة المشاريع 📂" to "ما هي أفضل الاستراتيجيات لإدارة المشاريع بإنتاجية عالية وتنظيم المهام بكفاءة بالغة؟"
                                )

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(max = 110.dp)
                                    ) {
                                        LazyColumn(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalArrangement = Arrangement.spacedBy(4.dp),
                                            horizontalAlignment = Alignment.End
                                        ) {
                                            items(presets.size) { index ->
                                                val preset = presets[index]
                                                Surface(
                                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                                                    shape = RoundedCornerShape(12.dp),
                                                    modifier = Modifier
                                                        .clickable {
                                                            viewModel.askFloatingBallAssistant(preset.second)
                                                        }
                                                        .padding(vertical = 1.dp)
                                                ) {
                                                    Text(
                                                        text = preset.first,
                                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                        color = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                        textAlign = TextAlign.End
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Display Response or Loading
                                if (viewModel.isFloatingBallLoading) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(110.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text("جاري الكتابة والتوليد بدقة...", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                        }
                                    }
                                } else if (viewModel.floatingBallResponse.isNotEmpty()) {
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .weight(1f, fill = false)
                                            .heightIn(max = 160.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .weight(1f)
                                            ) {
                                                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                                                    item {
                                                        Text(
                                                            text = viewModel.floatingBallResponse,
                                                            fontSize = 11.sp,
                                                            lineHeight = 16.sp,
                                                            textAlign = TextAlign.End,
                                                            modifier = Modifier.fillMaxWidth()
                                                        )
                                                    }
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                IconButton(
                                                    onClick = { viewModel.floatingBallResponse = "" },
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Icon(Icons.Default.Delete, contentDescription = "حذف الرد", tint = Color.Red.copy(alpha = 0.7f), modifier = Modifier.size(14.dp))
                                                }
                                                Button(
                                                    onClick = {
                                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                        val clip = ClipData.newPlainText("AI Generated Content", viewModel.floatingBallResponse)
                                                        clipboard.setPrimaryClip(clip)
                                                        Toast.makeText(context, "تم نسخ النص المولد للحافظة بنجاح! 📋", Toast.LENGTH_SHORT).show()
                                                    },
                                                    shape = RoundedCornerShape(8.dp),
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                    modifier = Modifier.height(24.dp)
                                                ) {
                                                    Text("نسخ النص 📋", fontSize = 9.sp)
                                                }
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                Divider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f))
                                Spacer(modifier = Modifier.height(8.dp))

                                // Quick navigation shortcuts at bottom
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    QuickShortcutButton("📝 تدوين") {
                                        isBubbleExpanded = false
                                        onNavigateToCompose()
                                    }
                                    QuickShortcutButton("🧠 المستشار") {
                                        isBubbleExpanded = false
                                        viewModel.currentTabState = 2
                                    }
                                    QuickShortcutButton("🎓 العلاج") {
                                        isBubbleExpanded = false
                                        viewModel.currentTabState = 3
                                    }
                                }
                            }
                        }
                    }

                    // Glowing Floating Ball trigger button
                    FloatingActionButton(
                        onClick = { isBubbleExpanded = !isBubbleExpanded },
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = Color.White,
                        shape = CircleShape,
                        modifier = Modifier
                            .size(60.dp)
                            .testTag("glowing_floating_ball")
                    ) {
                        Text("🧠", fontSize = 28.sp)
                    }
                }
            }
        }

        // 3. Daily Tasks Overlay Dialog
        if (showDailyTasksDialog) {
            DailyTasksDialog(
                viewModel = viewModel,
                onDismiss = { showDailyTasksDialog = false }
            )
        }
    }
}

@Composable
fun QuickShortcutButton(
    label: String,
    onClick: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}
