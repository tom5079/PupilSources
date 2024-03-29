
object AndroidConfig {
    const val COMPILE_SDK = 33
    const val MIN_SDK = 21
    const val TARGET_SDK = 33
}

object Versions {
    const val KOTLIN = "1.7.10"
    const val COROUTINE = "1.6.4"

    const val JETPACK_COMPOSE = "1.3.0"
    const val ACCOMPANIST = "0.27.0"

    const val KTOR_CLIENT = "2.1.3"

    const val ROOM = "2.4.3"
}

object Kotlin {
    const val STDLIB = "org.jetbrains.kotlin:kotlin-stdlib:${Versions.KOTLIN}"
    const val SERIALIZATION = "org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.1"
    const val COROUTINE = "org.jetbrains.kotlinx:kotlinx-coroutines-android:${Versions.COROUTINE}"
    const val COROUTINE_TEST = "org.jetbrains.kotlinx:kotlinx-coroutines-test:${Versions.COROUTINE}"
}

object JetpackCompose {
    const val UI = "androidx.compose.ui:ui:${Versions.JETPACK_COMPOSE}"
    const val UI_TOOLING = "androidx.compose.ui:ui-tooling:${Versions.JETPACK_COMPOSE}"
    const val FOUNDATION = "androidx.compose.foundation:foundation:${Versions.JETPACK_COMPOSE}"
    const val MATERIAL = "androidx.compose.material:material:${Versions.JETPACK_COMPOSE}"
    const val MATERIAL_ICONS = "androidx.compose.material:material-icons-extended:${Versions.JETPACK_COMPOSE}"
    const val RUNTIME_LIVEDATA = "androidx.compose.runtime:runtime-livedata:${Versions.JETPACK_COMPOSE}"
    const val UI_UTIL = "androidx.compose.ui:ui-util:${Versions.JETPACK_COMPOSE}"
    const val ANIMATION = "androidx.compose.animation:animation:${Versions.JETPACK_COMPOSE}"

    const val UI_TEST = "androidx.compose.ui:ui-test-junit4:${Versions.JETPACK_COMPOSE}"
    const val UI_TEST_MANIFEST = "androidx.compose.ui:ui-test-manifest:${Versions.JETPACK_COMPOSE}"
}

object Accompanist {
    const val FLOW_LAYOUT = "com.google.accompanist:accompanist-flowlayout:${Versions.ACCOMPANIST}"
    const val APPCOMPAT_THEME = "com.google.accompanist:accompanist-appcompat-theme:${Versions.ACCOMPANIST}"
    const val INSETS = "com.google.accompanist:accompanist-insets:${Versions.ACCOMPANIST}"
    const val INSETS_UI = "com.google.accompanist:accompanist-insets-ui:${Versions.ACCOMPANIST}"
    const val DRAWABLE_PAINTER = "com.google.accompanist:accompanist-drawablepainter:${Versions.ACCOMPANIST}"
    const val SYSTEM_UI_CONTROLLER = "com.google.accompanist:accompanist-systemuicontroller:${Versions.ACCOMPANIST}"
}

object AndroidX {
    const val APPCOMPAT = "androidx.appcompat:appcompat:1.4.1"
    const val CORE_KTX = "androidx.core:core-ktx:1.7.0"
    const val NAVIGATION_COMPOSE = "androidx.navigation:navigation-compose:2.4.1"
    const val ROOM_RUNTIME = "androidx.room:room-runtime:${Versions.ROOM}"
    const val ROOM_COMPILER = "androidx.room:room-compiler:${Versions.ROOM}"
    const val ROOM_KTX = "androidx.room:room-ktx:${Versions.ROOM}"
    const val ROOM_TESTING = "androidx.room:room-testing:${Versions.ROOM}"
    const val DATASTORE = "androidx.datastore:datastore:1.0.0"
    const val DATASTORE_PREFERENCES = "androidx.datastore:datastore-preferences:1.0.0"
    const val LIFECYCLE_LIVEDATA_KTX = "androidx.lifecycle:lifecycle-livedata-ktx:2.4.1"
}

object Kodein {
    const val DI = "org.kodein.di:kodein-di-framework-compose:7.11.0"
    const val LOG = "org.kodein.log:kodein-log:0.11.1"
}

object KtorClient {
    const val CORE = "io.ktor:ktor-client-core:${Versions.KTOR_CLIENT}"
    const val OKHTTP = "io.ktor:ktor-client-okhttp:${Versions.KTOR_CLIENT}"
    const val SERIALIZATION = "io.ktor:ktor-client-serialization:${Versions.KTOR_CLIENT}"

    const val TEST = "io.ktor:ktor-client-mock:${Versions.KTOR_CLIENT}"
}

object Firebase {
    const val BOM = "com.google.firebase:firebase-bom:29.0.3"
    const val ANALYTICS = "com.google.firebase:firebase-analytics-ktx"
    const val CRASHLYTICS = "com.google.firebase:firebase-crashlytics-ktx"
    const val PERF = "com.google.firebase:firebase-perf-ktx"
}

object Test {
    const val JUNIT = "junit:junit:4.13.1"
}

object AndroidTest {
    const val CORE = "androidx.test:core:1.4.0"
    const val JUNIT = "androidx.test.ext:junit:1.1.3"
    const val RULES = "androidx.test:rules:1.4.0"
    const val RUNNER = "androidx.test:runner:1.4.0"
    const val ESPRESSO = "androidx.test.espresso:espresso-core:3.4.0"
}

object Misc {
    const val COIL_COMPOSE = "io.coil-kt:coil-compose:2.0.0-rc03"
    const val PROTOBUF = "com.google.protobuf:protobuf-javalite:3.19.1"
    const val DOCUMENTFILEX = "xyz.quaver:documentfilex:0.7.1"
    const val SUBSAMPLEDIMAGE = "xyz.quaver:subsampledimage:0.0.1-alpha22-SNAPSHOT"
    const val JSOUP = "org.jsoup:jsoup:1.14.3"
    const val DISK_LRU_CACHE = "com.jakewharton:disklrucache:2.0.2"
}