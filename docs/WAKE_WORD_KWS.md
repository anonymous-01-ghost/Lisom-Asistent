# Local wake-word detection: "Oye Lisom"

**Status:** Initial Sherpa-ONNX detector adapters added for Android and Windows/JVM. Model selection is provisional; compile validation, keyword phrase validation, false-trigger testing, and engine handoff are pending.

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

## Important integration gap: preserve speech after the wake phrase

The new detector is an adapter building block, not yet an end-to-end "Oye Lisom" experience. The core audio pipeline must retain a bounded pre-roll buffer and hand the captured speech following the keyword to ASR without opening a second, late microphone subscription. Otherwise a user who says "Oye Lisom, reproduce ..." quickly may lose the first words of the command.

Before enabling wake-word mode in Lisom Play, implement and test a single-owner audio session with bounded buffering, event-to-ASR handoff, cancellation, and a policy that prevents the keyword detector and command recognizer from fighting over microphone lifecycle.

## Release validation

- Compile the Android and Windows/JVM adapters in CI.
- Run recordings of "Oye Lisom" from multiple Peruvian Spanish speakers, microphones, distances, and noise conditions.
- Measure false accept rate during ordinary conversation and media playback, and false reject rate for the wake phrase.
- Test that a command spoken immediately after the wake phrase is fully preserved.
- Measure always-on CPU, memory, latency, and battery/power use.
- Review the model, training-data, Sherpa engine, and Piper voice licences and attributions before redistribution.
- Keep the feature opt-in until these tests pass.
