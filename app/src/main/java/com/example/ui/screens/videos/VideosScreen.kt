package com.example.ui.screens.videos

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.PlaylistEntity
import com.example.data.model.VideoEntity
import com.example.ui.components.GlassCard
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.NeonRed
import com.example.ui.theme.StatusSuccess

enum class VideoFilter {
    ALL, FAVORITES, DOWNLOADED
}

@Composable
fun VideosScreen(
    videos: List<VideoEntity>,
    playlists: List<PlaylistEntity>,
    isGridLayout: Boolean,
    onToggleLayout: (Boolean) -> Unit,
    onPlayVideo: (VideoEntity) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onDownloadVideo: (VideoEntity) -> Unit,
    onAddToPlaylist: (videoId: String, playlistName: String) -> Unit,
    onDeleteVideo: (VideoEntity) -> Unit,
    onOpenAddDialog: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var currentFilter by remember { mutableStateOf(VideoFilter.ALL) }

    val filteredVideos = videos.filter { video ->
        val matchesSearch = searchQuery.isBlank() ||
                video.title.contains(searchQuery, ignoreCase = true) ||
                video.sourceDomain.contains(searchQuery, ignoreCase = true)

        val matchesFilter = when (currentFilter) {
            VideoFilter.ALL -> true
            VideoFilter.FAVORITES -> video.isFavorite
            VideoFilter.DOWNLOADED -> video.isDownloaded
        }

        matchesSearch && matchesFilter
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("videos_screen")
    ) {
        // Top Bar: Search & View Toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("ভিডিও খুঁজুন...", fontSize = 14.sp) },
                singleLine = true,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = CyanPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .testTag("search_video_input"),
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CyanPrimary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                )
            )

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = { onToggleLayout(!isGridLayout) },
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = CircleShape
                    )
                    .testTag("toggle_layout_button")
            ) {
                Icon(
                    imageVector = if (isGridLayout) Icons.Default.ViewList else Icons.Default.GridView,
                    contentDescription = "Toggle Layout",
                    tint = CyanPrimary
                )
            }
        }

        // Filter Chips Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = currentFilter == VideoFilter.ALL,
                onClick = { currentFilter = VideoFilter.ALL },
                label = { Text("সব ভিডিও (${videos.size})", fontSize = 12.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = CyanPrimary.copy(alpha = 0.2f),
                    selectedLabelColor = CyanPrimary
                )
            )
            FilterChip(
                selected = currentFilter == VideoFilter.FAVORITES,
                onClick = { currentFilter = VideoFilter.FAVORITES },
                label = { Text("প্রিয় (${videos.count { it.isFavorite }})", fontSize = 12.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = NeonRed.copy(alpha = 0.2f),
                    selectedLabelColor = NeonRed
                )
            )
            FilterChip(
                selected = currentFilter == VideoFilter.DOWNLOADED,
                onClick = { currentFilter = VideoFilter.DOWNLOADED },
                label = { Text("ডাউনলোড (${videos.count { it.isDownloaded }})", fontSize = 12.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = StatusSuccess.copy(alpha = 0.2f),
                    selectedLabelColor = StatusSuccess
                )
            )
        }

        if (filteredVideos.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.VideoLibrary,
                        contentDescription = null,
                        tint = CyanPrimary.copy(alpha = 0.6f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (searchQuery.isNotEmpty()) "কোনো ভিডিও খুঁজে পাওয়া যায়নি" else "কোনো ভিডিও এখনো সংরক্ষিত নেই",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "উপরে বা নিচের '+' বাটনে চাপ দিয়ে যেকোনো সরাসরি ভিডিও লিংক যোগ করুন।",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            if (isGridLayout) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredVideos, key = { it.id }) { video ->
                        VideoGridCard(
                            video = video,
                            playlists = playlists,
                            onPlay = { onPlayVideo(video) },
                            onToggleFav = { onToggleFavorite(video.id) },
                            onDownload = { onDownloadVideo(video) },
                            onAddToPlaylist = { pName -> onAddToPlaylist(video.id, pName) },
                            onDelete = { onDeleteVideo(video) }
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredVideos, key = { it.id }) { video ->
                        VideoListCard(
                            video = video,
                            playlists = playlists,
                            onPlay = { onPlayVideo(video) },
                            onToggleFav = { onToggleFavorite(video.id) },
                            onDownload = { onDownloadVideo(video) },
                            onAddToPlaylist = { pName -> onAddToPlaylist(video.id, pName) },
                            onDelete = { onDeleteVideo(video) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun VideoGridCard(
    video: VideoEntity,
    playlists: List<PlaylistEntity>,
    onPlay: () -> Unit,
    onToggleFav: () -> Unit,
    onDownload: () -> Unit,
    onAddToPlaylist: (String) -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    var showPlaylistSubmenu by remember { mutableStateOf(false) }

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onPlay() }
            .testTag("video_card_${video.id}")
    ) {
        Column {
            // Thumbnail with Play button overlay & badges
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                if (video.thumbnailUrl.isNotBlank()) {
                    AsyncImage(
                        model = video.thumbnailUrl,
                        contentDescription = video.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    listOf(ElectricBlue.copy(alpha = 0.5f), CyanPrimary.copy(alpha = 0.2f))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = CyanPrimary,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                if (video.isDownloaded) {
                    Surface(
                        color = StatusSuccess,
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier
                            .padding(6.dp)
                            .align(Alignment.TopStart)
                    ) {
                        Text(
                            text = "OFFLINE",
                            color = Color.Black,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }

                if (video.lastPositionMs > 0) {
                    LinearProgressIndicator(
                        progress = { 0.5f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .align(Alignment.BottomCenter),
                        color = CyanPrimary,
                        trackColor = Color.Black.copy(alpha = 0.5f)
                    )
                }
            }

            // Info row
            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = video.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = video.sourceDomain.ifBlank { "Direct Stream" },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        maxLines = 1
                    )

                    Box {
                        IconButton(
                            onClick = { showMenu = true },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Options",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        VideoActionDropdown(
                            expanded = showMenu,
                            onDismiss = { showMenu = false },
                            isFavorite = video.isFavorite,
                            isDownloaded = video.isDownloaded,
                            playlists = playlists,
                            onToggleFav = onToggleFav,
                            onDownload = onDownload,
                            onAddToPlaylist = onAddToPlaylist,
                            onDelete = onDelete
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun VideoListCard(
    video: VideoEntity,
    playlists: List<PlaylistEntity>,
    onPlay: () -> Unit,
    onToggleFav: () -> Unit,
    onDownload: () -> Unit,
    onAddToPlaylist: (String) -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onPlay() }
            .testTag("video_card_${video.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail
            Box(
                modifier = Modifier
                    .width(120.dp)
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                if (video.thumbnailUrl.isNotBlank()) {
                    AsyncImage(
                        model = video.thumbnailUrl,
                        contentDescription = video.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    listOf(ElectricBlue.copy(alpha = 0.5f), CyanPrimary.copy(alpha = 0.2f))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = CyanPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                if (video.isDownloaded) {
                    Surface(
                        color = StatusSuccess,
                        shape = RoundedCornerShape(3.dp),
                        modifier = Modifier
                            .padding(4.dp)
                            .align(Alignment.TopStart)
                    ) {
                        Text(
                            text = "OFFLINE",
                            color = Color.Black,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = video.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = video.sourceDomain.ifBlank { "Direct Stream" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
            }

            IconButton(onClick = onToggleFav) {
                Icon(
                    imageVector = if (video.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Favorite",
                    tint = if (video.isFavorite) NeonRed else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }

            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Menu",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }

                VideoActionDropdown(
                    expanded = showMenu,
                    onDismiss = { showMenu = false },
                    isFavorite = video.isFavorite,
                    isDownloaded = video.isDownloaded,
                    playlists = playlists,
                    onToggleFav = onToggleFav,
                    onDownload = onDownload,
                    onAddToPlaylist = onAddToPlaylist,
                    onDelete = onDelete
                )
            }
        }
    }
}

@Composable
fun VideoActionDropdown(
    expanded: Boolean,
    onDismiss: () -> Unit,
    isFavorite: Boolean,
    isDownloaded: Boolean,
    playlists: List<PlaylistEntity>,
    onToggleFav: () -> Unit,
    onDownload: () -> Unit,
    onAddToPlaylist: (String) -> Unit,
    onDelete: () -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss
    ) {
        DropdownMenuItem(
            text = { Text(if (isFavorite) "প্রিয় তালিকা থেকে মুছুন" else "প্রিয়তে যুক্ত করুন") },
            leadingIcon = {
                Icon(
                    imageVector = if (isFavorite) Icons.Default.FavoriteBorder else Icons.Default.Favorite,
                    contentDescription = null,
                    tint = NeonRed
                )
            },
            onClick = {
                onToggleFav()
                onDismiss()
            }
        )

        if (!isDownloaded) {
            DropdownMenuItem(
                text = { Text("ডাউনলোড করুন") },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Download, contentDescription = null, tint = CyanPrimary)
                },
                onClick = {
                    onDownload()
                    onDismiss()
                }
            )
        }

        if (playlists.isNotEmpty()) {
            playlists.forEach { playlist ->
                DropdownMenuItem(
                    text = { Text("+ ${playlist.name} প্লেলিস্ট") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.PlaylistAdd, contentDescription = null)
                    },
                    onClick = {
                        onAddToPlaylist(playlist.name)
                        onDismiss()
                    }
                )
            }
        }

        DropdownMenuItem(
            text = { Text("মুছে ফেলুন", color = NeonRed) },
            onClick = {
                onDelete()
                onDismiss()
            }
        )
    }
}
