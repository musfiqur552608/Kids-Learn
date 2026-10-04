package com.freedu.kidslearn.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.freedu.kidslearn.ui.theme.KidTheme
import kotlin.math.abs

/** How the child is doing with the trace. */
enum class TraceQuality {
    /** Not enough of a stroke to judge. */
    IDLE,

    /** Touched the canvas but has not covered enough of the letter yet. */
    IN_PROGRESS,

    /** Covered enough of the guide letter. */
    SUCCESS,
}

/** Result of a finished trace attempt. */
data class TraceResult(
    val quality: TraceQuality,
    /** 0..1 - share of the guide stroke the finger actually covered. */
    val coverage: Float,
)

/**
 * Finger-tracing practice for a letter shape.
 *
 * ## How accuracy is measured
 * A trace is "covered" when the finger passes within a tolerance of some sample
 * point on the guide. That choice of metric is deliberate:
 *
 *  * **Scale invariant** - guide points are normalised to 0..1, so the same guide
 *    works at any canvas size and any screen density.
 *  * **Speed independent** - a fast swipe and a slow, careful trace both count. A
 *    child must not be penalised for moving quickly.
 *  * **Direction independent** - tracing bottom-to-top is still correct. Enforcing
 *    direction would be discouraging and teaches nothing at this age.
 *
 * ## Why not a real handwriting recogniser
 * It would mean shipping a model in the APK, and - more importantly - it could
 * output a *negative* verdict ("that is not an A"). This app's core rule is
 * positive-only feedback, so the metric is built so the only possible outcomes are
 * "not yet" and "yes".
 */
@Composable
fun LetterTracingCanvas(
    guide: TraceGuide,
    onTraceFinished: (TraceResult) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    accentColor: Color,
) {
    val guidePoints = remember(guide) { guide.normalisedPoints() }
    LetterTracingCanvas(
        guidePoints = guidePoints,
        label = guide.label,
        onTraceFinished = onTraceFinished,
        modifier = modifier,
        enabled = enabled,
        accentColor = accentColor,
    )
}

/**
 * The same tracing panel driven by precomputed points - the entry point for
 * glyph-outline guides ([GlyphTrace]), which resolve their font at the call
 * site rather than from an enum.
 */
@Composable
fun LetterTracingCanvas(
    guidePoints: List<Offset>,
    label: String,
    onTraceFinished: (TraceResult) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    accentColor: Color,
) {
    // Read the palette here, in composition scope. `Canvas`'s draw block is a
    // DrawScope lambda, not a composable, so a `KidTheme.colors` read inside it
    // would not compile - and hoisting also means one lookup per recomposition
    // rather than one per redraw.
    val successColor = KidTheme.colors.success
    val tracingColor = KidTheme.colors.starGold
    val guideBackground = KidTheme.colors.successContainer
    val userPath = remember { mutableStateListOf<Offset>() }
    var isDrawing by remember { mutableStateOf(false) }
    var quality by remember { mutableStateOf(TraceQuality.IDLE) }
    // Pixel size of this canvas. Touches arrive in pixels but the guides live
    // in 0..1, so every recorded point is normalised here, at record time -
    // recording pixels and converting later is what made tracing unwinnable.
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    Box(
        modifier = modifier
            .background(guideBackground, RoundedCornerShape(28.dp))
            .onSizeChanged { canvasSize = it }
            .semantics { contentDescription = "Tracing area for the letter $label" },
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(enabled, guidePoints) {
                    if (!enabled) return@pointerInput
                    // Hand-rolled instead of detectDragGestures for two child-driven
                    // reasons: a tap that never passes touch slop must still end
                    // the attempt (otherwise tapping does literally nothing), and
                    // every move must be consumed so the surrounding LazyColumn
                    // does not steal a trace as a scroll.
                    awaitEachGesture {
                        isDrawing = true
                        quality = TraceQuality.IN_PROGRESS
                        userPath.clear()
                        try {
                            // Every event is recorded, starting with the down
                            // itself: a tap that never moves still ends here
                            // (with one point, scoring IDLE) instead of doing
                            // nothing, and every move is consumed so the
                            // surrounding LazyColumn never steals a trace.
                            while (true) {
                                val pressed = awaitPointerEvent().changes
                                    .filter { it.pressed }
                                if (pressed.isEmpty()) break
                                pressed.forEach {
                                    it.consume()
                                    userPath += it.position.normalizedBy(canvasSize)
                                }
                            }
                            val result = evaluateTrace(userPath.toList(), guidePoints)
                            quality = result.quality
                            onTraceFinished(result)
                            // The stroke stays on screen briefly so the child can see
                            // what they drew, then clears on the next attempt.
                        } finally {
                            isDrawing = false
                        }
                    }
                },
        ) {
            val guideInPixels = guidePoints.map { Offset(it.x * size.width, it.y * size.height) }

            // Guide: a thick, pale stroke the child follows.
            drawPath(
                path = guideInPixels.asPath(),
                color = GUIDE_COLOR,
                style = Stroke(
                    width = GUIDE_STROKE.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round,
                ),
            )

            // Start marker, so the child knows where to begin.
            guideInPixels.firstOrNull()?.let { start ->
                drawCircle(
                    color = successColor,
                    radius = START_MARKER_RADIUS.toPx(),
                    center = start,
                )
            }

            if (userPath.size > 1) {
                drawPath(
                    // Stored normalised; scaled back to pixels for drawing.
                    path = userPath
                        .map { Offset(it.x * size.width, it.y * size.height) }
                        .asPath(),
                    color = if (quality == TraceQuality.SUCCESS) successColor else accentColor,
                    style = Stroke(
                        width = USER_STROKE.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round,
                    ),
                )
            }
        }
    }
}

