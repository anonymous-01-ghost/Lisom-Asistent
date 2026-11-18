# Sherpa-ONNX Integration Plan

Status: implementation design; no native runtime or model is bundled yet.

## Goal

Add local speech recognition to Lisom Assistant without changing its public, platform-neutral API. Audio must remain on-device. The SDK must not require accounts, remote services, API keys, paid hosting, or network access at runtime.

## Existing boundary

The shared speech contract is:

```kotlin
interface SpeechRecognizer {
    suspend fun recognize(audio: Flow<PcmAudioFrame>): SpeechRecognitionResult
}
```

Platform-specific code must implement this contract. Do not expose Sherpa-ONNX types in `lisom-api`, `lisom-models`, or common source sets. Keep the existing command interpreter and host-app command API independent from the ASR engine.

## Proposed module split

- `lisom-speech`: common audio frame, recognizer contract, result and lifecycle abstractions.
- `lisom-speech-sherpa`: shared adapter configuration and model metadata only, if these can remain dependency-free.
- Android source set/adapter: package the compatible Sherpa Android AAR and JNI libraries; create and release recognizer instances on a controlled dispatcher.
- JVM/Windows adapter: use the compatible Sherpa JVM API plus matching Windows native runtime artifacts. Verify x64/arm64 support before publishing.
- iOS adapter: use Sherpa C API/framework built for device and simulator, bridged through Kotlin/Native cinterop. This target must be built and tested on macOS with Xcode; it cannot be validated from a Windows-only environment.

Do not put Android AAR or desktop native binaries in commonMain. Keep each native dependency in its platform source set or dedicated platform module.

## Audio contract requirements

Before wiring a native recognizer, make the PCM contract explicit and test it:

- signed 16-bit little-endian PCM;
- mono channel;
- fixed sample rate selected to match the chosen model (do not silently resample);
- frame timestamps or a documented continuous-stream ordering guarantee;
- bounded frame sizes and back-pressure;
- explicit end-of-utterance/cancellation behavior;
- deterministic release of native streams and recognizer handles.

If current `PcmAudioFrame` does not encode these properties, extend it compatibly before integrating a model. Do not infer sample rate or channel count from the byte array.

## Recognition pipeline

1. Platform microphone capture obtains permission and produces the documented PCM format.
2. Optional local VAD/endpointing determines when an utterance is complete.
3. The platform adapter feeds PCM frames to Sherpa-ONNX.
4. The adapter returns `SpeechRecognitionResult` with normalized text, language tag and a calibrated confidence value.
5. Existing common NLU converts text into `RecognizedCommand`.
6. The host app executes commands through its own playback/catalog/lyrics interfaces.

The recognizer must not access the network. Model loading should accept an app-provided local file/resource path; do not auto-download models at runtime.

## Model and licensing gate

The Spanish Moonshine model previously evaluated is only a candidate, not an approved dependency. Before shipping it, record the exact model revision, source URL, checksum, model license, redistribution terms, size, supported sample rate, and measured Spanish recognition quality. Review the model license separately from the Sherpa-ONNX library license.

Do not commit model binaries or native runtime binaries to this repository. Document how the consuming app supplies the model asset and how packaging/licensing notices are delivered.

## Validation gates

- Unit tests: PCM validation, frame ordering, cancellation, empty audio, confidence bounds, resource cleanup.
- Android: physical-device microphone capture and offline recognition with airplane mode enabled.
- Windows: JVM/native loading and offline recognition on a supported Windows architecture.
- iOS: device and simulator builds on macOS; on-device offline recognition and lifecycle tests.
- Quality: Spanish test corpus with word error rate, latency, memory, cold-start time, and false activation measurements.
- CI: keep common/JVM tests green; add platform jobs only when their toolchains and native artifacts are reproducibly available.

## Wake word and TTS are separate milestones

A working ASR adapter does not implement the exact wake phrase `Oye Lisom`. Keyword spotting needs a dedicated local model and false-accept/false-reject evaluation. Likewise, offline TTS requires its own Spanish voice/model and license review. Neither feature should be marked complete merely because Sherpa-ONNX is present.

## Current limitations

This design does not claim that microphone capture, Sherpa inference, wake-word detection, or TTS is implemented. The next coding milestone is to formalize and test the PCM frame contract, then implement and validate Android first, followed by Windows. iOS requires a macOS/Xcode build environment.
