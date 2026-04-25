@file:OptIn(ExperimentalMaterial3Api::class)

package com.mawidplus.patient.ui.screens.assistant

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mawidplus.patient.ui.theme.Manrope
import com.mawidplus.patient.ui.theme.OnPrimary
import com.mawidplus.patient.ui.theme.OnSecondaryContainer
import com.mawidplus.patient.ui.theme.OnSurface
import com.mawidplus.patient.ui.theme.OnSurfaceVariant
import com.mawidplus.patient.ui.theme.Outline
import com.mawidplus.patient.ui.theme.OutlineVariant
import com.mawidplus.patient.ui.theme.Primary
import com.mawidplus.patient.ui.theme.PublicSans
import com.mawidplus.patient.ui.theme.Secondary
import com.mawidplus.patient.ui.theme.SecondaryContainer
import com.mawidplus.patient.ui.theme.PrimaryContainer
import com.mawidplus.patient.ui.theme.Surface
import com.mawidplus.patient.ui.theme.SurfaceContainerHigh
import com.mawidplus.patient.ui.theme.SurfaceContainerLow
import com.mawidplus.patient.ui.theme.SurfaceContainerLowest

private val EmergencyRed = Color(0xFFD32F2F)
private val EmergencyBg = Color(0xFFFDE8E8)
private val HighOrange = Color(0xFFE65100)
private val HighOrangeBg = Color(0xFFFFF3E0)

@Composable
fun ChatScreen(
    onBack: () -> Unit,
    onBookDoctor: (String) -> Unit,
    viewModel: AssistantViewModel = viewModel(),
) {
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val quickReplies by viewModel.quickReplies.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val caseSummary by viewModel.caseSummary.collectAsStateWithLifecycle()
    val severity by viewModel.severity.collectAsStateWithLifecycle()

    var input by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size, isLoading) {
        val count = listState.layoutInfo.totalItemsCount
        if (count > 0) {
            listState.scrollToItem(count - 1)
        }
    }

    Scaffold(
        containerColor = Surface,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "مساعد موعد الذكي",
                            fontFamily = Manrope,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp,
                            color = OnSurface,
                        )
                        Text(
                            "مساعدك الصحي الذكي",
                            fontFamily = PublicSans,
                            fontSize = 12.sp,
                            color = OnSurfaceVariant,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع", tint = OnSurface)
                    }
                },
                actions = {
                    IconButton(onClick = { /* menu placeholder */ }) {
                        Icon(Icons.Default.MoreVert, contentDescription = null, tint = OnSurface)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Surface.copy(alpha = 0.92f),
                ),
            )
        },
        bottomBar = {
            ChatInputBar(
                value = input,
                onValueChange = { input = it },
                enabled = !isLoading,
                onSend = {
                    if (input.isNotBlank() && !isLoading) {
                        viewModel.sendUserMessage(input)
                        input = ""
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { MedicalDisclaimerCard() }

            // ملخص الحالة الديناميكي من الـ LLM
            if (!caseSummary.isNullOrBlank()) {
                item {
                    CaseSummaryCard(
                        summary = caseSummary!!,
                        severity = severity,
                    )
                }
            }

            items(messages, key = { it.id }) { msg ->
                Column(Modifier.fillMaxWidth()) {
                    if (msg.isUser) {
                        UserBubble(text = msg.text)
                    } else {
                        AssistantBubble(text = msg.text)
                        if (msg.isError && msg.id == messages.lastOrNull()?.id) {
                            TextButton(
                                onClick = { viewModel.retryLastMessage() },
                                modifier = Modifier.padding(start = 4.dp, top = 4.dp),
                            ) {
                                Text(
                                    "إعادة المحاولة",
                                    fontFamily = PublicSans,
                                    fontSize = 14.sp,
                                    color = Primary,
                                )
                            }
                        }
                    }
                    if (msg.recommendedDoctors.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                        ) {
                            msg.recommendedDoctors.forEach { doc ->
                                val id = doc.id
                                if (id != null) {
                                    DoctorSuggestionCard(
                                        doctor = doc,
                                        onBook = { onBookDoctor(id) },
                                    )
                                }
                            }
                        }
                    }
                }
            }
            if (isLoading) {
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .padding(start = 40.dp, top = 4.dp)
                                .size(22.dp),
                            strokeWidth = 2.dp,
                            color = Primary,
                        )
                    }
                }
            }

            // مؤشر الخطورة — يظهر فقط في حالات الطوارئ أو الخطورة العالية
            if (severity == "emergency" && !isLoading) {
                item { EmergencyBanner() }
            } else if (severity == "high" && !isLoading) {
                item { HighSeverityNote() }
            }

            if (quickReplies.isNotEmpty() && !isLoading) {
                item {
                    QuickRepliesBlock(
                        replies = quickReplies,
                        onSelect = { viewModel.sendUserMessage(it) },
                    )
                }
            }
            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}

