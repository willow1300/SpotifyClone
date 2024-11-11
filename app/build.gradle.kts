plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    id("org.jetbrains.kotlin.kapt")
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "com.example.spotifyclone"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.spotifyclone"
        minSdk = 24
        targetSdk = 34
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    //Architectural component
    val lifecycle_version = "2.8.7"

    // ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:$lifecycle_version")
    // ViewModel utilities for Compose
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:$lifecycle_version")
    // LiveData
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:$lifecycle_version")
    // Lifecycles only (without ViewModel or LiveData)
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:$lifecycle_version")
    // Annotation processor
    kapt(libs.androidx.lifecycle.compiler)

    //Coroutine implementation
    implementation(libs.kotlinx.coroutines.android)

    //Navigation
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation("androidx.navigation:navigation-ui-ktx:2.8.3")

    //glide implementation
    implementation(libs.glide)
    kapt("com.github.bumptech.glide:compiler:4.15.1")

    //implementation of dagger hilt
    implementation(libs.hilt.android)
    kapt(libs.hilt.android.compiler)

    //implementation of timber
    implementation(libs.timber)

    //Implementation of Firebase
    // Import the BoM for the Firebase platform
    implementation(platform(libs.firebase.bom))

    // Declare the dependency for the Cloud Firestore library
    // When using the BoM, you don't specify versions in Firebase library dependencies
    implementation(libs.google.firebase.firestore)

    // Add the dependency for the Cloud Storage library
    implementation(libs.firebase.storage)

    //Implementation of Firebase coroutines

    //Implementation of ExoPlayer
    implementation(libs.androidx.media3.exoplayer.v100)
    implementation(libs.androidx.media3.ui.v100)
    implementation(libs.androidx.media3.session.v100) // Ensure this is added
    implementation(libs.androidx.media)


}