package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.PrimaryIndigo
import com.example.ui.theme.cardSurface
import com.example.ui.theme.textMuted
import com.example.ui.theme.textPrimary
import com.example.ui.theme.textSecondary
import com.example.ui.util.AppLanguage
import com.example.ui.util.AppThemeMode
import com.example.ui.util.UiStrings
import com.example.ui.viewmodel.KeyTestStatus

import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.material.icons.filled.OpenInNew

@Composable
fun SettingsView(
    targetLanguage: String,
    uiLanguage: AppLanguage = AppLanguage.PERSIAN,
    themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    geminiApiKey: String = "",
    keyTestStatus: KeyTestStatus = KeyTestStatus.Idle,
    tmdbApiKey: String = "",
    tmdbKeyTestStatus: KeyTestStatus = KeyTestStatus.Idle,
    omdbApiKey: String = "",
    omdbKeyTestStatus: KeyTestStatus = KeyTestStatus.Idle,
    onLanguageSelected: (String) -> Unit,
    onUiLanguageSelected: (AppLanguage) -> Unit = {},
    onThemeModeSelected: (AppThemeMode) -> Unit = {},
    onSaveAndTestKey: (String) -> Unit = {},
    onClearKey: () -> Unit = {},
    onSaveAndTestTmdbKey: (String) -> Unit = {},
    onClearTmdbKey: () -> Unit = {},
    onSaveAndTestOmdbKey: (String) -> Unit = {},
    onClearOmdbKey: () -> Unit = {},
    onRescanClick: () -> Unit,
    onClearCacheClick: () -> Unit,
    onRunAiGrouping: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val strings = UiStrings(uiLanguage)
    val uriHandler = LocalUriHandler.current
    val languages = listOf("Persian", "English", "Japanese", "Spanish", "German", "French")

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = null,
                tint = PrimaryIndigo,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = strings.appSettings,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.textPrimary
                )
                Text(
                    text = strings.settingsSubtitle,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.textMuted
                )
            }
        }

        // App UI Language Selection Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.cardSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Language, contentDescription = null, tint = PrimaryIndigo)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = strings.uiLanguageSetting,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.textPrimary
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = strings.uiLanguageDesc,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.textMuted
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    AppLanguage.values().forEach { appLang ->
                        val isSelected = uiLanguage == appLang
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) PrimaryIndigo else MaterialTheme.colorScheme.surfaceVariant)
                                .border(1.dp, if (isSelected) PrimaryIndigo else MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                                .clickable { onUiLanguageSelected(appLang) }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (strings.isFa) appLang.displayNameFa else appLang.displayNameEn,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.textPrimary
                            )
                        }
                    }
                }
            }
        }

        // App Theme Mode Selection Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.cardSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Contrast, contentDescription = null, tint = PrimaryIndigo)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = strings.themeModeSetting,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.textPrimary
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = strings.themeModeDesc,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.textMuted
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    AppThemeMode.values().forEach { mode ->
                        val isSelected = themeMode == mode
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) PrimaryIndigo else MaterialTheme.colorScheme.surfaceVariant)
                                .border(1.dp, if (isSelected) PrimaryIndigo else MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                                .clickable { onThemeModeSelected(mode) }
                                .padding(vertical = 10.dp, horizontal = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (strings.isFa) mode.displayNameFa else mode.displayNameEn,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // Movie & Poster API Keys Card (TMDB / OMDb)
        var tmdbInput by remember(tmdbApiKey) { mutableStateOf(tmdbApiKey) }
        var isTmdbVisible by remember { mutableStateOf(false) }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.cardSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Movie,
                            contentDescription = null,
                            tint = PrimaryIndigo,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "کلید API پوستر و تصویر فیلم‌ها (TMDB / OMDb)",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.textPrimary
                        )
                    }

                    if (tmdbApiKey.isNotBlank() && tmdbKeyTestStatus is KeyTestStatus.Success) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF10B981).copy(alpha = 0.2f))
                                .border(1.dp, Color(0xFF10B981).copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "فعال ✓",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF10B981)
                            )
                        }
                    }
                }

                Text(
                    text = "برای دریافت کاور و پوستر باکیفیت فیلم‌ها، می‌توانید کلید API اختصاصی خود را وارد کنید:",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.textMuted,
                    lineHeight = 18.sp
                )

                // Direct link buttons for TMDB and OMDb registration
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedButton(
                        onClick = { uriHandler.openUri("https://www.themoviedb.org/settings/api") },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).height(38.dp)
                    ) {
                        Icon(imageVector = Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(14.dp), tint = PrimaryIndigo)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "دریافت کلید TMDB ↗", fontSize = 11.sp, color = PrimaryIndigo, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = { uriHandler.openUri("https://www.omdbapi.com/apikey.aspx") },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).height(38.dp)
                    ) {
                        Icon(imageVector = Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(14.dp), tint = PrimaryIndigo)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "دریافت کلید OMDb ↗", fontSize = 11.sp, color = PrimaryIndigo, fontWeight = FontWeight.Bold)
                    }
                }

                OutlinedTextField(
                    value = tmdbInput,
                    onValueChange = { tmdbInput = it },
                    label = { Text("TMDB API Key", color = MaterialTheme.colorScheme.textMuted) },
                    placeholder = { Text("مثال: 1a2b3c4d5e...", color = MaterialTheme.colorScheme.textMuted) },
                    singleLine = true,
                    visualTransformation = if (isTmdbVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { isTmdbVisible = !isTmdbVisible }) {
                            Icon(
                                imageVector = if (isTmdbVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = "نمایش کلید",
                                tint = MaterialTheme.colorScheme.textSecondary
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                when (tmdbKeyTestStatus) {
                    is KeyTestStatus.Testing -> {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = PrimaryIndigo)
                            Text("در حال آزمایش کلید TMDB...", fontSize = 12.sp, color = MaterialTheme.colorScheme.textPrimary)
                        }
                    }
                    is KeyTestStatus.Success -> {
                        Text(text = tmdbKeyTestStatus.message, fontSize = 12.sp, color = Color(0xFF10B981), fontWeight = FontWeight.Bold)
                    }
                    is KeyTestStatus.Error -> {
                        Text(text = tmdbKeyTestStatus.errorMessage, fontSize = 12.sp, color = Color(0xFFFF4757), fontWeight = FontWeight.Bold)
                    }
                    else -> {}
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = { onSaveAndTestTmdbKey(tmdbInput) },
                        enabled = tmdbInput.isNotBlank() && tmdbKeyTestStatus !is KeyTestStatus.Testing,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                        modifier = Modifier.weight(1f).height(42.dp)
                    ) {
                        Text(text = "ذخیره و بررسی کلید TMDB", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    if (tmdbApiKey.isNotBlank() || tmdbInput.isNotBlank()) {
                        OutlinedButton(
                            onClick = {
                                tmdbInput = ""
                                onClearTmdbKey()
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.height(42.dp)
                        ) {
                            Text(text = "پاکسازی", fontSize = 12.sp, color = Color(0xFFFF4757))
                        }
                    }
                }
            }
        }

        // Gemini API Key Input & Verification Card
        var keyInput by remember(geminiApiKey) { mutableStateOf(geminiApiKey) }
        var isKeyVisible by remember { mutableStateOf(false) }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.cardSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Key,
                            contentDescription = null,
                            tint = PrimaryIndigo,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "کلید Google Gemini API",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.textPrimary
                        )
                    }

                    if (geminiApiKey.isNotBlank() && keyTestStatus is KeyTestStatus.Success) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF10B981).copy(alpha = 0.2f))
                                .border(1.dp, Color(0xFF10B981).copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "فعال ✓",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF10B981)
                            )
                        }
                    }
                }

                Text(
                    text = "برای استفاده مستقیم از هوش مصنوعی Gemini در پاسخگویی، تحلیل آرشیو و گروه‌بندی فایل‌ها، کلید API خود را وارد کنید:",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.textMuted,
                    lineHeight = 17.sp
                )

                OutlinedButton(
                    onClick = { uriHandler.openUri("https://aistudio.google.com/app/apikey") },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().height(38.dp)
                ) {
                    Icon(imageVector = Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(14.dp), tint = PrimaryIndigo)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "دریافت رایگان کلید Gemini API از Google AI Studio ↗", fontSize = 11.sp, color = PrimaryIndigo, fontWeight = FontWeight.Bold)
                }

                OutlinedTextField(
                    value = keyInput,
                    onValueChange = { keyInput = it },
                    label = { Text("Gemini API Key (AIzaSy...)", color = MaterialTheme.colorScheme.textMuted) },
                    placeholder = { Text("AIzaSy...", color = MaterialTheme.colorScheme.textMuted) },
                    singleLine = true,
                    visualTransformation = if (isKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { isKeyVisible = !isKeyVisible }) {
                            Icon(
                                imageVector = if (isKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = "تغییر وضعیت نمایش کلید",
                                tint = MaterialTheme.colorScheme.textSecondary
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("gemini_api_key_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                // Key Test Status Badge
                when (keyTestStatus) {
                    is KeyTestStatus.Testing -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = PrimaryIndigo
                            )
                            Text(
                                text = "در حال ارسال درخواست تست به سرور گوگل...",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.textPrimary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    is KeyTestStatus.Success -> {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF10B981)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF10B981),
                                    modifier = Modifier.size(22.dp)
                                )
                                Text(
                                    text = keyTestStatus.message,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.textPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    is KeyTestStatus.Error -> {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF4757)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = Color(0xFFFF4757),
                                    modifier = Modifier.size(22.dp)
                                )
                                Text(
                                    text = keyTestStatus.errorMessage,
                                    fontSize = 12.sp,
                                    color = Color(0xFFFF4757),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    is KeyTestStatus.Idle -> {
                        if (geminiApiKey.isBlank()) {
                            Text(
                                text = "💡 می‌توانید کلید رایگان را از Google AI Studio (aistudio.google.com) دریافت کرده و اینجا وارد کنید.",
                                fontSize = 11.sp,
                                color = PrimaryIndigo
                            )
                        }
                    }
                }

                // Action buttons for API Key
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = { onSaveAndTestKey(keyInput) },
                        enabled = keyInput.isNotBlank() && keyTestStatus !is KeyTestStatus.Testing,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("test_gemini_key_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "تست و بررسی کلید Gemini", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    if (geminiApiKey.isNotBlank() || keyInput.isNotBlank()) {
                        OutlinedButton(
                            onClick = {
                                keyInput = ""
                                onClearKey()
                            },
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF4757)),
                            modifier = Modifier.height(44.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                tint = Color(0xFFFF4757),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "پاک کردن", fontSize = 12.sp, color = Color(0xFFFF4757))
                        }
                    }
                }
            }
        }

        // Multi-language Selection
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.cardSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Language, contentDescription = null, tint = PrimaryIndigo)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "زبان متادیتا و هوش مصنوعی",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.textPrimary
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "زبان مورد نظر برای ترجمه خلاصه‌ها و دریافت پیشنهادها را انتخاب کنید:",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.textMuted
                )

                Spacer(modifier = Modifier.height(12.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    languages.forEach { lang ->
                        val isSelected = targetLanguage.equals(lang, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) PrimaryIndigo else MaterialTheme.colorScheme.surfaceVariant)
                                .border(1.dp, if (isSelected) PrimaryIndigo else MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                                .clickable { onLanguageSelected(lang) }
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = when(lang) {
                                        "Persian" -> "فارسی (Persian)"
                                        "English" -> "انگلیسی (English)"
                                        "Japanese" -> "ژاپنی (Japanese)"
                                        "Spanish" -> "اسپانیایی (Spanish)"
                                        "German" -> "آلمانی (German)"
                                        "French" -> "فرانسوی (French)"
                                        else -> lang
                                    },
                                    fontSize = 14.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.textPrimary
                                )
                                if (isSelected) {
                                    Text(text = "✓", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Action Buttons
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onRunAiGrouping,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("settings_re_categorize_button"),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo)
            ) {
                Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "دسته‌بندی مجدد تمام فایل‌ها با هوش مصنوعی", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onRescanClick,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("rescan_library_button"),
                    shape = RoundedCornerShape(24.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = null, tint = MaterialTheme.colorScheme.textPrimary)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "اسکن مجدد", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.textPrimary)
                }

                OutlinedButton(
                    onClick = onClearCacheClick,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("clear_cache_button"),
                    shape = RoundedCornerShape(24.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF4757))
                ) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = null, tint = Color(0xFFFF4757))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "پاکسازی دیتابیس", color = Color(0xFFFF4757), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
