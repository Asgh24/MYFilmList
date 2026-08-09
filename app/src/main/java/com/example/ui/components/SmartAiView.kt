package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.remote.GeminiMetadataService
import kotlinx.coroutines.launch

import com.example.ui.theme.PrimaryIndigo
import com.example.ui.theme.cardSurface
import com.example.ui.theme.textMuted
import com.example.ui.theme.textPrimary
import com.example.ui.theme.textSecondary

import com.example.ui.util.AppLanguage
import com.example.ui.util.UiStrings

@Composable
fun SmartAiView(
    targetLanguage: String,
    uiLanguage: AppLanguage = AppLanguage.PERSIAN,
    onRunAiGrouping: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val strings = UiStrings(uiLanguage)
    var promptInput by remember { mutableStateOf("") }
    var aiResponse by remember { mutableStateOf<String?>(null) }
    var isGenerating by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

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
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = PrimaryIndigo,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = strings.geminiAssistant,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.textPrimary
                )
                Text(
                    text = strings.geminiSubtitle(targetLanguage),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.textMuted
                )
            }
        }

        // Dedicated Batch AI Folder Clustering Action Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = PrimaryIndigo),
            border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryIndigo)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "🪄", fontSize = 22.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = strings.batchGroupTitle,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = strings.batchGroupDesc,
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.85f),
                    lineHeight = 18.sp
                )
                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = onRunAiGrouping,
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = PrimaryIndigo
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .testTag("run_ai_clustering_tab_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "شروع تحلیل و دسته‌بندی پوشه",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Quick AI Action Cards
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.cardSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "✨ قابلیت‌های Gemini AI برای آرشیو شما:",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.textPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "• پاکسازی واترمارک و نام سایت‌های دانلود از اسم فایل‌ها\n• دریافت خلاصه داستان رسمی از AniList و TMDB\n• ترجمه و معادل‌سازی عناوین فیلم‌ها و انیمه‌ها به فارسی/انگلیسی\n• پیشنهاد عناوین مشابه بر اساس ژانر و تم داستانی",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.textSecondary,
                    lineHeight = 20.sp
                )
            }
        }

        // Prompt Input Area
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.cardSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "پرسش مستقیم از Gemini",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.textPrimary
                )

                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    contentAlignment = Alignment.TopStart
                ) {
                    if (promptInput.isEmpty()) {
                        Text(
                            text = if (strings.isFa) "سؤال یا درخواست خود را اینجا بنویسید..." else "Type your prompt or question here...",
                            color = MaterialTheme.colorScheme.textMuted,
                            fontSize = 13.sp
                        )
                    }
                    BasicTextField(
                        value = promptInput,
                        onValueChange = { promptInput = it },
                        textStyle = TextStyle(color = MaterialTheme.colorScheme.textPrimary, fontSize = 13.sp),
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("gemini_prompt_input")
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        val trimmed = promptInput.trim()
                        if (trimmed.isBlank()) {
                            aiResponse = "اتصال به هوش مصنوعی برقرار نشد: لطفاً ابتدا پرسش یا درخواست خود را وارد کنید."
                            return@Button
                        }
                        scope.launch {
                            isGenerating = true
                            aiResponse = null
                            val res = GeminiMetadataService.generateCustomPromptResponse(
                                prompt = trimmed,
                                targetLanguage = if (strings.isFa) "Persian" else "English"
                            )
                            isGenerating = false
                            if (res.isSuccess) {
                                aiResponse = res.getOrNull()
                            } else {
                                val err = res.exceptionOrNull()?.message ?: "خطای غیرمنتظره در برقراری ارتباط با هوش مصنوعی"
                                aiResponse = err
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("ask_gemini_button"),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo)
                ) {
                    if (isGenerating) {
                        CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                    } else {
                        Icon(imageVector = Icons.Default.Send, contentDescription = "Send", tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "ارسال پرسش به Gemini", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Response Output Card
        if (aiResponse != null) {
            val isError = aiResponse!!.contains("برقرار نشد") || aiResponse!!.contains("خطا")
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.cardSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, if (isError) Color(0xFFEF4444) else PrimaryIndigo)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (isError) "⚠️ وضعیت هوش مصنوعی" else "✨ پاسخ Gemini",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isError) Color(0xFFEF4444) else PrimaryIndigo
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = aiResponse!!,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.textPrimary,
                        lineHeight = 19.sp
                    )
                }
            }
        }
    }
}
