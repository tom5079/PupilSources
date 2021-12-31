
plugins {
    id("com.android.library")
    id("kotlin-android")
}

android {
    compileSdk = AndroidConfig.COMPILE_SDK

    defaultConfig {
        minSdk = AndroidConfig.MIN_SDK
        targetSdk = AndroidConfig.TARGET_SDK

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        getByName("release") {
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
        freeCompilerArgs += "-Xopt-in=kotlin.RequiresOptIn"
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

    implementation(AndroidX.APPCOMPAT)
    implementation(AndroidX.NAVIGATION_COMPOSE)
    implementation(AndroidX.DATASTORE)

    implementation(JetpackCompose.FOUNDATION)
    implementation(JetpackCompose.UI)
    implementation(JetpackCompose.UI_UTIL)
    implementation(JetpackCompose.ANIMATION)
    implementation(JetpackCompose.MATERIAL)
    implementation(JetpackCompose.MATERIAL_ICONS)

    implementation(Accompanist.INSETS)
    implementation(Accompanist.INSETS_UI)
    implementation(Accompanist.SYSTEM_UI_CONTROLLER)
    implementation(Accompanist.DRAWABLE_PAINTER)

    implementation(Misc.DOCUMENTFILEX)
    implementation(Misc.SUBSAMPLEDIMAGE)

    implementation(KtorClient.CORE)
    implementation(KtorClient.OKHTTP)
    implementation(KtorClient.SERIALIZATION)

    implementation(Kodein.DI)
    implementation(Kodein.LOG)

    implementation(Misc.PROTOBUF)
}