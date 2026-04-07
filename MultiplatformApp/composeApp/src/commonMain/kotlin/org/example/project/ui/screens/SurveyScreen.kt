package org.example.project.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.example.project.TUMColors
import org.example.project.data.DefaultAppContainer
import org.example.project.model.viewModels.SurveyViewModel
import org.example.project.ui.reusableComponents.CustomCard
import org.example.project.ui.reusableComponents.CustomDropdownMenu
import org.example.project.ui.reusableComponents.CustomHorizontalDivider
import org.example.project.ui.reusableComponents.CustomTextButton
import org.example.project.ui.reusableComponents.CustomTextField
import org.example.project.ui.reusableComponents.CustomTopBar
import org.example.project.ui.reusableComponents.DurationSlider
import org.example.project.ui.reusableComponents.FilesHistoryCard
import org.example.project.ui.reusableComponents.HorizontalDistributionChart
import androidx.compose.ui.tooling.preview.Preview
import org.example.project.ui.reusableComponents.CustomLoadingMessage

@Composable
fun SurveyScreen(
    viewModel: SurveyViewModel,
    isSimple: Boolean,
    onBack: () -> Unit,
    isTabletHorizontal: Boolean = false
) {
    val uiState by viewModel.uiState.collectAsState()
    val title = if (isSimple) "Simple Survey" else "Complex Survey"
    val scrollState = rememberLazyListState()

    LaunchedEffect(Unit) {
        viewModel.loadSurveyUiState()
        viewModel.fetchSurveyHistory()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TUMColors.tumGray9)
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        if (uiState.selectedSurveyFile == null || isTabletHorizontal) {
            Column(modifier = Modifier.fillMaxSize()) {
                CustomTopBar(title, onBack)
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    state = scrollState
                ) {
                    item {
                        SurveyContentCard(viewModel, isSimple)
                    }
                }
            }
        } else {
            SurveyDetailScreen(
                viewModel = viewModel,
                onBack = { viewModel.clearSelectedHistory() }
            )
        }
    }
}

@Composable
private fun SurveyContentCard(
    viewModel: SurveyViewModel,
    isSimple: Boolean
) {
    val uiState by viewModel.uiState.collectAsState()

    CustomCard(
        title = if (isSimple )"Simple Survey Settings" else "Complex Survey Settings",
        statusMessage = uiState.channelStatusMessage,
        actionMessage = uiState.surveyActionMessage,
        onClearActionMessage = { viewModel.clearSurveyActionMessage() },
    ) {
        CustomTextField(
            message = uiState.textMessage ?: "",
            placeholder = "Message",
            onValueChange = { newMessage ->
                viewModel.onMessageChanged(newMessage)
            },
            enabled = true
        )

        CustomHorizontalDivider()

        CustomTextField(
            message = uiState.mainTopic ?: "",
            placeholder = "Main Topic",
            onValueChange = { newTopic ->
                viewModel.onMainTopicChanged(newTopic)
            },
            enabled = true
        )

        CustomHorizontalDivider()

        if (isSimple) {
            CustomDropdownMenu(
                onItemSelected = { buttonType -> viewModel.onButtonTypeSelected(buttonType) },
                items = uiState.buttonTypes,
                selectedItem = uiState.selectedButtonType,
                menuTitle = "Button Type",
                dropdownTitle = "Select button type",
                itemLabel = { buttonType -> buttonType ?: "Select button type" }
            )
        } else {
            uiState.questions.forEachIndexed { index, (questionText, buttonType) ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Column(modifier = Modifier.weight(4f)) {
                        CustomTextField(
                            message = questionText,
                            placeholder = "Question ${index + 1}",
                            onValueChange = { viewModel.updateQuestion(index, it) },
                            enabled = true
                        )
                    }
                    Column(modifier = Modifier.weight(3f)) {
                        CustomDropdownMenu(
                            onItemSelected = { type -> type?.let { viewModel.updateQuestionButtonType(index, it) } },
                            items = uiState.buttonTypes,
                            selectedItem = buttonType,
                            menuTitle = "",
                            dropdownTitle = "Select type",
                            itemLabel = { it.toString() },
                            showRedStar = false
                        )
                    }
                    if (uiState.questions.size > 1) {
                        IconButton(
                            modifier = Modifier.weight(1f).padding(end = 16.dp),
                            onClick = { viewModel.removeQuestion(index) }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Remove",
                                tint = TUMColors.tumRed
                            )
                        }
                    }
                }
                CustomHorizontalDivider()
            }
            CustomTextButton(
                onClick = { viewModel.addQuestion() },
                text = "Add another question",
                enabled = true,
                isSending = false,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }

        CustomHorizontalDivider()

        CustomDropdownMenu(
            onItemSelected = { channel -> viewModel.onChannelSelected(channel) },
            items = uiState.channels,
            selectedItem = uiState.selectedChannel,
            menuTitle = "Channel",
            dropdownTitle = "Select a channel",
            itemLabel = { channel ->
                channel?.name ?: "Select a channel"
            }
        )

        CustomHorizontalDivider()

        DurationSlider(
            duration = uiState.duration,
            onDurationChanged = { viewModel.onDurationChanged(it) }
        )

        CustomHorizontalDivider()

        val enabled = if (isSimple) {
            uiState.selectedButtonType != null && uiState.selectedChannel != null && uiState.textMessage != null && uiState.mainTopic != null
        } else {
            uiState.selectedChannel != null && uiState.textMessage != null && uiState.mainTopic != null && uiState.questions.all { it.first.isNotBlank() }
        }

        CustomTextButton(
            onClick = {
                if (isSimple) {
                    viewModel.createSimpleSurvey()
                } else {
                    viewModel.createComplexSurvey()
                }
            },
            text = if (isSimple) "Start Attendance" else "Start Survey",
            enabled = enabled,
            isSending = uiState.isSending,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }

    FilesHistoryCard(
        title = "Survey History",
        isFeedback = false,
        history = uiState.surveyHistory,
        isLoading = uiState.isLoadingHistory,
        actionMessage = uiState.historyActionMessage,
        onClearActionMessage = { viewModel.clearHistoryActionMessage() },
        onItemClick = { viewModel.fetchSurveyContent(it) },
        onReload = { viewModel.fetchSurveyHistory() }
    )
    Spacer(modifier = Modifier.height(16.dp))
}

