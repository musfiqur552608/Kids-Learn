package com.freedu.kidslearn.ui.components

import androidx.compose.ui.geometry.Offset
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * The tracing metric is pure geometry, so it is fully testable without a device -
 * which matters because the tolerance and the pass threshold are the two numbers
 * most likely to be re-tuned after playing with real children.
 */
class LetterTracingCanvasTest {

    private fun guidePoints(vararg points: Pair<Float, Float>): List<Offset> =
        points.map { Offset(it.first, it.second) }

    @Test
    fun `a tap is not an attempt`() {
        val result = evaluateTrace(listOf(Offset(0.5f, 0.5f)), guidePoints(0.5f to 0.5f))

        assertThat(result.quality).isEqualTo(TraceQuality.IDLE)
    }

    @Test
    fun `a path that misses the guide does not pass`() {
        val guide = guidePoints(0.5f to 0.5f)
        val path = List(20) { Offset(0.05f, 0.05f) }

        val result = evaluateTrace(path, guide)

        assertThat(result.quality).isEqualTo(TraceQuality.IN_PROGRESS)
        assertThat(result.coverage).isEqualTo(0f)
    }

    @Test
    fun `a path straight down the guide passes`() {
        val guide = guidePoints(
            0.5f to 0.1f, 0.5f to 0.3f, 0.5f to 0.5f, 0.5f to 0.7f, 0.5f to 0.9f,
        )
        val path = guide.map { Offset(it.x, it.y + 0.02f) }

        val result = evaluateTrace(path, guide)

        assertThat(result.quality).isEqualTo(TraceQuality.SUCCESS)
        assertThat(result.coverage).isEqualTo(1f)
    }

    @Test
    fun `a small wobble still passes`() {
        // Children cannot trace a straight line; the tolerance has to absorb this.
        val guide = guidePoints(
            0.5f to 0.1f, 0.5f to 0.3f, 0.5f to 0.5f, 0.5f to 0.7f, 0.5f to 0.9f,
        )
        val path = guide.map { Offset(it.x + 0.08f, it.y + 0.08f) }

        assertThat(evaluateTrace(path, guide).quality).isEqualTo(TraceQuality.SUCCESS)
    }

    @Test
    fun `a large wobble does not pass`() {
        val guide = guidePoints(
            0.5f to 0.1f, 0.5f to 0.3f, 0.5f to 0.5f, 0.5f to 0.7f, 0.5f to 0.9f,
        )
        val path = guide.map { Offset(it.x + 0.45f, it.y) }

        assertThat(evaluateTrace(path, guide).quality).isEqualTo(TraceQuality.IN_PROGRESS)
    }

    @Test
    fun `tracing backwards is accepted`() {
        // Direction is deliberately not judged: penalising it would teach nothing
        // and would fail children whose curriculum teaches top-down.
        val guide = guidePoints(0.2f to 0.2f, 0.2f to 0.8f, 0.8f to 0.8f)
        val path = guide.reversed()

        assertThat(evaluateTrace(path, guide).quality).isEqualTo(TraceQuality.SUCCESS)
    }

    @Test
    fun `partial coverage is reported as a fraction`() {
        val guide = guidePoints(
            0.1f to 0.1f, 0.1f to 0.5f, 0.9f to 0.5f, 0.9f to 0.9f,
        )
        // Cover only the first half.
        val path = listOf(
            Offset(0.1f, 0.1f), Offset(0.1f, 0.5f), Offset(0.1f, 0.5f), Offset(0.1f, 0.5f),
        )

        val result = evaluateTrace(path, guide)

        assertThat(result.coverage).isWithin(0.01f).of(0.5f)
    }

    @Test
    fun `an empty guide is handled`() {
        val result = evaluateTrace(listOf(Offset(0.5f, 0.5f)), emptyList())

        assertThat(result.quality).isEqualTo(TraceQuality.IDLE)
    }

    @Test
    fun `touch points are normalised to the guide space`() {
        val size = androidx.compose.ui.unit.IntSize(1000, 2000)

        assertThat(Offset(500f, 1000f).normalizedBy(size)).isEqualTo(Offset(0.5f, 0.5f))
        assertThat(Offset(0f, 0f).normalizedBy(size)).isEqualTo(Offset(0f, 0f))
        // Zero size (first frame, before layout) passes the point through rather
        // than dividing by zero.
        assertThat(Offset(7f, 9f).normalizedBy(androidx.compose.ui.unit.IntSize.Zero))
            .isEqualTo(Offset(7f, 9f))
    }

    @Test
    fun `a real finger trace down the guide passes`() {
        // Regression: touches used to be recorded in raw pixels while guides
        // are 0..1, so coverage was always 0 and tracing could never succeed.
        // This replays the production flow - pixels in, normalise, score.
        val canvas = androidx.compose.ui.unit.IntSize(1080, 1200)
        val guide = guidePoints(
            0.5f to 0.1f, 0.5f to 0.3f, 0.5f to 0.5f, 0.5f to 0.7f, 0.5f to 0.9f,
        )
        val fingerPixels = guide.map { Offset(it.x * canvas.width, it.y * canvas.height) }
        val recorded = fingerPixels.map { it.normalizedBy(canvas) }

        val result = evaluateTrace(recorded, guide)

        assertThat(result.quality).isEqualTo(TraceQuality.SUCCESS)
        assertThat(result.coverage).isWithin(0.01f).of(1f)
    }

    @Test
    fun `raw pixels scored directly never match`() {
        // Documents why normalisation exists: this is the old broken behaviour.
        val guide = guidePoints(0.5f to 0.5f)
        val rawPixels = List(20) { Offset(540f, 600f) }

        assertThat(evaluateTrace(rawPixels, guide).coverage).isEqualTo(0f)
    }

    @Test
    fun `two strokes accumulate to a pass`() {
        // Multi-stroke letters (A, E, ঘ) need two or more lifts of the finger.
        // Strokes share one attempt, so covering half the guide twice is a pass
        // while either half alone is not.
        val guide = guidePoints(
            0.5f to 0.1f, 0.5f to 0.3f, 0.5f to 0.5f, 0.5f to 0.7f, 0.5f to 0.9f,
        )
        val firstStroke = guide.take(2)
        val secondStroke = guide.drop(2)

        assertThat(evaluateTrace(firstStroke + List(2) { firstStroke.last() }, guide).quality)
            .isEqualTo(TraceQuality.IN_PROGRESS)
        assertThat(evaluateTrace(firstStroke + secondStroke, guide).quality)
            .isEqualTo(TraceQuality.SUCCESS)
    }

    @Test
    fun `every guide produces a usable polyline`() {
        TraceGuide.entries.forEach { guide ->
            val points = guide.normalisedPoints()

            assertThat(points.size).isAtLeast(2)
            points.forEach { point ->
                // Normalised coordinates outside 0..1 would draw outside the canvas.
                assertThat(point.x).isAtLeast(0f)
                assertThat(point.x).isAtMost(1f)
                assertThat(point.y).isAtLeast(0f)
                assertThat(point.y).isAtMost(1f)
            }
        }
    }
}
