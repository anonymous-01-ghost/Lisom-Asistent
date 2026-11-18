# Local wake-word detection: "Oye Lisom"

**Status:** Android and Windows/JVM KWS adapters compile in CI run `44388881542`. The core bounded audio handoff and silence-endpoint tests pass in run `44393655204`. The model remains experimental; exact-phrase accuracy and false-trigger testing are pending.

## Model candidate

Candidate: [TKA KWS — Spanish Zipformer 3.3M](https://huggingface.co/jguerrisi/sherpa-onnx-kws-zipformer-es-3M-2026-08-08).

The model card describes an open-vocabulary streaming Spanish keyword spotter compatible with Sherpa-ONNX. It accepts runtime-configured keyword phrases via a generated `keywords.txt`, so changing the wake phrase does not require retraining. It expects mono 16 kHz audio and reports approximately 320 ms latency. The model card declares Apache-2.0, but separately notes MLS (CC BY 4.0), MUSAN, and Piper-synthesized keyword audio; each voice's licence and attribution still need review.

The published evaluation is not a validation of "Oye Lisom": it covers five safety phrases on one audio clip, and the model author warns about far-field/noisy speech and limited dialect coverage. Peruvian Spanish must be tested before enabling this as a default wake word.

## Local model directory

The host app must install the following files locally:

- `encoder-epoch-20-avg-2-chunk-16-left-64.onnx`
- `decoder-epoch-20-avg-2-chunk-16-left-64.onnx`
- `joiner-epoch-20-avg-2-chunk-16-left-64.onnx`
- `tokens.txt`
- `bpe.model`
- `keywords.txt` (generated for the phrase used by the app)

The SDK does not download these assets, and audio is not sent to any service. The `bpe.model` is needed when generating `keywords.txt`; the runtime adapter also validates that the file exists.

## Generate the "Oye Lisom" keyword file on Windows

Install the Sherpa-ONNX CLI once during model preparation (not at runtime). From the SDK repository root, run:

```powershell
pip install sherpa-onnx
./scripts/generate-oye-lisom-keywords.ps1 -ModelDirectory "C:\models\sherpa-onnx-kws-zipformer-es-3M-2026-08-08"
```

The raw phrase and tag are stored in [oye-lisom-keywords.raw.txt](assets/oye-lisom-keywords.raw.txt):

```text
oye lisom :1.0 #0.20 @oye_lisom
```

The CLI converts the phrase into the model's BPE token representation. The tag is returned by Sherpa when the phrase is detected. Boost and threshold values are initial tuning values, not calibrated settings; measure false accepts and false rejects on the target devices and speech population before release.

## Adapter API

Platform adapters:

- Android: `SherpaZipformerWakeWordDetector` in `androidMain`.
- Windows x64/JVM: `SherpaZipformerWakeWordDetector` in `jvmMain`.

Both accept the local model directory and stream mono PCM16 audio at 16 kHz. They use CPU execution, a single inference thread by default, and the same configurable keyword boost/threshold. The event phrase is returned from the `@tag` in the keyword file, with underscores converted to spaces.

The Sherpa KWS result does not expose a calibrated confidence score. `WakeWordEvent.confidence` is therefore nullable; the adapter emits `null` instead of inventing a score.

## Wake-word to ASR audio handoff

The first core implementation is now present in `LisomAssistantEngine`:

- One physical microphone capture is relayed into a bounded `MutableSharedFlow` with 16 frames of replay (about 1.6 seconds with the current 100 ms adapters).
- The KWS detector emits one event and is cancelled before ASR starts.
- ASR subscribes to the same relay, receiving the cached pre-roll plus incoming frames. This prevents the first part of a fast command from being lost when the user says "Oye Lisom, reproduce ...".
- `untilSpeechEndpoint` closes the utterance after 1.2 seconds of trailing low-amplitude PCM or a 20-second maximum, then emits the shared end-of-stream marker.
- The core stops and restarts the microphone session in one owner-controlled loop; the detector and ASR do not open separate microphone devices.

The PCM handoff and silence endpoint have unit tests in `lisom-core:jvmTest`, passing CI run `44393655204`. The voice activity threshold (mean absolute amplitude 500/32768 by default) and silence duration are initial heuristics, not tuned VAD parameters. Tests with real background noise, delayed commands, microphone interruptions, and physical devices remain necessary.

## Release validation

- [x] Compile the Android and Windows/JVM adapters in CI (run `44388881542`).
- Run recordings of "Oye Lisom" from multiple Peruvian Spanish speakers, microphones, distances, and noise conditions.
- Measure false accept rate during ordinary conversation and media playback, and false reject rate for the wake phrase.
- [x] Add bounded pre-roll and wake-word-to-ASR handoff with local silence endpoint; core JVM tests pass in run `44393655204`.
- Test that a command spoken immediately after the wake phrase is fully preserved on physical devices.
- Measure always-on CPU, memory, latency, and battery/power use.
- Review the model, training-data, Sherpa engine, and Piper voice licences and attributions before redistribution.
- Keep the feature opt-in until these tests pass.
