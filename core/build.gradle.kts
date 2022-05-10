import com.google.protobuf.gradle.*

plugins {
    id("com.android.library")
    id("kotlin-android")
    `maven-publish`
    signing
    id("com.google.protobuf")
}

group = "xyz.quaver.pupil.sources"
version = "0.0.1-alpha01-DEV26"

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
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = Versions.JETPACK_COMPOSE
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
        freeCompilerArgs = freeCompilerArgs + "-Xopt-in=kotlin.RequiresOptIn"
    }
    namespace = "xyz.quaver.pupil.sources.core"
}

dependencies {
    implementation(AndroidX.NAVIGATION_COMPOSE)
    implementation(AndroidX.DATASTORE)

    implementation(KtorClient.CORE)
    implementation(KtorClient.OKHTTP)
    implementation(KtorClient.SERIALIZATION)

    implementation(Kodein.DI)
    implementation(Kodein.LOG)

    implementation(Misc.PROTOBUF)

    testImplementation(Test.JUNIT)
    androidTestImplementation(AndroidTest.JUNIT)
    androidTestImplementation(AndroidTest.RULES)
    androidTestImplementation(AndroidTest.RUNNER)
    androidTestImplementation(AndroidTest.ESPRESSO)
}

val ossrhUsername: String? by project
val ossrhPassword: String? by project

val sourceJar by tasks.registering(Jar::class) {
    archiveClassifier.set("sources")
    from(android.sourceSets["main"].java.srcDirs)
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                groupId = group.toString()
                artifactId = "core"
                version = project.version as String

                from(components["release"])
                artifact(sourceJar)

                pom {
                    name.set("core")
                    description.set("PupilSources Core library")
                    url.set("https://github.com/tom5079/PupilSources")

                    licenses {
                        license {
                            name.set("The Apache License, Version 2.0")
                            url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                        }
                    }
                    developers {
                        developer {
                            id.set("tom5079")
                            email.set("tom5079@naver.com")
                        }
                    }
                    scm {
                        connection.set("scm:git:git://github.com/tom5079/PupilSources.git")
                        developerConnection.set("scm:git:ssh://github.com:tom5079/PupilSources.git")
                        url.set("https://github.com/tom5079/PupilSources")
                    }
                }
            }
        }

        repositories {
            maven {
                val releasesRepoUrl = "https://oss.sonatype.org/service/local/staging/deploy/maven2/"
                val snapshotRepoUrl = "https://oss.sonatype.org/content/repositories/snapshots/"

                setUrl(
                    if (version.toString().endsWith("SNAPSHOT"))
                        snapshotRepoUrl
                    else
                        releasesRepoUrl
                )

                credentials {
                    username = ossrhUsername
                    password = ossrhPassword
                }
            }
        }
    }

    signing {
        sign(publishing.publications)
    }
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
