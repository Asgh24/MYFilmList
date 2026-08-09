package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.MediaCollection
import com.example.data.model.MediaMetadata
import com.example.data.model.MediaType
import com.example.ui.theme.DarkCardSurface
import com.example.ui.theme.DarkOutline
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.PrimaryIndigo
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionAiEditSheet(
    collection: MediaCollection,
    proposedMetadata: MediaMetadata?,
    isAnalyzing: Boolean,
    onConfirmUpdate: (newTitle: String, newType: MediaType, newSynopsis: String, newPoster: String) -> Unit,
    onReFetchAi: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var editableTitle by remember(collection, proposedMetadata) {
        mutableStateOf(proposedMetadata?.titleEnglish ?: proposedMetadata?.titleRomaji ?: collection.title)
    }
    var selectedType by remember(collection, proposedMetadata) {
        mutableStateOf(
            if (proposedMetadata != null) {
                if (proposedMetadata.genres.any { it.contains("Anime", ignoreCase = true) }) MediaType.ANIME else collection.mediaType
            } else collection.mediaType
        )
    }
    var editableSynopsis by remember(collection, proposedMetadata) {
        mutableStateOf(proposedMetadata?.synopsis ?: collection.primaryMetadata?.synopsis ?: "")
    }
    var editablePoster by remember(collection, proposedMetadata) {
        mutableStateOf(proposedMetadata?.posterUrl ?: collection.primaryMetadata?.posterUrl ?: "")
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DarkSurface,
        modifier = modifier.fillMaxHeight(0.9f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(PrimaryIndigo.copy(alpha = 0.2f))
                            .border(1.dp, PrimaryIndigo.copy(alpha = 0.5f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = PrimaryIndigo,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "تحلیل و ویرایش هوشمند مجموعه با AI",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "تصحیح نوع اثر (انیمه/فیلم)، عنوان رسمی و خلاصه داستان فارسی",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "بستن", tint = TextMuted)
                }
            }

            if (isAnalyzing) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(color = PrimaryIndigo)
                        Text(
                            text = "هوش مصنوعی در حال تحلیل اطلاعات و دریافت مشخصات انیمه/فیلم...",
                            fontSize = 12.sp,
                            color = PrimaryIndigo,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    // Preview Card
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = DarkCardSurface),
                            border = androidx.compose.foundation.BorderStroke(1.dp, DarkOutline)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(70.dp)
                                        .aspectRatio(2f / 3f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(DarkSurfaceVariant)
                                ) {
                                    if (editablePoster.isNotBlank()) {
                                        AsyncImage(
                                            model = editablePoster,
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                }

                                Column(
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = editableTitle.ifBlank { collection.title },
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "نام قبلی: ${collection.title} (${collection.totalCount} فایل)",
                                        fontSize = 11.sp,
                                        color = TextMuted
                                    )
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(PrimaryIndigo.copy(alpha = 0.2f))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = selectedType.name,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = PrimaryIndigo
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Editable Title
                    item {
                        OutlinedTextField(
                            value = editableTitle,
                            onValueChange = { editableTitle = it },
                            label = { Text("عنوان تصحیح‌شده (فارسی / انگلیسی / ژاپنی)", fontSize = 12.sp) },
                            singleLine = true,
                            leadingIcon = {
                                Icon(imageVector = Icons.Default.Edit, contentDescription = null, tint = PrimaryIndigo, modifier = Modifier.size(18.dp))
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    // Media Type Selection Chips
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("دسته‌بندی اثر (تشخیص نوع):", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                TypeSelectorChip(
                                    label = "🌸 انیمه (Anime)",
                                    isSelected = selectedType == MediaType.ANIME,
                                    onClick = { selectedType = MediaType.ANIME },
                                    modifier = Modifier.weight(1f)
                                )
                                TypeSelectorChip(
                                    label = "📺 سریال (Series)",
                                    isSelected = selectedType == MediaType.SERIES,
                                    onClick = { selectedType = MediaType.SERIES },
                                    modifier = Modifier.weight(1f)
                                )
                                TypeSelectorChip(
                                    label = "🎬 فیلم (Movie)",
                                    isSelected = selectedType == MediaType.MOVIE,
                                    onClick = { selectedType = MediaType.MOVIE },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    // Editable Synopsis
                    item {
                        OutlinedTextField(
                            value = editableSynopsis,
                            onValueChange = { editableSynopsis = it },
                            label = { Text("خلاصه داستان به زبان فارسی", fontSize = 12.sp) },
                            minLines = 3,
                            maxLines = 6,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    // Poster URL
                    item {
                        OutlinedTextField(
                            value = editablePoster,
                            onValueChange = { editablePoster = it },
                            label = { Text("لینک تصویر پوستر", fontSize = 12.sp) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onReFetchAi,
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("تحلیل مجدد با AI", fontSize = 11.sp, color = TextSecondary)
                    }

                    Button(
                        onClick = {
                            onConfirmUpdate(
                                editableTitle.trim().ifBlank { collection.title },
                                selectedType,
                                editableSynopsis,
                                editablePoster
                            )
                        },
                        modifier = Modifier
                            .weight(1.3f)
                            .height(46.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo)
                    ) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("تایید و ثبت تغییرات", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun TypeSelectorChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) PrimaryIndigo else DarkCardSurface)
            .border(1.dp, if (isSelected) PrimaryIndigo else DarkOutline, RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(vertical = 10.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) Color.White else TextSecondary
        )
    }
}
