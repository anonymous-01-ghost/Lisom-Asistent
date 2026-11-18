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

## Local Spanish TTS model setup

The TTS adapters do not download or bundle voice assets. The host application must install a compatible directory such as `vits-piper-es_MX-claude-high` containing:

- the voice `.onnx` file;
- `tokens.txt`;
- `espeak-ng-data`.

The provisional model archive is documented by [Sherpa-ONNX](https://k2-fsa.github.io/sherpa/onnx/tts/all/Spanish/vits-piper-es_MX-claude-high.html). Pass the model filename and, preferably, its SHA-256 through `expectedModelSha256`. Keep the model, dataset notices, and licence files outside this SDK repository until the formal redistribution review is complete.

## Runtime network policy

Gradle may need network access during the first build to retrieve build plugins and dependencies. That is build-time tooling only. Lisom's runtime audio, ASR, wake-word detection, NLU, and TTS are required to run locally without Internet access.
