# Lisom Assistant SDK

**Version:** 0.1.0-SNAPSHOT  
**Status:** Phase 1 foundation  
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
| `lisom-platform` | Platform identification and native adapter boundary |
| `lisom-testing` | Fakes for tests |
| `lisom-demo` | Small JVM NLU demo; not a host application |

## Implemented in this foundation

- Multi-module Gradle project and version catalog.
- Common command/state models and public API.
- Offline-only configuration guard.
- Audio, ASR, wake-word, and TTS interfaces.
- Reference core coordinator with dependency injection.
- Initial Spanish rule-based NLU and unit tests.
- JVM command-line demo and CI workflow.

## Not implemented yet

No production microphone capture, Sherpa-ONNX binding, trained "Oye Lisom" model, VAD/endpointing engine, or offline TTS provider is included yet. Platform source sets currently identify the runtime but do not access the microphone. These features are next engineering phases, not capabilities being claimed as complete.

## Build requirements

- JDK 17+
- Gradle 9.7.0
- Android SDK 36 for Android target builds
- macOS with Xcode 26.4 for iOS native builds

The pinned combination is Kotlin 2.4.21 and Android Gradle Plugin 9.3.1. Gradle 9.7.0 is selected to stay within the published Kotlin Multiplatform compatibility range.

## Run on Windows

After cloning the private repository and installing JDK 17 and Gradle 9.7.0:

```powershell
gradle :lisom-nlu:jvmTest
gradle :lisom-demo:run --args="Oye Lisom reproduce música de Morat"
```

## Documentation

- [Architecture](docs/ARCHITECTURE.md)
- [Public API](docs/API.md)
- [Installation](docs/INSTALLATION.md)
- [Platform support](docs/PLATFORM_SUPPORT.md)
- [Offline mode](docs/OFFLINE_MODE.md)
- [Roadmap](docs/ROADMAP.md)

## Licensing

A distribution licence has not yet been selected. Model and native-engine licences will be reviewed before bundling or redistributing those assets.