@Composable
private fun MedicalDisclaimerCard() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceContainerLow)
            .padding(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(40.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Primary),
        )
        Spacer(Modifier.width(10.dp))
        Text(
            "هذا المساعد يقدّم معلومات صحية عامة وليس تشخيصاً طبياً. " +
                "في الحالات الطارئة يرجى الاتصال بالإسعاف فوراً.",
            fontFamily = PublicSans,
            fontSize = 13.sp,
            color = OnSurfaceVariant,
            lineHeight = 20.sp,
        )
    }
}

@Composable
private fun CaseSummaryCard(summary: String, severity: String?) {
    val accentColor = when (severity) {
        "emergency" -> EmergencyRed
        "high" -> HighOrange
        else -> Primary
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceContainerLowest)
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(20.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(accentColor),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "ملخص الحالة",
                fontFamily = PublicSans,
                fontSize = 12.sp,
                color = OnSurfaceVariant,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            summary,
            fontFamily = Manrope,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = OnSurface,
        )
    }
}

/** بانر طوارئ — يظهر فقط عندما الـ LLM يحدد الحالة كطوارئ */
@Composable
private fun EmergencyBanner() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(EmergencyBg)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Filled.Warning,
            contentDescription = null,
            tint = EmergencyRed,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.width(10.dp))
        Text(
            "⚠️ حالتك قد تستدعي تدخل طبي عاجل. يرجى الاتصال بالإسعاف (997) أو التوجه لأقرب طوارئ فوراً.",
            fontFamily = PublicSans,
            fontSize = 13.sp,
            color = EmergencyRed,
            lineHeight = 20.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

/** ملاحظة خطورة عالية — أقل حدة من الطوارئ */
@Composable
private fun HighSeverityNote() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(HighOrangeBg)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Filled.Warning,
            contentDescription = null,
            tint = HighOrange,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(10.dp))
        Text(
            "ننصحك بزيارة طبيب في أقرب وقت ممكن لتقييم حالتك.",
            fontFamily = PublicSans,
            fontSize = 13.sp,
            color = HighOrange,
            lineHeight = 20.sp,
        )
    }
}

@Composable
private fun UserBubble(text: String) {
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    // RTL: user messages على يمين الشاشة = CenterStart
    val align = if (rtl) Alignment.CenterStart else Alignment.CenterEnd
    Box(Modifier.fillMaxWidth()) {
        Surface(
            modifier = Modifier
                .align(align)
                .fillMaxWidth(0.88f),
            color = PrimaryContainer,
            shape = RoundedCornerShape(24.dp, 24.dp, 8.dp, 24.dp),
            shadowElevation = 2.dp,
        ) {
            Text(
                text,
                modifier = Modifier.padding(16.dp),
                fontFamily = PublicSans,
                fontSize = 15.sp,
                color = OnPrimary,
                textAlign = TextAlign.Start,
            )
        }
    }
}

