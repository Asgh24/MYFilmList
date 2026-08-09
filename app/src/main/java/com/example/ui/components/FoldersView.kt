package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MediaItem
import com.example.data.model.MediaType

import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Circle

import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkCardSurface
import com.example.ui.theme.DarkOutline
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.PrimaryIndigo
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import androidx.compose.foundation.border

import com.example.ui.util.AppLanguage
import com.example.ui.util.UiStrings

@Composable
fun FoldersView(
    mediaItems: List<MediaItem>,
    uiLanguage: AppLanguage = AppLanguage.PERSIAN,
    isSelectionMode: Boolean = false,
    selectedFileIds: Set<String> = emptySet(),
    onToggleFileSelection: (String) -> Unit = {},
    onRescanFolder: () -> Unit,
    onSelectFolderClick: () -> Unit = {},
    onItemClick: (MediaItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = UiStrings(uiLanguage)
    val animeCount = mediaItems.count { it.parsedInfo.detectedType == MediaType.ANIME }
    val movieCount = mediaItems.count { it.parsedInfo.detectedType == MediaType.MOVIE }
    val seriesCount = mediaItems.count { it.parsedInfo.detectedType == MediaType.SERIES }

    val totalStorageBytes = mediaItems.sumOf { it.fileSize }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = strings.storageAndFolders,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = strings.scannedFilesCount(mediaItems.size),
                    fontSize = 11.sp,
                    color = TextMuted
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                androidx.compose.material3.OutlinedButton(
                    onClick = onSelectFolderClick,
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkOutline),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.height(34.dp).testTag("select_custom_folder_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = null,
                        tint = PrimaryIndigo,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = strings.selectFolder, color = TextPrimary, fontSize = 11.sp)
                }

                Button(
                    onClick = onRescanFolder,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                    shape = RoundedCornerShape(16.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(34.dp).testTag("rescan_folders_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = strings.scan,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = strings.rescan, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Storage Analytics Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkCardSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkOutline)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = strings.storageBreakdown,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = strings.totalIndexed(formatFileSize(totalStorageBytes)),
                    fontSize = 12.sp,
                    color = TextMuted
                )

                Spacer(modifier = Modifier.height(12.dp))

                LinearProgressIndicator(
                    progress = { 0.65f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = PrimaryIndigo,
                    trackColor = DarkSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    CategoryCountBadge(label = strings.anime, count = animeCount, icon = "📺", isFa = strings.isFa)
                    CategoryCountBadge(label = strings.movies, count = movieCount, icon = "🎬", isFa = strings.isFa)
                    CategoryCountBadge(label = strings.series, count = seriesCount, icon = "🌟", isFa = strings.isFa)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = strings.scannedFilesList,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(mediaItems, key = { it.id }) { item ->
                val isSelected = selectedFileIds.contains(item.id)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (isSelectionMode) {
                                onToggleFileSelection(item.id)
                            } else {
                                onItemClick(item)
                            }
                        },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) DarkSurfaceVariant else DarkCardSurface
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isSelected) PrimaryIndigo else DarkOutline
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isSelectionMode) {
                            Icon(
                                imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Outlined.Circle,
                                contentDescription = null,
                                tint = if (isSelected) PrimaryIndigo else TextMuted,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                        }

                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(PrimaryIndigo.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = when (item.parsedInfo.detectedType) {
                                    MediaType.ANIME -> Icons.Default.Tv
                                    MediaType.MOVIE -> Icons.Default.Movie
                                    MediaType.SERIES -> Icons.Default.PlayCircle
                                    else -> Icons.Default.Folder
                                },
                                contentDescription = null,
                                tint = PrimaryIndigo
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.parsedInfo.cleanTitle,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = item.fileName,
                                fontSize = 10.sp,
                                color = TextMuted,
                                maxLines = 1
                            )
                        }

                        Text(
                            text = formatFileSize(item.fileSize),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = PrimaryIndigo
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryCountBadge(label: String, count: Int, icon: String, isFa: Boolean = true) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text = icon, fontSize = 14.sp)
        Spacer(modifier = Modifier.width(4.dp))
        Column {
            Text(text = label, fontSize = 11.sp, color = TextMuted)
            Text(text = if (isFa) "$count عدد" else "$count items", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        }
    }
}

private fun formatFileSize(size: Long): String {
    val kb = size / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0
    return when {
        gb >= 1.0 -> "%.2f GB".format(gb)
        mb >= 1.0 -> "%.1f MB".format(mb)
        else -> "${kb.toInt()} KB"
    }
}
