package com.freedu.kidslearn.ui.components

import android.graphics.Paint
import android.graphics.PathMeasure
import androidx.compose.ui.geometry.Offset
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Trace guides sampled from real glyph outlines.
 *
 * Hand-drawn skeletons ([TraceGuide]) cover A-Z, but authoring 74 more
 * polylines for Bangla and Arabic by hand would be error-prone and would rot
 * every time a font changes. Instead the outline is read out of the very font
 * file the letter is rendered with (`Paint.getTextPath`), walked with
 * [PathMeasure], and normalised into the same 0..1 box the tracing canvas
 * scores in - so Bangla, Arabic (and Q, which has no hand skeleton) all get a
 * tracing panel for free, in exactly the shapes the child sees on the card.
 *
 * Multi-contour glyphs (ই, ع and friends have inner loops) are sampled
 * proportionally to contour length with a 3-sample minimum, so a tiny inner
 * loop still counts without dominating the coverage score.
 */
object GlyphTrace {

    /** Matches the hand-guide density, so difficulty is comparable. */
    const val SAMPLES = 28

    /**
     * Samples [samples] points along [text]'s outline.
     *
     * Never returns an empty list: a missing font or an unmeasurable path falls
     * back to an ellipse, mirroring [TraceGuide]'s safety net, so the panel is
     * never blank - a blank tracing panel reads as a broken screen.
     */
    fun samplePoints(paint: Paint, text: String, samples: Int = SAMPLES): List<Offset> {
        if (text.isEmpty() || samples <= 0) return fallback()
        val path = android.graphics.Path()
        paint.getTextPath(text, 0, text.length, 0f, 0f, path)

        val measure = PathMeasure()
        val lengths = mutableListOf<Float>()
        measure.setPath(path, false)
        do {
            lengths.add(measure.length)
        } while (measure.nextContour())
        if (lengths.sum() <= 0f) return fallback()

        val points = mutableListOf<Offset>()
        measure.setPath(path, false)
        var contour = 0
        val position = FloatArray(2)
        do {
            val length = lengths[contour++]
            val count = max(3, (samples * length / lengths.sum()).roundToInt())
            for (i in 0 until count) {
                measure.getPosTan(length * i / count, position, null)
                points.add(Offset(position[0], position[1]))
            }
        } while (measure.nextContour())
        return normalize(points).ifEmpty { fallback() }
    }

    /**
     * Fits [points] into the 0..1 box with uniform scale and [PAD] padding.
     *
     * Uniform (not per-axis) so round glyphs stay round; the shorter axis is
     * centred. Pure math, unit-tested on the JVM without Android.
     */
    internal fun normalize(points: List<Offset>): List<Offset> {
        if (points.isEmpty()) return emptyList()
        val minX = points.minOf { it.x }
        val maxX = points.maxOf { it.x }
        val minY = points.minOf { it.y }
        val maxY = points.maxOf { it.y }
        val span = max(maxX - minX, maxY - minY)
        if (span <= 0f) return points.map { Offset(0.5f, 0.5f) }
        val scale = (1f - 2 * PAD) / span
        val xOffset = (1f - (maxX - minX) * scale) / 2f - minX * scale
        val yOffset = (1f - (maxY - minY) * scale) / 2f - minY * scale
        return points.map { Offset(it.x * scale + xOffset, it.y * scale + yOffset) }
    }

    /** Ellipse safety net - the same shape TraceGuide falls back to. */
    fun fallback(steps: Int = SAMPLES): List<Offset> =
        (0..steps).map { i ->
            val t = 2.0 * Math.PI * i / steps
            Offset(
                0.5f + (0.3f * kotlin.math.cos(t)).toFloat(),
                0.5f + (0.4f * kotlin.math.sin(t)).toFloat(),
            )
        }

    private const val PAD = 0.12f
}
