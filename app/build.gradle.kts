@file:Suppress("UnstableApiUsage")

plugins {
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.android.application)
    alias(libs.plugins.ksp)
    alias(libs.plugins.compose)
}


android {
    namespace = "cz.jaro.rozvrh"
    compileSdk = 34

    defaultConfig {
        applicationId = "cz.jaro.rozvrheditor"
        minSdk = 33
        targetSdk = 34
        versionCode = 34
        versionName = "2.4.4"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += "-Xcontext-receivers"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    applicationVariants.forEach { variant ->
        sourceSets.getByName(variant.name) {
            java {
                srcDir("build/generated/ksp/${variant.name}/kotlin")
            }
        }
    }
    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlin.stdlib)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.bundles.androidx.navigation)
    implementation(libs.bundles.androidx.jetpack.compose)
    implementation(libs.bundles.koin)
    ksp(libs.koin.ksp.compiler)
    ksp(libs.compose.destinations.ksp)
    implementation(libs.compose.material3.datetime.pickers)

    testImplementation(libs.json)
}

tasks.withType<Test> {
    useJUnitPlatform()
}
