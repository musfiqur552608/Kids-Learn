package com.freedu.kidslearn.domain.usecase

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDate

class GenerateParentGateUseCaseTest {

    private val useCase = GenerateParentGateUseCase()

    @Test
    fun `the prompt equals the sum`() {
        repeat(200) { index ->
            val question = useCase(seed = index.toLong())

            assertThat(question.answer).isEqualTo(question.first + question.second)
        }
    }

    @Test
    fun `the answer stays within the declared range`() {
        repeat(200) { index ->
            val question = useCase(seed = index.toLong())

            assertThat(question.answer).isAtLeast(4)
            assertThat(question.answer).isAtMost(GenerateParentGateUseCase.MAX_ANSWER)
        }
    }

    @Test
    fun `the same seed always produces the same question`() {
        // The Compose gate test relies on this.
        assertThat(useCase(seed = 42L)).isEqualTo(useCase(seed = 42L))
    }

    @Test
    fun `operands are never both multiples of five`() {
        // `5 + 5 = 10` is guessable from the answer being round.
        repeat(200) { index ->
            val question = useCase(seed = index.toLong())

            assertThat(question.first % 5).isNotEqualTo(0)
            assertThat(question.second % 5).isNotEqualTo(0)
        }
    }

    @Test
    fun `the two operands differ`() {
        repeat(200) { index ->
            val question = useCase(seed = index.toLong())

            assertThat(question.first).isNotEqualTo(question.second)
        }
    }

    @Test
    fun `the prompt is human readable`() {
        val question = useCase(seed = 7L)

        assertThat(question.prompt).isEqualTo("${question.first} + ${question.second}")
    }

    @Test
    fun `a zero seed falls back to the date so two screens differ`() {
        val first = useCase(seed = 0L, today = LocalDate.of(2026, 1, 1))
        val second = useCase(seed = 0L, today = LocalDate.of(2026, 1, 2))

        assertThat(first).isNotEqualTo(second)
    }
}
