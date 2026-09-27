package com.freedu.kidslearn.ui.components

import androidx.compose.ui.geometry.Offset
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * A traceable letter outline, expressed as a normalised polyline.
 *
 * ## Coordinates
 * All points are in a 0..1 box with the origin at the **top left**, matching
 * Compose's coordinate system. `y = 0` is the top of the canvas.
 *
 * ## Why polylines and not real font outlines
 * Extracting a glyph outline from the TTF would need font parsing at runtime (or
 * a build step), and a traced outline asks the child to follow the *skeleton* of a
 * letter rather than its exact contour. The skeleton is also what a child is
 * mentally drawing when they trace a letter, so the simplified version is more
 * pedagogically accurate, not less.
 *
 * Only the letters worth tracing are listed. A fallback circle is used for
 * anything else so the feature never fails - a child tapping "trace" on an
 * unsupported letter still gets the interaction rather than a crash or a blank
 * panel.
 */
enum class TraceGuide(val label: String) {
    A("A"),
    B("B"),
    C("C"),
    D("D"),
    E("E"),
    F("F"),
    G("G"),
    H("H"),
    I("I"),
    J("J"),
    K("K"),
    L("L"),
    M("M"),
    N("N"),
    O("O"),
    P("P"),
    R("R"),
    S("S"),
    T("T"),
    U("U"),
    V("V"),
    W("W"),
    X("X"),
    Y("Y"),
    Z("Z"),
    ;

    /** The polyline, in 0..1 coordinates. */
    fun normalisedPoints(): List<Offset> = pointsFor(this) ?: fallback()

    private fun fallback(): List<Offset> = ellipse(cx = 0.5f, cy = 0.5f, rx = 0.3f, ry = 0.4f)

