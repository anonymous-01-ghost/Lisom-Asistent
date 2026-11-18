package dev.lisom.nlu

import dev.lisom.models.LisomCommand
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SpanishRuleBasedInterpreterTest {
    private val interpreter = SpanishRuleBasedInterpreter()

    @Test
    fun parsesArtistPlayback() {
        val result = interpreter.interpret("Oye Lisom, reproduce música de Morat")
        assertEquals(LisomCommand.PlayArtist("morat"), result.command)
    }

    @Test
    fun parsesArtistSearch() {
        val result = interpreter.interpret("Busca canciones de Shakira")
        assertEquals(LisomCommand.SearchArtist("shakira"), result.command)
    }

    @Test
    fun parsesPlaybackControls() {
        assertEquals(LisomCommand.Pause, interpreter.interpret("Pausa la música").command)
        assertEquals(LisomCommand.Resume, interpreter.interpret("Continúa la música").command)
        assertEquals(LisomCommand.Next, interpreter.interpret("Pasa a la siguiente canción").command)
        assertEquals(LisomCommand.Previous, interpreter.interpret("Canción anterior").command)
    }

    @Test
    fun parsesLyricsIntent() {
        assertEquals(LisomCommand.ShowLyrics, interpreter.interpret("Quiero escuchar las letras").command)
    }

    @Test
    fun leavesUnknownInputUnrecognized() {
        val result = interpreter.interpret("¿Qué hora es?")
        assertNull(result.command)
        assertTrue(result.reason == "unmatched_intent")
    }
}
