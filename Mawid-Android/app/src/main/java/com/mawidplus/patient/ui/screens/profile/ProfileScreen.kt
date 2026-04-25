@file:OptIn(ExperimentalMaterial3Api::class)

package com.mawidplus.patient.ui.screens.profile

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mawidplus.patient.core.region.MawidRegion
import com.mawidplus.patient.ui.components.initialsFromDisplayName
import com.mawidplus.patient.ui.theme.Error
import com.mawidplus.patient.ui.theme.Manrope
import com.mawidplus.patient.ui.theme.OnPrimary
import com.mawidplus.patient.ui.theme.OnSurface
import com.mawidplus.patient.ui.theme.OnSurfaceVariant
import com.mawidplus.patient.ui.theme.OutlineVariant
import com.mawidplus.patient.ui.theme.Primary
import com.mawidplus.patient.ui.theme.PublicSans
import com.mawidplus.patient.ui.theme.Surface
import com.mawidplus.patient.ui.theme.SurfaceBright
import com.mawidplus.patient.ui.theme.SurfaceContainerLow
import com.mawidplus.patient.ui.theme.SurfaceContainerLowest
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.format.DateTimeFormatter

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel = viewModel { ProfileViewModel() },
    onNavigateToNotifications: () -> Unit = {},
    onNavigateToAppointments: () -> Unit = {},
    onNavigateToSearch: () -> Unit = {},
    onNavigateToQueue: () -> Unit = {},
    onNavigateToSearchFilters: () -> Unit = {},
    onSignOut: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var snackbarIsError by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val scheme = MaterialTheme.colorScheme

    Scaffold(
        containerColor = Surface,
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = if (snackbarIsError) {
                        scheme.errorContainer
                    } else {
                        scheme.primaryContainer
                    },
                    contentColor = if (snackbarIsError) {
                        scheme.onErrorContainer
                    } else {
                        scheme.onPrimaryContainer
                    },
                    shape = RoundedCornerShape(14.dp),
                )
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "حسابي",
                            fontFamily = Manrope,
                            fontWeight = FontWeight.Bold,
                            color = Primary,
                        )
                        Text(
                            "معلوماتك واختصارات التطبيق",
                            fontFamily = PublicSans,
                            fontSize = 11.sp,
                            color = OnSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                },
                actions = {
                    if (uiState is ProfileUiState.Ready) {
                        IconButton(
                            onClick = { viewModel.toggleEditMode() },
                            enabled = !(uiState as ProfileUiState.Ready).isSaving,
                        ) {
                            Icon(
                                Icons.Filled.Edit,
                                contentDescription = "تعديل الملف",
                                tint = Primary,
                            )
                        }
                    }
                    IconButton(onClick = onNavigateToNotifications) {
                        Icon(
                            Icons.Filled.Notifications,
                            contentDescription = "إشعارات",
                            tint = OnSurfaceVariant,
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = SurfaceBright.copy(alpha = 0.92f),
                ),
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 28.dp),
            ) {
                Spacer(modifier = Modifier.height(12.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = SurfaceContainerLowest,
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        when (val s = uiState) {
                            is ProfileUiState.Loading -> {
                                CircularProgressIndicator(
                                    color = Primary,
                                    modifier = Modifier.padding(12.dp),
                                    strokeWidth = 2.dp,
                                )
                                Text(
                                    "جاري تحميل بياناتك…",
                                    fontFamily = PublicSans,
                                    fontSize = 14.sp,
                                    color = OnSurfaceVariant,
                                )
                            }
                            is ProfileUiState.NotSignedIn -> {
                                Text(
                                    "سجّل الدخول لعرض بيانات حسابك وإدارة ملفك.",
                                    fontFamily = PublicSans,
                                    fontSize = 14.sp,
                                    color = OnSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 22.sp,
                                )
                            }
                            is ProfileUiState.Error -> {
                                Text(
                                    s.message,
                                    fontFamily = PublicSans,
                                    fontSize = 14.sp,
                                    color = Error,
                                    textAlign = TextAlign.Center,
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                TextButton(onClick = { viewModel.refresh() }) {
                                    Text("إعادة المحاولة", fontFamily = Manrope, color = Primary)
                                }
                            }
                            is ProfileUiState.Ready -> {
                                val nameForInitials = if (s.isEditMode) s.editedName else s.profile.fullName
                                Box(
                                    modifier = Modifier
                                        .size(92.dp)
                                        .clip(CircleShape)
                                        .background(Primary.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        initialsFromDisplayName(nameForInitials.ifBlank { "؟" }),
                                        fontFamily = Manrope,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 30.sp,
                                        color = Primary,
                                    )
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                if (s.isEditMode) {
                                    Text(
                                        "تعديل الاسم الظاهر في التطبيق",
                                        fontFamily = PublicSans,
                                        fontSize = 13.sp,
                                        color = OnSurfaceVariant,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    OutlinedTextField(
                                        value = s.editedName,
                                        onValueChange = viewModel::updateEditedName,
                                        label = { Text("الاسم الكامل", fontFamily = PublicSans) },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = Primary,
                                            unfocusedBorderColor = OutlineVariant,
                                            focusedTextColor = OnSurface,
                                            unfocusedTextColor = OnSurface,
                                            focusedContainerColor = SurfaceContainerLow,
                                            unfocusedContainerColor = SurfaceContainerLow,
                                        ),
                                        textStyle = TextStyle(
                                            fontFamily = PublicSans,
                                            textAlign = TextAlign.Start,
                                            color = Color(0xFF0A0A0A),
                                            fontSize = 16.sp,
                                        ),
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    OutlinedTextField(
                                        value = s.profile.phone,
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("رقم الهاتف", fontFamily = PublicSans) },
                                        supportingText = {
                                            Text(
                                                "مربوط بحسابك — للتغيير تواصل مع الدعم",
                                                fontFamily = PublicSans,
                                                fontSize = 11.sp,
                                            )
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = OutlineVariant,
                                            unfocusedBorderColor = OutlineVariant,
                                            focusedTextColor = OnSurface,
                                            unfocusedTextColor = OnSurface,
                                            disabledTextColor = OnSurface,
                                            focusedContainerColor = SurfaceContainerLow,
                                            unfocusedContainerColor = SurfaceContainerLow,
                                        ),
                                        textStyle = TextStyle(
                                            fontFamily = PublicSans,
                                            textAlign = TextAlign.Start,
                                            color = Color(0xFF0A0A0A),
                                            fontSize = 16.sp,
                                        ),
                                    )
                                    Spacer(modifier = Modifier.height(18.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    ) {
                                        Button(
                                            onClick = {
                                                viewModel.saveProfile { msg, isErr ->
                                                    snackbarIsError = isErr
                                                    scope.launch {
                                                        snackbarHostState.showSnackbar(
                                                            message = msg,
                                                            duration = if (isErr) {
                                                                SnackbarDuration.Long
                                                            } else {
                                                                SnackbarDuration.Short
                                                            },
                                                            withDismissAction = true,
                                                        )
                                                    }
                                                }
                                            },
                                            modifier = Modifier.weight(1f),
                                            enabled = !s.isSaving,
                                            shape = RoundedCornerShape(14.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Primary,
                                                contentColor = OnPrimary,
                                            ),
                                        ) {
                                            if (s.isSaving) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(20.dp),
                                                    color = OnPrimary,
                                                    strokeWidth = 2.dp,
                                                )
                                            } else {
                                                Text("حفظ", fontFamily = Manrope, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                        OutlinedButton(
                                            onClick = { viewModel.cancelEdit() },
                                            modifier = Modifier.weight(1f),
                                            enabled = !s.isSaving,
                                            shape = RoundedCornerShape(14.dp),
                                        ) {
                                            Text("إلغاء", fontFamily = PublicSans, color = Primary)
                                        }
                                    }
                                } else {
                                    Text(
                                        s.profile.fullName.ifBlank { "مستخدم موعد+" },
                                        fontFamily = Manrope,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 22.sp,
                                        color = OnSurface,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                    if (s.profile.phone.isNotBlank()) {
                                        Text(
                                            s.profile.phone,
                                            fontFamily = PublicSans,
                                            fontSize = 14.sp,
                                            color = OnSurfaceVariant,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 8.dp),
                                        )
                                    }
                                    formatRegistrationLabel(s.profile.createdAt)?.let { reg ->
                                        Text(
                                            reg,
                                            fontFamily = PublicSans,
                                            fontSize = 12.sp,
                                            color = OnSurfaceVariant.copy(alpha = 0.9f),
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.padding(top = 10.dp),
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        "اضغط أيقونة القلم أعلاه لتعديل الاسم.",
                                        fontFamily = PublicSans,
                                        fontSize = 12.sp,
                                        color = OnSurfaceVariant,
                                        textAlign = TextAlign.Center,
                                        lineHeight = 18.sp,
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(22.dp))

                Text(
                    "اختصارات",
                    fontFamily = Manrope,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = OnSurface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 2.dp)
                        .padding(bottom = 10.dp),
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceContainerLowest),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        ProfileMenuRow(
                            icon = Icons.Filled.CalendarToday,
                            title = "مواعيدي",
                            onClick = onNavigateToAppointments,
                        )
                        HorizontalDivider(color = OutlineVariant.copy(alpha = 0.35f))
                        ProfileMenuRow(
                            icon = Icons.Filled.Schedule,
                            title = "طابوري الحالي",
                            onClick = onNavigateToQueue,
                        )
                        HorizontalDivider(color = OutlineVariant.copy(alpha = 0.35f))
                        ProfileMenuRow(
                            icon = Icons.Filled.Search,
                            title = "البحث عن طبيب",
                            onClick = onNavigateToSearch,
                        )
                        HorizontalDivider(color = OutlineVariant.copy(alpha = 0.35f))
                        ProfileMenuRow(
                            icon = Icons.Filled.Notifications,
                            title = "الإشعارات",
                            onClick = onNavigateToNotifications,
                        )
                        HorizontalDivider(color = OutlineVariant.copy(alpha = 0.35f))
                        ProfileMenuRow(
                            icon = Icons.Filled.Settings,
                            title = "تفضيلات البحث",
                            onClick = onNavigateToSearchFilters,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(22.dp))

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onSignOut),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Error.copy(alpha = 0.06f),
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp, horizontal = 18.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "تسجيل الخروج",
                            fontFamily = Manrope,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Error,
                        )
                    }
                }
            }
        }
    }
}

private fun formatRegistrationLabel(createdAt: String?): String? {
    if (createdAt.isNullOrBlank()) return null
    return runCatching {
        val instant = Instant.parse(createdAt)
        val z = instant.atZone(MawidRegion.timeZone)
        val fmt = DateTimeFormatter.ofPattern("d MMMM yyyy", MawidRegion.arabicLocale)
        "عضو منذ ${z.format(fmt)}"
    }.getOrNull()
}

@Composable
private fun ProfileMenuRow(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = Primary,
                    modifier = Modifier.size(22.dp),
                )
            }
            Text(
                title,
                fontFamily = PublicSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                color = OnSurface,
                textAlign = TextAlign.Start,
            )
        }
        Icon(
            Icons.Filled.KeyboardArrowLeft,
            contentDescription = null,
            tint = OnSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(22.dp),
        )
    }
}
