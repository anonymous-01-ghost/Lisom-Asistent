# Lisom Assistant SDK

**Status:** Initial development scaffold  
**Version:** 0.1.0-SNAPSHOT  
**Primary technology:** Kotlin Multiplatform  
**Processing model:** Local and offline-only

Lisom Assistant is an embeddable, reusable voice-engine SDK. It provides speech recognition, wake-word detection, local command interpretation, and speech synthesis through a platform-neutral API.

This repository contains **Lisom Assistant only**. It does not contain Lynox Music, a music player, a music catalogue, provider integrations, backend services, or cloud infrastructure.

## Product constraints

- All speech processing and command interpretation must run locally.
- No remote speech, language, or text-to-speech APIs.
- No server, account, subscription, or mandatory network dependency.
- Push-to-talk is supported as the first interaction mode.
- The wake phrase is configurable, with **Oye Lisom** as the default.
- Public contracts are separated from platform-specific implementations.
- The first supported platforms are Android, Windows (JVM), and iOS.

## Repository modules

| Module | Responsibility |
| --- | --- |
| `lisom-api` | Public SDK contracts and configuration |
| `lisom-models` | Commands, events, states, and shared data types |
| `lisom-core` | Runtime lifecycle and orchestration |
| `lisom-speech` | Offline automatic speech recognition abstraction |
| `lisom-wakeword` | Local wake-word detection abstraction |
| `lisom-nlu` | Local Spanish command interpretation |
| `lisom-tts` | Offline speech synthesis abstraction |
| `lisom-platform` | Android, Windows/JVM, and iOS adapters |
| `lisom-testing` | Shared test utilities |
| `lisom-demo` | Small developer sample; not a host product |

## Development status

The initial commit establishes the module layout, public contracts, a first rule-based Spanish command interpreter, and build configuration. It does not yet include microphone capture, Sherpa-ONNX bindings, a trained wake-word model, or a TTS engine. Those are implemented and validated in subsequent phases.

## Build requirements

- JDK 17
- Gradle 9.7
- Android SDK 36 for Android compilation
- macOS and Xcode for iOS compilation

The project uses Kotlin 2.4.21 and the Android-KMP library plugin. See [Installation](docs/INSTALLATION.md) before building.

## Documentation

- [Architecture](docs/ARCHITECTURE.md)
- [Public API](docs/API.md)
- [Installation](docs/INSTALLATION.md)
- [Platform support](docs/PLATFORM_SUPPORT.md)
- [Offline mode](docs/OFFLINE_MODE.md)
- [Development roadmap](docs/ROADMAP.md)

## License

No distribution license has been selected yet. Do not assume that the project or its future model assets are licensed for redistribution until the license review is completed.
