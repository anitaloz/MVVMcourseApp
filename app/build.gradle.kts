plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    kotlin("plugin.serialization") version "2.1.10"
    id("kotlin-parcelize")
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
    id ("androidx.navigation.safeargs.kotlin")
}

android {
    namespace = "com.example.mvvmcourseapp"
    compileSdk = 35

//    testOptions {
//        unitTests {
//            isIncludeAndroidResources = true
//            all {
//                // ✅ ОТКЛЮЧИТЕ параллельное выполнение
//                it.maxParallelForks = 1
//                it.forkEvery = 0
//                // ✅ Используйте JUnit Platform
//                it.useJUnitPlatform()
//            }
//        }
//    }

    defaultConfig {
        applicationId = "com.example.mvvmcourseapp"
        minSdk = 24
        targetSdk = 35
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
        sourceCompatibility = JavaVersion.VERSION_17 // Или выше
        targetCompatibility = JavaVersion.VERSION_17 // Или выше
    }
    kotlinOptions {
        jvmTarget = "17" // Или выше
    }
    buildFeatures {
        viewBinding = true // Если используете View Binding
        //noinspection DataBindingWithoutKapt
        dataBinding = false
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.hilt.android)
    //implementation(libs.androidx.compiler)
    ksp(libs.hilt.android.compiler)
    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.annotation)
    implementation(libs.androidx.legacy.support.v4)
    implementation(libs.androidx.fragment.ktx)
    ksp(libs.androidx.room.compiler) // Используем KSP для Room
    implementation(libs.androidx.room.ktx)
    // Kotlin Coroutines
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    // Jetpack Compose integration
    implementation(libs.androidx.navigation.compose)

    // Views/Fragments integration
    implementation(libs.androidx.navigation.fragment)
    implementation(libs.androidx.navigation.ui)
    // Feature module support for Fragments
    implementation(libs.androidx.navigation.dynamic.features.fragment)
    implementation(libs.androidx.lifecycle.viewmodel.ktx.v262)
    // Testing Navigation
    androidTestImplementation(libs.androidx.navigation.testing)

    // JSON serialization library, works with the Kotlin serialization plugin
    implementation(libs.kotlinx.serialization.json)

    // Lifecycle KTX (ViewModelScope, LiveData)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation (libs.androidx.security.crypto)
    // Testing
//    testImplementation(libs.junit)
//    androidTestImplementation(libs.androidx.test.ext.junit)
//    androidTestImplementation(libs.androidx.test.espresso.core)
    // Тестирование
//    testImplementation (libs.androidx.core)
//    testImplementation(libs.kotlinx.coroutines.test.v171)
//    testImplementation(libs.androidx.core.testing)
//
//    // Mocking
//    testImplementation (libs.mockk)
//    testImplementation (libs.mockk.agent.jvm)
//
//    // AndroidX Testing
//    androidTestImplementation (libs.androidx.junit.v130)
//    androidTestImplementation (libs.androidx.espresso.core.v370)
//    androidTestImplementation (libs.androidx.ui.test.junit4)
//    testImplementation(kotlin("test"))


}

