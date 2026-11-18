# Architecture

## Product boundary

Lisom Assistant is a reusable voice SDK. This repository contains no host application, music player, media catalogue, provider integration, or backend. A host app receives typed intents and decides what to do with them.

## Runtime pipeline

Push-to-talk:
```text
Microphone adapter -> offline ASR -> local Spanish NLU
                  -> Flow<RecognizedCommand> -> host application
```

Wake-word mode:
```text
One microphone capture session
          |
Bounded PCM relay (16 frames of pre-roll)
          |
Local "Oye Lisom" keyword spotter
          |
Wake event; stop keyword spotter
          |
Replay buffered PCM + continue same capture stream
          |
Local silence endpoint (1.2 s trailing silence; 20 s maximum)
          |
Offline ASR -> local Spanish NLU -> typed command -> host app
```

The relay uses a bounded multicast buffer so the recognizer can subscribe after the wake event without losing the short command that follows the activation phrase. The wake-word path does not open a second microphone device. The current silence endpoint is a simple PCM mean-absolute-amplitude gate; thresholds and endpoint timing require validation against real microphones, ambient noise, and Peruvian Spanish voices.

Host app -> `LisomAssistant.speak(text)` -> offline TTS adapter -> audio output.

## Module dependency direction

- `lisom-models`: dependency-free shared command and state types.
- `lisom-api`: public contracts, configuration, and event streams.
- `lisom-speech`, `lisom-wakeword`, `lisom-nlu`, `lisom-tts`: replaceable engine contracts and independent implementations.
- `lisom-core`: orchestrates the contracts through dependency injection and performs the bounded wake-word-to-ASR audio handoff.
- `lisom-platform`: native microphone and Sherpa adapter implementations.
- `lisom-testing`: test doubles.
- `lisom-demo`: a command-line NLU sample, not a consumer product.

## Platform strategy

Kotlin Multiplatform shares models, APIs, NLU rules, and orchestration. Audio capture and native engine bindings belong in platform source sets. Android uses `AudioRecord`; Windows is initially supported through Kotlin/JVM and Java Sound. iOS native compilation requires macOS and Xcode.

## Audio contract

The initial audio format is mono, signed 16-bit PCM at 16 kHz. Platform adapters expose a hot multicast frame stream while capture is active. In wake-word mode, `lisom-core` relays that single capture into a bounded replay buffer. This lets keyword spotting and then ASR observe the same stream without competing for exclusive microphone access.

## Model and engine loading

Speech, wake-word, and TTS models are local assets. Their paths, checksums, versions, licences, and installation strategy must be documented before bundling. No model is downloaded automatically by the SDK at runtime.

## Current implementation boundary

Android and Windows x64 have initial microphone, Sherpa Moonshine offline ASR, and experimental Spanish Zipformer KWS adapters. Source compilation for both platforms and shared core handoff tests pass in GitHub Actions run `44393655204`. Real hardware capture, model inference, wake phrase accuracy, false-trigger rates, and lifecycle behaviour remain pending. iOS microphone/ASR/KWS adapters and offline Spanish TTS are not implemented.
