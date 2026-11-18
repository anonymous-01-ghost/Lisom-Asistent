# Android offline ASR adapter

**Status:** Initial implementation committed; Android compilation and physical-device validation are still pending.

## Components

- `AndroidAudioInput`: captures mono PCM16 at 16 kHz with Android `AudioRecord`.
- `SherpaMoonshineSpeechRecognizer`: runs Sherpa-ONNX OfflineRecognizer with the Spanish Moonshine v2 model.
- `PcmAudioFrame.isEndOfStream`: closes a push-to-talk utterance after `AudioInput.stop()`.
- `SpeechRecognitionResult.confidence`: nullable. The Sherpa offline result does not expose a calibrated confidence score, so this adapter returns `null` rather than inventing one.

The recognizer is offline batch ASR: it buffers one bounded utterance (20 seconds by default) and decodes it after push-to-talk stops. It does not yet produce partial streaming transcripts.

## Build dependency

The `lisom-platform` Android source set uses the upstream Sherpa-ONNX v2.0.1 AAR. Gradle downloads it into the module build directory during Android compilation and verifies SHA-256:

`a8d09d1d32f3eb8520482c6d3d43fae674681c5b050385967c70a3c2c9bf0ee7`

The 47.2 MB AAR is not committed to Git. This is a build-time dependency fetch only; the app performs no network requests for recognition and the recognizer does not download models.

## Model installation

The host app must install the Spanish model files locally before creating the recognizer. The model directory must contain:

- `encoder_model.ort`
- `decoder_model_merged.ort`
- `tokens.txt`

Candidate model: `sherpa-onnx-moonshine-base-es-quantized-2026-02-27` (approximately 63 MB unpacked). The model's own LICENSE must be reviewed before redistribution. Do not bundle it in the SDK repository. The model archive is published in the upstream `asr-models` release.

Pass the local directory to the adapter:

```kotlin
val modelDirectory = File(
    context.filesDir,
    "models/sherpa-onnx-moonshine-base-es-quantized-2026-02-27",
)
val audioInput = AndroidAudioInput(context)
val speechRecognizer = SherpaMoonshineSpeechRecognizer(modelDirectory)
```

The model path must refer to readable local files. Missing files fail with a specific diagnostic.

## Microphone permission

The library manifest declares `android.permission.RECORD_AUDIO`, but the host application must request runtime permission before starting capture. `AndroidAudioInput.start()` fails clearly if permission has not been granted.

The host app remains responsible for permission UX, lifecycle ownership, and any foreground-service requirements applicable to its use case.

## Current limitations

- No Android build or device test has yet been run against this commit.
- The model is not downloaded or bundled automatically.
- The adapter currently uses CPU inference and two compute threads by default.
- The 20-second maximum utterance is configurable (1–60 seconds).
- Wake-word detection, post-keyword audio handoff, streaming partial transcripts, TTS, and Windows remain separate work.
- Do not mark Android ASR as supported until a clean Android build and an airplane-mode microphone test pass on physical hardware.
