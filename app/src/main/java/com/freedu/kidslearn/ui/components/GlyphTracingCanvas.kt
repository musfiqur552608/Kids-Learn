package com.freedu.kidslearn.ui.components

import android.graphics.Paint
import androidx.annotation.FontRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.res.ResourcesCompat

/**
 * Tracing for scripts with no hand-drawn skeleton: Bangla, Arabic (and Q).
 *
 * Resolves the module's bundled font, samples the glyph outline via
 * [GlyphTrace], and hands the points to the shared tracing panel - so the
 * scoring, the guide stroke and the celebration are identical to A-Z, only the
 * shape source differs. A missing font degrades to the platform typeface
 * (Latin still draws); an unmeasurable outline degrades to the ellipse, so the
 * panel is never blank.
 */
@Composable
fun GlyphTracingCanvas(
    text: String,
    @FontRes fontRes: Int?,
    onTraceFinished: (TraceResult) -> Unit,
    accentColor: Color,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val context = LocalContext.current
    val guidePoints = remember(text, fontRes) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = GLYPH_TEXT_SIZE_PX
            typeface = fontRes?.let { id ->
                runCatching { ResourcesCompat.getFont(context, id) }.getOrNull()
            }
        }
        GlyphTrace.samplePoints(paint, text)
    }
    LetterTracingCanvas(
        guidePoints = guidePoints,
        label = text,
        onTraceFinished = onTraceFinished,
        modifier = modifier,
        enabled = enabled,
        accentColor = accentColor,
    )
}

/** Paint units - the outline is normalised to 0..1 afterwards, so any size works. */
private const val GLYPH_TEXT_SIZE_PX = 100f
