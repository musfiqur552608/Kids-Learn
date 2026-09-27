package com.freedu.kidslearn.domain.usecase

import java.time.LocalDate
import javax.inject.Inject
import kotlin.random.Random

/**
 * Generates the "what is 7 + 5?" challenge that guards the parent zone.
 *
 * ## Design notes
 * * Addition only, and no result above 20. The gate is aimed at a parent, so it
 *   should be a trivially-solvable sanity check rather than a maths test.
 * * Multiples of 5 are avoided because `5 + 5 = 10` is guessable from the answer
 *   being round.
 * * [seed] makes the question reproducible, which is what lets a Compose UI test
 *   assert on a specific prompt.
 */
class GenerateParentGateUseCase @Inject constructor() {

    data class Question(
        val first: Int,
        val second: Int,
        val answer: Int,
        val prompt: String = "$first + $second",
    )

    operator fun invoke(seed: Long = 0L, today: LocalDate = LocalDate.now()): Question {
        val random = Random(seed.takeIf { it != 0L } ?: today.toEpochDay())
        var a: Int
        var b: Int
        do {
            a = random.nextInt(2, 16)
            b = random.nextInt(2, 16)
        } while (a + b > MAX_ANSWER || a == b || a % 5 == 0 || b % 5 == 0)
        return Question(first = a, second = b, answer = a + b)
    }

    companion object {
        const val MAX_ANSWER = 20
    }
}
