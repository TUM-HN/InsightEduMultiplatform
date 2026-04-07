package org.example.project.model.viewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.example.project.data.DiscordBotRepository
import org.example.project.data.LocalStorage
import org.example.project.data.RepositoryResponse
import org.example.project.model.GroupModel
import org.example.project.model.UiMessage

class SettingsViewModel(private val repository: DiscordBotRepository) : ViewModel() {
    data class SettingsUiState(
        val serverUrl: String = "",
        val apiKey: String = "",
        val groups: List<GroupModel> = emptyList(),
        val developerMode: Boolean = false,
        val token: String = "",
        val devToken: String = "",
        val geminiApiKey: String = "",
        val groupsCount: Int = 10,

        val isServerEditing: Boolean = false,
        val isManagementEditing: Boolean = false,
        val isSaving: Boolean = false,
        val showDiscardDialog: Boolean = false,
        val isServerOnline: Boolean = false,
        val isBotRunning: Boolean = false,
        val serverActionMessage: UiMessage? = null,
        val managementActionMessage: UiMessage? = null,
    )
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    fun loadSettingsUiState() {
        val settings = repository.getInitialSettings()
        _uiState.update {
            it.copy(
                serverUrl = settings.serverUrl,
                apiKey = settings.apiKey,
                groups = settings.groups,
                groupsCount = settings.groups.size,
                developerMode = settings.developerMode,
                token = settings.token,
                devToken = settings.devToken,
                geminiApiKey = settings.geminiApiKey,
                isServerEditing = false,
                isManagementEditing = false,
                showDiscardDialog = false
            )
        }
        fetchSettingsFromServer()
        viewModelScope.launch {
            repository.botStatus.collect { botStatus ->
                _uiState.update { it.copy(isServerOnline = botStatus.isServerOnline, isBotRunning = botStatus.isBotRunning) }
            }
        }
    }

