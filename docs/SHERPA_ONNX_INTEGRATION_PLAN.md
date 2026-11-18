# Sherpa-ONNX Integration Plan

**Status:** Initial Android and Windows/JVM offline ASR adapters have been added. Android source compilation passed CI run `44383261711`; Windows/JVM compilation and PCM conversion tests passed run `44387376519`. Physical-device ASR validation is pending.

## Goal

Add local speech recognition to Lisom Assistant without changing its public, platform-neutral API. Audio must remain on-device. The SDK must not require accounts, remote services, API keys, paid hosting, or network access at runtime.

## Existing boundary

The shared speech contract is:

```kotlin
interface SpeechRecognizer {
    suspend fun recognize(audio: Flow<PcmAudioFrame>): SpeechRecognitionResult
}
```

Platform-specific code implements this contract. Sherpa-ONNX types remain out of `lisom-api`, `lisom-models`, and common source sets. Keep the command interpreter and host-app command API independent from the ASR engine.

## Current adapters

- Android: `AndroidAudioInput` uses `AudioRecord`; `SherpaMoonshineSpeechRecognizer` uses Sherpa-ONNX 2.0.1 through the Android AAR.
- Windows/JVM: `WindowsAudioInput` uses Java Sound; `SherpaMoonshineSpeechRecognizer` uses the Sherpa JVM API and Windows x64 native libraries.
- Both recognizers use the Spanish Moonshine v2 model from local files, CPU inference, bounded utterances, and a shared end-of-stream marker.
- Native engine artifacts are pinned by SHA-256 and downloaded only during builds. No model is downloaded by the SDK, and no audio is uploaded.
- Android compilation and Windows JVM adapter compilation/tests pass in GitHub Actions. Target-hardware tests remain pending.

## Proposed platform boundaries

- `lisom-speech`: common audio-frame, recognizer contract, result, and lifecycle abstractions.
- Android source set: Android microphone and Sherpa AAR bindings.
- JVM source set: Java Sound microphone and Sherpa JVM/native bindings for Windows x64.
- iOS source set: Sherpa C API/framework built for device and simulator, bridged through Kotlin/Native cinterop. This requires macOS and Xcode.

Do not put Android AAR or desktop native binaries in `commonMain`. Keep every native dependency in its platform source set or dedicated platform module.

## Audio contract

The common PCM contract is mono, signed 16-bit samples at 16 kHz. Platform adapters must preserve these properties and must not silently resample. Audio delivery must provide bounded frames, a documented continuous ordering guarantee, back-pressure behavior, and explicit end-of-utterance/cancellation semantics. Missing sample-rate or channel metadata must never be guessed from the audio buffer.

## Recognition pipeline

1. Platform microphone capture obtains the required permission and produces the documented PCM format.
2. Push-to-talk stop emits an explicit end-of-stream marker.
3. The platform adapter validates and buffers one bounded utterance.
4. Sherpa-ONNX runs offline inference against the locally installed Spanish model.
5. The adapter returns text and language. Confidence remains `null` because the selected result does not expose a calibrated score.
6. Existing common NLU converts text into `RecognizedCommand`.
7. The host app executes commands through its own playback, catalogue, and lyrics interfaces.

The recognizer must not access the network. The host or an explicit setup flow installs model files locally and passes their path to the adapter.

## Model and licensing gate

The Spanish Moonshine model previously evaluated remains a candidate, not an approved dependency. Before shipping, record the exact model revision, source URL, checksum, model license, redistribution terms, size, supported sample rate, and measured Spanish recognition quality. Review the model license separately from the Sherpa-ONNX engine and native binary licences.

Do not commit model binaries or native runtime binaries to this repository. Document how the consuming app supplies model assets and delivers required notices.

## Validation gates

- Unit tests: PCM validation and conversion, frame ordering, cancellation, empty audio, confidence bounds, and resource cleanup.
- Android: physical-device microphone capture and offline recognition with airplane mode enabled.
- Windows: Windows x64 microphone capture, native DLL loading, and offline recognition on a clean machine.
- iOS: device and simulator builds on macOS; on-device offline recognition and lifecycle tests.
- Quality: Spanish command corpus with transcript assertions, latency, real-time factor, memory, cold-start time, and false activation measurements.
- CI: keep common/JVM tests green and build platform adapters using pinned native artifacts.

## Wake word and TTS are separate milestones

A working ASR adapter does not implement the exact wake phrase `Oye Lisom`. Keyword spotting needs a dedicated local model and false-accept/false-reject evaluation. Offline TTS requires its own Spanish voice/model and licence review. Neither feature should be marked complete merely because Sherpa-ONNX is present.

## Current limitations

- Android compilation has passed but physical microphone/model validation remains pending.
- Windows x64 adapter compilation and PCM tests have passed; physical microphone capture and model inference remain pending.
- Both ASR adapters currently decode complete utterances after push-to-talk ends; partial streaming transcripts are not produced.
- No wake-word model, post-keyword speech handoff, offline TTS engine, or iOS native adapter is approved yet.
- Sherpa engine and model licensing must be reviewed before redistribution.
