package com.mawidplus.patient.ui.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.mawidplus.patient.R

/**
 * خطوط متغيرة (Google Fonts OFL): Manrope للعناوين، Public Sans للنصوص.
 */
val Manrope: FontFamily = FontFamily(
    Font(R.font.manrope_vf, FontWeight.Light),
    Font(R.font.manrope_vf, FontWeight.Normal),
    Font(R.font.manrope_vf, FontWeight.Medium),
    Font(R.font.manrope_vf, FontWeight.SemiBold),
    Font(R.font.manrope_vf, FontWeight.Bold),
    Font(R.font.manrope_vf, FontWeight.ExtraBold),
    Font(R.font.manrope_vf, FontWeight.Black)
)

val PublicSans: FontFamily = FontFamily(
    Font(R.font.public_sans_vf, FontWeight.Light),
    Font(R.font.public_sans_vf, FontWeight.Normal),
    Font(R.font.public_sans_vf, FontWeight.Medium),
    Font(R.font.public_sans_vf, FontWeight.SemiBold),
    Font(R.font.public_sans_vf, FontWeight.Bold)
)
