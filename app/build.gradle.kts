import org.gradle.kotlin.dsl.androidTestImplementation
import org.gradle.kotlin.dsl.testImplementation


plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.moorixlabs.televault"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.moorixlabs.televault"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    packaging {
        resources {
            excludes += setOf(
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE",
                "META-INF/LICENSE.txt",
                "META-INF/LICENSE.md",
                "META-INF/LICENSE.markdown",
                "META-INF/NOTICE",
                "META-INF/NOTICE.txt",
                "META-INF/NOTICE.md",
                "META-INF/NOTICE.markdown", // 👈 this one fixes your new error
                "META-INF/ASL2.0",
                "META-INF/INDEX.LIST"
            )
        }
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.room.common.jvm)
    implementation(libs.room.runtime)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    // Telegram Bots library
    implementation(libs.telegrambots)
// Required for the library to function correctly
    implementation(libs.slf4j.simple)
    testImplementation(libs.junit)
    androidTestImplementation(libs.junit.v130)
    androidTestImplementation(libs.espresso.core.v370)
    annotationProcessor(libs.room.compiler)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.lifecycle.viewmodel.ktx)
    // ViewModel
    implementation(libs.lifecycle.viewmodel)
// LiveData
    implementation(libs.lifecycle.livedata)
    implementation(libs.glide)
    annotationProcessor(libs.compiler)
    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")
    implementation(libs.photoview)

}