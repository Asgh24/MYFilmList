package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LibraryAdd
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ManualCollectionInput
import com.example.data.model.ManualEpisode
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
fun ManualAddCollectionSheet(
    onConfirm: (ManualCollectionInput) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var titleInput by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(MediaType.ANIME) }
    var synopsisInput by remember { mutableStateOf("") }
    var posterInput by remember { mutableStateOf("") }
    var episodes by remember { mutableStateOf(listOf(ManualEpisode(fileName = ""))) }

    val canSubmit = titleInput.trim().isNotBlank()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DarkSurface,
        modifier = modifier.fillMaxHeight(0.92f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
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
                            .border(1.dp, PrimaryIndigo.copy(alpha = 0.6f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.LibraryAdd,
                            contentDescription = null,
                            tint = PrimaryIndigo,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "افزودن مجموعه به صورت دستی",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "بدون نیاز به اسکن فایل، عنوان و قسمت‌ها را خودتان وارد کنید",
                            fontSize = 11.sp,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "بستن", tint = TextMuted)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Title
            OutlinedTextField(
                value = titleInput,
                onValueChange = { titleInput = it },
                label = { Text("عنوان مجموعه (الزامی)", fontSize = 11.sp) },
                singleLine = true,
                isError = titleInput.isBlank(),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Type selection
            Text(
                text = "نوع مجموعه:",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                ManualTypeChip(
                    label = " انیمه ",
                    isSelected = selectedType == MediaType.ANIME,
                    onClick = { selectedType = MediaType.ANIME },
                    modifier = Modifier.weight(1f)
                )
                ManualTypeChip(
                    label = " سریال ",
                    isSelected = selectedType == MediaType.SERIES,
                    onClick = { selectedType = MediaType.SERIES },
                    modifier = Modifier.weight(1f)
                )
                ManualTypeChip(
                    label = " فیلم ",
                    isSelected = selectedType == MediaType.MOVIE,
                    onClick = { selectedType = MediaType.MOVIE },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Synopsis
            OutlinedTextField(
                value = synopsisInput,
                onValueChange = { synopsisInput = it },
                label = { Text("خلاصه داستان (اختیاری)", fontSize = 11.sp) },
                minLines = 2,
                maxLines = 4,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Poster URL
            OutlinedTextField(
                value = posterInput,
                onValueChange = { posterInput = it },
                label = { Text("لینک تصویر پوستر (اختیاری)", fontSize = 11.sp) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Episodes section header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "قسمت‌ها (${episodes.size})",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                OutlinedButton(
                    onClick = {
                        episodes = episodes + ManualEpisode(fileName = "")
                    },
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryIndigo.copy(alpha = 0.6f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = PrimaryIndigo,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "افزودن قسمت",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryIndigo
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "در صورت عدم افزودن قسمت، یک مدخل با نام مجموعه ایجاد می‌شود.",
                fontSize = 10.sp,
                color = TextMuted
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Episode rows
            episodes.forEachIndexed { index, ep ->
                ManualEpisodeRow(
                    episode = ep,
                    canDelete = episodes.size > 1,
                    onChange = { updated -> episodes = episodes.mapIndexed { i, e -> if (i == index) updated else e } },
                    onDelete = { episodes = episodes.filterIndexed { i, _ -> i != index } }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Submit
            Button(
                onClick = {
                    onConfirm(
                        ManualCollectionInput(
                            title = titleInput,
                            mediaType = selectedType,
                            synopsis = synopsisInput,
                            posterUrl = posterInput,
                            episodes = episodes.map {
                                ManualEpisode(
                                    season = it.season,
                                    episode = it.episode,
                                    fileName = it.fileName.trim()
                                )
                            }
                        )
                    )
                },
                enabled = canSubmit,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo)
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "افزودن مجموعه به کتابخانه",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun ManualEpisodeRow(
    episode: ManualEpisode,
    canDelete: Boolean,
    onChange: (ManualEpisode) -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCardSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkOutline)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = episode.season?.toString() ?: "",
                    onValueChange = { text ->
                        val parsed = text.toIntOrNull()
                        onChange(episode.copy(season = if (parsed != null && parsed in 1..99) parsed else null))
                    },
                    label = { Text("فصل", fontSize = 9.sp) },
                    singleLine = true,
                    modifier = Modifier.width(72.dp),
                    shape = RoundedCornerShape(8.dp)
                )
                OutlinedTextField(
                    value = episode.episode?.toString() ?: "",
                    onValueChange = { text ->
                        val parsed = text.toIntOrNull()
                        onChange(episode.copy(episode = if (parsed != null && parsed in 1..999) parsed else null))
                    },
                    label = { Text("قسمت", fontSize = 9.sp) },
                    singleLine = true,
                    modifier = Modifier.width(72.dp),
                    shape = RoundedCornerShape(8.dp)
                )
                OutlinedTextField(
                    value = episode.fileName,
                    onValueChange = { text -> onChange(episode.copy(fileName = text)) },
                    label = { Text("نام فایل (اختیاری)", fontSize = 9.sp) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                )
                if (canDelete) {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "حذف قسمت",
                            tint = TextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ManualTypeChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) PrimaryIndigo else DarkCardSurface)
            .border(1.dp, if (isSelected) PrimaryIndigo else DarkOutline, RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(vertical = 7.dp),
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
