@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
package com.mawidplus.patient.ui.screens.login

import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.mawidplus.patient.ui.components.rememberCrossfadeImageRequest
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.mawidplus.patient.R
import com.mawidplus.patient.core.phone.EgyptPhone
import com.mawidplus.patient.ui.components.ErrorCard
import com.mawidplus.patient.ui.theme.Manrope
import com.mawidplus.patient.ui.theme.MawidTheme
import com.mawidplus.patient.ui.theme.OnPrimary
import com.mawidplus.patient.ui.theme.PublicSans
import kotlin.math.PI
import kotlin.math.sin

private const val TAG_FLOW = "AuthPhoneFlowScreen"
private val LoginBlue = Color(0xFF1A73E8)
private val LoginBlueDark = Color(0xFF0D47A1)
private val Slate900 = Color(0xFF0F172A)
private val Slate500 = Color(0xFF64748B)
private val Slate400 = Color(0xFF94A3B8)
/** نص الحقول: أسود/أزرق داكن لضمان الوضوح على كل الشاشات */
private val InputTextBlack = Color(0xFF0A0A0A)
private val BorderLight = Color(0xFFE2E8F0)
/** حد زر Google الرسمي (رمادي داكن — قريب من إرشادات العلامة) */
private val GoogleButtonBorder = Color(0xFF747775)
private val ErrorRed = Color(0xFFEF4444)
private val GradientTop = Color(0xFFF0F7FF)
private const val IMG_LOGIN_BANNER =
    "https://lh3.googleusercontent.com/aida-public/AB6AXuCOCO3TDq0iH_QGBjEonzLnajPNloPRjzOi0cQUiTA6lComZNzi54igI7_AfO4hBvws5MMMR9tAeVR8Sacfcm1xS3ci7uPUifkPZvuMTAB3XERp3D4xspZ4hworBnxhLc1-2nlGOagsXjL8WG2owXxfD5_YdXZY4v_YZb7V6AogrMQbefijcKTNlmM-qQQRFich9ikPBwSk-21PK3NoAUO5a6DXUMBtBk8J9mTjITFB7qzvuPj0g6kUVzcFRAaHRu6KNN0d1NRD7pY"

private fun friendlyErrorMessage(message: String): String {
    val m = message.lowercase()
    return when {
        m.contains("unable to resolve") || m.contains("failed to connect") ||
            m.contains("network") || m.contains("timeout") || m.contains("socket") ||
            m.contains("connection") || m.contains("unreachable") -> "تحقق من اتصالك بالإنترنت"
        else -> message
    }
}

private fun validatePhoneForUi(phoneDigits: String): String? {
    if (phoneDigits.isBlank()) return "أدخل رقم هاتفك أولاً"
    if (phoneDigits.length < 10) return "رقم الهاتف غير مكتمل"
    if (!EgyptPhone.isValidLocal(phoneDigits)) return "أدخل رقم مصري صحيح"
    return null
}

@Composable
private fun HeartbeatLine(modifier: Modifier, color: Color, progress: Float) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val midY = h / 2f
        val path = Path()
        val steps = 48
        path.moveTo(0f, midY)
        for (i in 1..steps) {
            val t = i / steps.toFloat()
            val x = w * t
            val base = sin(t * 6f * PI.toFloat() + progress * 2f * PI.toFloat()) * (h * 0.08f)
            val spike = when {
                t in 0.42f..0.48f -> -h * 0.35f
                t in 0.48f..0.52f -> h * 0.2f
                else -> 0f
            }
            val y = (midY + base + spike).coerceIn(0f, h)
            path.lineTo(x, y)
        }
        val measure = PathMeasure()
        measure.setPath(path, false)
        val len = measure.length
        val dest = Path()
        measure.getSegment(0f, len * progress.coerceIn(0f, 1f), dest, true)
        drawPath(path = dest, color = color, style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round))
    }
}

