buildscript {
    dependencies {
        classpath("com.google.gms:google-services:4.4.2")
    }
    repositories {
        google()
        mavenCentral()
    }
}

plugins {
    id("com.android.application") version "8.9.0" apply false
    id("org.jetbrains.kotlin.android") version "2.0.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21" apply false
}

tasks.register<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}
