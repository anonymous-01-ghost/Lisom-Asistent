# Public API

## Configuration

`LisomConfig` defaults to Spanish (Peru), 16 kHz mono input, push-to-talk, optional wake-word activation, mandatory offline execution, and a 0.70 minimum NLU confidence threshold. The constructor rejects an online configuration and invalid thresholds. The bundled Spanish interpreter supports `es`, `es-PE`, `es-MX`, and `es-ES`; unsupported configured locales fail during initialization.

## Host integration

The SDK exposes `LisomAssistant`:

- `initialize()`
- `startPushToTalk()` / `stopPushToTalk()`
- `enableWakeWord()` / `disableWakeWord()`
- `speak(text)`
- `stopSpeaking()`
- `release()`
- `state: StateFlow<LisomAssistantState>`
- `commands: Flow<RecognizedCommand>`

The host creates the engine by injecting platform audio and local Sherpa adapters. For Android, the wiring looks like this (the host supplies the local model directories):

```kotlin
val assistant = LisomAssistantEngine(
    config = LisomConfig(languageTag = "es-PE"),
    audioInput = AndroidAudioInput(context),
    speechRecognizer = SherpaMoonshineSpeechRecognizer(asrModelDirectory),
    commandInterpreter = SpanishRuleBasedInterpreter(),
    wakeWordDetector = SherpaZipformerWakeWordDetector(kwsModelDirectory),
    speechSynthesizer = SherpaPiperSpeechSynthesizer(
        modelDirectory = ttsModelDirectory,
        audioOutput = AndroidPcmAudioOutput(),
        modelFileName = ttsModelFileName,
    ),
)
assistant.initialize()
assistant.enableWakeWord()
```

For Windows/JVM, use `WindowsAudioInput`, the JVM Sherpa adapters, and `JvmPcmAudioOutput`; start the host JVM with `java.library.path` set to the extracted Sherpa DLL directory. Push-to-talk hosts call `startPushToTalk()` on button press and `stopPushToTalk()` on release. Call `stopSpeaking()` to interrupt spoken output and `release()` when the host is shutting down.

The host collects typed commands:

```kotlin
lisom.commands.collect { recognized ->
    when (val command = recognized.command) {
        is LisomCommand.PlayArtist -> hostSearchAndPlay(command.artist)
        is LisomCommand.PlayTrack -> hostPlayTrack(command.title, command.artist)
        is LisomCommand.SearchMusic -> hostSearch(command.query)
        is LisomCommand.SearchLyrics -> hostSearchLyrics(command.phrase)
        is LisomCommand.SetVolume -> hostSetVolume(command.percent)
        LisomCommand.Pause -> hostPlayer.pause()
        LisomCommand.Resume -> hostPlayer.play()
        LisomCommand.Next -> hostPlayer.seekToNext()
        LisomCommand.Previous -> hostPlayer.seekToPrevious()
        LisomCommand.ShowLyrics -> hostLyrics.show()
        LisomCommand.HideLyrics -> hostLyrics.hide()
        else -> Unit
    }
}
```

The example describes the host-side responsibility; `hostSearchAndPlay`, `hostPlayTrack`, `hostSearch`, `hostSearchLyrics`, `hostSetVolume`, `hostPlayer`, and `hostLyrics` belong to the integrating application and are not part of this repository.

## Offline TTS adapters

The public engine accepts a SpeechSynthesizer implementation. Android and Windows/JVM provide local Sherpa Piper/VITS adapters that require a host-installed model directory containing the selected .onnx voice, tokens.txt, and espeak-ng-data. The adapter accepts an optional expectedModelSha256 so the host can reject a changed model before native initialization.

The host also chooses the audio sink:

- Android: AndroidPcmAudioOutput backed by AudioTrack.
- Windows/JVM: JvmPcmAudioOutput backed by SourceDataLine.

The SDK never downloads the model or sends text/audio to a remote service. The provisional voice candidate is vits-piper-es_MX-claude-high; its licence and physical-device quality still require release validation.

## Command model

The deterministic Spanish grammar currently recognizes:

- Search music and artists.
- Search a song by a phrase from its lyrics.
- Play artists, tracks, and playlists.
- Pause, resume, skip to next, and return to previous.
- Show or hide lyrics.
- Set playback volume from 0 to 100 percent.

These are intents only. Lisom does not own a music catalogue, player, queue, lyrics database, or media provider. The host application maps each command to its own existing functionality.

## Stability

The API is 0.x and may change before the first stable release. Platform engine factories and model-loading APIs will be added after native integrations are selected and tested.
