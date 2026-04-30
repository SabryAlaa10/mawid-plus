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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel = viewModel { ProfileViewModel() },
    onNavigateToNotifications: () -> Unit = {},
    onNavigateToAppointments: () -> Unit = {},
    onNavigateToSearch: () -> Unit = {},
    onNavigateToQueue: () -> Unit = {},
    onNavigateToSearchFilters: () -> Unit = {},
    onSignOut: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        containerColor = Surface,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "حسابي",
                        fontFamily = Manrope,
                        fontWeight = FontWeight.Bold,
                        color = Primary
                    )
                },
                actions = {
                    if (uiState is ProfileUiState.Ready) {
                        IconButton(
                            onClick = { viewModel.toggleEditMode() },
                            enabled = !(uiState as ProfileUiState.Ready).isSaving
                        ) {
                            Icon(
                                Icons.Filled.Edit,
                                contentDescription = "تعديل",
                                tint = Primary
                            )
                        }
                    }
                    IconButton(onClick = onNavigateToNotifications) {
                        Icon(
                            Icons.Filled.Notifications,
                            contentDescription = "إشعارات",
                            tint = OnSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = SurfaceBright.copy(alpha = 0.92f)
                )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 24.dp)
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(SurfaceContainerLowest)
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    when (val s = uiState) {
                        is ProfileUiState.Loading -> {
                            CircularProgressIndicator(color = Primary, modifier = Modifier.padding(8.dp))
                            Text(
                                "جاري التحميل…",
                                fontFamily = PublicSans,
                                fontSize = 14.sp,
                                color = OnSurfaceVariant,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                        is ProfileUiState.NotSignedIn -> {
                            Text(
                                "سجّل الدخول لعرض بيانات حسابك.",
                                fontFamily = PublicSans,
                                fontSize = 14.sp,
                                color = OnSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                        is ProfileUiState.Error -> {
                            Text(
                                s.message,
                                fontFamily = PublicSans,
                                fontSize = 14.sp,
                                color = Error,
                                textAlign = TextAlign.Center
                            )
                        }
                        is ProfileUiState.Ready -> {
                            val nameForInitials = if (s.isEditMode) s.editedName else s.profile.fullName
                            Box(
                                modifier = Modifier
                                    .size(88.dp)
                                    .clip(CircleShape)
                                    .background(Primary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    initialsFromDisplayName(nameForInitials.ifBlank { "؟" }),
                                    fontFamily = Manrope,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 28.sp,
                                    color = Primary
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            if (s.isEditMode) {
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
                                        focusedContainerColor = SurfaceContainerLowest,
                                        unfocusedContainerColor = SurfaceContainerLowest,
                                    ),
                                    textStyle = TextStyle(
                                        fontFamily = PublicSans,
                                        textAlign = TextAlign.Start,
                                        color = Color(0xFF0A0A0A),
                                        fontSize = 16.sp
                                    )
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = s.profile.phone,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("رقم الهاتف", fontFamily = PublicSans) },
                                    supportingText = {
                                        Text(
                                            "لا يمكن تغيير رقم الهاتف من هنا",
                                            fontFamily = PublicSans,
                                            fontSize = 11.sp
                                        )
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = OutlineVariant,
                                        unfocusedBorderColor = OutlineVariant,
                                        focusedTextColor = OnSurface,
                                        unfocusedTextColor = OnSurface,
                                        disabledTextColor = OnSurface,
                                        focusedContainerColor = SurfaceContainerLowest,
                                        unfocusedContainerColor = SurfaceContainerLowest,
                                    ),
                                    textStyle = TextStyle(
                                        fontFamily = PublicSans,
                                        textAlign = TextAlign.Start,
                                        color = Color(0xFF0A0A0A),
                                        fontSize = 16.sp
                                    ),
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            viewModel.saveProfile { msg, isErr ->
                                                scope.launch {
                                                    snackbarHostState.showSnackbar(msg)
                                                }
                                            }
                                        },
                                        modifier = Modifier.weight(1f),
                                        enabled = !s.isSaving,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Primary,
                                            contentColor = OnPrimary
                                        )
                                    ) {
                                        if (s.isSaving) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(20.dp),
                                                color = OnPrimary,
                                                strokeWidth = 2.dp
                                            )
                                        } else {
                                            Text("حفظ التغييرات", fontFamily = Manrope)
                                        }
                                    }
                                    OutlinedButton(
                                        onClick = { viewModel.cancelEdit() },
                                        modifier = Modifier.weight(1f),
                                        enabled = !s.isSaving
                                    ) {
                                        Text("إلغاء", fontFamily = PublicSans, color = Primary)
                                    }
                                }
                            } else {
                                Text(
                                    s.profile.fullName.ifBlank { "مستخدم موعد+" },
                                    fontFamily = Manrope,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp,
                                    color = OnSurface,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
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
                                            .padding(top = 8.dp)
                                    )
                                }
                                formatRegistrationLabel(s.profile.createdAt)?.let { reg ->
                                    Text(
                                        reg,
                                        fontFamily = PublicSans,
                                        fontSize = 12.sp,
                                        color = OnSurfaceVariant,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(top = 8.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                TextButton(onClick = { viewModel.toggleEditMode() }) {
                                    Text("تعديل المعلومات", fontFamily = Manrope, color = Primary)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    "اختصارات",
                    fontFamily = Manrope,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = OnSurfaceVariant,
                    textAlign = TextAlign.Start,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 8.dp)
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(SurfaceContainerLowest)
                ) {
                    ProfileMenuRow(
                        icon = Icons.Filled.CalendarToday,
                        title = "مواعيدي",
                        onClick = onNavigateToAppointments
                    )
                    Divider(color = OutlineVariant.copy(alpha = 0.35f))
                    ProfileMenuRow(
                        icon = Icons.Filled.Schedule,
                        title = "طابوري الحالي",
                        onClick = onNavigateToQueue
                    )
                    Divider(color = OutlineVariant.copy(alpha = 0.35f))
                    ProfileMenuRow(
                        icon = Icons.Filled.Search,
                        title = "البحث عن طبيب",
                        onClick = onNavigateToSearch
                    )
                    Divider(color = OutlineVariant.copy(alpha = 0.35f))
                    ProfileMenuRow(
                        icon = Icons.Filled.Notifications,
                        title = "الإشعارات",
                        onClick = onNavigateToNotifications
                    )
                    Divider(color = OutlineVariant.copy(alpha = 0.35f))
                    ProfileMenuRow(
                        icon = Icons.Filled.Settings,
                        title = "تفضيلات البحث",
                        onClick = onNavigateToSearchFilters
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Error.copy(alpha = 0.08f))
                        .clickable(onClick = onSignOut)
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "تسجيل الخروج",
                        fontFamily = Manrope,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Error
                    )
                }
            }
        }
    }
}

private fun formatRegistrationLabel(createdAt: String?): String? {
    if (createdAt.isNullOrBlank()) return null
    return runCatching {
        val instant = Instant.parse(createdAt)
        val z = instant.atZone(ZoneId.of("Asia/Riyadh"))
        val fmt = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale("ar", "SA"))
        "تاريخ التسجيل: ${z.format(fmt)}"
    }.getOrNull()
}

@Composable
private fun ProfileMenuRow(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = Primary,
                modifier = Modifier.size(22.dp)
            )
            Text(
                title,
                fontFamily = PublicSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                color = OnSurface,
                textAlign = TextAlign.Start
            )
        }
        Icon(
            Icons.Filled.ArrowBack,
            contentDescription = null,
            tint = OnSurfaceVariant.copy(alpha = 0.45f),
            modifier = Modifier.size(18.dp)
        )
    }
}
