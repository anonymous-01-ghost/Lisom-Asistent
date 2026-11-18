package dev.lisom.demo

import dev.lisom.nlu.SpanishRuleBasedInterpreter

fun main(args: Array<String>) {
    val phrase = args.joinToString(" ").ifBlank { "Oye Lisom, reproduce música de Morat" }
    val result = SpanishRuleBasedInterpreter().interpret(phrase)
    println("Lisom Assistant - local NLU demo")
    println("Input: $phrase")
    println("Normalized: ${result.normalizedText}")
    println("Recognized: ${result.isRecognized}")
    println("Command: ${result.command ?: "none"}")
    println("Confidence: ${result.confidence}")
}
