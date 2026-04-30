package com.mawidplus.patient.ui.screens.login

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mawidplus.patient.data.repository.AuthRepository

@Composable
fun LoginScreen(
    viewModel: LoginViewModel = viewModel { LoginViewModel(AuthRepository()) },
    onLoginSuccess: () -> Unit,
    onNavigateToRegister: () -> Unit
) {
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
        onAuthSuccess = onLoginSuccess
    )
}
