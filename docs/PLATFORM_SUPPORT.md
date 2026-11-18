# Platform support

| Platform | Kotlin target | Initial goal | Important limitation |
| --- | --- | --- | --- |
| Android | Android-KMP | Native microphone, push-to-talk, offline engines | The host app must request microphone permission; continuous background listening is subject to Android foreground-service and OS rules |
| Windows | JVM | Desktop microphone adapter and offline engines | Wake-word monitoring is available only while the host process and audio session are running |
| iOS | Kotlin/Native | Push-to-talk and local engines | macOS and Xcode are required to build; background microphone behavior is constrained by iOS and is not guaranteed |

## Shared code

Models, API contracts, the initial Spanish command grammar, and core orchestration are shared in `commonMain`.

## Native code still required

The current foundation does not include production microphone capture, platform audio playback, Sherpa-ONNX binaries, wake-word model assets, or TTS engines. Those must be added and tested per platform before claiming end-to-end voice support.
