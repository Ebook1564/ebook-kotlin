plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp") version "2.0.21-1.0.25"
    id("kotlin-parcelize")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.freeebookbundle"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.freeebookbundle"
        minSdk = 24
        targetSdk = 36
        versionCode = 54
        versionName = "50.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        dataBinding = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Desugaring for Java 8+ features used by Readium 3.x
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.4")
    
    // Joda Time - required for Readium
    implementation("joda-time:joda-time:2.12.7")
    implementation("org.joda:joda-convert:2.2.2")

    // AndroidX Core & Compose
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation(platform("androidx.compose:compose-bom:2024.10.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.8.3")
    implementation("androidx.fragment:fragment-ktx:1.8.5")
    implementation("androidx.webkit:webkit:1.12.1")

    // Image Loading
    implementation("io.coil-kt:coil-compose:2.7.0")

    // Paging 3 (Production Scale Lists)
    val pagingVersion = "3.3.2"
    implementation("androidx.paging:paging-runtime-ktx:$pagingVersion")
    implementation("androidx.paging:paging-compose:$pagingVersion")

    // JSON Parsing
    implementation("com.google.code.gson:gson:2.11.0")

    // Local Storage (Room - replacement for Hive)
    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    implementation("androidx.room:room-paging:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")

    // Readium
    val readiumVersion = "3.1.2"
    implementation("org.readium.kotlin-toolkit:readium-shared:$readiumVersion")
    implementation("org.readium.kotlin-toolkit:readium-streamer:$readiumVersion")
    implementation("org.readium.kotlin-toolkit:readium-navigator:$readiumVersion")
    implementation("org.readium.kotlin-toolkit:readium-opds:$readiumVersion")
    implementation("org.readium.kotlin-toolkit:readium-lcp:$readiumVersion")

    // HTML Parsing
    implementation("org.jsoup:jsoup:1.18.1")

    // Networking
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.joda:joda-convert:2.2.2")

    // AppCompat for locale support
    implementation("androidx.appcompat:appcompat:1.7.0")

    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
    implementation("com.google.firebase:firebase-analytics-ktx")
    implementation("com.google.firebase:firebase-messaging-ktx")
    implementation("com.google.firebase:firebase-config-ktx")

    // Play Core for In-App Reviews
    implementation("com.google.android.play:review-ktx:2.0.0")

    // Local Server (used by some reader components)
    implementation("org.nanohttpd:nanohttpd:2.3.1")

}
