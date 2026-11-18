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
