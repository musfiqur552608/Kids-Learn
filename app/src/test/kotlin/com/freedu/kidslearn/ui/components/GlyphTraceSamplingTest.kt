package com.freedu.kidslearn.ui.components

import android.content.Context
import android.graphics.Paint
import androidx.core.content.res.ResourcesCompat
import androidx.test.core.app.ApplicationProvider
import com.freedu.kidslearn.R
import com.freedu.kidslearn.domain.model.ModuleType
import com.freedu.kidslearn.ui.alphabet.LetterTraceGuide
import com.freedu.kidslearn.ui.alphabet.TraceGuideSupport
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Samples real glyphs with the real bundled fonts.
 *
 * Under Robolectric `getTextPath` may return a true outline or nothing at all
 * (shadow limits) - either way the contract holds: points are non-empty and
 * bounded, so the panel is never blank. The routing half is exact: A-Z keep
 * their hand skeletons, Bangla/Arabic/Q resolve to font outlines, and maths
 * opts out.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class GlyphTraceSamplingTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private fun paintFor(fontRes: Int?): Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 100f
        typeface = fontRes?.let { ResourcesCompat.getFont(context, it) }
    }

    private fun assertBounded(points: List<androidx.compose.ui.geometry.Offset>) {
        assertThat(points.size).isAtLeast(8)
        points.forEach {
            assertThat(it.x).isAtLeast(0f)
            assertThat(it.x).isAtMost(1f)
            assertThat(it.y).isAtLeast(0f)
            assertThat(it.y).isAtMost(1f)
        }
    }

    @Test
    fun `bangla glyph samples inside the box`() {
        assertBounded(GlyphTrace.samplePoints(paintFor(R.font.noto_sans_bengali), "অ"))
    }

    @Test
    fun `arabic glyph samples inside the box`() {
        assertBounded(GlyphTrace.samplePoints(paintFor(R.font.noto_naskh_arabic), "ب"))
    }

    @Test
    fun `latin without a skeleton still traces`() {
        assertBounded(GlyphTrace.samplePoints(paintFor(null), "Q"))
    }

    @Test
    fun `hand skeletons win for A to Z`() {
        val guide = TraceGuideSupport.forLetter("A", ModuleType.ENGLISH)

        assertThat(guide).isInstanceOf(LetterTraceGuide.Hand::class.java)
        assertThat((guide as LetterTraceGuide.Hand).guide).isEqualTo(TraceGuide.A)
    }

    @Test
    fun `bangla arabic and Q resolve to font outlines`() {
        assertThat(TraceGuideSupport.forLetter("অ", ModuleType.BANGLA))
            .isEqualTo(LetterTraceGuide.Glyph("অ", R.font.noto_sans_bengali))
        assertThat(TraceGuideSupport.forLetter("ب", ModuleType.ARABIC))
            .isEqualTo(LetterTraceGuide.Glyph("ب", R.font.noto_naskh_arabic))
        assertThat(TraceGuideSupport.forLetter("Q", ModuleType.ENGLISH))
            .isEqualTo(LetterTraceGuide.Glyph("Q", null))
    }

    @Test
    fun `maths and blank input opt out`() {
        assertThat(TraceGuideSupport.forLetter("5", ModuleType.MATHS)).isNull()
        assertThat(TraceGuideSupport.forLetter("  ", ModuleType.BANGLA)).isNull()
    }
}
