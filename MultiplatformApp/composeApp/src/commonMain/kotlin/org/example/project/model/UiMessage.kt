package org.example.project.model

sealed class UiMessage {
    abstract val text: String
    abstract val autoDismiss: Boolean

    data class Success(
        override val text: String,
        override val autoDismiss: Boolean = true
    ) : UiMessage()

    data class Info(
        override val text: String,
        override val autoDismiss: Boolean = false
    ) : UiMessage()

    data class Warning(
        override val text: String,
        override val autoDismiss: Boolean = false
    ) : UiMessage()

    data class Error(
        override val text: String,
        val isNetworkError: Boolean = false,
        override val autoDismiss: Boolean = false
    ) : UiMessage()
}
