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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.example.project.TUMColors
import org.example.project.data.DefaultAppContainer
import org.example.project.model.Channel
import org.example.project.model.viewModels.ClearMessagesViewModel
import org.example.project.ui.reusableComponents.CustomCard
import org.example.project.ui.reusableComponents.CustomDropdownMenu
import org.example.project.ui.reusableComponents.CustomTextButton
import org.example.project.ui.reusableComponents.CustomHorizontalDivider
import org.example.project.ui.reusableComponents.CustomTopBar
import org.example.project.ui.reusableComponents.NumberStepperItem
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun ClearMessagesScreen(
    viewModel: ClearMessagesViewModel,
    onBack: () -> Unit,
) {
    LaunchedEffect(Unit) {
        viewModel.loadClearMessagesUiState()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TUMColors.tumGray9)
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            CustomTopBar("Clear Messages", onBack)
            ContentCard(viewModel = viewModel)
        }
    }
}

@Composable
private fun ContentCard(
    viewModel: ClearMessagesViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedChannel: Channel? by remember { mutableStateOf(null) }
    var limit: Int by remember { mutableStateOf(1) }

    CustomCard(
        showTitle = false,
        statusMessage = uiState.statusMessage,
        actionMessage = uiState.actionMessage,
        onClearActionMessage = { viewModel.clearActionMessage() },
    ) {
        CustomDropdownMenu(
            onItemSelected = { channel -> selectedChannel = channel },
            items = uiState.channels,
            selectedItem = selectedChannel,
            menuTitle = "Channel",
            dropdownTitle = "Select a channel",
            itemLabel = { channel -> channel?.name ?: "Select a channel" },
        )

        CustomHorizontalDivider()

        NumberStepperItem(
            value = limit,
            onValueChange = { newValue -> limit = newValue },
            range = 1..10
        )

        CustomHorizontalDivider()

        CustomTextButton(
            onClick = {
                viewModel.clearMessages(selectedChannel!!, limit)
            },
            text = "Clear Messages",
            enabled = selectedChannel != null,
            isSending = uiState.isClearing,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }
}

@Preview
@Composable
fun ClearMessagesScreenPreview() {
    val repo = DefaultAppContainer().discordBotRepository
    MaterialTheme {
        Surface(color = TUMColors.tumGray9) {
            Box(modifier = Modifier.padding(16.dp)) {
                ClearMessagesScreen(ClearMessagesViewModel(repo), {})
            }
        }
    }
}
