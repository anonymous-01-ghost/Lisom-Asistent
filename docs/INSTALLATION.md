# Installation and build

## Requirements

- JDK 17 or newer.
- Gradle 9.7.0 for the checked-in Kotlin 2.4.21 / Android Gradle Plugin 9.3.1 combination.
- Android SDK 36 for Android target compilation.
- macOS with Xcode 26.4 for iOS native compilation.

The Kotlin Multiplatform compatibility table lists Gradle 9.7.0 as the upper supported Gradle version for Kotlin 2.4.20–2.4.21. The repository pins that compatible version rather than a newer Gradle release.

## Windows: run the NLU demo

Install a compatible JDK and Gradle, clone the private repository, then run:

```powershell
gradle :lisom-nlu:jvmTest
gradle :lisom-demo:run --args="Oye Lisom reproduce música de Morat"
```

## Compile the JVM target

```powershell
gradle :lisom-platform:compileKotlinJvm
gradle :lisom-core:compileKotlinJvm
```

## Android and iOS

Android tasks require Android SDK 36. iOS targets can be compiled only on macOS with the required Xcode toolchain. A Windows host can edit shared and JVM code but cannot produce iOS binaries.

## Runtime network policy

Gradle may need network access during the first build to retrieve build plugins and dependencies. That is build-time tooling only. Lisom's runtime audio, ASR, wake-word detection, NLU, and TTS are required to run locally without Internet access.