@Composable
fun AuthPhoneFlowScreen(
    viewModel: LoginViewModel,
    heroTitle: String,
    heroSubtitle: String,
    confirmLabel: String,
    confirmIcon: ImageVector,
    bottomPrompt: String,
    bottomActionLabel: String,
    onBottomLinkClick: () -> Unit,
    onTopBarBack: (() -> Unit)?,
    isRegisterFlow: Boolean,
    /** شاشة الدخول: التحقق من الرقم ثم تسجيل الدخول أو التوجيه للتسجيل */
    isLoginFlow: Boolean = false,
    onAuthSuccess: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var phone by rememberSaveable { mutableStateOf("") }
    var fullName by rememberSaveable { mutableStateOf("") }
    var phoneError by remember { mutableStateOf<String?>(null) }
    val isLoading = uiState is LoginViewModel.UiState.Submitting
    val loadingMessage = when {
        isLoginFlow && !isRegisterFlow && isLoading -> "جارٍ التحقق من الرقم…"
        isLoading -> "جارٍ التحقق…"
        else -> ""
    }
    val isAuthenticated = uiState is LoginViewModel.UiState.Authenticated
    var authNavigationFired by remember { mutableStateOf(false) }

    LaunchedEffect(phone) {
        if (phoneError != null) phoneError = null
    }

    LaunchedEffect(uiState) {
        when (uiState) {
            is LoginViewModel.UiState.Authenticated -> {
                if (!authNavigationFired) {
                    authNavigationFired = true
                    kotlinx.coroutines.delay(400)
                    try {
                        onAuthSuccess()
                    } catch (_: Throwable) {
                        authNavigationFired = false
                    }
                }
            }
            is LoginViewModel.UiState.Idle,
            is LoginViewModel.UiState.Error -> authNavigationFired = false
            else -> Unit
        }
    }

    val context = LocalContext.current
    val webClientId = stringResource(R.string.default_web_client_id)
    val gso = remember(webClientId) {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId)
            .requestEmail()
            .requestProfile()
            .build()
    }
    val googleClient = remember(context, webClientId) {
        GoogleSignIn.getClient(context, gso)
    }

    val googleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val idToken = account?.idToken
            if (idToken != null) {
                viewModel.signInWithGoogle(
                    phone,
                    idToken,
                    account.email,
                    account.displayName,
                    fullName.trim().takeIf { it.isNotEmpty() }
                )
            } else {
                viewModel.reportError("لم يُستلم رمز تعريف من Google. تحقق من إعدادات OAuth.")
            }
        } catch (e: ApiException) {
            if (e.statusCode != 12501) {
                viewModel.reportError("تعذر تسجيل الدخول بجوجل: ${e.message ?: e.statusCode.toString()}")
            }
        }
    }

    var logoVisible by remember { mutableStateOf(false) }
    var contentVisible by remember { mutableStateOf(false) }
    var buttonVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(100)
        logoVisible = true
        kotlinx.coroutines.delay(200)
        contentVisible = true
        kotlinx.coroutines.delay(300)
        buttonVisible = true
    }

    val infiniteTransition = rememberInfiniteTransition(label = "logo_pulse")
    val logoScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    val lineProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(2000, easing = LinearEasing)),
        label = "line"
    )

    val stats = remember {
        listOf(
            "أكثر من 2000 ممارس صحي معتمد",
            "احجز موعدك في أقل من دقيقة",
            "متابعة طابورك لحظة بلحظة"
        )
    }
    var currentStatIndex by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(3000)
            currentStatIndex = (currentStatIndex + 1) % stats.size
        }
    }

    val loginInteractionSource = remember { MutableInteractionSource() }
    val loginPressed by loginInteractionSource.collectIsPressedAsState()
    val loginBtnScale by animateFloatAsState(
        targetValue = if (loginPressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "login_scale"
    )
    val googleInteractionSource = remember { MutableInteractionSource() }
    val googlePressed by googleInteractionSource.collectIsPressedAsState()
    val googleScale by animateFloatAsState(
        targetValue = if (googlePressed) 0.98f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "google_scale"
    )
    val phoneDigits = phone
    val canSubmitPhone = phoneDigits.isNotBlank()
    fun runPhoneValidation(): Boolean {
        val err = validatePhoneForUi(phoneDigits)
        phoneError = err
        return err == null && EgyptPhone.isValidLocal(phoneDigits)
    }

    val scroll = rememberScrollState()

    // شاشة بيضاء: في الوضع الليلي onSurface فاتح فيظهر النص فاتحاً على خلفية بيضاء — نفرض الوضع الفاتح هنا
    MawidTheme(darkTheme = false) {
    Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
        Box(
            modifier = Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    colors = listOf(GradientTop, Color.White),
                    startY = 0f,
                    endY = 500f
                )
            )
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(scroll)
                .padding(bottom = 24.dp)
        ) {
            onTopBarBack?.let { back ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = back) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع", tint = LoginBlue)
                    }
                }
            }

            AnimatedVisibility(
                visible = logoVisible,
                enter = fadeIn(tween(300)) + slideInVertically(tween(300)) { -it / 2 },
                exit = fadeOut() + slideOutVertically()
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.scale(logoScale)) {
                        HeartbeatLine(
                            modifier = Modifier.size(120.dp, 40.dp).padding(bottom = 8.dp),
                            color = LoginBlue,
                            progress = lineProgress
                        )
                    }
                    Text(
                        "Mawid+",
                        fontFamily = Manrope,
                        fontWeight = FontWeight.Black,
                        fontSize = 32.sp,
                        color = LoginBlue
                    )
                    Text(
                        "صحتك أولويتنا",
                        fontFamily = PublicSans,
                        fontSize = 14.sp,
                        color = Slate500
                    )
                }
            }

            AnimatedVisibility(visible = contentVisible, enter = fadeIn(tween(400, delayMillis = 0))) {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
                    Text(
                        heroTitle,
                        fontFamily = Manrope,
                        fontWeight = FontWeight.Bold,
                        fontSize = 26.sp,
                        color = Slate900,
                        lineHeight = 34.sp,
                        textAlign = TextAlign.Start,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        heroSubtitle,
                        fontFamily = PublicSans,
                        fontSize = 14.sp,
                        color = Slate500,
                        textAlign = TextAlign.Start,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            AnimatedVisibility(
                visible = contentVisible,
                enter = fadeIn(tween(400, delayMillis = 100)) +
                    slideInVertically(tween(400, delayMillis = 100)) { it / 4 }
            ) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                    ) {
                        val phoneBorderColor = if (phoneError != null) ErrorRed else LoginBlue
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .border(
                                    width = 2.dp,
                                    color = phoneBorderColor,
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .background(Color.White, RoundedCornerShape(16.dp))
                                .padding(horizontal = 16.dp)
                                .semantics { contentDescription = "mawid_phone_field" },
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Phone,
                                    contentDescription = null,
                                    tint = LoginBlue,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "+20 ",
                                    color = Slate500,
                                    fontSize = 16.sp,
                                    fontFamily = FontFamily.Default
                                )
                                BasicTextField(
                                    value = phone,
                                    onValueChange = { phone = EgyptPhone.digitsOnly(it) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(56.dp),
                                    textStyle = TextStyle(
                                        color = Color.Black,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Medium,
                                        fontFamily = FontFamily.Default
                                    ),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    cursorBrush = SolidColor(LoginBlue),
                                    singleLine = true,
                                    decorationBox = { innerTextField ->
                                        Box(
                                            modifier = Modifier.fillMaxWidth(),
                                            contentAlignment = Alignment.CenterStart
                                        ) {
                                            if (phone.isEmpty()) {
                                                Text(
                                                    text = EgyptPhone.PLACEHOLDER,
                                                    color = Slate400,
                                                    fontSize = 16.sp,
                                                    fontFamily = FontFamily.Default
                                                )
                                            }
                                            innerTextField()
                                        }
                                    }
                                )
                            }
                        }
                        phoneError?.let { err ->
                            Text(
                                text = err,
                                color = Color(0xFFBA1A1A),
                                fontSize = 12.sp,
                                fontFamily = PublicSans,
                                modifier = Modifier.padding(start = 4.dp, top = 6.dp)
                            )
                        }
                    }
                }
            }

            if (isRegisterFlow) {
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    singleLine = true,
                    label = { Text("الاسم الكامل", fontFamily = PublicSans) },
                    placeholder = { Text("مثال: أحمد محمد", fontFamily = PublicSans) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .height(56.dp)
                        .semantics { contentDescription = "mawid_full_name_field" },
                    shape = RoundedCornerShape(16.dp),
                    textStyle = TextStyle(
                        color = InputTextBlack,
                        fontSize = 16.sp,
                        fontFamily = PublicSans
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LoginBlue,
                        unfocusedBorderColor = BorderLight,
                        focusedTextColor = InputTextBlack,
                        unfocusedTextColor = InputTextBlack,
                        disabledTextColor = InputTextBlack,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    )
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            AnimatedVisibility(
                visible = buttonVisible,
                enter = fadeIn(tween(400)) + slideInVertically(tween(400)) { it / 5 }
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
                    val enabledForClick = !isLoading && canSubmitPhone &&
                        (!isRegisterFlow || fullName.isNotBlank())
                    Box(modifier = Modifier.fillMaxWidth().scale(loginBtnScale)) {
                        Button(
                            onClick = {
                                if (!runPhoneValidation()) {
                                    Log.d(TAG_FLOW, "phone validation failed: $phoneError")
                                    return@Button
                                }
                                if (isRegisterFlow && fullName.isBlank()) {
                                    viewModel.reportError("أدخل الاسم الكامل")
                                    return@Button
                                }
                                when {
                                    isRegisterFlow -> viewModel.submitPhoneAuth(phone, fullName, true)
                                    isLoginFlow -> viewModel.submitPhoneLogin(phone)
                                    else -> viewModel.submitPhoneAuth(phone, fullName, false)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .semantics {
                                    contentDescription = if (isRegisterFlow) "mawid_submit_register" else "mawid_submit_login"
                                },
                            enabled = enabledForClick,
                            interactionSource = loginInteractionSource,
                            shape = RoundedCornerShape(28.dp),
                            contentPadding = PaddingValues(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Transparent,
                                disabledContainerColor = Color(0xFFE2E8F0)
                            )
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .then(
                                        if (enabledForClick) {
                                            Modifier.background(
                                                Brush.horizontalGradient(listOf(LoginBlue, LoginBlueDark)),
                                                RoundedCornerShape(28.dp)
                                            )
                                        } else {
                                            Modifier.background(Color(0xFFE2E8F0), RoundedCornerShape(28.dp))
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        confirmLabel,
                                        fontFamily = Manrope,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = if (enabledForClick) OnPrimary else Slate500
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Icon(
                                        confirmIcon,
                                        contentDescription = null,
                                        tint = if (enabledForClick) OnPrimary else Slate500
                                    )
                                }
                            }
                        }
                    }

                    if (isLoading) {
                        Spacer(modifier = Modifier.height(12.dp))
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                            color = LoginBlue,
                            trackColor = BorderLight
                        )
                        Text(
                            loadingMessage.ifBlank { "جارٍ التحقق…" },
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            textAlign = TextAlign.Center,
                            fontFamily = PublicSans,
                            fontSize = 13.sp,
                            color = Slate500
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.weight(1f).height(1.dp).background(BorderLight))
                        Text("أو", modifier = Modifier.padding(horizontal = 16.dp), color = Slate500, fontSize = 13.sp, fontFamily = PublicSans)
                        Box(modifier = Modifier.weight(1f).height(1.dp).background(BorderLight))
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    val googlePillHeight = 52.dp
                    val googlePillShape = RoundedCornerShape(googlePillHeight / 2)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(googlePillHeight)
                            .scale(googleScale)
                            .clip(googlePillShape)
                            .border(BorderStroke(1.dp, GoogleButtonBorder), googlePillShape)
                            .background(Color.White, googlePillShape)
                            .clickable(
                                interactionSource = googleInteractionSource,
                                indication = null,
                                enabled = !isLoading,
                                onClick = {
                                    if (!runPhoneValidation()) return@clickable
                                    if (isRegisterFlow && fullName.isBlank()) {
                                        viewModel.reportError("أدخل الاسم الكامل أولاً لمتابعة التسجيل بجوجل")
                                        return@clickable
                                    }
                                    if (webClientId.contains("REPLACE", ignoreCase = true)) {
                                        viewModel.reportError(
                                            "أضف default_web_client_id (Web Client ID من Google Cloud) في strings.xml"
                                        )
                                    } else {
                                        googleLauncher.launch(googleClient.signInIntent)
                                    }
                                },
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                            Row(
                                modifier = Modifier.padding(horizontal = 22.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                            ) {
                                Image(
                                    painter = painterResource(R.drawable.ic_google_g),
                                    contentDescription = "Google",
                                    modifier = Modifier.size(24.dp),
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    "تسجيل الدخول بواسطة Google",
                                    fontFamily = PublicSans,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 15.sp,
                                    color = Slate900,
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(bottomPrompt, fontFamily = PublicSans, color = Slate500, fontSize = 14.sp)
                        Text(
                            bottomActionLabel,
                            fontFamily = PublicSans,
                            color = LoginBlue,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            textDecoration = TextDecoration.Underline,
                            modifier = Modifier
                                .clickable(
                                    role = Role.Button,
                                    onClickLabel = if (isRegisterFlow) "login" else "register",
                                    onClick = onBottomLinkClick
                                )
                                .semantics {
                                    contentDescription = if (isRegisterFlow) "mawid_nav_login" else "mawid_nav_register"
                                }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .height(200.dp)
            ) {
                val loginBannerReq = rememberCrossfadeImageRequest(IMG_LOGIN_BANNER)
                AsyncImage(
                    model = loginBannerReq ?: IMG_LOGIN_BANNER,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                // تدرج أبيض خفيف من الأسفل فقط لقراءة النص دون إخفاء الصورة
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(100.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.92f))
                            )
                        )
                )
                Column(
                    modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(20.dp)
                ) {
                    Text(
                        "لماذا موعد+؟",
                        fontFamily = PublicSans,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        color = Slate900
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    AnimatedContent(
                        targetState = currentStatIndex,
                        transitionSpec = { fadeIn(tween(500)) togetherWith fadeOut(tween(500)) },
                        label = "stats"
                    ) { index ->
                        Text(
                            stats[index],
                            fontFamily = Manrope,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Slate900,
                            lineHeight = 20.sp
                        )
                    }
                }
            }
        }

        if (isAuthenticated) {
            Box(modifier = Modifier.fillMaxSize().background(Color(0xFF2E7D32).copy(alpha = 0.22f)))
        }

        when (val state = uiState) {
            is LoginViewModel.UiState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize().statusBarsPadding(),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    ErrorCard(
                        message = friendlyErrorMessage(state.message),
                        onRetry = { viewModel.clearError() },
                        modifier = Modifier.fillMaxWidth().padding(16.dp)
                    )
                }
            }
            else -> {}
        }
    }
    }
}
