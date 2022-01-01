
object AndroidConfig {
    const val COMPILE_SDK = 31
    const val MIN_SDK = 21
    const val TARGET_SDK = 31
}

object Versions {
    const val KOTLIN = "1.5.31"

    const val JETPACK_COMPOSE = "1.0.5"
    const val ACCOMPANIST = "0.20.3"

    const val KTOR_CLIENT = "1.6.7"
}

object Kotlin {
    const val STDLIB = "org.jetbrains.kotlin:kotlin-stdlib:${Versions.KOTLIN}"
    const val SERIALIZATION = "org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.2"
    const val COROUTINE = "org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.0"
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
    const val APPCOMPAT = "androidx.appcompat:appcompat:1.4.0"
    const val CORE_KTX = "androidx.core:core-ktx:1.7.0"
    const val NAVIGATION_COMPOSE = "androidx.navigation:navigation-compose:2.4.0-rc01"
    const val ROOM_RUNTIME = "androidx.room:room-runtime:2.4.0"
    const val ROOM_COMPILER = "androidx.room:room-compiler:2.4.0"
    const val ROOM_KTX = "androidx.room:room-ktx:2.4.0"
    const val DATASTORE = "androidx.datastore:datastore:1.0.0"
    const val DATASTORE_PREFERENCES = "androidx.datastore:datastore-preferences:1.0.0"
}

object Kodein {
    const val DI = "org.kodein.di:kodein-di-framework-compose:7.10.0"
    const val DI_VIEWMODEL = "org.kodein.di:kodein-di-framework-android-x-viewmodel:7.10.0"
    const val DI_VIEWMODEL_SAVEDSTATE = "org.kodein.di:kodein-di-framework-android-x-viewmodel-savedstate:7.10.0"
    const val LOG = "org.kodein.log:kodein-log:0.11.1"
}

object KtorClient {
    const val CORE = "io.ktor:ktor-client-core:${Versions.KTOR_CLIENT}"
    const val OKHTTP = "io.ktor:ktor-client-okhttp:${Versions.KTOR_CLIENT}"
    const val SERIALIZATION = "io.ktor:ktor-client-serialization:${Versions.KTOR_CLIENT}"
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
    const val COIL_COMPOSE = "io.coil-kt:coil-compose:1.4.0"
    const val PROTOBUF = "com.google.protobuf:protobuf-javalite:3.19.1"
    const val DOCUMENTFILEX = "xyz.quaver:documentfilex:0.7.1"
    const val SUBSAMPLEDIMAGE = "xyz.quaver:subsampledimage:0.0.1-alpha19-SNAPSHOT"
    const val JSOUP = "org.jsoup:jsoup:1.14.3"
    const val QUICKJS = "app.cash.zipline:zipline:1.0.0-SNAPSHOT"
    const val GUAVA = "com.google.guava:guava:31.0.1-jre"
}