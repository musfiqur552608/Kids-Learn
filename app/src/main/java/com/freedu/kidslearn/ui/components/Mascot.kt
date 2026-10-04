package com.freedu.kidslearn.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.rememberLottieComposition
import com.freedu.kidslearn.R
import com.freedu.kidslearn.ui.theme.KidTheme
import com.freedu.kidslearn.ui.theme.Sunshine
import com.freedu.kidslearn.ui.theme.SunshineDark

/**
 * Mascot mood. Drives both the face and the wording the mascot "says".
 */
enum class MascotMood {
    /** Cheerful and idle, used on most screens. */
    HAPPY,

    /** Celebrating a correct answer. */
    CHEER,

    /** Inviting another attempt after a wrong answer - a smile, never a frown. */
    ENCOURAGE,

    /** Proud, shown on the result screen. */
    PROUD,
}

/**
 * The mascot.
 *
 * ## Why a drawn Canvas and not an image or Lottie
 * An idle mascot sits on nearly every screen, so its cost is paid constantly.
 * A vector [Canvas] draws in microseconds and costs nothing in APK size, whereas a
 * bitmap would need a density-bucketed asset set and a Lottie would keep a
 * composition alive for no visual benefit when the character is only blinking and
 * rocking. Lottie is reserved for one-shot celebrations where the animation *is*
 * the content.
 *
 * The character is deliberately abstract - two dots and a curve. A specific
 * animal or person would date the app and imply a gender/personality that has to
 * be maintained in every line of copy.
 */
@Composable
fun Mascot(
    modifier: Modifier = Modifier,
    mood: MascotMood = MascotMood.HAPPY,
    size: androidx.compose.ui.unit.Dp = 96.dp,
) {
    val transition = rememberInfiniteTransition(label = "mascot-idle")
    val bob by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = androidx.compose.animation.core.FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "mascot-bob",
    )
    val wave by transition.animateFloat(
        initialValue = -8f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = androidx.compose.animation.core.LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "mascot-wave",
    )

    // Announced without the mood: "mascot, encourage" is developer vocabulary,
    // not something a TalkBack user can act on, and the mood name is English-only.
    val mascotDescription = androidx.compose.ui.res.stringResource(R.string.cd_mascot)
    Box(
        modifier = modifier
            .size(size)
            .scale(1f + bob * 0.03f)
            .semantics { contentDescription = mascotDescription },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val w = this.size.width
            val h = this.size.height
            val faceColor = Sunshine
            val lineColor = SunshineDark

            // Body: a rounded blob.
            drawOval(
                color = faceColor,
                topLeft = Offset(w * 0.08f, h * 0.10f),
                size = Size(w * 0.84f, h * 0.80f),
            )

            // Two antennae.
            drawLine(
                color = lineColor,
                start = Offset(w * 0.34f, h * 0.16f),
                end = Offset(w * 0.28f, h * 0.02f),
                strokeWidth = w * 0.05f,
            )
            drawCircle(
                color = lineColor,
                radius = w * 0.06f,
                center = Offset(w * 0.27f, h * 0.01f),
            )
            drawLine(
                color = lineColor,
                start = Offset(w * 0.66f, h * 0.16f),
                end = Offset(w * 0.74f, h * 0.03f),
                strokeWidth = w * 0.05f,
            )
            drawCircle(
                color = lineColor,
                radius = w * 0.06f,
                center = Offset(w * 0.75f, h * 0.02f),
            )

            // Eyes. Closed (arcs) when cheering, open otherwise.
            val eyeY = h * 0.42f
            if (mood == MascotMood.CHEER || mood == MascotMood.PROUD) {
                drawArc(
                    color = lineColor,
                    startAngle = 200f,
                    sweepAngle = 140f,
                    useCenter = false,
                    topLeft = Offset(w * 0.24f, eyeY - h * 0.04f),
                    size = Size(w * 0.16f, h * 0.12f),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = w * 0.045f),
                )
                drawArc(
                    color = lineColor,
                    startAngle = 200f,
                    sweepAngle = 140f,
                    useCenter = false,
                    topLeft = Offset(w * 0.60f, eyeY - h * 0.04f),
                    size = Size(w * 0.16f, h * 0.12f),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = w * 0.045f),
                )
            } else {
                drawCircle(color = lineColor, radius = w * 0.075f, center = Offset(w * 0.32f, eyeY))
                drawCircle(color = lineColor, radius = w * 0.075f, center = Offset(w * 0.68f, eyeY))
                // Catchlight - makes the character read as alive rather than dead.
                drawCircle(
                    color = Color.White,
                    radius = w * 0.025f,
                    center = Offset(w * 0.30f, eyeY - h * 0.025f),
                )
                drawCircle(
                    color = Color.White,
                    radius = w * 0.025f,
                    center = Offset(w * 0.66f, eyeY - h * 0.025f),
                )
            }

            // Mouth: a small smile that widens with the mood. Always upturned -
            // an encouraging character never frowns in this app.
            val smileWidth = when (mood) {
                MascotMood.CHEER, MascotMood.PROUD -> w * 0.30f
                else -> w * 0.22f
            }
            drawArc(
                color = lineColor,
                startAngle = 0f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(w * 0.5f - smileWidth / 2f, h * 0.52f),
                size = Size(smileWidth, smileWidth * 0.7f),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = w * 0.045f),
            )

            // Cheeks.
            drawCircle(
                color = Color(0xFFFF9A9A).copy(alpha = 0.55f),
                radius = w * 0.06f,
                center = Offset(w * 0.15f, h * 0.56f),
            )
            drawCircle(
                color = Color(0xFFFF9A9A).copy(alpha = 0.55f),
                radius = w * 0.06f,
                center = Offset(w * 0.85f, h * 0.56f),
            )

            // A little waving arm on the right, waving faster when celebrating.
            rotate(degrees = wave * if (mood == MascotMood.CHEER) 1.6f else 1f, pivot = Offset(w * 0.92f, h * 0.55f)) {
                drawOval(
                    color = faceColor,
                    topLeft = Offset(w * 0.86f, h * 0.44f),
                    size = Size(w * 0.16f, h * 0.24f),
                )
            }
        }
    }
}

