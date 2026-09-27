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
