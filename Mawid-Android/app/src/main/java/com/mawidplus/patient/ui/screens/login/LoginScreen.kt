package com.mawidplus.patient.ui.screens.login

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mawidplus.patient.data.repository.AuthRepository

@Composable
fun LoginScreen(
    viewModel: LoginViewModel = viewModel { LoginViewModel(AuthRepository()) },
    onLoginSuccess: () -> Unit,
    /** إنشاء حساب: بدون رقم مسبق من شاشة الدخول */
    onNavigateToRegister: () -> Unit,
    /** بعد التحقق: الرقم غير مسجل — التسجيل مع رقم مُدخل مسبقاً */
    onNavigateToRegisterWithPhone: (phoneLocalDigits: String) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val pendingPhone by viewModel.pendingPhoneLocalDigits.collectAsStateWithLifecycle()

    LaunchedEffect(uiState, pendingPhone) {
        if (uiState is LoginViewModel.UiState.PhoneNotFound && !pendingPhone.isNullOrBlank()) {
            onNavigateToRegisterWithPhone(pendingPhone!!)
            viewModel.clearPhoneNotFound()
        }
    }

    AuthPhoneFlowScreen(
        viewModel = viewModel,
        heroTitle = "مرحباً بك 👋",
        heroSubtitle = "أدخل رقم هاتفك للمتابعة",
        confirmLabel = "تسجيل الدخول",
        confirmIcon = Icons.AutoMirrored.Filled.Login,
        bottomPrompt = "ليس لديك حساب؟ ",
        bottomActionLabel = "إنشاء حساب جديد",
        onBottomLinkClick = onNavigateToRegister,
        onTopBarBack = null,
        isRegisterFlow = false,
        isLoginFlow = true,
        onAuthSuccess = onLoginSuccess
    )
}
