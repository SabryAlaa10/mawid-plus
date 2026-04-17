@file:OptIn(ExperimentalMaterial3Api::class)

package com.mawidplus.patient.ui.screens.register

import android.widget.Toast
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mawidplus.patient.core.phone.EgyptPhone
import com.mawidplus.patient.ui.components.ErrorCard
import com.mawidplus.patient.ui.theme.Manrope
import com.mawidplus.patient.ui.theme.MawidTheme
import com.mawidplus.patient.ui.theme.OnPrimary
import com.mawidplus.patient.ui.theme.PublicSans

private val LoginBlue = Color(0xFF1A73E8)
private val LoginBlueDark = Color(0xFF0D47A1)
private val Slate900 = Color(0xFF0F172A)
private val Slate500 = Color(0xFF64748B)
private val Slate400 = Color(0xFF94A3B8)
private val InputTextBlack = Color(0xFF0A0A0A)
private val BorderLight = Color(0xFFE2E8F0)
private val GradientTop = Color(0xFFF0F7FF)

@Composable
fun RegisterScreen(
    preFilledPhoneLocalDigits: String,
    onRegisterSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit,
) {
    val vm: RegisterViewModel = viewModel(
        key = "register_${preFilledPhoneLocalDigits}",
        factory = RegisterViewModel.factory(preFilledPhoneLocalDigits),
    )
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    var phone by rememberSaveable { mutableStateOf(preFilledPhoneLocalDigits) }
    var fullName by rememberSaveable { mutableStateOf("") }
    val phoneLocked = preFilledPhoneLocalDigits.isNotBlank()
    val context = LocalContext.current

    LaunchedEffect(preFilledPhoneLocalDigits) {
        if (preFilledPhoneLocalDigits.isNotBlank()) phone = preFilledPhoneLocalDigits
    }

    val registerSuccess = uiState as? RegisterViewModel.UiState.Success
    LaunchedEffect(registerSuccess) {
        val s = registerSuccess ?: return@LaunchedEffect
        s.duplicateLoginMessage?.let { Toast.makeText(context, it, Toast.LENGTH_LONG).show() }
        onRegisterSuccess()
    }

    val nameValid = fullName.trim().length >= 3 && fullName.trim().none { it.isDigit() }
    val phoneValid = EgyptPhone.isValidLocal(EgyptPhone.digitsOnly(phone))
    val canSubmit = phoneValid && nameValid && uiState !is RegisterViewModel.UiState.Submitting

    MawidTheme(darkTheme = false) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(GradientTop, Color.White),
                            startY = 0f,
                            endY = 500f,
                        ),
                    )
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 16.dp),
            ) {
                IconButton(onClick = onNavigateToLogin) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع", tint = LoginBlue)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "إنشاء حساب جديد",
                    fontFamily = Manrope,
                    fontWeight = FontWeight.Bold,
                    fontSize = 26.sp,
                    color = Slate900,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "أدخل اسمك الكامل لإكمال التسجيل",
                    fontFamily = PublicSans,
                    fontSize = 14.sp,
                    color = Slate500,
                )
                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    "رقم الموبايل",
                    fontFamily = PublicSans,
                    fontSize = 12.sp,
                    color = Slate500,
                    modifier = Modifier.padding(bottom = 6.dp),
                )
                val phoneBg = if (phoneLocked) Color(0xFFF1F5F9) else Color.White
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .border(2.dp, BorderLight, RoundedCornerShape(16.dp))
                        .background(phoneBg, RoundedCornerShape(16.dp))
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Phone, contentDescription = null, tint = LoginBlue, modifier = Modifier.padding(end = 8.dp))
                        Text("+20 ", color = Slate500, fontSize = 16.sp)
                        if (phoneLocked) {
                            Text(phone, color = Slate900, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        } else {
                            BasicTextField(
                                value = phone,
                                onValueChange = { phone = EgyptPhone.digitsOnly(it) },
                                textStyle = TextStyle(
                                    color = Color.Black,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    fontFamily = FontFamily.Default,
                                ),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                cursorBrush = SolidColor(LoginBlue),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                decorationBox = { inner ->
                                    Box {
                                        if (phone.isEmpty()) {
                                            Text(EgyptPhone.PLACEHOLDER, color = Slate400, fontSize = 16.sp)
                                        }
                                        inner()
                                    }
                                },
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    singleLine = true,
                    label = { Text("الاسم الكامل", fontFamily = PublicSans) },
                    placeholder = { Text("الاسم الكامل", fontFamily = PublicSans) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    textStyle = TextStyle(
                        color = InputTextBlack,
                        fontSize = 16.sp,
                        fontFamily = PublicSans,
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LoginBlue,
                        unfocusedBorderColor = BorderLight,
                    ),
                )

                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { vm.submitRegister(phone, fullName) },
                    enabled = canSubmit,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LoginBlue,
                        disabledContainerColor = Color(0xFFE2E8F0),
                    ),
                ) {
                    if (uiState is RegisterViewModel.UiState.Submitting) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = OnPrimary,
                                strokeWidth = 2.dp,
                            )
                            Text(
                                "جارٍ إنشاء حسابك…",
                                fontFamily = Manrope,
                                fontWeight = FontWeight.Bold,
                                color = OnPrimary,
                                modifier = Modifier.padding(start = 12.dp),
                            )
                        }
                    } else {
                        Text(
                            "إنشاء الحساب",
                            fontFamily = Manrope,
                            fontWeight = FontWeight.Bold,
                            color = if (canSubmit) OnPrimary else Slate500,
                        )
                    }
                }

                if (uiState is RegisterViewModel.UiState.Submitting) {
                    Spacer(modifier = Modifier.height(12.dp))
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth().height(4.dp),
                        color = LoginBlue,
                        trackColor = BorderLight,
                    )
                    Text(
                        "جارٍ التحقق من الرقم…",
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        textAlign = TextAlign.Center,
                        fontFamily = PublicSans,
                        fontSize = 13.sp,
                        color = Slate500,
                    )
                }
            }

            when (val s = uiState) {
                is RegisterViewModel.UiState.Error -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding(),
                        contentAlignment = Alignment.BottomCenter,
                    ) {
                        ErrorCard(
                            message = s.message,
                            onRetry = { vm.clearError() },
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                        )
                    }
                }
                else -> {}
            }
        }
    }
}
