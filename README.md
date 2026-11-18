# Lisom Assistant SDK

**Version:** 0.1.0-SNAPSHOT  
**Status:** Android/Windows offline ASR, experimental KWS, and local TTS adapters implemented; hardware validation pending
**Technology:** Kotlin Multiplatform  
**Runtime processing:** Local and offline-only

Lisom Assistant is an embeddable voice-engine SDK for applications that need local speech recognition, wake-word activation, command interpretation, and spoken responses.

> This repository is exclusively Lisom Assistant. Lisom Play and all other host applications remain separate projects.

## Non-negotiable product rules

- Speech recognition, wake-word detection, NLU, and TTS must execute locally.
- No remote voice APIs, mandatory servers, accounts, subscriptions, or API keys.
- Push-to-talk and the configurable wake phrase **Oye Lisom** are the supported activation modes.
- The SDK emits typed commands. The host application owns its UI, media, catalogue, and command execution.
- Shared logic stays separate from native Android, Windows/JVM, and iOS adapters.

## Modules

| Module | Responsibility |
| --- | --- |
| `lisom-models` | Commands, assistant states, and shared value types |
| `lisom-api` | Public SDK configuration and contracts |
| `lisom-core` | Runtime lifecycle and orchestration |
| `lisom-speech` | Audio input and offline ASR contracts |
| `lisom-wakeword` | Local keyword detection contract |
| `lisom-nlu` | Deterministic Spanish command interpreter |
| `lisom-tts` | Offline speech synthesis contract |
| `lisom-platform` | Native microphone and ASR adapters |
| `lisom-testing` | Fakes for tests |
| `lisom-demo` | Small JVM NLU demo; not a host application |

## Implemented

- Multi-module Kotlin Multiplatform foundation and shared public contracts.
- Offline-only configuration guard and deterministic Spanish command interpreter with tests.
- Android microphone capture using `AudioRecord`, plus a Sherpa-ONNX Moonshine v2 adapter.
- Windows/JVM microphone capture using Java Sound, plus a Sherpa-ONNX Moonshine v2 adapter targeting Windows x64.
- Experimental Spanish open-vocabulary wake-word adapters for Android and Windows/JVM, using local Sherpa Zipformer KWS assets and a generated `oye lisom` keyword list.
- Local Sherpa Piper/VITS TTS adapters for Android and Windows/JVM, with host-supplied model files and platform PCM output sinks.
- A bounded 1.6-second wake-word pre-roll relay and local silence endpoint that hand the same microphone stream to ASR without reopening the microphone.
- Pinned Android, JVM and Windows native Sherpa artifacts with SHA-256 verification during build.
- GitHub Actions checks for shared JVM/NLU logic, Android source compilation, and Windows x64 ASR/KWS adapter compilation and PCM tests.

Android source compilation passed in run `44383261711`. Windows x64 adapter compilation and PCM conversion tests passed in run `44387376519`; the Android and Windows KWS adapter source also compiled in run `44388881542`, and the core handoff tests passed in run `44393655204`. Neither platform is declared fully supported until physical microphone tests and offline Spanish inference succeed.

## Still pending

- Physical-device audio capture and airplane-mode ASR validation for Android and Windows.
- Validation of the provisional local **Oye Lisom** wake-word model on Peruvian Spanish voices, fast-following command preservation, noise conditions, and false-trigger rates.
- Physical-device validation of offline Spanish TTS voice quality, interruption, and audio output lifecycle.
- iOS native implementation and compilation with macOS/Xcode.
- Dependency and model licence review and final distribution licence.

## Build requirements

- JDK 17+
- Gradle 9.7.0, supplied by the checked-in Gradle Wrapper (no global Gradle installation required).
- Android SDK 36 for Android target builds
- Windows x64 for the current Windows native runtime
- macOS with Xcode 26.4 for iOS native builds

The pinned combination is Kotlin 2.4.21 and Android Gradle Plugin 9.3.1.

## Run checks on Windows

After cloning the private repository and installing JDK 17, run the checks through the pinned Gradle Wrapper:

```powershell
.\\gradlew.bat :lisom-nlu:jvmTest
.\\gradlew.bat :lisom-speech:jvmTest
.\\gradlew.bat :lisom-core:jvmTest
.\\gradlew.bat :lisom-platform:jvmTest
.\\gradlew.bat :lisom-demo:run --args="Oye Lisom reproduce música de Morat"
```

For Windows Sherpa native runtime preparation and JVM launch configuration, see [Windows ASR](docs/WINDOWS_ASR.md).

## Documentation

- [Architecture](docs/ARCHITECTURE.md)
- [Public API](docs/API.md)
- [Installation](docs/INSTALLATION.md)
- [Platform support](docs/PLATFORM_SUPPORT.md)
- [Offline mode](docs/OFFLINE_MODE.md)
- [Android ASR](docs/ANDROID_ASR.md)
- [Windows ASR](docs/WINDOWS_ASR.md)
- [Wake-word KWS](docs/WAKE_WORD_KWS.md)
- [Runtime architecture](docs/ARCHITECTURE.md)
- [Roadmap](docs/ROADMAP.md)

## Licensing

A distribution licence has not yet been selected. Model and native-engine licences will be reviewed before bundling or redistributing those assets.
