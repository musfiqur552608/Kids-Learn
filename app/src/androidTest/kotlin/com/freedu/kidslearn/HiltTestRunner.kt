package com.freedu.kidslearn

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner
import dagger.hilt.android.testing.HiltTestApplication

/**
 * Swaps [KidsLearnApplication] for Hilt's test application so `@TestInstallIn`
 * modules can replace bindings in instrumented tests.
 *
 * Referenced from `defaultConfig.testInstrumentationRunner` in `build.gradle.kts`.
 */
class HiltTestRunner : AndroidJUnitRunner() {

    override fun newApplication(
        classLoader: ClassLoader?,
        className: String?,
        context: Context?,
    ): Application = super.newApplication(classLoader, HiltTestApplication::class.java.name, context)
}
