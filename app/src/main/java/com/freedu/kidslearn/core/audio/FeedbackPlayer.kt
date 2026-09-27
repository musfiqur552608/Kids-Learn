package com.freedu.kidslearn.core.audio

import com.freedu.kidslearn.domain.model.ModuleType
import com.freedu.kidslearn.domain.model.UiLanguage
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The single audio entry point for the UI layer.
 *
 * ## Why one facade instead of two injected players
 * Feedback is always *both* a sound effect and (sometimes) a spoken line, and the
 * two must be coordinated: speech must not start until the tap sound has finished,
 * or the two overlap into mud. ViewModels that injected [SoundEffectPlayer] and
 * [SpeechPlayer] separately would each have to re-derive that ordering. Routing
 * everything through [FeedbackPlayer] means the rule is implemented once.
 *
 * This class is also where the tone of the app lives: the wording of every
 * encouragement is a product decision, and it belongs in one auditable place.
 */
@Singleton
class FeedbackPlayer @Inject constructor(
    private val effects: SoundEffectPlayer,
    private val speech: SpeechPlayer,
) {

    // ------------------------------------------------------------- interaction

    fun onTap() = effects.play(SoundEffect.TAP)

    fun onNavigate() = effects.play(SoundEffect.PAGE_TURN)

    // ---------------------------------------------------------------- answers

    /**
     * Right answer: ascending arpeggio plus a spoken, specific praise.
     *
     * The praise is deliberately *varied*. Children quickly stop responding to a
     * single repeated phrase, so three equivalents are rotated.
     */
    fun onCorrect(encouragement: Encouragement = Encouragement.random()) {
        effects.play(SoundEffect.CORRECT)
        encouragement.speak(speech)
    }

    /**
     * Wrong answer.
     *
     * Positive-only by design: this says "let's try that one again", never "wrong"
     * or "no". No descending buzzer, no red X sound - the cue is a soft blip and
     * an invitation to retry.
     */
    fun onTryAgain() {
        effects.play(SoundEffect.TRY_AGAIN)
        Encouragement.TryAgain.speak(speech)
    }

    // ------------------------------------------------------------ celebrations

    fun onStarEarned() = effects.play(SoundEffect.STAR)

    fun onModuleComplete() = effects.play(SoundEffect.MODULE_COMPLETE)

    fun onBadgeUnlocked(badgeName: String) {
        effects.play(SoundEffect.BADGE_UNLOCKED)
        speech.speak(badgeName, Locales.ENGLISH)
    }

    fun onCountdownTick() = effects.play(SoundEffect.TICK)

    // ------------------------------------------------------------ pronunciation

    /** Reads a lesson's letter and its example word in the module's language. */
    fun pronounceLesson(moduleType: ModuleType, letter: String, word: String) {
        speech.speak("$letter. $letter for $word", Locales.forModule(moduleType))
    }

    /** Reads a quiz prompt, e.g. "How many?" or "3 plus 2". */
    fun pronouncePrompt(text: String, localeTag: String) = speech.speak(text, localeTag)

    // ------------------------------------------------------- mascot vocabulary

    /**
     * Mascot lines, kept as an enum so the copy can be reviewed in one place and
     * referenced from tests.
     */
    enum class Encouragement(private val english: String, private val bangla: String) {
        GreatJob("Great job!", "চমৎকার!"),
        WellDone("Well done!", "খুব ভালো!"),
        Awesome("Awesome!", "দারুণ!"),
        YouGotIt("You got it!", "পেরেছো!"),
        TryAgain("Let's try that one again", "আবার চেষ্টা করো");

        fun speak(player: SpeechPlayer, language: UiLanguage = UiLanguage.ENGLISH) {
            val text = if (language == UiLanguage.BANGLA) bangla else english
            // The Bangla greeting must be spoken with a Bangla voice, otherwise the
            // default English engine mangles it into nonsense syllables.
            player.speak(text, Locales.forLanguage(language))
        }

        companion object {
            private var index = 0

            /**
             * Round-robins rather than being random: a random praise repeats often
             * enough to feel canned, and a fixed order is easy to test.
             */
            fun random(): Encouragement {
                val value = entries[index % entries.size]
                index++
                return value
            }
        }
    }

    /** BCP-47 tags, mirroring `QuizFactory.ttsLocale`. */
    object Locales {
        const val ENGLISH = "en-US"
        const val BANGLA = "bn-BD"
        const val ARABIC = "ar-SA"

        fun forModule(moduleType: ModuleType): String = when (moduleType) {
            ModuleType.ENGLISH -> ENGLISH
            ModuleType.BANGLA -> BANGLA
            ModuleType.ARABIC -> ARABIC
            ModuleType.MATHS -> ENGLISH
        }

        fun forLanguage(language: UiLanguage): String = when (language) {
            UiLanguage.ENGLISH -> ENGLISH
            UiLanguage.BANGLA -> BANGLA
        }
    }
}
