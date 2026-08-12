package com.example.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.FolderCopy
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.MediaCollection
import com.example.data.model.MediaType

import androidx.compose.foundation.border
import androidx.compose.material3.MaterialTheme
import com.example.ui.theme.PrimaryIndigo
import com.example.ui.theme.cardSurface
import com.example.ui.theme.textMuted
import com.example.ui.theme.textPrimary

@Composable
fun CollectionCard(
    collection: MediaCollection,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val meta = collection.primaryMetadata
    val total = collection.totalCount

    Column(
        modifier = modifier
            .testTag("collection_card_${collection.collectionKey}")
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .aspectRatio(2f / 3.2f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.cardSurface)
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
        ) {
            // Poster Image
            if (!collection.posterUrl.isNullOrEmpty()) {
                AsyncImage(
                    model = collection.posterUrl,
                    contentDescription = collection.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            when (collection.mediaType) {
                                MediaType.ANIME -> Color(0xFF1E1B2E)
                                MediaType.MOVIE -> Color(0xFF1B242E)
                                MediaType.SERIES -> Color(0xFF1A261E)
                                else -> Color(0xFF222631)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = when (collection.mediaType) {
                            MediaType.ANIME -> "📺"
                            MediaType.MOVIE -> "🎬"
                            MediaType.SERIES -> "🌟"
                            else -> "📁"
                        },
                        fontSize = 36.sp
                    )
                }
            }

            // Sleek dark gradient overlay at bottom
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.85f)
                            ),
                            startY = 100f
                        )
                    )
            )

            // Top-left "Needs Review / Uncertain AI" Badge
            if (collection.needsReview || collection.candidateMatches.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFF59E0B))
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "⚠️ تایید",
                            color = Color.Black,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Top-right episode / file count tag
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (total > 1) PrimaryIndigo else Color.Black.copy(alpha = 0.7f)
                    )
                    .padding(horizontal = 7.dp, vertical = 3.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    if (total > 1) {
                        Icon(
                            imageVector = Icons.Default.FolderCopy,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(10.dp)
                        )
                    }
                    Text(
                        text = if (total > 1) "$total قسمت" else "۱ فایل",
                        color = Color.White,
                        fontSize = 10.sp,
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
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val source = collection.scoreSource ?: "AniList"
                val sourceColor = when {
                    source.contains("AniList", true) -> Color(0xFF02A9FF)
                    source.contains("TMDB", true) -> Color(0xFF01B4E4)
                    source.contains("MAL", true) || source.contains("MyAnimeList", true) -> Color(0xFF2E51A2)
                    else -> PrimaryIndigo
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(sourceColor.copy(alpha = 0.9f))
                        .padding(horizontal = 5.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = source.take(7),
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                val score = collection.rating?.let { "%.1f".format(it) } ?: "8.5"
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Black.copy(alpha = 0.6f))
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = "★ $score",
                        color = Color(0xFFFFB300),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Title and Collection info below poster
        Column(modifier = Modifier.padding(top = 8.dp, start = 2.dp, end = 2.dp)) {
            Text(
                text = meta?.titleEnglish ?: meta?.titleRomaji ?: collection.title,
                color = MaterialTheme.colorScheme.textPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            val categoryLabel = when (collection.mediaType) {
                MediaType.ANIME -> "انیمه"
                MediaType.SERIES -> "سریال"
                MediaType.MOVIE -> "فیلم"
                else -> "ویدیو"
            }

            Text(
                text = "$categoryLabel • $total قسمت",
                color = MaterialTheme.colorScheme.textMuted,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