@Composable
fun SurveyDetailScreen(
    viewModel: SurveyViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val file = uiState.selectedSurveyFile ?: return
    val name = file.name.split("_")[1]

    Column(modifier = Modifier.fillMaxSize()) {
        CustomTopBar(title = name, onBack = onBack)

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            if (uiState.isLoadingDetails) {
                item {
                    CustomCard(
                        title = "Loading",
                        showRedStar = false,
                        actionMessage = uiState.historyActionMessage,
                        onClearActionMessage = { viewModel.clearHistoryActionMessage() }
                    ) {
                        Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = TUMColors.tumBlue)
                        }
                    }
                }
            } else if (uiState.surveyDetails.isEmpty()) {
                item {
                    CustomCard(
                        title = "Response Distribution",
                        showRedStar = false,
                        actionMessage = uiState.historyActionMessage,
                        onClearActionMessage = { viewModel.clearHistoryActionMessage() }
                    ) {
                        CustomLoadingMessage("No attendees found")
                    }
                }
            } else {
                val allQuestions = uiState.surveyDetails
                    .flatMap { it.responsePairs.map { pair -> pair.first } }
                    .distinct()
                    .sorted()

                allQuestions.forEach { question ->
                    if (question != "Open Feedback") {
                        item {
                            CustomCard(
                                title = question,
                                actionMessage = uiState.historyActionMessage,
                                onClearActionMessage = { viewModel.clearHistoryActionMessage() },
                                showRedStar = false
                            ) {
                                val distribution = uiState.surveyDetails
                                    .mapNotNull { entry -> entry.responsePairs.find { it.first == question }?.second }
                                    .groupingBy { it }
                                    .eachCount()
                                    .toList()
                                    .sortedByDescending { it.second }
                                HorizontalDistributionChart(items = distribution)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun SimpleSurveyScreenPreview() {
    val repo = DefaultAppContainer().discordBotRepository
    MaterialTheme {
        Surface(color = TUMColors.tumGray9) {
            Box(modifier = Modifier.padding(16.dp)) {
                SurveyScreen(SurveyViewModel(repo, true), isSimple = true, onBack = {})
            }
        }
    }
}

@Preview
@Composable
fun ComplexSurveyScreenPreview() {
    val repo = DefaultAppContainer().discordBotRepository
    MaterialTheme {
        Surface(color = TUMColors.tumGray9) {
            Box(modifier = Modifier.padding(16.dp)) {
                SurveyScreen(SurveyViewModel(repo, false), isSimple = false, onBack = {})
            }
        }
    }
}
