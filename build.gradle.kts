plugins {
    id("com.android.application") version "8.2.2" apply false
    id("org.jetbrains.kotlin.android") version "1.9.0" apply false
    id("com.chaquo.python") version "15.0.0" apply false
    //     implementation "androidx.work:work-runtime-ktx:2.7.1"  // Update to the latest version
}

buildscript {
    dependencies {
        classpath("com.android.tools.build:gradle:8.0.2")

    }
}

tasks.register<Delete>("clean") {
    delete(rootProject.buildDir)
}