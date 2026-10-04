package com.freedu.kidslearn.ui.navigation

import com.freedu.kidslearn.domain.model.ModuleType
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Guards quiz-route id encoding.
 *
 * Bangla/Arabic lesson ids carry non-ASCII glyphs (`BANGLA_V_অ`), which must
 * be encoded for the nav path and decoded back before matching catalog ids.
 * Parsing the still-encoded form matched nothing, leaving every non-English
 * single-letter quiz on an eternal loading spinner.
 */
class RouteTest {

    @Test
    fun `bangla and arabic ids round-trip through the route`() {
        listOf("BANGLA_V_অ", "BANGLA_C_খ", "ARABIC_V_ا", "ARABIC_C_ب").forEach { id ->
            val route = Route.Quiz.createRoute(ModuleType.BANGLA, listOf(id))
            val encoded = route.substringAfter("quiz/BANGLA/").substringBefore("/")
            assertThat(Route.Quiz.parseItemIds(encoded)).containsExactly(id)
        }
    }

    @Test
    fun `empty and missing filters parse to no ids`() {
        assertThat(Route.Quiz.parseItemIds(Route.Quiz.NO_ITEMS)).isEmpty()
        assertThat(Route.Quiz.parseItemIds("")).isEmpty()
        assertThat(Route.Quiz.parseItemIds(null)).isEmpty()
        assertThat(
            Route.Quiz.parseItemIds(
                Route.Quiz.createRoute(ModuleType.ENGLISH).split("/")[2],
            ),
        ).isEmpty()
    }

    @Test
    fun `multiple ids survive the comma join`() {
        val ids = listOf("ENGLISH_A", "ENGLISH_B", "ENGLISH_C")
        val route = Route.Quiz.createRoute(ModuleType.ENGLISH, ids)
        val encoded = route.substringAfter("quiz/ENGLISH/").substringBefore("/")
        assertThat(Route.Quiz.parseItemIds(encoded)).containsExactlyElementsIn(ids)
    }
}