/** Builds a [Path] through a list of points. */
private fun List<Offset>.asPath(): Path {
    val path = Path()
    firstOrNull()?.let { path.moveTo(it.x, it.y) }
    // Skip repeated points: `detectDragGestures` can deliver two identical
    // positions during a pause, and a zero-length segment is drawn as an
    // artefact by some renderers.
    forEachIndexed { index, point ->
        if (index > 0 && point != this[index - 1]) path.lineTo(point.x, point.y)
    }
    return path
}

/**
 * Maps a raw touch point into the guide's 0..1 box.
 *
 * Touches arrive in pixels; guides live normalised. Comparing the two spaces
 * directly can never match (500px vs 0.5), which once made every trace score
 * zero - so normalisation happens at record time, point by point, and this
 * helper is the single place that does it.
 */
internal fun Offset.normalizedBy(size: IntSize): Offset {
    if (size.width <= 0 || size.height <= 0) return this
    return Offset(x / size.width, y / size.height)
}

/**
 * Scores a trace against the guide.
 *
 * Both [path] and [guidePoints] must already be normalised to 0..1 (see
 * [normalizedBy]) - the tolerance below is in the same units, so feeding
 * pixels here silently scores everything as a miss.
 *
 * Uses Chebyshev (square) distance rather than Euclidean so the tolerance region
 * is a square. For a tracing tolerance, square is the better shape: a child's
 * finger is roughly circular, but a square region never leaves an awkward
 * diagonal dead zone that a circle-shaped test would create at the corners.
 */
internal fun evaluateTrace(path: List<Offset>, guidePoints: List<Offset>): TraceResult {
    if (path.size < MIN_POINTS_FOR_ATTEMPT || guidePoints.isEmpty()) {
        return TraceResult(TraceQuality.IDLE, 0f)
    }
    val covered = guidePoints.count { guide ->
        path.any { user ->
            abs(user.x - guide.x) < HIT_TOLERANCE && abs(user.y - guide.y) < HIT_TOLERANCE
        }
    }
    val coverage = covered.toFloat() / guidePoints.size
    return TraceResult(
        quality = if (coverage >= PASS_THRESHOLD) TraceQuality.SUCCESS else TraceQuality.IN_PROGRESS,
        coverage = coverage,
    )
}

private val GUIDE_COLOR = Color(0x22000000)

private val GUIDE_STROKE: Dp = 26.dp
private val USER_STROKE: Dp = 14.dp
private val START_MARKER_RADIUS: Dp = 12.dp

/** A tap that does not drag is not an attempt. */
private const val MIN_POINTS_FOR_ATTEMPT = 3

/**
 * Hit tolerance as a fraction of the canvas size.
 *
 * 14% is generous on purpose: a child's fingertip covers a large area of the
 * screen, and Android's reported touch coordinate for a fat finger is already
 * well away from the exact point of contact.
 */
private const val HIT_TOLERANCE = 0.14f

/** 70% coverage earns the star. */
private const val PASS_THRESHOLD = 0.7f
