package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.data.model.MediaType

import com.example.ui.theme.DarkCardSurface
import com.example.ui.theme.DarkOutline
import com.example.ui.theme.PrimaryIndigo
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary

import com.example.ui.util.AppLanguage
import com.example.ui.util.UiStrings

@Composable
fun FilterChips(
    selectedCategory: MediaType,
    onCategorySelected: (MediaType) -> Unit,
    uiLanguage: AppLanguage = AppLanguage.PERSIAN,
    modifier: Modifier = Modifier
) {
    val strings = UiStrings(uiLanguage)
    val scrollState = rememberScrollState()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChipItem(
            label = if (strings.isFa) "همه فایل‌ها" else "All Files",
            isSelected = selectedCategory == MediaType.ALL,
            onClick = { onCategorySelected(MediaType.ALL) },
            testTag = "chip_all"
        )
        FilterChipItem(
            label = strings.filterAnime,
            isSelected = selectedCategory == MediaType.ANIME,
            onClick = { onCategorySelected(MediaType.ANIME) },
            testTag = "chip_anime"
        )
        FilterChipItem(
            label = if (strings.isFa) "فیلم‌ها" else "Movies",
            isSelected = selectedCategory == MediaType.MOVIE,
            onClick = { onCategorySelected(MediaType.MOVIE) },
            testTag = "chip_movies"
        )
        FilterChipItem(
            label = if (strings.isFa) "سریال‌ها" else "Series",
            isSelected = selectedCategory == MediaType.SERIES,
            onClick = { onCategorySelected(MediaType.SERIES) },
            testTag = "chip_series"
        )
        FilterChipItem(
            label = if (strings.isFa) "شناسایی‌نشده" else "Uncategorized",
            isSelected = selectedCategory == MediaType.UNKNOWN,
            onClick = { onCategorySelected(MediaType.UNKNOWN) },
            testTag = "chip_unknown"
        )
    }
}

@Composable
private fun FilterChipItem(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    val bgColor = if (isSelected) PrimaryIndigo else DarkCardSurface
    val textColor = if (isSelected) Color.White else TextMuted
    val borderColor = if (isSelected) PrimaryIndigo else DarkOutline

    Box(
        modifier = Modifier
            .testTag(testTag)
            .clip(RoundedCornerShape(20.dp))
            .background(bgColor)
            .border(
                width = 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(20.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}
