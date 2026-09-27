package com.freedu.kidslearn.ui.splash

import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.freedu.kidslearn.R
import com.freedu.kidslearn.ui.components.Mascot
import com.freedu.kidslearn.ui.components.MascotMood
import kotlinx.coroutines.delay
import com.freedu.kidslearn.ui.theme.KidTheme

/**
 * The launch screen.
 *
 * ## Why it exists for a fixed duration
 * The app has no network and no startup I/O worth waiting for, so this is not a
 * loading screen - it is a *moment*. For a child, an app that appears instantly
 * gives no sense of having opened; a brief animated title card is what makes
 * "I pressed the button and something happened" legible.
 *
 * The duration is kept short ([HOLD_MS]) and is cancelled early by
 * [onFinished] if the caller is ready sooner, so it never becomes an obstacle.
 */
@Composable
fun SplashScreen(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
    holdMillis: Long = HOLD_MS,
) {
    val transition = rememberInfiniteTransition(label = "splash")
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "splash-alpha",
    )

    LaunchedEffect(Unit) {
        delay(holdMillis)
        onFinished()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Mascot(
                mood = MascotMood.HAPPY,
                size = 150.dp,
                modifier = Modifier.alpha(alpha),
            )
            Spacer(Modifier.height(20.dp))
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.home_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = KidTheme.colors.tryAgain,
                textAlign = TextAlign.Center,
            )
        }
    }
}

private const val HOLD_MS = 1_600L
