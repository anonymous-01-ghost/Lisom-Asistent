# Development roadmap

## Phase 1 — Foundation

- [x] Create repository identity and product boundary.
- [x] Establish Gradle multi-module structure.
- [x] Define shared command, state, and public API contracts.
- [x] Add the first deterministic Spanish NLU grammar.
- [x] Add initial NLU unit tests and a JVM demo.
- [x] Verify NLU tests and JVM demo in GitHub Actions (run for commit `596ccecf41743c787b134d9ca6039fdeadf7c3b0`).
- [x] Add and verify Gradle wrapper files with Gradle 9.7.0.

## Phase 2 — Speech recognition (in progress)

- [x] Evaluate and document sherpa-onnx v2.0.1 as a provisional engine candidate.
- [x] Identify Moonshine Base Spanish quantized as the first ASR model candidate (licence and device tests pending).
- [x] Document per-platform adapter boundaries, verified upstream artifact checksums, and implementation gates in [SHERPA_ADAPTER_IMPLEMENTATION.md](SHERPA_ADAPTER_IMPLEMENTATION.md).
- [ ] Validate one pinned engine build across Android, JVM/Windows, and iOS; iOS requires a macOS/Xcode build host.
- [x] Record upstream engine/model license sources and keep the model archive host-supplied.
- [ ] Complete formal native binary, model, dataset, and redistribution licence review.
- [x] Add Android AudioRecord capture for mono PCM16/16 kHz with bounded shared buffering and an explicit end-of-utterance marker (physical-device validation pending).
- [x] Add an Android Sherpa-ONNX 2.0.1 Moonshine v2 adapter using app-supplied local model files.
- [x] Pin the upstream Android AAR by SHA-256 and download it at build time; no native binary is committed to the repository.
- [x] Pass Android source compilation and shared JVM/NLU CI in run `44383261711`; a later full workflow also passed Android and shared checks in run `44387376519`.
- [ ] Validate Android runtime permission flow, physical-device microphone capture, and airplane-mode inference.
- [x] Add Windows/JVM microphone capture using Java Sound for mono PCM16/16 kHz.
- [x] Add a Windows x64 Sherpa-ONNX Moonshine v2 adapter and pin the JVM/native artifacts by SHA-256.
- [x] Pass Windows x64 adapter compilation and PCM conversion tests in run `44387376519`.
- [ ] Validate Windows microphone capture and Spanish model inference on a physical Windows x64 machine.
- [ ] Validate Spanish recognition accuracy, latency, memory, and lifecycle offline.
- [ ] Implement iOS only after a macOS/Xcode build host is available.

## Phase 3 — Wake word (in progress)

- [x] Select a provisional open-vocabulary Spanish KWS model candidate; exact "Oye Lisom" accuracy and licensing remain under review.
- [x] Add Sherpa Zipformer keyword-spotter adapters for Android and Windows/JVM.
- [x] Make wake-word confidence nullable when the engine provides no calibrated score.
- [x] Add the raw "oye lisom" keyword definition and a Windows script to generate local BPE keyword tokens.
- [x] Pass Android and Windows/JVM KWS adapter source compilation in CI run `44388881542`.
- [ ] Validate exact "Oye Lisom" phrase detection on Peruvian Spanish voices and tune false-accept/false-reject thresholds.
- [x] Add bounded 16-frame pre-roll and post-keyword handoff to ASR through one shared microphone relay.
- [x] Add a local amplitude-based utterance endpoint (1.2 s trailing silence, 20 s maximum) and core JVM tests; CI run `44393655204` passed.
- [x] Integrate the detector with the engine's single-owner microphone lifecycle and cancellation cleanup.
- [ ] Validate fast-following commands, delayed speech, background noise, and microphone interruptions on physical hardware.
- [ ] Measure always-on CPU, memory, latency, false accepts, false rejects, and battery use.

## Phase 4 — NLU (in progress)

- [x] Expand the Spanish grammar and add paraphrase tests for playback, artist, track, playlist, lyrics, and volume intents.
- [x] Normalize Spanish accents, punctuation, and whitespace before intent matching.
- [x] Preserve structured command entities and add a configurable minimum NLU confidence threshold to emitted commands.
- [ ] Calibrate confidence thresholds against a Spanish evaluation corpus.
- [x] Declare supported Spanish locale tags and reject unsupported configured locales during engine initialization.
- [ ] Add additional language grammars and locale-specific behavior.
- [x] Keep an explicit unknown-intent result for unsupported or out-of-range requests.

## Phase 5 — TTS

- [x] Select a provisional offline Spanish voice candidate: Sherpa-ONNX Piper/VITS "vits-piper-es_MX-claude-high" (model licence review pending).
- [x] Implement Android and Windows/JVM Sherpa Piper/VITS synthesis adapters using host-supplied local model files.
- [x] Add Android AudioTrack and Windows/JVM SourceDataLine PCM output adapters.
- [ ] Implement and compile the iOS native TTS output adapter on macOS/Xcode.
- [ ] Test interruption, cancellation, audio-session coexistence, voice quality, latency, and offline operation on physical hardware.

## Phase 6 — Platform hardening

- [x] Declare Android RECORD_AUDIO permission in the library manifest; the host app must still request it at runtime.
- [ ] Validate Android foreground-service requirements and device lifecycle.
- [ ] Validate iOS foreground behavior and document background restrictions.
- [ ] Test supported Windows audio devices and native library loading.

## Phase 7 — SDK release

- [ ] Add binary publishing and semantic versioning.
- [ ] Finalize SDK documentation and samples.
- [ ] Complete dependency, model, and licence review.
- [x] Make CI use the checked-in Gradle Wrapper.
- [ ] Add broader reproducible compatibility builds and artifact tests.

Lisom Play is the first host application. It may consume this SDK later, but no Lisom Play code belongs in this repository.
