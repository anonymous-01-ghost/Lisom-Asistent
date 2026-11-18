# Offline mode and privacy

Offline operation is a hard product requirement, not an optional optimization.

## Rules

1. No network speech recognition, wake-word, natural-language, or TTS requests.
2. No audio or transcript telemetry.
3. No mandatory account, subscription, remote server, or API key.
4. Models are loaded from local storage or bundled application assets.
5. The host application remains responsible for its own network features; those are outside Lisom's SDK boundary.

## Build-time versus runtime

Gradle and Maven repositories may be contacted during development to download open-source build dependencies. This does not create a server dependency at runtime. After the application and local models are installed, Lisom's voice pipeline must work with network connectivity disabled.

## Model distribution

Before adding a model, record its source, version, checksum, size, target architectures, licence, attribution requirements, and whether redistribution is permitted. Do not commit large model binaries until distribution and repository-size decisions are approved.

## Verification

An offline acceptance test must block network access at runtime, then verify push-to-talk transcription, wake-word detection, intent parsing, and TTS on every supported platform.
