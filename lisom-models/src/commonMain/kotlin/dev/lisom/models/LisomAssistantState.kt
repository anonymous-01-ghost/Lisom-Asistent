package dev.lisom.models

sealed interface LisomAssistantState {
    data object Uninitialized : LisomAssistantState
    data object Ready : LisomAssistantState
    data object Listening : LisomAssistantState
    data object Processing : LisomAssistantState
    data object Speaking : LisomAssistantState
    data class Error(val code: String, val message: String, val recoverable: Boolean = true) : LisomAssistantState
    data object Closed : LisomAssistantState
}
