package dev.lisom.nlu

import dev.lisom.models.LisomCommand

/**
 * Deterministic, fully local Spanish intent parser.
 *
 * This grammar intentionally returns an unknown intent when a phrase is ambiguous;
 * the host application remains responsible for executing recognized commands.
 */
class SpanishRuleBasedInterpreter : CommandInterpreter {
    override fun interpret(text: String): InterpretationResult {
        val normalized = normalize(text)
        if (normalized.isBlank()) return InterpretationResult(null, normalized, 0f, "empty_input")

        val command = when {
            isAny(
                normalized, "pausa", "pausa la musica", "pausa musica", "deten la musica",
                "deten la reproduccion", "para la musica", "para la reproduccion", "detener",
            ) -> LisomCommand.Pause

            isAny(
                normalized, "continua", "continua la musica", "reanuda", "reanuda la musica",
                "sigue", "sigue reproduciendo", "continua reproduciendo",
            ) -> LisomCommand.Resume

            isAny(
                normalized, "siguiente", "siguiente cancion", "pasa a la siguiente",
                "pasa a la siguiente cancion", "pon la siguiente", "salta esta cancion",
                "reproduce la siguiente",
            ) -> LisomCommand.Next

            isAny(
                normalized, "anterior", "cancion anterior", "pasa a la anterior",
                "vuelve a la anterior", "regresa a la cancion anterior",
            ) -> LisomCommand.Previous

            isAny(
                normalized, "muestra la letra", "muestra las letras", "ver letra",
                "quiero ver la letra", "quiero escuchar las letras", "mostrar letra",
                "ensename la letra", "abre la letra",
            ) -> LisomCommand.ShowLyrics

            isAny(normalized, "oculta la letra", "oculta las letras", "cerrar letra", "ocultar letra") ->
                LisomCommand.HideLyrics

            normalized.startsWith("busca la cancion que dice ") ->
                searchLyrics(normalized.removePrefix("busca la cancion que dice "))
            normalized.startsWith("busca canciones que dicen ") ->
                searchLyrics(normalized.removePrefix("busca canciones que dicen "))
            normalized.startsWith("busca en las letras ") ->
                searchLyrics(normalized.removePrefix("busca en las letras "))
            normalized.startsWith("encuentra la cancion que dice ") ->
                searchLyrics(normalized.removePrefix("encuentra la cancion que dice "))

            parseVolume(normalized) != null -> parseVolume(normalized)

            normalized.startsWith("reproduce mi lista ") ->
                normalized.removePrefix("reproduce mi lista ").takeIf(String::isNotBlank)
                    ?.let { LisomCommand.PlayPlaylist("mi lista $it") }
            normalized.startsWith("reproduce lista ") ->
                normalized.removePrefix("reproduce lista ").takeIf(String::isNotBlank)?.let(LisomCommand::PlayPlaylist)
            normalized.startsWith("pon mi lista ") ->
                normalized.removePrefix("pon mi lista ").takeIf(String::isNotBlank)
                    ?.let { LisomCommand.PlayPlaylist("mi lista $it") }
            normalized.startsWith("pon la lista ") ->
                normalized.removePrefix("pon la lista ").takeIf(String::isNotBlank)?.let(LisomCommand::PlayPlaylist)

            normalized.startsWith("reproduce la cancion ") ->
                parseTrack(normalized.removePrefix("reproduce la cancion "))
            normalized.startsWith("reproduce cancion ") ->
                parseTrack(normalized.removePrefix("reproduce cancion "))
            normalized.startsWith("pon la cancion ") ->
                parseTrack(normalized.removePrefix("pon la cancion "))
            normalized.startsWith("pon cancion ") ->
                parseTrack(normalized.removePrefix("pon cancion "))
            normalized.startsWith("busca canciones de ") ->
                normalized.removePrefix("busca canciones de ").takeIf(String::isNotBlank)?.let(LisomCommand::SearchArtist)
            normalized.startsWith("busca artista ") ->
                normalized.removePrefix("busca artista ").takeIf(String::isNotBlank)?.let(LisomCommand::SearchArtist)
            normalized.startsWith("busca musica de ") ->
                normalized.removePrefix("busca musica de ").takeIf(String::isNotBlank)?.let(LisomCommand::SearchArtist)

            normalized.startsWith("reproduce musica de ") ->
                normalized.removePrefix("reproduce musica de ").takeIf(String::isNotBlank)?.let(LisomCommand::PlayArtist)
            normalized.startsWith("pon musica de ") ->
                normalized.removePrefix("pon musica de ").takeIf(String::isNotBlank)?.let(LisomCommand::PlayArtist)
            normalized.startsWith("pon canciones de ") ->
                normalized.removePrefix("pon canciones de ").takeIf(String::isNotBlank)?.let(LisomCommand::PlayArtist)
            normalized.startsWith("quiero escuchar musica de ") ->
                normalized.removePrefix("quiero escuchar musica de ").takeIf(String::isNotBlank)?.let(LisomCommand::PlayArtist)
            normalized.startsWith("quiero escuchar ") ->
                parseTrack(normalized.removePrefix("quiero escuchar "))

            normalized.startsWith("busca ") ->
                normalized.removePrefix("busca ").takeIf(String::isNotBlank)?.let(LisomCommand::SearchMusic)
            normalized.startsWith("encuentra ") ->
                normalized.removePrefix("encuentra ").takeIf(String::isNotBlank)?.let(LisomCommand::SearchMusic)
            else -> null
        }

        return if (command == null) {
            InterpretationResult(null, normalized, 0.25f, "unmatched_intent")
        } else {
            InterpretationResult(command, normalized, 0.92f)
        }
    }

    private fun searchLyrics(phrase: String): LisomCommand? =
        phrase.trim().takeIf(String::isNotBlank)?.let(LisomCommand::SearchLyrics)

    private fun parseVolume(value: String): LisomCommand.SetVolume? {
        val patterns = listOf(
            Regex("^(?:pon|ajusta|establece) (?:el )?volumen (?:a|al|en) ([0-9]{1,3})$"),
            Regex("^volumen (?:a|al|en) ([0-9]{1,3})$"),
            Regex("^(?:sube|baja) (?:el )?volumen (?:a|al|hasta) ([0-9]{1,3})$"),
        )
        val amount = patterns.firstNotNullOfOrNull { it.matchEntire(value)?.groupValues?.get(1) }
            ?.toIntOrNull()
            ?.takeIf { it in 0..100 }
            ?: return null
        return LisomCommand.SetVolume(amount)
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
