package com.freedu.kidslearn.ui.components

import androidx.compose.ui.geometry.Offset
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Guards the glyph-outline math without Android: normalisation must land every
 * point inside the 0..1 box with aspect preserved, and the fallback must never
 * be empty (it is the tracing panel's last line of defence).
 */
class GlyphTraceTest {

    @Test
    fun `normalise fits a wide box with padding and centres the short axis`() {
        // 200x100 box: x spans the padded range, y is centred.
        val out = GlyphTrace.normalize(
            listOf(Offset(0f, 0f), Offset(200f, 0f), Offset(200f, 100f), Offset(0f, 100f)),
        )

        assertThat(out).hasSize(4)
        out.forEach {
            assertThat(it.x).isWithin(1e-6f).of(it.x.coerceIn(0f, 1f))
            assertThat(it.y).isWithin(1e-6f).of(it.y.coerceIn(0f, 1f))
        }
        val xs = out.map { it.x }
        assertThat(xs.min()).isWithin(1e-6f).of(0.12f)
        assertThat(xs.max()).isWithin(1e-6f).of(0.88f)
        // Height 100 of span 200 -> centred band, not stretched edge to edge.
        val ys = out.map { it.y }
        assertThat(ys.min()).isGreaterThan(0.12f)
        assertThat(ys.max()).isLessThan(0.88f)
        assertThat(ys.max() - ys.min()).isWithin(1e-6f).of(0.38f)
    }

    @Test
    fun `normalise handles degenerate input`() {
        assertThat(GlyphTrace.normalize(emptyList())).isEmpty()

        val single = GlyphTrace.normalize(listOf(Offset(5f, -3f)))
        assertThat(single).containsExactly(Offset(0.5f, 0.5f))
    }

    @Test
    fun `fallback is a bounded ellipse`() {
        val out = GlyphTrace.fallback()

        assertThat(out.size).isAtLeast(8)
        out.forEach {
            assertThat(it.x).isAtLeast(0f)
            assertThat(it.x).isAtMost(1f)
            assertThat(it.y).isAtLeast(0f)
            assertThat(it.y).isAtMost(1f)
        }
    }

    @Test
    fun `empty text samples to the fallback, never to nothing`() {
        // Paint is a stub on plain JVM, so every outline path degrades here -
        // which is exactly the fallback contract under test.
        assertThat(GlyphTrace.samplePoints(android.graphics.Paint(), "")).isNotEmpty()
    }
}
