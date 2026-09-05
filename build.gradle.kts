// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.koin.compiler) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.spotless) apply false
    kotlin("plugin.serialization") version "2.4.10" apply false
    alias(libs.plugins.android.room) apply false
}

subprojects {
    apply(plugin = "com.diffplug.spotless")
    configure<com.diffplug.gradle.spotless.SpotlessExtension> {
        kotlin {
            target("**/*.kt")
            targetExclude("${layout.buildDirectory}/**/*.kt")
            ktlint()
            // compose functions start with a capital letter
            suppressLintsFor {
                step = "ktlint"
                shortCode = "standard:function-naming"
            }
            // I don't care about having a single class inside a file with a different name
            suppressLintsFor {
                step = "ktlint"
                shortCode = "standard:filename"
            }
            // 140 characters limit is stupid, what are you buying widescreens for?
            suppressLintsFor {
                step = "ktlint"
                shortCode = "standard:max-line-length"
            }
        }
        kotlinGradle {
            target("*.gradle.kts")
            targetExclude("${layout.buildDirectory}/**/*.kt")
            ktlint()
        }
    }
}