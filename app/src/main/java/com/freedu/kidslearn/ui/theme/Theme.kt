package com.freedu.kidslearn.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.view.WindowCompat
import com.freedu.kidslearn.domain.model.ModuleType

private val LightColors = lightColorScheme(
    primary = SkyBlue,
    onPrimary = InkOnColor,
    primaryContainer = SkyBlueContainer,
    onPrimaryContainer = SkyBlueDark,
    secondary = Sunshine,
    onSecondary = InkOnColor,
    secondaryContainer = SunshineContainer,
    onSecondaryContainer = SunshineDark,
    tertiary = PartyPalette.Grape,
    onTertiary = InkOnColor,
    background = NeutralBackground,
    onBackground = Ink,
    surface = NeutralSurface,
    onSurface = Ink,
    surfaceVariant = NeutralSurfaceVariant,
    onSurfaceVariant = InkMuted,
    outline = NeutralOutline,
    error = Danger,
    onError = InkOnColor,
    scrim = Color(0x99000000),
)

private val DarkColors = darkColorScheme(
    primary = SkyBlueLight,
    onPrimary = Color(0xFF00344F),
    primaryContainer = SkyBlueDark,
    onPrimaryContainer = SkyBlueLight,
    secondary = SunshineLight,
    onSecondary = Color(0xFF432C00),
    secondaryContainer = SunshineDark,
    onSecondaryContainer = SunshineLight,
    tertiary = PartyPalette.Grape,
    onTertiary = Color(0xFF241452),
    background = NightBackground,
    onBackground = NightInk,
    surface = NightSurface,
    onSurface = NightInk,
    surfaceVariant = NightSurfaceVariant,
    onSurfaceVariant = NightInkMuted,
    outline = NightOutline,
    error = Color(0xFFFF8A94),
    onError = Color(0xFF5A0010),
    scrim = Color(0xCC000000),
)

/**
 * Semantic colours that Material 3 has no slot for.
 *
 * Module identity colours, the star/coin colours and the "try again" amber are
 * product-level meanings, not Material roles. Exposing them through a
 * [CompositionLocal] means any composable can reach them without threading a
 * `Color` parameter through every signature - and, critically, without a
 * composable hard-coding a colour that will not follow the theme.
 */
data class KidColors(
    val success: Color,
    val successContainer: Color,
    val tryAgain: Color,
    val tryAgainContainer: Color,
    val starGold: Color,
    val starEmpty: Color,
    val coinGold: Color,
    val english: Color,
    val bangla: Color,
    val arabic: Color,
    val maths: Color,
    val games: Color,
    val progress: Color,
    val parentZone: Color,
    /** Soft container tints, used behind module tiles. */
    val englishContainer: Color,
    val banglaContainer: Color,
    val arabicContainer: Color,
    val mathsContainer: Color,
    val gamesContainer: Color,
    val progressContainer: Color,
    val parentZoneContainer: Color,
) {
    /** Accent colour for a module, used on its tile, top bar and cards. */
    fun accentFor(module: ModuleType): Color = when (module) {
        ModuleType.ENGLISH -> english
        ModuleType.BANGLA -> bangla
        ModuleType.ARABIC -> arabic
        ModuleType.MATHS -> maths
    }

    fun containerFor(module: ModuleType): Color = when (module) {
        ModuleType.ENGLISH -> englishContainer
        ModuleType.BANGLA -> banglaContainer
        ModuleType.ARABIC -> arabicContainer
        ModuleType.MATHS -> mathsContainer
    }
}

private val LightKidColors = KidColors(
    success = Success,
    successContainer = SuccessContainer,
    tryAgain = TryAgain,
    tryAgainContainer = TryAgainContainer,
    starGold = StarGold,
    starEmpty = StarGoldEmpty,
    coinGold = CoinGold,
    english = EnglishColor,
    bangla = BanglaColor,
    arabic = ArabicColor,
    maths = MathsColor,
    games = GamesColor,
    progress = ProgressColor,
    parentZone = ParentZoneColor,
    englishContainer = EnglishContainer,
    banglaContainer = BanglaContainer,
    arabicContainer = ArabicContainer,
    mathsContainer = MathsContainer,
    gamesContainer = GamesContainer,
    progressContainer = ProgressContainer,
    parentZoneContainer = ParentZoneContainer,
)

private val DarkKidColors = LightKidColors.copy(
    // Containers become muted so a colourful tile still reads as "lit" against a
    // dark background rather than glowing.
    englishContainer = Color(0xFF3A2E63),
    banglaContainer = Color(0xFF63302C),
    arabicContainer = Color(0xFF14503C),
    mathsContainer = Color(0xFF144E70),
    gamesContainer = Color(0xFF63380F),
    progressContainer = Color(0xFF1D4A33),
    parentZoneContainer = Color(0xFF333A49),
    starEmpty = Color(0xFF444C5E),
    successContainer = Color(0xFF11522C),
    tryAgainContainer = Color(0xFF5C3607),
)

private val LocalKidColors = staticCompositionLocalOf { LightKidColors }

/**
 * Entry point for the semantic colours.
 *
 * An object rather than a top-level property so the colours are reachable as
 * `KidTheme.colors` from anywhere. That is what lets any composable use them
 * without every signature in the app growing a `Color` parameter, and without a
 * screen ever hard-coding a colour that will not follow the theme.
 */
object KidTheme {

    /**
     * The active semantic palette.
     *
     * A `@Composable` getter, so it always reads the value provided by the nearest
     * `KidsLearnTheme` above it. Reading it outside a composition throws, which is
     * the correct outcome - there is no meaningful "current" palette without one.
     */
    val colors: KidColors
        @Composable
        get() = LocalKidColors.current
}

/**
 * Root theme.
 *
 * @param useDarkTheme resolved from the parent's setting; the platform's dark
 *   setting is *not* consulted, because a three-year-old flipping the system theme
 *   to dark at 7pm should not silently change the app out from under them, and
 *   because the parent zone is the only place this is controlled from.
 * @param layoutDirection driven by the active module rather than the system
 *   locale: only the Arabic screens are RTL, and switching the whole app would
 *   mirror the English tiles as well. Individual screens opt in by wrapping
 *   themselves in `ProvideLocalLayoutDirection`.
 */
@Composable
fun KidsLearnTheme(
    useDarkTheme: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (useDarkTheme) DarkColors else LightColors
    val kidColors = if (useDarkTheme) DarkKidColors else LightKidColors

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            // Draw behind the system bars: the app draws its own colourful top bar,
            // and a translucent status bar keeps the header from being visually
            // truncated. This is opt-in per API level and silently ignored below 21.
            WindowCompat.setDecorFitsSystemWindows(window, false)
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !useDarkTheme
                isAppearanceLightNavigationBars = !useDarkTheme
            }
        }
    }

    CompositionLocalProvider(LocalKidColors provides kidColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = KidsTypography,
            shapes = KidsShapes,
            content = content,
        )
    }
}

/**
 * Forces right-to-left layout for a subtree.
 *
 * Used exclusively by the Arabic module. See the note on
 * [KidsLearnTheme.layoutDirection] for why this is a local decision rather than a
 * global one.
 */
@Composable
fun ProvideRtl(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        content()
    }
}
