package dev.lisom.models

/**
 * Commands are intents only. The host application decides how to execute them.
 * Lisom never owns a catalogue, player, queue, or media provider.
 */
sealed interface LisomCommand {
    data class SearchMusic(val query: String) : LisomCommand
    data class SearchLyrics(val phrase: String) : LisomCommand
    data class SearchArtist(val artist: String) : LisomCommand
    data class PlayArtist(val artist: String) : LisomCommand
    data class PlayTrack(val title: String, val artist: String? = null) : LisomCommand
    data class PlayPlaylist(val playlist: String) : LisomCommand
    data object Pause : LisomCommand
    data object Resume : LisomCommand
    data object Next : LisomCommand
    data object Previous : LisomCommand
    data object ShowLyrics : LisomCommand
    data object HideLyrics : LisomCommand
    data class SetVolume(val percent: Int) : LisomCommand {
        init { require(percent in 0..100) { "Volume must be between 0 and 100." } }
    }
}
