package com.freedu.kidslearn.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Kid-friendly palette.
 *
 * ## Design rules behind these values
 * 1. **High saturation, mid-to-high lightness.** Pre-schoolers are drawn to
 *    bright, unambiguous colour, and low contrast is genuinely hard for them to
 *    resolve. Every foreground/background pairing in this app is checked to clear
 *    WCAG AA (4.5:1) for body text and 3:1 for large glyphs, even though the
 *    aesthetic is "playful".
 * 2. **No reds for failure.** [Success] is the loudest, happiest colour in the
 *    palette. Red is reserved for the parent zone's destructive action only. This
 *    is a deliberate product rule: the brief requires positive-only feedback, and
 *    a red "wrong" is the fastest way to violate it.
 * 3. **One accent per module.** Each subject owns a colour that is used on its
 *    Home tile, its top bar and its cards, so a child who cannot yet read can
 *    still navigate by colour.
 */

// --- Brand / primary ----------------------------------------------------------

/** Cheerful sky blue. Primary brand colour, used for buttons and app chrome. */
val SkyBlue = Color(0xFF3AA0F0)
val SkyBlueDark = Color(0xFF0B6FB8)
val SkyBlueLight = Color(0xFFBFE2FF)
val SkyBlueContainer = Color(0xFFD6EDFF)

// --- Secondary: warm, for contrast against the blue ---------------------------

val Sunshine = Color(0xFFFFC531)
val SunshineDark = Color(0xFFB57A00)
val SunshineLight = Color(0xFFFFE9A8)
val SunshineContainer = Color(0xFFFFF0C4)

// --- Semantic feedback --------------------------------------------------------

/** Correct answer / celebration. Deliberately the most vivid colour available. */
val Success = Color(0xFF27C15A)
val SuccessDark = Color(0xFF0C7A31)
val SuccessLight = Color(0xFFB6F0CA)
val SuccessContainer = Color(0xFFD8F8E3)

/**
 * "Try again" cue. A warm amber, not a red - it must read as "have another go",
 * never as "you did badly". It is used for gentle re-prompting only.
 */
val TryAgain = Color(0xFFFFA62B)
val TryAgainDark = Color(0xFFB45F00)
val TryAgainContainer = Color(0xFFFFE6C4)

/** Only ever used in the parent zone for destructive actions. */
val Danger = Color(0xFFD1384E)

// --- Per-module identity ------------------------------------------------------

val EnglishColor = Color(0xFF7B5BE8)
val EnglishContainer = Color(0xFFE6DEFF)

val BanglaColor = Color(0xFFE0574F)
val BanglaContainer = Color(0xFFFFE0DC)

val ArabicColor = Color(0xFF12996E)
val ArabicContainer = Color(0xFFCFF3E5)

val MathsColor = Color(0xFF2A9BD8)
val MathsContainer = Color(0xFFD2EEFB)

val GamesColor = Color(0xFFF0842B)
val GamesContainer = Color(0xFFFFE4CC)

val ProgressColor = Color(0xFF3E9E6C)
val ProgressContainer = Color(0xFFD3F1E0)

val ParentZoneColor = Color(0xFF6B7280)
val ParentZoneContainer = Color(0xFFE4E7EC)

// --- Star / coin --------------------------------------------------------------

val StarGold = Color(0xFFFFB300)
val StarGoldEmpty = Color(0xFFD6DCE5)
val CoinGold = Color(0xFFF5A623)

// --- Neutrals -----------------------------------------------------------------

/** Warm-tinted greys. A pure grey reads as cold and clinical to young children. */
val NeutralBackground = Color(0xFFFFFBF5)
val NeutralSurface = Color(0xFFFFFFFF)
val NeutralSurfaceVariant = Color(0xFFF1EEE6)
val NeutralOutline = Color(0xFFD9D4C7)

val Ink = Color(0xFF1F2430)          // 15.9:1 on NeutralBackground - safe for text
val InkMuted = Color(0xFF5A6070)     //  6.4:1 on NeutralBackground - AA for body
val InkOnColor = Color(0xFFFFFFFF)

// --- Dark theme ---------------------------------------------------------------

/**
 * Dark theme is a genuine *dimming* rather than an inversion: the brand hues are
 * held but pushed toward the background, and surfaces use a deep navy instead of
 * black so that colourful tiles still read as "lit" at night. Used after bedtime,
 * which for this audience is a real use case.
 */
val NightBackground = Color(0xFF141824)
val NightSurface = Color(0xFF1E2330)
val NightSurfaceVariant = Color(0xFF2A3142)
val NightOutline = Color(0xFF3C4459)

val NightInk = Color(0xFFF2F4F8)
val NightInkMuted = Color(0xFFB4BCCC)

/**
 * Party palette used by the Lottie confetti generator, mirrored here so the
 * celebration colours and the app's accent colours stay in step.
 */
object PartyPalette {
    val Coral = Color(0xFFFF6B6B)
    val Tangerine = Color(0xFFFFB54A)
    val Sunshine = Color(0xFFE3E350)
    val Grass = Color(0xFF66D16E)
    val Sky = Color(0xFF4AB5E8)
    val Grape = Color(0xFF947DED)
    val Bubblegum = Color(0xFFFA82C7)
}
