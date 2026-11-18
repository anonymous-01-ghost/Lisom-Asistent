# Development roadmap

## Phase 1 — Foundation (current)

- [x] Create repository identity and product boundary.
- [x] Establish Gradle multi-module structure.
- [x] Define shared command, state, and public API contracts.
- [x] Add the first deterministic Spanish NLU grammar.
- [x] Add initial NLU unit tests and a JVM demo.
- [ ] Run the build and tests on a configured developer machine.
- [ ] Add Gradle wrapper files.

## Phase 2 — Speech recognition

- [ ] Select and pin a Sherpa-ONNX release compatible with Android, JVM/Windows, and iOS.
- [ ] Review native binary and model licences.
- [ ] Implement 16 kHz mono audio capture on each platform.
- [ ] Integrate a Spanish offline ASR model.
- [ ] Add endpointing/VAD and confidence handling.
- [ ] Validate accuracy and latency offline.

## Phase 3 — Wake word

- [ ] Select or train a local "Oye Lisom" keyword model.
- [ ] Integrate local keyword spotting.
- [ ] Implement audio pre-roll and handoff to ASR.
- [ ] Measure false accepts, false rejects, CPU and battery use.

## Phase 4 — NLU

- [ ] Expand the Spanish grammar and test paraphrases.
- [ ] Add entity extraction, normalization, and confidence thresholds.
- [ ] Add locale and language configuration.
- [ ] Keep an explicit unknown-intent result.

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

Lynox Music may later consume the published SDK, but no Lynox Music code belongs in this repository.
