# Public API

## Configuration

`LisomConfig` defaults to Spanish (Peru), 16 kHz mono input, push-to-talk, optional wake-word activation, and mandatory offline execution. The constructor rejects an online configuration.

## Host integration

The SDK exposes `LisomAssistant`:

- `initialize()`
- `startPushToTalk()` / `stopPushToTalk()`
- `enableWakeWord()` / `disableWakeWord()`
- `speak(text)`
- `release()`
- `state: StateFlow<LisomAssistantState>`
- `commands: Flow<RecognizedCommand>`

The host collects typed commands:

```kotlin
lisom.commands.collect { recognized ->
    when (val command = recognized.command) {
        is LisomCommand.PlayArtist -> hostSearchAndPlay(command.artist)
        LisomCommand.Pause -> hostPlayer.pause()
        LisomCommand.ShowLyrics -> hostLyrics.show()
        else -> Unit
    }
}
```

The example describes the host-side responsibility; `hostSearchAndPlay`, `hostPlayer`, and `hostLyrics` belong to the integrating application and are not part of this repository.

## Command model

The initial command types include music search, artist and track playback, playlist playback, pause, resume, next, previous, lyrics visibility, and volume adjustment. These are intents; Lisom does not perform media operations itself.

## Stability

The API is 0.x and may change before the first stable release. Platform engine factories and model-loading APIs will be added after native integrations are selected and tested.
