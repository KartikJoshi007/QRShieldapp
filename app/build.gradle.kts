plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "com.example.qrshieldapp"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.qrshieldapp"
        minSdk = 31
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)
    implementation(libs.firebase.auth.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation ("com.github.yuriy-budiyev:code-scanner:2.3.0")
    implementation ("com.microsoft.onnxruntime:onnxruntime-android:1.15.1")
    implementation("com.microsoft.onnxruntime:onnxruntime-android:1.16.3")



    implementation(platform("com.google.firebase:firebase-bom:32.7.1")) // Firebase BOM (Always use latest version)

    // Firebase Core Services
    implementation("com.google.firebase:firebase-analytics-ktx")
    implementation("com.google.firebase:firebase-auth-ktx")  // Authentication
    implementation("com.google.firebase:firebase-firestore-ktx") // Firestore Database
    implementation("com.google.firebase:firebase-database-ktx") // Realtime Database
    implementation("com.google.firebase:firebase-storage-ktx") // Firebase Storage
    implementation("com.google.firebase:firebase-messaging-ktx") // Firebase Cloud Messaging (Push Notifications)
    implementation("org.jetbrains.kotlin:kotlin-stdlib:2.0.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3") // Compatible Coroutines version
    implementation ("org.tensorflow:tensorflow-lite:2.12.0")



}