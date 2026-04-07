package org.example.project.model.viewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.example.project.data.DiscordBotRepository
import org.example.project.data.RepositoryResponse
import org.example.project.model.Channel
import org.example.project.model.QAPair
import org.example.project.model.QuizStatistics
import org.example.project.model.UiMessage
import org.example.project.model.toDomain

data class ContentQuestionsUiState(
    val selectedFileName: String? = null,
    val selectedFileBytes: ByteArray? = null,
    val numberOfQuestions: Int = 5,
    val isSending: Boolean = false,

    val settingsStatusMessage: UiMessage? = null,
    val generateActionMessage: UiMessage? = null,
    val publishActionMessage: UiMessage? = null,
    val historyActionMessage: UiMessage? = null,

    val generatedQAPairs: List<QAPair> = emptyList(),
    val editingIndex: Int? = null,
    val editingQuestion: String = "",
    val editingAnswer: String = "",
    val channels: List<Channel> = emptyList(),
    val selectedChannel: Channel? = null,
    val duration: Int = 300,
    val aiFeedback: Boolean = false,

    val quizHistory: List<QuizStatistics> = emptyList(),
    val isLoadingHistory: Boolean = false,
    val selectedQuiz: QuizStatistics? = null,
)

class ContentQuestionsViewModel(private val repository: DiscordBotRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(ContentQuestionsUiState())
    val uiState: StateFlow<ContentQuestionsUiState> = _uiState.asStateFlow()

    fun onFileSelected(fileName: String, fileBytes: ByteArray) {
        _uiState.update { it.copy(selectedFileName = fileName, selectedFileBytes = fileBytes, generateActionMessage = null) }
    }

    fun onNumberOfQuestionsChanged(count: Int) {
        _uiState.update { it.copy(numberOfQuestions = count.coerceIn(1, 5)) }
    }

    fun clearFile() {
        _uiState.update { it.copy(selectedFileName = null, selectedFileBytes = null, generateActionMessage = null) }
    }

    fun submitContentQuestions() {
        val state = _uiState.value
        val fileBytes = state.selectedFileBytes ?: return
        val fileName = state.selectedFileName ?: return

        _uiState.update { it.copy(isSending = true, generateActionMessage = null) }

        viewModelScope.launch {
            val response = repository.generateQA(fileName, fileBytes, state.numberOfQuestions)
            when (response) {
                is RepositoryResponse.Success -> {
                    val sanitizedQAPairs = response.data.qaPairs.map { pair ->
                        if (pair.question.length > 100) {
                            pair.copy(question = pair.question.take(100))
                        } else {
                            pair
                        }
                    }
                    _uiState.update {
                        it.copy(
                            isSending = false,
                            generateActionMessage = UiMessage.Success("Generated ${response.data.qaPairs.size} questions successfully."),
                            generatedQAPairs = sanitizedQAPairs
                        )
                    }
                }
                is RepositoryResponse.Error -> {
                    _uiState.update {
                        it.copy(isSending = false, generateActionMessage = UiMessage.Error(response.message, response.isNetworkError))
                    }
                }
            }
        }
    }

    fun startEditing(index: Int) {
        val qaPair = _uiState.value.generatedQAPairs.getOrNull(index) ?: return
        _uiState.update {
            it.copy(
                editingIndex = index,
                editingQuestion = qaPair.question,
                editingAnswer = qaPair.answer
            )
        }
    }

    fun updateEditingQuestion(question: String) {
        if (question.length <= 100) {
            _uiState.update { it.copy(editingQuestion = question) }
        }
    }

    fun updateEditingAnswer(answer: String) {
        _uiState.update { it.copy(editingAnswer = answer) }
    }

    fun saveEditing() {
        val state = _uiState.value
        val index = state.editingIndex ?: return
        val updatedList = state.generatedQAPairs.toMutableList()
        updatedList[index] = QAPair(state.editingQuestion, state.editingAnswer)
        _uiState.update {
            it.copy(
                generatedQAPairs = updatedList,
                editingIndex = null,
                editingQuestion = "",
                editingAnswer = ""
            )
        }
    }

    fun cancelEditing() {
        val editingIdx = _uiState.value.editingIndex
        if (_uiState.value.editingQuestion == "" && _uiState.value.editingAnswer == "" && editingIdx != null) {
            val updatedList = _uiState.value.generatedQAPairs.toMutableList()
            updatedList.removeAt(editingIdx)
            _uiState.update { it.copy(generatedQAPairs = updatedList) }
        }
        _uiState.update {
            it.copy(editingIndex = null, editingQuestion = "", editingAnswer = "")
        }
    }

    fun deleteQAPair(index: Int) {
        val updatedList = _uiState.value.generatedQAPairs.toMutableList()
        updatedList.removeAt(index)
        _uiState.update { it.copy(generatedQAPairs = updatedList) }
    }

    fun addNewQAPair() {
        val updatedList = _uiState.value.generatedQAPairs + QAPair("", "")
        _uiState.update {
            it.copy(
                generatedQAPairs = updatedList,
                editingIndex = updatedList.lastIndex,
                editingQuestion = "",
                editingAnswer = ""
            )
        }
    }

    fun setQAPairs(qaPairs: List<QAPair>) {
        val sanitizedQAPairs = qaPairs.map { pair ->
            if (pair.question.length > 100) {
                pair.copy(question = pair.question.take(100))
            } else {
                pair
            }
        }
        _uiState.update { it.copy(generatedQAPairs = sanitizedQAPairs) }
    }

    fun loadChannels() {
        viewModelScope.launch {
            repository.botStatus.collect { status ->
                val channels = status.channels.map { it.toDomain() }.filter { it.type == "text" }
                _uiState.update {
                    it.copy(
                        channels = channels,
                        settingsStatusMessage = if (status.errorMessage != null) UiMessage.Error(status.errorMessage) else null
                    )
                }
            }
        }
    }

    fun onChannelSelected(channel: Channel?) {
        _uiState.update { it.copy(selectedChannel = channel) }
    }

    fun onDurationChanged(duration: Int) {
        _uiState.update { it.copy(duration = duration) }
    }

    fun onAiFeedbackChange(aiFeedback: Boolean) {
        _uiState.update { it.copy(aiFeedback = aiFeedback) }
    }

    fun sendQuestionsToDiscord() {
        val state = _uiState.value
        val channel = state.selectedChannel ?: return
        if (state.generatedQAPairs.isEmpty()) return

        _uiState.update { it.copy(isSending = true, publishActionMessage = null) }

        viewModelScope.launch {
            val response = repository.saveQAPairs(state.generatedQAPairs, channel.id, state.duration, state.aiFeedback)
            when (response) {
                is RepositoryResponse.Success -> {
                    _uiState.update {
                        it.copy(isSending = false, publishActionMessage = UiMessage.Success("Questions sent to Discord successfully."))
                    }
                    viewModelScope.launch {
                        delay((state.duration + state.numberOfQuestions * 2 + 10) * 1000L)
                        fetchQAStatistics()
                    }
                }
                is RepositoryResponse.Error -> {
                    _uiState.update {
                        it.copy(isSending = false, publishActionMessage = UiMessage.Error(response.message, response.isNetworkError))
                    }
                }
            }
        }
    }

    fun fetchQAStatistics() {
        _uiState.update { it.copy(isLoadingHistory = true, historyActionMessage = null) }
        viewModelScope.launch {
            when (val response = repository.fetchQAStatistics(aiFeedbackOnly = true)) {
                is RepositoryResponse.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoadingHistory = false,
                            quizHistory = response.data.statistics.quizzes
                        )
                    }
                }
                is RepositoryResponse.Error -> {
                    _uiState.update {
                        it.copy(isLoadingHistory = false, historyActionMessage = UiMessage.Error(response.message, response.isNetworkError))
                    }
                }
            }
        }
    }

    fun selectQuiz(quiz: QuizStatistics) {
        _uiState.update { it.copy(selectedQuiz = quiz) }
    }

    fun clearSelectedQuiz() {
        _uiState.update { it.copy(selectedQuiz = null) }
    }

    fun clearGenerateActionMessage() {
        _uiState.update { it.copy(generateActionMessage = null) }
    }

    fun clearPublishActionMessage() {
        _uiState.update { it.copy(publishActionMessage = null) }
    }

    fun clearHistoryActionMessage() {
        _uiState.update { it.copy(historyActionMessage = null) }
    }
}
