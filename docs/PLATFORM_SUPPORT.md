# Platform support

| Platform | Kotlin target | Current implementation status | Validation still required |
| --- | --- | --- | --- |
| Android | Android-KMP | Initial `AudioRecord` microphone adapter and Sherpa-ONNX Moonshine v2 ASR adapter added; source compilation passed CI run `44383261711`. | Runtime permission flow, physical-device capture, airplane-mode inference, foreground-service and lifecycle behavior. |
| Windows | JVM | Initial Java Sound microphone adapter and Sherpa-ONNX Moonshine v2 ASR adapter added for Windows x64; native DLLs are prepared by a checksum-verified Gradle task. | Windows CI compile/test result, real microphone capture, native loading and offline inference on a Windows x64 machine. |
| iOS | Kotlin/Native | Shared contracts and target declarations exist; no native microphone or ASR adapter yet. | Requires macOS/Xcode, native Sherpa packaging, device/simulator compilation, permissions and AVAudioSession tests. |

Neither Android nor Windows is declared fully supported until its target-hardware capture, local model inference, lifecycle, and offline validation gates pass. iOS remains planned.

## Shared code

Models, API contracts, the initial Spanish command grammar, and core orchestration are shared in `commonMain`. Platform-specific microphone and Sherpa types remain in their native source sets.

## Platform-independent limitations

- The current Sherpa adapters perform batch recognition after push-to-talk stops; they do not emit partial streaming transcripts.
- The SDK does not bundle or automatically download the Spanish model. The host installs the model files locally and passes the directory to the adapter.
- The exact `Oye Lisom` wake-word model and offline Spanish TTS engine have not yet been selected and validated.
- The host application owns UI, permissions UX, lifecycle, and execution of typed commands.
