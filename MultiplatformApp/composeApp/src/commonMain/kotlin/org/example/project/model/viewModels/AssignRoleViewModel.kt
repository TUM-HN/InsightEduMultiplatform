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
import org.example.project.model.Role
import org.example.project.model.UiMessage
import org.example.project.model.toDomain

class AssignRoleViewModel(private val repository: DiscordBotRepository) : ViewModel() {
    data class AssignRoleUiState(
        val members: List<Member> = emptyList(),
        val roles: List<Role> = emptyList(),
        val selectedMember: Member? = null,
        val selectedRoles: List<Role> = emptyList(),
        val initialRoleIds: Set<String> = emptySet(),
        val isLoading: Boolean = false,
        val isAssigning: Boolean = false,
        val statusMessage: UiMessage? = null,
        val actionMessage: UiMessage? = null,
    )

    private val _uiState = MutableStateFlow(AssignRoleUiState())
    val uiState: StateFlow<AssignRoleUiState> = _uiState.asStateFlow()

    fun loadGiveRoleUiState() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            repository.botStatus.collect { status ->
                val members = status.members.map { it.toDomain() }
                val roles = status.roles.map { it.toDomain() }
                _uiState.update {
                    it.copy(
                        members = members,
                        roles = roles,
                        isLoading = false,
                        statusMessage = if (status.errorMessage != null) UiMessage.Error(status.errorMessage) else null
                    )
                }
            }

        }
    }

    fun onMemberSelected(member: Member?) {
        _uiState.update {
            if (member == null) {
                it.copy(selectedMember = null, selectedRoles = emptyList(), initialRoleIds = emptySet())
            } else {
                val memberRoles = it.roles.filter { role -> member.roles.contains(role.id) }
                it.copy(
                    selectedMember = member,
                    selectedRoles = memberRoles,
                    initialRoleIds = member.roles.toSet()
                )
            }
        }
    }

    fun onRoleToggled(role: Role) {
        _uiState.update { state ->
            val currentSelected = state.selectedRoles
            val newSelected = if (currentSelected.contains(role)) {
                currentSelected - role
            } else {
                currentSelected + role
            }
            state.copy(selectedRoles = newSelected)
        }
    }

    fun saveChanges() {
        val state = _uiState.value
        val member = state.selectedMember ?: return
        val currentRoleIds = state.selectedRoles.map { it.id }.toSet()
        val initialRoleIds = state.initialRoleIds

        val rolesToAdd = currentRoleIds - initialRoleIds
        val rolesToRemove = initialRoleIds - currentRoleIds

        if (rolesToAdd.isEmpty() && rolesToRemove.isEmpty()) {
            _uiState.update { it.copy(actionMessage = UiMessage.Success("No changes to save.")) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isAssigning = true, actionMessage = null) }

            var success = true
            val errorMessages = mutableListOf<String>()

            rolesToAdd.forEach { roleId ->
                val response = repository.giveMemberRole(member.id, roleId)
                if (response is RepositoryResponse.Error) {
                    success = false
                    errorMessages.add("Add role ${roleId}: ${response.message}")
                }
            }

            rolesToRemove.forEach { roleId ->
                val response = repository.removeMemberRole(member.id, roleId)
                if (response is RepositoryResponse.Error) {
                    success = false
                    errorMessages.add("Remove role ${roleId}: ${response.message}")
                }
            }

            val finalMessage = if (success) {
                UiMessage.Success("Member roles updated successfully.")
            } else {
                UiMessage.Error("Some updates failed: ${errorMessages.joinToString(", ")}")
            }

            _uiState.update {
                it.copy(
                    isAssigning = false,
                    actionMessage = finalMessage,
                    initialRoleIds = if (success) currentRoleIds else it.initialRoleIds
                )
            }

            repository.refreshStatus()
        }
    }

    fun clearActionMessage() {
        _uiState.update { it.copy(actionMessage = null) }
    }
}
