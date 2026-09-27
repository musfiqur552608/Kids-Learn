package com.freedu.kidslearn.data.content

import com.freedu.kidslearn.domain.model.CountingItem
import com.freedu.kidslearn.domain.model.LessonItem
import com.freedu.kidslearn.domain.model.ShapeItem
import com.freedu.kidslearn.domain.model.ModuleType

/**
 * Maths catalog: counting 1-20, plus the shapes & colours bonus sub-module.
 *
 * Every counting lesson uses a *different* emoji. Reusing one emoji for 1-20 would
 * make the "count the objects" card a wall of identical glyphs that a child
 * cannot scan; varying the object keeps the card interesting and makes it a real
 * counting exercise rather than a shape-matching one.
 */
internal object MathsCatalog {

    val counting: List<CountingItem> = listOf(
        count(1, "🍎", "One"),
        count(2, "🚗", "Two"),
        count(3, "⭐", "Three"),
        count(4, "🐟", "Four"),
        count(5, "🌸", "Five"),
        count(6, "🐝", "Six"),
        count(7, "🎈", "Seven"),
        count(8, "🐘", "Eight"),
        count(9, "🍓", "Nine"),
        count(10, "🧁", "Ten"),
        count(11, "🐤", "Eleven"),
        count(12, "🐞", "Twelve"),
        count(13, "🍕", "Thirteen"),
        count(14, "🐥", "Fourteen"),
        count(15, "🎁", "Fifteen"),
        count(16, "🦋", "Sixteen"),
        count(17, "🌈", "Seventeen"),
        count(18, "🐢", "Eighteen"),
        count(19, "🌺", "Nineteen"),
        count(20, "🎂", "Twenty"),
    )

    /**
     * Shapes and colours.
     *
     * Eight colour lessons and five shape lessons. Kept as one flat list so the
     * sub-module screen can lay them out in a single grid; the [id] prefix tells
     * the screen which grid section a tile belongs to.
     */
    val shapes: List<ShapeItem> = listOf(
        colour("Red", "🔴"),
        colour("Blue", "🔵"),
        colour("Yellow", "🟡"),
        colour("Green", "🟢"),
        colour("Orange", "🟠"),
        colour("Purple", "🟣"),
        colour("Black", "⚫"),
        colour("White", "⚪"),
        shape("Circle", "⭕"),
        shape("Square", "🟦"),
        shape("Triangle", "🔺"),
        shape("Star", "⭐"),
        shape("Heart", "❤️"),
    )

    private fun count(number: Int, emoji: String, word: String) = CountingItem(
        id = "${ModuleType.MATHS.name}_COUNT_${number.toString().padStart(2, '0')}",
        number = number,
        visualEmoji = emoji,
        word = word,
        visual = number.toString(),
    )

    private fun colour(name: String, emoji: String) = ShapeItem(
        id = "${ModuleType.MATHS.name}_COLOUR_$name",
        shapeName = name,
        colourName = name,
        visual = emoji,
    )

    private fun shape(name: String, emoji: String) = ShapeItem(
        id = "${ModuleType.MATHS.name}_SHAPE_$name",
        shapeName = name,
        // A shape lesson has no colour dimension; the UI leaves this slot blank
        // rather than inventing one.
        colourName = "",
        visual = emoji,
    )
}

/** Counts lessons of the maths module. Excludes shapes & colours. */
internal fun ModuleType.mathsCountingCount(): Int = MathsCatalog.counting.size
