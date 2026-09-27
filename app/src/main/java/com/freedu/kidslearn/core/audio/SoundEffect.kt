package com.freedu.kidslearn.core.audio

import androidx.annotation.RawRes
import com.freedu.kidslearn.R

/**
 * Every bundled sound effect, with the raw resource it maps to.
 *
 * ## Why an enum instead of passing resource ids around
 * `R.raw.*` ids are regenerated when resources are renamed, so scattering them
 * through ViewModels makes a rename a silent behaviour change. An enum gives the
 * compiler a rename check, and gives one place to tune per-cue volume.
 *
 * ## SoundPool vs MediaPlayer
 * All of these are well under a second, and they fire while a child is tapping
 * rapidly, so [SoundEffectPlayer] uses `SoundPool`: it decodes to raw PCM once
 * and mixes in hardware, which is what makes a tap feel instant. MediaPlayer would
 * allocate a decoder per cue and is audibly laggy for this workload.
 */
enum class SoundEffect(
    @param:RawRes val rawResId: Int,
    val volume: Float = DEFAULT_VOLUME,
) {
    /** Every tappable tile. Deliberately very quiet - it fires constantly. */
    TAP(R.raw.sfx_tap, 0.45f),

    /** Right answer. */
    CORRECT(R.raw.sfx_correct),

    /**
     * Wrong answer. Intentionally a *soft, low* two-note blip rather than a
     * descending buzzer: the brief requires positive-only feedback, and a buzzer
     * reads as punishment to a pre-schooler.
     */
    TRY_AGAIN(R.raw.sfx_try_again, 0.8f),

    /** A star was earned. */
    STAR(R.raw.sfx_star),

    /** Whole module / lesson finished - plays under the confetti. */
    MODULE_COMPLETE(R.raw.sfx_module_complete),

    /** Badge unlocked. */
    BADGE_UNLOCKED(R.raw.sfx_badge_unlocked),

    /** Screen transition. Kept subtle so it never masks the mascot's voice. */
    PAGE_TURN(R.raw.sfx_page_turn, 0.4f),

    /** Last-seconds tick in the timed quiz. */
    TICK(R.raw.sfx_tick, 0.4f),
    ;

    companion object {
        const val DEFAULT_VOLUME = 0.8f
    }
}
