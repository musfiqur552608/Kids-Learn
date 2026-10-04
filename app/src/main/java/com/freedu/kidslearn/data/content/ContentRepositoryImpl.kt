package com.freedu.kidslearn.data.content

import com.freedu.kidslearn.domain.model.Badge
import com.freedu.kidslearn.domain.model.BadgeKey
import com.freedu.kidslearn.domain.model.CountingItem
import com.freedu.kidslearn.domain.model.LetterItem
import com.freedu.kidslearn.domain.model.ModuleType
import com.freedu.kidslearn.domain.model.QuizQuestion
import com.freedu.kidslearn.domain.model.ShapeItem
import com.freedu.kidslearn.domain.repository.ContentRepository
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Serves lesson content from the in-APK catalogs.
 *
 * ## Why there is no database here
 * Content is immutable, read-only and identical for every install, so persisting
 * it would buy nothing and cost a migration every time a letter is added. The
 * catalogs are compiled Kotlin constants: they are verified at compile time,
 * available on the first frame (no splash spinner waiting on I/O) and impossible
 * to corrupt.
 *
 * Only *child-generated* data - stars, scores, badges - needs a database, and
 * that lives in `data/local`.
 *
 * @Singleton because the catalogs are effectively immutable constants; building
 * the lookup maps once per process avoids re-walking ~90 entries on every screen.
 */
@Singleton
class ContentRepositoryImpl @Inject constructor(
    private val quizFactory: QuizFactory = QuizFactory(),
) : ContentRepository {

    private val lettersByModule: Map<ModuleType, List<LetterItem>> = mapOf(
        ModuleType.ENGLISH to EnglishCatalog.letters,
        ModuleType.BANGLA to BanglaCatalog.letters,
        ModuleType.ARABIC to ArabicCatalog.letters,
        ModuleType.MATHS to emptyList(),
    )

    private val lettersById: Map<String, LetterItem> = lettersByModule
        .values
        .flatten()
        .associateBy { it.id }

    private val countingByNumber: Map<Int, CountingItem> =
        MathsCatalog.counting.associateBy { it.number }

    private val shapesById: Map<String, ShapeItem> = MathsCatalog.shapes.associateBy { it.id }

    override fun letters(moduleType: ModuleType): List<LetterItem> =
        lettersByModule[moduleType].orEmpty()

    override fun letter(moduleType: ModuleType, itemId: String): LetterItem? =
        lettersById[itemId]?.takeIf { it.moduleType == moduleType }

    override fun countingItems(): List<CountingItem> = MathsCatalog.counting

    override fun countingItem(number: Int): CountingItem? = countingByNumber[number]

    override fun shapeItems(): List<ShapeItem> = MathsCatalog.shapes

    /**
     * The dashboard denominator.
     *
     * Maths deliberately excludes the shapes & colours sub-module: those lessons
     * are not individually tracked in `LessonProgress` (they feed the mixed maths
     * quiz instead), so counting them here would show a module that can never
     * reach 100%.
     */
    override fun totalItems(moduleType: ModuleType): Int = when (moduleType) {
        ModuleType.ENGLISH -> EnglishCatalog.letters.size
        ModuleType.BANGLA -> BanglaCatalog.letters.size
        ModuleType.ARABIC -> ArabicCatalog.letters.size
        // Counting *and* shapes: both are completable lessons, so both count.
        // Counting only the 20 numbers while shape completions increment the
        // numerator lets the bar read "25 of 20".
        ModuleType.MATHS -> MathsCatalog.counting.size + MathsCatalog.shapes.size
    }

    override fun buildQuiz(
        moduleType: ModuleType,
        itemIds: List<String>,
        count: Int,
        seed: Long,
    ): List<QuizQuestion> = quizFactory.buildQuiz(moduleType, itemIds, count, seed)

    /**
     * Filters stored badge keys down to ones this build still understands.
     *
     * A user who downgrades (or a row written by a newer build) can leave an
     * unknown key in the table. Parsing that to an enum would throw and take the
     * whole badge screen down, so unknown keys are dropped instead.
     */
    override fun knownBadges(storedKeys: List<String>): List<Badge> {
        val today = LocalDate.now()
        return storedKeys.mapNotNull { key ->
            BadgeKey.entries.firstOrNull { it.key == key }?.let { Badge(it, today) }
        }
    }
}
