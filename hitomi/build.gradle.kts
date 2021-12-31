import com.google.protobuf.gradle.*

plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("kapt")
    kotlin("plugin.serialization") version "1.6.10"
    id("com.google.protobuf")
}

extra.apply {
    set("sourceName", "hitomi.la")
    set("applicationIdSuffix", "hitomi")
    set("sourcePath", ".Hitomi")
    set("versionCode", 1)
    set("versionName", "0.0.1-alpha01")
}

android {
    compileSdk = AndroidConfig.COMPILE_SDK

    defaultConfig {
        applicationIdSuffix = extra["applicationIdSuffix"] as String
        minSdk = AndroidConfig.MIN_SDK
        targetSdk = AndroidConfig.TARGET_SDK
        versionCode = extra["versionCode"] as Int
        versionName = extra["versionName"] as String

        manifestPlaceholders.apply {
            put("sourceName", "[Pupil] ${extra["sourceName"]}")
            put("sourcePath", extra["sourcePath"]!!)
        }

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
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
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = Versions.JETPACK_COMPOSE
    }
}

dependencies {
    implementation(project(":core"))
    implementation(project(":base"))
    implementation(Kotlin.STDLIB)
    implementation(Kotlin.SERIALIZATION)
    implementation(Kotlin.COROUTINE)

    implementation(AndroidX.CORE_KTX)
    implementation(AndroidX.NAVIGATION_COMPOSE)
    implementation(AndroidX.ROOM_RUNTIME)
    annotationProcessor(AndroidX.ROOM_COMPILER)
    kapt(AndroidX.ROOM_COMPILER)
    implementation(AndroidX.ROOM_KTX)
    implementation(AndroidX.DATASTORE)

    implementation(JetpackCompose.FOUNDATION)
    implementation(JetpackCompose.UI)
    implementation(JetpackCompose.ANIMATION)
    implementation(JetpackCompose.MATERIAL)
    implementation(JetpackCompose.MATERIAL_ICONS)

    implementation(Accompanist.FLOW_LAYOUT)
    implementation(Accompanist.INSETS)
    implementation(Accompanist.INSETS_UI)
    implementation(Accompanist.SYSTEM_UI_CONTROLLER)

    implementation(Misc.COIL_COMPOSE)

    implementation(KtorClient.CORE)
    implementation(KtorClient.OKHTTP)
    implementation(KtorClient.SERIALIZATION)

    implementation(Kodein.DI)
    implementation(Kodein.DI_VIEWMODEL)
    implementation(Kodein.DI_VIEWMODEL_SAVEDSTATE)
    implementation(Kodein.LOG)

    implementation(Misc.PROTOBUF)

    implementation(Misc.JSOUP)
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:3.19.1"
    }
    generateProtoTasks {
        all().forEach { task ->
            task.builtins {
                id("java") {
                    option("lite")
                }
            }
        }
    }
}