package com.example.ui.screens.settings

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppSettings
import com.example.data.repository.StorageBreakdown
import com.example.data.repository.SyncResult
import com.example.ui.components.GlassCard
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.NeonRed
import com.example.ui.theme.StatusSuccess
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SettingsScreen(
    settings: AppSettings,
    storageInfo: StorageBreakdown?,
    onUpdateSpeed: (Float) -> Unit,
    onUpdateAutoPlay: (Boolean) -> Unit,
    onSetPin: (String) -> Unit,
    onDisablePin: () -> Unit,
    onUpdateTheme: (isDark: Boolean, isAmoled: Boolean) -> Unit,
    onUpdateLayout: (isGrid: Boolean) -> Unit,
    onUpdateDownloadLocation: (toAppPrivate: Boolean) -> Unit,
    onUpdateTelegramConfig: (token: String, channel: String, autoSync: Boolean) -> Unit,
    onClearCache: () -> Unit,
    onExportJson: suspend () -> String,
    onImportJson: suspend (String) -> Result<Pair<Int, Int>>,
    onTelegramBackup: suspend () -> SyncResult
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current

    var showPinDialog by remember { mutableStateOf(false) }
    var showTelegramDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var exportedJsonText by remember { mutableStateOf("") }
    var isSyncingTelegram by remember { mutableStateOf(false) }
    var telegramSyncMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("settings_screen")
    ) {
        Text(
            text = "সেটিংস ও কনফিগারেশন",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(16.dp)
        )

        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // Playback Section
            item {
                SettingsSectionTitle("প্লেব্যাক সেটিংস", Icons.Default.PlayCircle)
                Spacer(modifier = Modifier.height(8.dp))
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        // Default Speed
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "ডিফল্ট স্পিড (Playback Speed)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "বর্তমানে: ${settings.defaultPlaybackSpeed}x",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = CyanPrimary
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                listOf(1.0f, 1.25f, 1.5f, 2.0f).forEach { sp ->
                                    val isSelected = settings.defaultPlaybackSpeed == sp
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSelected) CyanPrimary else MaterialTheme.colorScheme.surfaceVariant,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable { onUpdateSpeed(sp) }
                                    ) {
                                        Text(
                                            text = "${sp}x",
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                        // Auto-play Next
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Auto-play Next Video",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "ভিডিও শেষ হলে নিজে থেকেই পরবর্তী ভিডিও চলবে",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = settings.autoPlayNext,
                                onCheckedChange = onUpdateAutoPlay,
                                colors = SwitchDefaults.colors(checkedThumbColor = CyanPrimary)
                            )
                        }
                    }
                }
            }

            // Security & App Lock Section
            item {
                SettingsSectionTitle("নিরাপত্তা ও অ্যাপ লক", Icons.Default.Security)
                Spacer(modifier = Modifier.height(8.dp))
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "৪-ডিজিট PIN লক",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = if (settings.isAppLockEnabled) "অ্যাপ লক সক্রিয় রয়েছে (PIN Protected)" else "অ্যাপ খোলার সময় পিন সুরক্ষার জন্য সক্রিয় করুন",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (settings.isAppLockEnabled) StatusSuccess else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Switch(
                            checked = settings.isAppLockEnabled,
                            onCheckedChange = { enable ->
                                if (enable) {
                                    showPinDialog = true
                                } else {
                                    onDisablePin()
                                }
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = CyanPrimary)
                        )
                    }
                }
            }

            // Storage & Download Location
            item {
                SettingsSectionTitle("স্টোরেজ ও ডাউনলোড পাথ", Icons.Default.Storage)
                Spacer(modifier = Modifier.height(8.dp))
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "গোপন ডাউনলোড ফোল্ডার (Android/data)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = if (settings.downloadToAppPrivate)
                                        "অন থাকলে ভিডিওগুলো সরাসরি ফোনের গ্যালারি বা সাধারণ ফাইল ম্যানেজারে শো করবে না (App-Private Storage)"
                                    else "সাধারণ স্টোরেজে ডাউনলোড হবে",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = settings.downloadToAppPrivate,
                                onCheckedChange = onUpdateDownloadLocation,
                                colors = SwitchDefaults.colors(checkedThumbColor = CyanPrimary)
                            )
                        }

                        if (storageInfo != null) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("ক্যাশ মেমোরি:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(storageInfo.formatBytes(storageInfo.cacheSizeBytes), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = CyanPrimary)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("ডাউনলোড সাইজ:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(storageInfo.formatBytes(storageInfo.downloadsSizeBytes), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = StatusSuccess)
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = {
                                    onClearCache()
                                    Toast.makeText(context, "ক্যাশ মেমোরি পরিষ্কার করা হয়েছে!", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.CleaningServices, contentDescription = null, tint = CyanPrimary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Clear Cache (ক্যাশ মুছুন)", color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // Telegram Cloud Sync & Backup Section
            item {
                SettingsSectionTitle("টেলিগ্রাম ক্লাউড ব্যাকআপ", Icons.Default.CloudSync)
                Spacer(modifier = Modifier.height(8.dp))
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "আপনার ব্যক্তিগত Telegram Channel-কে ফ্রি ক্লাউড ব্যাকআপ হিসেবে ব্যবহার করুন। ভিডিও লিংক, প্লেলিস্ট এবং সেটিংস ব্যাকআপ রাখুন।",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        if (settings.lastSyncTimestamp > 0) {
                            val dateStr = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(settings.lastSyncTimestamp))
                            Text(
                                text = "সর্বশেষ সিঙ্ক: $dateStr",
                                fontSize = 11.sp,
                                color = StatusSuccess
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { showTelegramDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("কনফিগার", color = Color.White, fontSize = 12.sp)
                            }

                            Button(
                                onClick = {
                                    scope.launch {
                                        isSyncingTelegram = true
                                        val result = onTelegramBackup()
                                        isSyncingTelegram = false
                                        when (result) {
                                            is SyncResult.Success -> {
                                                Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
                                            }
                                            is SyncResult.Error -> {
                                                Toast.makeText(context, result.error, Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    }
                                },
                                enabled = settings.telegramBotToken.isNotBlank() && settings.telegramChannelId.isNotBlank() && !isSyncingTelegram,
                                colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary),
                                modifier = Modifier.weight(1f)
                            ) {
                                if (isSyncingTelegram) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.Black)
                                } else {
                                    Text("এখনই ব্যাকআপ", color = Color.Black, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }

            // JSON Export / Import Section
            item {
                SettingsSectionTitle("ডাটা এক্সপোর্ট ও ইমপোর্ট", Icons.Default.Folder)
                Spacer(modifier = Modifier.height(8.dp))
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                scope.launch {
                                    exportedJsonText = onExportJson()
                                    showExportDialog = true
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.CloudUpload, contentDescription = null, tint = CyanPrimary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Export JSON", color = MaterialTheme.colorScheme.onSurface, fontSize = 11.sp)
                        }

                        Button(
                            onClick = { showImportDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.CloudDownload, contentDescription = null, tint = StatusSuccess, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Import JSON", color = MaterialTheme.colorScheme.onSurface, fontSize = 11.sp)
                        }
                    }
                }
            }

            // About & 32-bit Compatibility
            item {
                SettingsSectionTitle("সম্পর্কে ও ডিভাইস তথ্য", Icons.Default.Info)
                Spacer(modifier = Modifier.height(8.dp))
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "MyTube Video Streaming & Downloader",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = CyanPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Version: 1.0 (Build for Infinix Smart 9)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Target Architecture: armeabi-v7a (32-bit ARM) & arm64-v8a",
                            style = MaterialTheme.typography.bodySmall,
                            color = StatusSuccess
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Engine: AndroidX Media3 (ExoPlayer), Room Local SQLite, Jetpack Compose",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Set PIN Dialog
    if (showPinDialog) {
        var pinInput by remember { mutableStateOf("") }
        var confirmPinInput by remember { mutableStateOf("") }
        var pinError by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showPinDialog = false },
            title = { Text("নতুন ৪-সংখ্যার PIN সেট করুন") },
            text = {
                Column {
                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = { if (it.length <= 4 && it.all { char -> char.isDigit() }) pinInput = it },
                        label = { Text("৪-ডিজিট PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = confirmPinInput,
                        onValueChange = { if (it.length <= 4 && it.all { char -> char.isDigit() }) confirmPinInput = it },
                        label = { Text("পিন নিশ্চিত করুন") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (pinError != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(pinError!!, color = NeonRed, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (pinInput.length != 4) {
                            pinError = "PIN ঠিক ৪ সংখ্যার হতে হবে।"
                        } else if (pinInput != confirmPinInput) {
                            pinError = "উভয় PIN মিলছে না!"
                        } else {
                            onSetPin(pinInput)
                            showPinDialog = false
                            Toast.makeText(context, "PIN লক সফলভাবে চালু হয়েছে!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary)
                ) {
                    Text("সংরক্ষণ", color = Color.Black)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPinDialog = false }) {
                    Text("বাতিল")
                }
            }
        )
    }

    // Telegram Configuration Dialog
    if (showTelegramDialog) {
        var botToken by remember { mutableStateOf(settings.telegramBotToken) }
        var channelId by remember { mutableStateOf(settings.telegramChannelId) }
        var autoSync by remember { mutableStateOf(settings.autoSyncOnOpen) }

        AlertDialog(
            onDismissRequest = { showTelegramDialog = false },
            title = { Text("টেলিগ্রাম ব্যাকআপ কনফিগারেশন") },
            text = {
                Column {
                    Text(
                        text = "1. @BotFather থেকে একটি নতুন বট তৈরি করে Bot Token নিন।\n2. আপনার Telegram Channel-এ বটটিকে Administrator হিসেবে যুক্ত করুন।\n3. Channel ID দিন (যেমন: -1001234567890)।",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = botToken,
                        onValueChange = { botToken = it },
                        label = { Text("Bot Token") },
                        placeholder = { Text("123456:ABC-DEF1234ghIkl-zyx57W2v1u123ew11") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = channelId,
                        onValueChange = { channelId = it },
                        label = { Text("Channel ID / @channel_username") },
                        placeholder = { Text("-1001234567890 বা @mychannel") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(checked = autoSync, onCheckedChange = { autoSync = it })
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("অ্যাপ ওপেন করার সময় Auto Sync করুন", fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateTelegramConfig(botToken.trim(), channelId.trim(), autoSync)
                        showTelegramDialog = false
                        Toast.makeText(context, "Telegram কনফিগারেশন সংরক্ষিত হয়েছে!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary)
                ) {
                    Text("সংরক্ষণ", color = Color.Black)
                }
            },
            dismissButton = {
                TextButton(onClick = { showTelegramDialog = false }) {
                    Text("বাতিল")
                }
            }
        )
    }

    // JSON Export Dialog
    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text("JSON ব্যাকআপ ডেটা") },
            text = {
                Column {
                    Text("নিচের JSON কোডটি কপি করে সেভ করে রাখুন:", fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = exportedJsonText,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(exportedJsonText))
                        Toast.makeText(context, "JSON ক্লিপবোর্ডে কপি হয়েছে!", Toast.LENGTH_SHORT).show()
                        showExportDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary)
                ) {
                    Text("কপি করুন", color = Color.Black)
                }
            },
            dismissButton = {
                TextButton(onClick = { showExportDialog = false }) {
                    Text("বন্ধ করুন")
                }
            }
        )
    }

    // JSON Import Dialog
    if (showImportDialog) {
        var importJsonInput by remember { mutableStateOf("") }
        var importError by remember { mutableStateOf<String?>(null) }
        var isImporting by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("JSON ব্যাকআপ ইমপোর্ট করুন") },
            text = {
                Column {
                    Text("পূর্বে সংরক্ষিত JSON ডেটা এখানে পেস্ট করুন:", fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = importJsonInput,
                        onValueChange = { importJsonInput = it },
                        placeholder = { Text("{ \"videos\": [...], ... }") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                    )
                    if (importError != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(importError!!, color = NeonRed, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (importJsonInput.isBlank()) {
                            importError = "JSON ডেটা প্রদান করুন"
                        } else {
                            scope.launch {
                                isImporting = true
                                val result = onImportJson(importJsonInput.trim())
                                isImporting = false
                                result.fold(
                                    onSuccess = { (vCount, pCount) ->
                                        Toast.makeText(
                                            context,
                                            "সফলভাবে $vCount টি ভিডিও এবং $pCount টি প্লেলিস্ট ইমপোর্ট হয়েছে!",
                                            Toast.LENGTH_LONG
                                        ).show()
                                        showImportDialog = false
                                    },
                                    onFailure = { err ->
                                        importError = "ইমপোর্ট ব্যর্থ: ${err.localizedMessage}"
                                    }
                                )
                            }
                        }
                    },
                    enabled = !isImporting,
                    colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary)
                ) {
                    Text("ইমপোর্ট", color = Color.Black)
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) {
                    Text("বাতিল")
                }
            }
        )
    }
}

@Composable
fun SettingsSectionTitle(title: String, icon: ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = CyanPrimary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}
