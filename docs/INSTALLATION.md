# Installation and build

## Requirements

- JDK 17 or newer.
- Gradle 9.7.0 is pinned by the checked-in Gradle Wrapper; no separate Gradle installation is needed.
- Android SDK 36 for Android target compilation.
- macOS with Xcode 26.4 for iOS native compilation.

The repository wrapper pins Gradle 9.7.0 for the Kotlin 2.4.21 / Android Gradle Plugin 9.3.1 build. The first wrapper run downloads that Gradle distribution.

## Windows: run the NLU demo

Install JDK 17 or newer, clone the private repository, then run:

```powershell
.\\gradlew.bat :lisom-nlu:jvmTest
.\\gradlew.bat :lisom-demo:run --args="Oye Lisom reproduce música de Morat"
```

## Compile the JVM target

```powershell
.\\gradlew.bat :lisom-platform:compileKotlinJvm
.\\gradlew.bat :lisom-core:compileKotlinJvm
```

## Android and iOS

Android tasks require Android SDK 36. iOS targets can be compiled only on macOS with the required Xcode toolchain. A Windows host can edit shared and JVM code but cannot produce iOS binaries.

## Local Spanish TTS model setup

The TTS adapters do not download or bundle voice assets. The host application must install a compatible directory such as `vits-piper-es_MX-claude-high` containing:

- the voice `.onnx` file;
- `tokens.txt`;
- `espeak-ng-data`.

The provisional model archive is documented by [Sherpa-ONNX](https://k2-fsa.github.io/sherpa/onnx/tts/all/Spanish/vits-piper-es_MX-claude-high.html). Pass the model filename and, preferably, its SHA-256 through `expectedModelSha256`. Keep the model, dataset notices, and licence files outside this SDK repository until the formal redistribution review is complete.

## Runtime network policy

Gradle may need network access during the first build to retrieve build plugins and dependencies. That is build-time tooling only. Lisom's runtime audio, ASR, wake-word detection, NLU, and TTS are required to run locally without Internet access.

## Host application: Sherpa native dependencies

The SDK prepares the pinned Sherpa binaries as a separate local Maven repository under `build/repository`. This keeps native artifacts separate from the Lisom platform library instead of nesting an AAR inside another AAR.

Prepare the required repository on the development machine:

```powershell
.\gradlew.bat :lisom-platform:prepareSherpaAndroidMaven
.\gradlew.bat :lisom-platform:prepareSherpaWindowsMaven
```

In the host application's `settings.gradle.kts`, add the generated repository (adjust the path to the location of the SDK checkout):

```kotlin
dependencyResolutionManagement {
    repositories {
        maven { url = uri("../Lisom-Asistent/build/repository") }
        google()
        mavenCentral()
    }
}
```

Add the matching dependency to the host application:

- Android: `implementation("dev.lisom.thirdparty:sherpa-onnx-android:2.0.1")`
- Windows/JVM: `implementation("dev.lisom.thirdparty:sherpa-onnx-jvm:2.0.1")`

The Windows JVM POM declares the matching Windows x64 native JAR transitively. The host must still extract the DLLs using `:lisom-platform:prepareSherpaWindowsNativeRuntime` and launch its JVM with the resulting directory on `java.library.path`. These are build-time/local dependencies; speech inference remains offline.