    companion object {
        private fun p(x: Float, y: Float) = Offset(x, y)

        private fun ellipse(cx: Float, cy: Float, rx: Float, ry: Float, steps: Int = 28): List<Offset> =
            (0..steps).map { i ->
                val t = 2.0 * PI * i / steps
                p(cx + (rx * cos(t)).toFloat(), cy + (ry * sin(t)).toFloat())
            }

        private fun line(vararg points: Pair<Float, Float>): List<Offset> =
            points.map { (x, y) -> p(x, y) }

        /** Two strokes, e.g. the two diagonals of an X. */
        private fun strokes(vararg polylines: List<Offset>): List<Offset> =
            polylines.toList().flatten()

        private fun pointsFor(guide: TraceGuide): List<Offset>? = when (guide) {
            // Up-and-down strokes are built bottom-to-top, which is how most
            // handwriting curricula teach them.
            TraceGuide.A -> strokes(
                listOf(p(0.15f, 0.85f), p(0.5f, 0.15f), p(0.85f, 0.85f)),
                line(0.28f to 0.58f, 0.72f to 0.58f),
            )
            TraceGuide.B -> strokes(
                listOf(p(0.3f, 0.15f), p(0.3f, 0.85f)),
                listOf(p(0.3f, 0.15f), p(0.62f, 0.3f), p(0.3f, 0.5f)),
                listOf(p(0.3f, 0.5f), p(0.66f, 0.68f), p(0.3f, 0.85f)),
            )
            TraceGuide.C -> ellipse(0.52f, 0.5f, 0.32f, 0.35f, 24).let {
                // Skip the right-hand side so the C does not close.
                it.filter { point -> point.x < 0.66f }
            }
            TraceGuide.D -> strokes(
                listOf(p(0.3f, 0.15f), p(0.3f, 0.85f)),
                listOf(p(0.3f, 0.15f), p(0.6f, 0.28f), p(0.7f, 0.5f), p(0.6f, 0.72f), p(0.3f, 0.85f)),
            )
            TraceGuide.E -> strokes(
                listOf(p(0.32f, 0.15f), p(0.32f, 0.85f)),
                line(0.32f to 0.15f, 0.72f to 0.15f),
                line(0.32f to 0.5f, 0.66f to 0.5f),
                line(0.32f to 0.85f, 0.72f to 0.85f),
            )
            TraceGuide.F -> strokes(
                listOf(p(0.32f, 0.15f), p(0.32f, 0.85f)),
                line(0.32f to 0.15f, 0.72f to 0.15f),
                line(0.32f to 0.5f, 0.66f to 0.5f),
            )
            TraceGuide.G -> ellipse(0.5f, 0.5f, 0.32f, 0.35f, 24).let { arc ->
                arc.filter { it.x < 0.72f } + line(0.66f to 0.55f, 0.82f to 0.55f, 0.82f to 0.72f)
            }
            TraceGuide.H -> strokes(
                listOf(p(0.28f, 0.15f), p(0.28f, 0.85f)),
                listOf(p(0.72f, 0.15f), p(0.72f, 0.85f)),
                line(0.28f to 0.52f, 0.72f to 0.52f),
            )
            TraceGuide.I -> line(0.5f to 0.15f, 0.5f to 0.85f)
            TraceGuide.J -> strokes(
                listOf(p(0.7f, 0.15f), p(0.7f, 0.7f)),
                // Hook at the bottom.
                (0..10).map { i ->
                    val t = PI * i / 10
                    p(0.7f - (0.3f + 0.3f * cos(t).toFloat()), 0.7f + (0.15f * sin(t).toFloat()))
                },
            )
            TraceGuide.K -> strokes(
                listOf(p(0.3f, 0.15f), p(0.3f, 0.85f)),
                line(0.75f to 0.15f, 0.3f to 0.52f),
                line(0.3f to 0.52f, 0.75f to 0.85f),
            )
            TraceGuide.L -> line(0.32f to 0.15f, 0.32f to 0.85f, 0.75f to 0.85f)
            TraceGuide.M -> listOf(
                p(0.18f, 0.85f), p(0.18f, 0.15f), p(0.5f, 0.6f), p(0.82f, 0.15f), p(0.82f, 0.85f),
            )
            TraceGuide.N -> listOf(p(0.25f, 0.85f), p(0.25f, 0.15f), p(0.75f, 0.85f), p(0.75f, 0.15f))
            TraceGuide.O -> ellipse(0.5f, 0.5f, 0.32f, 0.35f)
            TraceGuide.P -> strokes(
                listOf(p(0.32f, 0.85f), p(0.32f, 0.15f)),
                listOf(p(0.32f, 0.15f), p(0.65f, 0.3f), p(0.32f, 0.5f)),
            )
            TraceGuide.R -> strokes(
                listOf(p(0.32f, 0.85f), p(0.32f, 0.15f)),
                listOf(p(0.32f, 0.15f), p(0.65f, 0.3f), p(0.32f, 0.5f)),
                line(0.4f to 0.5f, 0.78f to 0.85f),
            )
            TraceGuide.S -> (0..24).map { i ->
                // A figure-eight-ish double curve gives a recognisable S.
                val t = PI * i / 12
                val sign = if (i <= 12) -1f else 1f
                p(0.5f + 0.28f * cos(t).toFloat() * sign, 0.5f + 0.36f * sin(t).toFloat())
            }
            TraceGuide.T -> strokes(
                listOf(p(0.5f, 0.15f), p(0.5f, 0.85f)),
                line(0.18f to 0.15f, 0.82f to 0.15f),
            )
            TraceGuide.U -> listOf(
                p(0.25f, 0.15f), p(0.25f, 0.6f), p(0.35f, 0.82f), p(0.65f, 0.82f), p(0.75f, 0.6f), p(0.75f, 0.15f),
            )
            TraceGuide.V -> listOf(p(0.2f, 0.15f), p(0.5f, 0.85f), p(0.8f, 0.15f))
            TraceGuide.W -> listOf(
                p(0.12f, 0.15f), p(0.3f, 0.85f), p(0.5f, 0.35f), p(0.7f, 0.85f), p(0.88f, 0.15f),
            )
            TraceGuide.X -> strokes(
                line(0.22f to 0.15f, 0.78f to 0.85f),
                line(0.78f to 0.15f, 0.22f to 0.85f),
            )
            TraceGuide.Y -> strokes(
                line(0.2f to 0.15f, 0.5f to 0.5f),
                line(0.8f to 0.15f, 0.5f to 0.5f),
                line(0.5f to 0.5f, 0.5f to 0.85f),
            )
            TraceGuide.Z -> listOf(
                p(0.22f, 0.15f), p(0.78f, 0.15f), p(0.22f, 0.85f), p(0.78f, 0.85f),
            )
        }
    }
}
