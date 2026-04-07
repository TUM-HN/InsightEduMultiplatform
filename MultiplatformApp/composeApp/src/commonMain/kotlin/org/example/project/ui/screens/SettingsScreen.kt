package org.example.project.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.example.project.TUMColors
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import org.example.project.data.DefaultAppContainer
import org.example.project.model.viewModels.SettingsViewModel
import org.example.project.ui.reusableComponents.CustomAlertDialog
import org.example.project.ui.reusableComponents.CustomCard
import org.example.project.ui.reusableComponents.CustomDropdownMenu
import org.example.project.ui.reusableComponents.CustomHorizontalDivider
import org.example.project.ui.reusableComponents.CustomLabeledTextField
import org.example.project.ui.reusableComponents.CustomTextButton
import org.example.project.ui.reusableComponents.CustomTextField
import org.example.project.ui.reusableComponents.CustomToggle
import org.example.project.ui.reusableComponents.CustomTopBar

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
) {
    val scrollState = rememberScrollState()

    LaunchedEffect(Unit) {
        viewModel.loadSettingsUiState()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TUMColors.tumGray9)
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            CustomTopBar("Settings") { viewModel.onBack(onBack) }
            Column(modifier = Modifier.fillMaxSize().verticalScroll(scrollState)) {
                ContentCard(viewModel = viewModel, onBack = onBack)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ContentCard(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()

    if (!uiState.isBotRunning) {
        CustomCard(
            title = "Server Settings",
            showRedStar = false,
            actionMessage = uiState.serverActionMessage,
            onClearActionMessage = { viewModel.clearServerActionMessage() }
        ) {
            CustomLabeledTextField(
                label = "Server URL",
                value = uiState.serverUrl,
                onValueChange = {
                    viewModel.onServerUrlChange(it)
                },
                placeholder = "Type here",
                enabled = uiState.isServerEditing
            )

            CustomHorizontalDivider()

            CustomLabeledTextField(
                label = "API Key",
                value = uiState.apiKey,
                onValueChange = {
                    viewModel.onApiKeyChange(it)
                },
                placeholder = "Type here",
                enabled = uiState.isServerEditing
            )

            CustomHorizontalDivider()

            CustomTextButton(
                onClick = {
                    viewModel.toggleServerEditing()
                },
                text = if (uiState.isServerEditing) "Save Server Settings" else "Edit Server Settings",
                enabled = !uiState.isSaving,
                isSending = uiState.isSaving && uiState.isServerEditing,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
        }

        if (uiState.isServerOnline) {
            CustomCard(
                title = "Management",
                showRedStar = false,
                actionMessage = uiState.managementActionMessage,
                onClearActionMessage = { viewModel.clearManagementActionMessage() }
            ) {
                CustomTextField(
                    message = "",
                    placeholder = "Bot Settings:",
                    onValueChange = {},
                    enabled = true,
                    isTitle = true,
                )

                CustomHorizontalDivider()

                CustomToggle(
                    label = "Developer Mode",
                    checked = uiState.developerMode,
                    onCheckedChange = {
                        viewModel.onDeveloperModeChange(it)
                    },
                    enabled = uiState.isManagementEditing
                )

                CustomHorizontalDivider()

                CustomLabeledTextField(
                    label = "Bot Token",
                    value = if (uiState.isManagementEditing) {
                        if (uiState.developerMode) uiState.devToken else uiState.token
                    } else { "*******************" },
                    onValueChange = {
                        viewModel.onBotTokenChange(it)
                    },
                    placeholder = "Type here",
                    enabled = uiState.isManagementEditing
                )

                CustomHorizontalDivider()

                CustomLabeledTextField(
                    label = "Gemini API Key",
                    value = if (uiState.isManagementEditing) uiState.geminiApiKey else { "*******************" },
                    onValueChange = {
                        viewModel.onGeminiApiKeyChange(it)
                    },
                    placeholder = "Type here",
                    enabled = uiState.isManagementEditing
                )

                CustomHorizontalDivider()

                CustomTextField(
                    message = "",
                    placeholder = "Groups Management:",
                    onValueChange = {},
                    enabled = true,
                    isTitle = true,
                )

                CustomHorizontalDivider()

                if (uiState.isManagementEditing) {
                    CustomDropdownMenu(
                        onItemSelected = { count ->
                            if (count != null) {
                                viewModel.onGroupsCountChange(count)
                            }
                        },
                        items = (1..10).toList(),
                        selectedItem = uiState.groupsCount,
                        menuTitle = "Number of Groups:",
                        dropdownTitle = "Select number of groups",
                        itemLabel = { it?.toString() ?: "" },
                        isNoneOption = false,
                        showRedStar = false,
                    )
                    CustomHorizontalDivider()
                    for (group in uiState.groups) {
                        CustomDropdownMenu(
                            onItemSelected = { isValidString ->
                                viewModel.onGroupValidityChange(group.name, isValidString == "valid")
                            },
                            items = listOf("valid", "notValid"),
                            selectedItem = if (group.isValid) "valid" else "notValid",
                            menuTitle = group.name,
                            dropdownTitle = "Select validity",
                            itemLabel = { it ?: "" },
                            isNoneOption = false,
                        )
                        CustomHorizontalDivider()
                    }
                } else {
                    CustomLabeledTextField(
                        label = "Number of Groups:",
                        value = uiState.groupsCount.toString(),
                        onValueChange = {},
                        enabled = false,
                        isValid = true,
                        textWeight = 4f,
                        textFieldWeight = 2f,
                    )
                    CustomHorizontalDivider()
                    for (group in uiState.groups.filter { it.isValid }) {
                        CustomLabeledTextField(
                            label = group.name,
                            value = "valid",
                            onValueChange = {},
                            enabled = false,
                            isValid = true
                        )
                        CustomHorizontalDivider()
                    }
                }

                CustomTextButton(
                    onClick = {
                        viewModel.toggleManagementEditing()
                    },
                    text = if (uiState.isManagementEditing) "Save Management Settings" else "Edit Management Settings",
                    enabled = !uiState.isSaving,
                    isSending = uiState.isSaving && uiState.isManagementEditing,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )

                Spacer(modifier = Modifier.height(16.dp))
            }
        } else {
            CustomCard(
                title = "Management",
                showRedStar = false,
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudOff,
                            contentDescription = "Server Offline",
                            modifier = Modifier.size(32.dp),
                            tint = TUMColors.tumRed
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Server Offline",
                            style = MaterialTheme.typography.titleLarge,
                            color = TUMColors.tumRed
                        )
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "Management settings cannot be modified while the server is offline. To access and modify these settings, please connect to the server by configuring the Server Settings. Make sure it is running.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TUMColors.tumGray4
                    )
                }
            }
        }

        if (uiState.showDiscardDialog) {
            CustomAlertDialog(
                title = "Discard Changes",
                text = "Do you want to discard changes?",
                onConfirm = { viewModel.confirmDiscard(onBack) },
                onDismiss = { viewModel.dismissDiscardDialog() }
            )
        }
    } else {
        CustomCard(
            showRedStar = false,
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = "Bot is running",
                        modifier = Modifier.size(32.dp),
                        tint = TUMColors.tumGreen
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Bot is running",
                        style = MaterialTheme.typography.titleLarge,
                        color = TUMColors.tumGreen
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Settings cannot be modified while the bot is running.\nTo modify settings, please stop the bot first.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TUMColors.tumGray4
                )
            }
        }
    }
}

@Preview
@Composable
private fun SendMessageScreenPreview() {
    val repo = DefaultAppContainer().discordBotRepository
    MaterialTheme {
        Surface(color = TUMColors.tumGray9) {
            Box(modifier = Modifier.padding(16.dp)) {
                SettingsScreen(SettingsViewModel(repo), {})
            }
        }
    }
}
