// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.koin.compiler) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.spotless) apply false
    kotlin("plugin.serialization") version "2.4.20" apply false
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

    pluginManager.withPlugin("com.android.application") {
        val debugKeystore = rootProject.file("debug.jks")
        val githubKeystore =
            providers
                .environmentVariable("GITHUB_KEYSTORE")
                .orElse(providers.environmentVariable("KEYSTORE"))
                .orNull
                ?.let(rootProject::file)
        val playstoreKeystore =
            providers
                .environmentVariable("PLAYSTORE_KEYSTORE")
                .orNull
                ?.let(rootProject::file)

        configure<com.android.build.api.dsl.ApplicationExtension> {
            signingConfigs {
                getByName("debug") {
                    if (debugKeystore.exists()) {
                        storeFile = debugKeystore
                        storePassword = "android"
                        keyAlias = "androiddebugkey"
                        keyPassword = "android"
                    }
                }
                create("githubRelease") {
                    if (githubKeystore?.exists() == true) {
                        storeFile = githubKeystore
                        storePassword =
                            providers
                                .environmentVariable("GITHUB_KEYSTORE_PASSWORD")
                                .orElse(providers.environmentVariable("KEYSTORE_PASSWORD"))
                                .orNull
                        keyAlias =
                            providers
                                .environmentVariable("GITHUB_KEY_ALIAS")
                                .orElse(providers.environmentVariable("KEY_ALIAS"))
                                .orNull
                        keyPassword =
                            providers
                                .environmentVariable("GITHUB_KEY_PASSWORD")
                                .orElse(providers.environmentVariable("KEY_PASSWORD"))
                                .orNull
                    }
                }
                create("playstoreRelease") {
                    if (playstoreKeystore?.exists() == true) {
                        storeFile = playstoreKeystore
                        storePassword = providers.environmentVariable("PLAYSTORE_KEYSTORE_PASSWORD").orNull
                        keyAlias = providers.environmentVariable("PLAYSTORE_KEY_ALIAS").orNull
                        keyPassword = providers.environmentVariable("PLAYSTORE_KEY_PASSWORD").orNull
                    }
                }
            }

            productFlavors.configureEach {
                when (name) {
                    "foss" -> {
                        if (githubKeystore?.exists() == true) {
                            signingConfig = signingConfigs.getByName("githubRelease")
                        }
                    }
                    "playstore" -> {
                        if (playstoreKeystore?.exists() == true) {
                            signingConfig = signingConfigs.getByName("playstoreRelease")
                        }
                    }
                }
            }

            buildTypes {
                getByName("debug") {
                    if (debugKeystore.exists()) {
                        signingConfig = signingConfigs.getByName("debug")
                    }
                    versionNameSuffix = "-dev"
                    applicationIdSuffix = ".debug"
                }
            }
        }
    }
}