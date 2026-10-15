package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.VideoEntity
import com.example.ui.MainViewModel
import com.example.ui.components.AddVideoDialog
import com.example.ui.components.MyTubeBottomNavBar
import com.example.ui.components.NavigationTab
import com.example.ui.components.PinLockScreen
import com.example.ui.screens.downloads.DownloadsScreen
import com.example.ui.screens.player.PlayerScreen
import com.example.ui.screens.playlists.PlaylistsScreen
import com.example.ui.screens.settings.SettingsScreen
import com.example.ui.screens.videos.VideosScreen
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val viewModel: MainViewModel = viewModel()
            val settings by viewModel.settings.collectAsStateWithLifecycle()

            MyApplicationTheme(
                darkTheme = settings.isDarkTheme
            ) {
                // Request Notification Permission on Android 13+
                val context = LocalContext.current
                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { /* Handle result */ }

                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        if (ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.POST_NOTIFICATIONS
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                }

                val isUnlocked by viewModel.isUnlocked.collectAsStateWithLifecycle()

                if (settings.isAppLockEnabled && !isUnlocked && settings.pinHash.isNotBlank()) {
                    PinLockScreen(
                        correctPinHash = settings.pinHash,
                        onUnlocked = { viewModel.unlockApp() }
                    )
                } else {
                    MyTubeMainScreen(viewModel = viewModel)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyTubeMainScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    var currentTab by remember { mutableStateOf(NavigationTab.VIDEOS) }
    var showAddDialog by remember { mutableStateOf(false) }

    val videos by viewModel.videos.collectAsStateWithLifecycle()
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val downloads by viewModel.downloads.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val storageInfo by viewModel.storageInfo.collectAsStateWithLifecycle()
    val currentPlayingVideo by viewModel.currentPlayingVideo.collectAsStateWithLifecycle()
    val currentQueue by viewModel.currentQueue.collectAsStateWithLifecycle()

    // Fullscreen Video Player Overlay
    if (currentPlayingVideo != null) {
        PlayerScreen(
            currentVideo = currentPlayingVideo!!,
            queue = currentQueue,
            playlists = playlists,
            defaultSpeed = settings.defaultPlaybackSpeed,
            autoPlayNext = settings.autoPlayNext,
            onBack = { viewModel.stopPlayer() },
            onVideoCompleted = { /* auto-play logic */ },
            onPlayNextVideo = { next -> viewModel.playVideo(next, currentQueue) },
            onSavePosition = { id, pos -> viewModel.updatePlaybackPosition(id, pos) },
            onToggleFavorite = { id -> viewModel.toggleFavorite(id) },
            onAddToPlaylist = { id, name -> viewModel.addToPlaylist(id, name) }
        )
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                painter = painterResource(id = R.drawable.mytube_logo),
                                contentDescription = "Logo",
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "MyTube",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 20.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    },
                    actions = {
                        Button(
                            onClick = { showAddDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary),
                            shape = CircleShape,
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .testTag("top_add_video_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add Video",
                                tint = Color.Black,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Add Link",
                                color = Color.Black,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier.testTag("app_top_bar")
                )
            },
            bottomBar = {
                MyTubeBottomNavBar(
                    currentTab = currentTab,
                    onTabSelected = { currentTab = it }
                )
            },
            modifier = Modifier.fillMaxSize()
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                when (currentTab) {
                    NavigationTab.VIDEOS -> {
                        VideosScreen(
                            videos = videos,
                            playlists = playlists,
                            isGridLayout = settings.isGridLayout,
                            onToggleLayout = { viewModel.updateLayout(it) },
                            onPlayVideo = { video -> viewModel.playVideo(video, videos) },
                            onToggleFavorite = { id -> viewModel.toggleFavorite(id) },
                            onDownloadVideo = { video ->
                                viewModel.startDownload(video)
                                Toast.makeText(context, "ডাউনলোড শুরু হয়েছে! Downloads ট্যাবে দেখুন।", Toast.LENGTH_SHORT).show()
                            },
                            onAddToPlaylist = { id, name ->
                                viewModel.addToPlaylist(id, name)
                                Toast.makeText(context, "$name প্লেলিস্টে যুক্ত হয়েছে!", Toast.LENGTH_SHORT).show()
                            },
                            onDeleteVideo = { video -> viewModel.deleteVideo(video) },
                            onOpenAddDialog = { showAddDialog = true }
                        )
                    }

                    NavigationTab.PLAYLISTS -> {
                        PlaylistsScreen(
                            playlists = playlists,
                            videos = videos,
                            onCreatePlaylist = { viewModel.createPlaylist(it) },
                            onDeletePlaylist = { viewModel.deletePlaylist(it) },
                            onPlayAll = { pVideos ->
                                if (pVideos.isNotEmpty()) {
                                    viewModel.playVideo(pVideos.first(), pVideos)
                                }
                            },
                            onDownloadAll = { pVideos ->
                                pVideos.forEach { v -> viewModel.startDownload(v) }
                                Toast.makeText(context, "${pVideos.size} টি ভিডিওর ডাউনলোড কিউতে যুক্ত হয়েছে!", Toast.LENGTH_SHORT).show()
                            },
                            onPlayVideo = { video -> viewModel.playVideo(video, videos) },
                            onRemoveFromPlaylist = { id, name -> viewModel.removeFromPlaylist(id, name) }
                        )
                    }

                    NavigationTab.DOWNLOADS -> {
                        DownloadsScreen(
                            downloads = downloads,
                            onPlayLocalFile = { path, title ->
                                val offlineVideo = VideoEntity(
                                    id = "local_${System.currentTimeMillis()}",
                                    title = title,
                                    originalUrl = path,
                                    streamUrl = path,
                                    thumbnailUrl = "",
                                    localFilePath = path,
                                    isDownloaded = true
                                )
                                viewModel.playVideo(offlineVideo, listOf(offlineVideo))
                            },
                            onPauseDownload = { id -> viewModel.pauseDownload(id) },
                            onResumeDownload = { id -> viewModel.resumeDownload(id) },
                            onCancelDownload = { id -> viewModel.cancelDownload(id) },
                            onDeleteDownload = { id -> viewModel.deleteDownload(id) }
                        )
                    }

                    NavigationTab.SETTINGS -> {
                        SettingsScreen(
                            settings = settings,
                            storageInfo = storageInfo,
                            onUpdateSpeed = { viewModel.updateSpeed(it) },
                            onUpdateAutoPlay = { viewModel.updateAutoPlay(it) },
                            onSetPin = { viewModel.setPin(it) },
                            onDisablePin = { viewModel.disablePin() },
                            onUpdateTheme = { dark, amoled -> viewModel.updateTheme(dark, amoled) },
                            onUpdateLayout = { viewModel.updateLayout(it) },
                            onUpdateDownloadLocation = { viewModel.updateDownloadLocation(it) },
                            onUpdateTelegramConfig = { token, channel, autoSync ->
                                viewModel.updateTelegramConfig(token, channel, autoSync)
                            },
                            onClearCache = { viewModel.clearCache() },
                            onExportJson = { viewModel.exportJson() },
                            onImportJson = { viewModel.importJson(it) },
                            onTelegramBackup = { viewModel.backupToTelegram() }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddVideoDialog(
            playlists = playlists,
            onDismiss = { showAddDialog = false },
            onAddVideo = { url, title, playlist, andPlay ->
                viewModel.addVideo(url, title, playlist, andPlay) { video ->
                    showAddDialog = false
                    if (video != null) {
                        Toast.makeText(context, "ভিডিও যুক্ত হয়েছে: ${video.title}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }
}
