# Windows offline ASR adapter

**Status:** Windows x64 ASR, experimental KWS, local Piper/VITS TTS adapters, and PCM conversion tests compile in CI. Real microphone capture, offline model inference, wake-word accuracy, and TTS playback on Windows hardware remain unvalidated.

## Components

- `WindowsAudioInput`: captures mono signed PCM16 audio through Java Sound at 16 kHz.
- `SherpaMoonshineSpeechRecognizer`: uses Sherpa-ONNX 2.0.1 and Moonshine v2 with the local Spanish model.
- `PcmAudioFrame.isEndOfStream`: ends a push-to-talk utterance when `WindowsAudioInput.stop()` is called.
- `SpeechRecognitionResult.confidence`: returned as `null`; the selected offline result does not expose a calibrated confidence value.

The recognizer buffers one utterance and decodes it locally after capture ends. The default maximum is 20 seconds and can be configured from 1 to 60 seconds. Partial streaming transcripts are not implemented.

## Build-time dependencies

The `jvmMain` source set uses two pinned Sherpa-ONNX v2.0.1 release artifacts:

| Artifact | SHA-256 |
| --- | --- |
| `sherpa-onnx-jvm-2.0.1.jar` | `8320e2457b6e38dd624fb619d3552e11ef78ffeb394622056126b16f2fd99d12` |
| `sherpa-onnx-native-lib-win-x64-2.0.1.jar` | `536f1c09025996a780b22f972b2e56e0e8b2064e1ceb534456ea7eba0a6ae4cd` |

Gradle downloads these files into the module build directory, verifies their hashes, and extracts `sherpa-onnx-jni.dll` and `onnxruntime.dll`. The artifacts are not committed to Git. This is a build-time dependency download only; inference does not make network requests.

Prepare the Windows native runtime:

```powershell
.\\gradlew.bat :lisom-platform:prepareSherpaWindowsMaven
```

## Configure native library loading

The host JVM must be started with `java.library.path` pointing to the extracted DLL directory. Set it before the JVM starts; changing it after Sherpa classes are loaded is too late.

Example PowerShell launch:

```powershell
$nativeDir = (Resolve-Path ".\\lisom-platform\\build\\third-party\\sherpa-onnx-win-x64").Path
java "-Djava.library.path=$nativeDir" -jar .\\LisomPlay.jar
```

The adapter validates that both DLL files are present in one of the configured `java.library.path` directories. This initial native package targets Windows x64 (AMD64), not Windows ARM64.

## Model installation

The host application must install the Spanish Moonshine model locally before creating the recognizer. The model directory must contain:

- `encoder_model.ort`
- `decoder_model_merged.ort`
- `tokens.txt`

Candidate model: `sherpa-onnx-moonshine-base-es-quantized-2026-02-27`. Its licence must be reviewed before redistribution. The model is not included in this SDK and is not downloaded by the recognizer.

Example:

```kotlin
val modelDirectory = File(
    "models/sherpa-onnx-moonshine-base-es-quantized-2026-02-27",
)
val audioInput = WindowsAudioInput()
val speechRecognizer = SherpaMoonshineSpeechRecognizer(modelDirectory)
```

Connect these adapters to `LisomAssistantEngine` using the existing dependency-injection contract. The host remains responsible for UI, command execution, and lifecycle.

## Current limitations

- Windows CI compiles the adapter and executes the PCM conversion tests; it does not access a physical microphone or load the Spanish model.
- Real microphone capture and Spanish-model inference have not yet been validated on a Windows x64 device.
- Java Sound must be able to open a mono 16 kHz PCM16 input line. Unsupported devices fail with a diagnostic instead of silently changing sample rates.
- CPU inference is used by default with two threads.
- The model is not downloaded or bundled automatically.
- Experimental Spanish wake-word detection and bounded handoff to ASR are implemented, but phrase accuracy and false-trigger rates need real-device testing. Offline Spanish TTS adapters are implemented; audio playback, interruption, and voice quality still need hardware validation. Partial streaming ASR transcripts remain unimplemented.
