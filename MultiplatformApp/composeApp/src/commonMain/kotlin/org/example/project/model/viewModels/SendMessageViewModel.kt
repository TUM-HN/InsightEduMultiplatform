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
import org.example.project.model.Member
import org.example.project.model.UiMessage
import org.example.project.model.toDomain

class SendMessageViewModel(private val repository: DiscordBotRepository) : ViewModel() {

    data class SendMessageUiState(
        val members: List<Member> = emptyList(),
        val isBotRunning: Boolean = false,
        val isLoading: Boolean = false,
        val isSending: Boolean = false,
        val statusMessage: UiMessage? = null,
        val actionMessage: UiMessage? = null,
    )

    private val _uiState = MutableStateFlow(SendMessageUiState())
    val uiState: StateFlow<SendMessageUiState> = _uiState.asStateFlow()

    fun loadSendMessageUiState() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            repository.botStatus.collect { status ->
                val members = status.members.map { it.toDomain() }
                _uiState.update { it.copy(
                    members = members,
                    isBotRunning = status.isBotRunning,
                    isLoading = false,
                    statusMessage = if (status.errorMessage != null) {
                        UiMessage.Error(status.errorMessage, isNetworkError = true)
                    } else null
                )}
            }
        }
    }


    fun sendMessageToMember(memberID: String, message: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSending = true, actionMessage = null) }
            val response = repository.sendMessage(memberID, message)

            val actionMessage = when (response) {
                is RepositoryResponse.Success -> UiMessage.Success(response.message)
                is RepositoryResponse.Error -> UiMessage.Error(response.message, response.isNetworkError)
            }

            _uiState.update {
                it.copy(
                    isSending = false,
                    actionMessage = actionMessage
                )
            }
        }
    }

    fun clearActionMessage() {
        _uiState.update { it.copy(actionMessage = null) }
    }
}
