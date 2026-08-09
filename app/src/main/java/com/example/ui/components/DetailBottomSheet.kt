package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.MediaItem
import com.example.data.model.RecommendationItem

import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkCardSurface
import com.example.ui.theme.DarkOutline
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.PrimaryIndigo
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DetailBottomSheet(
    mediaItem: MediaItem,
    recommendations: List<RecommendationItem>,
    isLoadingRecommendations: Boolean,
    selectedRecommendationSource: String = "AI",
    onSourceChange: (String) -> Unit = {},
    onDismiss: () -> Unit,
    onPlayClick: (filePath: String, targetPackage: String?) -> Unit,
    onToggleWatched: (MediaItem) -> Unit,
    onToggleFavorite: (MediaItem) -> Unit,
    onRecommendationClick: (RecommendationItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val meta = mediaItem.metadata
    val parsed = mediaItem.parsedInfo

    var selectedPlayerPackage by remember { mutableStateOf<String?>(null) } // null = chooser

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DarkSurface,
        modifier = modifier.fillMaxHeight(0.92f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp)
        ) {
            // Header Image / Banner Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                val banner = meta?.bannerUrl ?: meta?.posterUrl
                if (!banner.isNull_or_empty()) {
                    AsyncImage(
                        model = banner,
                        contentDescription = "Banner",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(DarkSurfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "🎬 myFILMlist", color = TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.55f))
                )

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.7f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White
                    )
                }
            }

            // Media Info Overview Card
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Poster
                Box(
                    modifier = Modifier
                        .width(110.dp)
                        .aspectRatio(2f / 3f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkCardSurface)
                        .border(1.dp, DarkOutline, RoundedCornerShape(12.dp))
                ) {
                    if (!meta?.posterUrl.isNull_or_empty()) {
                        AsyncImage(
                            model = meta!!.posterUrl,
                            contentDescription = "Poster",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = meta?.titleEnglish ?: meta?.titleRomaji ?: parsed.cleanTitle,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(PrimaryIndigo.copy(alpha = 0.2f))
                                .border(1.dp, PrimaryIndigo.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = meta?.scoreSource ?: "AniList",
                                color = PrimaryIndigo,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        val rating = meta?.rating?.let { "%.1f".format(it) } ?: "8.5"
                        Text(
                            text = "★ $rating",
                            color = Color(0xFFFFB300),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )

                        meta?.releaseYear?.let {
                            Text(text = "$it", fontSize = 12.sp, color = TextMuted)
                        }
                    }

                    // Genre Chips
                    if (!meta?.genres.isNullOrEmpty()) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            meta!!.genres.take(4).forEach { genre ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(DarkCardSurface)
                                        .border(1.dp, DarkOutline, RoundedCornerShape(8.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = genre,
                                        fontSize = 10.sp,
                                        color = TextSecondary,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }

                    // Quick Watched & Favorite Toggle Actions
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onToggleWatched(mediaItem) },
                            modifier = Modifier.height(32.dp),
                            shape = RoundedCornerShape(16.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, DarkOutline)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = if (mediaItem.isWatched) Color(0xFF10B981) else TextMuted
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (mediaItem.isWatched) "دیده‌شده" else "نشانه دیده‌شده",
                                fontSize = 11.sp,
                                color = TextPrimary
                            )
                        }

                        IconButton(
                            onClick = { onToggleFavorite(mediaItem) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = if (mediaItem.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Favorite",
                                tint = if (mediaItem.isFavorite) Color(0xFFFF4757) else TextMuted
                            )
                        }
                    }
                }
            }

            // Synopsis Section with expandable & resizable font controls
            ExpandableSynopsisCard(
                synopsisText = meta?.synopsis ?: "توضیحات مفصلی برای این فایل ثبت نشده است.",
                modifier = Modifier.padding(vertical = 4.dp)
            )

            // External Video Player Selector & Prominent Play Button
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "پخش‌کننده ویدیو",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    PlayerOptionChip(
                        name = "پیش‌فرض سیستم",
                        isSelected = selectedPlayerPackage == null,
                        onClick = { selectedPlayerPackage = null }
                    )
                    PlayerOptionChip(
                        name = "VLC",
                        isSelected = selectedPlayerPackage == "org.videolan.vlc",
                        onClick = { selectedPlayerPackage = "org.videolan.vlc" }
                    )
                    PlayerOptionChip(
                        name = "MX Player",
                        isSelected = selectedPlayerPackage == "com.mxtech.videoplayer.ad",
                        onClick = { selectedPlayerPackage = "com.mxtech.videoplayer.ad" }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        onPlayClick(mediaItem.filePath, selectedPlayerPackage)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("play_video_button"),
                    shape = RoundedCornerShape(25.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "پخش ویدیو",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            // Raw File Specs Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkCardSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkOutline)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "مشخصات فنی فایل",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "نام فایل: ${mediaItem.fileName}",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = TextSecondary
                    )
                    Text(
                        text = "حجم: ${formatFileSize(mediaItem.fileSize)} | کیفیت: ${parsed.resolution ?: "1080p"} | کدک: ${parsed.codec ?: "x264/HEVC"}",
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                }
            }

            // Recommendations Slider
            RecommendationsSlider(
                recommendations = recommendations,
                isLoading = isLoadingRecommendations,
                selectedSource = selectedRecommendationSource,
                onSourceChange = onSourceChange,
                onRecommendationClick = onRecommendationClick,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun PlayerOptionChip(
    name: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) PrimaryIndigo else DarkCardSurface)
            .border(
                width = 1.dp,
                color = if (isSelected) PrimaryIndigo else DarkOutline,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = name,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) Color.White else TextSecondary
        )
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

private fun String?.isNull_or_empty(): Boolean = this == null || this.isEmpty()
