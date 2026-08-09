package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.compose.material3.MaterialTheme
import com.example.data.model.MediaItem
import com.example.data.model.MediaType
import com.example.ui.theme.textPrimary
import com.example.ui.theme.textSecondary

@Composable
fun MediaCard(
    mediaItem: MediaItem,
    onClick: () -> Unit,
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    onLongClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val meta = mediaItem.metadata
    val parsed = mediaItem.parsedInfo

    Column(
        modifier = modifier
            .testTag("media_card_${mediaItem.id}")
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .aspectRatio(2f / 3f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(if (isSelected) Color(0xFFE8DEF8) else Color(0xFFE7E0EC))
                .border(
                    width = if (isSelected) 3.dp else 0.dp,
                    color = if (isSelected) Color(0xFF6750A4) else Color.Transparent,
                    shape = RoundedCornerShape(16.dp)
                )
        ) {
            // Poster Image
            if (!meta?.posterUrl.isNull_or_empty()) {
                AsyncImage(
                    model = meta!!.posterUrl,
                    contentDescription = parsed.cleanTitle,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // Placeholder according to category
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            when (parsed.detectedType) {
                                MediaType.ANIME -> Color(0xFF303030)
                                MediaType.MOVIE -> Color(0xFF252525)
                                MediaType.SERIES -> Color(0xFF353535)
                                else -> Color(0xFF404040)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = when (parsed.detectedType) {
                            MediaType.ANIME -> "📺"
                            MediaType.MOVIE -> "🎬"
                            MediaType.SERIES -> "🌟"
                            else -> "❓"
                        },
                        fontSize = 36.sp
                    )
                }
            }

            // Dark gradient overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.7f)
                            ),
                            startY = 100f
                        )
                    )
            )

            // Selection Indicator
            if (isSelectionMode) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) Color(0xFF6750A4) else Color.Black.copy(alpha = 0.6f))
                        .padding(4.dp)
                ) {
                    Icon(
                        imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Outlined.Circle,
                        contentDescription = "انتخاب فایل",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Top-right episode tag / resolution badge
            val epTag = when {
                parsed.season != null && parsed.episode != null -> "S${parsed.season} E${parsed.episode}"
                parsed.episode != null -> "EP ${parsed.episode}"
                parsed.resolution != null -> parsed.resolution
                meta?.totalEpisodes != null -> "EP ${meta.totalEpisodes}"
                else -> null
            }

            if (epTag != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black.copy(alpha = 0.6f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = epTag,
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Bottom metadata badges (Source & Rating)
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val source = meta?.scoreSource ?: "AniList"
                val sourceColor = when (source) {
                    "AniList" -> Color(0xFF00FF41)
                    "TMDB" -> Color(0xFFFFD700)
                    "MAL" -> Color(0xFF2E51A2)
                    else -> Color(0xFFD0BCFF)
                }
                val sourceTextColor = if (source == "MAL") Color.White else Color.Black

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(sourceColor)
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = source,
                        color = sourceTextColor,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                val score = meta?.rating?.let { "%.1f".format(it) } ?: "8.5"
                Text(
                    text = "$score/10",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }
        }

        // Title and Raw Filename
        Column(modifier = Modifier.padding(top = 6.dp)) {
            Text(
                text = meta?.titleEnglish ?: meta?.titleRomaji ?: parsed.cleanTitle,
                color = MaterialTheme.colorScheme.textPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = mediaItem.fileName,
                color = MaterialTheme.colorScheme.textSecondary,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun String?.isNull_or_empty(): Boolean = this == null || this.isEmpty()
