package org.example.project.model.viewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.example.project.data.DiscordBotRepository
import org.example.project.data.RepositoryResponse
import org.example.project.model.UiMessage

data class HeaderUiState(
    val refreshCounter: Int = 0,
    val isServerOnline: Boolean = false,
    val isBotRunning: Boolean = false,
    val statusMessage: UiMessage? = null,
    val actionMessage: UiMessage? = null,

    val onlineMembers: Int = 0,
    val offlineMembers: Int = 0,
    val totalMembers: Int = 0,

    val isLoadingBotAction: Boolean = false,
    val showClearDataDialog: Boolean = false,
    val serverUrl : String = "",
)

class HeaderViewModel(private val repository: DiscordBotRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(HeaderUiState())
    val uiState = _uiState.asStateFlow()

    init {
        val (url, _, _) = repository.getInitialSettings()

        startStatusPooling()

        viewModelScope.launch {
            repository.botStatus.collect { status ->
                val members = status.members
                val onlineMembers = members.count { it.status.equals("online", ignoreCase = true) }
                val totalMembers = members.size
                val offlineMembers = (totalMembers - onlineMembers).coerceAtLeast(0)

                _uiState.update { it.copy(
                    serverUrl = status.serverUrl,
                    refreshCounter = status.refreshCounter,
                    isServerOnline = status.isServerOnline,
                    isBotRunning = status.isBotRunning,
                    statusMessage = if (status.errorMessage != null) {
                        UiMessage.Error(status.errorMessage, isNetworkError = true)
                    } else null,
                    onlineMembers = onlineMembers,
                    offlineMembers = offlineMembers,
                    totalMembers = totalMembers,
                )}
            }
        }
    }

    private fun startStatusPooling() {
        viewModelScope.launch {
            while (isActive) {
                repository.refreshStatus()
                delay(5000)
            }
        }
    }

    fun onStartStopClicked() {
        val state = _uiState.value
        if (state.isLoadingBotAction || !state.isServerOnline) return
        if (state.isBotRunning) stopBot() else startBot()
    }

    private fun startBot() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingBotAction = true, actionMessage = UiMessage.Info("Starting bot...")) }
            val response = repository.sendStart()
            val success = response is RepositoryResponse.Success

            val actionMessage = when (response) {
                is RepositoryResponse.Success -> UiMessage.Success(response.message)
                is RepositoryResponse.Error -> UiMessage.Error(response.message, response.isNetworkError)
            }

            _uiState.update {
                it.copy(
                    isLoadingBotAction = false,
                    isBotRunning = success,
                    actionMessage = actionMessage
                )
            }

            if (success) {
                delay(2000)
                repository.refreshStatus()
            }
        }
    }

    private fun stopBot() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingBotAction = true, actionMessage = UiMessage.Info("Stopping bot...")) }
            val response = repository.stopBot()
            val success = response is RepositoryResponse.Success

            val actionMessage = when (response) {
                is RepositoryResponse.Success -> UiMessage.Success(response.message)
                is RepositoryResponse.Error -> UiMessage.Error(response.message, response.isNetworkError)
            }

            _uiState.update {
                it.copy(
                    isLoadingBotAction = false,
                    isBotRunning = if (success) false else it.isBotRunning,
                    actionMessage = actionMessage,
                    onlineMembers = if (success) 0 else it.onlineMembers,
                    offlineMembers = if (success) 0 else it.offlineMembers,
                    totalMembers = if (success) 0 else it.totalMembers,
                )
            }
        }
    }

    fun clearActionMessage() {
        _uiState.update { it.copy(actionMessage = null) }
    }

    fun onClearDataClicked() {
        _uiState.update { it.copy(showClearDataDialog = true) }
    }

    fun onDismissClearDataDialog() {
        _uiState.update { it.copy(showClearDataDialog = false) }
    }

    fun onConfirmClearData() {
        _uiState.update { it.copy(showClearDataDialog = false, isLoadingBotAction = true, actionMessage = UiMessage.Info("Clearing all data...")) }
        viewModelScope.launch {
            val actionMessage = when (val response = repository.clearAllData()) {
                is RepositoryResponse.Success -> UiMessage.Success(response.message)
                is RepositoryResponse.Error -> UiMessage.Error(response.message, response.isNetworkError)
            }
            _uiState.update { it.copy(isLoadingBotAction = false, actionMessage = actionMessage) }
        }
    }
}
