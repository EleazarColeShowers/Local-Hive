import java.util.Properties

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinx.serialization)
}

// The Cloudflare Worker's address, from `localhive.apiUrl` in local.properties: the same setting the
// Android app reads into BuildConfig, written into iOS code here since iOS has no BuildConfig.
val localHiveApiUrl: String = Properties().apply {
    rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
}.getProperty("localhive.apiUrl", "")

val generateIosConfig by tasks.registering {
    val outputDir = layout.buildDirectory.dir("generated/iosConfig/kotlin")
    val apiUrl = localHiveApiUrl
    inputs.property("apiUrl", apiUrl)
    outputs.dir(outputDir)
    doLast {
        val file = outputDir.get().file("com/eleazar/localhive/IosConfig.kt").asFile
        file.parentFile.mkdirs()
        file.writeText(
            "package com.eleazar.localhive\n\n" +
                "/** Generated from local.properties by :shared:generateIosConfig. */\n" +
                "internal const val LOCALHIVE_API_URL = \"$apiUrl\"\n"
        )
    }
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Shared"
            isStatic = true
        }
    }

    sourceSets {
        androidMain.dependencies {
            // Reads the App Check token for Worker requests; the app installs the provider.
            implementation(project.dependencies.platform("com.google.firebase:firebase-bom:33.7.0"))
            implementation("com.google.firebase:firebase-appcheck")
            implementation(libs.androidx.compose.ui.tooling.preview)
            implementation(libs.androidx.compose.ui.tooling)
            implementation("io.coil-kt.coil3:coil-network-okhttp:3.0.4")
            // Video playback for posts.
            implementation("androidx.media3:media3-exoplayer:1.4.1")
            implementation("androidx.media3:media3-ui:1.4.1")
            // Photo picker and reading what it returns.
            implementation(libs.androidx.activity.compose)
            implementation("androidx.exifinterface:exifinterface:1.3.7")
            implementation(libs.koin.android)
            implementation(libs.ktor.client.okhttp)
        }
        iosMain {
            kotlin.srcDir(generateIosConfig)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
            // Coil needs a network fetcher on iOS; Android uses OkHttp's above.
            implementation("io.coil-kt.coil3:coil-network-ktor3:3.0.4")
        }
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.materialIconsExtended)
            implementation("io.coil-kt.coil3:coil-compose:3.0.4")
            implementation(libs.jetbrains.navigation.compose)
            api(libs.jetbrains.lifecycle.viewmodel.compose)
            api(libs.jetbrains.lifecycle.runtime.compose)

            // Firebase from common code; on iOS the app links the Firebase SDK itself (see iosApp).
            api(libs.gitlive.firebase.auth)
            api(libs.gitlive.firebase.firestore)
            api(libs.gitlive.firebase.messaging)

            api(libs.koin.core)
            api(libs.koin.compose)
            api(libs.koin.compose.viewmodel)

            implementation(libs.ktor.client.core)
            implementation(libs.kotlinx.serialization.json)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation("io.coil-kt.coil3:coil-network-ktor3:3.0.4")
        }
    }
}

android {
    namespace = "com.eleazar.localhive.shared"
    compileSdk = 36

    defaultConfig {
        minSdk = 24
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
