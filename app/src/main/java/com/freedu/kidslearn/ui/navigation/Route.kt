package com.freedu.kidslearn.ui.navigation

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
    // A single route serves all three alphabets, parameterised by module, and
    // the letter list / letter detail are ViewModel stages, not destinations.
    // See `AlphabetModuleScreen` for why the three are not separate graphs.
    data object AlphabetModule : Route("module/alphabet/{module}") {
        const val ARG_MODULE = "module"
        fun createRoute(module: ModuleType) = "module/alphabet/${module.name}"
    }

    // --- Maths -----------------------------------------------------------------
    // A single route: tabs (counting / shapes / addition) are ViewModel state,
    // not destinations.
    data object MathsModule : Route("module/maths")

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

        /**
         * Reverses [createRoute]'s id encoding.
         *
         * Each id is decoded individually: Bangla/Arabic ids carry non-ASCII
         * glyphs (`BANGLA_V_অ`), and comparing the still-encoded form against
         * catalog ids matches nothing - which used to leave every non-English
         * single-letter quiz on an eternal loading spinner.
         */
        fun parseItemIds(encoded: String?): List<String> {
            if (encoded.isNullOrBlank() || encoded == NO_ITEMS) return emptyList()
            return encoded.split(",").map { decode(it) }
        }
    }

    // --- Games -----------------------------------------------------------------
    data object GamesHub : Route("games")
    data object MemoryMatch : Route("games/memory")
    data object TapTheAnswer : Route("games/tap")
    data object TimedQuiz : Route("games/timed")
    data object OddOneOut : Route("games/odd")

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
