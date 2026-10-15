package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PlaylistEntity
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.ElectricBlue

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddVideoDialog(
    playlists: List<PlaylistEntity>,
    onDismiss: () -> Unit,
    onAddVideo: (url: String, title: String?, playlistName: String?, andPlay: Boolean) -> Unit
) {
    var urlText by remember { mutableStateOf("") }
    var titleText by remember { mutableStateOf("") }
    var selectedPlaylist by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    val clipboardManager = LocalClipboardManager.current

    // Sample video links for convenient testing
    val samples = listOf(
        "Big Buck Bunny (MP4)" to "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
        "Elephants Dream (MP4)" to "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4",
        "Tears of Steel (HLS)" to "https://demo.unified-streaming.com/k8s/features/stable/video/tears-of-steel/tears-of-steel.ism/.m3u8"
    )

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        modifier = Modifier.testTag("add_video_dialog"),
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Link,
                    contentDescription = null,
                    tint = CyanPrimary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "নতুন ভিডিও যুক্ত করুন",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "ভিডিওর Direct URL, HLS (m3u8), TeraBox লিংক পেস্ট করুন:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = urlText,
                    onValueChange = { urlText = it },
                    placeholder = { Text("https://example.com/video.mp4") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("video_url_input"),
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                clipboardManager.getText()?.text?.let { clipText ->
                                    urlText = clipText.trim()
                                }
                            },
                            modifier = Modifier.testTag("paste_url_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentPaste,
                                contentDescription = "Paste",
                                tint = CyanPrimary
                            )
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyanPrimary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = titleText,
                    onValueChange = { titleText = it },
                    label = { Text("ভিডিওর নাম (ঐচ্ছিক)") },
                    placeholder = { Text("স্বয়ংক্রিয়ভাবে বের করা হবে") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("video_title_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyanPrimary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )

                if (playlists.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "প্লেলিস্টে যুক্ত করুন (ঐচ্ছিক):",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        playlists.forEach { playlist ->
                            val isSelected = selectedPlaylist == playlist.name
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    selectedPlaylist = if (isSelected) null else playlist.name
                                },
                                label = { Text(playlist.name, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CyanPrimary.copy(alpha = 0.2f),
                                    selectedLabelColor = CyanPrimary
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "দ্রুত টেস্ট করতে স্যাম্পল ভিডিও:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                samples.forEach { (name, link) ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                urlText = link
                                titleText = name
                            },
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Text(
                            text = "▶ $name",
                            style = MaterialTheme.typography.bodySmall,
                            color = CyanPrimary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Row {
                Button(
                    onClick = {
                        if (urlText.isNotBlank()) {
                            isLoading = true
                            onAddVideo(
                                urlText.trim(),
                                titleText.takeIf { it.isNotBlank() },
                                selectedPlaylist,
                                true // Play immediately
                            )
                        }
                    },
                    enabled = urlText.isNotBlank() && !isLoading,
                    colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary),
                    modifier = Modifier.testTag("done_and_play_button")
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color.Black,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Save & Play", color = Color.Black)
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        if (urlText.isNotBlank()) {
                            isLoading = true
                            onAddVideo(
                                urlText.trim(),
                                titleText.takeIf { it.isNotBlank() },
                                selectedPlaylist,
                                false
                            )
                        }
                    },
                    enabled = urlText.isNotBlank() && !isLoading,
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                    modifier = Modifier.testTag("save_video_button")
                ) {
                    Text("সংরক্ষণ", color = Color.White)
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isLoading,
                modifier = Modifier.testTag("cancel_add_video_button")
            ) {
                Text("বাতিল", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    )
}
