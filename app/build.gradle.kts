import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

/**
 * Bundled native libraries we do not use. Lottie ships `liblottie.so` for 4 ABIs;
 * this app is pure Kotlin/Compose so none of them are needed. Stripping them keeps
 * the APK materially smaller for the low-end devices this app targets.
 */
val unusedNativeLibs = setOf("**/liblottie*.so")

android {
    namespace = "com.freedu.kidslearn"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.freedu.kidslearn"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "com.freedu.kidslearn.HiltTestRunner"

        vectorDrawables { useSupportLibrary = true }
    }

    signingConfigs {
        // Debug keeps the default auto-generated keystore. A release keystore is
        // intentionally NOT committed; supply it via `local.properties` (see README).
        getByName("debug") {
            storeFile = file(System.getProperty("user.home") + "/.android/debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            // Uncomment to assemble a locally signed release build:
            // signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        // Allows java.time on minSdk 24 (desugared). Used by the streak calculator
        // so we can compare LocalDate values without a 3rd-party library.
        isCoreLibraryDesugaringEnabled = true
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += unusedNativeLibs
        }
    }

    androidResources {
        // Ship only English + Bangla translations. The bundled Bengali font family
        // means the app renders correctly for Bangla-speaking families with no
        // network and no downloadable language packs.
        localeFilters += listOf("en", "bn")
    }

    lint {
        // `UnsafeOptInUsageError` etc. are not relevant here; fail the build on
        // correctness issues that matter for a kids' app shipped to parents.
        abortOnError = true
        warningsAsErrors = false
        disable += setOf("GradleDependency", "AndroidGradlePluginVersion", "ObsoleteLintCustomCheck")
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
    }

}

// Room + Hilt both use KSP (Kotlin Symbol Processing). KSP is a drop-in
// replacement for kapt that is an order of magnitude faster and plays nicely
// with the Compose compiler plugin.
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("room.incremental", "true")
    arg("room.generateKotlin", "true")
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
        freeCompilerArgs.addAll(
            "-opt-in=kotlin.RequiresOptIn",
        )
    }
}

dependencies {
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")

    // --- AndroidX foundation ---------------------------------------------------
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.kotlinx.coroutines.android)

    // --- Lifecycle / ViewModel -------------------------------------------------
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // --- Compose ---------------------------------------------------------------
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.animation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material3.window.size)
    implementation(libs.androidx.compose.ui.tooling.preview)
    // Resource shrinking keeps only the icons we actually reference.
    implementation(libs.androidx.compose.material.icons.extended) {
        // Exclude unused heavy transitive deps pulled in by the icon AAR.
        exclude(group = "androidx.compose.material", module = "material-ripple")
    }
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // --- Navigation ------------------------------------------------------------
    implementation(libs.androidx.navigation.compose)

    // --- Persistence -----------------------------------------------------------
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.datastore.preferences)

    // --- Dependency injection --------------------------------------------------
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    // --- Animation -------------------------------------------------------------
    implementation(libs.lottie.compose)

    // --- Unit tests ------------------------------------------------------------
    testImplementation(libs.junit)
    testImplementation(libs.truth)
    testImplementation(libs.turbine)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)

    // Screens are rendered under Robolectric so that a bad resource reference in
    // any composable fails the build instead of the child's device.
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.test.ext.junit)
    testImplementation(libs.androidx.room.testing)
    testImplementation(libs.androidx.arch.core.testing)

    // --- Instrumented tests ----------------------------------------------------
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.junit)
    androidTestImplementation(libs.truth)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.test.core.ktx)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(libs.androidx.room.testing)
    androidTestImplementation(libs.androidx.navigation.testing)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.hilt.android.testing)
    kspAndroidTest(libs.hilt.compiler)
}
