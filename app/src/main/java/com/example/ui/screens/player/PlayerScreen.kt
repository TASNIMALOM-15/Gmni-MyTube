package com.example.ui.screens.player

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.provider.Settings
import android.view.ViewGroup
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.data.model.PlaylistEntity
import com.example.data.model.VideoEntity
import com.example.player.ExoPlayerHelper
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.NeonRed
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    currentVideo: VideoEntity,
    queue: List<VideoEntity>,
    playlists: List<PlaylistEntity>,
    defaultSpeed: Float = 1.0f,
    autoPlayNext: Boolean = true,
    onBack: () -> Unit,
    onVideoCompleted: (VideoEntity) -> Unit,
    onPlayNextVideo: (VideoEntity) -> Unit,
    onSavePosition: (videoId: String, positionMs: Long) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onAddToPlaylist: (videoId: String, playlistName: String) -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()

    var isPlaying by remember { mutableStateOf(true) }
    var currentPosition by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var showControls by remember { mutableStateOf(true) }
    var isFullscreen by remember { mutableStateOf(false) }
    var playbackSpeed by remember { mutableFloatStateOf(defaultSpeed) }
    var showSpeedDialog by remember { mutableStateOf(false) }
    var showPlaylistMenu by remember { mutableStateOf(false) }
    var isBuffering by remember { mutableStateOf(true) }
    var gestureFeedback by remember { mutableStateOf<String?>(null) }

    // Initialize ExoPlayer
    val exoPlayer = remember(context) {
        ExoPlayerHelper.createPlayer(context, defaultSpeed)
    }

    // Handle system back
    BackHandler {
        if (isFullscreen) {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            isFullscreen = false
        } else {
            onSavePosition(currentVideo.id, exoPlayer.currentPosition)
            onBack()
        }
    }

    // Load video
    LaunchedEffect(currentVideo.id) {
        val streamUrl = currentVideo.localFilePath ?: currentVideo.streamUrl
        val mediaItem = ExoPlayerHelper.buildMediaItem(streamUrl, currentVideo.title)
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()

        if (currentVideo.lastPositionMs > 1000) {
            exoPlayer.seekTo(currentVideo.lastPositionMs)
        }
        exoPlayer.playWhenReady = true
    }

    // Player event listener
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_BUFFERING -> isBuffering = true
                    Player.STATE_READY -> {
                        isBuffering = false
                        duration = exoPlayer.duration.coerceAtLeast(0L)
                    }
                    Player.STATE_ENDED -> {
                        isBuffering = false
                        onVideoCompleted(currentVideo)
                        if (autoPlayNext) {
                            val currentIndex = queue.indexOfFirst { it.id == currentVideo.id }
                            if (currentIndex != -1 && currentIndex + 1 < queue.size) {
                                onPlayNextVideo(queue[currentIndex + 1])
                            }
                        }
                    }
                    else -> isBuffering = false
                }
            }
        }
        exoPlayer.addListener(listener)

        onDispose {
            onSavePosition(currentVideo.id, exoPlayer.currentPosition)
            exoPlayer.removeListener(listener)
            exoPlayer.release()
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    // Periodic position updater
    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            currentPosition = exoPlayer.currentPosition.coerceAtLeast(0L)
            duration = exoPlayer.duration.coerceAtLeast(0L)
            delay(1000)
            onSavePosition(currentVideo.id, currentPosition)
        }
    }

    // Auto-hide controls timer
    LaunchedEffect(showControls, isPlaying) {
        if (showControls && isPlaying) {
            delay(4000)
            showControls = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("player_screen")
    ) {
        // Player View
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = {
                            showControls = !showControls
                        },
                        onDoubleTap = { offset ->
                            val screenWidth = size.width
                            if (offset.x < screenWidth / 2) {
                                // Double tap left: Rewind 10s
                                val newPos = (exoPlayer.currentPosition - 10000).coerceAtLeast(0)
                                exoPlayer.seekTo(newPos)
                                gestureFeedback = "-10s"
                            } else {
                                // Double tap right: Forward 10s
                                val newPos = (exoPlayer.currentPosition + 10000).coerceAtMost(exoPlayer.duration)
                                exoPlayer.seekTo(newPos)
                                gestureFeedback = "+10s"
                            }
                            scope.launch {
                                delay(1000)
                                gestureFeedback = null
                            }
                        }
                    )
                }
        )

        // Buffering Indicator
        if (isBuffering) {
            CircularProgressIndicator(
                modifier = Modifier
                    .size(56.dp)
                    .align(Alignment.Center),
                color = CyanPrimary,
                strokeWidth = 3.dp
            )
        }

        // Gesture feedback overlay
        if (gestureFeedback != null) {
            Surface(
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.7f),
                modifier = Modifier.align(Alignment.Center)
            ) {
                Text(
                    text = gestureFeedback!!,
                    color = CyanPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
                )
            }
        }

        // Animated Overlaid Controls
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Black.copy(alpha = 0.75f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.85f)
                            )
                        )
                    )
            ) {
                // Top Action Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 24.dp)
                        .align(Alignment.TopCenter),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            onSavePosition(currentVideo.id, exoPlayer.currentPosition)
                            onBack()
                        },
                        modifier = Modifier.testTag("player_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = currentVideo.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    // Favorite Button
                    IconButton(onClick = { onToggleFavorite(currentVideo.id) }) {
                        Icon(
                            imageVector = if (currentVideo.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (currentVideo.isFavorite) NeonRed else Color.White
                        )
                    }

                    // Add to Playlist Button
                    Box {
                        IconButton(onClick = { showPlaylistMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.PlaylistAdd,
                                contentDescription = "Add to playlist",
                                tint = Color.White
                            )
                        }
                        DropdownMenu(
                            expanded = showPlaylistMenu,
                            onDismissRequest = { showPlaylistMenu = false }
                        ) {
                            playlists.forEach { pl ->
                                DropdownMenuItem(
                                    text = { Text("+ ${pl.name}") },
                                    onClick = {
                                        onAddToPlaylist(currentVideo.id, pl.name)
                                        showPlaylistMenu = false
                                    }
                                )
                            }
                        }
                    }

                    // Speed Selector Button
                    Box {
                        IconButton(
                            onClick = { showSpeedDialog = true },
                            modifier = Modifier.testTag("player_speed_button")
                        ) {
                            Icon(Icons.Default.Speed, contentDescription = "Speed", tint = CyanPrimary)
                        }

                        DropdownMenu(
                            expanded = showSpeedDialog,
                            onDismissRequest = { showSpeedDialog = false }
                        ) {
                            val speeds = listOf(0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f)
                            speeds.forEach { sp ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            "${sp}x",
                                            color = if (playbackSpeed == sp) CyanPrimary else MaterialTheme.colorScheme.onSurface
                                        )
                                    },
                                    onClick = {
                                        playbackSpeed = sp
                                        exoPlayer.playbackParameters = PlaybackParameters(sp)
                                        showSpeedDialog = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Center Play/Pause & Skip Controls
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalArrangement = Arrangement.spacedBy(28.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Previous Video
                    val currentIndex = queue.indexOfFirst { it.id == currentVideo.id }
                    IconButton(
                        onClick = {
                            if (currentIndex > 0) {
                                onPlayNextVideo(queue[currentIndex - 1])
                            }
                        },
                        enabled = currentIndex > 0
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = "Previous",
                            tint = if (currentIndex > 0) Color.White else Color.Gray,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    // Replay 10s
                    IconButton(onClick = {
                        val newPos = (exoPlayer.currentPosition - 10000).coerceAtLeast(0)
                        exoPlayer.seekTo(newPos)
                    }) {
                        Icon(
                            imageVector = Icons.Default.Replay10,
                            contentDescription = "Rewind 10s",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    // Play / Pause FAB
                    Surface(
                        shape = CircleShape,
                        color = CyanPrimary,
                        modifier = Modifier.size(68.dp)
                    ) {
                        IconButton(
                            onClick = {
                                if (exoPlayer.isPlaying) {
                                    exoPlayer.pause()
                                } else {
                                    exoPlayer.play()
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = Color.Black,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                    }

                    // Forward 10s
                    IconButton(onClick = {
                        val newPos = (exoPlayer.currentPosition + 10000).coerceAtMost(exoPlayer.duration)
                        exoPlayer.seekTo(newPos)
                    }) {
                        Icon(
                            imageVector = Icons.Default.Forward10,
                            contentDescription = "Forward 10s",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    // Next Video
                    IconButton(
                        onClick = {
                            if (currentIndex != -1 && currentIndex + 1 < queue.size) {
                                onPlayNextVideo(queue[currentIndex + 1])
                            }
                        },
                        enabled = currentIndex != -1 && currentIndex + 1 < queue.size
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "Next",
                            tint = if (currentIndex != -1 && currentIndex + 1 < queue.size) Color.White else Color.Gray,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                // Bottom Timeline & Controls
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 20.dp)
                        .align(Alignment.BottomCenter)
                ) {
                    // Time text & Fullscreen
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${formatDuration(currentPosition)} / ${formatDuration(duration)}",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )

                        IconButton(
                            onClick = {
                                if (isFullscreen) {
                                    activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                                    isFullscreen = false
                                } else {
                                    activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                                    isFullscreen = true
                                }
                            },
                            modifier = Modifier.testTag("fullscreen_toggle_button")
                        ) {
                            Icon(
                                imageVector = if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                                contentDescription = "Fullscreen",
                                tint = CyanPrimary
                            )
                        }
                    }

                    // Seek Slider
                    Slider(
                        value = if (duration > 0) (currentPosition.toFloat() / duration.toFloat()).coerceIn(0f, 1f) else 0f,
                        onValueChange = { frac ->
                            val seekPos = (frac * duration).toLong()
                            currentPosition = seekPos
                            exoPlayer.seekTo(seekPos)
                        },
                        colors = SliderDefaults.colors(
                            thumbColor = CyanPrimary,
                            activeTrackColor = CyanPrimary,
                            inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

fun formatDuration(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val hours = minutes / 60
    return if (hours > 0) {
        String.format("%02d:%02d:%02d", hours, minutes % 60, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}
