package com.example.ui.components

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.MediaCollection
import com.example.data.model.MediaItem
import com.example.data.model.RecommendationItem

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
fun CollectionBottomSheet(
    collection: MediaCollection,
    recommendations: List<RecommendationItem>,
    isLoadingRecommendations: Boolean,
    selectedRecommendationSource: String = "AI",
    onSourceChange: (String) -> Unit = {},
    onDismiss: () -> Unit,
    onPlayClick: (filePath: String, targetPackage: String?) -> Unit,
    onToggleWatched: (MediaItem) -> Unit,
    onToggleFavorite: (MediaItem) -> Unit,
    onMarkAllWatched: (Boolean) -> Unit = {},
    onReAnalyzeCollection: () -> Unit = {},
    onDeleteCollection: () -> Unit = {},
    onRecommendationClick: (RecommendationItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val meta = collection.primaryMetadata
    var selectedPlayerPackage by remember { mutableStateOf<String?>(null) } // null = System Chooser
    var searchQuery by remember { mutableStateOf("") }
    var isSortAscending by remember { mutableStateOf(true) }

    // Guaranteed rich synopsis for all collections
    val synopsisText = meta?.synopsis?.takeIf { it.isNotBlank() }
        ?: "مجموعه آرشیو «${collection.title}» شامل ${collection.totalCount} قسمت ویدیویی با کیفیت بالا. دارای جدول دقیق قسمت‌ها و امکان علامت‌گذاری قسمت‌های دیده‌شده."

    // Watched statistics calculation
    val watchedCount = collection.items.count { it.isWatched }
    val progressFraction = if (collection.totalCount > 0) watchedCount.toFloat() / collection.totalCount.toFloat() else 0f
    val progressPercent = (progressFraction * 100).toInt()

    // Find next unwatched episode for hero play button
    val nextUnwatchedItem = collection.items.firstOrNull { !it.isWatched } ?: collection.items.firstOrNull()

    // Filter and sort items
    val filteredItems = remember(collection.items, searchQuery, isSortAscending) {
        var list = collection.items
        if (searchQuery.isNotBlank()) {
            list = list.filter {
                it.fileName.contains(searchQuery, ignoreCase = true) ||
                (it.parsedInfo.episode?.toString() ?: "").contains(searchQuery)
            }
        }
        if (!isSortAscending) {
            list = list.reversed()
        }
        list
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DarkSurface,
        modifier = modifier.fillMaxHeight(0.92f)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 24.dp)
        ) {
            // Header Banner / Poster Area
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                ) {
                    val banner = collection.bannerUrl ?: collection.posterUrl
                    if (!banner.isNullOrEmpty()) {
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
                            Text(
                                text = "🎬 ${collection.title}",
                                color = TextPrimary,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
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
            }

            // Collection Main Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Poster
                    Box(
                        modifier = Modifier
                            .width(90.dp)
                            .aspectRatio(2f / 3f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(DarkCardSurface)
                            .border(1.dp, DarkOutline, RoundedCornerShape(12.dp))
                    ) {
                        if (!collection.posterUrl.isNullOrEmpty()) {
                            AsyncImage(
                                model = collection.posterUrl,
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
                            text = meta?.titleEnglish ?: meta?.titleRomaji ?: collection.title,
                            fontSize = 17.sp,
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
                                    text = "${collection.totalCount} قسمت",
                                    color = PrimaryIndigo,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            val rating = collection.rating?.let { "%.1f".format(it) } ?: "8.5"
                            Text(
                                text = "★ $rating",
                                color = Color(0xFFFFB300),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        val genresList = meta?.genres?.takeIf { it.isNotEmpty() } ?: listOf("HD", "ویدیویی")
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            genresList.take(4).forEach { genre ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(DarkCardSurface)
                                        .border(1.dp, DarkOutline, RoundedCornerShape(6.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = genre,
                                        fontSize = 10.sp,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // AI Re-analysis & Delete Actions Row
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onReAnalyzeCollection,
                        modifier = Modifier
                            .weight(1.5f)
                            .height(38.dp),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryIndigo.copy(alpha = 0.6f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = PrimaryIndigo,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "بررسی مجدد با AI 🪄",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryIndigo
                        )
                    }

                    OutlinedButton(
                        onClick = onDeleteCollection,
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = "حذف مجموعه 🗑️",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFEF4444)
                        )
                    }
                }
            }

            // Expandable & Resizable Synopsis Section
            item {
                ExpandableSynopsisCard(synopsisText = synopsisText)
            }

            // Watched Progress Bar Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkCardSurface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkOutline)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "پیشرفت تماشا: $watchedCount از ${collection.totalCount} قسمت ($progressPercent٪)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )

                            Text(
                                text = if (watchedCount == collection.totalCount) "تکمیل شد ✓" else "در حال تماشا",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (watchedCount == collection.totalCount) Color(0xFF10B981) else PrimaryIndigo
                            )
                        }

                        LinearProgressIndicator(
                            progress = { progressFraction },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = Color(0xFF10B981),
                            trackColor = DarkSurfaceVariant
                        )
                    }
                }
            }

            // Hero Play Next Unwatched Episode Button
            if (nextUnwatchedItem != null) {
                item {
                    val epText = nextUnwatchedItem.parsedInfo.episode?.let { "قسمت $it" } ?: nextUnwatchedItem.fileName
                    Button(
                        onClick = { onPlayClick(nextUnwatchedItem.filePath, selectedPlayerPackage) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                            .height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                    ) {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "ادامه تماشا ($epText)",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            // Target Player Selection Chips & Batch Action Row
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("پلیر:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                        PlayerOptionChip(
                            name = "سیستم",
                            isSelected = selectedPlayerPackage == null,
                            onClick = { selectedPlayerPackage = null }
                        )
                        PlayerOptionChip(
                            name = "VLC",
                            isSelected = selectedPlayerPackage == "org.videolan.vlc",
                            onClick = { selectedPlayerPackage = "org.videolan.vlc" }
                        )
                        PlayerOptionChip(
                            name = "MX",
                            isSelected = selectedPlayerPackage == "com.mxtech.videoplayer.ad",
                            onClick = { selectedPlayerPackage = "com.mxtech.videoplayer.ad" }
                        )
                    }

                    // Batch mark all as watched
                    OutlinedButton(
                        onClick = {
                            val markTarget = watchedCount < collection.totalCount
                            onMarkAllWatched(markTarget)
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DoneAll,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = PrimaryIndigo
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (watchedCount < collection.totalCount) "علامت همه دیده‌شده" else "حذف علامت همه",
                            fontSize = 11.sp,
                            color = PrimaryIndigo,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Episode List Header & Search/Sort Bar
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.VideoLibrary,
                            contentDescription = null,
                            tint = PrimaryIndigo,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "جدول قسمت‌ها (${filteredItems.size})",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }

                    IconButton(
                        onClick = { isSortAscending = !isSortAscending },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sort,
                            contentDescription = "مرتب‌سازی",
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Search text box if items > 4
            if (collection.totalCount > 4) {
                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("جستجوی شماره قسمت یا نام فایل...", fontSize = 11.sp, color = TextMuted) },
                        singleLine = true,
                        leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = TextMuted, modifier = Modifier.size(16.dp)) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(imageVector = Icons.Default.Close, contentDescription = "پاک کردن", tint = TextMuted, modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 2.dp)
                            .height(46.dp),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }

            // Episode List Items (Directly inside parent LazyColumn for smooth scrolling!)
            itemsIndexed(filteredItems, key = { _, item -> item.id }) { index, item ->
                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                    EpisodeItemRow(
                        index = index + 1,
                        item = item,
                        onPlayClick = { onPlayClick(item.filePath, selectedPlayerPackage) },
                        onToggleWatched = { onToggleWatched(item) },
                        onToggleFavorite = { onToggleFavorite(item) }
                    )
                }
            }

            // Recommendations Slider
            item {
                RecommendationsSlider(
                    recommendations = recommendations,
                    isLoading = isLoadingRecommendations,
                    selectedSource = selectedRecommendationSource,
                    onSourceChange = onSourceChange,
                    onRecommendationClick = onRecommendationClick,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }
        }
    }
}

@Composable
fun ExpandableSynopsisCard(
    synopsisText: String,
    modifier: Modifier = Modifier
) {
    var isExpanded by rememberSaveable { mutableStateOf(false) }
    var fontSizeSp by rememberSaveable { mutableStateOf(13) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCardSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkOutline)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Header Row with title, font size adjust, and expand/collapse button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📖 خلاصه داستان",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Font Size Decrease A-
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(DarkSurfaceVariant)
                            .clickable { if (fontSizeSp > 10) fontSizeSp -= 1 }
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Text("A-", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                    }

                    Text("${fontSizeSp}pt", fontSize = 10.sp, color = TextMuted)

                    // Font Size Increase A+
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(DarkSurfaceVariant)
                            .clickable { if (fontSizeSp < 18) fontSizeSp += 1 }
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Text("A+", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Expand/Collapse Toggle Button
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(PrimaryIndigo.copy(alpha = 0.15f))
                            .border(1.dp, PrimaryIndigo.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            .clickable { isExpanded = !isExpanded }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (isExpanded) "بستن ▴" else "نمایش کامل ▾",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryIndigo
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Body Text
            Text(
                text = synopsisText,
                fontSize = fontSizeSp.sp,
                color = TextSecondary,
                lineHeight = (fontSizeSp * 1.5).sp,
                maxLines = if (isExpanded) Int.MAX_VALUE else 3,
                overflow = TextOverflow.Ellipsis
            )

            if (!isExpanded && synopsisText.length > 80) {
                Text(
                    text = "برای مطالعه کامل داستان کلیک کنید...",
                    fontSize = 11.sp,
                    color = PrimaryIndigo,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .padding(top = 6.dp)
                        .clickable { isExpanded = true }
                )
            }
        }
    }
}

@Composable
private fun EpisodeItemRow(
    index: Int,
    item: MediaItem,
    onPlayClick: () -> Unit,
    onToggleWatched: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    val parsed = item.parsedInfo
    val epLabel = when {
        parsed.season != null && parsed.episode != null -> "S${parsed.season}:E${parsed.episode}"
        parsed.episode != null -> "قسمت ${parsed.episode}"
        else -> "قسمت $index"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onPlayClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (item.isWatched) DarkCardSurface.copy(alpha = 0.7f) else DarkCardSurface
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (item.isWatched) Color(0xFF10B981).copy(alpha = 0.4f) else DarkOutline.copy(alpha = 0.6f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Episode badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (item.isWatched) Color(0xFF10B981).copy(alpha = 0.2f) else PrimaryIndigo.copy(alpha = 0.2f))
                    .border(
                        1.dp,
                        if (item.isWatched) Color(0xFF10B981).copy(alpha = 0.4f) else PrimaryIndigo.copy(alpha = 0.4f),
                        RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 5.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = epLabel,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (item.isWatched) Color(0xFF10B981) else PrimaryIndigo
                )
            }

            // File Info
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = item.fileName,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (item.isWatched) TextSecondary else TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = "${formatFileSize(item.fileSize)} • ${parsed.resolution ?: "1080p"}",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = TextMuted
                )
            }

            // Direct "Mark as Watched" Toggle Button
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (item.isWatched) Color(0xFF10B981).copy(alpha = 0.2f) else DarkSurfaceVariant
                    )
                    .border(
                        1.dp,
                        if (item.isWatched) Color(0xFF10B981) else DarkOutline,
                        RoundedCornerShape(8.dp)
                    )
                    .clickable { onToggleWatched() }
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = if (item.isWatched) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                        contentDescription = "علامت دیده‌شده",
                        tint = if (item.isWatched) Color(0xFF10B981) else TextMuted,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = if (item.isWatched) "دیده‌شده" else "دیده‌نشده",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (item.isWatched) Color(0xFF10B981) else TextMuted
                    )
                }
            }

            // Play button icon
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(PrimaryIndigo)
                    .clickable { onPlayClick() }
                    .padding(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "پخش",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
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
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) PrimaryIndigo else DarkCardSurface)
            .border(
                width = 1.dp,
                color = if (isSelected) PrimaryIndigo else DarkOutline,
                shape = RoundedCornerShape(10.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = name,
            fontSize = 10.sp,
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
