plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "com.codecrafters.locationtracker"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.codecrafters.locationtracker"
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
    buildFeatures {
        viewBinding = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.play.services.base)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.database.ktx)
    implementation(libs.play.services.location)
    implementation(libs.androidx.preference.ktx)
    implementation(libs.firebase.storage.ktx)
    implementation(libs.androidx.media3.common)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation("org.osmdroid:osmdroid-android:6.1.10")
    implementation("com.google.android.gms:play-services-auth:20.4.0")
    implementation("com.google.firebase:firebase-auth:22.1.1")
    implementation("com.airbnb.android:lottie:5.2.0")
    implementation("com.google.android.material:material:1.4.0")
    implementation("com.github.bumptech.glide:glide:4.12.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.12.0")
    implementation("com.firebaseui:firebase-ui-database:8.0.1")
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")

}