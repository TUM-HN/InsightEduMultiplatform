package org.example.project.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.example.project.TUMColors
import org.example.project.data.DefaultAppContainer
import org.example.project.model.viewModels.AssignRoleViewModel
import org.example.project.ui.reusableComponents.CustomCard
import org.example.project.ui.reusableComponents.CustomDropdownMenu
import org.example.project.ui.reusableComponents.CustomHorizontalDivider
import org.example.project.ui.reusableComponents.CustomTextButton
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import org.example.project.ui.reusableComponents.CustomTopBar
import androidx.compose.ui.graphics.Color

@Composable
fun AssignRoleScreen(
    viewModel: AssignRoleViewModel,
    onBack: () -> Unit,
) {
    LaunchedEffect(Unit) {
        viewModel.loadGiveRoleUiState()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TUMColors.tumGray9)
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            CustomTopBar("Assign Role Command", onBack)
            ContentCard(viewModel = viewModel)
        }
    }
}

@Composable
private fun ContentCard(
    viewModel: AssignRoleViewModel
) {
    val uiState by viewModel.uiState.collectAsState()

    CustomCard(
        showTitle = false,
        statusMessage = uiState.statusMessage,
        actionMessage = uiState.actionMessage,
        onClearActionMessage = { viewModel.clearActionMessage() },
    ) {
        CustomDropdownMenu(
            onItemSelected = { member -> viewModel.onMemberSelected(member) },
            items = uiState.members,
            selectedItem = uiState.selectedMember,
            menuTitle = "Member",
            dropdownTitle = "Select a member",
            itemLabel = { member ->
                if (member != null) {
                    member.displayName + " (" + member.name + ")"
                } else "Select a member"
            }
        )

        CustomHorizontalDivider()

        CustomDropdownMenu(
            onItemSelected = { role ->
                if (role != null) {
                    viewModel.onRoleToggled(role)
                }
            },
            items = uiState.roles,
            selectedItem = null,
            menuTitle = "Roles",
            dropdownTitle = "Select roles",
            itemLabel = { role -> role?.name ?: "Select roles" },
            isMultiSelect = true,
            selectedItems = uiState.selectedRoles,
            isNoneOption = false,
            showRedStar = false
        )

        if (uiState.selectedMember != null && uiState.selectedRoles.isEmpty()) {
            Text(
                text = "No special roles",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray,
                modifier = Modifier.padding(16.dp).align(Alignment.CenterHorizontally)
            )
        }

        uiState.selectedRoles.forEach { role ->
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                thickness = 0.5.dp,
                color = TUMColors.tumGray8
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = role.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Black
                )
                IconButton(onClick = { viewModel.onRoleToggled(role) }) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Remove",
                        tint = TUMColors.tumRed
                    )
                }
            }
        }

        CustomHorizontalDivider()

        val isEnabled = uiState.selectedMember != null

        CustomTextButton(
            onClick = {
                viewModel.saveChanges()
            },
            text = "Save Changes",
            enabled = isEnabled,
            isSending = uiState.isAssigning,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }
}

@Preview
@Composable
private fun GiveRoleScreenPreview() {
    val repo = DefaultAppContainer().discordBotRepository
    MaterialTheme {
        Surface(color = TUMColors.tumGray9) {
            Box(modifier = Modifier.padding(16.dp)) {
                AssignRoleScreen(AssignRoleViewModel(repo), {})
            }
        }
    }
}
