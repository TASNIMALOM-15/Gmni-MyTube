package com.example.ui.screens.playlists

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PlaylistEntity
import com.example.data.model.VideoEntity
import com.example.ui.components.GlassCard
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.NeonRed

@Composable
fun PlaylistsScreen(
    playlists: List<PlaylistEntity>,
    videos: List<VideoEntity>,
    onCreatePlaylist: (String) -> Unit,
    onDeletePlaylist: (String) -> Unit,
    onPlayAll: (List<VideoEntity>) -> Unit,
    onDownloadAll: (List<VideoEntity>) -> Unit,
    onPlayVideo: (VideoEntity) -> Unit,
    onRemoveFromPlaylist: (videoId: String, playlistName: String) -> Unit
) {
    var selectedPlaylist by remember { mutableStateOf<PlaylistEntity?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }

    if (selectedPlaylist != null) {
        // Detailed Playlist View
        val playlist = selectedPlaylist!!
        val playlistVideos = when (playlist.name) {
            "All Videos" -> videos
            "Favorites" -> videos.filter { it.isFavorite }
            else -> videos.filter { it.playlistNames.split(",").map { p -> p.trim() }.contains(playlist.name) }
        }

        PlaylistDetailView(
            playlist = playlist,
            videos = playlistVideos,
            onBack = { selectedPlaylist = null },
            onPlayAll = { onPlayAll(playlistVideos) },
            onDownloadAll = { onDownloadAll(playlistVideos) },
            onPlayVideo = onPlayVideo,
            onRemoveVideo = { videoId -> onRemoveFromPlaylist(videoId, playlist.name) }
        )
    } else {
        // Playlists List
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .testTag("playlists_screen")
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "প্লেলিস্ট সমূহ",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Button(
                    onClick = { showCreateDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.testTag("create_playlist_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("নতুন প্লেলিস্ট", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Default system playlists: All Videos and Favorites
            val favoriteCount = videos.count { it.isFavorite }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                item {
                    // System Playlist 1: All Videos
                    PlaylistItemCard(
                        title = "All Videos",
                        subtitle = "${videos.size} টি ভিডিও",
                        icon = Icons.Default.VideoLibrary,
                        iconTint = CyanPrimary,
                        isSystem = true,
                        onClick = {
                            selectedPlaylist = PlaylistEntity(id = "sys_all", name = "All Videos", isSystem = true)
                        },
                        onDelete = {}
                    )
                }

                item {
                    // System Playlist 2: Favorites
                    PlaylistItemCard(
                        title = "Favorites",
                        subtitle = "$favoriteCount টি প্রিয় ভিডিও",
                        icon = Icons.Default.Favorite,
                        iconTint = NeonRed,
                        isSystem = true,
                        onClick = {
                            selectedPlaylist = PlaylistEntity(id = "sys_fav", name = "Favorites", isSystem = true)
                        },
                        onDelete = {}
                    )
                }

                items(playlists, key = { it.id }) { playlist ->
                    val count = videos.count {
                        it.playlistNames.split(",").map { p -> p.trim() }.contains(playlist.name)
                    }
                    PlaylistItemCard(
                        title = playlist.name,
                        subtitle = "$count টি ভিডিও",
                        icon = Icons.Default.Folder,
                        iconTint = ElectricBlue,
                        isSystem = playlist.isSystem,
                        onClick = { selectedPlaylist = playlist },
                        onDelete = { onDeletePlaylist(playlist.id) }
                    )
                }
            }
        }
    }

    if (showCreateDialog) {
        var newPlaylistName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("নতুন প্লেলিস্ট তৈরি করুন") },
            text = {
                OutlinedTextField(
                    value = newPlaylistName,
                    onValueChange = { newPlaylistName = it },
                    placeholder = { Text("প্লেলিস্টের নাম লিখুন...") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("new_playlist_name_input")
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPlaylistName.isNotBlank()) {
                            onCreatePlaylist(newPlaylistName.trim())
                            showCreateDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary)
                ) {
                    Text("তৈরি করুন", color = Color.Black)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("বাতিল")
                }
            }
        )
    }
}

@Composable
fun PlaylistItemCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    isSystem: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = iconTint.copy(alpha = 0.15f),
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (!isSystem) {
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun PlaylistDetailView(
    playlist: PlaylistEntity,
    videos: List<VideoEntity>,
    onBack: () -> Unit,
    onPlayAll: () -> Unit,
    onDownloadAll: () -> Unit,
    onPlayVideo: (VideoEntity) -> Unit,
    onRemoveVideo: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("playlist_detail_screen")
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Column {
                Text(
                    text = playlist.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${videos.size} টি ভিডিও",
                    style = MaterialTheme.typography.bodySmall,
                    color = CyanPrimary
                )
            }
        }

        // Action Buttons: Play All, Download All
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onPlayAll,
                enabled = videos.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Play All", color = Color.Black)
            }

            Button(
                onClick = onDownloadAll,
                enabled = videos.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Download, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Download All", color = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (videos.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "এই প্লেলিস্টে এখনো কোনো ভিডিও নেই।",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(videos, key = { it.id }) { video ->
                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPlayVideo(video) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlaylistPlay,
                                contentDescription = null,
                                tint = CyanPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = video.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = video.sourceDomain.ifBlank { "Direct Stream" },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            }
                            if (!playlist.isSystem) {
                                IconButton(onClick = { onRemoveVideo(video.id) }) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Remove",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
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
