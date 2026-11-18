import java.net.URL
import java.security.MessageDigest

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kmp)
}

val sherpaAar = layout.buildDirectory.file("third-party/sherpa-onnx-2.0.1.aar")
val downloadSherpaAar by tasks.registering {
    outputs.file(sherpaAar)
    doLast {
        val target = sherpaAar.get().asFile
        target.parentFile.mkdirs()
        val expectedSha256 = "a8d09d1d32f3eb8520482c6d3d43fae674681c5b050385967c70a3c2c9bf0ee7"
        val validExisting = target.isFile &&
            MessageDigest.getInstance("SHA-256").digest(target.readBytes())
                .joinToString("") { "%02x".format(it) } == expectedSha256

        if (!validExisting) {
            target.delete()
            URL("https://github.com/k2-fsa/sherpa-onnx/releases/download/v2.0.1/sherpa-onnx-2.0.1.aar")
                .openStream().use { input ->
                    target.outputStream().use { output -> input.copyTo(output) }
                }
        }

        val actualSha256 = MessageDigest.getInstance("SHA-256").digest(target.readBytes())
            .joinToString("") { "%02x".format(it) }
        check(actualSha256 == expectedSha256) {
            "Sherpa-ONNX AAR SHA-256 mismatch. Expected $expectedSha256, got $actualSha256"
        }
    }
}

kotlin {
    android {
        namespace = "dev.lisom.platform"
        compileSdk = libs.versions.androidCompileSdk.get().toInt()
        minSdk = libs.versions.androidMinSdk.get().toInt()
    }
    jvm()
    iosArm64()
    iosSimulatorArm64()
    iosX64()
    sourceSets {
        commonMain.dependencies {
            api(project(":lisom-api"))
            implementation(project(":lisom-speech"))
            implementation(project(":lisom-wakeword"))
            implementation(project(":lisom-tts"))
        }
        androidMain.dependencies {
            // Build-time download only; ASR inference and model loading are fully local.
            implementation(files(sherpaAar))
        }
        commonTest.dependencies { implementation(kotlin("test")) }
    }
}

tasks.configureEach {
    if (name.contains("compile", ignoreCase = true) &&
        name.contains("Android", ignoreCase = true)
    ) {
        dependsOn(downloadSherpaAar)
    }
}
