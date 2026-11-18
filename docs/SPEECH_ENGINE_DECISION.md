# Speech engine evaluation and provisional decision

**Status:** Provisional technical selection — not yet integrated or validated on all target platforms.  
**Evaluation date:** 2026-11-18.

## Decision

Use **sherpa-onnx v2.0.1** as the first engine candidate for local ASR, keyword spotting, VAD, and TTS adapters.

Reasons:

- The upstream project documents local inference without an Internet connection.
- It provides Kotlin/Java and Swift APIs, plus Android, iOS, Windows, and other native targets.
- The v2.0.1 release publishes an Android AAR, a JVM API JAR, and Windows x64/arm64 native JARs.
- The engine includes ASR, keyword spotting, VAD, and TTS capabilities, avoiding separate online services.

Official references:

- Project and supported platforms/APIs: https://github.com/k2-fsa/sherpa-onnx
- Pinned release: https://github.com/k2-fsa/sherpa-onnx/releases/tag/v2.0.1
- Kotlin API documentation: https://k2-fsa.github.io/sherpa/onnx/kotlin-api/
- Swift API documentation: https://k2-fsa.github.io/sherpa/onnx/swift-api/

## Initial Spanish ASR model candidate

Evaluate **sherpa-onnx-moonshine-base-es-quantized-2026-02-27** first.

The upstream model page documents Spanish support and a package containing:

- `encoder_model.ort` — approximately 20 MB.
- `decoder_model_merged.ort` — approximately 42 MB.
- `tokens.txt` and a model `LICENSE` file.

Approximate unpacked model payload: 63 MB. This is a model-size estimate from the upstream documentation, not an installed application size.

The model page includes a microphone/VAD usage path and sample audio. It accepts mono 16-bit WAV input and supports resampling to 16 kHz in the documented example.

Model reference:
https://k2-fsa.github.io/sherpa/onnx/moonshine/models-v2.html

**Important:** the model's bundled LICENSE must be reviewed before redistribution. No model files are committed to this SDK repository.

## Target integration strategy

| Target | Candidate packaging strategy | Remaining validation |
|---|---|---|
| Android | Upstream v2.0.1 AAR in an Android-specific adapter | ABI coverage, minSdk, APK/AAB size, device memory and latency |
| Windows/JVM | Kotlin/JVM API JAR plus matching Windows native JAR | JDK compatibility, x64/arm64 loading, microphone capture |
| iOS/Kotlin Native | Build/use the upstream C API as an iOS framework and expose it through Kotlin/Native cinterop | Build with Xcode on macOS, device/simulator architectures, lifecycle and app packaging |

Do not put native engine dependencies in commonMain. Keep the public interfaces in commonMain and load platform binaries only in their platform source sets/adapters.

The existence of upstream Kotlin and Swift APIs does not by itself prove that one dependency can be consumed directly from every Kotlin Multiplatform target. iOS interop must be demonstrated with a real Xcode build.

## Wake-word plan

Use sherpa-onnx keyword spotting only after selecting or training a model that detects the exact phrase **“Oye Lisom”**.

No pretrained “Oye Lisom” model has been approved in this decision. Do not substitute a generic keyword model and claim that the custom wake phrase works. Evaluate model creation, false accepts/rejects, background CPU use, and licensing before shipping it.

## TTS plan

Sherpa-onnx has local TTS capabilities, but the Spanish voice/model has not yet been selected. Evaluate voice quality, model size, license, pronunciation, and Android/Windows/iOS availability before pinning a TTS model.

## Offline and cost constraints

- No hosted inference, cloud account, API key, VPS, or paid speech API.
- Model inference must run locally.
- Model downloads may occur during installation or app setup, but speech processing must not require Internet access.
- Runtime model loading must use local files and fail clearly when a model is missing.
- Model and native binary licenses must be reviewed independently from the engine's license.

## Exit criteria before implementation is marked complete

1. Pin and checksum the exact v2.0.1 artifacts used by each target.
2. Verify engine and model licenses for redistribution.
3. Compile and run Android, Windows/JVM, and iOS adapters on their supported build hosts.
4. Run the same Spanish audio fixtures through each target and compare transcripts.
5. Measure real-time factor, cold-start time, peak memory, and app package size.
6. Confirm airplane-mode operation after local model installation.