    fun fetchSettingsFromServer() {
        viewModelScope.launch {
            try {
                val response = repository.fetchSettings()
                if (response is RepositoryResponse.Success) {
                    val data = response.data.data
                    if (data != null) {
                        val bot = data.bot
                        _uiState.update {
                            it.copy(
                                developerMode = bot.developmentMode,
                                token = bot.token,
                                devToken = bot.devToken,
                                geminiApiKey = bot.geminiApiKey ?: "",
                                groups = data.groups.map { groupName ->
                                    GroupModel(name = groupName, isValid = true)
                                },
                                groupsCount = data.groups.size
                            )
                        }
                    }
                } else if (response is RepositoryResponse.Error) {
                    _uiState.update { it.copy(serverActionMessage = UiMessage.Error("Failed to fetch settings from server")) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(serverActionMessage = UiMessage.Error("Failed to fetch settings from server")) }
            }
        }
    }

    fun onGroupsCountChange(count: Int) {
        _uiState.update { state ->
            val currentGroups = state.groups
            val newGroups = if (count > currentGroups.size) {
                currentGroups + List(count - currentGroups.size) { i ->
                    GroupModel(name = "Group ${currentGroups.size + i + 1}", isValid = false)
                }
            } else {
                currentGroups.take(count)
            }
            state.copy(groups = newGroups, groupsCount = count)
        }
    }

    fun toggleManagementEditing() {
        val currentState = _uiState.value
        val developerMode = currentState.developerMode
        val token = currentState.token
        val devToken = currentState.devToken
        val geminiApiKey = currentState.geminiApiKey
        if (currentState.isManagementEditing) {
            _uiState.update { it.copy(isSaving = true, managementActionMessage = null) }
            viewModelScope.launch {
                try {
                    LocalStorage.saveGroups(currentState.groups)
                    LocalStorage.saveBotSettings(developerMode, token, devToken, geminiApiKey)

                    val updateDevMode = repository.updateDevelopmentMode(developerMode)
                    val updateToken = repository.updateBotToken(developerMode, if (developerMode) devToken else token)
                    val updateGemini = repository.updateGeminiApiKey(geminiApiKey)
                    val updateGroups = repository.updateGroups(currentState.groups.filter { it.isValid }.map { it.name })

                    if (updateDevMode is RepositoryResponse.Success &&
                        updateToken is RepositoryResponse.Success &&
                        updateGemini is RepositoryResponse.Success &&
                        updateGroups is RepositoryResponse.Success
                    ) {
                        _uiState.update { it.copy(managementActionMessage = UiMessage.Success("Successfully updated settings")) }
                    } else {
                        _uiState.update { it.copy(managementActionMessage = UiMessage.Error("Failed to update settings")) }
                    }

                } catch (e: Exception) {
                    _uiState.update { it.copy(managementActionMessage = UiMessage.Error("Failed to update settings")) }
                } finally {
                    _uiState.update { it.copy(isManagementEditing = false, isSaving = false) }
                }
            }
        } else {
            _uiState.update { it.copy(isManagementEditing = true, managementActionMessage = null) }
        }
    }

    fun onDeveloperModeChange(developerMode: Boolean) {
        _uiState.update { it.copy(developerMode = developerMode) }
    }

    fun onBotTokenChange(token: String) {
        _uiState.update { state ->
            if (state.developerMode) {
                state.copy(devToken = token)
            } else {
                state.copy(token = token)
            }
        }
    }

    fun onGeminiApiKeyChange(geminiApiKey: String) {
        _uiState.update { it.copy(geminiApiKey = geminiApiKey) }
    }

    fun onGroupValidityChange(groupName: String, isValid: Boolean) {
        _uiState.update { state ->
            state.copy(
                groups = state.groups.map {
                    if (it.name == groupName) it.copy(isValid = isValid) else it
                }
            )
        }
    }

    fun toggleServerEditing() {
        val currentState = _uiState.value
        val serverUrl = currentState.serverUrl
        val apiKey = currentState.apiKey
        if (currentState.isServerEditing) {
            if (!serverUrl.isEmpty()) {
                _uiState.update { it.copy(isSaving = true, serverActionMessage = null) }
                repository.updateSettings(serverUrl, apiKey)
                _uiState.update {
                    it.copy(
                        isServerEditing = false,
                        isSaving = false,
                        serverActionMessage = UiMessage.Success("Successfully updated settings")
                    )
                }
            }
        } else {
            _uiState.update { it.copy(isServerEditing = true, serverActionMessage = null) }
        }
    }

    fun clearServerActionMessage() {
        _uiState.update { it.copy(serverActionMessage = null) }
    }

    fun clearManagementActionMessage() {
        _uiState.update { it.copy(managementActionMessage = null) }
    }

    fun onServerUrlChange(url: String) {
        _uiState.update { it.copy(serverUrl = url) }
    }

    fun onApiKeyChange(key: String) {
        _uiState.update { it.copy(apiKey = key) }
    }

    fun onBack(onBack: () -> Unit) {
        viewModelScope.launch {
            if (_uiState.value.isServerEditing || _uiState.value.isManagementEditing) {
                _uiState.update { it.copy(showDiscardDialog = true) }
            } else {
                onBack()
                repository.refreshStatus()
            }
        }
    }

    fun confirmDiscard(onBack: () -> Unit) {
        val settings = repository.getInitialSettings()
        _uiState.update {
            it.copy(
                serverUrl = settings.serverUrl,
                apiKey = settings.apiKey,
                groups = settings.groups,
                groupsCount = settings.groups.size,
                developerMode = settings.developerMode,
                token = settings.token,
                devToken = settings.devToken,
                geminiApiKey = settings.geminiApiKey,
                isServerEditing = false,
                isManagementEditing = false,
                showDiscardDialog = false
            )
        }
        onBack()
    }

    fun dismissDiscardDialog() {
        _uiState.update { it.copy(showDiscardDialog = false) }
    }
}
