import java.net.URL
import java.security.MessageDigest
import java.util.zip.ZipFile

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kmp)
}

val sherpaAar = layout.buildDirectory.file("third-party/sherpa-onnx-2.0.1.aar")
val sherpaJvmJar = layout.buildDirectory.file("third-party/sherpa-onnx-jvm-2.0.1.jar")
val sherpaWindowsNativeJar = layout.buildDirectory.file(
    "third-party/sherpa-onnx-native-lib-win-x64-2.0.1.jar",
)
val sherpaWindowsNativeDirectory = layout.buildDirectory.dir(
    "third-party/sherpa-onnx-win-x64",
)

fun downloadAndVerify(url: String, target: java.io.File, expectedSha256: String) {
    target.parentFile.mkdirs()
    val validExisting = target.isFile &&
        MessageDigest.getInstance("SHA-256").digest(target.readBytes())
            .joinToString("") { "%02x".format(it) } == expectedSha256

    if (!validExisting) {
        target.delete()
        URL(url).openStream().use { input ->
            target.outputStream().use { output -> input.copyTo(output) }
        }
    }

    val actualSha256 = MessageDigest.getInstance("SHA-256").digest(target.readBytes())
        .joinToString("") { "%02x".format(it) }
    check(actualSha256 == expectedSha256) {
        "Sherpa-ONNX artifact SHA-256 mismatch. Expected $expectedSha256, got $actualSha256"
    }
}

val downloadSherpaAar by tasks.registering {
    outputs.file(sherpaAar)
    doLast {
        downloadAndVerify(
            "https://github.com/k2-fsa/sherpa-onnx/releases/download/v2.0.1/sherpa-onnx-2.0.1.aar",
            sherpaAar.get().asFile,
            "a8d09d1d32f3eb8520482c6d3d43fae674681c5b050385967c70a3c2c9bf0ee7",
        )
    }
}

val prepareSherpaWindowsNativeRuntime by tasks.registering {
    outputs.files(sherpaJvmJar, sherpaWindowsNativeJar)
    outputs.dir(sherpaWindowsNativeDirectory)
    doLast {
        downloadAndVerify(
            "https://github.com/k2-fsa/sherpa-onnx/releases/download/v2.0.1/sherpa-onnx-jvm-2.0.1.jar",
            sherpaJvmJar.get().asFile,
            "8320e2457b6e38dd624fb619d3552e11ef78ffeb394622056126b16f2fd99d12",
        )
        downloadAndVerify(
            "https://github.com/k2-fsa/sherpa-onnx/releases/download/v2.0.1/sherpa-onnx-native-lib-win-x64-2.0.1.jar",
            sherpaWindowsNativeJar.get().asFile,
            "536f1c09025996a780b22f972b2e56e0e8b2064e1ceb534456ea7eba0a6ae4cd",
        )

        val nativeDirectory = sherpaWindowsNativeDirectory.get().asFile
        nativeDirectory.deleteRecursively()
        nativeDirectory.mkdirs()
        val requiredLibraries = setOf("onnxruntime.dll", "sherpa-onnx-jni.dll")
        val extractedLibraries = mutableSetOf<String>()

        ZipFile(sherpaWindowsNativeJar.get().asFile).use { archive ->
            val entries = archive.entries()
            while (entries.hasMoreElements()) {
                val entry = entries.nextElement()
                val libraryName = entry.name.substringAfterLast('/')
                if (!entry.isDirectory &&
                    entry.name.startsWith("sherpa-onnx/native/win-x64/") &&
                    libraryName in requiredLibraries
                ) {
                    archive.getInputStream(entry).use { input ->
                        nativeDirectory.resolve(libraryName).outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    extractedLibraries += libraryName
                }
            }
        }

        check(extractedLibraries == requiredLibraries) {
            "Windows Sherpa runtime is incomplete. Found $extractedLibraries"
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
        jvmMain.dependencies {
            // Pinned upstream JVM API and Windows x64 native runtime; no runtime network access.
            implementation(files(sherpaJvmJar, sherpaWindowsNativeJar))
            implementation(libs.kotlinx.coroutines.core)
        }
        commonTest.dependencies { implementation(kotlin("test")) }
    }
}

tasks.configureEach {
    when {
        name.contains("compile", ignoreCase = true) &&
            name.contains("Android", ignoreCase = true) -> dependsOn(downloadSherpaAar)
        name == "compileKotlinJvm" -> dependsOn(prepareSherpaWindowsNativeRuntime)
    }
}
