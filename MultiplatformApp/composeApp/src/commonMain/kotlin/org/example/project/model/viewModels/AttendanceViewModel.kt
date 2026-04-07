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
import org.example.project.model.AttendanceEntryDto
import org.example.project.model.FileDto
import org.example.project.model.GroupModel
import org.example.project.model.Member
import org.example.project.model.UiMessage
import org.example.project.model.toDomain

class AttendanceViewModel(private val repository: DiscordBotRepository) : ViewModel() {

    data class AttendanceUiState(
        val admins: List<Member> = emptyList(),
        val groups: List<GroupModel> = emptyList(),
        val isLoadingAdmins: Boolean = false,

        val setupStatusMessage: UiMessage? = null,
        val attendanceActionMessage: UiMessage? = null,
        val historyActionMessage: UiMessage? = null,

        val selectedGroup: GroupModel? = null,
        val selectedAdmin: Member? = null,
        val attendanceCode: String = "",
        val isSending: Boolean = false,

        val attendanceHistory: List<FileDto> = emptyList(),
        val isLoadingHistory: Boolean = false,
        val attendanceDetails: List<AttendanceEntryDto> = emptyList(),
        val isLoadingDetails: Boolean = false,
        val selectedHistoryFile: FileDto? = null,
    )

    private val _uiState = MutableStateFlow(AttendanceUiState())
    val uiState: StateFlow<AttendanceUiState> = _uiState.asStateFlow()

    fun loadAttendanceUiState() {
        viewModelScope.launch {
            val settings = repository.getInitialSettings()
            _uiState.update {
                it.copy(
                    groups = settings.groups.filter { it.isValid }
                )
            }

            repository.botStatus.collect { status ->
                val adminRole = status.roles.find { it.name == "Admin" }?.id
                val admins = status.members.map { it.toDomain() }.filter { it.roles.contains(adminRole) }
                _uiState.update {
                    it.copy(
                        admins = admins,
                        setupStatusMessage = if (status.errorMessage != null) {
                            UiMessage.Error(status.errorMessage, isNetworkError = true)
                        } else null
                    )
                }
            }
        }
    }

    fun fetchAttendanceHistory() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingHistory = true, historyActionMessage = null) }
            val response = repository.fetchAttendanceFiles()
            if (response is RepositoryResponse.Success) {
                val userGroups = _uiState.value.groups.map { it.name.lowercase() }
                val filteredFiles = response.data.files.filter { file ->
                    userGroups.any { groupName -> file.name.lowercase().contains(groupName) }
                }.sortedByDescending { it.created }
                _uiState.update {
                    it.copy(
                        attendanceHistory = filteredFiles,
                        isLoadingHistory = false
                    )
                }
            } else {
                val actionMessage = when (response) {
                    is RepositoryResponse.Error -> UiMessage.Error(response.message, response.isNetworkError)
                }
                _uiState.update {
                    it.copy(
                        isLoadingHistory = false,
                        historyActionMessage = actionMessage
                    )
                }
            }
        }
    }

    fun fetchAttendanceContent(file: FileDto) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingDetails = true, selectedHistoryFile = file, historyActionMessage = null) }
            val response = repository.fetchAttendanceContent(file.name)
            if (response is RepositoryResponse.Success) {
                _uiState.update {
                    it.copy(
                        attendanceDetails = response.data.content,
                        isLoadingDetails = false
                    )
                }
            } else {
                val actionMessage = when (response) {
                    is RepositoryResponse.Error -> UiMessage.Error(response.message, response.isNetworkError)
                }
                _uiState.update {
                    it.copy(
                        isLoadingDetails = false,
                        historyActionMessage = actionMessage
                    )
                }
            }
        }
    }

    fun clearSelectedHistory() {
        _uiState.update { it.copy(selectedHistoryFile = null, attendanceDetails = emptyList()) }
    }

    fun onGroupSelected(group: GroupModel?) {
        _uiState.update { it.copy(
            selectedGroup = group,
            attendanceCode = group?.attendanceCode ?: ""
        ) }
    }

    fun onAdminSelected(admin: Member?) {
        _uiState.update { it.copy(selectedAdmin = admin) }
    }

    fun onAttendanceCodeChanged(code: String) {
        _uiState.update { state ->
            val updatedGroup = state.selectedGroup?.copy(attendanceCode = code)
            val updatedGroups = state.groups.map {
                if (it.name == updatedGroup?.name) updatedGroup else it
            }
            state.copy(
                attendanceCode = code,
                selectedGroup = updatedGroup,
                groups = updatedGroups
            )
        }

        LocalStorage.saveGroups(_uiState.value.groups)
    }

    fun manageAttendance() {
        val currentState = _uiState.value
        val group = currentState.selectedGroup ?: return
        val admin = currentState.selectedAdmin ?: return
        val code = currentState.attendanceCode

        viewModelScope.launch {
            _uiState.update { it.copy(isSending = true, attendanceActionMessage = null) }
            val status = if (group.attendanceActive) "stop" else "start"
            val response = repository.manageAttendance(
                groupId = group.name,
                targetUserId = admin.id,
                status = status,
                code = code
            )

            if (response is RepositoryResponse.Success) {
                val updatedGroup = group.copy(attendanceActive = !group.attendanceActive)
                val updatedGroups = currentState.groups.map {
                    if (it.name == group.name) updatedGroup else it
                }
                LocalStorage.saveGroups(updatedGroups)
                _uiState.update {
                    it.copy(
                        isSending = false,
                        selectedGroup = updatedGroup,
                        groups = updatedGroups,
                        attendanceActionMessage = UiMessage.Success(response.message)
                    )
                }
                if (!updatedGroup.attendanceActive) {
                    fetchAttendanceHistory()
                }
                repository.refreshStatus()
            } else {
                val actionMessage = when (response) {
                    is RepositoryResponse.Error -> UiMessage.Error(response.message, response.isNetworkError)
                }
                _uiState.update {
                    it.copy(
                        isSending = false,
                        attendanceActionMessage = actionMessage
                    )
                }
            }
        }
    }

    fun clearAttendanceActionMessage() {
        _uiState.update { it.copy(attendanceActionMessage = null) }
    }

    fun clearHistoryActionMessage() {
        _uiState.update { it.copy(historyActionMessage = null) }
    }
}
