package org.example.project.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import org.example.project.TUMColors
import org.example.project.data.DefaultAppContainer
import org.example.project.model.viewModels.SendMessageViewModel
import org.example.project.model.Member
import org.example.project.ui.reusableComponents.CustomCard
import org.example.project.ui.reusableComponents.CustomDropdownMenu
import org.example.project.ui.reusableComponents.CustomHorizontalDivider
import org.example.project.ui.reusableComponents.CustomTextButton
import org.example.project.ui.reusableComponents.CustomTextField
import org.example.project.ui.reusableComponents.CustomTopBar
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun SendMessageScreen(
    viewModel: SendMessageViewModel,
    onBack: () -> Unit,
) {
    LaunchedEffect(Unit) {
        viewModel.loadSendMessageUiState()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TUMColors.tumGray9)
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            CustomTopBar("Message Command", onBack)
            ContentCard(viewModel = viewModel)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ContentCard(
    viewModel: SendMessageViewModel,
) {
    val uiState by viewModel.uiState.collectAsState()
    var selected: Member? by remember { mutableStateOf(null) }
    var message by remember { mutableStateOf("") }

    CustomCard(
        showTitle = false,
        statusMessage = uiState.statusMessage,
        actionMessage = uiState.actionMessage,
        onClearActionMessage = { viewModel.clearActionMessage() },
    ) {
        CustomTextField(
            message = message,
            placeholder = "Message",
            onValueChange = { newMessage ->
                message = newMessage
            },
            enabled = uiState.isBotRunning
        )

        CustomHorizontalDivider()

        CustomDropdownMenu(
            onItemSelected = { member -> selected = member },
            items = uiState.members,
            selectedItem = selected,
            menuTitle = "Member",
            dropdownTitle = "Select a member",
            itemLabel = { member ->
                if (member != null) {
                    member.displayName + " (" + member.name + ")"
                } else "Select a member"
            },
        )

        CustomHorizontalDivider()

        val isEnabled =
            uiState.isBotRunning && !uiState.isSending && selected != null && message.isNotBlank()

        CustomTextButton(
            onClick = {
                selected?.let { viewModel.sendMessageToMember(it.id, message) }
            },
            text = "Send Message",
            enabled = isEnabled,
            isSending = uiState.isSending,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
    }
}

@Preview
@Composable
private fun SendMessageScreenPreview() {
    val repo = DefaultAppContainer().discordBotRepository
    MaterialTheme {
        Surface(color = TUMColors.tumGray9) {
            Box(modifier = Modifier.padding(16.dp)) {
                SendMessageScreen(SendMessageViewModel(repo), {})
            }
        }
    }
}
