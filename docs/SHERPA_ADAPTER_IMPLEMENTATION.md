# Sherpa-ONNX adapter implementation plan

**Status:** Android adapter source and build wiring committed; Android compilation and device validation pending. Windows and iOS remain planned.
**Pinned engine candidate:** sherpa-onnx v2.0.1.
**Constraint:** all audio capture and inference remain local; no cloud service, API key, hosted model, or paid runtime dependency.

## Verified upstream artifacts

The upstream v2.0.1 release publishes these artifacts and SHA-256 checksums:

| Target | Artifact | SHA-256 |
|---|---|---|
| Android | `sherpa-onnx-2.0.1.aar` (47.2 MB) | `a8d09d1d32f3eb8520482c6d3d43fae674681c5b050385967c70a3c2c9bf0ee7` |
| JVM API | `sherpa-onnx-jvm-2.0.1.jar` (188 KB) | `8320e2457b6e38dd624fb619d3552e11ef78ffeb394622056126b16f2fd99d12` |
| Windows x64 native | `sherpa-onnx-native-lib-win-x64-2.0.1.jar` (7.78 MB) | `536f1c09025996a780b22f972b2e56e0e8b2064e1ceb534456ea7eba0a6ae4cd` |
| Windows ARM64 native | `sherpa-onnx-native-lib-win-arm64-2.0.1.jar` (7.51 MB) | `95a2a4fe4a368fb4d9f458f68fd8ed179f9f2c07bc448185633bc199066a38bb` |

Checksums are release metadata, not proof that the binaries are licensed for every redistribution scenario. Preserve these hashes in dependency verification and verify the downloaded bytes before packaging.

Official references:
- Release assets and checksums: https://github.com/k2-fsa/sherpa-onnx/releases/tag/v2.0.1
- Java/JVM Maven example and native artifact matrix: https://github.com/k2-fsa/sherpa-onnx/blob/master/java-api-examples/maven-examples/README.md
- Android AAR build and packaging guidance: https://k2-fsa.github.io/sherpa/onnx/android/build-sherpa-onnx.html
- Kotlin API: https://k2-fsa.github.io/sherpa/onnx/kotlin-api/

## Adapter boundaries

Keep all Sherpa classes out of `commonMain`. The existing common contracts remain the SDK boundary:

- `AudioInput`: one mono, signed 16-bit PCM stream at 16 kHz.
- `SpeechRecognizer`: local ASR with endpoint/VAD handling.
- `WakeWordDetector`: local keyword spotting for the exact phrase `Oye Lisom`.
- `SpeechSynthesizer`: local TTS.

Platform adapters must own native resource lifecycle, model paths, microphone permissions, audio focus/session handling, and native error translation. The SDK must not silently download models at runtime. The host app or an explicit setup flow installs model files locally and passes their paths to the adapter.

## Android implementation progress

Initial source has been added under `lisom-platform/src/androidMain`:

- `AndroidAudioInput` captures mono PCM16/16 kHz using `AudioRecord` and emits an explicit end-of-stream frame.
- `SherpaMoonshineSpeechRecognizer` buffers one bounded push-to-talk utterance and decodes it with the Sherpa offline Moonshine v2 API.
- The v2.0.1 AAR is downloaded into the build directory and SHA-256 verified; it is not committed to Git.
- The host app supplies the local Spanish model directory and requests microphone permission at runtime.
- Android compilation has been added to CI. Source is not considered supported until CI and physical-device airplane-mode tests pass.

## Android implementation sequence

1. Add a dedicated Android-only adapter source set/module; do not place Android JNI dependencies in shared JVM source sets.
2. Consume the upstream AAR and validate the actual package/API surface against v2.0.1.
3. Implement microphone capture with Android `AudioRecord`, 16 kHz mono PCM, bounded buffers, and cancellation-safe shutdown.
4. Feed Sherpa streaming ASR in small chunks and use its endpoint/VAD mechanism to close utterances.
5. Add model-path validation and actionable errors for missing or unreadable model files.
6. Test permission denial, interruption, repeated start/stop, device rotation/process lifecycle, airplane mode, and low-memory behavior.

## Windows/JVM implementation sequence

1. Keep the JVM API dependency and Windows native runtime dependency in the Windows/JVM adapter only.
2. Select the native artifact for the actual process architecture (x64 or ARM64); fail fast with a diagnostic on unsupported architectures.
3. Implement microphone capture behind `AudioInput` without binding the SDK to one UI framework.
4. Validate native library loading on a clean Windows machine with no developer PATH assumptions.
5. Test device selection, microphone removal, repeated start/stop, and application shutdown.

The upstream Maven example documents coordinates for the JVM API and separate platform-native artifacts. Confirm the repository configuration and resolve the artifacts in a clean Windows build before adding them to the version catalog.

## iOS/Kotlin Native

Do not add an iOS dependency until a macOS/Xcode build host is available. The current release asset list does not provide an iOS Kotlin/Native framework that can simply be added to this project. Build the upstream C API for supported iOS device and simulator architectures, package it as an XCFramework, then expose the required C API through Kotlin/Native cinterop. Validate microphone permission, AVAudioSession lifecycle, interruption handling, and app-background restrictions on a real iOS device.

## Shared audio pipeline requirements

- Do not let the wake-word detector and ASR compete for independent microphone captures. One platform capture session must fan out frames to consumers.
- A wake-word event must preserve enough post-keyword audio for a user who says `Oye Lisom, reproduce ...` without an artificial pause. The current event contract does not yet define this handoff; resolve it before claiming wake-word-to-command operation.
- Bound buffering and define overflow behavior. Never accumulate unbounded PCM frames.
- Use cancellation-safe ownership: stopping or releasing the engine must stop the microphone and release every native recognizer/stream exactly once.
- Keep push-to-talk and wake-word modes mutually exclusive unless an explicit shared-session state machine is implemented.
- A recognition failure must not leave the microphone active or the engine stuck in `Listening`.

## Model and licensing gate

The Spanish Moonshine model named in `SPEECH_ENGINE_DECISION.md` remains a candidate only. Before shipping, inspect and archive the model's own LICENSE, verify permitted redistribution, record the source revision and checksums, and test Spanish command phrases. Engine, native binaries, and each model have separate licensing obligations.

No wake-word model for the exact phrase `Oye Lisom` or Spanish TTS voice is approved yet.

## Completion criteria

An adapter is complete only after:
- clean build on its target OS and architecture;
- real microphone capture and offline inference on target hardware;
- shared Spanish audio fixtures and transcript assertions;
- model-missing and permission-denied tests;
- airplane-mode test after local model installation;
- lifecycle, latency, real-time-factor, memory, and package-size measurements;
- recorded binary/model licenses and checksums.

Until then, the platform remains **planned**, not supported.
