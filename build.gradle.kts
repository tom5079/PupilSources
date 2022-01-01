// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:7.0.4")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:${Versions.KOTLIN}")

        // NOTE: Do not place your application dependencies here; they belong
        // in the individual module build.gradle files
        classpath("com.google.gms:google-services:4.3.10")
        classpath("com.google.firebase:firebase-crashlytics-gradle:2.8.1")
        classpath("com.google.firebase:perf-plugin:1.4.0")
        classpath("com.google.android.gms:oss-licenses-plugin:0.10.4")
        classpath("com.google.protobuf:protobuf-gradle-plugin:0.8.18")
    }
}

tasks.register("assembleRelease") {
    dependsOn(*childProjects.values.filter { it.name !in listOf("base", "core") }.mapNotNull {
        it.tasks.findByName("assembleRelease")
    }.toTypedArray())
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}