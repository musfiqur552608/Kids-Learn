package com.freedu.kidslearn.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Corner radii.
 *
 * Generous, almost everything-soft rounding. Soft shapes read as friendly and
 * non-threatening to young children, and a large radius also visually enlarges
 * the apparent touch target near the edges of a card.
 */
val KidsShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp),
)

/**
 * The minimum interactive size in this app.
 *
 * 64dp is a hard floor, not a preference. Children aged 3-5 have imprecise motor
 * control, and the commonly cited target for a reliable tap from that age group
 * starts around 12-14mm; at typical phone densities that is 64-80dp. Every
 * tappable composable in `ui/components` is at least this tall.
 */
val MinTouchTarget = 64.dp

/** Standard size for a primary call-to-action button. */
val PrimaryButtonHeight = 72.dp

/** Size of a module tile on the Home screen grid. */
val ModuleTileHeight = 132.dp

/** Size of a letter card in the alphabet grid. */
val LetterCardSize = 96.dp

/** Corner radius for the full-bleed answer tiles used in quizzes. */
val AnswerCorner = 24.dp
