package org.example.project.model.viewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.example.project.data.DiscordBotRepository
import org.example.project.data.RepositoryResponse
import org.example.project.model.Channel
import org.example.project.model.UiMessage
import org.example.project.model.toDomain

class ClearMessagesViewModel(private val repository: DiscordBotRepository) : ViewModel() {

    data class ClearMessagesUiState(
        val channels: List<Channel> = emptyList(),
        val isLoading: Boolean = false,
        val isClearing: Boolean = false,
        val statusMessage: UiMessage? = null,
        val actionMessage: UiMessage? = null,
    )

    private val _uiState = MutableStateFlow(ClearMessagesUiState())
    val uiState: StateFlow<ClearMessagesUiState> = _uiState.asStateFlow()

    fun loadClearMessagesUiState() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            repository.botStatus.collect { status ->
                val channels = status.channels.map { it.toDomain() }.filter { it.type == "text" }
                _uiState.update { it.copy(
                    channels = channels,
                    isLoading = false,
                    statusMessage = if (status.errorMessage != null) UiMessage.Error(status.errorMessage) else null
                )}
            }
        }
    }

    fun clearMessages(channel: Channel, limit: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isClearing = true, actionMessage = null) }
            val actionMessage = when (val response = repository.clearMessages(channel.id, limit)) {
                is RepositoryResponse.Success -> UiMessage.Success(response.message)
                is RepositoryResponse.Error -> UiMessage.Error(response.message, response.isNetworkError)
            }

            _uiState.update {
                it.copy(
                    isClearing = false,
                    actionMessage = actionMessage
                )
            }
        }
    }

    fun clearActionMessage() {
        _uiState.update { it.copy(actionMessage = null) }
    }
}
