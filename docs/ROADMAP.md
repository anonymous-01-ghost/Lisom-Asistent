# Development roadmap

## Phase 1 — Foundation

- [x] Create repository identity and product boundary.
- [x] Establish Gradle multi-module structure.
- [x] Define shared command, state, and public API contracts.
- [x] Add the first deterministic Spanish NLU grammar.
- [x] Add initial NLU unit tests and a JVM demo.
- [x] Verify NLU tests and JVM demo in GitHub Actions (run for commit `596ccecf41743c787b134d9ca6039fdeadf7c3b0`).
- [ ] Add and verify Gradle wrapper files.

## Phase 2 — Speech recognition (in progress)

- [x] Evaluate and document sherpa-onnx v2.0.1 as a provisional engine candidate.
- [x] Identify Moonshine Base Spanish quantized as the first ASR model candidate (license and device tests pending).
- [x] Document per-platform adapter boundaries, verified upstream artifact checksums, and implementation gates in [SHERPA_ADAPTER_IMPLEMENTATION.md](SHERPA_ADAPTER_IMPLEMENTATION.md).
- [ ] Validate one pinned engine build across Android, JVM/Windows, and iOS; iOS requires a macOS/Xcode build host.
- [ ] Review native binary and model licences.
- [ ] Implement 16 kHz mono audio capture on each platform.
- [ ] Integrate a Spanish offline ASR model.
- [ ] Add endpointing/VAD and confidence handling.
- [ ] Validate accuracy and latency offline.

## Phase 3 — Wake word

- [ ] Select or train a local "Oye Lisom" keyword model.
- [ ] Integrate local keyword spotting.
- [ ] Define bounded audio pre-roll and post-keyword handoff to ASR; the current event contract does not yet guarantee that speech immediately following the wake phrase is preserved.
- [ ] Measure false accepts, false rejects, CPU and battery use.

## Phase 4 — NLU (in progress)

- [x] Expand the Spanish grammar and add paraphrase tests for playback, artist, track, playlist, lyrics, and volume intents.
- [x] Normalize Spanish accents, punctuation, and whitespace before intent matching.
- [ ] Add structured entity extraction and calibrated confidence thresholds.
- [ ] Add locale and language configuration.
- [x] Keep an explicit unknown-intent result for unsupported or out-of-range requests.

## Phase 5 — TTS

- [ ] Select an offline Spanish synthesis engine and voice.
- [ ] Implement Android, JVM/Windows, and iOS output adapters.
- [ ] Test interruption, cancellation, and audio-session coexistence.

## Phase 6 — Platform hardening

- [ ] Implement microphone permission and lifecycle adapters.
- [ ] Validate Android foreground-service requirements.
- [ ] Validate iOS foreground behavior and document background restrictions.
- [ ] Test supported Windows audio devices.

## Phase 7 — SDK release

- [ ] Add binary publishing and semantic versioning.
- [ ] Finalize SDK documentation and samples.
- [ ] Complete dependency, model, and licence review.
- [ ] Add reproducible CI builds and compatibility tests.

Lisom Play is the first host application. It may consume this SDK later, but no Lisom Play code belongs in this repository.
