# Platform support

| Platform | Kotlin target | Current implementation status | Validation still required |
| --- | --- | --- | --- |
| Android | Android-KMP | Initial `AudioRecord` microphone, Sherpa Moonshine v2 ASR, and Spanish Zipformer KWS adapters added; source compilation and core handoff tests passed run `44393655204`. | Runtime permission flow, physical-device capture, airplane-mode inference, foreground-service and lifecycle behavior. |
| Windows | JVM | Initial Java Sound microphone, Sherpa Moonshine v2 ASR, and Spanish Zipformer KWS adapters added for Windows x64; adapter compilation and PCM tests passed in runs `44388881542` and `44393655204`. | Real microphone capture, native loading and offline inference on a Windows x64 machine. |
| iOS | Kotlin/Native | Shared contracts and target declarations exist; no native microphone or ASR adapter yet. | Requires macOS/Xcode, native Sherpa packaging, device/simulator compilation, permissions and AVAudioSession tests. |

Neither Android nor Windows is declared fully supported until its target-hardware capture, local model inference, lifecycle, and offline validation gates pass. iOS remains planned.

## Shared code

Models, API contracts, the initial Spanish command grammar, and core orchestration are shared in `commonMain`. Platform-specific microphone and Sherpa types remain in their native source sets.

## Platform-independent limitations

- The current Sherpa adapters perform batch recognition after push-to-talk stops; they do not emit partial streaming transcripts.
- The SDK does not bundle or automatically download the Spanish model. The host installs the model files locally and passes the directory to the adapter.
- The Spanish open-vocabulary KWS model is a provisional candidate. The bounded wake-word audio handoff is implemented and passes core JVM tests, but exact `Oye Lisom` accuracy, real-device command preservation, and false-trigger rates are not yet validated. Offline Spanish TTS is also pending.
- The host application owns UI, permissions UX, lifecycle, and execution of typed commands.
