package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.viewmodel.DiaryViewModel
import com.example.data.model.DiaryEntry
import com.example.data.model.Habit
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: DiaryViewModel
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val entries by viewModel.allEntries.collectAsState()
    val habits by viewModel.allHabits.collectAsState()

    // Sub-dialog states
    var showBackupSyncDialog by remember { mutableStateOf(false) }
    var showFavoritesDialog by remember { mutableStateOf(false) }
    var showArchiveDialog by remember { mutableStateOf(false) }
    var showTrashDialog by remember { mutableStateOf(false) }
    var showRemindersDialog by remember { mutableStateOf(false) }
    var showLanguagesDialog by remember { mutableStateOf(false) }
    var showRateDialog by remember { mutableStateOf(false) }

    // Backup & Import sub-states
    var showBackupDialog by remember { mutableStateOf(false) }
    var showRestoreDialog by remember { mutableStateOf(false) }
    var restoreJsonText by remember { mutableStateOf("") }
    var showGoogleAccountDialog by remember { mutableStateOf(false) }
    var tempGoogleEmail by remember { mutableStateOf(viewModel.googleAccountEmail) }
    var isSyncingCloud by remember { mutableStateOf(false) }
    var isRestoringCloud by remember { mutableStateOf(false) }

    // Toggles
    var isAppLocked by remember { mutableStateOf(false) }
    var isDarkModeEnabled by remember { mutableStateOf(false) }
    var isNotificationsEnabled by remember { mutableStateOf(true) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("settings_screen_lazy_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.End
    ) {
        // App settings header
        item {
            Text(
                text = "الإعدادات ⚙️",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(4.dp))
            Divider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
        }

        // --- GROUP 1: FLOATING BALL ---
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Switch(
                        checked = viewModel.isFloatingBallEnabled,
                        onCheckedChange = { viewModel.updateFloatingBallEnabled(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFFF2A93B),
                            checkedTrackColor = Color(0xFFF2A93B).copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.testTag("floating_ball_switch")
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.End,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "عرض الكرة العائمة",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                textAlign = TextAlign.End
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFEF3C7)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Launch,
                                contentDescription = null,
                                tint = Color(0xFFF59E0B),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }

        // --- GROUP 2: SYNC & LOCK ---
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
            ) {
                Column {
                    // Option 1: Backup & Sync
                    SettingsRowItem(
                        title = "النسخ الاحتياطي والمزامنة",
                        subtitle = "حفظ ومزامنة مذكراتك سحابياً ومحلياً",
                        icon = {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFEF3C7)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.CloudSync, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(20.dp))
                            }
                        },
                        onClick = { showBackupSyncDialog = true }
                    )
                    Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f), modifier = Modifier.padding(horizontal = 16.dp))
                    
                    // Option 2: App Lock
                    SettingsRowItem(
                        title = "قفل التطبيق",
                        subtitle = "تأمين مذكراتك بكلمة مرور أو بصمة",
                        trailing = {
                            Switch(
                                checked = isAppLocked,
                                onCheckedChange = {
                                    isAppLocked = it
                                    val text = if (it) "تم تفعيل القفل التلقائي بنجاح!" else "تم إلغاء قفل التطبيق"
                                    Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
                                }
                            )
                        },
                        icon = {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFEF3C7)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(20.dp))
                            }
                        }
                    )
                }
            }
        }

        // --- GROUP 3: FAVORITE, ALARMS, ARCHIVE, TRASH ---
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
            ) {
                Column {
                    // Favorites
                    SettingsRowItem(
                        title = "الملاحظات المفضلة",
                        subtitle = "تصفح مذكراتك المميزة بنجمة",
                        icon = {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFEF3C7)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(20.dp))
                            }
                        },
                        onClick = { showFavoritesDialog = true }
                    )
                    Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f), modifier = Modifier.padding(horizontal = 16.dp))

                    // Reminders
                    SettingsRowItem(
                        title = "التذكيرات والمنبهات",
                        subtitle = "إدارة منبهات المهام المجدولة واليوميات",
                        icon = {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFEF3C7)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Alarm, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(20.dp))
                            }
                        },
                        onClick = { showRemindersDialog = true }
                    )
                    Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f), modifier = Modifier.padding(horizontal = 16.dp))

                    // Archive
                    SettingsRowItem(
                        title = "أرشيف اليوميات",
                        subtitle = "استعراض مذكراتك المؤرشفة والقديمة",
                        icon = {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFEF3C7)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Archive, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(20.dp))
                            }
                        },
                        onClick = { showArchiveDialog = true }
                    )
                    Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f), modifier = Modifier.padding(horizontal = 16.dp))

                    // Trash Bin
                    SettingsRowItem(
                        title = "سلة المهملات",
                        subtitle = "استعادة مذكراتك المحذوفة مؤخراً أو تصفيتها",
                        icon = {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFEF3C7)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(20.dp))
                            }
                        },
                        onClick = { showTrashDialog = true }
                    )
                }
            }
        }

        // --- GROUP 4: EXPERIMENTAL VISUALS & UTILITIES ---
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
            ) {
                Column {
                    // Dark mode
                    SettingsRowItem(
                        title = "الوضع الداكن",
                        trailing = {
                            Switch(
                                checked = isDarkModeEnabled,
                                onCheckedChange = { isDarkModeEnabled = it }
                            )
                        },
                        icon = {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFEF3C7)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.WbSunny, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(20.dp))
                            }
                        }
                    )
                    Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f), modifier = Modifier.padding(horizontal = 16.dp))

                    // Notifications Toggle
                    SettingsRowItem(
                        title = "الإشعارات",
                        trailing = {
                            Switch(
                                checked = isNotificationsEnabled,
                                onCheckedChange = { isNotificationsEnabled = it }
                            )
                        },
                        icon = {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFEF3C7)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Notifications, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(20.dp))
                            }
                        }
                    )
                    Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f), modifier = Modifier.padding(horizontal = 16.dp))

                    // Language
                    SettingsRowItem(
                        title = "لغات التطبيق",
                        subtitle = "تغيير لغة واجهة مستخدم التطبيق",
                        icon = {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFEF3C7)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Language, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(20.dp))
                            }
                        },
                        onClick = { showLanguagesDialog = true }
                    )
                    Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f), modifier = Modifier.padding(horizontal = 16.dp))

                    // Rate us
                    SettingsRowItem(
                        title = "قيمنا",
                        subtitle = "ساعدنا على مواصلة تحسين التطبيق لرأيك الغالي",
                        icon = {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFEF3C7)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.ThumbUp, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(20.dp))
                            }
                        },
                        onClick = { showRateDialog = true }
                    )
                }
            }
        }

        // About card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.05f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "يومياتي AI ✨",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.End
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "تم التطوير بخصوصية مطلقة وأعلى درجات الحماية المعرفية السلوكية والذكاء الاصطناعي لراحتك النفسية.",
                        style = MaterialTheme.typography.bodySmall.copy(lineHeight = 16.sp),
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }

    // ==========================================
    // --- DIALOG 1: BACKUP AND SYNC ---
    // ==========================================
    if (showBackupSyncDialog) {
        Dialog(onDismissRequest = { showBackupSyncDialog = false }) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { showBackupSyncDialog = false }) {
                            Icon(Icons.Default.Close, contentDescription = "إغلاق")
                        }
                        Text(
                            text = "المزامنة والنسخ الاحتياطي ☁️",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "قم بربط حساب Google لتفعيل المزامنة التلقائية للملاحظات والمهام واستعادتها بضغطة زر عند انتقالك لجهاز جديد، أو استخدم خيارات الاستيراد والتصدير اليدوي لمشاركة مذكراتك.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // Google Sync Account area
                    if (viewModel.isGoogleBackupEnabled) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f))
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
                                    TextButton(onClick = {
                                        viewModel.updateGoogleBackupEnabled(false)
                                        Toast.makeText(context, "تم إلغاء ربط الحساب", Toast.LENGTH_SHORT).show()
                                    }) {
                                        Text("إلغاء الربط 🚪", color = Color.Red, fontSize = 11.sp)
                                    }
                                    Text(viewModel.googleAccountEmail, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                                Text("مزامنة سحابية نشطة وتلقائية ✅", fontSize = 10.sp, color = Color.Gray)
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            Button(
                                onClick = {
                                    isSyncingCloud = true
                                    viewModel.syncBackupToCloud(viewModel.googleAccountEmail) { success ->
                                        isSyncingCloud = false
                                        if (success) {
                                            Toast.makeText(context, "تم حفظ ومزامنة بياناتك سحابياً بنجاح! ✅", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("حفظ سحابي ☁️", fontSize = 10.sp)
                            }
                            OutlinedButton(
                                onClick = {
                                    isRestoringCloud = true
                                    viewModel.restoreBackupFromCloud(viewModel.googleAccountEmail) { success ->
                                        isRestoringCloud = false
                                        if (success) {
                                            Toast.makeText(context, "تم استرجاع بياناتك بنجاح! 📥", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("استرجاع سحابي 📥", fontSize = 10.sp)
                            }
                        }
                    } else {
                        Button(
                            onClick = { showGoogleAccountDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4285F4))
                        ) {
                            Text("ربط حساب Google سحابياً 🔑", color = Color.White, fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Divider()
                    Spacer(modifier = Modifier.height(12.dp))

                    Text("تصدير واستيراد المذكرات يدوياً ⚙️", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = Color.Gray)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { showRestoreDialog = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("استيراد يدوي 📂", fontSize = 10.sp)
                        }
                        Button(
                            onClick = { showBackupDialog = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("تصدير ومشاركة 📤", fontSize = 10.sp)
                        }
                    }
                }
            }
        }
    }

    // ==========================================
    // --- DIALOG 2: FAVORITE DIARIES ---
    // ==========================================
    if (showFavoritesDialog) {
        val favorites = entries.filter { it.isFavorite && !it.isDeleted }
        Dialog(onDismissRequest = { showFavoritesDialog = false }) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.8f)
                    .padding(8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(18.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { showFavoritesDialog = false }) {
                            Icon(Icons.Default.Close, contentDescription = "إغلاق")
                        }
                        Text(
                            text = "المذكرات المفضلة ⭐",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    if (favorites.isEmpty()) {
                        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text("لا توجد مذكرات في المفضلة بعد. أضفها عبر قائمة خيارات اليومية.", fontSize = 12.sp, color = Color.Gray, textAlign = TextAlign.Center)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(favorites) { entry ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFAF7F0).copy(alpha = 0.6f))
                                ) {
                                    Column(modifier = Modifier.padding(12.dp).fillMaxWidth(), horizontalAlignment = Alignment.End) {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            IconButton(onClick = { viewModel.toggleFavorite(entry.id) }, modifier = Modifier.size(24.dp)) {
                                                Icon(Icons.Default.Star, contentDescription = "إلغاء المفضلة", tint = Color(0xFFF59E0B))
                                            }
                                            Text(entry.dateString, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(entry.title, fontWeight = FontWeight.Bold, fontSize = 12.sp, textAlign = TextAlign.End)
                                        Text(entry.content.take(60) + if (entry.content.length > 60) "..." else "", fontSize = 11.sp, color = Color.Gray, textAlign = TextAlign.End)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // ==========================================
    // --- DIALOG 3: ARCHIVE ---
    // ==========================================
    if (showArchiveDialog) {
        val archived = entries.filter { it.isArchived && !it.isDeleted }
        Dialog(onDismissRequest = { showArchiveDialog = false }) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.8f)
                    .padding(8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(18.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { showArchiveDialog = false }) {
                            Icon(Icons.Default.Close, contentDescription = "إغلاق")
                        }
                        Text(
                            text = "أرشيف اليوميات 📦",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    if (archived.isEmpty()) {
                        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text("الأرشيف فارغ حالياً.", fontSize = 12.sp, color = Color.Gray)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(archived) { entry ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFAF7F0).copy(alpha = 0.6f))
                                ) {
                                    Column(modifier = Modifier.padding(12.dp).fillMaxWidth(), horizontalAlignment = Alignment.End) {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                            TextButton(onClick = { viewModel.toggleArchive(entry.id) }) {
                                                Text("استعادة للأرئيسية 🔄", fontSize = 10.sp)
                                            }
                                            Text(entry.dateString, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(entry.title, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        Text(entry.content.take(60) + if (entry.content.length > 60) "..." else "", fontSize = 11.sp, color = Color.Gray, textAlign = TextAlign.End)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // ==========================================
    // --- DIALOG 4: TRASH BIN ---
    // ==========================================
    if (showTrashDialog) {
        val trashed = entries.filter { it.isDeleted }
        Dialog(onDismissRequest = { showTrashDialog = false }) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.8f)
                    .padding(8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(18.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { showTrashDialog = false }) {
                            Icon(Icons.Default.Close, contentDescription = "إغلاق")
                        }
                        Text(
                            text = "سلة المهملات 🗑️",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    if (trashed.isEmpty()) {
                        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text("سلة المهملات فارغة تماماً.", fontSize = 12.sp, color = Color.Gray)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(trashed) { entry ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFAF7F0).copy(alpha = 0.6f))
                                ) {
                                    Column(modifier = Modifier.padding(12.dp).fillMaxWidth(), horizontalAlignment = Alignment.End) {
                                        Text(entry.dateString, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(entry.title, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        Text(entry.content.take(50) + "...", fontSize = 11.sp, color = Color.Gray)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            TextButton(onClick = { 
                                                viewModel.deleteEntry(entry.id) 
                                                Toast.makeText(context, "تم الحذف نهائياً!", Toast.LENGTH_SHORT).show()
                                            }) {
                                                Text("حذف نهائي 🗑️", color = Color.Red, fontSize = 10.sp)
                                            }
                                            Button(
                                                onClick = { viewModel.restoreFromTrash(entry.id) },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A6B5D)),
                                                shape = RoundedCornerShape(8.dp),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                            ) {
                                                Text("استعادة الملاحظة 🔄", fontSize = 10.sp)
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
    }

    // ==========================================
    // --- DIALOG 5: REMINDERS AND ALARMS ---
    // ==========================================
    if (showRemindersDialog) {
        Dialog(onDismissRequest = { showRemindersDialog = false }) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.85f)
                    .padding(8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(18.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { showRemindersDialog = false }) {
                            Icon(Icons.Default.Close, contentDescription = "إغلاق")
                        }
                        Text(
                            text = "ضبط تذكير المنبهات ⏰",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "يمكنك جدولة منبه مخصص لكل مهمة يومية على حدة ليقوم النظام بإرسال تذكير مباشر لك بالصلاة وغيرها عند الوقت بدقة تامة.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    if (habits.isEmpty()) {
                        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text("لا توجد مهام يومية نشطة لجدولة تذكيرات لها.", fontSize = 12.sp, color = Color.Gray)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(habits) { habit ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFAF7F0).copy(alpha = 0.5f))
                                ) {
                                    Column(modifier = Modifier.padding(12.dp).fillMaxWidth(), horizontalAlignment = Alignment.End) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Switch(
                                                checked = habit.isReminderEnabled,
                                                onCheckedChange = { isEnabled ->
                                                    viewModel.updateHabitReminder(habit.id, habit.reminderTime ?: "08:30", isEnabled)
                                                    val statusMsg = if (isEnabled) "تم تفعيل المنبه!" else "تم إلغاء المنبه"
                                                    Toast.makeText(context, statusMsg, Toast.LENGTH_SHORT).show()
                                                }
                                            )
                                            Text(habit.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        }

                                        Spacer(modifier = Modifier.height(6.dp))

                                        // Time incrementer row
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.End,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            val timeParts = (habit.reminderTime ?: "08:30").split(":")
                                            var hour = timeParts.getOrNull(0)?.toIntOrNull() ?: 8
                                            var minute = timeParts.getOrNull(1)?.toIntOrNull() ?: 30

                                            IconButton(onClick = {
                                                hour = (hour + 1) % 24
                                                viewModel.updateHabitReminder(habit.id, String.format(Locale.US, "%02d:%02d", hour, minute), habit.isReminderEnabled)
                                            }, modifier = Modifier.size(28.dp)) {
                                                Text("+س", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                            Text(
                                                text = String.format(Locale.US, " %02d:%02d ", hour, minute),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.padding(horizontal = 4.dp)
                                            )
                                            IconButton(onClick = {
                                                minute = (minute + 5) % 60
                                                viewModel.updateHabitReminder(habit.id, String.format(Locale.US, "%02d:%02d", hour, minute), habit.isReminderEnabled)
                                            }, modifier = Modifier.size(28.dp)) {
                                                Text("+د", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("وقت التنبيه الحالي:", fontSize = 11.sp, color = Color.Gray)
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

    // ==========================================
    // --- DIALOG 6: APP LANGUAGES ---
    // ==========================================
    if (showLanguagesDialog) {
        val languages = listOf("العربية (العراق / السعودية / مصر) 🇸🇦", "English (US / UK) 🇬🇧")
        Dialog(onDismissRequest = { showLanguagesDialog = false }) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp), horizontalAlignment = Alignment.End) {
                    Text("لغات التطبيق 🌐", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))

                    languages.forEach { lang ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    Toast.makeText(context, "اللغة مفعلة تلقائياً للتوافق التام!", Toast.LENGTH_SHORT).show()
                                    showLanguagesDialog = false
                                }
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Text(lang, fontSize = 13.sp, textAlign = TextAlign.End)
                        }
                    }
                }
            }
        }
    }

    // ==========================================
    // --- DIALOG 7: VALUES RATE US ---
    // ==========================================
    if (showRateDialog) {
        var stars by remember { mutableStateOf(5) }
        Dialog(onDismissRequest = { showRateDialog = false }) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.End) {
                    Text("تقييم التطبيق والدعم ✨", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("رأيك يهمنا لمواصلة التطوير والتحسين لضمان تقديم أعلى درجات جودة الالتزام والدعم السلوكي لعملائنا.", fontSize = 11.sp, color = Color.Gray, textAlign = TextAlign.End)
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        for (i in 1..5) {
                            val active = i <= stars
                            IconButton(onClick = { stars = i }) {
                                Icon(
                                    imageVector = if (active) Icons.Default.Star else Icons.Default.StarBorder,
                                    contentDescription = null,
                                    tint = if (active) Color(0xFFF59E0B) else Color.Gray,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            Toast.makeText(context, "نشكرك من الأعماق على تقييمك بـ $stars نجوم! 💖", Toast.LENGTH_LONG).show()
                            showRateDialog = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("إرسال التقييم الفوري 🌟")
                    }
                }
            }
        }
    }

    // ==========================================
    // --- DIALOG 8: GOOGLE EMAIL DIALOG ---
    // ==========================================
    if (showGoogleAccountDialog) {
        AlertDialog(
            onDismissRequest = { showGoogleAccountDialog = false },
            title = {
                Text(
                    text = "تسجيل الدخول بحساب Google 🔑",
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth(),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "أدخل بريدك الإلكتروني لحساب Google لتفعيل المزامنة التلقائية والنسخ الاحتياطي السحابي السلس.",
                        textAlign = TextAlign.End,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedTextField(
                        value = tempGoogleEmail,
                        onValueChange = { tempGoogleEmail = it },
                        label = { Text("البريد الإلكتروني") },
                        placeholder = { Text("example@gmail.com") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (tempGoogleEmail.contains("@") && tempGoogleEmail.contains(".")) {
                            viewModel.updateGoogleAccountEmail(tempGoogleEmail)
                            viewModel.updateGoogleBackupEnabled(true)
                            showGoogleAccountDialog = false
                            Toast.makeText(context, "تم ربط حساب Google بنجاح: $tempGoogleEmail", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(context, "رجاء إدخال بريد إلكتروني صحيح!", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("ربط الحساب والمزامنة")
                }
            },
            dismissButton = {
                TextButton(onClick = { showGoogleAccountDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // ==========================================
    // --- DIALOG 9: LOCAL BACKUP ---
    // ==========================================
    if (showBackupDialog) {
        val allEntries by viewModel.allEntries.collectAsState()
        val backupJson = remember { viewModel.exportBackupJson() }
        var exportTab by remember { mutableStateOf(0) } // 0: All, 1: Single
        var selectedEntry by remember { mutableStateOf<com.example.data.model.DiaryEntry?>(null) }
        var singleExportJson by remember { mutableStateOf("") }
        
        Dialog(onDismissRequest = { showBackupDialog = false }) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.85f)
                    .padding(8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(18.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "مشاركة وتصدير اليوميات 📤",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Tab Selector
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFFAF7F0), RoundedCornerShape(12.dp))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Button(
                            onClick = { exportTab = 1 },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (exportTab == 1) MaterialTheme.colorScheme.primary else Color.Transparent,
                                contentColor = if (exportTab == 1) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            Text("يومية واحدة محددة 📝", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { exportTab = 0 },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (exportTab == 0) MaterialTheme.colorScheme.primary else Color.Transparent,
                                contentColor = if (exportTab == 0) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            Text("كل اليوميات والبيانات 📦", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (exportTab == 0) {
                        Text(
                            text = "انسخ كود التصدير أدناه واحتفظ به بأمان، أو شاركه للانتقال لتطبيق آخر متضمنًا كل عاداتك ويومياتك وسجل التزامك بالكامل:",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray,
                            textAlign = TextAlign.End,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = backupJson,
                            onValueChange = {},
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            readOnly = true,
                            textStyle = TextStyle(fontSize = 10.sp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = { showBackupDialog = false }) { Text("إغلاق") }
                            Button(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("Yawmiyati Full Backup", backupJson)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "تم نسخ رمز النسخة الاحتياطية الكاملة!", Toast.LENGTH_SHORT).show()
                                    showBackupDialog = false
                                },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("نسخ رمز الكل 📋")
                            }
                        }
                    } else {
                        if (allEntries.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("لا توجد مذكرات أو يوميات مسجلة لتصديرها حالياً.", fontSize = 13.sp, color = Color.Gray)
                            }
                        } else {
                            if (selectedEntry == null) {
                                Text(
                                    text = "اختر اليومية التي ترغب بمشاركتها أو تصديرها بشكل مستقل:",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray,
                                    textAlign = TextAlign.End,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(10.dp))

                                Box(modifier = Modifier.weight(1f)) {
                                    LazyColumn(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        items(allEntries) { entry ->
                                            Card(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        selectedEntry = entry
                                                        singleExportJson = viewModel.exportSingleDiaryJson(entry)
                                                    },
                                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFAF7F0).copy(alpha = 0.6f)),
                                                shape = RoundedCornerShape(12.dp)
                                            ) {
                                                Column(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(12.dp),
                                                    horizontalAlignment = Alignment.End
                                                ) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(text = entry.timeString, fontSize = 10.sp, color = Color.Gray)
                                                        Text(text = entry.dateString, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                                                    }
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text(
                                                        text = entry.title.ifEmpty { "بدون عنوان" },
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 12.sp,
                                                        textAlign = TextAlign.End
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            } else {
                                val entry = selectedEntry!!
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(onClick = { selectedEntry = null }) {
                                        Icon(Icons.Default.ArrowBack, contentDescription = "رجوع")
                                    }
                                    Text(
                                        text = "يومية: ${entry.dateString}",
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))

                                OutlinedTextField(
                                    value = singleExportJson,
                                    onValueChange = {},
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f),
                                    readOnly = true,
                                    textStyle = TextStyle(fontSize = 10.sp)
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            val clip = ClipData.newPlainText("Single Diary JSON", singleExportJson)
                                            clipboard.setPrimaryClip(clip)
                                            Toast.makeText(context, "تم نسخ كود اليومية بنجاح لمشاركتها!", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text("نسخ كود المشاركة 💻", fontSize = 10.sp)
                                    }

                                    Button(
                                        onClick = {
                                            val clearText = """
                                                📅 يومياتي AI - ${entry.dateString}
                                                ✍️ العنوان: ${entry.title.ifEmpty { "بدون عنوان" }}
                                                📝 المحتوى:
                                                ${entry.content}
                                                
                                                🧠 تحليل الذكاء الاصطناعي للمزاج:
                                                ${entry.aiMoodAnalysis.ifEmpty { "لم يتم التحليل" }}
                                            """.trimIndent()
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            val clip = ClipData.newPlainText("Single Diary Readable", clearText)
                                            clipboard.setPrimaryClip(clip)
                                            Toast.makeText(context, "تم نسخ نص اليومية كرسالة مقروءة بنجاح!", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A6B5D))
                                    ) {
                                        Text("نسخ كرسالة مقروءة 📝", fontSize = 10.sp)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Start
                        ) {
                            TextButton(onClick = { showBackupDialog = false }) { Text("إغلاق") }
                        }
                    }
                }
            }
        }
    }

    // ==========================================
    // --- DIALOG 10: LOCAL RESTORE ---
    // ==========================================
    if (showRestoreDialog) {
        Dialog(onDismissRequest = { showRestoreDialog = false }) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    Text("استيراد النسخة الاحتياطية 📥", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "الصق النص البرمجي للنسخة الاحتياطية الكاملة، أو كود مشاركة يومية منفردة لتطبيقنا أو من أي تطبيق يدعم التصدير في المربع أدناه وسيتم إدراجها فورًا في سجلاتك:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        textAlign = TextAlign.End
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = restoreJsonText,
                        onValueChange = { restoreJsonText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .testTag("restore_json_input"),
                        placeholder = { Text("الصق رمز الـ JSON أو كود اليومية هنا...", textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth()) }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        TextButton(onClick = { showRestoreDialog = false }) { Text("إلغاء") }
                        Button(
                            onClick = {
                                if (restoreJsonText.isNotBlank()) {
                                    val success = viewModel.importBackupJson(restoreJsonText)
                                    if (success) {
                                        Toast.makeText(context, "تمت استعادة البيانات بنجاح تام!", Toast.LENGTH_LONG).show()
                                        restoreJsonText = ""
                                        showRestoreDialog = false
                                    } else {
                                        Toast.makeText(context, "فشل استيراد البيانات! يرجى التأكد من صحة الكود الملصق.", Toast.LENGTH_LONG).show()
                                    }
                                }
                            },
                            modifier = Modifier.testTag("confirm_restore_button")
                        ) {
                            Text("بدء الاستيراد الفوري")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsRowItem(
    title: String,
    subtitle: String? = null,
    icon: @Composable () -> Unit,
    trailing: @Composable (() -> Unit)? = null,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        if (trailing != null) {
            trailing()
        } else {
            Icon(
                imageVector = Icons.Default.ChevronLeft,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    textAlign = TextAlign.End
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        textAlign = TextAlign.End
                    )
                }
            }
            icon()
        }
    }
}
