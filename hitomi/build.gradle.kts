import com.google.protobuf.gradle.*
import groovy.json.JsonBuilder
import groovy.json.JsonSlurper

plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("kapt")
    kotlin("plugin.serialization") version "1.6.10"
    id("com.google.protobuf")
}

object Constants {
    const val packageName = "hitomi.la"
    const val applicationIdSuffix = "hitomi"
    const val sources = "hitomi.la:.Hitomi"
    const val versionCode = 1
    const val versionName = "0.0.1-alpha12"
}

android {
    compileSdk = AndroidConfig.COMPILE_SDK

    signingConfigs {
        create("release") {
            storeFile = File("/tmp/keystore.jks")
            storePassword = System.getenv("SIGNING_STORE_PASSWORD")
            keyAlias = System.getenv("SIGNING_KEY_ALIAS")
            keyPassword = System.getenv("SIGNING_KEY_PASSWORD")
        }
    }

    defaultConfig {
        applicationIdSuffix = Constants.applicationIdSuffix
        minSdk = AndroidConfig.MIN_SDK
        targetSdk = AndroidConfig.TARGET_SDK
        versionCode = Constants.versionCode
        versionName = Constants.versionName

        manifestPlaceholders.apply {
            put("sourceName", "[Pupil] ${Constants.packageName}")
            put("sources", Constants.sources)
        }

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")

            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
        freeCompilerArgs = freeCompilerArgs + "-Xopt-in=kotlin.RequiresOptIn"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = Versions.JETPACK_COMPOSE
    }
}

tasks.register("generateApkMetadata") {
    doLast {
        val apkDir =
            android.applicationVariants.first { it.name == "release" }
                .outputs.first().outputFile.parentFile

        val metadata = mapOf(
            "projectName" to project.name,
            "name" to Constants.packageName,
            "version" to Constants.versionName
        )

        val jsonFile = File(apkDir, "metadata.json")

        jsonFile.writeText(JsonBuilder(metadata).toString())
    }
}

tasks.register("updateVersionLedger") {
    doLast {
        val ledgerFile = File(rootDir, "versions.json")

        val ledger = runCatching {
            (JsonSlurper().parse(ledgerFile) as Map<*, *>).toMutableMap()
        }.getOrDefault(mutableMapOf())

        ledger[Constants.packageName] = mapOf(
            "projectName" to project.name,
            "name" to Constants.packageName,
            "version" to Constants.versionName
        )

        ledgerFile.writeText(JsonBuilder(ledger).toPrettyString())
    }
}

afterEvaluate {
    tasks.findByName("assembleRelease")?.apply {
        finalizedBy("generateApkMetadata")
        finalizedBy("updateVersionLedger")
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

    testImplementation(Test.JUNIT)
    androidTestImplementation(AndroidTest.JUNIT)
    androidTestImplementation(AndroidTest.RULES)
    androidTestImplementation(AndroidTest.RUNNER)
    androidTestImplementation(AndroidTest.ESPRESSO)
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