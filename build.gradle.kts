// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.koin.compiler) apply false
    kotlin("plugin.serialization") version "2.3.20" apply false
    id("com.google.devtools.ksp") version "2.3.11" apply false
}