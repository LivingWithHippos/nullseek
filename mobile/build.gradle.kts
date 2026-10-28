plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.koin.compiler)
    kotlin("plugin.serialization")
}

android {
    namespace = "com.github.livingwithhippos.nullseek"
    compileSdk =
        libs.versions.compileSdk
            .get()
            .toInt()

    defaultConfig {
        applicationId = "com.github.livingwithhippos.nullseek"
        minSdk =
            libs.versions.minSdk
                .get()
                .toInt()
        targetSdk =
            libs.versions.targetSdk
                .get()
                .toInt()
        versionCode =
            libs.versions.versionCode
                .get()
                .toInt()
        versionName = libs.versions.versionName.get()

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    flavorDimensions += "distribution"

    productFlavors {
        create("foss") {
            dimension = "distribution"
            buildConfigField("boolean", "IS_FOSS", "true")
            // donation links only ship in the FOSS build
            buildConfigField("String", "DONATE_KOFI_URL", "\"https://ko-fi.com/E1E412NFX7\"")
            buildConfigField("String", "DONATE_GITHUB_URL", "\"https://github.com/LivingWithHippos/nullseek#donate-coffee\"")
            buildConfigField("String", "SUBSCRIPTION_PRODUCT_ID", "\"\"")
        }
        create("playstore") {
            dimension = "distribution"
            buildConfigField("boolean", "IS_FOSS", "false")
            buildConfigField("String", "DONATE_KOFI_URL", "\"\"")
            buildConfigField("String", "DONATE_GITHUB_URL", "\"\"")
            buildConfigField("String", "SUBSCRIPTION_PRODUCT_ID", "\"nullseek_premium_widgets\"")
        }
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    val playstoreImplementation by configurations

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3.alpha)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.runtime)
    implementation(libs.androidx.compose.material3.adaptive.navigationSuite)
    implementation(libs.androidx.compose.materialWindow)
    implementation(libs.accompanist.adaptive)

    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.material3)
    implementation(libs.androidx.work.runtime.ktx)
    playstoreImplementation(libs.play.billing.ktx)

    implementation(libs.androidx.paging)
    implementation(libs.androidx.paging.compose)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    implementation(libs.datastore)

    implementation(libs.kotlin.serialization.core)
    implementation(libs.kotlin.serialization.json)
    implementation(libs.kotlinx.datetime)

    implementation(libs.square.logcat)
    implementation(libs.okhttp)

    implementation(libs.compose.navigation)
    implementation(libs.compose.navigation.adaptive)

    implementation(libs.torbox.client)

    implementation(platform(libs.koin.bom))
    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)
    implementation(libs.koin.annotations)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(project(":core"))
}
