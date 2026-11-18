package dev.lisom.core

import dev.lisom.api.LisomConfig
import dev.lisom.models.LisomAssistantState
import dev.lisom.nlu.SpanishRuleBasedInterpreter
import dev.lisom.testing.FakeAudioInput
import dev.lisom.testing.FakeSpeechRecognizer
import dev.lisom.testing.FakeSpeechSynthesizer
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class LisomAssistantEngineTest {
    @Test
    fun releaseReleasesInjectedSpeechSynthesizer() = runBlocking {
        val synthesizer = FakeSpeechSynthesizer()
        val engine = createEngine(speechSynthesizer = synthesizer)

        engine.initialize()
        engine.release()

        assertTrue(synthesizer.released)
        assertEquals(LisomAssistantState.Closed, engine.state.value)
    }

    @Test
    fun speakDelegatesTextToInjectedSynthesizer() = runBlocking {
        val synthesizer = FakeSpeechSynthesizer()
        val engine = createEngine(speechSynthesizer = synthesizer)

        engine.initialize()
        engine.speak("Hola, soy Lisom")

        assertEquals(listOf("Hola, soy Lisom"), synthesizer.spokenTexts)
        assertEquals(LisomAssistantState.Ready, engine.state.value)
        engine.release()
    }

    @Test
    fun stopSpeakingDelegatesToInjectedSynthesizer() = runBlocking {
        val synthesizer = FakeSpeechSynthesizer()
        val engine = createEngine(speechSynthesizer = synthesizer)

        engine.initialize()
        engine.speak("Hola, soy Lisom")
        engine.stopSpeaking()

        assertTrue(synthesizer.stopped)
        assertEquals(LisomAssistantState.Ready, engine.state.value)
        engine.release()
    }

    @Test
    fun minimumNluConfidenceFiltersWeakInterpretations() = runBlocking {
        val emitted = mutableListOf<dev.lisom.models.RecognizedCommand>()
        val engine = createEngine(
            config = LisomConfig(
                wakeWordEnabled = false,
                minimumNluConfidence = 0.95f,
            ),
        )
        val collector = launch { engine.commands.collect { emitted += it } }

        engine.startPushToTalk()
        yield()
        engine.stopPushToTalk()
        collector.cancelAndJoin()

        assertTrue(emitted.isEmpty())
    }

    @Test
    fun rejectsLocaleUnsupportedByTheInjectedInterpreter() = runBlocking {
        val engine = createEngine(
            config = LisomConfig(
                languageTag = "en-US",
                wakeWordEnabled = false,
            ),
        )

        assertFailsWith<IllegalStateException> {
            engine.initialize()
        }
        Unit
    }

    @Test
    fun emittedCommandIncludesNluConfidence() = runBlocking {
        val emitted = mutableListOf<dev.lisom.models.RecognizedCommand>()
        val engine = createEngine()
        val collector = launch { engine.commands.collect { emitted += it } }

        engine.startPushToTalk()
        yield()
        engine.stopPushToTalk()
        withTimeout(1_000) {
            while (emitted.isEmpty()) yield()
        }
        collector.cancelAndJoin()

        assertEquals(1, emitted.size)
        assertEquals(0.92f, emitted.single().nluConfidence)
    }

    private fun createEngine(
        config: LisomConfig = LisomConfig(wakeWordEnabled = false),
        speechSynthesizer: FakeSpeechSynthesizer? = null,
    ): LisomAssistantEngine = LisomAssistantEngine(
        config = config,
        audioInput = FakeAudioInput(),
        speechRecognizer = FakeSpeechRecognizer(),
        commandInterpreter = SpanishRuleBasedInterpreter(),
        speechSynthesizer = speechSynthesizer,
    )
}
