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
import org.example.project.model.FeedbackEntryDto
import org.example.project.model.FileDto
import org.example.project.model.GroupModel
import org.example.project.model.UiMessage
import org.example.project.model.toDomain

class TutorFeedbackViewModel(private val repository: DiscordBotRepository): ViewModel() {
    data class TutorFeedbackUiState(
        val channels: List<Channel> = emptyList(),
        val groups: List<GroupModel> = emptyList(),
        val isLoadingChannels: Boolean = false,

        val configurationStatusMessage: UiMessage? = null,
        val configurationActionMessage: UiMessage? = null,
        val analysisActionMessage: UiMessage? = null,
        val historyActionMessage: UiMessage? = null,

        val selectedGroup: GroupModel? = null,
        val selectedChannel: Channel? = null,
        val duration: Int = 60,
        val isSending: Boolean = false,
        val isGeneratingAnalysis: Boolean = false,

        val feedbackHistory: List<FileDto> = emptyList(),
        val isLoadingHistory: Boolean = false,
        val feedbackDetails: List<FeedbackEntryDto> = emptyList(),
        val isLoadingDetails: Boolean = false,
        val selectedFeedbackFile: FileDto? = null,

        val includeStatistics: Boolean = true,
        val selectedAnalysisChannels: List<Channel> = emptyList(),
        val startDate: String = "",
        val endDate: String = "",
        val aiAnalysis: String? = null,
        val isLoadingAnalysis: Boolean = false,
    )

    private val _uiState = MutableStateFlow(TutorFeedbackUiState())
    val uiState: StateFlow<TutorFeedbackUiState> = _uiState.asStateFlow()

    fun loadFeedbackUiState() {
        viewModelScope.launch {
            val settings = repository.getInitialSettings()
            _uiState.update {
                it.copy(
                    groups = settings.groups.filter { it.isValid }
                )
            }

            repository.botStatus.collect { status ->
                val channels = status.channels.map { it.toDomain() }.filter { it.type == "text" }
                _uiState.update {
                    it.copy(
                        channels = channels,
                        configurationStatusMessage = if (status.errorMessage != null) UiMessage.Error(status.errorMessage) else null
                    )
                }
            }
        }
    }

    fun fetchFeedbackHistory() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingHistory = true, historyActionMessage = null) }
            val response = repository.fetchFeedbackFiles()
            if (response is RepositoryResponse.Success) {
                val userGroups = _uiState.value.groups.map { it.name.lowercase() }
                val filteredFiles = response.data.files.filter { file ->
                    userGroups.any { groupName -> file.name.lowercase().contains(groupName) }
                }.sortedByDescending { it.created }
                _uiState.update {
                    it.copy(
                        feedbackHistory = filteredFiles,
                        isLoadingHistory = false
                    )
                }
            } else {
                val isNetworkError = if (response is RepositoryResponse.Error) response.isNetworkError else false
                _uiState.update {
                    it.copy(
                        isLoadingHistory = false,
                        historyActionMessage = UiMessage.Error(response.message, isNetworkError)
                    )
                }
            }
        }
    }

    fun fetchFeedbackContent(file: FileDto) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingDetails = true, selectedFeedbackFile = file, historyActionMessage = null) }
            val response = repository.fetchFeedbackContent(file.name)
            if (response is RepositoryResponse.Success) {
                _uiState.update {
                    it.copy(
                        feedbackDetails = response.data.content,
                        isLoadingDetails = false
                    )
                }
            } else {
                val isNetworkError = if (response is RepositoryResponse.Error) response.isNetworkError else false
                _uiState.update {
                    it.copy(
                        isLoadingDetails = false,
                        historyActionMessage = UiMessage.Error(response.message, isNetworkError)
                    )
                }
            }
        }
    }

    fun clearSelectedHistory() {
        _uiState.update { it.copy(selectedFeedbackFile = null, feedbackDetails = emptyList()) }
    }

    fun onGroupSelected(group: GroupModel?) {
        _uiState.update { it.copy(
            selectedGroup = group,
            duration = 30,
        ) }
    }

    fun onChannelSelected(channel: Channel?) {
        _uiState.update { it.copy(selectedChannel = channel) }
    }

    fun onDurationChanged(duration: Int) {
        _uiState.update { it.copy(duration = duration) }
    }

    fun onStartDateChanged(date: String) {
        _uiState.update { it.copy(startDate = date) }
    }

    fun onEndDateChanged(date: String) {
        _uiState.update { it.copy(endDate = date) }
    }

    fun onIncludeStatisticsChanged(value: Boolean) {
        _uiState.update { it.copy(includeStatistics = value) }
    }

    fun onChannelSelectionToggled(channel: Channel) {
        _uiState.update { state ->
            val currentSelected = state.selectedAnalysisChannels
            val newSelected = if (currentSelected.any { it.id == channel.id }) {
                currentSelected.filter { it.id != channel.id }
            } else {
                currentSelected + channel
            }
            state.copy(selectedAnalysisChannels = newSelected)
        }
    }

    fun generateAiAnalysis() {
        val currentState = _uiState.value
        val startDate = currentState.startDate
        val endDate = currentState.endDate
        val useStatistics = currentState.includeStatistics
        val channelIds = currentState.selectedAnalysisChannels.map { it.id }

        if (startDate.isEmpty() || endDate.isEmpty()) {
            _uiState.update { it.copy(analysisActionMessage = UiMessage.Warning("Please select start and end dates")) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingAnalysis = true, isGeneratingAnalysis = true, analysisActionMessage = null) }
            val response = repository.getAiFeedbackAnalysis(
                useStatistics = useStatistics,
                channelIds = channelIds,
                startDate = startDate,
                endDate = endDate
            )

            if (response is RepositoryResponse.Success) {
                _uiState.update {
                    it.copy(
                        aiAnalysis = response.data.analysis,
                        isLoadingAnalysis = false,
                        isGeneratingAnalysis = false,
                        analysisActionMessage = UiMessage.Success("Analysis generated successfully")
                    )
                }
            } else {
                val isNetworkError = if (response is RepositoryResponse.Error) response.isNetworkError else false
                _uiState.update {
                    it.copy(
                        isLoadingAnalysis = false,
                        isGeneratingAnalysis = false,
                        analysisActionMessage = UiMessage.Error(response.message, isNetworkError)
                    )
                }
            }
        }
    }

    fun manageFeedback() {
        val currentState = _uiState.value
        val group = currentState.selectedGroup ?: return
        val channel = currentState.selectedChannel ?: return
        val duration = currentState.duration

        viewModelScope.launch {
            _uiState.update { it.copy(isSending = true, configurationActionMessage = null) }
            val response = repository.manageFeedback(
                groupId = group.name,
                channelId = channel.id,
                duration = duration,
            )

            val actionMessage = when (response) {
                is RepositoryResponse.Success -> UiMessage.Success(response.message)
                is RepositoryResponse.Error -> UiMessage.Error(response.message, response.isNetworkError)
            }

            _uiState.update {
                it.copy(
                    isSending = false,
                    configurationActionMessage = actionMessage
                )
            }
        }
    }

    fun clearConfigurationActionMessage() {
        _uiState.update { it.copy(configurationActionMessage = null) }
    }

    fun clearAnalysisActionMessage() {
        _uiState.update { it.copy(analysisActionMessage = null) }
    }

    fun clearHistoryActionMessage() {
        _uiState.update { it.copy(historyActionMessage = null) }
    }
}
