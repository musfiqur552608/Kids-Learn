package com.freedu.kidslearn.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.ui.graphics.vector.ImageVector
import com.freedu.kidslearn.domain.model.ModuleType

/**
 * Every destination in the app, as a type-safe route.
 *
 * ## Why routes are objects with `createRoute` rather than raw strings
 * String routes work, but nothing catches a typo: a misspelled argument silently
 * navigates nowhere, and the failure surfaces as a blank screen in front of a
 * child. Making the route a value that builds its own pattern means the argument
 * and the pattern cannot drift apart.
 *
 * ## Argument encoding
 * Item ids contain non-ASCII glyphs (a Bangla letter, an Arabic letter) for some
 * content types. Those are URL-encoded on the way in and decoded on the way out,
 * which is what keeps the route a legal navigation path.
 */
sealed class Route(val pattern: String) {

    data object Splash : Route("splash")
    data object Home : Route("home")

    // --- Alphabet modules ------------------------------------------------------
    // A single route serves all three alphabets, parameterised by module. See
    // `AlphabetModuleScreen` for why the three are not separate graphs.
    data object AlphabetModule : Route("module/alphabet/{module}") {
        const val ARG_MODULE = "module"
        fun createRoute(module: ModuleType) = "module/alphabet/${module.name}"
    }

    data object LetterList : Route("module/alphabet/{module}/letters") {
        const val ARG_MODULE = "module"
        fun createRoute(module: ModuleType) = "module/alphabet/${module.name}/letters"
    }

    data object LetterDetail : Route("module/alphabet/{module}/letter/{itemId}") {
        const val ARG_MODULE = "module"
        const val ARG_ITEM_ID = "itemId"
        fun createRoute(module: ModuleType, itemId: String) =
            "module/alphabet/${module.name}/letter/${encode(itemId)}"
    }

    // --- Maths -----------------------------------------------------------------
    data object MathsModule : Route("module/maths")
    data object CountingDetail : Route("module/maths/counting/{number}") {
        const val ARG_NUMBER = "number"
        fun createRoute(number: Int) = "module/maths/counting/$number"
    }
    data object Shapes : Route("module/maths/shapes")

    // --- Shared quiz + result --------------------------------------------------
    /**
     * A quiz is described entirely by its arguments, which is what lets the
     * English, Bangla, Arabic and maths modules (and the games) all reuse one
     * screen and one ViewModel.
     */
    data object Quiz : Route("quiz/{module}/{itemIds}/{questionCount}/{seed}") {
        const val ARG_MODULE = "module"
        const val ARG_ITEM_IDS = "itemIds"
        const val ARG_QUESTION_COUNT = "questionCount"
        const val ARG_SEED = "seed"
        const val NO_ITEMS = "-"

        fun createRoute(
            module: ModuleType,
            itemIds: List<String> = emptyList(),
            questionCount: Int = 5,
            seed: Long = 0L,
        ): String {
            val encodedIds = if (itemIds.isEmpty()) {
                NO_ITEMS
            } else {
                itemIds.joinToString(",") { encode(it) }
            }
            return "quiz/${module.name}/$encodedIds/$questionCount/$seed"
        }
    }

    data object Result : Route("result/{module}/{correct}/{total}/{stars}/{coins}") {
        const val ARG_MODULE = "module"
        const val ARG_CORRECT = "correct"
        const val ARG_TOTAL = "total"
        const val ARG_STARS = "stars"
        const val ARG_COINS = "coins"

        fun createRoute(
            module: ModuleType,
            correct: Int,
            total: Int,
            stars: Int,
            coins: Int,
        ) = "result/${module.name}/$correct/$total/$stars/$coins"
    }

    // --- Games -----------------------------------------------------------------
    data object GamesHub : Route("games")
    data object MemoryMatch : Route("games/memory")
    data object TapTheAnswer : Route("games/tap")
    data object TimedQuiz : Route("games/timed")

    // --- Parents ---------------------------------------------------------------
    data object Progress : Route("progress")
    data object ParentZone : Route("parent")
}

/** Percent-encodes a path segment, leaving the characters routing actually uses. */
internal fun encode(value: String): String =
    java.net.URLEncoder.encode(value, Charsets.UTF_8.name())

/** Reverses [encode]. */
internal fun decode(value: String): String =
    runCatching { java.net.URLDecoder.decode(value, Charsets.UTF_8.name()) }.getOrDefault(value)

/**
 * The Home tile for a destination.
 *
 * Keeping the icon next to the route means a module can never be added to Home
 * without also being given an icon and a colour, which is the usual way a kids'
 * app ends up with a grey, unlabelled tile.
 */
data class HomeTile(
    val route: String,
    val labelRes: Int,
    val icon: ImageVector,
    val containerColor: Int,
    val moduleType: ModuleType?,
)

/** Selected / unselected tab icon pair, for the bottom bar. */
val FilledTabIcon: ImageVector = Icons.Filled.Circle
val EmptyTabIcon: ImageVector = Icons.Outlined.Circle
