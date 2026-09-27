package com.freedu.kidslearn.domain.model

/**
 * The four learning subjects, plus the identifier used to persist progress.
 *
 * Architecture note: the *ordinal* of this enum is never persisted. Room stores
 * [name][ModuleType.name] via the [ModuleType] type converter, so reordering or
 * inserting entries later cannot corrupt existing rows.
 */
enum class ModuleType {
    ENGLISH,
    BANGLA,
    ARABIC,
    MATHS,
    ;

    /** True when the module's script must be laid out right-to-left. */
    val isRtl: Boolean
        get() = this == ARABIC

    /** Alphabet modules show letters; maths shows numbers, shapes and colours. */
    val isAlphabet: Boolean
        get() = this != MATHS
}
