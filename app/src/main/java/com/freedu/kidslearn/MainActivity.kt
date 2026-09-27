package com.freedu.kidslearn

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.freedu.kidslearn.domain.model.AppSettings
import com.freedu.kidslearn.domain.model.ThemeMode
import com.freedu.kidslearn.domain.repository.SettingsRepository
import com.freedu.kidslearn.ui.navigation.KidsNavHost
import com.freedu.kidslearn.ui.navigation.Route
import com.freedu.kidslearn.ui.theme.KidsLearnTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * The single Activity.
 *
 * ## Why one Activity
 * Navigation Compose owns the back stack in a `NavController`. A second Activity
 * would mean a second back stack plus manual coordination between the two, so
 * every screen is a composable destination instead. That also gives correct
 * system back handling, saved-state restoration and future deep linking for free.
 *
 * ## System bars
 * The app draws edge-to-edge (transparent bars, configured in
 * [KidsLearnTheme]) and applies insets per-screen with `systemBarsPadding`, so a
 * coloured top bar can extend under the status bar while its *content* stays
 * clear of the notch and the gesture bar.
 *
 * ## Orientation
 * Locked to portrait in the manifest. The layouts are designed for a tall,
 * two-column grid, and supporting landscape would mean a second set of
 * breakpoints for no benefit to the audience.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            // Collected here, not inside a ViewModel: the theme and the RTL base
            // direction are needed *above* the NavHost, and settings change rarely
            // enough that a single collector at the root is the simplest correct
            // place for them.
            val settings by settingsRepository.settings
                .collectAsState(initial = AppSettings.DEFAULT)

            KidsLearnTheme(
                useDarkTheme = settings.themeMode == ThemeMode.DARK,
            ) {
                // The root direction is LTR even for a Bangla or Arabic device.
                // Only the Arabic module forces RTL for its own subtree: mirroring
                // the whole app would also mirror the English tiles, and the app
                // chrome (Home, settings) is authored left-to-right.
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Surface(
                        color = MaterialTheme.colorScheme.background,
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .systemBarsPadding(),
                        ) {
                            KidsNavHost(
                                startDestination = Route.Splash,
                            )
                        }
                    }
                }
            }
        }
    }
}