@Composable
private fun AssistantBubble(text: String) {
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val align = if (rtl) Alignment.CenterEnd else Alignment.CenterStart
    Box(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .align(align)
                .fillMaxWidth(0.92f),
            horizontalArrangement = if (rtl) Arrangement.End else Arrangement.Start,
            verticalAlignment = Alignment.Top,
        ) {
            if (!rtl) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(PrimaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = OnPrimary,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Spacer(Modifier.width(8.dp))
            }
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .clip(RoundedCornerShape(24.dp, 24.dp, 8.dp, 24.dp))
                    .background(SurfaceContainerLowest),
            ) {
                Text(
                    text,
                    modifier = Modifier.padding(16.dp),
                    fontFamily = PublicSans,
                    fontSize = 15.sp,
                    color = OnSurface,
                    lineHeight = 22.sp,
                )
            }
            if (rtl) {
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(SecondaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = OnSecondaryContainer,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickRepliesBlock(replies: List<String>, onSelect: (String) -> Unit) {
    if (replies.isEmergencyQuickReplies()) {
        EmergencyQuickRepliesBlock(replies = replies, onSelect = onSelect)
    } else {
        StandardQuickRepliesBlock(replies = replies, onSelect = onSelect)
    }
}

/** سياق «إسعاف/طوارئ» — أزرار أوضح في نصف العرض */
private fun List<String>.isEmergencyQuickReplies(): Boolean {
    if (isEmpty()) return false
    val hasAmbulance = any { it.contains("إسعاف") || it.contains("الإسعاف") }
    val hasEr = any { it.contains("طوارئ") }
    return hasAmbulance && hasEr
}

@Composable
private fun EmergencyQuickRepliesBlock(replies: List<String>, onSelect: (String) -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            "اختر إجابة أو اكتب ما تريد",
            fontFamily = PublicSans,
            fontSize = 14.sp,
            color = OnSurfaceVariant,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 2.dp),
            textAlign = TextAlign.Center,
        )
        replies.forEach { r ->
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Button(
                    onClick = { onSelect(r) },
                    modifier = Modifier
                        .defaultMinSize(minHeight = 52.dp)
                        .fillMaxWidth(0.5f),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Primary,
                        contentColor = OnPrimary,
                    ),
                ) {
                    Text(
                        r,
                        fontFamily = PublicSans,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

@Composable
private fun StandardQuickRepliesBlock(replies: List<String>, onSelect: (String) -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            "اقتراحات سريعة",
            fontFamily = PublicSans,
            fontSize = 13.sp,
            color = OnSurfaceVariant,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 2.dp),
            textAlign = TextAlign.Start,
        )
        replies.forEach { r ->
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                FilledTonalButton(
                    onClick = { onSelect(r) },
                    modifier = Modifier
                        .defaultMinSize(minHeight = 48.dp)
                        .fillMaxWidth(0.92f),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Text(
                        r,
                        fontFamily = PublicSans,
                        fontSize = 14.sp,
                        color = Primary,
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatInputBar(
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean,
    onSend: () -> Unit,
) {
        Surface(
        color = SurfaceContainerLowest,
        shadowElevation = 6.dp,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    ) {
        val inputLen = value.length
        val maxInput = 4000
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 12.dp, vertical = 10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = { /* V1: voice placeholder */ },
                    enabled = false,
                ) {
                    Icon(Icons.Default.Mic, contentDescription = "ميكروفون", tint = Outline.copy(alpha = 0.4f))
                }
                OutlinedTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp, max = 120.dp),
                    enabled = enabled,
                    placeholder = {
                        Text(
                            "…اكتب سؤالك أو أعراضك هنا",
                            fontFamily = PublicSans,
                            color = Outline,
                        )
                    },
                    shape = RoundedCornerShape(28.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = SurfaceContainerLow,
                        unfocusedContainerColor = SurfaceContainerLow,
                        focusedBorderColor = Primary.copy(alpha = 0.4f),
                        unfocusedBorderColor = OutlineVariant.copy(alpha = 0.3f),
                    ),
                    maxLines = 4,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { onSend() }),
                )
                Spacer(Modifier.width(6.dp))
                FilledIconButton(
                    onClick = onSend,
                    enabled = enabled && value.isNotBlank(),
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "إرسال", tint = OnPrimary)
                }
            }
            if (inputLen > 3800) {
                Text(
                    text = "$inputLen / $maxInput",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 48.dp, top = 4.dp),
                    fontFamily = PublicSans,
                    fontSize = 12.sp,
                    color = if (inputLen > maxInput) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.secondary
                    },
                )
            }
        }
    }
}