/**
 * The mascot plus a speech bubble.
 *
 * This is the app's primary channel for instruction. For a pre-reader it carries
 * more meaning than any on-screen text, which is why the wording is kept to a few
 * words and duplicated in spoken form by [com.freedu.kidslearn.core.audio.FeedbackPlayer].
 */
@Composable
fun MascotSpeech(
    message: String,
    modifier: Modifier = Modifier,
    mood: MascotMood = MascotMood.HAPPY,
    showMascot: Boolean = true,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (showMascot) {
            Mascot(mood = mood, size = 76.dp)
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .background(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(24.dp),
                )
                .padding(horizontal = 18.dp, vertical = 14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // A small speaker glyph signals "this is said out loud too", which
                // teaches parents the audio is the primary channel.
                Icon(
                    imageVector = Icons.Filled.GraphicEq,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Start,
                )
            }
        }
    }
}

/**
 * Full-screen celebration overlay: Lottie confetti plus a mascot.
 *
 * ## Performance
 * Lottie here is a one-shot `LottieComposition` with `iterations = 1`. The
 * composable is removed from the tree when [visible] goes false, so the
 * composition is not kept alive behind a celebration the child cannot see.
 */
@Composable
fun CelebrationOverlay(
    visible: Boolean,
    message: String,
    modifier: Modifier = Modifier,
    onFinished: () -> Unit = {},
) {
    if (!visible) return

    // `LottieCompositionSpec.RawRes` is the current way to point Lottie at a
    // bundled asset. The older `LottieCompositionSpec.LottieRawRes` name was
    // removed in Lottie 6, and passing a `LottieCompositionResult` to
    // `LottieAnimation` no longer type-checks, so the result is unwrapped here.
    val composition by rememberLottieComposition(
        LottieCompositionSpec.RawRes(R.raw.confetti),
    )

    androidx.compose.animation.AnimatedVisibility(
        visible = visible,
        enter = androidx.compose.animation.fadeIn(
            animationSpec = tween(220),
        ),
        exit = androidx.compose.animation.fadeOut(
            animationSpec = tween(260),
        ),
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 420.dp),
            contentAlignment = Alignment.Center,
        ) {
            com.airbnb.lottie.compose.LottieAnimation(
                composition = composition,
                iterations = 1,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp),
            )
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Mascot(mood = MascotMood.CHEER, size = 110.dp)
                Text(
                    text = message,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onPrimary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .background(
                            KidTheme.colors.starGold,
                            RoundedCornerShape(20.dp),
                        )
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                )
                KidButton(
                    text = androidx.compose.ui.res.stringResource(R.string.yay),
                    onClick = onFinished,
                    color = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}
