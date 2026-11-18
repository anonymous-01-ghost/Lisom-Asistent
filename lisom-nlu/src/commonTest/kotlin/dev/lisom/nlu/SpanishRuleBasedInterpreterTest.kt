package dev.lisom.nlu

import dev.lisom.models.LisomCommand
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SpanishRuleBasedInterpreterTest {
    private val interpreter = SpanishRuleBasedInterpreter()

    @Test
    fun parsesArtistPlaybackAndSearch() {
        assertEquals(
            LisomCommand.PlayArtist("morat"),
            interpreter.interpret("Oye Lisom, reproduce música de Morat").command,
        )
        assertEquals(
            LisomCommand.SearchArtist("shakira"),
            interpreter.interpret("Busca canciones de Shakira").command,
        )
    }

    @Test
    fun parsesTrackAndPlaylistRequests() {
        assertEquals(
            LisomCommand.PlayTrack("la camisa negra", "juan es"),
            interpreter.interpret("Reproduce la canción La camisa negra de Juan Es").command,
        )
        assertEquals(
            LisomCommand.PlayTrack("ojos color sol"),
            interpreter.interpret("Pon la canción Ojos color sol").command,
        )
        assertEquals(
            LisomCommand.PlayPlaylist("favoritos"),
            interpreter.interpret("Reproduce lista Favoritos").command,
        )
    }

    @Test
    fun parsesPlaybackControlsAndParaphrases() {
        assertEquals(LisomCommand.Pause, interpreter.interpret("Detén la reproducción").command)
        assertEquals(LisomCommand.Resume, interpreter.interpret("Sigue reproduciendo").command)
        assertEquals(LisomCommand.Next, interpreter.interpret("Salta esta canción").command)
        assertEquals(LisomCommand.Previous, interpreter.interpret("Regresa a la canción anterior").command)
    }

    @Test
    fun parsesLyricsDisplayAndPhraseSearch() {
        assertEquals(LisomCommand.ShowLyrics, interpreter.interpret("Quiero escuchar las letras").command)
        assertEquals(LisomCommand.HideLyrics, interpreter.interpret("Oculta la letra").command)
        assertEquals(
            LisomCommand.SearchLyrics("y nos dieron las diez"),
            interpreter.interpret("Busca la canción que dice 'Y nos dieron las diez'").command,
        )
    }

    @Test
    fun parsesVolumeOnlyWithinSupportedRange() {
        assertEquals(LisomCommand.SetVolume(35), interpreter.interpret("Pon el volumen al 35").command)
        assertEquals(LisomCommand.SetVolume(100), interpreter.interpret("Volumen a 100").command)
        assertNull(interpreter.interpret("Pon el volumen al 120").command)
    }

    @Test
    fun leavesUnknownAndEmptyInputUnrecognized() {
        val unknown = interpreter.interpret("¿Qué hora es?")
        assertNull(unknown.command)
        assertEquals("unmatched_intent", unknown.reason)

        val empty = interpreter.interpret("   ")
        assertNull(empty.command)
        assertEquals("empty_input", empty.reason)
    }
}
