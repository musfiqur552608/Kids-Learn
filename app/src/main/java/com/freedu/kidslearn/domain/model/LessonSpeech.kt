package com.freedu.kidslearn.domain.model

/**
 * The exact line spoken for a lesson card, in the module's own phrasing.
 *
 * One function because three places must agree byte-for-byte: the Listen
 * button ([pronounceLesson]), the quiz prompt, and the bundled-narration
 * generator (`tools/generate_narration.py` mirrors this rule in Python). Any
 * drift means the clip lookup misses and the child hears system TTS - or, on
 * most devices, the fallback chime - instead of the bundled voice.
 *
 * English names each case once ("Capital A, small a, A for Apple") rather
 * than repeating the bare letter, which is what sounded like a stutter.
 * Bangla and Arabic primers simply juxtapose letter and word.
 */
fun lessonSpeechText(
    moduleType: ModuleType,
    letter: String,
    secondary: String,
    word: String,
): String = when {
    moduleType == ModuleType.ENGLISH && secondary.isNotEmpty() ->
        "Capital $letter, small $secondary, $letter for $word"
    moduleType == ModuleType.ENGLISH -> "$letter. $letter for $word"
    else -> "$letter, $word"
}
