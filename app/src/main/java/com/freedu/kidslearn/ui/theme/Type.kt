package com.freedu.kidslearn.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.sp
import com.freedu.kidslearn.R

/**
 * Font families.
 *
 * ## Why script fonts are bundled
 * The platform's default (Roboto) has **no** Bengali or Arabic coverage. Relying
 * on the system fallback would render Bangla letters with whatever font the OEM
 * happens to ship - inconsistent between devices, and missing entirely on some
 * budget hardware. Noto Sans Bengali and Noto Naskh Arabic are bundled in
 * `res/font` (SIL Open Font License, see `assets/licenses/`) so the app renders
 * identically everywhere with no network and no downloadable fonts.
 *
 * Both bundled files are *variable* fonts. Compose exposes the weight axis on
 * API 26+; on API 24-25 the default instance (Regular) is used. Since the app
 * renders Bangla and Arabic at one or two weights only, the difference is not
 * perceptible and the alternative - shipping separate static cuts - would double
 * the font payload for no visible gain.
 */
val NotoSansBengali = FontFamily(
    Font(R.font.noto_sans_bengali, FontWeight.Normal),
    Font(R.font.noto_sans_bengali, FontWeight.Medium),
    Font(R.font.noto_sans_bengali, FontWeight.Bold),
)

val NotoNaskhArabic = FontFamily(
    Font(R.font.noto_naskh_arabic, FontWeight.Normal),
    Font(R.font.noto_naskh_arabic, FontWeight.Bold),
)

/**
 * Type scale.
 *
 * ## Sizing rules for a 3-8 audience
 * * The smallest style used for a *child-facing* label is [bodyLarge] at 18sp.
 *   Material's default [bodySmall] is 12sp, which is roughly the size of the print
 *   in a picture book - unreadable for most of this audience.
 * * `letterSpacing = 0` everywhere. This is a pre-literate, pre-dyslexic-friendly
 *   audience; tracked-out capitals measurably slow letter recognition.
 * * Line heights are generous (1.2-1.4x) because the scripts involved - Bangla
 *   matras above and below the line, Arabic diacritics - need vertical room or
 *   they clip.
 */
val KidsTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 72.sp,
        lineHeight = 80.sp,
        letterSpacing = 0.sp,
    ),
    displayMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 56.sp,
        lineHeight = 64.sp,
        letterSpacing = 0.sp,
    ),
    displaySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 44.sp,
        lineHeight = 52.sp,
        letterSpacing = 0.sp,
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 34.sp,
        lineHeight = 42.sp,
        letterSpacing = 0.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 30.sp,
        letterSpacing = 0.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 26.sp,
        letterSpacing = 0.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 18.sp,
        lineHeight = 26.sp,
        letterSpacing = 0.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        letterSpacing = 0.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp,
    ),
)

/**
 * Bangla text style.
 *
 * Bangla glyphs carry above- and below-line marks (matra, hasanta) and a
 * horizontal headline, so they need more line height than Latin text at the same
 * size or the marks get visually clipped by adjacent lines.
 */
val BanglaTextStyle = TextStyle(
    fontFamily = NotoSansBengali,
    fontWeight = FontWeight.Normal,
    fontSize = 22.sp,
    lineHeight = 34.sp,
    letterSpacing = 0.sp,
    lineHeightStyle = LineHeightStyle(
        alignment = LineHeightStyle.Alignment.Center,
        trim = LineHeightStyle.Trim.None,
    ),
)

/** Arabic is compact vertically; Naskh needs slightly tighter leading than Bangla. */
val ArabicTextStyle = TextStyle(
    fontFamily = NotoNaskhArabic,
    fontWeight = FontWeight.Normal,
    fontSize = 26.sp,
    lineHeight = 40.sp,
    letterSpacing = 0.sp,
)

/** For the very large letter on a lesson card, where the glyph *is* the content. */
val BigLetterStyle = TextStyle(
    fontFamily = FontFamily.Default,
    fontWeight = FontWeight.ExtraBold,
    fontSize = 96.sp,
    lineHeight = 104.sp,
    letterSpacing = 0.sp,
)
