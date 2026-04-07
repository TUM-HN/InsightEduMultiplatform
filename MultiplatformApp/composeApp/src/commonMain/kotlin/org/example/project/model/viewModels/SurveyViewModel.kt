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
import org.example.project.model.FileDto
import org.example.project.model.SurveyEntryDto
import org.example.project.model.UiMessage
import org.example.project.model.toDomain

class SurveyViewModel(private val repository: DiscordBotRepository, val isSimpleSurvey: Boolean): ViewModel() {
    data class SurveyUiState(
        val questions: List<Pair<String, String>> = listOf("" to "Difficulty"),
        val channels: List<Channel> = emptyList(),
        val buttonTypes: List<String> = listOf("Difficulty", "Score"),
        val textMessage: String? = null,
        val mainTopic: String? = null,
        val isLoadingChannels: Boolean = false,

        val channelStatusMessage: UiMessage? = null,
        val surveyActionMessage: UiMessage? = null,
        val historyActionMessage: UiMessage? = null,

        val selectedChannel: Channel? = null,
        val selectedButtonType: String? = null,
        val duration: Int = 30,
        val isSending: Boolean = false,

        val surveyHistory: List<FileDto> = emptyList(),
        val isLoadingHistory: Boolean = false,
        val surveyDetails: List<SurveyEntryDto> = emptyList(),
        val isLoadingDetails: Boolean = false,
        val selectedSurveyFile: FileDto? = null,
    )

    private val _uiState = MutableStateFlow(SurveyUiState())
    val uiState: StateFlow<SurveyUiState> = _uiState.asStateFlow()

    fun loadSurveyUiState() {
        viewModelScope.launch {
            repository.botStatus.collect { status ->
                val channels = status.channels.map { it.toDomain() }.filter { it.type == "text" }
                _uiState.update {
                    it.copy(
                        channels = channels,
                        channelStatusMessage = if (status.errorMessage != null) UiMessage.Error(status.errorMessage) else null
                    )
                }
            }
        }
    }

    fun fetchSurveyHistory() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingHistory = true, historyActionMessage = null) }
            val response = repository.fetchSurveyFiles()
            if (response is RepositoryResponse.Success) {
                _uiState.update { it ->
                    it.copy(
                        surveyHistory = if (isSimpleSurvey) {
                            response.data.files.filter { it.name.contains("SS_") }.sortedByDescending { it.created }
                        } else {
                            response.data.files.filter { it.name.contains("CS_") }.sortedByDescending { it.created }
                        },
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

    fun fetchSurveyContent(file: FileDto) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingDetails = true, selectedSurveyFile = file, historyActionMessage = null) }
            val response = repository.fetchSurveyContent(file.name)
            if (response is RepositoryResponse.Success) {
                _uiState.update {
                    it.copy(
                        surveyDetails = response.data.content,
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
        _uiState.update { it.copy(selectedSurveyFile = null, surveyDetails = emptyList()) }
    }

    fun onMessageChanged(newMessage: String?) {
        _uiState.update { it.copy(textMessage = newMessage) }
    }

    fun onChannelSelected(channel: Channel?) {
        _uiState.update { it.copy(selectedChannel = channel) }
    }

    fun onMainTopicChanged(newTopic: String) {
        _uiState.update { it.copy(mainTopic = newTopic) }
    }

    fun onButtonTypeSelected(buttonType: String?) {
        _uiState.update { it.copy(selectedButtonType = buttonType) }
    }

    fun onDurationChanged(duration: Int) {
        _uiState.update { it.copy(duration = duration) }
    }

    fun createSimpleSurvey() {
        val currentState = _uiState.value
        val message = currentState.textMessage ?: return
        val mainTopic = currentState.mainTopic ?: return
        val channel = currentState.selectedChannel ?: return
        val buttonType = currentState.selectedButtonType ?: return
        val duration = currentState.duration

        viewModelScope.launch {
            _uiState.update { it.copy(isSending = true, surveyActionMessage = null) }
            val response = repository.createSimpleSurvey(
                message = message,
                mainTopic = mainTopic,
                channelId = channel.id,
                buttonType = buttonType,
                duration = duration
            )

            val actionMessage = when (response) {
                is RepositoryResponse.Success -> UiMessage.Success(response.message)
                is RepositoryResponse.Error -> UiMessage.Error(response.message, response.isNetworkError)
            }

            _uiState.update {
                it.copy(
                    isSending = false,
                    surveyActionMessage = actionMessage
                )
            }
        }
    }

    fun addQuestion() {
        _uiState.update { it.copy(questions = it.questions + ("" to "Difficulty")) }
    }

    fun removeQuestion(index: Int) {
        _uiState.update { state ->
            if (state.questions.size > 1) {
                state.copy(questions = state.questions.filterIndexed { i, _ -> i != index })
            } else state
        }
    }

    fun updateQuestion(index: Int, text: String) {
        _uiState.update { state ->
            state.copy(questions = state.questions.mapIndexed { i, pair ->
                if (i == index) text to pair.second else pair
            })
        }
    }

    fun updateQuestionButtonType(index: Int, buttonType: String) {
        _uiState.update { state ->
            state.copy(questions = state.questions.mapIndexed { i, pair ->
                if (i == index) pair.first to buttonType else pair
            })
        }
    }

    fun createComplexSurvey() {
        val currentState = _uiState.value
        val message = currentState.textMessage ?: return
        val mainTopic = currentState.mainTopic ?: return
        val channel = currentState.selectedChannel ?: return
        val questions = currentState.questions
        val duration = currentState.duration

        viewModelScope.launch {
            _uiState.update { it.copy(isSending = true, surveyActionMessage = null) }
            val response = repository.createComplexSurvey(
                message = message,
                mainTopic = mainTopic,
                channelId = channel.id,
                questions = questions,
                duration = duration
            )

            val actionMessage = when (response) {
                is RepositoryResponse.Success -> UiMessage.Success(response.message)
                is RepositoryResponse.Error -> UiMessage.Error(response.message, response.isNetworkError)
            }

            _uiState.update {
                it.copy(
                    isSending = false,
                    surveyActionMessage = actionMessage
                )
            }
        }
    }

    fun clearSurveyActionMessage() {
        _uiState.update { it.copy(surveyActionMessage = null) }
    }

    fun clearHistoryActionMessage() {
        _uiState.update { it.copy(historyActionMessage = null) }
    }
}
