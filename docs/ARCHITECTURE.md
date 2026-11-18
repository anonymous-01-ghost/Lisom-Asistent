# Architecture

## Product boundary

Lisom Assistant is a reusable voice SDK. This repository contains no host application, music player, media catalogue, provider integration, or backend. A host app receives typed intents and decides what to do with them.

## Runtime pipeline

```text
Microphone adapter
   ├── Wake-word detector (optional: "Oye Lisom")
   └── Push-to-talk trigger
             |
       Offline speech recognition
             |
       Local command interpreter
             |
       Flow<RecognizedCommand>
             |
       Host application

Host app -> LisomAssistant.speak(text) -> Offline TTS adapter -> Audio output
```

## Module dependency direction

- `lisom-models`: dependency-free shared command and state types.
- `lisom-api`: public contracts, configuration, and event streams.
- `lisom-speech`, `lisom-wakeword`, `lisom-nlu`, `lisom-tts`: replaceable engine contracts and independent implementations.
- `lisom-core`: orchestrates the contracts through dependency injection.
- `lisom-platform`: platform identification and native adapter boundary.
- `lisom-testing`: test doubles.
- `lisom-demo`: a command-line NLU sample, not a consumer product.

## Platform strategy

Kotlin Multiplatform shares models, APIs, NLU rules, and orchestration. Audio capture and native engine bindings belong in platform source sets. Windows is initially supported through Kotlin/JVM. iOS native compilation requires macOS and Xcode.

## Audio contract

The initial audio format is mono, signed 16-bit PCM at 16 kHz. Platform adapters must expose a hot multicast frame stream while capture is active. This allows keyword spotting and speech recognition to observe the same microphone input without competing for exclusive microphone access.

## Model and engine loading

Speech, wake-word, and TTS models are local assets. Their paths, checksums, versions, licences, and installation strategy must be documented before bundling. No model is downloaded automatically by the SDK at runtime.

## Current implementation boundary

The foundation includes public contracts, a reference coordinator, and a deterministic Spanish command grammar. It does not yet ship a real microphone adapter, Sherpa-ONNX binding, trained wake-word model, VAD implementation, or TTS engine.
