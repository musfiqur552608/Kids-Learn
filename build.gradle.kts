// Top-level build file. Plugin versions live in `gradle/libs.versions.toml`;
// they are declared here with `apply false` so each subproject can opt in
// via `alias(libs.plugins.…)` while Gradle still resolves a single version.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
}
