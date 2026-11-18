package dev.lisom.nlu

import dev.lisom.models.LisomCommand

/**
 * Deterministic, fully local Spanish intent parser for the first SDK milestone.
 * This is a starter grammar, not a statistical or generative language model.
 */
class SpanishRuleBasedInterpreter : CommandInterpreter {
    override fun interpret(text: String): InterpretationResult {
        val normalized = normalize(text)
        if (normalized.isBlank()) return InterpretationResult(null, normalized, 0f, "empty_input")

        val command = when {
            isAny(normalized, "pausa", "pausa la musica", "deten la musica", "detener") ->
                LisomCommand.Pause
            isAny(normalized, "continua", "continua la musica", "reanuda", "reanuda la musica", "sigue") ->
                LisomCommand.Resume
            isAny(normalized, "siguiente", "siguiente cancion", "pasa a la siguiente", "pasa a la siguiente cancion", "pon la siguiente") ->
                LisomCommand.Next
            isAny(normalized, "anterior", "cancion anterior", "pasa a la anterior", "vuelve a la anterior") ->
                LisomCommand.Previous
            isAny(normalized, "muestra la letra", "muestra las letras", "ver letra", "quiero ver la letra", "quiero escuchar las letras", "mostrar letra") ->
                LisomCommand.ShowLyrics
            isAny(normalized, "oculta la letra", "oculta las letras", "cerrar letra", "ocultar letra") ->
                LisomCommand.HideLyrics
            normalized.startsWith("reproduce lista ") ->
                normalized.removePrefix("reproduce lista ").takeIf(String::isNotBlank)?.let(LisomCommand::PlayPlaylist)
            normalized.startsWith("reproduce mi lista ") ->
                normalized.removePrefix("reproduce mi lista ").takeIf(String::isNotBlank)
                    ?.let { LisomCommand.PlayPlaylist("mi lista $it") }
            normalized.startsWith("reproduce la cancion ") ->
                parseTrack(normalized.removePrefix("reproduce la cancion "))
            normalized.startsWith("reproduce cancion ") ->
                parseTrack(normalized.removePrefix("reproduce cancion "))
            normalized.startsWith("busca canciones de ") ->
                normalized.removePrefix("busca canciones de ").takeIf(String::isNotBlank)?.let(LisomCommand::SearchArtist)
            normalized.startsWith("busca artista ") ->
                normalized.removePrefix("busca artista ").takeIf(String::isNotBlank)?.let(LisomCommand::SearchArtist)
            normalized.startsWith("reproduce musica de ") ->
                normalized.removePrefix("reproduce musica de ").takeIf(String::isNotBlank)?.let(LisomCommand::PlayArtist)
            normalized.startsWith("pon musica de ") ->
                normalized.removePrefix("pon musica de ").takeIf(String::isNotBlank)?.let(LisomCommand::PlayArtist)
            normalized.startsWith("pon canciones de ") ->
                normalized.removePrefix("pon canciones de ").takeIf(String::isNotBlank)?.let(LisomCommand::PlayArtist)
            normalized.startsWith("quiero escuchar musica de ") ->
                normalized.removePrefix("quiero escuchar musica de ").takeIf(String::isNotBlank)?.let(LisomCommand::PlayArtist)
            normalized.startsWith("busca ") ->
                normalized.removePrefix("busca ").takeIf(String::isNotBlank)?.let(LisomCommand::SearchMusic)
            else -> null
        }

        return if (command == null) {
            InterpretationResult(null, normalized, 0.25f, "unmatched_intent")
        } else {
            InterpretationResult(command, normalized, 0.92f)
        }
    }

    private fun parseTrack(value: String): LisomCommand? {
        if (value.isBlank()) return null
        val parts = value.split(" de ", limit = 2)
        return if (parts.size == 2 && parts[0].isNotBlank() && parts[1].isNotBlank()) {
            LisomCommand.PlayTrack(title = parts[0], artist = parts[1])
        } else {
            LisomCommand.PlayTrack(title = value)
        }
    }

    private fun isAny(value: String, vararg candidates: String): Boolean = candidates.any { value == it }

    private fun normalize(value: String): String {
        var result = value.lowercase().trim()
            .replace("á", "a").replace("é", "e").replace("í", "i")
            .replace("ó", "o").replace("ú", "u").replace("ü", "u")
        result = result.replace(Regex("[^a-z0-9ñ\\s]"), " ")
        result = result.replace(Regex("\\s+"), " ").trim()
        if (result.startsWith("oye lisom ")) result = result.removePrefix("oye lisom ").trim()
        if (result.startsWith("lisom ")) result = result.removePrefix("lisom ").trim()
        return result
    }
}
